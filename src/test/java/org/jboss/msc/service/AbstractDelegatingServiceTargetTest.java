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

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.Future;

import org.jboss.msc.util.TestServiceListener;
import org.jboss.msc.value.Values;
import org.junit.Before;
import org.junit.Test;

/**
 * Test for {@code ServiceTarget} delegates. 
 * 
 * @author <a href="mailto:flavia.rainone@jboss.com">Flavia Rainone</a>
 */
public abstract class AbstractDelegatingServiceTargetTest extends AbstractServiceTest {

    private static final ServiceName serviceName = ServiceName.of("service", "name");
    private static final ServiceName anotherServiceName = ServiceName.of("service", "another", "name");
    private static final ServiceName oneMoreServiceName = ServiceName.of("service", "one", "more", "name");
    private static final ServiceName extraServiceName = ServiceName.of("service", "extra", "name");
    private static final TestServiceListener testListener = new TestServiceListener();

    private ServiceTarget serviceTarget;

    @Before
    public void initializeServiceTarget() {
        serviceTarget = getDelegatingServiceTarget(serviceContainer);
    }

    @Test
    public void addService() throws Exception {
        Future<ServiceController<?>> serviceStart = testListener.expectServiceStart(serviceName);
        serviceTarget.addListener(testListener);
        serviceTarget.addService(serviceName, Service.NULL).install();
        assertController(serviceName, serviceStart);
    }

    @Test
    public void addServiceValue() throws Exception {
        Future<ServiceController<?>> serviceStart = testListener.expectServiceStart(serviceName);
        serviceTarget.addListener(testListener);
        serviceTarget.addServiceValue(serviceName, Values.immediateValue(Service.NULL)).install();
        assertController(serviceName, serviceStart);
    }

    @Test
    public void addServiceWithDependencyToBatchBuilder() throws Exception {
        BatchBuilder builder = serviceContainer.batchBuilder();
        ServiceTarget serviceTarget = getDelegatingServiceTarget(builder);
        serviceTarget.addService(anotherServiceName, Service.NULL).install();
        serviceTarget.addListener(testListener);
        serviceTarget.addDependency(anotherServiceName);
        serviceTarget.addServiceValue(serviceName, Values.immediateValue(Service.NULL)).install();

        Future<ServiceController<?>> serviceStart = testListener.expectServiceStart(serviceName);
        Future<ServiceController<?>> anotherServiceStart = testListener.expectServiceStart(anotherServiceName);
        builder.install();
        assertController(serviceName, serviceStart);
        assertController(anotherServiceName, anotherServiceStart);
    }

    @Test
    public void addServiceWithDependenciesToBatchBuilder() throws Exception {
        BatchBuilder builder = serviceContainer.batchBuilder();
        ServiceTarget serviceTarget = getDelegatingServiceTarget(builder);

        ServiceTarget subTarget = getDelegatingServiceTarget(serviceTarget.subTarget());
        serviceTarget.addService(oneMoreServiceName, Service.NULL).install();
        serviceTarget.addService(anotherServiceName, Service.NULL).install();
        serviceTarget.addListener(testListener);
        subTarget.addDependency(anotherServiceName, oneMoreServiceName);
        subTarget.addServiceValue(serviceName, Values.immediateValue(Service.NULL)).install();
        subTarget.addServiceValue(extraServiceName, Values.immediateValue(Service.NULL)).install();

        Future<ServiceController<?>> oneMoreServiceStart = testListener.expectServiceStart(oneMoreServiceName);
        Future<ServiceController<?>> anotherServiceStart = testListener.expectServiceStart(anotherServiceName);
        Future<ServiceController<?>> serviceStart = testListener.expectServiceStart(serviceName);
        Future<ServiceController<?>> extraServiceStart = testListener.expectServiceStart(extraServiceName);
        builder.install();
        assertController(oneMoreServiceName, oneMoreServiceStart);
        assertController(anotherServiceName, anotherServiceStart);
        assertController(serviceName, serviceStart);
        assertController(extraServiceName, extraServiceStart);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void addServiceWithDependencyCollection() throws Exception {
        BatchBuilder builder = serviceTarget.batchBuilder();
        ServiceTarget builderTarget = getDelegatingServiceTarget(builder);
        TestServiceListener testListener1 = new TestServiceListener();
        TestServiceListener testListener2 = new TestServiceListener();
        TestServiceListener testListener3 = new TestServiceListener();
        serviceTarget.addListener(testListener1, testListener2, testListener3);

        Future<ServiceController<?>> oneMoreServiceStart1 = testListener1.expectServiceStart(oneMoreServiceName);
        Future<ServiceController<?>> oneMoreServiceStart2 = testListener2.expectServiceStart(oneMoreServiceName);
        Future<ServiceController<?>> oneMoreServiceStart3 = testListener3.expectServiceStart(oneMoreServiceName);
        serviceTarget.addService(oneMoreServiceName, Service.NULL).install();
        ServiceController<?> oneMoreServiceController = assertController(oneMoreServiceName, oneMoreServiceStart1);
        assertController(oneMoreServiceController, oneMoreServiceStart2);
        assertController(oneMoreServiceController, oneMoreServiceStart3);

        Collection<ServiceName> dependencies = new ArrayList<ServiceName>();
        dependencies.add(oneMoreServiceName);
        builderTarget.addDependency(dependencies);
        builderTarget.addServiceValue(serviceName, Values.immediateValue(Service.NULL)).install();
        builderTarget.addServiceValue(anotherServiceName, Values.immediateValue(Service.NULL)).install();
        builderTarget.addServiceValue(extraServiceName, Values.immediateValue(Service.NULL)).install();
        Collection<ServiceListener<Object>> listeners = new ArrayList<ServiceListener<Object>>();
        listeners.add(testListener1);
        listeners.add(testListener3);
        builderTarget.addListener(listeners);

        Future<ServiceController<?>> serviceStart1 = testListener1.expectServiceStart(serviceName);
        Future<ServiceController<?>> serviceStart3 = testListener3.expectServiceStart(serviceName);
        Future<ServiceController<?>> anotherServiceStart1 = testListener1.expectServiceStart(anotherServiceName);
        Future<ServiceController<?>> anotherServiceStart3 = testListener3.expectServiceStart(anotherServiceName);
        Future<ServiceController<?>> extraServiceStart1 = testListener1.expectServiceStart(extraServiceName);
        Future<ServiceController<?>> extraServiceStart3 = testListener3.expectServiceStart(extraServiceName);
        builder.install();

        ServiceController<?> serviceController = assertController(serviceName, serviceStart1);
        assertController(serviceController, serviceStart3);
        ServiceController<?> anotherServiceController = assertController(anotherServiceName, anotherServiceStart1);
        assertController(anotherServiceController, anotherServiceStart3);
        ServiceController<?> extraServiceController = assertController(extraServiceName, extraServiceStart1);
        assertController(extraServiceController, extraServiceStart3);
    }

    /**
     * Returns the delegating ServiceTarget that should be tested.
     * 
     * @param serviceTarget     the delegate
     * @return  the delegating  the delegating servicer target
     */
    protected abstract ServiceTarget getDelegatingServiceTarget(ServiceTarget serviceTarget);
}
