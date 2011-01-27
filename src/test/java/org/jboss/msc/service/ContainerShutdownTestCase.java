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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.jboss.msc.service.ServiceContainer.TerminateListener;
import org.jboss.msc.service.ServiceController.State;
import org.jboss.msc.util.TestServiceListener;
import org.jboss.msc.value.Values;
import org.junit.Before;
import org.junit.Test;

/**
 * Verifies the server shutdown process and related features such as {@link TerminateListener} notification, and 
 * {@link ServiceContainer#awaitTermination() awaitTermination}.
 * 
 * @author <a href="mailto:flavia.rainone@jboss.com">Flavia Rainone</a>
 *
 */
public class ContainerShutdownTestCase extends AbstractServiceTest{

    private TestServiceListener testListener;

    @Before
    public void initTestServiceListener() {
        testListener = new TestServiceListener();
    }

    @Test
    public void testDoubleShutdown() throws Exception {
        final ServiceName serviceName = ServiceName.of("service", "name");
        final ServiceController<?> controller = addService(serviceName);
        Future<ServiceController<?>> serviceRemoval = testListener.expectServiceRemoval(serviceName);
        shutdownContainer();
        assertController(controller, serviceRemoval);

        serviceRemoval = testListener.expectNoServiceRemoval(serviceName);
        shutdownContainer();
        assertNull(serviceRemoval.get());
    }

    @Test
    public void testDoubleShutdownOnEmptyContainer() {
        shutdownContainer();
        shutdownContainer();
    }

    @Test
    public void testTerminateListener() throws Exception {
        final ServiceName serviceName = ServiceName.of("service", "name");
        final TestTerminateListener terminateListener = new TestTerminateListener();
        serviceContainer.addTerminateListener(terminateListener);
        ServiceController<?> serviceController = addService(serviceName);
        shutdownContainer();
        TerminateListener.Info terminateInfo = terminateListener.getTerminateInfo();
        // make sure that service is removed when terminateInto.getTerminateInfo returns
        assertEquals(State.REMOVED, serviceController.getState());
        assertTerminateListenerInfo(terminateInfo);
    }

    @Test
    public void testMultipleTerminateListeners() throws Exception {
        final ServiceController<?> serviceController1 = addService(ServiceName.of("service", "name", "1"));
        final ServiceController<?> serviceController2 = addService(ServiceName.of("service", "name", "2"));
        final ServiceController<?> serviceController3 = addService(ServiceName.of("service", "name", "3"));
        final ServiceController<?> serviceController4 = addService(ServiceName.of("service", "name", "4"));
        final ServiceController<?> serviceController5 = addService(ServiceName.of("service", "name", "5"));
        final ServiceController<?> serviceController6 = addService(ServiceName.of("service", "name", "6"));
        final TestTerminateListener terminateListener1 = new TestTerminateListener();
        final TestTerminateListener terminateListener2 = new TestTerminateListener();
        final TestTerminateListener terminateListener3 = new TestTerminateListener();
        final TestTerminateListener terminateListener4 = new TestTerminateListener();
        serviceContainer.addTerminateListener(terminateListener1);
        serviceContainer.addTerminateListener(terminateListener2);
        serviceContainer.addTerminateListener(terminateListener3);
        serviceContainer.addTerminateListener(terminateListener4);

        // shutdown the container
        shutdownContainer();

        TerminateListener.Info terminateInfo1 = terminateListener1.getTerminateInfo();
        TerminateListener.Info terminateInfo2 = terminateListener2.getTerminateInfo();
        TerminateListener.Info terminateInfo3 = terminateListener3.getTerminateInfo();
        TerminateListener.Info terminateInfo4 = terminateListener4.getTerminateInfo();
        // make sure that all services are removed when terminateInfo.getTerminateInfo returns
        assertEquals(State.REMOVED, serviceController1.getState());
        assertEquals(State.REMOVED, serviceController2.getState());
        assertEquals(State.REMOVED, serviceController3.getState());
        assertEquals(State.REMOVED, serviceController4.getState());
        assertEquals(State.REMOVED, serviceController5.getState());
        assertEquals(State.REMOVED, serviceController6.getState());
        assertTerminateListenerInfo(terminateInfo1);
        assertTerminateListenerInfo(terminateInfo2);
        assertTerminateListenerInfo(terminateInfo3);
        assertTerminateListenerInfo(terminateInfo4);
        assertEquals(terminateInfo1, terminateInfo2);
        assertEquals(terminateInfo2, terminateInfo3);
        assertEquals(terminateInfo3, terminateInfo4);
    }

