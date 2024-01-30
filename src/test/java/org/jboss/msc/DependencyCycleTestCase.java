/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2021, Red Hat, Inc., and individual contributors
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

package org.jboss.msc;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import org.jboss.msc.service.CircularDependencyException;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.junit.Before;
import org.junit.Test;

import java.util.function.Consumer;

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
    TestLifecycleListener testListener;

    @Before
    public void initializeTestListener() {
        testListener = new TestLifecycleListener();
        serviceContainer.addListener(testListener);
    }

    @Test
    public void simpleCycle() throws Exception {
        ServiceBuilder<?> sb = serviceContainer.addService();
        Consumer<String> providedValue = sb.provides(serviceAName);
        sb.setInstance(Service.newInstance(providedValue, serviceAName.toString()));
        sb.requires(serviceBName);
        sb.install();

        sb = serviceContainer.addService();
        providedValue = sb.provides(serviceBName);
        sb.setInstance(Service.newInstance(providedValue, serviceBName.toString()));
        sb.requires(serviceCName);
        sb.install();

        try {
            sb = serviceContainer.addService();
            providedValue = sb.provides(serviceCName);
            sb.setInstance(Service.newInstance(providedValue, serviceCName.toString()));
            sb.requires(serviceAName);
            sb.install();
            fail ("CircularDependencyException expected");
        } catch (CircularDependencyException e) {
            assertCycle(e, new ServiceName[]{serviceAName, serviceBName, serviceCName});
        }

        serviceContainer.awaitStability();

        assertEquals(2, testListener.downValues().size());
        assertTrue(testListener.downValues().contains(serviceAName));
        assertTrue(testListener.downValues().contains(serviceBName));
    }

    // full scenario:
    // A->B,F; B->C; C->D; D->E; E->C; F->G; G->H; H->I,W; I->H,J; J->K; K->G,H;
    // L->M; M->N; N->H,O; O->L;
    // P->Q; Q->R; R->S; S->T; T->P
    // U->V (no cycle here)
    @Test
    public void multipleCycles() throws Exception {
        // first install A, B, C, D, L, M, O
        ServiceBuilder<?> sb = serviceContainer.addService();
        Consumer<String> providedValue = sb.provides(serviceAName);
        sb.setInstance(Service.newInstance(providedValue, serviceAName.toString()));
        sb.requires(serviceBName);
        sb.requires(serviceFName);
        final ServiceController<?> serviceAController = sb.install();

        sb = serviceContainer.addService();
        providedValue = sb.provides(serviceBName);
        sb.setInstance(Service.newInstance(providedValue, serviceBName.toString()));
        sb.requires(serviceCName);
        final ServiceController<?> serviceBController = sb.install();

        sb = serviceContainer.addService();
        providedValue = sb.provides(serviceCName);
        sb.setInstance(Service.newInstance(providedValue, serviceCName.toString()));
        sb.requires(serviceDName);
        final ServiceController<?> serviceCController = sb.install();

        sb = serviceContainer.addService();
        providedValue = sb.provides(serviceDName);
        sb.setInstance(Service.newInstance(providedValue, serviceDName.toString()));
        sb.requires(serviceEName);
        final ServiceController<?> serviceDController = sb.install();

        sb = serviceContainer.addService();
        providedValue = sb.provides(serviceLName);
        sb.setInstance(Service.newInstance(providedValue, serviceLName.toString()));
        sb.requires(serviceMName);
        final ServiceController<?> serviceLController = sb.install();

        sb = serviceContainer.addService();
        providedValue = sb.provides(serviceMName);
        sb.setInstance(Service.newInstance(providedValue, serviceMName.toString()));
        sb.requires(serviceNName);
        final ServiceController<?> serviceMController = sb.install();

        sb = serviceContainer.addService();
        providedValue = sb.provides(serviceOName);
        sb.setInstance(Service.newInstance(providedValue, serviceOName.toString()));
        sb.requires(serviceLName);
        final ServiceController<?> serviceOController = sb.install();

        // install service N
        try {
            sb = serviceContainer.addService();
            providedValue = sb.provides(serviceNName);
            sb.setInstance(Service.newInstance(providedValue, serviceNName.toString()));
            sb.requires(serviceHName);
            sb.requires(serviceOName);
            sb.install();
            fail ("CircularDependencyException expected");
        } catch (CircularDependencyException e) {
            assertCycle(e, new ServiceName[] {serviceOName, serviceLName, serviceMName, serviceNName});
        }

        // install E, F, G, H, I, V
        try {
            sb = serviceContainer.addService();
            providedValue = sb.provides(serviceEName);
            sb.setInstance(Service.newInstance(providedValue, serviceEName.toString()));
            sb.requires(serviceCName);
            sb.install();
            fail("CircularDependencyException expected");
        } catch (CircularDependencyException e) {
            assertCycle(e, new ServiceName[]{serviceCName, serviceDName, serviceEName});
        }

        sb = serviceContainer.addService();
        providedValue = sb.provides(serviceFName);
        sb.setInstance(Service.newInstance(providedValue, serviceFName.toString()));
        sb.requires(serviceGName);
        final ServiceController<?> serviceFController = sb.install();

        sb = serviceContainer.addService();
        providedValue = sb.provides(serviceGName);
        sb.setInstance(Service.newInstance(providedValue, serviceGName.toString()));
        sb.requires(serviceHName);
        final ServiceController<?> serviceGController = sb.install();

        sb = serviceContainer.addService();
        providedValue = sb.provides(serviceHName);
        sb.setInstance(Service.newInstance(providedValue, serviceHName.toString()));
        sb.requires(serviceIName);
        sb.requires(serviceWName);
        final ServiceController<?> serviceHController = sb.install();

        try {
            sb = serviceContainer.addService();
            providedValue = sb.provides(serviceIName);
            sb.setInstance(Service.newInstance(providedValue, serviceIName.toString()));
            sb.requires(serviceHName);
            sb.requires(serviceJName);
            sb.install();
            fail("CirculardependencyException expected");
        } catch (CircularDependencyException e) {
            assertCycle(e, new ServiceName[]{serviceHName, serviceIName});
        }

        sb = serviceContainer.addService();
        providedValue = sb.provides(serviceVName);
        sb.setInstance(Service.newInstance(providedValue, serviceVName.toString()));
        sb.install();

        serviceContainer.awaitStability();

        assertTrue(testListener.downValues().contains(serviceAName));
        assertTrue(testListener.downValues().contains(serviceBName));
        assertTrue(testListener.downValues().contains(serviceCName));
        assertTrue(testListener.downValues().contains(serviceDName));
        assertTrue(testListener.downValues().contains(serviceFName));
        assertTrue(testListener.upValues().contains(serviceVName));

        // install J, P, Q, R, S, T, U
        sb = serviceContainer.addService();
        providedValue = sb.provides(serviceJName);
        sb.setInstance(Service.newInstance(providedValue, serviceJName.toString()));
        sb.requires(serviceKName);
        final ServiceController<?> serviceJController = sb.install();

        sb = serviceContainer.addService();
        providedValue = sb.provides(servicePName);
        sb.setInstance(Service.newInstance(providedValue, servicePName.toString()));
        sb.requires(serviceQName);
        sb.install();

        sb = serviceContainer.addService();
        providedValue = sb.provides(serviceQName);
        sb.setInstance(Service.newInstance(providedValue, serviceQName.toString()));
        sb.requires(serviceRName);
        sb.install();

        sb = serviceContainer.addService();
        providedValue = sb.provides(serviceRName);
        sb.setInstance(Service.newInstance(providedValue, serviceRName.toString()));
        sb.requires(serviceSName);
        sb.install();

        sb = serviceContainer.addService();
        providedValue = sb.provides(serviceSName);
        sb.setInstance(Service.newInstance(providedValue, serviceSName.toString()));
        sb.requires(serviceTName);
        sb.install();

        try {
            sb = serviceContainer.addService();
            providedValue = sb.provides(serviceTName);
            sb.setInstance(Service.newInstance(providedValue, serviceTName.toString()));
            sb.requires(servicePName);
            sb.install();
            fail("CircularDependencyException expected");
        } catch (CircularDependencyException e) {
            assertCycle(e, new ServiceName[]{servicePName, serviceQName, serviceRName, serviceSName, serviceTName});
        }

        sb = serviceContainer.addService();
        providedValue = sb.provides(serviceUName);
        sb.setInstance(Service.newInstance(providedValue, serviceUName.toString()));
        sb.requires(serviceVName);
        sb.install();

        serviceContainer.awaitStability();

        assertTrue(testListener.downValues().contains(servicePName));
        assertTrue(testListener.downValues().contains(serviceQName));
        assertTrue(testListener.downValues().contains(serviceRName));
        assertTrue(testListener.downValues().contains(serviceSName));
        assertTrue(testListener.upValues().contains(serviceUName));

        // install service K
        sb = serviceContainer.addService();
        providedValue = sb.provides(serviceKName);
        sb.setInstance(Service.newInstance(providedValue, serviceKName.toString()));
        sb.requires(serviceGName);
        sb.requires(serviceHName);
        sb.install();

        // install service W
        sb = serviceContainer.addService();
        providedValue = sb.provides(serviceWName);
        sb.setInstance(Service.newInstance(providedValue, serviceWName.toString()));
        sb.install();

        serviceContainer.awaitStability();

        assertTrue(testListener.downValues().contains(serviceAName));
        assertTrue(testListener.downValues().contains(serviceFName));
        assertTrue(testListener.downValues().contains(serviceGName));
        assertTrue(testListener.downValues().contains(serviceHName));
        assertTrue(testListener.downValues().contains(serviceJName));
        assertTrue(testListener.downValues().contains(serviceKName));
        assertTrue(testListener.downValues().contains(serviceLName));
        assertTrue(testListener.downValues().contains(serviceMName));
        assertTrue(testListener.downValues().contains(serviceOName));
        assertTrue(testListener.upValues().contains(serviceWName));
    }

    private void assertCycle(CircularDependencyException e, ServiceName[]... cycles) {
        ServiceName[] actualCycle = e.getCycle();
        assertNotNull(actualCycle);
        for (int i = 0; i < cycles.length; i++) {
            if (actualCycle.length == cycles[i].length) {
                for (int j = 0; j < cycles[i].length; j++) {
                    if(!cycles[i][j].equals(actualCycle[j])) {
                        break;
                    }
                }
                return;
            }
        }
        StringBuffer buffer = new StringBuffer();
        buffer.append("Actual cycle is different from expected: ");
        buffer.append(actualCycle[0]);
        for (int i = 1; i < actualCycle.length; i++) {
            buffer.append(", ").append(actualCycle[i]);
        }
        fail(buffer.toString());
    }
}
