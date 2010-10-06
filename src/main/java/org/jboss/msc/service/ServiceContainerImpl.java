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

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.jboss.msc.ref.Reaper;
import org.jboss.msc.ref.Reference;
import org.jboss.msc.ref.WeakReference;
import org.jboss.msc.value.ImmediateValue;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 * @author <a href="mailto:flavia.rainone@jboss.com">Flavia Rainone</a>
 */
final class ServiceContainerImpl extends AbstractServiceTarget implements ServiceContainer {
    final Object lock = new Object();
    final ServiceInstanceImpl<ServiceContainer> root;

    private static final class ExecutorHolder {
        private static final Executor VALUE;

        static {
            final ThreadPoolExecutor executor = new ThreadPoolExecutor(0, 1, 30L, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(), new ThreadFactory() {
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
            executor.setCorePoolSize(1);
            VALUE = executor;
        }

        private ExecutorHolder() {
        }
    }

    private static final class ShutdownHookHolder {
        private static final Set<Reference<ServiceContainerImpl, Void>> containers;
        private static boolean down = false;

        static {
            containers = new HashSet<Reference<ServiceContainerImpl, Void>>();
            AccessController.doPrivileged(new PrivilegedAction<Void>() {
                public Void run() {
                    final Thread hook = new Thread(new Runnable() {
                        public void run() {
                            // shut down all services in all containers.
                            final Set<Reference<ServiceContainerImpl, Void>> set = containers;
                            final LatchListener listener;
                            synchronized (set) {
                                down = true;
                                listener = new LatchListener(set.size());
                                for (Reference<ServiceContainerImpl, Void> containerRef : set) {
                                    final ServiceContainerImpl container = containerRef.get();
                                    if (container == null) {
                                        listener.countDown();
                                        continue;
                                    }
                                    final ServiceInstanceImpl<ServiceContainer> root = container.root;
                                    root.setMode(ServiceController.Mode.REMOVE);
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
                    }, "MSC Shutdown Thread");
                    hook.setDaemon(false);
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
        final Set<Reference<ServiceContainerImpl, Void>> set = ShutdownHookHolder.containers;
        synchronized (set) {
            // if the shutdown hook was triggered, then no services can ever come up in any new containers.
            final boolean down = ShutdownHookHolder.down;
            root = new ServiceInstanceImpl<ServiceContainer>(new ImmediateValue<Service<ServiceContainer>>(new Service<ServiceContainer>() {
                public void start(final StartContext context) throws StartException {
                }

                public void stop(final StopContext context) {
                }

                public ServiceContainer getValue() throws IllegalStateException {
                    return ServiceContainerImpl.this;
                }
            }), null, new ServiceRegistrationImpl[0], new ValueInjection<?>[0], new ServiceRegistrationImpl(this, ServiceName.of("root")), new ServiceRegistrationImpl[0]);
            if (! down) {
                set.add(new WeakReference<ServiceContainerImpl, Void>(this, null, new Reaper<ServiceContainerImpl, Void>() {
                    public void reap(final Reference<ServiceContainerImpl, Void> reference) {
                        ShutdownHookHolder.containers.remove(reference);
                    }
                }));
            }
        }
    }

    public void setExecutor(final Executor executor) {
        this.executor = executor;
    }

    public void shutdown() {
        root.setMode(ServiceController.Mode.REMOVE);
    }

    protected void finalize() throws Throwable {
        root.setMode(ServiceController.Mode.REMOVE);
    }

    static final class LatchListener extends CountDownLatch implements ServiceListener<Object> {

        public LatchListener(int count) {
            super(count);
        }

        public void listenerAdded(final ServiceController<?> serviceController) {
            final ServiceController.State state = serviceController.getState();
            if (state == ServiceController.State.REMOVED) {
                countDown();
            }
        }

        public void serviceStarting(final ServiceController<?> serviceController) {
        }

        public void serviceStarted(final ServiceController<?> serviceController) {
        }

        public void serviceFailed(final ServiceController<?> serviceController, final StartException reason) {
        }

        public void serviceStopping(final ServiceController<?> serviceController) {
        }

        public void serviceStopped(final ServiceController<?> serviceController) {
        }

        public void serviceRemoved(final ServiceController<?> serviceController) {
            countDown();
        }
    }

    Executor getExecutor() {
        final Executor executor = this.executor;
        return executor != null ? executor : ExecutorHolder.VALUE;
    }

    private final ConcurrentMap<ServiceName, ServiceRegistrationImpl> registry = new ConcurrentHashMap<ServiceName, ServiceRegistrationImpl>();

    /**
     * Install a collection of service definitions into the registry.  Will install the services
     * in dependency order.
     *
     * @param serviceBatch The service batch to install
     * @throws ServiceRegistryException If any problems occur resolving the dependencies or adding to the registry.
     */
    @Override
    void install(final BatchBuilderImpl serviceBatch) throws ServiceRegistryException {
        try {
            resolve(serviceBatch.getBatchServices());
        } catch (ResolutionException e) {
            throw new ServiceRegistryException("Failed to resolve dependencies", e);
        }
    }

    /**
     * Remove an entry.
     *
     * @param serviceName the service name
     * @param registration the controller
     */
    void remove(final ServiceName serviceName, final ServiceRegistrationImpl registration) {
        registry.remove(serviceName, registration);
    }

    private void resolve(final Map<ServiceName, ServiceBuilderImpl<?>> serviceBuilders) throws ServiceRegistryException {
        for (ServiceBuilderImpl<?> serviceBuilder : serviceBuilders.values()) {
            doInstall(serviceBuilder);
        }
    }

    <S> void doInstall(final ServiceBuilderImpl<S> serviceBuilder) throws DuplicateServiceException {
        // First, create all registrations
        final ServiceName name = serviceBuilder.getName();
        ServiceRegistrationImpl primaryRegistration = getOrCreateRegistration(name);
        final ServiceName[] aliases = serviceBuilder.getAliases();
        final ServiceRegistrationImpl[] aliasRegistrations = new ServiceRegistrationImpl[aliases.length];
        for (int i = 0; i < aliases.length; i++) {
            aliasRegistrations[i] = getOrCreateRegistration(aliases[i]);
        }

        // Next create the actual controller
        final ServiceInstanceImpl<S> instance = new ServiceInstanceImpl<S>(serviceBuilder.getServiceValue(), null, null, null, primaryRegistration, aliasRegistrations);
        // Try to install the controller in each registration
        primaryRegistration.setInstance(instance);
    }

    private ServiceRegistrationImpl getOrCreateRegistration(final ServiceName name) {
        ServiceRegistrationImpl registration = registry.get(name);
        if (registration == null) {
            registration = new ServiceRegistrationImpl(this, name);
            ServiceRegistrationImpl appearing = registry.putIfAbsent(name, registration);
            if (appearing != null) {
                return appearing;
            }
        }
        return registration;
    }

    @Override
    public ServiceController<?> getRequiredService(final ServiceName serviceName) throws ServiceNotFoundException {
        final ServiceController<?> controller = getService(serviceName);
        if (controller == null) {
            throw new ServiceNotFoundException("Service " + serviceName + " not found");
        }
        return controller;
    }

    @Override
    public ServiceController<?> getService(final ServiceName serviceName) {
        final ServiceRegistrationImpl registration = registry.get(serviceName);
        return registration == null ? null : registration.getInstance();
    }

    @Override
    void install(final ServiceBuilderImpl<?> serviceBuilder) throws DuplicateServiceException {
        if (serviceBuilder.getTarget() == this) {
            apply(serviceBuilder);
        }
        doInstall(serviceBuilder);
    }

    @Override
    boolean hasService(ServiceName name) {
        final ServiceRegistrationImpl serviceRegistration = registry.get(name);
        return serviceRegistration != null && serviceRegistration.getInstance() != null;
    }

    @Override
    void validateTargetState() {
    }
}