    @Test
    public void testTerminateListenerOnEmptyContainer() throws Exception {
        final TestTerminateListener terminateListener1 = new TestTerminateListener();
        final TestTerminateListener terminateListener2 = new TestTerminateListener();
        final TestTerminateListener terminateListener3 = new TestTerminateListener();
        serviceContainer.addTerminateListener(terminateListener1);
        serviceContainer.addTerminateListener(terminateListener2);
        serviceContainer.addTerminateListener(terminateListener3);

        // shutdown the container
        shutdownContainer();

        assertTerminateListenerInfo(terminateListener1.getTerminateInfo());
        assertTerminateListenerInfo(terminateListener2.getTerminateInfo());
        assertTerminateListenerInfo(terminateListener3.getTerminateInfo());
    }

    @Test
    public void testAddTerminateListenerToShutdownContainer() throws Exception {
        final TestTerminateListener terminateListener1 = new TestTerminateListener();
        final TestTerminateListener terminateListener2 = new TestTerminateListener();
        final TestTerminateListener terminateListener3 = new TestTerminateListener();
        final TestTerminateListener terminateListener4 = new TestTerminateListener();
        final TestTerminateListener terminateListener5 = new TestTerminateListener();
        final TestTerminateListener terminateListener6 = new TestTerminateListener();

        // shutdown the container
        shutdownContainer();

        // add terminate listeners after the container shutdown process is started
        serviceContainer.addTerminateListener(terminateListener1);
        serviceContainer.addTerminateListener(terminateListener2);
        serviceContainer.addTerminateListener(terminateListener3);
        serviceContainer.addTerminateListener(terminateListener4);
        serviceContainer.addTerminateListener(terminateListener5);
        serviceContainer.addTerminateListener(terminateListener6);

        final TerminateListener.Info terminateInfo1 = terminateListener1.getTerminateInfo();
        final TerminateListener.Info terminateInfo2 = terminateListener2.getTerminateInfo();
        final TerminateListener.Info terminateInfo3 = terminateListener3.getTerminateInfo();
        final TerminateListener.Info terminateInfo4 = terminateListener4.getTerminateInfo();
        final TerminateListener.Info terminateInfo5 = terminateListener5.getTerminateInfo();
        assertTerminateListenerInfo(terminateInfo1);
        assertTerminateListenerInfo(terminateInfo2);
        assertTerminateListenerInfo(terminateInfo3);
        assertTerminateListenerInfo(terminateInfo4);
        assertTerminateListenerInfo(terminateInfo5);
        assertEquals(terminateInfo1, terminateInfo2);
        assertEquals(terminateInfo1, terminateInfo3);
        assertEquals(terminateInfo1, terminateInfo4);
        assertEquals(terminateInfo1, terminateInfo5);
        assertEquals(terminateInfo2, terminateInfo3);
        assertEquals(terminateInfo2, terminateInfo4);
        assertEquals(terminateInfo2, terminateInfo5);
        assertEquals(terminateInfo3, terminateInfo4);
        assertEquals(terminateInfo3, terminateInfo5);
        assertEquals(terminateInfo4, terminateInfo5);
    }

    @Test
    public void awaitTermination() throws Exception {
        final TerminationAwait terminationAwait = new TerminationAwait(serviceContainer);
        final Thread thread = new Thread(terminationAwait);
        thread.start();
        assertTrue(thread.isAlive());
        shutdownContainer();
        
        thread.join();
        assertNull(terminationAwait.getException());
    }

    @Test
    public void awaitTerminationAfterShutdown() throws Exception {
        final TerminationAwait terminationAwait = new TerminationAwait(serviceContainer);
        final Thread thread = new Thread(terminationAwait);
        shutdownContainer();
        thread.start();
        thread.join();
        assertNull(terminationAwait.getException());
    }

