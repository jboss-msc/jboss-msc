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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.function.Consumer;

import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
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
    protected ServiceTarget serviceTarget;

    @Before
    public void initializeServiceTarget() throws Exception {
        serviceTarget = getServiceTarget(serviceContainer);
    }

    @Test
    public void addService() throws Exception {
        // adding and removing listener is ok
        final TestLifecycleListener testListener = new TestLifecycleListener();
        serviceTarget.addListener(testListener);
        serviceTarget.removeListener(testListener);
        serviceTarget.addListener(testListener);
        // adding null will be ignored
        serviceTarget.addListener(null);
        ServiceBuilder<?> sb = serviceTarget.addService();
        Consumer<String> providedValue = sb.provides(serviceName);
        sb.setInstance(Service.newInstance(providedValue, serviceName.toString()));
        ServiceController<?> serviceController = sb.install();
        serviceContainer.awaitStability();
        assertSame(serviceContainer.getRequiredService(serviceName), serviceController);
        assertEquals(testListener.upValues().size(), 1);
        assertTrue(testListener.upValues().contains(serviceName));
        // removing null will be ignored
        serviceTarget.removeListener(null);
    }

    @Test
    public void addServiceWithDependency() throws Exception {
        final TestLifecycleListener testListener = new TestLifecycleListener();

        ServiceTarget serviceTarget = getServiceTarget(serviceContainer);
        serviceTarget.addListener(testListener);

        ServiceBuilder<?> sb = serviceTarget.addService();
        Consumer<String> providedValue = sb.provides(anotherServiceName);
        sb.setInstance(Service.newInstance(providedValue, anotherServiceName.toString()));
        sb.install();

        serviceTarget.addDependency(anotherServiceName);
        serviceTarget.addDependency((ServiceName) null);// null dependency should be ignored

        sb = serviceTarget.addService();
        providedValue = sb.provides(serviceName);
        sb.setInstance(Service.newInstance(providedValue, serviceName.toString()));
        sb.install();

        serviceContainer.awaitStability();

        assertEquals(testListener.upValues().size(), 2);
        assertTrue(testListener.upValues().contains(serviceName));
        assertTrue(testListener.upValues().contains(anotherServiceName));
    }

    @Test
    public void addServiceWithDependencies() throws Exception {
        ServiceTarget serviceTarget = getServiceTarget(serviceContainer);
        final TestLifecycleListener testListener = new TestLifecycleListener();

        ServiceTarget subTarget = getServiceTarget(serviceTarget.subTarget());
        serviceTarget.addListener(testListener);

        ServiceBuilder<?> sb = serviceTarget.addService();
        Consumer<String> providedValue = sb.provides(oneMoreServiceName);
        sb.setInstance(Service.newInstance(providedValue, oneMoreServiceName.toString()));
        ServiceController<?> oneMoreServiceController = sb.install();

        sb = serviceTarget.addService();
        providedValue = sb.provides(anotherServiceName);
        sb.setInstance(Service.newInstance(providedValue, anotherServiceName.toString()));
        ServiceController<?> anotherServiceController = sb.install();

        subTarget.addDependency(anotherServiceName);
        subTarget.addDependency(oneMoreServiceName);

        sb = serviceTarget.addService();
        providedValue = sb.provides(serviceName);
        sb.setInstance(Service.newInstance(providedValue, serviceName.toString()));
        ServiceController<?> serviceController = sb.install();

        sb = serviceTarget.addService();
        providedValue = sb.provides(extraServiceName);
        sb.setInstance(Service.newInstance(providedValue, extraServiceName.toString()));
        ServiceController<?> extraServiceController = sb.install();

        assertSame(serviceContainer.getRequiredService(oneMoreServiceName), oneMoreServiceController);
        assertSame(serviceContainer.getRequiredService(anotherServiceName), anotherServiceController);
        assertSame(serviceContainer.getRequiredService(serviceName), serviceController);
        assertSame(serviceContainer.getRequiredService(extraServiceName), extraServiceController);
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
