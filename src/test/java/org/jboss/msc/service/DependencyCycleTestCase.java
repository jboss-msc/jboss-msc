/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.msc.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Future;

import org.jboss.msc.service.ServiceBuilder.DependencyType;
import org.jboss.msc.service.ServiceController.Mode;
import org.jboss.msc.service.ServiceController.State;
import org.jboss.msc.service.util.FailToStartService;
import org.jboss.msc.util.TestServiceListener;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests scenarios with dependency cycles
 * 
 * @author <a href="mailto:flavia.rainone@jboss.com">Flavia Rainone</a>
 */
public class DependencyCycleTestCase extends AbstractServiceTest {

    private final ServiceName serviceAName = ServiceName.of("A");
    private final ServiceName serviceBName = ServiceName.of("B");
    private final ServiceName serviceCName = ServiceName.of("C");
    private final ServiceName serviceDName = ServiceName.of("D");
    private final ServiceName serviceEName = ServiceName.of("E");
    private final ServiceName serviceFName = ServiceName.of("F");
    private final ServiceName serviceGName = ServiceName.of("G");
    private final ServiceName serviceHName = ServiceName.of("H");
    private final ServiceName serviceIName = ServiceName.of("I");
    private final ServiceName serviceJName = ServiceName.of("J");
    private final ServiceName serviceKName = ServiceName.of("K");
    private final ServiceName serviceLName = ServiceName.of("L");
    private final ServiceName serviceMName = ServiceName.of("M");
    private final ServiceName serviceNName = ServiceName.of("N");
    private final ServiceName serviceOName = ServiceName.of("O");
    private final ServiceName servicePName = ServiceName.of("P");
    private final ServiceName serviceQName = ServiceName.of("Q");
    private final ServiceName serviceRName = ServiceName.of("R");
    private final ServiceName serviceSName = ServiceName.of("S");
    private final ServiceName serviceTName = ServiceName.of("T");
    private final ServiceName serviceUName = ServiceName.of("U");
    private final ServiceName serviceVName = ServiceName.of("V");
    private final ServiceName serviceWName = ServiceName.of("W");
    TestServiceListener testListener;

    @Before
    public void initializeTestListener() {
        testListener = new TestServiceListener();
        serviceContainer.addListener(testListener);
    }

    @Test
    public void emptyContainer() {
        final Collection<List<ServiceName>> cycles = serviceContainer.detectCircularity();
        assertTrue(cycles == null || cycles.isEmpty());
    }

    @Test
    public void simpleCycle() throws Exception {
        final Future<ServiceController<?>> serviceAListenerAdded = testListener.expectListenerAdded(serviceAName);
        final Future<ServiceController<?>> serviceBListenerAdded = testListener.expectListenerAdded(serviceBName);
        final Future<ServiceController<?>> serviceCListenerAdded = testListener.expectListenerAdded(serviceCName);

        serviceContainer.addService(serviceAName, Service.NULL).addDependency(serviceBName).install();
        serviceContainer.addService(serviceBName, Service.NULL).addDependency(serviceCName).install();
        serviceContainer.addService(serviceCName, Service.NULL).addDependency(serviceAName).install();

        final ServiceController<?> serviceAController = assertController(serviceAName, serviceAListenerAdded);
        assertSame(State.DOWN, serviceAController.getState());
        final ServiceController<?> serviceBController = assertController(serviceBName, serviceBListenerAdded);
        assertSame(State.DOWN, serviceBController.getState());
        final ServiceController<?> serviceCController = assertController(serviceCName, serviceCListenerAdded);
        assertSame(State.DOWN, serviceCController.getState());

        final Collection<List<ServiceName>> cycles = serviceContainer.detectCircularity();
        assertNotNull(cycles);
        assertEquals(1, cycles.size());
        assertCycle(cycles.iterator().next(), serviceAName, serviceBName, serviceCName);
    }

    @Test
    public void cycleOnRunning() throws Exception {
        final Future<ServiceController<?>> serviceADepMissing = testListener.expectDependencyUninstall(serviceAName);
        final Future<ServiceController<?>> serviceBDepMissing = testListener.expectDependencyUninstall(serviceBName);

        serviceContainer.addService(serviceAName, Service.NULL).addDependency(serviceBName).install();
        serviceContainer.addService(serviceBName, Service.NULL).addDependency(serviceCName).install();

        final ServiceController<?> serviceAController = assertController(serviceAName, serviceADepMissing);
        final ServiceController<?> serviceBController = assertController(serviceBName, serviceBDepMissing);

        Collection<List<ServiceName>> cycles = serviceContainer.detectCircularity();
        assertNotNull(cycles);
        assertTrue(cycles.isEmpty());

        final Future<ServiceController<?>> serviceCListenerAdded = testListener.expectListenerAdded(serviceCName);
        final Future<ServiceController<?>> serviceADepInstall = testListener.expectDependencyInstall(serviceAName);
        final Future<ServiceController<?>> serviceBDepInstall = testListener.expectDependencyInstall(serviceBName);
        serviceContainer.addService(serviceCName, Service.NULL).addDependency(serviceAName).install();
        assertController(serviceCName, serviceCListenerAdded);
        assertController(serviceAController, serviceADepInstall);
        assertController(serviceBController, serviceBDepInstall);
        final ServiceController<?> serviceCController = assertController(serviceCName, serviceCListenerAdded);
        assertSame(State.DOWN, serviceCController.getState());

        cycles = serviceContainer.detectCircularity();
        assertNotNull(cycles);
        assertEquals(1, cycles.size());
        assertCycle(cycles.iterator().next(), serviceAName, serviceBName, serviceCName);
    }

