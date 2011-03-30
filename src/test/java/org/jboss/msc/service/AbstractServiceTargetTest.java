/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Future;

import org.jboss.msc.service.ServiceController.Mode;
import org.jboss.msc.service.ServiceController.State;
import org.jboss.msc.util.TestServiceListener;
import org.jboss.msc.value.Values;
import org.junit.Before;
import org.junit.Test;

/**
 * Test for {@code ServiceTarget} implementations. 
 * 
 * @author <a href="mailto:flavia.rainone@jboss.com">Flavia Rainone</a>
 */
public abstract class AbstractServiceTargetTest extends AbstractServiceTest {

    protected static final ServiceName serviceName = ServiceName.of("service", "name");
    protected static final ServiceName anotherServiceName = ServiceName.of("service", "another", "name");
    protected static final ServiceName oneMoreServiceName = ServiceName.of("service", "one", "more", "name");
    protected static final ServiceName extraServiceName = ServiceName.of("service", "extra", "name");
    protected static final TestServiceListener testListener = new TestServiceListener();

    protected ServiceTarget serviceTarget;

    @Before
    public void initializeServiceTarget() throws Exception {
        serviceTarget = getServiceTarget(serviceContainer);
    }

    @Test
    public void addService() throws Exception {
        Future<ServiceController<?>> serviceStart = testListener.expectServiceStart(serviceName);
        // adding and removing listener is ok
        serviceTarget.addListener(testListener);
        serviceTarget.removeListener(testListener);
        serviceTarget.addListener(testListener);
        // adding null will be ignored
        serviceTarget.addListener((ServiceListener<Object>) null);
        ServiceController<?> serviceController = serviceTarget.addService(serviceName, Service.NULL).install();
        assertController(serviceName, serviceController);
        assertController(serviceController, serviceStart);
        // removing null will be ignored
        serviceTarget.removeListener(null);
    }

    @Test
    public void addServiceValue() throws Exception {
        Future<ServiceController<?>> serviceStart = testListener.expectServiceStart(serviceName);
        List<ServiceListener<Object>> listeners = new ArrayList<ServiceListener<Object>>();
        listeners.add(testListener);
        serviceTarget.addListener(listeners);
        ServiceController<?> serviceController = serviceTarget.addServiceValue(serviceName, Values.immediateValue(Service.NULL)).install();
        assertController(serviceName, serviceController);
        assertController(serviceController, serviceStart);
    }

    @Test
    public void addNullServiceValue() throws Exception {
        try {
            serviceTarget.addServiceValue(serviceName, null);
            fail ("IllegalArgumentException expected");
        } catch (IllegalArgumentException e) {}
    }

    @Test
    public void addServiceWithDependency() throws Exception {
        Future<ServiceController<?>> serviceStart = testListener.expectServiceStart(serviceName);
        Future<ServiceController<?>> anotherServiceStart = testListener.expectServiceStart(anotherServiceName);

        ServiceTarget serviceTarget = getServiceTarget(serviceContainer);
        serviceTarget.addListener(testListener);
        serviceTarget.addService(anotherServiceName, Service.NULL).install();
        serviceTarget.addDependency(anotherServiceName);
        serviceTarget.addDependency((ServiceName) null);// null dependency should be ignored
        serviceTarget.addServiceValue(serviceName, Values.immediateValue(Service.NULL)).install();

        assertController(serviceName, serviceStart);
        assertController(anotherServiceName, anotherServiceStart);
    }

