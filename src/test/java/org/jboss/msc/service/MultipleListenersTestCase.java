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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Set;
import java.util.concurrent.Future;

import org.jboss.msc.util.TestServiceListener;
import org.junit.Test;

/**
 * Test to verify the behavior of scenarios involving multiple listeners.
 *
 * @author John Bailey
 */
public class MultipleListenersTestCase extends AbstractServiceTest {

    private static final ServiceName firstServiceName = ServiceName.of("firstService");
    private static final ServiceName secondServiceName = ServiceName.of("secondService");

    @Test
    public void test1() throws Exception {
        final TestServiceListener listenerOne = new TestServiceListener();
        Future<ServiceController<?>> listener1Service1Future = listenerOne.expectServiceStart(firstServiceName);
        Future<ServiceController<?>> listener1Service2Future = listenerOne.expectServiceStart(secondServiceName);
        final TestServiceListener listenerTwo = new TestServiceListener();
        Future<ServiceController<?>> listener2Service1Future = listenerTwo.expectServiceStart(firstServiceName);
        Future<ServiceController<?>> listener2Service2Future = listenerTwo.expectServiceStart(secondServiceName);
        final TestServiceListener listenerThree = new TestServiceListener();
        Future<ServiceController<?>> listener3Service1Future = listenerThree.expectServiceStart(firstServiceName);
        Future<ServiceController<?>> listener3Service2Future = listenerThree.expectServiceStart(secondServiceName);
        final TestServiceListener testListener = new TestServiceListener();
        serviceContainer.addListener(testListener);

        serviceContainer.addListener(listenerOne);
        serviceContainer.addListener(listenerTwo);

        serviceContainer.addListener(listenerThree);
        ServiceController<?> firstServiceController = serviceContainer.addService(firstServiceName, Service.NULL).install();
        ServiceController<?> secondServiceController = serviceContainer.addService(secondServiceName, Service.NULL).install();

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
        serviceContainer.awaitStability();


        assertTrue(listener1Service1Future.get() == firstServiceController);
        assertTrue(listener1Service2Future.get() == secondServiceController);
        assertTrue(listener2Service1Future.get() == firstServiceController);
        assertTrue(listener2Service2Future.get() == secondServiceController);
        assertTrue(listener3Service1Future.get() == firstServiceController);
        assertTrue(listener3Service2Future.get() == secondServiceController);
    }

    @Test
    public void test2() throws Exception {
        final TestServiceListener listener1 = new TestServiceListener();
        Future<ServiceController<?>> listener1Service1Future = listener1.expectServiceStart(firstServiceName);
        Future<ServiceController<?>> listener1Service2Future = listener1.expectServiceStart(secondServiceName);
        final TestServiceListener listener2 = new TestServiceListener();
        Future<ServiceController<?>> listener2Service1Future = listener2.expectServiceStart(firstServiceName);
        Future<ServiceController<?>> listener2Service2Future = listener2.expectServiceStart(secondServiceName);

        final TestServiceListener testListener = new TestServiceListener();
        serviceContainer.addListener(testListener);
        serviceContainer.addListener(listener1);

        ServiceController<?> firstServiceController = serviceContainer.addService(firstServiceName, Service.NULL).install();

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
        ServiceController<?> secondServiceController = subTarget.addService(secondServiceName, Service.NULL).install();

        listeners = subTarget.getListeners();
        assertNotNull(listeners);
        assertEquals(1, listeners.size());
        assertTrue(listeners.contains(listener2));

        serviceContainer.awaitStability();

        assertTrue(listener1Service1Future.get() == firstServiceController);
        assertTrue(listener1Service2Future.get() == secondServiceController);

        assertFalse(listener2Service1Future.get() == firstServiceController);
        assertTrue(listener2Service2Future.get() == secondServiceController);
    }

    @Test
    public void testAddListenerOnListenerAdded() throws Exception {
        TestServiceListener listener1 = new TestServiceListener();
        ListenerAdder listener2 = new ListenerAdder(listener1);
        Future<ServiceController<?>> serviceStartFuture = listener1.expectServiceStart(firstServiceName);

        ServiceController<?> firstServiceController = serviceContainer.addService(firstServiceName, Service.NULL).addListener(listener2).install();

        serviceContainer.awaitStability();
        assertTrue(serviceStartFuture.get() == firstServiceController);
    }

    private static class ListenerAdder extends AbstractServiceListener<Object> {

        private ServiceListener<Object> listener;

        public ListenerAdder(ServiceListener<Object> listenerToAdd) {
            listener = listenerToAdd;
        }

        @Override
        public void listenerAdded(ServiceController<? extends Object> serviceController) {
            serviceController.addListener(listener);
        }
    }
}