    // full scenario:
    // A->B,F; B->C; C->D; D->E; E->C; F->G; G->H; H->I,W; I->H,J; J->K; K->G,H;
    // L->M; M->N; N->H,O; O->L;
    // P->Q; Q->R; R->S; S->T; T->P
    // U->V (no cycle here)
    @Test
    public void multipleCycles() throws Exception {
        // first install A, B, C, D, L, M, O
        final Future<ServiceController<?>> serviceADepMissing = testListener.expectDependencyUninstall(serviceAName);
        final Future<ServiceController<?>> serviceBDepMissing = testListener.expectDependencyUninstall(serviceBName);
        final Future<ServiceController<?>> serviceCDepMissing = testListener.expectDependencyUninstall(serviceCName);
        final Future<ServiceController<?>> serviceDDepMissing = testListener.expectDependencyUninstall(serviceDName);
        final Future<ServiceController<?>> serviceLDepMissing = testListener.expectDependencyUninstall(serviceLName);
        final Future<ServiceController<?>> serviceMDepMissing = testListener.expectDependencyUninstall(serviceMName);
        final Future<ServiceController<?>> serviceODepMissing = testListener.expectDependencyUninstall(serviceOName);

        serviceContainer.addService(serviceAName, Service.NULL).addDependencies(serviceBName, serviceFName).install();
        serviceContainer.addService(serviceBName, Service.NULL).addDependency(serviceCName).install();
        serviceContainer.addService(serviceCName, Service.NULL).addDependency(serviceDName).install();
        serviceContainer.addService(serviceDName, Service.NULL).addDependency(serviceEName).install();
        serviceContainer.addService(serviceLName, Service.NULL).addDependency(serviceMName).install();
        serviceContainer.addService(serviceMName, Service.NULL).addDependency(serviceNName).install();
        serviceContainer.addService(serviceOName, Service.NULL).addDependency(serviceLName).install();

        final ServiceController<?> serviceAController = assertController(serviceAName, serviceADepMissing);
        final ServiceController<?> serviceBController = assertController(serviceBName, serviceBDepMissing);
        final ServiceController<?> serviceCController = assertController(serviceCName, serviceCDepMissing);
        final ServiceController<?> serviceDController = assertController(serviceDName, serviceDDepMissing);
        final ServiceController<?> serviceLController = assertController(serviceLName, serviceLDepMissing);
        final ServiceController<?> serviceMController = assertController(serviceMName, serviceMDepMissing);
        final ServiceController<?> serviceOController = assertController(serviceOName, serviceODepMissing);

        Collection<List<ServiceName>> cycles = serviceContainer.detectCircularity();
        assertNotNull(cycles);
        assertTrue(cycles.isEmpty());

        // install service N
        final Future<ServiceController<?>> serviceNDepMissing = testListener.expectDependencyUninstall(serviceNName);
        serviceContainer.addService(serviceNName, Service.NULL).addDependencies(serviceHName, serviceOName).install();
        final ServiceController<?> serviceNController = assertController(serviceNName, serviceNDepMissing);

        cycles = serviceContainer.detectCircularity();
        assertNotNull(cycles);
        assertEquals(1, cycles.size());
        assertCycle(cycles.iterator().next(), serviceLName, serviceMName, serviceNName, serviceOName);

        // install E, F, G, H, I, V
        final Future<ServiceController<?>> serviceBDepInstall = testListener.expectDependencyInstall(serviceBName);
        final Future<ServiceController<?>> serviceCDepInstall = testListener.expectDependencyInstall(serviceCName);
        final Future<ServiceController<?>> serviceDDepInstall = testListener.expectDependencyInstall(serviceDName);
        final Future<ServiceController<?>> serviceEListenerAdded = testListener.expectListenerAdded(serviceEName);
        final Future<ServiceController<?>> serviceFDepMissing = testListener.expectDependencyUninstall(serviceFName);
        final Future<ServiceController<?>> serviceGDepMissing = testListener.expectDependencyUninstall(serviceGName);
        final Future<ServiceController<?>> serviceHDepMissing = testListener.expectDependencyUninstall(serviceHName);
        final Future<ServiceController<?>> serviceIDepMissing = testListener.expectDependencyUninstall(serviceIName);
        final Future<ServiceController<?>> serviceVStart = testListener.expectServiceStart(serviceVName);

        serviceContainer.addService(serviceEName, Service.NULL).addDependency(serviceCName).install();
        serviceContainer.addService(serviceFName, Service.NULL).addDependency(serviceGName).install();
        serviceContainer.addService(serviceGName, Service.NULL).addDependency(serviceHName).install();
        serviceContainer.addService(serviceHName, Service.NULL).addDependencies(serviceIName, serviceWName).install();
        serviceContainer.addService(serviceIName, Service.NULL).addDependencies(serviceHName, serviceJName).install();
        serviceContainer.addService(serviceVName, Service.NULL).install();

        assertSame(State.DOWN, serviceAController.getState());
        assertController(serviceBController, serviceBDepInstall);
        assertSame(State.DOWN, serviceBController.getState());
        assertController(serviceCController, serviceCDepInstall);
        assertSame(State.DOWN, serviceCController.getState());
        assertController(serviceDController, serviceDDepInstall);
        assertSame(State.DOWN, serviceDController.getState());
        final ServiceController<?> serviceEController = assertController(serviceEName, serviceEListenerAdded);
        assertSame(State.DOWN, serviceEController.getState());
        final ServiceController<?> serviceFController = assertController(serviceFName, serviceFDepMissing);
        assertSame(State.DOWN, serviceFController.getState());
        final ServiceController<?> serviceGController = assertController(serviceGName, serviceGDepMissing);
        assertSame(State.DOWN, serviceFController.getState());
        final ServiceController<?> serviceHController = assertController(serviceHName, serviceHDepMissing);
        assertSame(State.DOWN, serviceFController.getState());
        final ServiceController<?> serviceIController = assertController(serviceIName, serviceIDepMissing);
        assertSame(State.DOWN, serviceFController.getState());
        assertController(serviceVName, serviceVStart);

        cycles = serviceContainer.detectCircularity();
        assertCycles(cycles, new ServiceName[] {serviceCName, serviceDName, serviceEName},  
                new ServiceName[] {serviceLName, serviceMName, serviceNName, serviceOName},
                new ServiceName[] { serviceHName, serviceIName});

        // install J, P, Q, R, S, T, U
        final Future<ServiceController<?>> serviceJDepMissing = testListener.expectDependencyUninstall(serviceJName);
        final Future<ServiceController<?>> servicePListenerAdded = testListener.expectListenerAdded(servicePName);
        final Future<ServiceController<?>> serviceQListenerAdded = testListener.expectListenerAdded(serviceQName);
        final Future<ServiceController<?>> serviceRListenerAdded = testListener.expectListenerAdded(serviceRName);
        final Future<ServiceController<?>> serviceSListenerAdded = testListener.expectListenerAdded(serviceSName);
        final Future<ServiceController<?>> serviceTListenerAdded = testListener.expectListenerAdded(serviceTName);
        final Future<ServiceController<?>> serviceUStart = testListener.expectServiceStart(serviceUName);

        serviceContainer.addService(serviceJName, Service.NULL).addDependency(serviceKName).install();
        serviceContainer.addService(servicePName, Service.NULL).addDependency(serviceQName).install();
        serviceContainer.addService(serviceQName, Service.NULL).addDependency(serviceRName).install();
        serviceContainer.addService(serviceRName, Service.NULL).addDependency(serviceSName).install();
        serviceContainer.addService(serviceSName, Service.NULL).addDependency(serviceTName).install();
        serviceContainer.addService(serviceTName, Service.NULL).addDependency(servicePName).install();
        serviceContainer.addService(serviceUName, Service.NULL).addDependency(serviceVName).install();

        final ServiceController<?> serviceJController = assertController(serviceJName, serviceJDepMissing);
        final ServiceController<?> servicePController = assertController(servicePName, servicePListenerAdded);
        assertSame(State.DOWN, servicePController.getState());
        final ServiceController<?> serviceQController = assertController(serviceQName, serviceQListenerAdded);
        assertSame(State.DOWN, serviceQController.getState());
        final ServiceController<?> serviceRController = assertController(serviceRName, serviceRListenerAdded);
        assertSame(State.DOWN, serviceRController.getState());
        final ServiceController<?> serviceSController = assertController(serviceSName, serviceSListenerAdded);
        assertSame(State.DOWN, serviceSController.getState());
        final ServiceController<?> serviceTController = assertController(serviceTName, serviceTListenerAdded);
        assertSame(State.DOWN, serviceTController.getState());
        assertController(serviceUName, serviceUStart);

        cycles = serviceContainer.detectCircularity();
        assertCycles(cycles, new ServiceName[] {serviceCName, serviceDName, serviceEName},  
                new ServiceName[] {serviceLName, serviceMName, serviceNName, serviceOName},
                new ServiceName[] {serviceHName, serviceIName},
                new ServiceName[] {servicePName, serviceQName, serviceRName, serviceSName, serviceTName});

        // install service K
        final Future<ServiceController<?>> serviceKDepMissing = testListener.expectDependencyUninstall(serviceKName);
        serviceContainer.addService(serviceKName, Service.NULL).addDependencies(serviceGName, serviceHName).install();
        final ServiceController<?> serviceKController = assertController(serviceKName, serviceKDepMissing);

        // install service W
        final Future<ServiceController<?>> serviceWStart = testListener.expectServiceStart(serviceWName);
        final Future<ServiceController<?>> serviceADepInstall = testListener.expectDependencyInstall(serviceAName);
        final Future<ServiceController<?>> serviceFDepInstall = testListener.expectDependencyInstall(serviceFName);
        final Future<ServiceController<?>> serviceGDepInstall = testListener.expectDependencyInstall(serviceGName);
        final Future<ServiceController<?>> serviceHDepInstall = testListener.expectDependencyInstall(serviceHName);
        final Future<ServiceController<?>> serviceIDepInstall = testListener.expectDependencyInstall(serviceIName);
        final Future<ServiceController<?>> serviceJDepInstall = testListener.expectDependencyInstall(serviceJName);
        final Future<ServiceController<?>> serviceKDepInstall = testListener.expectDependencyInstall(serviceKName);
        final Future<ServiceController<?>> serviceLDepInstall = testListener.expectDependencyInstall(serviceLName);
        final Future<ServiceController<?>> serviceMDepInstall = testListener.expectDependencyInstall(serviceMName);
        final Future<ServiceController<?>> serviceNDepInstall = testListener.expectDependencyInstall(serviceNName);
        final Future<ServiceController<?>> serviceODepInstall = testListener.expectDependencyInstall(serviceOName);
        serviceContainer.addService(serviceWName, Service.NULL).install();
        assertController(serviceWName, serviceWStart);
        assertController(serviceAController, serviceADepInstall);
        assertController(serviceFController, serviceFDepInstall);
        assertController(serviceGController, serviceGDepInstall);
        assertController(serviceHController, serviceHDepInstall);
        assertController(serviceIController, serviceIDepInstall);
        assertController(serviceJController, serviceJDepInstall);
        assertController(serviceKController, serviceKDepInstall);
        assertController(serviceLController, serviceLDepInstall);
        assertController(serviceMController, serviceMDepInstall);
        assertController(serviceNController, serviceNDepInstall);
        assertController(serviceOController, serviceODepInstall);
        assertSame(State.DOWN, serviceAController.getState());
        assertSame(State.DOWN, serviceFController.getState());
        assertSame(State.DOWN, serviceGController.getState());
        assertSame(State.DOWN, serviceHController.getState());
        assertSame(State.DOWN, serviceIController.getState());
        assertSame(State.DOWN, serviceJController.getState());
        assertSame(State.DOWN, serviceKController.getState());
        assertSame(State.DOWN, serviceLController.getState());
        assertSame(State.DOWN, serviceMController.getState());
        assertSame(State.DOWN, serviceNController.getState());
        assertSame(State.DOWN, serviceOController.getState());

        cycles = serviceContainer.detectCircularity();
        assertCycles(cycles, new ServiceName[] {serviceCName, serviceDName, serviceEName},
                new ServiceName[] {serviceGName, serviceHName, serviceIName, serviceJName, serviceKName},
                new ServiceName[] {serviceHName, serviceIName, serviceJName, serviceKName},
                new ServiceName[] {serviceHName, serviceIName},
                new ServiceName[] {serviceLName, serviceMName, serviceNName, serviceOName},
                new ServiceName[] {servicePName, serviceQName, serviceRName, serviceSName, serviceTName});
    }

