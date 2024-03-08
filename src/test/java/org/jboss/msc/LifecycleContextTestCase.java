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

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;
import static org.junit.Assert.assertSame;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import org.jboss.msc.service.LifecycleContext;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.service.ServiceController.Mode;
import org.jboss.msc.service.ServiceController.State;
import org.junit.Before;
import org.junit.Test;

/**
 * Test for {@link LifecycleContext} implementations.
 * 
 * @author <a href="mailto:flavia.rainone@jboss.com">Flavia Rainone</a>
 */
public class LifecycleContextTestCase extends AbstractServiceTest{

    private static final ServiceName serviceName = ServiceName.of("context", "service");
    private static final TestLifecycleListener testListener = new TestLifecycleListener();

    @Before
    public void beforeTest() {
        serviceContainer.addListener(testListener);
    }

    @Test
    public void testAsynchronousStart() throws Exception {
        final ServiceBuilder<?> sb = serviceContainer.addService();
        Consumer<Integer> providedValue = sb.provides(serviceName);
        final ContextService contextService = new ContextService(providedValue, 15, true, false);
        final StartService startService = new StartService(contextService);
        final Thread startServiceThread = new Thread(startService);
        startServiceThread.start();
        sb.setInstance(contextService);
        final ServiceController<?> contextServiceController = sb.install();
        serviceContainer.awaitStability();
        startServiceThread.join();

        assertSame(contextServiceController, serviceContainer.getRequiredService(serviceName));
        assertEquals(1, testListener.upValues().size());
        assertTrue(testListener.upValues().contains(serviceName));
        startService.assertNoError();

        contextServiceController.setMode(Mode.NEVER);
        serviceContainer.awaitStability();

        assertEquals(1, testListener.downValues().size());
        assertTrue(testListener.downValues().contains(serviceName));
    }

    @Test
    public void testAsynchronousStartFailure() throws Exception {
        final ServiceBuilder<?> sb = serviceContainer.addService();
        Consumer<Integer> providedValue = sb.provides(serviceName);
        final ContextService contextService = new ContextService(providedValue, 20, true, false);
        final StartService startService = new StartService(contextService, true);
        final Thread startServiceThread = new Thread(startService);
        startServiceThread.start();
        sb.setInstance(contextService);
        final ServiceController<?> contextServiceController = sb.install();
        serviceContainer.awaitStability();
        startServiceThread.join();

        assertSame(contextServiceController, serviceContainer.getRequiredService(serviceName));
        assertEquals(1, testListener.failedValues().size());
        assertTrue(testListener.failedValues().contains(serviceName));
        startService.assertNoError();
    }

    @Test
    public void testAsynchronousStop() throws Exception {
        final ServiceBuilder<?> sb = serviceContainer.addService();
        Consumer<Integer> providedValue = sb.provides(serviceName);
        final ContextService contextService = new ContextService(providedValue, 10, false, true);
        sb.setInstance(contextService);
        final ServiceController<?> contextServiceController = sb.install();
        serviceContainer.awaitStability();

        assertSame(contextServiceController, serviceContainer.getRequiredService(serviceName));
        assertEquals(1, testListener.upValues().size());
        assertTrue(testListener.upValues().contains(serviceName));

        final StopService stopService = new StopService(contextService);
        final Thread stopServiceThread = new Thread(stopService);
        stopServiceThread.start();

        contextServiceController.setMode(Mode.NEVER);
        stopServiceThread.join();
        serviceContainer.awaitStability();

        assertEquals(1, testListener.downValues().size());
        assertTrue(testListener.downValues().contains(serviceName));
        stopService.assertNoError();
    }