    @Test
    public void multipleAwaitTerminationThread() throws Exception {
        final TerminationAwait terminationAwait1 = new TerminationAwait(serviceContainer);
        final TerminationAwait terminationAwait2 = new ConfigurableTerminationAwait(serviceContainer, 10l, TimeUnit.SECONDS);
        final TerminationAwait terminationAwait3 = new ConfigurableTerminationAwait(serviceContainer, 10000000, TimeUnit.MILLISECONDS);
        final TerminationAwait terminationAwait4 = new TerminationAwait(serviceContainer);
        final Thread thread1 = new Thread(terminationAwait1);
        final Thread thread2 = new Thread(terminationAwait2);
        final Thread thread3 = new Thread(terminationAwait3);
        final Thread thread4 = new Thread(terminationAwait4);
        thread1.start();
        thread2.start();
        thread3.start();
        thread4.start();
        assertTrue(thread1.isAlive());
        assertTrue(thread2.isAlive());
        assertTrue(thread3.isAlive());
        assertTrue(thread4.isAlive());
        final ServiceController<?> serviceController1 = addService(ServiceName.of("service", "name", "1"));
        final ServiceController<?> serviceController2 = addService(ServiceName.of("service", "name", "2"));
        final ServiceController<?> serviceController3 = addService(ServiceName.of("service", "name", "3"));
        assertTrue(thread1.isAlive());
        assertTrue(thread2.isAlive());
        assertTrue(thread3.isAlive());
        assertTrue(thread4.isAlive());
        shutdownContainer();
        thread1.join();
        thread2.join();
        thread3.join();
        thread4.join();
        assertSame(State.REMOVED, serviceController1.getState());
        assertSame(State.REMOVED, serviceController2.getState());
        assertSame(State.REMOVED, serviceController3.getState());
        assertNull(terminationAwait1.getException());
        assertNull(terminationAwait2.getException());
        assertNull(terminationAwait3.getException());
        assertNull(terminationAwait4.getException());
    }