    // full scenario:
    // A->B,H,I,K,L; B->C; C->D; D->E; E->A,F; F->G; G->D,E; I->J; J->A; M->A,L,N,O; O->M
    // the dependencies F->G; M->O; A->I; A->L and M->N are optional
    @Test
    public void multipleCyclesWithOptionalDependencies() throws Exception {
        // install G
        Future<ServiceController<?>> serviceGMissingDep = testListener.expectDependencyUninstall(serviceGName);
        serviceContainer.addService(serviceGName, Service.NULL).addDependencies(serviceDName, serviceEName).install();
        final ServiceController<?> serviceGController = assertController(serviceGName, serviceGMissingDep);

        Collection<List<ServiceName>> cycles = serviceContainer.detectCircularity();
        assertNotNull(cycles);
        assertTrue(cycles.isEmpty());

        // install L
        final FailToStartService serviceL = new FailToStartService(true);
        Future<StartException> serviceLFailure = testListener.expectServiceFailure(serviceLName);
        serviceContainer.addService(serviceLName, serviceL).install();
        ServiceController<?> serviceLController = assertFailure(serviceLName, serviceLFailure);

        // install A, B, C, D, E, F, M, O
        Future<ServiceController<?>> serviceAMissingDep = testListener.expectDependencyUninstall(serviceAName);
        Future<ServiceController<?>> serviceBMissingDep = testListener.expectDependencyUninstall(serviceBName);
        Future<ServiceController<?>> serviceCMissingDep = testListener.expectDependencyUninstall(serviceCName);
        Future<ServiceController<?>> serviceDMissingDep = testListener.expectDependencyUninstall(serviceDName);
        Future<ServiceController<?>> serviceEMissingDep = testListener.expectDependencyUninstall(serviceEName);
        Future<ServiceController<?>> serviceFMissingDep = testListener.expectDependencyUninstall(serviceFName);
        Future<ServiceController<?>> serviceMMissingDep = testListener.expectDependencyUninstall(serviceMName);
        Future<ServiceController<?>> serviceOMissingDep = testListener.expectDependencyUninstall(serviceOName);
        Future<ServiceController<?>> serviceAFailedDep = testListener.expectDependencyFailure(serviceAName);
        Future<ServiceController<?>> serviceBFailedDep = testListener.expectDependencyFailure(serviceBName);
        Future<ServiceController<?>> serviceCFailedDep = testListener.expectDependencyFailure(serviceCName);
        Future<ServiceController<?>> serviceDFailedDep = testListener.expectDependencyFailure(serviceDName);
        Future<ServiceController<?>> serviceEFailedDep = testListener.expectDependencyFailure(serviceEName);
        Future<ServiceController<?>> serviceFFailedDep = testListener.expectDependencyFailure(serviceFName);
        Future<ServiceController<?>> serviceGFailedDep = testListener.expectDependencyFailure(serviceGName);
        Future<ServiceController<?>> serviceMFailedDep = testListener.expectDependencyFailure(serviceMName);
        Future<ServiceController<?>> serviceOFailedDep = testListener.expectDependencyFailure(serviceOName);

        serviceContainer.addService(serviceAName, Service.NULL).addDependencies(serviceBName, serviceHName, serviceIName, serviceKName)
            .addDependency(DependencyType.OPTIONAL, serviceLName).install();
        serviceContainer.addService(serviceBName, Service.NULL).addDependency(serviceCName).install();
        serviceContainer.addService(serviceCName, Service.NULL).addDependency(serviceDName).install();
        serviceContainer.addService(serviceDName, Service.NULL).addDependency(serviceEName).install();
        serviceContainer.addService(serviceEName, Service.NULL).addDependencies(serviceAName, serviceFName).install();
        serviceContainer.addService(serviceFName, Service.NULL).addDependency(DependencyType.OPTIONAL, serviceGName)
            .install();
        serviceContainer.addService(serviceOName, Service.NULL).addDependency(serviceMName).install();
        serviceContainer.addService(serviceMName, Service.NULL).addDependencies(serviceAName, serviceNName)
            .addDependencies(DependencyType.OPTIONAL, serviceLName, serviceOName).install();

        ServiceController<?> serviceAController = assertController(serviceAName, serviceAMissingDep);
        assertController(serviceAController, serviceAFailedDep);
        ServiceController<?> serviceEController = assertController(serviceEName, serviceEMissingDep);
        assertController(serviceEController, serviceEFailedDep);
        assertController(serviceGController, serviceGFailedDep);
        ServiceController<?> serviceBController = assertController(serviceBName, serviceBMissingDep);
        assertController(serviceBController, serviceBFailedDep);
        ServiceController<?> serviceCController = assertController(serviceCName, serviceCMissingDep);
        assertController(serviceCController, serviceCFailedDep);
        ServiceController<?> serviceDController = assertController(serviceDName, serviceDMissingDep);
        assertController(serviceDController, serviceDFailedDep);
        ServiceController<?> serviceFController = assertController(serviceFName, serviceFMissingDep);
        assertController(serviceFController, serviceFFailedDep);

        ServiceController<?> serviceMController = assertController(serviceMName, serviceMMissingDep);
        assertController(serviceMController, serviceMFailedDep);
        ServiceController<?> serviceOController = assertController(serviceOName, serviceOMissingDep);
        assertController(serviceOController, serviceOFailedDep);

        cycles = serviceContainer.detectCircularity();
        assertCycles(cycles, new ServiceName[] {serviceAName, serviceBName, serviceCName, serviceDName, serviceEName},
                new ServiceName[] {serviceDName, serviceEName, serviceFName, serviceGName},
                new ServiceName[] {serviceEName, serviceFName, serviceGName}, new ServiceName[] {serviceMName, serviceOName});

        // install N
        final Future<StartException> serviceNFailed = testListener.expectServiceFailure(serviceNName);
        serviceContainer.addService(serviceNName, new FailToStartService(true)).install();
        final ServiceController<?> serviceNController = assertFailure(serviceNName, serviceNFailed);

        cycles = serviceContainer.detectCircularity();
        assertCycles(cycles, new ServiceName[] {serviceAName, serviceBName, serviceCName, serviceDName, serviceEName},
                new ServiceName[] {serviceDName, serviceEName, serviceFName, serviceGName},
                new ServiceName[] {serviceEName, serviceFName, serviceGName}, new ServiceName[] {serviceMName, serviceOName});

        // install H, I, J
        final Future<ServiceController<?>> serviceHStart = testListener.expectServiceStart(serviceHName);
        final Future<ServiceController<?>> serviceIFailedDep = testListener.expectDependencyFailure(serviceIName);
        final Future<ServiceController<?>> serviceJFailedDep = testListener.expectDependencyFailure(serviceJName);

        serviceContainer.addService(serviceHName, Service.NULL).install();
        serviceContainer.addService(serviceIName, Service.NULL).addDependency(serviceJName).install();
        serviceContainer.addService(serviceJName, Service.NULL).addDependency(serviceAName).install();

        assertController(serviceHName, serviceHStart);
        final ServiceController<?> serviceIController = assertController(serviceIName, serviceIFailedDep);
        final ServiceController<?> serviceJController = assertController(serviceJName, serviceJFailedDep);

        cycles = serviceContainer.detectCircularity();
        assertCycles(cycles, new ServiceName[] {serviceAName, serviceBName, serviceCName, serviceDName, serviceEName},
                new ServiceName[] {serviceDName, serviceEName, serviceFName, serviceGName},
                new ServiceName[] {serviceEName, serviceFName, serviceGName},
                new ServiceName[] {serviceAName, serviceIName, serviceJName},
                new ServiceName[] {serviceMName, serviceOName});

        // install K
        final Future<StartException> serviceKFailure = testListener.expectServiceFailure(serviceKName);
        final Future<ServiceController<?>> serviceADepInstalled = testListener.expectDependencyInstall(serviceAName);
        final Future<ServiceController<?>> serviceBDepInstalled = testListener.expectDependencyInstall(serviceBName);
        final Future<ServiceController<?>> serviceCDepInstalled = testListener.expectDependencyInstall(serviceCName);
        final Future<ServiceController<?>> serviceDDepInstalled = testListener.expectDependencyInstall(serviceDName);
        final Future<ServiceController<?>> serviceEDepInstalled = testListener.expectDependencyInstall(serviceEName);
        final Future<ServiceController<?>> serviceFDepInstalled = testListener.expectDependencyInstall(serviceFName);
        final Future<ServiceController<?>> serviceGDepInstalled = testListener.expectDependencyInstall(serviceGName);
        final Future<ServiceController<?>> serviceMDepInstalled = testListener.expectDependencyInstall(serviceMName);
        final Future<ServiceController<?>> serviceODepInstalled = testListener.expectDependencyInstall(serviceOName);
        serviceContainer.addService(serviceKName, new FailToStartService(true)).install();
        final ServiceController<?> serviceKController = assertFailure(serviceKName, serviceKFailure);
        assertController(serviceAController, serviceADepInstalled);
        assertController(serviceBController, serviceBDepInstalled);
        assertController(serviceCController, serviceCDepInstalled);
        assertController(serviceDController, serviceDDepInstalled);
        assertController(serviceEController, serviceEDepInstalled);
        assertController(serviceFController, serviceFDepInstalled);
        assertController(serviceGController, serviceGDepInstalled);
        assertController(serviceMController, serviceMDepInstalled);
        assertController(serviceOController, serviceODepInstalled);

        cycles = serviceContainer.detectCircularity();
        assertCycles(cycles, new ServiceName[] {serviceAName, serviceBName, serviceCName, serviceDName, serviceEName},
                new ServiceName[] {serviceDName, serviceEName, serviceFName, serviceGName},
                new ServiceName[] {serviceEName, serviceFName, serviceGName},
                new ServiceName[] {serviceAName, serviceIName, serviceJName},
                new ServiceName[] {serviceMName, serviceOName});

        // remove service L
        final Future<ServiceController<?>> serviceLRemoval = testListener.expectServiceRemoval(serviceLName);
        serviceLController.setMode(Mode.REMOVE);
        assertController(serviceLController, serviceLRemoval);

        // restart K, this time without errors
        final Future<ServiceController<?>> serviceADepFailureCleared = testListener.expectDependencyFailureCleared(serviceAName);
        final Future<ServiceController<?>> serviceBDepFailureCleared = testListener.expectDependencyFailureCleared(serviceBName);
        final Future<ServiceController<?>> serviceCDepFailureCleared = testListener.expectDependencyFailureCleared(serviceCName);
        final Future<ServiceController<?>> serviceDDepFailureCleared = testListener.expectDependencyFailureCleared(serviceDName);
        final Future<ServiceController<?>> serviceEDepFailureCleared = testListener.expectDependencyFailureCleared(serviceEName);
        final Future<ServiceController<?>> serviceFDepFailureCleared = testListener.expectDependencyFailureCleared(serviceFName);
        final Future<ServiceController<?>> serviceGDepFailureCleared = testListener.expectDependencyFailureCleared(serviceGName);
        final Future<ServiceController<?>> serviceIDepFailureCleared = testListener.expectDependencyFailureCleared(serviceIName);
        final Future<ServiceController<?>> serviceJDepFailureCleared = testListener.expectDependencyFailureCleared(serviceJName);
        serviceKController.setMode(Mode.NEVER);
        assertController(serviceAController, serviceADepFailureCleared);
        assertController(serviceBController, serviceBDepFailureCleared);
        assertController(serviceCController, serviceCDepFailureCleared);
        assertController(serviceDController, serviceDDepFailureCleared);
        assertController(serviceEController, serviceEDepFailureCleared);
        assertController(serviceFController, serviceFDepFailureCleared);
        assertController(serviceGController, serviceGDepFailureCleared);
        assertController(serviceIController, serviceIDepFailureCleared);
        assertController(serviceJController, serviceJDepFailureCleared);

        final Future<ServiceController<?>> serviceKStart = testListener.expectServiceStart(serviceKName);
        serviceKController.setMode(Mode.ACTIVE);
        assertController(serviceKController, serviceKStart);

        cycles = serviceContainer.detectCircularity();
        assertCycles(cycles, new ServiceName[] {serviceAName, serviceBName, serviceCName, serviceDName, serviceEName},
                new ServiceName[] {serviceDName, serviceEName, serviceFName, serviceGName},
                new ServiceName[] {serviceEName, serviceFName, serviceGName},
                new ServiceName[] {serviceAName, serviceIName, serviceJName},
                new ServiceName[] {serviceMName, serviceOName});

        final Future<ServiceController<?>> serviceMDepFailureCleared = testListener.expectDependencyFailureCleared(serviceMName);
        final Future<ServiceController<?>> serviceODepFailureCleared = testListener.expectDependencyFailureCleared(serviceOName);
        serviceNController.setMode(Mode.NEVER);
        assertController(serviceMController, serviceMDepFailureCleared);
        assertController(serviceOController, serviceODepFailureCleared);

        cycles = serviceContainer.detectCircularity();
        assertCycles(cycles, new ServiceName[] {serviceAName, serviceBName, serviceCName, serviceDName, serviceEName},
                new ServiceName[] {serviceDName, serviceEName, serviceFName, serviceGName},
                new ServiceName[] {serviceEName, serviceFName, serviceGName},
                new ServiceName[] {serviceAName, serviceIName, serviceJName},
                new ServiceName[] {serviceMName, serviceOName});

        final Future<ServiceController<?>> serviceNStart = testListener.expectServiceStart(serviceNName);
        serviceNController.setMode(Mode.ACTIVE);
        assertController(serviceNController, serviceNStart);

        cycles = serviceContainer.detectCircularity();
        assertCycles(cycles, new ServiceName[] {serviceAName, serviceBName, serviceCName, serviceDName, serviceEName},
                new ServiceName[] {serviceDName, serviceEName, serviceFName, serviceGName},
                new ServiceName[] {serviceEName, serviceFName, serviceGName},
                new ServiceName[] {serviceAName, serviceIName, serviceJName},
                new ServiceName[] {serviceMName, serviceOName});

        final Future<ServiceController<?>> serviceLStart = testListener.expectServiceStart(serviceLName);
        serviceContainer.addService(serviceLName, serviceL).setInitialMode(Mode.ACTIVE).install();
        serviceLController = assertController(serviceLName, serviceLStart);

        cycles = serviceContainer.detectCircularity();
        assertCycles(cycles, new ServiceName[] {serviceAName, serviceBName, serviceCName, serviceDName, serviceEName},
                new ServiceName[] {serviceDName, serviceEName, serviceFName, serviceGName},
                new ServiceName[] {serviceEName, serviceFName, serviceGName},
                new ServiceName[] {serviceAName, serviceIName, serviceJName},
                new ServiceName[] {serviceMName, serviceOName});

        final Future<ServiceController<?>> serviceLStop = testListener.expectServiceStop(serviceLName);
        serviceLController.setMode(Mode.NEVER);
        assertController(serviceLController, serviceLStop);

        cycles = serviceContainer.detectCircularity();
        assertCycles(cycles, new ServiceName[] {serviceAName, serviceBName, serviceCName, serviceDName, serviceEName},
                new ServiceName[] {serviceDName, serviceEName, serviceFName, serviceGName},
                new ServiceName[] {serviceEName, serviceFName, serviceGName},
                new ServiceName[] {serviceAName, serviceIName, serviceJName},
                new ServiceName[] {serviceMName, serviceOName});

        serviceLFailure = testListener.expectServiceFailure(serviceLName);
        serviceL.failNextTime();
        serviceLController.setMode(Mode.PASSIVE);
        assertFailure(serviceLController, serviceLFailure);

        cycles = serviceContainer.detectCircularity();
        assertCycles(cycles, new ServiceName[] {serviceAName, serviceBName, serviceCName, serviceDName, serviceEName},
                new ServiceName[] {serviceDName, serviceEName, serviceFName, serviceGName},
                new ServiceName[] {serviceEName, serviceFName, serviceGName},
                new ServiceName[] {serviceAName, serviceIName, serviceJName},
                new ServiceName[] {serviceMName, serviceOName});

        final Future<ServiceController<?>> serviceARemoval = testListener.expectServiceRemoval(serviceAName);
        serviceBMissingDep = testListener.expectDependencyUninstall(serviceBName);
        serviceCMissingDep = testListener.expectDependencyUninstall(serviceCName);
        serviceDMissingDep = testListener.expectDependencyUninstall(serviceDName);
        serviceEMissingDep = testListener.expectDependencyUninstall(serviceEName);
        serviceFMissingDep = testListener.expectDependencyUninstall(serviceFName);
        serviceGMissingDep = testListener.expectDependencyUninstall(serviceGName);
        final Future<ServiceController<?>> serviceIMissingDep = testListener.expectDependencyUninstall(serviceIName);
        final Future<ServiceController<?>> serviceJMissingDep = testListener.expectDependencyUninstall(serviceJName);
        serviceMMissingDep = testListener.expectDependencyUninstall(serviceMName);
        serviceOMissingDep = testListener.expectDependencyUninstall(serviceOName);
        serviceAController.setMode(Mode.REMOVE);
        assertController(serviceAController, serviceARemoval);
        assertController(serviceBController, serviceBMissingDep);
        assertController(serviceCController, serviceCMissingDep);
        assertController(serviceDController, serviceDMissingDep);
        assertController(serviceEController, serviceEMissingDep);
        assertController(serviceFController, serviceFMissingDep);
        assertController(serviceGController, serviceGMissingDep);
        assertController(serviceIController, serviceIMissingDep);
        assertController(serviceJController, serviceJMissingDep);
        assertController(serviceMController, serviceMMissingDep);
        assertController(serviceOController, serviceOMissingDep);

        cycles = serviceContainer.detectCircularity();
        assertCycles(cycles, new ServiceName[] {serviceDName, serviceEName, serviceFName, serviceGName},
                new ServiceName[] {serviceEName, serviceFName, serviceGName},
                new ServiceName[] {serviceMName, serviceOName});
    }