    @Test
    public void testAsynchronousStartAndStop() throws Exception {
        final ServiceBuilder<?> sb = serviceContainer.addService();
        Consumer<Integer> providedValue = sb.provides(serviceName);
        final ContextService contextService = new ContextService(providedValue, 21, true, true);
        final StartService startService = new StartService(contextService);
        final Thread startServiceThread = new Thread(startService);
        startServiceThread.start();
        sb.setInstance(contextService);
        final ServiceController<?> contextServiceController = sb.install();
        serviceContainer.awaitStability();
        startServiceThread.join();

        assertSame(contextServiceController, serviceContainer.getRequiredService(serviceName));
        assertEquals(1, testListener.upValues().size());
        assertTrue(testListener.upValues().contains(serviceName));
        startService.assertNoError();

        final StopService stopService = new StopService(contextService);
        final Thread stopServiceThread = new Thread(stopService);
        stopServiceThread.start();

        contextServiceController.setMode(Mode.NEVER);
        stopServiceThread.join();
        serviceContainer.awaitStability();

        assertEquals(1, testListener.downValues().size());
        assertTrue(testListener.downValues().contains(serviceName));
        stopService.assertNoError();
    }

    @Test
    public void testInvalidStartComplete() throws Exception {
        final ServiceBuilder<?> sb = serviceContainer.addService();
        Consumer<Integer> providedValue = sb.provides(serviceName);
        final ContextService contextService = new ContextService(providedValue, 30, false, false);
        sb.setInstance(contextService);
        final ServiceController<?> contextServiceController = sb.install();
        serviceContainer.awaitStability();

        assertSame(contextServiceController, serviceContainer.getRequiredService(serviceName));
        assertEquals(1, testListener.upValues().size());
        assertTrue(testListener.upValues().contains(serviceName));

        final StartContext startContext = contextService.getStartContext();
        try {
            startContext.asynchronous();
            fail ("IllegalStateException expected");
        } catch (IllegalStateException e) {}

        try {
            startContext.complete();
            fail ("IllegalStateException expected");
        } catch (IllegalStateException e) {}

        try {
            startContext.failed(null);
            fail ("IllegalStateException expected");
        } catch (IllegalStateException e) {}

        try {
            startContext.failed(new StartException());
            fail ("IllegalStateException expected");
        } catch (IllegalStateException e) {}
    }

    @Test
    public void testInvalidStopComplete() throws Exception {
        final ServiceBuilder<?> sb = serviceContainer.addService();
        Consumer<Integer> providedValue = sb.provides(serviceName);
        final ContextService contextService = new ContextService(providedValue, 31, false, false);
        sb.setInstance(contextService);
        final ServiceController<?> contextServiceController = sb.install();
        serviceContainer.awaitStability();

        assertSame(contextServiceController, serviceContainer.getRequiredService(serviceName));
        assertEquals(1, testListener.upValues().size());
        assertTrue(testListener.upValues().contains(serviceName));

        contextServiceController.setMode(Mode.NEVER);
        serviceContainer.awaitStability();

        assertEquals(1, testListener.downValues().size());
        assertTrue(testListener.downValues().contains(serviceName));

        final StopContext stopContext = contextService.getStopContext();

        try {
            stopContext.asynchronous();
            fail ("IllegalStateException expected");
        } catch (IllegalStateException e) {}
        try {
            stopContext.complete();
            fail ("IllegalStateException expected");
        } catch (IllegalStateException e) {}
    }

    @Test
    public void startContextExecuteCommand() throws Exception {
        final ServiceBuilder<?> sb = serviceContainer.addService();
        Consumer<Integer> providedValue = sb.provides(serviceName);
        final ContextService contextService = new ContextService(providedValue, 56, false, false);
        sb.setInstance(contextService);
        final ServiceController<?> contextServiceController = sb.install();
        serviceContainer.awaitStability();

        assertSame(contextServiceController, serviceContainer.getRequiredService(serviceName));
        assertEquals(1, testListener.upValues().size());
        assertTrue(testListener.upValues().contains(serviceName));

        final StartContext startContext = contextService.getStartContext();
        try {
            startContext.execute(new DummyRunnable());
            fail("IllegalStateException expected");
        } catch (IllegalStateException ignored) {}
    }