    @Test
    public void aAwaitTerminationWithTerminateListener() throws Exception {
        final TerminationAwait terminationAwait1 = new TerminationAwait(serviceContainer);
        final TerminationAwait terminationAwait2 = new ConfigurableTerminationAwait(serviceContainer, 10l, TimeUnit.SECONDS);
        final TestTerminateListener terminateListener = new TestTerminateListener();
        final Thread thread1 = new Thread(terminationAwait1);
        final Thread thread2 = new Thread(terminationAwait2);
        final ServiceController<?> serviceController1 = addService(ServiceName.of("service", "name", "1"));
        final ServiceController<?> serviceController2 = addService(ServiceName.of("service", "name", "2"));
        final ServiceController<?> serviceController3 = addService(ServiceName.of("service", "name", "3"));
        thread1.start();
        thread2.start();
        serviceContainer.addTerminateListener(terminateListener);
        shutdownContainer();
        thread1.join();
        thread2.join();
        assertNull(terminationAwait1.getException());
        assertNull(terminationAwait2.getException());
        assertTerminateListenerInfo(terminateListener.getTerminateInfo());
        assertSame(State.REMOVED, serviceController1.getState());
        assertSame(State.REMOVED, serviceController2.getState());
        assertSame(State.REMOVED, serviceController3.getState());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void editContainerAfterShutdown() {
        final ServiceName serviceName1 = ServiceName.of("1");
        final ServiceName serviceName2 = ServiceName.of("2");
        final ServiceName serviceName3 = ServiceName.of("3");
        final ServiceName serviceName4 = ServiceName.of("4");
        final ServiceBuilder<Void> builderFromContainer = serviceContainer.addService(serviceName1, Service.NULL);

        serviceContainer.addListener(testListener);
        serviceContainer.addDependency(serviceName3);
        shutdownContainer();

        try {
            serviceContainer.addDependency(new ArrayList<ServiceName>());
            fail ("IllegalStateException expected");
        } catch (IllegalStateException e) {}

        try {
            serviceContainer.addDependency(serviceName2);
            fail ("IllegalStateException expected");
        } catch (IllegalStateException e) {}

        try {
            serviceContainer.addDependency(serviceName3, serviceName4);
            fail ("IllegalStateException expected");
        } catch (IllegalStateException e) {}

        try {
            serviceContainer.addListener(new ArrayList<ServiceListener<Object>>());
            fail ("IllegalStateException expected");
        } catch (IllegalStateException e) {}

        try {
            serviceContainer.addListener(new TestServiceListener());
            fail ("IllegalStateException expected");
        } catch (IllegalStateException e) {}

        try {
            serviceContainer.addListener(new TestServiceListener(), new TestServiceListener());
            fail ("IllegalStateException expected");
        } catch (IllegalStateException e) {}
        
        try {
            serviceContainer.addService(serviceName4, Service.NULL);
            fail ("IllegalStateException expected");
        } catch (IllegalStateException e) {}

        try {
            serviceContainer.addServiceValue(serviceName3, Values.<Service<Void>>nullValue());
            fail ("IllegalStateException expected");
        } catch (IllegalStateException e) {}

        // listeners can be read without any problems
        Set<ServiceListener<Object>> listeners = serviceContainer.getListeners();
        assertNotNull(listeners);
        assertEquals(1, listeners.size());
        assertSame(testListener, listeners.iterator().next());

        // the same goes with dependencies set
        Set<ServiceName> dependencies = serviceContainer.getDependencies();
        assertNotNull(dependencies);
        assertEquals(1, dependencies.size());
        assertSame(serviceName3, dependencies.iterator().next());

        // we can also invoke any method on the serviceBuilder...
        builderFromContainer.addAliases(serviceName2, serviceName3, serviceName4);
        builderFromContainer.addDependencies(new ArrayList<ServiceName>());
        builderFromContainer.addDependencies(serviceName1);
        builderFromContainer.addDependency(serviceName4);
        // ... as long as we don't try to install it
        try {
            builderFromContainer.install();
            fail ("IllegalStateException expected");
        } catch (IllegalStateException e) {}
    }

    private final ServiceController<?> addService(ServiceName serviceName) throws Exception {
        Future<ServiceController<?>> serviceStart = testListener.expectServiceStart(serviceName);
        serviceContainer.addService(serviceName, Service.NULL).addListener(testListener).install();
        return assertController(serviceName, serviceStart);
    }

    private static final void assertTerminateListenerInfo(TerminateListener.Info terminateInfo) {
        assertNotNull("TerminateInfo is null", terminateInfo);
        final long initiated = terminateInfo.getShutdownInitiated();
        final long completed = terminateInfo.getShutdownCompleted();
        assertTrue("TerminateInto.shutdownInitiated is not positive long: " + initiated, initiated > 0);
        assertTrue("TerminateInto.shutdownCompleted is not positive long: " + completed, completed > 0);
        assertTrue("Elapsed shtudown time is not a positive long", completed - initiated > 0);
    }

    private static class TerminationAwait implements Runnable {

        protected ServiceContainer serviceContainer;
        private Exception exception;

        public TerminationAwait(ServiceContainer serviceContainer) {
            this.serviceContainer = serviceContainer;
        }

        public Exception getException() {
            return exception;
        }

        @Override
        public void run() {
            try {
                await();
            } catch (Exception e) {
                exception = e;
            }
        }

        protected void await() throws InterruptedException {
            serviceContainer.awaitTermination();
        }
    }

    private static class ConfigurableTerminationAwait extends TerminationAwait{

        private long time;
        private TimeUnit unit; 

        public ConfigurableTerminationAwait(ServiceContainer serviceContainer, long time, TimeUnit unit) {
            super(serviceContainer);
            this.time = time;
            this.unit = unit;
        }

        @Override
        protected void await() throws InterruptedException {
            serviceContainer.awaitTermination(time, unit);
        }
    }

    private static final class TestTerminateListener implements TerminateListener {

        private volatile CountDownLatch countDown = new CountDownLatch(1);
        private TerminateListener.Info terminateInfo = null;

        @Override
        public void handleTermination(Info info) {
            terminateInfo = info;
            countDown.countDown();
        }
        
        public TerminateListener.Info getTerminateInfo() throws InterruptedException {
            countDown.await(60, TimeUnit.SECONDS);
            return terminateInfo;
        }
    }
}
