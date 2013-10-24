/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;
import static org.junit.Assert.assertSame;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.jboss.msc.inject.SetMethodInjector;
import org.jboss.msc.service.ServiceController.Mode;
import org.jboss.msc.service.ServiceController.State;
import org.jboss.msc.util.TestServiceListener;
import org.jboss.msc.value.Values;
import org.junit.Before;
import org.junit.Test;

/**
 * Test for {@link LifecycleContext} implementations.
 * 
 * @author <a href="mailto:flavia.rainone@jboss.com">Flavia Rainone</a>
 */
public class LifecycleContextTestCase extends AbstractServiceTest{

    private static final ServiceName serviceName = ServiceName.of("context", "service");
    private static final TestServiceListener testListener = new TestServiceListener();

    private static final SetMethodInjector<Integer> getValueInjector(ContextService contextService) throws Exception {
        return new SetMethodInjector<Integer>(Values.immediateValue(contextService), ContextService.class.getDeclaredMethod("setValue", Integer.class));
    }

    @Before
    public void beforeTest() {
        serviceContainer.addListener(testListener);
    }

    @Test
    public void testAsynchronousStart() throws Exception {
        final ContextService contextService = new ContextService(true, false);

        final StartService startService = new StartService(contextService);
        final Thread startServiceThread = new Thread(startService);
        startServiceThread.start();

        final Future<ServiceController<?>> serviceStart = testListener.expectServiceStart(serviceName);
        final ServiceController<?> contextServiceController = serviceContainer.addService(serviceName, contextService)
            .addInjection(getValueInjector(contextService), 15)
            .install();
        startServiceThread.join();
        assertController(serviceName, contextServiceController);
        assertController(contextServiceController, serviceStart);
        assertEquals(Integer.valueOf(15), contextService.getValue());

        startService.assertNoError();

        final Future<ServiceController<?>> serviceStop = testListener.expectServiceStop(serviceName);
        contextServiceController.setMode(Mode.NEVER);
        assertController(contextServiceController, serviceStop);
        assertNull(contextService.getValue());
    }

    @Test
    public void testAsynchronousStartFailure() throws Exception {
        final ContextService contextService = new ContextService(true, false);

        final StartService startService = new StartService(contextService, true);
        final Thread startServiceThread = new Thread(startService);
        startServiceThread.start();

        final Future<StartException> serviceFailure = testListener.expectServiceFailure(serviceName);
        final ServiceController<?> contextServiceController = serviceContainer.addService(serviceName, contextService)
            .addInjection(getValueInjector(contextService), 20)
            .install();
        startServiceThread.join();
        assertController(serviceName, contextServiceController);
        assertFailure(contextServiceController, serviceFailure);
        assertEquals(Integer.valueOf(20), contextService.getValue());

        startService.assertNoError();

        // todo it would be nice to check if the contextService value is uninjected when we set the mode to NEVER
    }

    @Test
    public void testAsynchronousStop() throws Exception {
        final ContextService contextService = new ContextService(false, true);

        final Future<ServiceController<?>> serviceStart = testListener.expectServiceStart(serviceName);
        final ServiceController<?> contextServiceController = serviceContainer.addService(serviceName, contextService)
            .addInjection(getValueInjector(contextService), 10)
            .install();
        assertController(serviceName, contextServiceController);
        assertController(contextServiceController, serviceStart);
        assertEquals(Integer.valueOf(10), contextService.getValue());

        final StopService stopService = new StopService(contextService);
        final Thread stopServiceThread = new Thread(stopService);
        stopServiceThread.start();

        final Future<ServiceController<?>> serviceStop = testListener.expectServiceStop(serviceName);
        contextServiceController.setMode(Mode.NEVER);
        stopServiceThread.join();
        assertController(contextServiceController, serviceStop);
        assertNull(contextService.getValue());

        stopService.assertNoError();
    }

    @Test
    public void testAsynchronousStartAndStop() throws Exception {
        final ContextService contextService = new ContextService(true, true);

        final StartService startService = new StartService(contextService);
        final Thread startServiceThread = new Thread(startService);
        startServiceThread.start();

        final Future<ServiceController<?>> serviceStart = testListener.expectServiceStart(serviceName);
        final ServiceController<?> contextServiceController = serviceContainer.addService(serviceName, contextService)
            .addInjection(getValueInjector(contextService), 21)
            .install();
        startServiceThread.join();
        assertController(serviceName, contextServiceController);
        assertController(contextServiceController, serviceStart);
        assertEquals(Integer.valueOf(21), contextService.getValue());
        startService.assertNoError();

        final StopService stopService = new StopService(contextService);
        final Thread stopServiceThread = new Thread(stopService);
        stopServiceThread.start();

        final Future<ServiceController<?>> serviceStop = testListener.expectServiceStop(serviceName);
        contextServiceController.setMode(Mode.NEVER);
        stopServiceThread.join();
        assertController(contextServiceController, serviceStop);
        assertNull(contextService.getValue());

        stopService.assertNoError();
    }

    @Test
    public void testAsynchronousStartWithLifecycleContextCallback() throws Exception {
        final ContextService contextService = new ContextService(true, false);

        final StartServiceWithRemoveListener startService = new StartServiceWithRemoveListener(serviceContainer, contextService);
        final Thread startServiceThread = new Thread(startService);
        startServiceThread.start();

        final Future<ServiceController<?>> serviceStart = testListener.expectServiceStart(serviceName);
        final ServiceController<?> contextServiceController = serviceContainer.addService(serviceName, contextService)
            .addInjection(getValueInjector(contextService), 15)
            .install();
        startServiceThread.join();
        assertController(serviceName, contextServiceController);
        assertController(contextServiceController, serviceStart);
        assertEquals(Integer.valueOf(15), contextService.getValue());

        startService.assertNoError();

        final Future<ServiceController<?>> serviceStop = testListener.expectServiceStop(serviceName);
        contextServiceController.setMode(Mode.NEVER);
        assertController(contextServiceController, serviceStop);
        assertNull(contextService.getValue());
    }