    // cycle involving aliases
    // full scenario:
    // A->B; B->C,E; C->D; E->G; F->I,J
    // whereas B->E and E->G are optional dependencies
    // A has alias D, F has alias G and B has alias H, I, J
    // first services are installed as described above. Then, the services with alias are uninstalled
    // and replaced by services without aliases, thus breaking the cycles.
    // this test will assert that cycles can be recovered, resulting in all services in UP state
    @Test
    public void cycleRecovery() throws Exception {
        // install B, C, E
        Future<ServiceController<?>> serviceBMissingDep = testListener.expectDependencyUninstall(serviceBName);
        Future<ServiceController<?>> serviceCMissingDep = testListener.expectDependencyUninstall(serviceCName);
        Future<ServiceController<?>> serviceEStart = testListener.expectServiceStart(serviceEName);

        serviceContainer.addService(serviceCName, Service.NULL).addDependency(serviceDName).install();
        serviceContainer.addService(serviceEName, Service.NULL).addDependency(DependencyType.OPTIONAL, serviceGName).install();
        serviceContainer.addService(serviceBName, Service.NULL).addAliases(serviceHName, serviceIName, serviceJName)
            .addDependency(serviceCName).addDependency(DependencyType.OPTIONAL, serviceEName).install();

        final ServiceController<?> serviceBController = assertController(serviceBName, serviceBMissingDep);
        final ServiceController<?> serviceCController = assertController(serviceCName, serviceCMissingDep);
        final ServiceController<?> serviceEController = assertController(serviceEName, serviceEStart);

        Collection<List<ServiceName>> cycles = serviceContainer.detectCircularity();
        assertTrue(cycles == null || cycles.isEmpty());

        // install service A
        Future<ServiceController<?>> serviceAListenerAdded = testListener.expectListenerAdded(serviceAName);
        Future<ServiceController<?>> serviceBInstalledDep = testListener.expectDependencyInstall(serviceBName);
        Future<ServiceController<?>> serviceCInstalledDep = testListener.expectDependencyInstall(serviceCName);
        serviceContainer.addService(serviceAName, Service.NULL).addAliases(serviceDName).addDependency(serviceBName).install();
        ServiceController<?> serviceAController = assertController(serviceAName, serviceAListenerAdded);
        assertController(serviceBController, serviceBInstalledDep);
        assertController(serviceCController, serviceCInstalledDep);

        cycles = serviceContainer.detectCircularity();
        assertNotNull(cycles);
        assertEquals(1, cycles.size());
        assertCycle(cycles.iterator().next(), serviceAName, serviceBName, serviceCName);

        // install service F
        final Future<ServiceController<?>> serviceFListenerAdded = testListener.expectListenerAdded(serviceFName);
        serviceContainer.addService(serviceFName, Service.NULL).addAliases(serviceGName).addDependencies(serviceIName, serviceJName).install();
        ServiceController<?> serviceFController = assertController(serviceFName, serviceFListenerAdded);

        cycles = serviceContainer.detectCircularity();
        assertNotNull(cycles);
        assertEquals(1, cycles.size());
        assertCycle(cycles.iterator().next(), serviceAName, serviceBName, serviceCName);

        // stop service E
        final Future<ServiceController<?>> serviceEStop = testListener.expectServiceStop(serviceEName);
        serviceEController.setMode(Mode.NEVER);
        assertController(serviceEController, serviceEStop);
        cycles = serviceContainer.detectCircularity();
        // the cycle B, E, F is found two times, because service F depends twice on service B 
        // (by depending both on I and J)

        assertCycles(cycles, new ServiceName[] {serviceAName, serviceBName, serviceCName},
                new ServiceName[] {serviceBName, serviceEName, serviceFName},
                new ServiceName[] {serviceBName, serviceEName, serviceFName});

        // reactivate E
        serviceEController.setMode(Mode.ACTIVE);
        // serviceE cannot start now that it is connected to its optional dependency G, creating a 
        // circularity in the dependencies
        assertSame(State.DOWN, serviceEController.getState());

        cycles = serviceContainer.detectCircularity();
        assertCycles(cycles, new ServiceName[] {serviceAName, serviceBName, serviceCName},
                new ServiceName[] {serviceBName, serviceEName, serviceFName},
                new ServiceName[] {serviceBName, serviceEName, serviceFName});

        Future<ServiceController<?>> serviceARemoval = testListener.expectServiceRemoval(serviceAName);
        serviceBMissingDep = testListener.expectDependencyUninstall(serviceBName);
        serviceCMissingDep = testListener.expectDependencyUninstall(serviceCName);
        Future<ServiceController<?>> serviceEMissingDep = testListener.expectDependencyUninstall(serviceEName);
        Future<ServiceController<?>> serviceFMissingDep = testListener.expectDependencyUninstall(serviceFName);
        serviceAController.setMode(Mode.REMOVE);
        assertController(serviceAController, serviceARemoval);
        assertController(serviceBName, serviceBMissingDep);
        assertController(serviceCName, serviceCMissingDep);
        assertController(serviceEName, serviceEMissingDep);
        assertController(serviceFName, serviceFMissingDep);

        cycles = serviceContainer.detectCircularity();
        assertCycles(cycles, new ServiceName[] {serviceBName, serviceEName, serviceFName},
                new ServiceName[] {serviceBName, serviceEName, serviceFName});

        // install service D, without aliases
        final FailToStartService serviceD = new FailToStartService(true);
        serviceBInstalledDep = testListener.expectDependencyInstall(serviceBName);
        serviceCInstalledDep = testListener.expectDependencyInstall(serviceCName);
        final Future<ServiceController<?>> serviceEInstalledDep = testListener.expectDependencyInstall(serviceEName);
        final Future<ServiceController<?>> serviceFInstalledDep = testListener.expectDependencyInstall(serviceFName);
        final Future<StartException> serviceDFailure = testListener.expectServiceFailure(serviceDName);
        final Future<ServiceController<?>> serviceBFailedDep = testListener.expectDependencyFailure(serviceBName);
        final Future<ServiceController<?>> serviceCFailedDep = testListener.expectDependencyFailure(serviceCName);
        final Future<ServiceController<?>> serviceEFailedDep = testListener.expectDependencyFailure(serviceEName);
        final Future<ServiceController<?>> serviceFFailedDep = testListener.expectDependencyFailure(serviceFName);
        serviceContainer.addService(serviceDName, serviceD).install();
        assertController(serviceBController, serviceBInstalledDep);
        assertController(serviceCController, serviceCInstalledDep);
        assertController(serviceEController, serviceEInstalledDep);
        assertController(serviceFController, serviceFInstalledDep);
        final ServiceController<?> serviceDController = assertFailure(serviceDName, serviceDFailure);
        assertController(serviceBController, serviceBFailedDep);
        assertController(serviceCController, serviceCFailedDep);
        assertController(serviceEController, serviceEFailedDep);
        assertController(serviceFController, serviceFFailedDep);

        cycles = serviceContainer.detectCircularity();
        assertCycles(cycles, new ServiceName[] {serviceBName, serviceEName, serviceFName},
                new ServiceName[] {serviceBName, serviceEName, serviceFName});

        final Future<ServiceController<?>> serviceBClearedDepFailure = testListener.expectDependencyFailureCleared(serviceBName);
        final Future<ServiceController<?>> serviceCClearedDepFailure = testListener.expectDependencyFailureCleared(serviceCName);
        final Future<ServiceController<?>> serviceEClearedDepFailure = testListener.expectDependencyFailureCleared(serviceEName);
        final Future<ServiceController<?>> serviceFClearedDepFailure = testListener.expectDependencyFailureCleared(serviceFName);
        serviceDController.setMode(Mode.NEVER);
        assertController(serviceBController, serviceBClearedDepFailure);
        assertController(serviceCController, serviceCClearedDepFailure);
        assertController(serviceEController, serviceEClearedDepFailure);
        assertController(serviceFController, serviceFClearedDepFailure);

        cycles = serviceContainer.detectCircularity();
        assertCycles(cycles, new ServiceName[] {serviceBName, serviceEName, serviceFName},
                new ServiceName[] {serviceBName, serviceEName, serviceFName});

        final Future<ServiceController<?>> serviceDStart = testListener.expectServiceStart(serviceDName);
        final Future<ServiceController<?>> serviceCStart = testListener.expectServiceStart(serviceCName);
        serviceDController.setMode(Mode.ACTIVE);
        assertController(serviceDController, serviceDStart);
        assertController(serviceCController, serviceCStart);

        cycles = serviceContainer.detectCircularity();
        assertCycles(cycles, new ServiceName[] {serviceBName, serviceEName, serviceFName},
                new ServiceName[] {serviceBName, serviceEName, serviceFName});


        final Future<ServiceController<?>> serviceBRemoval = testListener.expectServiceRemoval(serviceBName);
        serviceFMissingDep = testListener.expectDependencyUninstall(serviceFName);
        serviceEMissingDep = testListener.expectDependencyUninstall(serviceEName);
        serviceBController.setMode(Mode.REMOVE);
        assertController(serviceBController, serviceBRemoval);
        assertController(serviceFController, serviceFMissingDep);
        assertController(serviceEController, serviceEMissingDep);

        cycles = serviceContainer.detectCircularity();
        assertTrue(cycles == null || cycles.isEmpty());

        // install services I and J
        serviceEStart = testListener.expectServiceStart(serviceEName);
        final Future<ServiceController<?>> serviceFStart = testListener.expectServiceStart(serviceFName);
        final Future<ServiceController<?>> serviceIStart = testListener.expectServiceStart(serviceIName);
        final Future<ServiceController<?>> serviceJStart = testListener.expectServiceStart(serviceJName);

        serviceContainer.addService(serviceIName, Service.NULL).addDependency(serviceJName).install();
        serviceContainer.addService(serviceJName, Service.NULL).install();

        assertController(serviceEController, serviceEStart);
        assertController(serviceFController, serviceFStart);
        assertController(serviceIName, serviceIStart);
        assertController(serviceJName, serviceJStart);
        // services C and D else remain in UP state
        assertSame(State.UP, serviceCController.getState());
        assertSame(State.UP, serviceDController.getState());

        cycles = serviceContainer.detectCircularity();
        assertTrue(cycles == null || cycles.isEmpty());
    }

