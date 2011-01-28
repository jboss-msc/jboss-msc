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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;

import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.jboss.msc.service.ServiceController.Mode;
import org.jboss.msc.util.TestServiceListener;
import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

/**
 * Runs other test cases on a multi-thread environment configured with a
 * {@link ServiceContainer#ExecutorInjector}.
 * 
 * @author <a href="mailto:flavia.rainone@jboss.com">Flavia Rainone</a>
 */
@RunWith(Suite.class)
@SuiteClasses({MultipleThreadExecutorTestCase.DependencyListenersTest.class,
    MultipleThreadExecutorTestCase.OptionalDependencyListenersTest.class, 
    MultipleThreadExecutorTestCase.ServiceBuilderTest.class})
public class MultipleThreadExecutorTestCase {

    public static class DependencyListenersTest extends DependencyListenersTestCase {
        private MultipleThreadExecutor executor;
        @Before
        public void setExecutor() throws Exception {
            executor = new MultipleThreadExecutor(serviceContainer);
            executor.install();
        }

        @After
        public void unsetExecutor() throws Exception {
            executor.uninstall();
        }
    }

    public static class OptionalDependencyListenersTest extends OptionalDependencyListenersTestCase {
        private MultipleThreadExecutor executor;
        @Before
        public void setExecutor() throws Exception {
            executor = new MultipleThreadExecutor(serviceContainer);
            executor.install();
        }

        @After
        public void unsetExecutor() throws Exception {
            executor.uninstall();
        }
    }

    public static class ServiceBuilderTest extends ServiceBuilderTestCase {
        private MultipleThreadExecutor executor;
        @Before
        public void setExecutor() throws Exception {
            executor = new MultipleThreadExecutor(serviceContainer);
            executor.install();
        }

        @After
        public void unsetExecutor() throws Exception {
            executor.uninstall();
        }
    }

    private static final class MultipleThreadExecutor {
        private final ServiceContainer serviceContainer;
        private ServiceController<?> executorInjectorController;
        private TestServiceListener testListener;

        public MultipleThreadExecutor(ServiceContainer serviceContainer) {
            this.serviceContainer = serviceContainer;
        }

        public void install() throws Exception {
            final ThreadPoolExecutor executor = new ThreadPoolExecutor(3, 5, 50L, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(), new ThreadFactory() {
                public Thread newThread(final Runnable r) {
                    final Thread thread = new Thread(r);
                    thread.setDaemon(true);
                    thread.setUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
                        public void uncaughtException(final Thread t, final Throwable e) {
                            e.printStackTrace(System.err);
                        }
                    });
                    return thread;
                }
            });
            executor.allowCoreThreadTimeOut(true);
            final ServiceContainer.ExecutorInjector executorInjector = ServiceContainer.ExecutorInjector.create(serviceContainer);
            testListener = new TestServiceListener();
            final Future<ServiceController<?>> executorInjected = testListener.expectServiceStart(ServiceName.of("executor"));
            executorInjectorController = serviceContainer.addService(ServiceName.of("executor"), Service.NULL)
                .addListener(testListener).addInjection(executorInjector, executor).install();
            assertNotNull(executorInjector);
            assertSame(executorInjectorController, executorInjected.get());
        }

        public void uninstall() throws Exception {
            final Future<ServiceController<?>> executorUninjected = testListener.expectServiceStop(ServiceName.of("executor"));
            executorInjectorController.setMode(Mode.NEVER);
            assertSame(executorInjectorController, executorUninjected.get());
        }
    }
}
