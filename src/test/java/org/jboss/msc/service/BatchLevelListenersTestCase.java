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
 * Test to verify the functionality of batch level listeners.
 *
 * @author John Bailey
 */
public class BatchLevelListenersTestCase extends AbstractServiceTest {

    private final static ServiceName firstServiceName = ServiceName.of("firstService");
    private final static ServiceName secondServiceName = ServiceName.of("secondService");

    @Test
    public void testBatchLevel() throws Exception {
        final LatchedFinishListener latch = new LatchedFinishListener();
        final MockListener listenerOne = new MockListener(latch);
        final MockListener listenerTwo = new MockListener(latch);
        final MockListener listenerThree = new MockListener(latch);
        final BatchBuilder builder = serviceContainer.batchBuilder();
        final TestServiceListener testListener = new TestServiceListener();
        builder.addListener(testListener);

        builder.addListener(listenerOne);
        builder.addService(firstServiceName, Service.NULL).install();

        builder.addListener(listenerTwo);
        builder.addService(secondServiceName, Service.NULL).install();

        builder.addListener(listenerThree);

        Set<ServiceListener<Object>> listeners = builder.getListeners();
        assertNotNull(listeners);
        assertEquals(4, listeners.size());
        assertTrue(listeners.contains(testListener));
        assertTrue(listeners.contains(listenerOne));
        assertTrue(listeners.contains(listenerTwo));
        assertTrue(listeners.contains(listenerThree));

        Set<ServiceName> dependencies = builder.getDependencies();
        assertNotNull(dependencies);
        assertTrue(dependencies.isEmpty());

        builder.install();
        latch.await();

        assertTrue(listenerOne.startedServices.contains(firstServiceName));
        assertTrue(listenerOne.startedServices.contains(secondServiceName));
        assertTrue(listenerTwo.startedServices.contains(firstServiceName));
        assertTrue(listenerTwo.startedServices.contains(secondServiceName));
        assertTrue(listenerThree.startedServices.contains(firstServiceName));
        assertTrue(listenerThree.startedServices.contains(secondServiceName));
    }

    @Test
    public void testSubBatchLevel() throws Exception {
        final LatchedFinishListener latch = new LatchedFinishListener();
        final MockListener batchListener = new MockListener(latch);
        final MockListener subBatchListener = new MockListener(latch);

        final BatchBuilder builder = serviceContainer.batchBuilder();
        final TestServiceListener testListener = new TestServiceListener();
        builder.addListener(testListener);

        builder.addListener(batchListener);
        builder.addService(firstServiceName, Service.NULL).install();

        Set<ServiceListener<Object>> listeners = builder.getListeners();
        assertNotNull(listeners);
        assertEquals(2, listeners.size());
        assertTrue(listeners.contains(testListener));
        assertTrue(listeners.contains(batchListener));

        Set<ServiceName> dependencies = builder.getDependencies();
        assertNotNull(dependencies);
        assertTrue(dependencies.isEmpty());

        final ServiceTarget subBatchBuilder = builder.subTarget();
        subBatchBuilder.addListener(subBatchListener);
        subBatchBuilder.addService(secondServiceName, Service.NULL).install();

        listeners = subBatchBuilder.getListeners();
        assertNotNull(listeners);
        assertEquals(1, listeners.size());
        assertTrue(listeners.contains(subBatchListener));

        builder.install();
        latch.await();

        assertTrue(batchListener.startedServices.contains(firstServiceName));
        assertTrue(batchListener.startedServices.contains(secondServiceName));

        assertFalse(subBatchListener.startedServices.contains(firstServiceName));
        assertTrue(subBatchListener.startedServices.contains(secondServiceName));
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