    @Test
    public void stopContextExecuteCommand() throws Exception {
        final ServiceBuilder<?> sb = serviceContainer.addService();
        Consumer<Integer> providedValue = sb.provides(serviceName);
        final ContextService contextService = new ContextService(providedValue, 100, false, false);
        sb.setInstance(contextService);
        final ServiceController<?> contextServiceController = sb.install();
        serviceContainer.awaitStability();

        assertSame(contextServiceController, serviceContainer.getRequiredService(serviceName));
        assertEquals(1, testListener.upValues().size());
        assertTrue(testListener.upValues().contains(serviceName));

        contextServiceController.setMode(Mode.NEVER);
        serviceContainer.awaitStability();

        assertEquals(1, testListener.downValues().size());
        assertTrue(testListener.downValues().contains(serviceName));

        final StopContext stopContext = contextService.getStopContext();
        try {
            stopContext.execute(new DummyRunnable());
            fail("IllegalStateException expected");
        } catch (IllegalStateException ignored) {}
    }

    private static class StartService implements Runnable {

        private final ContextService contextService;
        private Throwable runError = null;
        private final boolean fail;

        public StartService(ContextService contextService) {
            this(contextService, false);
        }

        public StartService(ContextService contextService, boolean fail) {
            this.contextService = contextService;
            this.fail = fail;
        }

        @Override
        public void run() {
            final StartContext context;
            try {
                context = contextService.getStartContext();
                assertNotNull(context);
                final ServiceController<?> serviceController = context.getController();
                assertNotNull(serviceController);
                assertSame(State.STARTING, serviceController.getState());
                if (fail) {
                    context.failed(null);
                }
                else {
                    context.complete();
                }
                try {
                    context.complete();
                    fail("IllegalStateException expected");
                } catch (IllegalStateException e) {}
                try {
                    context.failed(new StartException());
                    fail("IllegalStateException expected");
                } catch (IllegalStateException e) {}
            } catch (Throwable t) {
                runError = t;
                contextService.getStartContext().complete();
            }
        }

        public void assertNoError() {
            assertNull("Unexpected throwable " + runError, runError);
        }
    }

    private static class StopService implements Runnable {

        private final ContextService contextService;
        private Throwable runError = null;

        public StopService(ContextService contextService) {
            this.contextService = contextService;
        }

        @Override
        public void run() {
            final StopContext context;
            try {
                context = contextService.getStopContext();
                assertNotNull(context);
                final ServiceController<?> serviceController = context.getController();
                assertNotNull(serviceController);
                assertSame(State.STOPPING, serviceController.getState());
                context.complete();
                try {
                    context.complete();
                    fail("IllegalStateException expected");
                } catch (IllegalStateException e) {}
            } catch (Throwable t) {
                runError = t;
                contextService.getStopContext().complete();
            }
        }

        public void assertNoError() {
            assertNull("Unexpected throwable " + runError, runError);
        }
    }

    private static class DummyRunnable implements Runnable {
        private boolean run = false;
        private final CountDownLatch runLatch = new CountDownLatch(1);

        @Override
        public void run() {
            run = true;
            runLatch.countDown();
        }

        public boolean isRun() throws Exception {
            runLatch.await(3000, TimeUnit.MILLISECONDS);
            return run;
        }
    }

    public static class ContextService implements Service {

        private final boolean startAsynchronous;
        private final boolean stopAsynchronous;
        private volatile StopContext stopContext;
        private volatile StartContext startContext;
        private final CountDownLatch startLatch = new CountDownLatch(1);
        private final CountDownLatch stopLatch = new CountDownLatch(1);
        private final Consumer<Integer> providedValue;
        private final int initialValue;

        ContextService(Consumer<Integer> providedValue, int initialValue, boolean startAsynchronous, boolean stopAsynchronous) {
            this.startAsynchronous = startAsynchronous;
            this.stopAsynchronous = stopAsynchronous;
            this.providedValue = providedValue;
            this.initialValue = initialValue;
        }
        
        @Override
        public void start(StartContext context) throws StartException {
            providedValue.accept(initialValue);
            if (startAsynchronous) {
                context.asynchronous();
            }
            startContext = context;
            startLatch.countDown();
        }

        StartContext getStartContext() {
            try {
                startLatch.await(10000, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            return startContext;
        }

        @Override
        public void stop(StopContext context) {
            providedValue.accept(null);
            if (stopAsynchronous) {
                context.asynchronous();
            }
            stopContext = context;
            stopLatch.countDown();
        }

        StopContext getStopContext() {
            try {
                stopLatch.await(10000, TimeUnit.MILLISECONDS);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            return stopContext;
        }
    }
    
}