    @Test
    public void testInvalidStartComplete() throws Exception {
        final ContextService contextService = new ContextService(false, false);

        final Future<ServiceController<?>> serviceStart = testListener.expectServiceStart(serviceName);
        final ServiceController<?> contextServiceController = serviceContainer.addService(serviceName, contextService)
            .addInjection(getValueInjector(contextService), 30)
            .install();
        assertController(serviceName, contextServiceController);
        assertController(contextServiceController, serviceStart);
        assertEquals(Integer.valueOf(30), contextService.getValue());

        final StartContext startContext = contextService.getStartContext();
        startContext.asynchronous();
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
        final ContextService contextService = new ContextService(false, false);

        final Future<ServiceController<?>> serviceStart = testListener.expectServiceStart(serviceName);
        final ServiceController<?> contextServiceController = serviceContainer.addService(serviceName, contextService)
            .addInjection(getValueInjector(contextService), 31)
            .install();
        assertController(serviceName, contextServiceController);
        assertController(contextServiceController, serviceStart);
        assertEquals(Integer.valueOf(31), contextService.getValue());

        final Future<ServiceController<?>> serviceStop = testListener.expectServiceStop(serviceName);
        contextServiceController.setMode(Mode.NEVER);
        assertController(contextServiceController, serviceStop);
        assertNull(contextService.getValue());

        final StopContext stopContext = contextService.getStopContext();

        stopContext.asynchronous();
        try {
            stopContext.complete();
            fail ("IllegalStateException expected");
        } catch (IllegalStateException e) {}
    }

    @Test
    public void startContextExecuteComand() throws Exception {
        final ContextService contextService = new ContextService(false, false);

        final Future<ServiceController<?>> serviceStart = testListener.expectServiceStart(serviceName);
        final ServiceController<?> contextServiceController = serviceContainer.addService(serviceName, contextService)
            .addInjection(getValueInjector(contextService), 56)
            .install();
        assertController(serviceName, contextServiceController);
        assertController(contextServiceController, serviceStart);
        assertEquals(Integer.valueOf(56), contextService.getValue());

        final StartContext startContext = contextService.getStartContext();
        final DummyRunnable runnable = new DummyRunnable();
        startContext.execute(runnable);
        assertTrue(runnable.isRun());
    }

    @Test
    public void stopContextExecuteComand() throws Exception {
        final ContextService contextService = new ContextService(false, false);

        final Future<ServiceController<?>> serviceStart = testListener.expectServiceStart(serviceName);
        final ServiceController<?> contextServiceController = serviceContainer.addService(serviceName, contextService)
            .addInjection(getValueInjector(contextService), 100)
            .install();
        assertController(serviceName, contextServiceController);
        assertController(contextServiceController, serviceStart);
        assertEquals(Integer.valueOf(100), contextService.getValue());

        final Future<ServiceController<?>> serviceStop = testListener.expectServiceStop(serviceName);
        contextServiceController.setMode(Mode.NEVER);
        assertController(contextServiceController, serviceStop);
        assertNull(contextService.getValue());

        final StopContext stopContext = contextService.getStopContext();
        final DummyRunnable runnable = new DummyRunnable();
        stopContext.execute(runnable);
        assertTrue(runnable.isRun());
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

    private static class StartServiceWithRemoveListener implements Runnable {

        private final ContextService contextService;
        private final ServiceContainer serviceContainer;
        private Throwable runError = null;

        public StartServiceWithRemoveListener(ServiceContainer container, ContextService contextService) {
            this.serviceContainer = container;
            this.contextService = contextService;
        }

        @Override
        public void run() {
            final StartContext context;
            try {
                context = contextService.getStartContext();
                assertNotNull(context);
                final ServiceName serviceAName = ServiceName.of("A");
                final ServiceName serviceBName = ServiceName.of("B");
                final ServiceName serviceCName = ServiceName.of("C");
                final MultipleRemoveListener<?> removeListener = MultipleRemoveListener.create(context);
                final ServiceController<?> serviceA = serviceContainer.addService(serviceAName, Service.NULL).addListener(removeListener).install();
                final ServiceController<?> serviceB = serviceContainer.addService(serviceBName, Service.NULL).addListener(removeListener).install();
                final ServiceController<?> serviceC = serviceContainer.addService(serviceCName, Service.NULL).addListener(removeListener).install();

                serviceA.setMode(Mode.REMOVE);
                serviceB.setMode(Mode.REMOVE);
                serviceC.setMode(Mode.REMOVE);
                removeListener.done();
            } catch (Throwable t) {
                runError = t;
                contextService.getStartContext().complete();
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

    public static class ContextService implements Service<Integer> {

        private final boolean startAsynchronous;
        private final boolean stopAsynchronous;
        private StopContext stopContext;
        private StartContext startContext;
        private CountDownLatch startLatch = new CountDownLatch(1);
        private CountDownLatch stopLatch = new CountDownLatch(1);
        private Integer value;

        ContextService(boolean startAsynchronous, boolean stopAsynchronous) {
            this.startAsynchronous = startAsynchronous;
            this.stopAsynchronous = stopAsynchronous;
        }
        
        public void setValue(Integer newValue) {
            value = newValue;
        }

        @Override
        public Integer getValue() throws IllegalStateException, IllegalArgumentException {
            return value;
        }

        @Override
        public void start(StartContext context) throws StartException {
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