    private final void assertCycle(List<ServiceName> cycle, ServiceName... expectedCycle) {
        String matchError = matchCycle(cycle, expectedCycle);
        if (matchError != null) {
            fail(matchError);
        }
    }

    private final String matchCycle(List<ServiceName> cycle, ServiceName... expectedCycle) {
        assertNotNull("Cycle is null", cycle);
        Iterator<ServiceName> cycleIterator = cycle.iterator();
        assertTrue("Cycle is empty", cycleIterator.hasNext());
        ServiceName firstServiceName = cycleIterator.next();
        int cycleStart = -1;
        for (int i = 0; i < expectedCycle.length; i++) {
            if (firstServiceName.equals(expectedCycle[i])) {
                cycleStart = i;
                break;
            }
        }
        if (cycleStart == -1) {
            return "First element of cycle unexpected: "+ firstServiceName;
        }
        for (int i = cycleStart + 1; i < expectedCycle.length; i++) {
            assertTrue("Cycle is shorter than expected", cycleIterator.hasNext());
            ServiceName serviceName = cycleIterator.next();
            if (!serviceName.equals(expectedCycle[i])) {
                return "Unexpected serviceName in cycle: " + serviceName;
            }
        }
        for (int i = 0; i <= cycleStart; i++) {
            assertTrue("Cycle is shorter than expected", cycleIterator.hasNext());
            ServiceName serviceName = cycleIterator.next();
            if (!serviceName.equals(expectedCycle[i])) {
                return "Unexpected serviceName in cycle: " + serviceName;
            }
        }
        assertFalse("Cycle is longer than expected", cycleIterator.hasNext());
        return null;
    }

    private void assertCycles(Collection<List<ServiceName>> cycles, ServiceName[]... expectedCycles) {
        assertNotNull(cycles);
        assertEquals(expectedCycles.length, cycles.size());
        boolean[] matchedCycles = new boolean[expectedCycles.length];
        for (List<ServiceName> cycle: cycles) {
            for (int i = 0; i < expectedCycles.length; i++) {
                ServiceName[] expectedCycle = expectedCycles[i];
                if (!matchedCycles[i] && expectedCycle.length == cycle.size() - 1 &&
                        matchCycle(cycle, expectedCycle) == null) {
                    matchedCycles[i] = true;
                    break;
                }
            }
        }
        for (int i = 0; i < matchedCycles.length; i++) {
            assertTrue("Cycle with index " + i + " did not find a match", matchedCycles[i]);
        }
    }
}