    @Test
    public void addServiceWithDependencies() throws Exception {
        ServiceTarget serviceTarget = getServiceTarget(serviceContainer);

        Future<ServiceController<?>> oneMoreServiceStart = testListener.expectServiceStart(oneMoreServiceName);
        Future<ServiceController<?>> anotherServiceStart = testListener.expectServiceStart(anotherServiceName);
        Future<ServiceController<?>> serviceStart = testListener.expectServiceStart(serviceName);
        Future<ServiceController<?>> extraServiceStart = testListener.expectServiceStart(extraServiceName);

        ServiceTarget subTarget = getServiceTarget(serviceTarget.subTarget());
        serviceTarget.addListener(testListener);
        serviceTarget.addService(oneMoreServiceName, Service.NULL).install();
        serviceTarget.addService(anotherServiceName, Service.NULL).install();
        List<ServiceName> dependencies = new ArrayList<ServiceName>();
        dependencies.add(anotherServiceName);
        dependencies.add(oneMoreServiceName);
        subTarget.addDependency(dependencies);
        subTarget.addServiceValue(serviceName, Values.immediateValue(Service.NULL)).install();
        subTarget.addServiceValue(extraServiceName, Values.immediateValue(Service.NULL)).install();

        assertController(oneMoreServiceName, oneMoreServiceStart);
        assertController(anotherServiceName, anotherServiceStart);
        assertController(serviceName, serviceStart);
        assertController(extraServiceName, extraServiceStart);
    }

