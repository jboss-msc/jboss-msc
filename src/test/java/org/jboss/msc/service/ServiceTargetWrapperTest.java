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
import org.junit.Test;

/**
 * Test for {@code ServiceTarget} wrapper implementations that wrap  {@code ServiceTarget} without changing basic
 * behavior. In other words, calling addDependency/Listener/Service on  the wrapper would have the exact same effect as
 * calling those methods on the wrapped target.
 * 
 * @author <a href="mailto:flavia.rainone@jboss.com">Flavia Rainone</a>
 */
public abstract class ServiceTargetWrapperTest extends AbstractServiceTargetTest {

    @SuppressWarnings("unchecked")
    @Test
    public void addServiceWithDependencyCollection() throws Exception {
        ServiceTarget builderTarget = getServiceTarget(serviceContainer);
        TestServiceListener testListener1 = new TestServiceListener();
        TestServiceListener testListener2 = new TestServiceListener();
        TestServiceListener testListener3 = new TestServiceListener();
        serviceTarget.addListener(testListener1, testListener2, testListener3);

        Future<ServiceController<?>> oneMoreServiceStart1 = testListener1.expectServiceStart(oneMoreServiceName);
        Future<ServiceController<?>> oneMoreServiceStart2 = testListener2.expectServiceStart(oneMoreServiceName);
        Future<ServiceController<?>> oneMoreServiceStart3 = testListener3.expectServiceStart(oneMoreServiceName);
        ServiceController<?> oneMoreServiceController = serviceTarget.addService(oneMoreServiceName, Service.NULL).install();
        assertController(oneMoreServiceName, oneMoreServiceController);
        assertController(oneMoreServiceController, oneMoreServiceStart1);
        assertController(oneMoreServiceController, oneMoreServiceStart2);
        assertController(oneMoreServiceController, oneMoreServiceStart3);

        Future<ServiceController<?>> serviceStart1 = testListener1.expectServiceStart(serviceName);
        Future<ServiceController<?>> serviceStart3 = testListener3.expectServiceStart(serviceName);
        Future<ServiceController<?>> anotherServiceStart1 = testListener1.expectServiceStart(anotherServiceName);
        Future<ServiceController<?>> anotherServiceStart3 = testListener3.expectServiceStart(anotherServiceName);
        Future<ServiceController<?>> extraServiceStart1 = testListener1.expectServiceStart(extraServiceName);
        Future<ServiceController<?>> extraServiceStart3 = testListener3.expectServiceStart(extraServiceName);

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

        ServiceController<?> serviceController = assertController(serviceName, serviceStart1);
        assertController(serviceController, serviceStart3);
        ServiceController<?> anotherServiceController = assertController(anotherServiceName, anotherServiceStart1);
        assertController(anotherServiceController, anotherServiceStart3);
        ServiceController<?> extraServiceController = assertController(extraServiceName, extraServiceStart1);
        assertController(extraServiceController, extraServiceStart3);
    }
}
