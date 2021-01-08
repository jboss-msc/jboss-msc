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

package org.jboss.msc.multi_value_services;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.jboss.msc.service.AbstractServiceTest;
import org.jboss.msc.service.LifecycleEvent;
import org.jboss.msc.service.LifecycleListener;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StabilityMonitor;

import java.util.function.Consumer;
import java.util.concurrent.CountDownLatch;
import java.util.ConcurrentModificationException;

import org.junit.Test;

/**
 * <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
public class WrongUsageOfNewServicesAPITestCase extends AbstractServiceTest {

    private static final ServiceName ID = ServiceName.of("id");
    private static final ServiceName FOO = ServiceName.of("foo");
    private static final ServiceName BAR = ServiceName.of("bar");
    private static final ServiceName BLA = ServiceName.of("bla");

    /**
     * It is forbidden to call ServiceBuilder.install() twice.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void installCalledTwice() {
        ServiceBuilder sb = serviceContainer.addService(ID);
        sb.provides(FOO);
        sb.setInstance(null);
        assertNotNull(sb.install());
        try {
            sb.install();
            fail("IllegalStateException expected");
        } catch (IllegalStateException ignored) {}
    }

    /**
     * It is forbidden to call ServiceBuilder.setInstance()
     * after ServiceBuilder.install() method call.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void setInstanceCalledAfterInstall() {
        ServiceBuilder sb = serviceContainer.addService(ID);
        sb.provides(FOO);
        assertNotNull(sb.install());
        try {
            sb.setInstance(null);
            fail("IllegalStateException expected");
        } catch (IllegalStateException ignored) {}
    }

    /**
     * It is forbidden to call ServiceBuilder.setInstance() twice.
     */
    @SuppressWarnings("unchecked")
    @Test
    public void setInstanceCalledTwice() {
        ServiceBuilder sb = serviceContainer.addService(ID);
        sb.provides(FOO);
        sb.setInstance(null);
        try {
            sb.setInstance(null);
            fail("IllegalStateException expected");
        } catch (IllegalStateException ignored) {}
    }

    /**
     * It is forbidden to call ServiceBuilder.setInitialMode()
     * after ServiceBuilder.install() method call.
     */
    @Test
    public void setInitialModeCalledAfterInstall() {
        ServiceBuilder sb = serviceContainer.addService(ID);
        sb.provides(FOO);
        assertNotNull(sb.install());
        try {
            sb.setInitialMode(ServiceController.Mode.LAZY);
            fail("IllegalStateException expected");
        } catch (IllegalStateException ignored) {}
    }

    /**
     * It is forbidden to call ServiceBuilder.setInitialMode() with null parameter.
     */
    @Test
    public void setInitialModeCalledWithNullParam() {
        ServiceBuilder sb = serviceContainer.addService(ID);
        try {
            sb.setInitialMode(null);
            fail("NullPointerException expected");
        } catch (NullPointerException ignored) {}
    }

    /**
     * It is forbidden to call ServiceBuilder.setInitialMode() with REMOVE parameter.
     */
    @Test
    public void setInitialModeCalledWithRemoveParam() {
        ServiceBuilder sb = serviceContainer.addService(ID);
        try {
            sb.setInitialMode(ServiceController.Mode.REMOVE);
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException ignored) {}
    }

    /**
     * It is forbidden to call ServiceBuilder.setInitialMode() twice.
     */
    @Test
    public void setInitialModeCalledTwice() {
        ServiceBuilder sb = serviceContainer.addService(ID);
        sb.setInitialMode(ServiceController.Mode.LAZY);
        try {
            sb.setInitialMode(ServiceController.Mode.ACTIVE);
            fail("IllegalStateException expected");
        } catch (IllegalStateException ignored) {}
    }

    /**
     * It is forbidden to call ServiceBuilder.addMonitor()
     * after ServiceBuilder.install() method call.
     */
    @Test
    public void addMonitorCalledAfterInstall() {
        ServiceBuilder sb = serviceContainer.addService(ID);
        sb.provides(FOO);
        assertNotNull(sb.install());
        try {
            sb.addMonitor(new StabilityMonitor());
            fail("IllegalStateException expected");
        } catch (IllegalStateException ignored) {}
    }

    /**
     * It is forbidden to call ServiceBuilder.addListener()
     * after ServiceBuilder.install() method call.
     */
    @Test
    public void addListenerCalledAfterInstall() {
        ServiceBuilder sb = serviceContainer.addService(ID);
        sb.provides(FOO);
        assertNotNull(sb.install());
        try {
            sb.addListener(UselessListener.INSTANCE);
            fail("IllegalStateException expected");
        } catch (IllegalStateException ignored) {}
    }

    /**
     * It is forbidden to call ServiceBuilder.addMonitor() with null parameter.
     */
    @Test
    public void addMonitorCalledWithNullParam() {
        ServiceBuilder sb = serviceContainer.addService(ID);
        try {
            sb.addMonitor(null);
            fail("NullPointerException expected");
        } catch (NullPointerException ignored) {}
    }

    /**
     * It is forbidden to call ServiceBuilder.addListener() with null parameter.
     */
    @Test
    public void addListenerCalledWithNullParam() {
        ServiceBuilder sb = serviceContainer.addService(ID);
        try {
            sb.addListener((LifecycleListener)null);
            fail("NullPointerException expected");
        } catch (NullPointerException ignored) {}
    }

    /**
     * It is forbidden to call ServiceBuilder.requires()
     * after ServiceBuilder.install() method call.
     */
    @Test
    public void requiresCalledAfterInstall() {
        ServiceBuilder sb = serviceContainer.addService(ID);
        sb.provides(FOO);
        assertNotNull(sb.install());
        try {
            sb.requires(BAR);
            fail("IllegalStateException expected");
        } catch (IllegalStateException ignored) {}
    }

    /**
     * It is forbidden to call ServiceBuilder.provides()
     * after ServiceBuilder.install() method call.
     */
    @Test
    public void providesCalledAfterInstall() {
        ServiceBuilder sb = serviceContainer.addService(ID);
        sb.provides(FOO);
        assertNotNull(sb.install());
        try {
            sb.provides(BAR);
            fail("IllegalStateException expected");
        } catch (IllegalStateException ignored) {}
    }

    /**
     * It is forbidden to call ServiceBuilder.requires() with null parameter.
     */
    @Test
    public void requiresCalledWithNullParam() {
        ServiceBuilder sb = serviceContainer.addService(ID);
        try {
            sb.requires(null);
            fail("NullPointerException expected");
        } catch (NullPointerException ignored) {}
    }

    /**
     * It is forbidden to call ServiceBuilder.provides() with null parameter.
     */
    @Test
    public void providesCalledWithNullParam() {
        ServiceBuilder sb = serviceContainer.addService(ID);
        try {
            sb.provides((ServiceName)null);
            fail("NullPointerException expected");
        } catch (NullPointerException ignored) {}
        try {
            sb.provides(FOO, null, BAR);
            fail("NullPointerException expected");
        } catch (NullPointerException ignored) {}
    }

    /**
     * It is forbidden to call ServiceBuilder.provides() with same parameter twice.
     */
    @Test
    public void providesCalledTwiceWithSameParam() {
        ServiceBuilder sb = serviceContainer.addService(ID);
        sb.provides(FOO);
        try {
            sb.provides(FOO);
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException ignored) {}
        try {
            sb.provides(BAR, FOO);
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException ignored) {}

        sb = serviceContainer.addService(ID);
        sb.provides(FOO, BAR);
        try {
            sb.provides(FOO);
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException ignored) {}
        try {
            sb.provides(BAR);
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException ignored) {}
        try {
            sb.provides(BLA, FOO);
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException ignored) {}
        try {
            sb.provides(BLA, BAR);
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException ignored) {}
    }

    /**
     * It is forbidden to call ServiceBuilder.requires()
     * and ServiceBuilder.provides() with same parameter.
     */
    @Test
    public void requiresCalledWithSameParamAsProvides() {
        ServiceBuilder sb = serviceContainer.addService(ID);
        sb.requires(FOO);
        sb.provides(BAR);
        try {
            sb.requires(BAR);
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException ignored) {}

        sb = serviceContainer.addService(ID);
        sb.requires(FOO);
        sb.provides(BAR, BLA);
        try {
            sb.requires(BLA);
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException ignored) {}

        sb = serviceContainer.addService(ID);
        sb.requires(BAR);
        sb.provides(FOO);
        try {
            sb.provides(BLA, BAR);
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException ignored) {}
    }

    /**
     * It is forbidden to call Injector.inject() method
     * outside of service start lifecycle method.
     */
    @Test
    public void consumerAcceptCalledOutsideOfServiceStart() throws Exception {
        StabilityMonitor monitor = new StabilityMonitor();
        ServiceBuilder<?> sb = serviceContainer.addService(ID);
        sb.addMonitor(monitor);
        Consumer<Object> providedValue = sb.provides(FOO);
        // trying to set value before service is started
        try {
            providedValue.accept(null);
            fail("IllegalStateException expected");
        } catch (IllegalStateException ignored) {}
        ServiceController controller = sb.install();
        assertNotNull(controller);
        monitor.awaitStability();
        // trying to set value when service is up and running
        try {
            providedValue.accept(null);
            fail("IllegalStateException expected");
        } catch (IllegalStateException ignored) {}
        controller.setMode(ServiceController.Mode.NEVER);
        monitor.awaitStability();
        // trying to set value after service have been stopped
        try {
            providedValue.accept(null);
            fail("IllegalStateException expected");
        } catch (IllegalStateException ignored) {}
    }

    /**
     * It is forbidden to call ServiceBuilder.requires() method from another thread.
     */
    @Test
    public void requiresFromAnotherThread() throws Exception {
        ServiceBuilder sb = serviceContainer.addService(ID);
        sb.requires(BLA);
        sb.provides(BAR);
        ForkedThread thread = new ForkedThread(sb, "requires");
        thread.start();
        Exception exception = thread.getException();
        assertNotNull(exception);
        assertTrue(exception instanceof ConcurrentModificationException);
    }

    /**
     * It is forbidden to call ServiceBuilder.provides() method from another thread.
     */
    @Test
    public void providesCalledFromAnotherThread() throws Exception {
        ServiceBuilder sb = serviceContainer.addService(ID);
        sb.requires(BLA);
        sb.provides(BAR);
        ForkedThread thread = new ForkedThread(sb, "provides");
        thread.start();
        Exception exception = thread.getException();
        assertNotNull(exception);
        assertTrue(exception instanceof ConcurrentModificationException);
    }

    /**
     * It is forbidden to call ServiceBuilder.setInitialMode() method from another thread.
     */
    @Test
    public void setInitialModeCalledFromAnotherThread() throws Exception {
        ServiceBuilder sb = serviceContainer.addService(ID);
        sb.requires(BLA);
        sb.provides(BAR);
        ForkedThread thread = new ForkedThread(sb, "setInitialMode");
        thread.start();
        Exception exception = thread.getException();
        assertNotNull(exception);
        assertTrue(exception instanceof ConcurrentModificationException);
    }

    /**
     * It is forbidden to call ServiceBuilder.setInstance() method from another thread.
     */
    @Test
    public void setInstanceCalledFromAnotherThread() throws Exception {
        ServiceBuilder sb = serviceContainer.addService(ID);
        sb.requires(BLA);
        sb.provides(BAR);
        ForkedThread thread = new ForkedThread(sb, "setInstance");
        thread.start();
        Exception exception = thread.getException();
        assertNotNull(exception);
        assertTrue(exception instanceof ConcurrentModificationException);
    }

    /**
     * It is forbidden to call ServiceBuilder.addListener() method from another thread.
     */
    @Test
    public void addListenerCalledFromAnotherThread() throws Exception {
        ServiceBuilder sb = serviceContainer.addService(ID);
        sb.requires(BLA);
        sb.provides(BAR);
        ForkedThread thread = new ForkedThread(sb, "addListener");
        thread.start();
        Exception exception = thread.getException();
        assertNotNull(exception);
        assertTrue(exception instanceof ConcurrentModificationException);
    }

    /**
     * It is forbidden to call ServiceBuilder.addMonitor() method from another thread.
     */
    @Test
    public void addMonitorCalledFromAnotherThread() throws Exception {
        ServiceBuilder sb = serviceContainer.addService(ID);
        sb.requires(BLA);
        sb.provides(BAR);
        ForkedThread thread = new ForkedThread(sb, "addMonitor");
        thread.start();
        Exception exception = thread.getException();
        assertNotNull(exception);
        assertTrue(exception instanceof ConcurrentModificationException);
    }

    /**
     * It is forbidden to call ServiceBuilder.install() method from another thread.
     */
    @Test
    public void installCalledFromAnotherThread() throws Exception {
        ServiceBuilder sb = serviceContainer.addService(ID);
        sb.requires(BLA);
        sb.provides(BAR);
        ForkedThread thread = new ForkedThread(sb, "install");
        thread.start();
        Exception exception = thread.getException();
        assertNotNull(exception);
        assertTrue(exception instanceof ConcurrentModificationException);
    }

    private static final class UselessListener implements LifecycleListener {
        private static final LifecycleListener INSTANCE = new UselessListener();

        @Override
        public void handleEvent(final ServiceController<?> controller, final LifecycleEvent event) {
        }
    }

    private static final class ForkedThread extends Thread {

        private final ServiceBuilder serviceBuilder;
        private final String methodName;
        private final CountDownLatch latch = new CountDownLatch(1);
        private volatile Exception exception;

        private ForkedThread(final ServiceBuilder serviceBuilder, final String methodName) {
            this.serviceBuilder = serviceBuilder;
            this.methodName = methodName;
        }

        @SuppressWarnings("unchecked")
        @Override
        public void run() {
            try {
                if (methodName.equals("install")) {
                    serviceBuilder.install();
                } else if (methodName.equals("requires")) {
                    serviceBuilder.requires(FOO);
                } else if (methodName.equals("provides")) {
                    serviceBuilder.provides(FOO);
                } else if (methodName.equals("addListener")) {
                    serviceBuilder.addListener(UselessListener.INSTANCE);
                } else if (methodName.equals("addMonitor")) {
                    serviceBuilder.addMonitor(new StabilityMonitor());
                } else if (methodName.equals("setInstance")) {
                    serviceBuilder.setInstance(null);
                } else if (methodName.equals("setInitialMode")) {
                    serviceBuilder.setInitialMode(ServiceController.Mode.LAZY);
                } else throw new UnsupportedOperationException();
            } catch (Exception t) {
                exception = t;
            } finally {
                latch.countDown();
            }
        }

        private Exception getException() throws InterruptedException {
            latch.await();
            return exception;
        }
    }

}
