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
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.jboss.msc.service.util.LatchedFinishListener;
import org.jboss.msc.util.TestServiceListener;
import org.junit.Test;

/**
 * Test to verify the behavior of scenarios involving multiple listeners.
 *
 * @author John Bailey
 */
public class MultipleListenersTestCase extends AbstractServiceTest {

    private final static ServiceName firstServiceName = ServiceName.of("firstService");
    private final static ServiceName secondServiceName = ServiceName.of("secondService");

    @Test
    public void test1() throws Exception {
        final LatchedFinishListener latch = new LatchedFinishListener();
        final MockListener listenerOne = new MockListener(latch);
        final MockListener listenerTwo = new MockListener(latch);
        final MockListener listenerThree = new MockListener(latch);
        final TestServiceListener testListener = new TestServiceListener();
        serviceContainer.addListener(testListener);

        serviceContainer.addListener(listenerOne);
        serviceContainer.addListener(listenerTwo);

        serviceContainer.addListener(listenerThree);
        serviceContainer.addService(firstServiceName, Service.NULL).install();
        serviceContainer.addService(secondServiceName, Service.NULL).install();

        Set<ServiceListener<Object>> listeners = serviceContainer.getListeners();
        assertNotNull(listeners);
        assertEquals(4, listeners.size());
        assertTrue(listeners.contains(testListener));
        assertTrue(listeners.contains(listenerOne));
        assertTrue(listeners.contains(listenerTwo));
        assertTrue(listeners.contains(listenerThree));

        Set<ServiceName> dependencies = serviceContainer.getDependencies();
        assertNotNull(dependencies);
        assertTrue(dependencies.isEmpty());

        latch.await();

        assertTrue(listenerOne.startedServices.contains(firstServiceName));
        assertTrue(listenerOne.startedServices.contains(secondServiceName));
        assertTrue(listenerTwo.startedServices.contains(firstServiceName));
        assertTrue(listenerTwo.startedServices.contains(secondServiceName));
        assertTrue(listenerThree.startedServices.contains(firstServiceName));
        assertTrue(listenerThree.startedServices.contains(secondServiceName));
    }

    @Test
    public void test2() throws Exception {
        final LatchedFinishListener latch = new LatchedFinishListener();
        final MockListener listener1 = new MockListener(latch);
        final MockListener listener2 = new MockListener(latch);

        final TestServiceListener testListener = new TestServiceListener();
        serviceContainer.addListener(testListener);
        serviceContainer.addListener(listener1);

        serviceContainer.addService(firstServiceName, Service.NULL).install();

        Set<ServiceListener<Object>> listeners = serviceContainer.getListeners();
        assertNotNull(listeners);
        assertEquals(2, listeners.size());
        assertTrue(listeners.contains(testListener));
        assertTrue(listeners.contains(listener1));

        Set<ServiceName> dependencies = serviceContainer.getDependencies();
        assertNotNull(dependencies);
        assertTrue(dependencies.isEmpty());

        final ServiceTarget subTarget = serviceContainer.subTarget();
        subTarget.addListener(listener2);
        subTarget.addService(secondServiceName, Service.NULL).install();

        listeners = subTarget.getListeners();
        assertNotNull(listeners);
        assertEquals(1, listeners.size());
        assertTrue(listeners.contains(listener2));

        latch.await();

        assertTrue(listener1.startedServices.contains(firstServiceName));
        assertTrue(listener1.startedServices.contains(secondServiceName));

        assertFalse(listener2.startedServices.contains(firstServiceName));
        assertTrue(listener2.startedServices.contains(secondServiceName));
    }

    private static class MockListener extends AbstractServiceListener<Object> {

        private final LatchedFinishListener latch;
        private final List<ServiceName> startedServices = Collections.synchronizedList(new ArrayList<ServiceName>());

        public MockListener(LatchedFinishListener latch) {
            this.latch = latch;
        }

        @Override
        public void listenerAdded(ServiceController<? extends Object> serviceController) {
            latch.listenerAdded(serviceController);
        }

        @Override
        public void serviceStarted(ServiceController<? extends Object> serviceController) {
            startedServices.add(serviceController.getName());
            latch.serviceStarted(serviceController);
        }
    }
}
