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

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.Writer;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.jboss.msc.Version;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.ref.Reaper;
import org.jboss.msc.ref.Reference;
import org.jboss.msc.ref.WeakReference;
import org.jboss.msc.service.ServiceController.Mode;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 * @author <a href="mailto:flavia.rainone@jboss.com">Flavia Rainone</a>
 */
final class ServiceContainerImpl extends AbstractServiceTarget implements ServiceContainer {

    private final Lock readLock;
    private final Lock writeLock;

    static final String PROFILE_OUTPUT;

    static {
        PROFILE_OUTPUT = AccessController.doPrivileged(new PrivilegedAction<String>() {
            public String run() {
                return System.getProperty("jboss.msc.profile.output");
            }
        });
        ServiceLogger.INSTANCE.greeting(Version.getVersionString());
    }

    private final Map<ServiceName, ServiceRegistrationImpl> registry = new HashMap<ServiceName, ServiceRegistrationImpl>();

    private final long start = System.nanoTime();

    private final List<TerminateListener> terminateListeners = new ArrayList<TerminateListener>(1);

    private static final class ExecutorHolder {
        private static final Executor VALUE;

        static {
            final ThreadPoolExecutor executor = new ThreadPoolExecutor(0, 1, 30L, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(), new ThreadFactory() {
                public Thread newThread(final Runnable r) {
                    final Thread thread = new Thread(r);
                    thread.setDaemon(true);
                    thread.setUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
                        public void uncaughtException(final Thread t, final Throwable e) {
                            ServiceLogger.INSTANCE.uncaughtException(e);
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
                                    container.shutdown();
                                    container.addTerminateListener(listener);
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

    private final Writer profileOutput;

    private TerminateListener.Info terminateInfo = null;

    private volatile boolean down = false;

    private volatile Executor executor;

    ServiceContainerImpl() {
        final ReentrantReadWriteLock readWriteLock = new ReentrantReadWriteLock();
        readLock = readWriteLock.readLock();
        writeLock = readWriteLock.writeLock();
        final Set<Reference<ServiceContainerImpl, Void>> set = ShutdownHookHolder.containers;
        Writer profileOutput = null;
        if (PROFILE_OUTPUT != null) {
            try {
                profileOutput = new OutputStreamWriter(new FileOutputStream(PROFILE_OUTPUT));
            } catch (FileNotFoundException e) {
                // ignore
            }
        }
        this.profileOutput = profileOutput;
        synchronized (set) {
            // if the shutdown hook was triggered, then no services can ever come up in any new containers.
            if (ShutdownHookHolder.down) {
                down = true;
            }
            //noinspection ThisEscapedInObjectConstruction
            else {
                //noinspection ThisEscapedInObjectConstruction
                set.add(new WeakReference<ServiceContainerImpl, Void>(this, null, new Reaper<ServiceContainerImpl, Void>() {
                    public void reap(final Reference<ServiceContainerImpl, Void> reference) {
                        ShutdownHookHolder.containers.remove(reference);
                    }
                }));
            }
        }
    }

    Writer getProfileOutput() {
        return profileOutput;
    }

    long getStart() {
        return start;
    }

    public void setExecutor(final Executor executor) {
        this.executor = executor;
    }

    @Override
    public synchronized void addTerminateListener(TerminateListener listener) {
        if (terminateInfo != null) { // if shutdown is already performed 
            listener.handleTermination(terminateInfo); // invoke handleTermination immediately
        }
        else {
            terminateListeners.add(listener);
        }
    }

    @Override
    public void awaitTermination() throws InterruptedException {
        final LatchListener listener = new LatchListener(1);
        addTerminateListener(listener);
        listener.await();
    }

    @Override
    public void awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        final LatchListener listener = new LatchListener(1);
        addTerminateListener(listener);
        listener.await(timeout, unit);
    }

    boolean isShutdown() {
        return down;
    }

    public void shutdown() {
        final MultipleRemoveListener<Runnable> shutdownListener;
        final long started;
        synchronized(this) {
            if (down){
                return;
            }
            down = true;
        }
        started =  System.nanoTime();
        shutdownListener = MultipleRemoveListener.create(new Runnable() {
            public void run() {
                shutdownComplete(started);
            }
        });
        final HashSet<ServiceInstanceImpl<?>> done = new HashSet<ServiceInstanceImpl<?>>();
        for (ServiceRegistrationImpl registration : registry.values()) {
            ServiceInstanceImpl<?> serviceInstance = registration.getInstance();
            if (serviceInstance != null && done.add(serviceInstance)) {
                try {
                    serviceInstance.addListener(shutdownListener);
                } catch (IllegalArgumentException e) {
                    continue;
                }
                serviceInstance.setMode(Mode.REMOVE);
            }
        }
        shutdownListener.done();
    }

    public boolean isShutdownComplete() {
        synchronized (this) {
            return terminateInfo != null;
        }
    }

    public void dumpServices() {
        dumpServices(System.out);
    }

    public void dumpServices(PrintStream out) {
        readLock.lock();
        try {
            if (registry.isEmpty()) {
                out.printf("Registry is empty");
            } else for (Map.Entry<ServiceName, ServiceRegistrationImpl> entry : registry.entrySet()) {
                final ServiceName name = entry.getKey();
                final ServiceRegistrationImpl registration = entry.getValue();
                final ServiceInstanceImpl<?> instance = registration.getInstance();
                if (instance != null) {
                    final ServiceInstanceImpl.Substate substate = instance.getSubstate();
                    out.printf("Service '%s' mode %s state=%s (%s)\n", name, instance.getMode(), substate.getState(), substate);
                }
            }
        } finally {
            readLock.unlock();
        }
    }

    protected void finalize() throws Throwable {
        shutdown();
    }

    private synchronized void shutdownComplete(long started) {
        terminateInfo = new TerminateListener.Info(started, System.nanoTime());
        for (TerminateListener terminateListener : terminateListeners) {
            try {
                terminateListener.handleTermination(terminateInfo);
            } catch (Throwable t) {
                // ignore
            }
        }
    }

    static final class LatchListener extends CountDownLatch implements TerminateListener {

        public LatchListener(int count) {
            super(count);
        }

        @Override
        public void handleTermination(Info info) {
            countDown();
        }
    }

    Executor getExecutor() {
        final Executor executor = this.executor;
        return executor != null ? executor : ExecutorHolder.VALUE;
    }

    /**
     * Install a collection of service definitions into the registry.  Will install the services
     * in dependency order.
     *
     * @param serviceBatch The service batch to install
     * @throws ServiceRegistryException If any problems occur resolving the dependencies or adding to the registry.
     */
    @Override
    void install(final BatchBuilderImpl serviceBatch) throws ServiceRegistryException {
        validateTargetState();
        install(serviceBatch.getBatchServices().values());
    }

    /**
     * Install a batch of builders.
     *
     * @param builders the builders
     * @throws DuplicateServiceException if a service is duplicated
     */
    private void install(final Collection<ServiceBuilderImpl<?>> builders) throws DuplicateServiceException {
        final Lock lock = writeLock;
        lock.lock();
        try {
            final Deque<ServiceBuilderImpl<?>> installedBuilders = new ArrayDeque<ServiceBuilderImpl<?>>(builders.size());
            final Deque<ServiceInstanceImpl<?>> installedInstances = new ArrayDeque<ServiceInstanceImpl<?>>(builders.size());
            boolean ok = false;
            try {
                for (ServiceBuilderImpl<?> builder : builders) {
                    installedInstances.addLast(doInstall(builder));
                    installedBuilders.addLast(builder);
                }
                ok = true;
            } finally {
                if (! ok) {
                    for (ServiceInstanceImpl<?> instance : installedInstances) {
                        rollback(instance);
                    }
                } else {
                    while (! installedBuilders.isEmpty()) {
                        final ServiceBuilderImpl<?> builder = installedBuilders.removeFirst();
                        final ServiceInstanceImpl<?> instance = installedInstances.removeFirst();
                        commit(builder.getInitialMode(), instance);
                    }
                }
            }
        } finally {
            lock.unlock();
        }
    }

    private <S> ServiceInstanceImpl<S> doInstall(final ServiceBuilderImpl<S> serviceBuilder) throws DuplicateServiceException {
        apply(serviceBuilder);

        // Get names & aliases
        final ServiceName name = serviceBuilder.getName();
        final ServiceName[] aliases = serviceBuilder.getAliases();
        final int aliasCount = aliases.length;

        // Create registrations
        final ServiceRegistrationImpl primaryRegistration = getOrCreateRegistration(name);
        final ServiceRegistrationImpl[] aliasRegistrations = new ServiceRegistrationImpl[aliases.length];

        for (int i = 0; i < aliasCount; i++) {
            aliasRegistrations[i] = getOrCreateRegistration(aliases[i]);
        }

        // Create the list of dependencies
        final Map<ServiceName, ServiceBuilderImpl.Dependency> dependencyMap = serviceBuilder.getDependencies();
        final int dependencyCount = dependencyMap.size();
        final Dependency[] dependencies = new Dependency[dependencyCount];
        final List<ValueInjection<?>> valueInjections = new ArrayList<ValueInjection<?>>(serviceBuilder.getValueInjections());

        // Dependencies
        int i = 0;
        for (ServiceName serviceName : dependencyMap.keySet()) {
            Dependency registration = getOrCreateRegistration(serviceName);
            final ServiceBuilderImpl.Dependency dependency = dependencyMap.get(serviceName);
            if (dependency.getDependencyType() == ServiceBuilder.DependencyType.OPTIONAL) {
                registration = new OptionalDependency(registration);
            }
            dependencies[i++] = registration;
            for (Injector<Object> injector : dependency.getInjectorList()) {
                valueInjections.add(new ValueInjection<Object>(registration, injector));
            }
        }
        final ValueInjection<?>[] injections = valueInjections.toArray(new ValueInjection<?>[valueInjections.size()]);

        // Next create the actual controller
        final ServiceInstanceImpl<S> instance = new ServiceInstanceImpl<S>(serviceBuilder.getServiceValue(), serviceBuilder.getLocation(), dependencies, injections, primaryRegistration, aliasRegistrations, serviceBuilder.getListeners());

        boolean ok = false;
        try {
            // Install the controller in each registration
            primaryRegistration.setInstance(instance);

            for (i = 0; i < aliasCount; i++) {
                aliasRegistrations[i].setInstance(instance);
            }
            ok = true;
        } finally {
            if (! ok) {
                rollback(instance);
            }
        }

        return instance;
    }

    /**
     * Commit a service install, kicking off the mode set and listener execution.
     *
     * @param initialMode the initial service mode
     * @param instance the service instance
     */
    private void commit(final ServiceController.Mode initialMode, final ServiceInstanceImpl<?> instance) {
        // Go!
        instance.setMode(initialMode == null ? ServiceController.Mode.ACTIVE : initialMode);
    }

    /**
     * Roll back a service install.
     *
     * @param instance
     */
    private void rollback(final ServiceInstanceImpl<?> instance) {
        instance.getPrimaryRegistration().clearInstance(instance);
        for (ServiceRegistrationImpl registration : instance.getAliasRegistrations()) {
            registration.clearInstance(instance);
        }
    }

    /**
     * Atomically get or create a registration.  Call with lock held.
     *
     * @param name the service name
     * @return the registration
     */
    private ServiceRegistrationImpl getOrCreateRegistration(final ServiceName name) {
        final Map<ServiceName, ServiceRegistrationImpl> registry = this.registry;
        ServiceRegistrationImpl registration;
        registration = registry.get(name);
        if (registration == null) {
            registration = new ServiceRegistrationImpl(this, name);
            registry.put(name, registration);
            return registration;
        } else {
            return registration;
        }
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
        final Lock lock = readLock;
        lock.lock();
        try {
            final ServiceRegistrationImpl registration = registry.get(serviceName);
            return registration == null ? null : registration.getInstance();
        } finally {
            lock.unlock();
        }
    }

    @Override
    public List<ServiceName> getServiceNames() {
        final Lock lock = readLock;
        lock.lock();
        try {
            final List<ServiceName> result = new ArrayList<ServiceName>(registry.size());
            for (Map.Entry<ServiceName, ServiceRegistrationImpl> registryEntry: registry.entrySet()) {
                if (registryEntry.getValue().getInstance() != null) {
                    result.add(registryEntry.getKey());
                }
            }
            return result;
        } finally {
            lock.unlock();
        }
    }

    @Override
    void install(final ServiceBuilderImpl<?> serviceBuilder) throws DuplicateServiceException {
        validateTargetState();
        install(Collections.<ServiceBuilderImpl<?>>singleton(serviceBuilder));
    }

    @Override
    boolean hasService(ServiceName name) {
        final Lock lock = readLock;
        lock.lock();
        try {
            final ServiceRegistrationImpl serviceRegistration = registry.get(name);
            return serviceRegistration != null && serviceRegistration.getInstance() != null;
        } finally {
            lock.unlock();
        }
    }

    @Override
    void validateTargetState() {
        if (down) {
            throw new IllegalStateException ("Container is down");
        }
    }
}
