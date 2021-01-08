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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import org.jboss.msc.service.ServiceController.Mode;
import org.jboss.msc.util.TestServiceListener;
import org.junit.Before;
import org.junit.Test;

/**
 * Test for implementations of {@link ServiceRegistry}
 * 
 * @author <a href="mailto:flavia.rainone@jboss.com">Flavia Rainone</a>
 */
public abstract class AbstractServiceRegistryTest extends AbstractServiceTest {

    private static final ServiceName oneTwoThree = ServiceName.of("one", "two", "three");
    private static final ServiceName oneTwoFive = ServiceName.of("one", "two", "five");
    private ServiceRegistry registry;

    protected abstract ServiceRegistry getServiceRegistry(ServiceContainer container);
    
    @Before
    public void addOneTwoThreeAndOneTwoFive() throws Exception {
        serviceContainer.addService(oneTwoThree, Service.NULL).install();
        serviceContainer.addService(oneTwoFive, Service.NULL).install();
        registry = getServiceRegistry(serviceContainer);
    }

    @Test
    public void getRequiredService() throws Exception {
        // retrieve oneTwoThreeController
        final ServiceController<?> oneTwoThreeController = registry.getRequiredService(oneTwoThree);
        // shouldn't be null
        assertNotNull(oneTwoThreeController);
        // the same service as obtained by serviceContainer
        assertSame(serviceContainer.getRequiredService(oneTwoThree), oneTwoThreeController);
        // check for consistency: all calls to getRequiredService return always the same controller
        assertSame(oneTwoThreeController, registry.getRequiredService(oneTwoThree));

        final ServiceController<?> oneTwoFiveController = registry.getRequiredService(oneTwoFive);
        assertNotNull(oneTwoFiveController);
        assertSame(serviceContainer.getRequiredService(oneTwoFive), oneTwoFiveController);
        assertSame(oneTwoFiveController, registry.getRequiredService(oneTwoFive));
        assertServiceNotFoundException(registry, "one");
        assertServiceNotFoundException(registry, "one", "two", "seven");
        assertServiceNotFoundException(registry, "one", "two");
        assertServiceNotFoundException(registry, "another one two three");
    }

    @Test
    public void getService() throws Exception {
        // retrieve oneTwoThreeController
        final ServiceController<?> oneTwoThreeController = registry.getService(oneTwoThree);
        // shouldn't be null
        assertNotNull(oneTwoThreeController);
        // the same service as obtained by serviceContainer
        assertSame(serviceContainer.getService(oneTwoThree), oneTwoThreeController);
        // check for consistency: all calls to getService return always the same controller
        assertSame(oneTwoThreeController, registry.getService(oneTwoThree));
        // and be also consistent with getRequiredService
        assertSame(oneTwoThreeController, registry.getRequiredService(oneTwoThree));

        final ServiceController<?> oneTwoFiveController = registry.getService(oneTwoFive);
        assertNotNull(oneTwoFiveController);
        assertSame(serviceContainer.getService(oneTwoFive), oneTwoFiveController);
        assertSame(oneTwoFiveController, registry.getService(oneTwoFive));
        assertSame(oneTwoFiveController, registry.getRequiredService(oneTwoFive));
        assertNull(registry.getService(ServiceName.of("one")));
        assertNull(registry.getService(ServiceName.of("one", "two", "seven")));
        assertNull(registry.getService(ServiceName.of("one", "two")));
        assertNull(registry.getService(ServiceName.of("another one two three")));
    }

    @Test
    public void getServiceNamesAfterRemoval() throws Exception {
        List<ServiceName> serviceNames = registry.getServiceNames();
        assertNotNull(serviceNames);
        assertEquals(2, serviceNames.size());
        assertTrue(serviceNames.contains(oneTwoThree));
        assertTrue(serviceNames.contains(oneTwoFive));
        removeService(oneTwoFive);
        serviceNames = registry.getServiceNames();
        assertNotNull(serviceNames);
        assertEquals(1, serviceNames.size());
        assertTrue(serviceNames.contains(oneTwoThree));
        removeService(oneTwoThree);
        serviceNames = registry.getServiceNames();
        assertEquals(0, serviceNames.size());
    }

    @Test
    public void getServiceNamesAfterAddition() throws Exception {
        final ServiceName twoThreeFour = ServiceName.of("two", "three", "four");
        final ServiceName twoThreeSix = ServiceName.of("two", "three", "six");
        final ServiceName twoThreeEight = ServiceName.of("two", "three", "eight");
        final ServiceName twoThreeTen = ServiceName.of("two", "three", "ten");
        serviceContainer.addService(twoThreeFour, Service.NULL).install();
        serviceContainer.addService(twoThreeSix, Service.NULL).install();
        serviceContainer.addService(twoThreeEight, Service.NULL).install();
        serviceContainer.addService(twoThreeTen, Service.NULL).install();
        final List<ServiceName> serviceNames = serviceContainer.getServiceNames();
        assertNotNull(serviceNames);
        assertEquals(6, serviceNames.size());
        assertTrue(serviceNames.contains(twoThreeFour));
        assertTrue(serviceNames.contains(twoThreeSix));
        assertTrue(serviceNames.contains(twoThreeEight));
        assertTrue(serviceNames.contains(twoThreeTen));
        assertTrue(serviceNames.contains(oneTwoThree));
        assertTrue(serviceNames.contains(oneTwoFive));
    }

    /**
     * Remove {@code serviceName} from {@code serviceContainer}.
     */
    private void removeService(ServiceName serviceName) throws InterruptedException, ExecutionException {
        final TestServiceListener testListener = new TestServiceListener();
        final ServiceController<?> serviceController = serviceContainer.getService(serviceName);
        final Future<ServiceController<?>> listenerAdded = testListener.expectListenerAdded(serviceName);
        serviceController.addListener(testListener);
        assertSame(serviceController, listenerAdded.get());
        final Future<ServiceController<?>> serviceRemoved = testListener.expectServiceRemoval(serviceName);
        serviceController.setMode(Mode.REMOVE);
        assertSame(serviceController, serviceRemoved.get());
    }

    /**
     * Assert that {@code registry} does not contain a service with name composed by {@code serviceNameParts}.
     */
    private static final void assertServiceNotFoundException(ServiceRegistry registry, String ... serviceNameParts) {
        try {
            registry.getRequiredService(ServiceName.of(serviceNameParts));
            fail("ServiceNotFoundExcepetion expected");
        } catch (ServiceNotFoundException e) {}
    }

}
