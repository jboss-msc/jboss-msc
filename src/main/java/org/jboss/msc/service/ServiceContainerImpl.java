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

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.jboss.msc.value.ImmediateValue;
import org.jboss.msc.value.Value;

final class ServiceContainerImpl implements ServiceContainer {
    final Object lock = new Object();
    final ServiceControllerImpl<ServiceContainer> root;

    private static final class ExecutorHolder {
        private static final Executor VALUE;

        static {
            final ThreadPoolExecutor executor = new ThreadPoolExecutor(0, 1, 30L, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());
            executor.allowCoreThreadTimeOut(true);
            executor.setCorePoolSize(1);
            VALUE = executor;
        }

        private ExecutorHolder() {
        }
    }

    private static final class ShutdownHookHolder {
        private static final Set<ServiceContainerImpl> containers;
        private static boolean down = false;

        static {
            containers = Collections.synchronizedSet(new HashSet<ServiceContainerImpl>());
            AccessController.doPrivileged(new PrivilegedAction<Void>() {
                public Void run() {
                    final Thread hook = new Thread(new Runnable() {
                        public void run() {
                            // shut down all services in all containers.
                            final Set<ServiceContainerImpl> set = containers;
                            final LatchListener listener;
                            synchronized (set) {
                                down = true;
                                listener = new LatchListener(set.size());
                                for (ServiceContainerImpl container : set) {
                                    final ServiceControllerImpl<ServiceContainer> root = container.root;
                                    root.setMode(ServiceController.Mode.NEVER);
                                    root.addListener(listener);
                                }
                                set.clear();
                            }
                            // wait for all services to finish.
                            for (;;) try {
                                listener.await();
                                break;
                            } catch (InterruptedException e) {
                            }
                        }
                    });
                    hook.setDaemon(true);
                    Runtime.getRuntime().addShutdownHook(hook);
                    return null;
                }
            });
        }

        private ShutdownHookHolder() {
        }
    }

    private volatile Executor executor;

    ServiceContainerImpl() {
        final Set<ServiceContainerImpl> set = ShutdownHookHolder.containers;
        synchronized (set) {
            // if the shutdown hook was triggered, then no services can ever come up in any new containers.
            final boolean down = ShutdownHookHolder.down;
            root = buildService(null, new ImmediateValue<ServiceContainer>(this)).setInitialMode(down ? ServiceController.Mode.NEVER : ServiceController.Mode.AUTOMATIC).getValue();
            if (! down) set.add(this);
        }
    }

    public <T> ServiceBuilderImpl<T> buildService(final Value<? extends Service> service, final Value<T> value) throws IllegalArgumentException {
        return new ServiceBuilderImpl<T>(this, service, value);
    }

    public <S extends Service> ServiceBuilderImpl<S> buildService(final Value<S> service) throws IllegalArgumentException {
        return new ServiceBuilderImpl<S>(this, service, service);
    }

    public void setExecutor(final Executor executor) {
        this.executor = executor;
    }

    static final class LatchListener extends CountDownLatch implements ServiceListener<Object> {

        public LatchListener(int count) {
            super(count);
        }

        public void serviceStarting(final ServiceController<? extends Object> serviceController) {
        }

        public void serviceStarted(final ServiceController<? extends Object> serviceController) {
        }

        public void serviceFailed(final ServiceController<? extends Object> serviceController, final StartException reason) {
        }

        public void serviceStopping(final ServiceController<? extends Object> serviceController) {
        }

        public void serviceStopped(final ServiceController<? extends Object> serviceController) {
            countDown();
            serviceController.removeListener(this);
        }

        public void serviceRemoved(final ServiceController<? extends Object> serviceController) {
        }
    }

    Executor getExecutor() {
        final Executor executor = this.executor;
        return executor != null ? executor : ExecutorHolder.VALUE;
    }
}