    @Test
    public void addRemoveDependency() throws Exception {
        // container target has a dependency on anotherSerivce
        final ServiceTarget containerTarget = getServiceTarget(serviceContainer);
        containerTarget.addDependency(anotherServiceName);
        // install service; it is expected to complain of the missing anotherService dependency
        Future<ServiceController<?>> missingDependency = testListener.expectImmediateDependencyUninstall(serviceName);
        containerTarget.addService(serviceName, Service.NULL).addListener(testListener).install();
        ServiceController<?> serviceController = assertController(serviceName, missingDependency);
        // remove service
        Future<ServiceController<?>> serviceRemoval = testListener.expectServiceRemoval(serviceName);
        serviceController.setMode(Mode.REMOVE);
        assertController(serviceController, serviceRemoval);
        // removing null dependency should be ignored
        containerTarget.removeDependency(null);
        // remove the dependency on anotherService from the container
        containerTarget.removeDependency(anotherServiceName);
        // install service; this time, it is expected to start normally as it does not depend on anotherService
        Future<ServiceController<?>> serviceStart = testListener.expectServiceStart(serviceName);
        containerTarget.addService(serviceName, Service.NULL).addListener(testListener).install();
        serviceController = assertController(serviceName, serviceStart);
        // remove service
        serviceRemoval = testListener.expectServiceRemoval(serviceName);
        serviceController.setMode(Mode.REMOVE);
        assertController(serviceController, serviceRemoval);
        // add dependencies on anotherService and extraService
        containerTarget.addDependency(anotherServiceName, extraServiceName);
        // install service; it should complain of the missing another/extraService dependencies
        missingDependency = testListener.expectImmediateDependencyUninstall(serviceName);
        containerTarget.addService(serviceName, Service.NULL).addListener(testListener).install();
        serviceController = assertController(serviceName, missingDependency);
        // remove the extraService dependency from container
        containerTarget.removeDependency(extraServiceName);
        // install anotherService
        final Future<ServiceController<?>> anotherServiceStart = testListener.expectServiceStart(anotherServiceName);
        containerTarget.addService(anotherServiceName, Service.NULL).addListener(testListener).install();
        assertController(anotherServiceName, anotherServiceStart);
        // service should still be down, as the removal of extraService as a dependency from the container...
        // ... didn't affect service
        assertSame(State.DOWN, serviceController.getState());
        // install extra service
        final Future<ServiceController<?>> extraServiceStart = testListener.expectServiceStart(extraServiceName);
        serviceStart = testListener.expectServiceStart(serviceName);
        containerTarget.addService(extraServiceName, Service.NULL).addListener(testListener).install();
        assertController(extraServiceName, extraServiceStart);
        // this time, service is expected to start
        assertController(serviceController, serviceStart);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void addServicesAfterShutdown() {
        final ServiceTarget containerTarget = getServiceTarget(serviceContainer);
        final ServiceBuilder<?> builderFromContainer = containerTarget.addService(anotherServiceName, Service.NULL);
        final TestServiceListener testListener = new TestServiceListener();

        containerTarget.addListener(testListener);
        containerTarget.addDependency(serviceName);
        shutdownContainer();

        // should be ok
        containerTarget.addDependency(new ArrayList<ServiceName>());
        containerTarget.addDependency(oneMoreServiceName);
        containerTarget.addDependency(serviceName, anotherServiceName);
        containerTarget.addListener(new ArrayList<ServiceListener<Object>>());
        containerTarget.addListener(new TestServiceListener());
        containerTarget.addListener(new TestServiceListener(), new TestServiceListener());
        containerTarget.addService(extraServiceName, Service.NULL);

        Set<ServiceListener<Object>> listeners = containerTarget.getListeners();
        assertNotNull(listeners);
        assertEquals(4, listeners.size());
        // should contain testListener plus the three previously created listeners
        assertTrue(listeners.contains(testListener));

        Set<ServiceName> dependencies = containerTarget.getDependencies();
        assertNotNull(dependencies);
        assertEquals(3, dependencies.size());
        assertTrue(dependencies.contains(serviceName));
        assertTrue(dependencies.contains(oneMoreServiceName));
        assertTrue(dependencies.contains(anotherServiceName));

        try {
            builderFromContainer.install();
            fail ("IllegalStateException expected");
        } catch (IllegalStateException e) {}
    }

    @Test
    public void batchServiceTarget() throws Exception {
        final ServiceTarget containerTarget = getServiceTarget(serviceContainer);
        final BatchServiceTarget batchTarget = containerTarget.batchTarget();
        // add testListener to containerTarget
        containerTarget.addListener(testListener);

        // install anotherService into containerTarget
        final Future<ServiceController<?>> anotherServiceStart = testListener.expectServiceStart(anotherServiceName);
        final ServiceController<?> anotherController = containerTarget.addService(anotherServiceName, Service.NULL)
            .install();
        assertController(anotherServiceName, anotherController);
        assertController(anotherController, anotherServiceStart);

        // install oneMoreService into batchTarget
        final Future<ServiceController<?>> oneServiceStart = testListener.expectServiceStart(oneMoreServiceName);
        final ServiceController<?> oneController = batchTarget.addService(oneMoreServiceName, Service.NULL).install();
        assertController(oneMoreServiceName, oneController);
        assertController(oneController, oneServiceStart);

        // calling batchTarget.removeServices should remove oneMoreService only
        Future<ServiceController<?>> oneServiceRemoval = testListener.expectServiceRemoval(oneMoreServiceName);
        batchTarget.removeServices();
        assertController(oneController, oneServiceRemoval);
        // anotherService should continue in the UP state
        assertSame(State.UP, anotherController.getState());

        // there should be no effect on this call, as no new services have been added
        batchTarget.removeServices(); 
    }

    @SuppressWarnings("unchecked")
    @Test
    public void batchServiceTargetWithDependencyAndListeners() throws Exception {
        final ServiceTarget containerTarget = getServiceTarget(serviceContainer);
        final BatchServiceTarget batchTarget = containerTarget.batchTarget();
        // this call should take no effect, as no new services have been added so far
        batchTarget.removeServices(); 

        // add duplicate testListener to containerTaget... it should be ignored
        containerTarget.addListener(testListener, testListener);
        // and 3 testListeners to batchTarget
        final TestServiceListener testListener1 = new TestServiceListener();
        final TestServiceListener testListener2 = new TestServiceListener();
        final TestServiceListener testListener3 = new TestServiceListener();
        batchTarget.addListener(testListener1, testListener2, testListener3);
        // plus one dependency on oneMoreServiceName
        batchTarget.addDependency(oneMoreServiceName);
        // containerTarget shouldn't have oneMoreServiceName as dependency
        assertTrue(containerTarget.getDependencies() == null || containerTarget.getDependencies().isEmpty());
        // and four listeners: testListener, testListener1, testListener2, and testListener3
        Set<ServiceListener<Object>> listeners = containerTarget.getListeners();
        assertEquals(1, listeners.size());
        assertSame(testListener, listeners.iterator().next());

        // install oneMoreService on batchTarget
        final Future<ServiceController<?>> oneServiceStart = testListener.expectServiceStart(oneMoreServiceName);
        final Future<ServiceController<?>> oneServiceStart1 = testListener1.expectServiceStart(oneMoreServiceName);
        final Future<ServiceController<?>> oneServiceStart2 = testListener2.expectServiceStart(oneMoreServiceName);
        final Future<ServiceController<?>> oneServiceStart3 = testListener3.expectServiceStart(oneMoreServiceName);
        final ServiceController<?> oneController = batchTarget.addService(oneMoreServiceName, Service.NULL).install();
        assertController(oneMoreServiceName, oneController);
        assertController(oneController, oneServiceStart);
        assertController(oneController, oneServiceStart1);
        assertController(oneController, oneServiceStart2);
        assertController(oneController, oneServiceStart3);

        // install anotherService on batchTarget
        final Future<ServiceController<?>> anotherServiceStart = testListener.expectServiceStart(anotherServiceName);
        final Future<ServiceController<?>> anotherServiceStart1 = testListener1.expectServiceStart(anotherServiceName);
        final Future<ServiceController<?>> anotherServiceStart2 = testListener2.expectServiceStart(anotherServiceName);
        final Future<ServiceController<?>> anotherServiceStart3 = testListener3.expectServiceStart(anotherServiceName);
        final ServiceController<?> anotherController = batchTarget.addService(anotherServiceName, Service.NULL)
            .install();
        assertController(anotherServiceName, anotherController);
        assertController(anotherController, anotherServiceStart);
        assertController(anotherController, anotherServiceStart1);
        assertController(anotherController, anotherServiceStart2);
        assertController(anotherController, anotherServiceStart3);

        // install extraService on batchTarget
        Future<ServiceController<?>> extraServiceStart = testListener.expectServiceStart(extraServiceName);
        final Future<ServiceController<?>> extraServiceStart1 = testListener1.expectServiceStart(extraServiceName);
        final Future<ServiceController<?>> extraServiceStart2 = testListener2.expectServiceStart(extraServiceName);
        final Future<ServiceController<?>> extraServiceStart3 = testListener3.expectServiceStart(extraServiceName);
        ServiceController<?> extraController = batchTarget.addService(extraServiceName, Service.NULL).install();
        assertController(extraServiceName, extraController);
        assertController(extraController, extraServiceStart);
        assertController(extraController, extraServiceStart1);
        assertController(extraController, extraServiceStart2);
        assertController(extraController, extraServiceStart3);

        // removing anotherService is ok
        final Future<ServiceController<?>> anotherServiceRemoval = testListener.expectServiceRemoval(anotherServiceName);
        final Future<ServiceController<?>> anotherServiceRemoval1 = testListener1.expectServiceRemoval(anotherServiceName);
        final Future<ServiceController<?>> anotherServiceRemoval2 = testListener2.expectServiceRemoval(anotherServiceName);
        final Future<ServiceController<?>> anotherServiceRemoval3 = testListener3.expectServiceRemoval(anotherServiceName);
        anotherController.setMode(Mode.REMOVE);
        assertController(anotherController, anotherServiceRemoval);
        assertController(anotherController, anotherServiceRemoval1);
        assertController(anotherController, anotherServiceRemoval2);
        assertController(anotherController, anotherServiceRemoval3);

        // call batchTarget.removeServices: only oneService and extraServices will be removed this time
        final Future<ServiceController<?>> oneServiceRemoval = testListener.expectServiceRemoval(oneMoreServiceName);
        final Future<ServiceController<?>> oneServiceRemoval1 = testListener1.expectServiceRemoval(oneMoreServiceName);
        final Future<ServiceController<?>> oneServiceRemoval2 = testListener2.expectServiceRemoval(oneMoreServiceName);
        final Future<ServiceController<?>> oneServiceRemoval3 = testListener3.expectServiceRemoval(oneMoreServiceName);
        Future<ServiceController<?>> extraServiceRemoval = testListener.expectServiceRemoval(extraServiceName);
        Future<ServiceController<?>> extraServiceRemoval1 = testListener1.expectServiceRemoval(extraServiceName);
        Future<ServiceController<?>> extraServiceRemoval2 = testListener2.expectServiceRemoval(extraServiceName);
        Future<ServiceController<?>> extraServiceRemoval3 = testListener3.expectServiceRemoval(extraServiceName);
        batchTarget.removeServices();
        assertController(oneController, oneServiceRemoval);
        assertController(oneController, oneServiceRemoval1);
        assertController(oneController, oneServiceRemoval2);
        assertController(oneController, oneServiceRemoval3);
        assertController(extraController, extraServiceRemoval);
        assertController(extraController, extraServiceRemoval1);
        assertController(extraController, extraServiceRemoval2);
        assertController(extraController, extraServiceRemoval3);

        // installing extraService is not successful because there is a missing dependency on oneMoreService
        Future<ServiceController<?>> extraServiceDepMissing = testListener.expectImmediateDependencyUninstall(extraServiceName);
        Future<ServiceController<?>> extraServiceDepMissing1 = testListener1.expectImmediateDependencyUninstall(extraServiceName);
        Future<ServiceController<?>> extraServiceDepMissing2 = testListener2.expectImmediateDependencyUninstall(extraServiceName);
        Future<ServiceController<?>> extraServiceDepMissing3 = testListener3.expectImmediateDependencyUninstall(extraServiceName);
        extraController = batchTarget.addService(extraServiceName, Service.NULL).install();
        assertController(extraServiceName, extraController);
        assertController(extraController, extraServiceDepMissing);
        assertController(extraController, extraServiceDepMissing1);
        assertController(extraController, extraServiceDepMissing2);
        assertController(extraController, extraServiceDepMissing3);

        // extraService should be removed on invoking batchTarget.removeServices()
        extraServiceRemoval = testListener.expectServiceRemoval(extraServiceName);
        extraServiceRemoval1 = testListener1.expectServiceRemoval(extraServiceName);
        extraServiceRemoval2 = testListener2.expectServiceRemoval(extraServiceName);
        extraServiceRemoval3 = testListener3.expectServiceRemoval(extraServiceName);
        batchTarget.removeServices();
        assertController(extraController, extraServiceRemoval);
        assertController(extraController, extraServiceRemoval1);
        assertController(extraController, extraServiceRemoval2);
        assertController(extraController, extraServiceRemoval3);

        // installing extraService on containerTarget is also not successful because there is the
        // dep on oneMoreService in containerTarget
        extraServiceDepMissing = testListener.expectImmediateDependencyUninstall(extraServiceName);
        extraServiceDepMissing1 = testListener1.expectImmediateDependencyUninstall(extraServiceName);
        extraServiceDepMissing2 = testListener2.expectImmediateDependencyUninstall(extraServiceName);
        extraServiceDepMissing3 = testListener3.expectImmediateDependencyUninstall(extraServiceName);
        extraController = batchTarget.addService(extraServiceName, Service.NULL).install();
        assertController(extraServiceName, extraController);
        assertController(extraController, extraServiceDepMissing);
        assertController(extraController, extraServiceDepMissing1);
        assertController(extraController, extraServiceDepMissing2);
        assertController(extraController, extraServiceDepMissing3);

        // there should be no effect on this call, as no new services have been added
        batchTarget.removeServices(); 
    }

    @SuppressWarnings("unchecked")
    @Test
    public void batchSubServiceTargetWithDependencyAndListeners() throws Exception {
        final ServiceTarget containerTarget = getServiceTarget(serviceContainer);
        final BatchServiceTarget batchTarget = containerTarget.batchTarget();
        final ServiceTarget batchSubTarget = batchTarget.subTarget();
        // this call should take no effect, as no new services have been added so far
        batchTarget.removeServices(); 

        // add testListener to containerTaget
        containerTarget.addListener(testListener);
        // and 3 testListeners to batchTarget
        final TestServiceListener testListener1 = new TestServiceListener();
        final TestServiceListener testListener2 = new TestServiceListener();
        final TestServiceListener testListener3 = new TestServiceListener();
        batchSubTarget.addListener(testListener1, testListener2, testListener3);
        // plus one dependency on oneMoreServiceName
        batchSubTarget.addDependency(oneMoreServiceName, extraServiceName);
        batchSubTarget.removeDependency(extraServiceName);
        // containerTarget should have no dependencies
        assertTrue(containerTarget.getDependencies() == null || containerTarget.getDependencies().isEmpty());
        assertTrue(batchTarget.getDependencies() == null || batchTarget.getDependencies().isEmpty());
        // and one listener: testListener
        Set<ServiceListener<Object>> listeners = containerTarget.getListeners();
        assertEquals(1, listeners.size());
        assertSame(testListener, listeners.iterator().next());
        assertTrue(batchTarget.getListeners() == null || batchTarget.getListeners().isEmpty());

        // install oneMoreService on batchTarget
        final Future<ServiceController<?>> oneServiceStart = testListener.expectServiceStart(oneMoreServiceName);
        final Future<ServiceController<?>> oneServiceStart1 = testListener1.expectServiceStart(oneMoreServiceName);
        final Future<ServiceController<?>> oneServiceStart2 = testListener2.expectServiceStart(oneMoreServiceName);
        final Future<ServiceController<?>> oneServiceStart3 = testListener3.expectServiceStart(oneMoreServiceName);
        final ServiceController<?> oneController = batchSubTarget.addService(oneMoreServiceName, Service.NULL).install();
        assertController(oneMoreServiceName, oneController);
        assertController(oneController, oneServiceStart);
        assertController(oneController, oneServiceStart1);
        assertController(oneController, oneServiceStart2);
        assertController(oneController, oneServiceStart3);

        // install anotherService on batchTarget
        final Future<ServiceController<?>> anotherServiceStart = testListener.expectServiceStart(anotherServiceName);
        final Future<ServiceController<?>> anotherServiceStart1 = testListener1.expectServiceStart(anotherServiceName);
        final Future<ServiceController<?>> anotherServiceStart2 = testListener2.expectServiceStart(anotherServiceName);
        final Future<ServiceController<?>> anotherServiceStart3 = testListener3.expectServiceStart(anotherServiceName);
        final ServiceController<?> anotherController = batchSubTarget.addService(anotherServiceName, Service.NULL)
            .install();
        assertController(anotherServiceName, anotherController);
        assertController(anotherController, anotherServiceStart);
        assertController(anotherController, anotherServiceStart1);
        assertController(anotherController, anotherServiceStart2);
        assertController(anotherController, anotherServiceStart3);

        // install extraService on batchTarget
        Future<ServiceController<?>> extraServiceStart = testListener.expectServiceStart(extraServiceName);
        ServiceController<?> extraController = batchTarget.addService(extraServiceName, Service.NULL).install();
        assertController(extraServiceName, extraController);
        assertController(extraController, extraServiceStart);

        // removing anotherService is ok
        final Future<ServiceController<?>> anotherServiceRemoval = testListener.expectServiceRemoval(anotherServiceName);
        final Future<ServiceController<?>> anotherServiceRemoval1 = testListener1.expectServiceRemoval(anotherServiceName);
        final Future<ServiceController<?>> anotherServiceRemoval2 = testListener2.expectServiceRemoval(anotherServiceName);
        final Future<ServiceController<?>> anotherServiceRemoval3 = testListener3.expectServiceRemoval(anotherServiceName);
        anotherController.setMode(Mode.REMOVE);
        assertController(anotherController, anotherServiceRemoval);
        assertController(anotherController, anotherServiceRemoval1);
        assertController(anotherController, anotherServiceRemoval2);
        assertController(anotherController, anotherServiceRemoval3);

        // call batchTarget.removeServices: only oneService and extraServices will be removed this time
        final Future<ServiceController<?>> oneServiceRemoval = testListener.expectServiceRemoval(oneMoreServiceName);
        final Future<ServiceController<?>> oneServiceRemoval1 = testListener1.expectServiceRemoval(oneMoreServiceName);
        final Future<ServiceController<?>> oneServiceRemoval2 = testListener2.expectServiceRemoval(oneMoreServiceName);
        final Future<ServiceController<?>> oneServiceRemoval3 = testListener3.expectServiceRemoval(oneMoreServiceName);
        Future<ServiceController<?>> extraServiceRemoval = testListener.expectServiceRemoval(extraServiceName);
        batchTarget.removeServices();
        assertController(oneController, oneServiceRemoval);
        assertController(oneController, oneServiceRemoval1);
        assertController(oneController, oneServiceRemoval2);
        assertController(oneController, oneServiceRemoval3);
        assertController(extraController, extraServiceRemoval);

        // installing extraService is not successful because there is a missing dependency on oneMoreService
        final Future<ServiceController<?>> extraServiceDepMissing = testListener.expectImmediateDependencyUninstall(extraServiceName);
        final Future<ServiceController<?>> extraServiceDepMissing1 = testListener1.expectImmediateDependencyUninstall(extraServiceName);
        final Future<ServiceController<?>> extraServiceDepMissing2 = testListener2.expectImmediateDependencyUninstall(extraServiceName);
        final Future<ServiceController<?>> extraServiceDepMissing3 = testListener3.expectImmediateDependencyUninstall(extraServiceName);
        extraController = batchSubTarget.addService(extraServiceName, Service.NULL).install();
        assertController(extraServiceName, extraController);
        assertController(extraController, extraServiceDepMissing);
        assertController(extraController, extraServiceDepMissing1);
        assertController(extraController, extraServiceDepMissing2);
        assertController(extraController, extraServiceDepMissing3);

        // extraService should be removed on invoking batchTarget.removeServices()
        extraServiceRemoval = testListener.expectServiceRemoval(extraServiceName);
        final Future<ServiceController<?>> extraServiceRemoval1 = testListener1.expectServiceRemoval(extraServiceName);
        final Future<ServiceController<?>> extraServiceRemoval2 = testListener2.expectServiceRemoval(extraServiceName);
        final Future<ServiceController<?>> extraServiceRemoval3 = testListener3.expectServiceRemoval(extraServiceName);
        batchTarget.removeServices();
        assertController(extraController, extraServiceRemoval);
        assertController(extraController, extraServiceRemoval1);
        assertController(extraController, extraServiceRemoval2);
        assertController(extraController, extraServiceRemoval3);

        // installing extraService on containerTarget is successful because there is no dependency
        // on oneMoreService in containerTarget (the dependency was added to batchTarget)
        extraServiceStart = testListener.expectServiceStart(extraServiceName);
        extraController = containerTarget.addService(extraServiceName, Service.NULL).install();
        assertController(extraServiceName, extraController);
        assertController(extraController, extraServiceStart);
        // there should be no effect on this call, as no new services have been added
        batchTarget.removeServices(); 
    }

    /**
     * Returns the ServiceTarget that should be tested.
     * 
     * @param serviceTarget     a serviceTarget where services should be installed
     * @return  a servicer target that delegates to {@code serviceTarget}, providing ServiceBuilders that
     *          will be installed into {@code serviceTarget}
     */
    protected abstract ServiceTarget getServiceTarget(ServiceTarget serviceTarget);
}
