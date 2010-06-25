/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat Middleware LLC, and individual contributors
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

import org.jboss.msc.service.util.LatchedFinishListener;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;

/**
 * Test to verify the functionality of batch level listeners.
 * 
 * @author John Bailey
 */
public class BatchLevelListenersTestCase {

    @Test
    public void testBatchLevel() throws Exception {
        final LatchedFinishListener listener = new LatchedFinishListener();

        final ServiceContainer serviceContainer = ServiceContainer.Factory.create();

        final BatchBuilder builder = serviceContainer.batchBuilder();
        final MockListener listenerOne = new MockListener();
        builder.addListener(listenerOne);
        builder.addService(ServiceName.of("firstService"), Service.NULL).addListener(listener);
        final MockListener listenerTwo = new MockListener();
        builder.addListener(listenerTwo);
        builder.addService(ServiceName.of("secondService"), Service.NULL).addListener(listener);
        final MockListener listenerThree = new MockListener();
        builder.addListener(listenerThree);
        builder.install();
        listener.await();
        Assert.assertTrue(listenerOne.startedServices.contains(ServiceName.of("firstService")));
        Assert.assertTrue(listenerOne.startedServices.contains(ServiceName.of("secondService")));
        Assert.assertTrue(listenerTwo.startedServices.contains(ServiceName.of("firstService")));
        Assert.assertTrue(listenerTwo.startedServices.contains(ServiceName.of("secondService")));
        Assert.assertTrue(listenerThree.startedServices.contains(ServiceName.of("firstService")));
        Assert.assertTrue(listenerThree.startedServices.contains(ServiceName.of("secondService")));
        serviceContainer.shutdown();
    }

    @Test
    public void testSubBatchLevel() throws Exception {
        final LatchedFinishListener listener = new LatchedFinishListener();
        final ServiceContainer serviceContainer = ServiceContainer.Factory.create();
        final BatchBuilder builder = serviceContainer.batchBuilder();
        final MockListener batchListener = new MockListener();
        builder.addListener(batchListener);

        builder.addService(ServiceName.of("firstService"), Service.NULL).addListener(listener);

        final SubBatchBuilder subBatchBuilder = builder.subBatchBuilder();

        final MockListener subBatchListener = new MockListener();
        subBatchBuilder.addListener(subBatchListener);

        subBatchBuilder.addService(ServiceName.of("secondService"), Service.NULL).addListener(listener);
        builder.install();
        listener.await();
        List<ServiceName> expectedStartedServices = Arrays.asList(ServiceName.of("firstService"), ServiceName.of("secondService"));
        Assert.assertEquals(expectedStartedServices, batchListener.startedServices);
        Assert.assertFalse(subBatchListener.startedServices.contains(ServiceName.of("firstService")));
        Assert.assertTrue(subBatchListener.startedServices.contains(ServiceName.of("secondService")));
        serviceContainer.shutdown();
    }

    private static class MockListener extends AbstractServiceListener<Object> {

        private final List<ServiceName> startedServices = new ArrayList<ServiceName>();

        @Override
        public void serviceStarted(ServiceController<? extends Object> serviceController) {
            startedServices.add(serviceController.getName());
        }
    }
}
