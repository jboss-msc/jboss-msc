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
        serviceTarget.removeListener((ServiceListener)null);
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

    /**
     * Returns the ServiceTarget that should be tested.
     * 
     * @param serviceTarget     a serviceTarget where services should be installed
     * @return  a servicer target that delegates to {@code serviceTarget}, providing ServiceBuilders that
     *          will be installed into {@code serviceTarget}
     */
    protected abstract ServiceTarget getServiceTarget(ServiceTarget serviceTarget);
}
