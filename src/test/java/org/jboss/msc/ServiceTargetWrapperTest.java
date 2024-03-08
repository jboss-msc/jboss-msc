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

import java.util.function.Consumer;

import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceTarget;
import org.junit.Test;

import static org.junit.Assert.*;

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
        TestLifecycleListener testListener1 = new TestLifecycleListener();
        TestLifecycleListener testListener2 = new TestLifecycleListener();
        TestLifecycleListener testListener3 = new TestLifecycleListener();
        serviceTarget.addListener(testListener1);
        serviceTarget.addListener(testListener2);
        serviceTarget.addListener(testListener3);

        ServiceBuilder<?> sb = serviceTarget.addService();
        Consumer<String> providedValue = sb.provides(oneMoreServiceName);
        sb.setInstance(Service.newInstance(providedValue, oneMoreServiceName.toString()));
        ServiceController<?> oneMoreServiceController = sb.install();

        serviceContainer.awaitStability();

        assertSame(serviceContainer.getRequiredService(oneMoreServiceName), oneMoreServiceController);
        assertEquals(testListener1.upValues().size(), 1);
        assertTrue(testListener1.upValues().contains(oneMoreServiceName));
        assertEquals(testListener2.upValues().size(), 1);
        assertTrue(testListener2.upValues().contains(oneMoreServiceName));
        assertEquals(testListener3.upValues().size(), 1);
        assertTrue(testListener3.upValues().contains(oneMoreServiceName));

        builderTarget.addDependency(oneMoreServiceName);

        sb = serviceTarget.addService();
        providedValue = sb.provides(serviceName);
        sb.setInstance(Service.newInstance(providedValue, serviceName.toString()));
        ServiceController<?> serviceController = sb.install();

        sb = serviceTarget.addService();
        providedValue = sb.provides(anotherServiceName);
        sb.setInstance(Service.newInstance(providedValue, anotherServiceName.toString()));
        ServiceController<?> anotherServiceController = sb.install();

        sb = serviceTarget.addService();
        providedValue = sb.provides(extraServiceName);
        sb.setInstance(Service.newInstance(providedValue, extraServiceName.toString()));
        ServiceController<?> extraServiceController = sb.install();

        serviceContainer.awaitStability();

        assertSame(serviceContainer.getRequiredService(serviceName), serviceController);
        assertSame(serviceContainer.getRequiredService(anotherServiceName), anotherServiceController);
        assertSame(serviceContainer.getRequiredService(extraServiceName), extraServiceController);

        assertEquals(testListener1.upValues().size(), 4);
        assertTrue(testListener1.upValues().contains(oneMoreServiceName));
        assertTrue(testListener1.upValues().contains(serviceName));
        assertTrue(testListener1.upValues().contains(anotherServiceName));
        assertTrue(testListener1.upValues().contains(extraServiceName));
        assertEquals(testListener2.upValues().size(), 4);
        assertTrue(testListener2.upValues().contains(oneMoreServiceName));
        assertTrue(testListener2.upValues().contains(serviceName));
        assertTrue(testListener2.upValues().contains(anotherServiceName));
        assertTrue(testListener2.upValues().contains(extraServiceName));
        assertEquals(testListener3.upValues().size(), 4);
        assertTrue(testListener3.upValues().contains(oneMoreServiceName));
        assertTrue(testListener3.upValues().contains(serviceName));
        assertTrue(testListener3.upValues().contains(anotherServiceName));
        assertTrue(testListener3.upValues().contains(extraServiceName));
    }
}
