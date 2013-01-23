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

import static org.jboss.modules.management.ObjectProperties.property;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.lang.management.ManagementFactory;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.jboss.modules.management.ObjectProperties;
import org.jboss.modules.ref.Reaper;
import org.jboss.modules.ref.Reference;
import org.jboss.modules.ref.WeakReference;
import org.jboss.msc.Version;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.ServiceController.Mode;
import org.jboss.msc.service.ServiceController.Substate;
import org.jboss.msc.service.management.ServiceContainerMXBean;
import org.jboss.msc.service.management.ServiceStatus;
import org.jboss.msc.value.InjectedValue;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 * @author <a href="mailto:flavia.rainone@jboss.com">Flavia Rainone</a>
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
final class ServiceContainerImpl extends ServiceTargetImpl implements ServiceContainer {

    private static final AtomicInteger SERIAL = new AtomicInteger(1);

    static final String PROFILE_OUTPUT;

    static {
        PROFILE_OUTPUT = AccessController.doPrivileged(new PrivilegedAction<String>() {
            public String run() {
                return System.getProperty("jboss.msc.profile.output");
            }
        });
        ServiceLogger.ROOT.greeting(Version.getVersionString());
    }

    private final ConcurrentMap<ServiceName, ServiceRegistrationImpl> registry = new UnlockedReadHashMap<ServiceName, ServiceRegistrationImpl>(512);
    private final long start = System.nanoTime();

    private final Set<ServiceController<?>> problems = new IdentityHashSet<ServiceController<?>>();
    private final Set<ServiceController<?>> failed = new IdentityHashSet<ServiceController<?>>();
    private final Object lock = new Object();

    private int unstableServices;
    private long shutdownInitiated;

    private final List<TerminateListener> terminateListeners = new ArrayList<TerminateListener>(1);
    private final boolean autoShutdown;

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
                                    if (container == null || ! container.isAutoShutdown()) {
                                        listener.countDown();
                                        continue;
                                    }
                                    container.addTerminateListener(listener);
                                    container.shutdown();
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

    private final ContainerExecutor executor;

    private final String name;
    private final MBeanServer mBeanServer;
    private final ObjectName objectName;

    private final ServiceContainerMXBean containerMXBean = new ServiceContainerMXBean() {
        public ServiceStatus getServiceStatus(final String name) {
            final ServiceRegistrationImpl registration = registry.get(ServiceName.parse(name));
            if (registration != null) {
                final ServiceControllerImpl<?> instance = registration.getInstance();
                if (instance != null) {
                    return instance.getStatus();
                }
            }
            return null;
        }

        public List<String> queryServiceNames() {
            final Set<ServiceName> names = registry.keySet();
            final List<String> list = new ArrayList<String>(names.size());
            for (ServiceName serviceName : names) {
                list.add(serviceName.getCanonicalName());
            }
            Collections.sort(list);
            return list;
        }

        public List<ServiceStatus> queryServiceStatuses() {
            final Collection<ServiceRegistrationImpl> registrations = registry.values();
            final List<ServiceStatus> list = new ArrayList<ServiceStatus>(registrations.size());
            for (ServiceRegistrationImpl registration : registrations) {
                final ServiceControllerImpl<?> instance = registration.getInstance();
                if (instance != null) list.add(instance.getStatus());
            }
            Collections.sort(list, new Comparator<ServiceStatus>() {
                public int compare(final ServiceStatus o1, final ServiceStatus o2) {
                    return o1.getServiceName().compareTo(o2.getServiceName());
                }
            });
            return list;
        }

        public void setServiceMode(final String name, final String mode) {
            final ServiceRegistrationImpl registration = registry.get(ServiceName.parse(name));
            if (registration != null) {
                final ServiceControllerImpl<?> instance = registration.getInstance();
                if (instance != null) {
                    instance.setMode(Mode.valueOf(mode.toUpperCase(Locale.US)));
                }
            }
        }

        public void dumpServices() {
            ServiceContainerImpl.this.dumpServices();
        }

        public String dumpServicesToString() {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PrintStream ps;
            try {
                ps = new PrintStream(baos, false, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                throw new IllegalStateException(e);
            }
            ServiceContainerImpl.this.dumpServices(ps);
            ps.flush();
            try {
                return new String(baos.toByteArray(), "UTF-8");
            } catch (UnsupportedEncodingException e) {
                throw new IllegalStateException(e);
            }
        }

        public String dumpServiceDetails(final String serviceName) {
            final ServiceRegistrationImpl registration = registry.get(ServiceName.parse(serviceName));
            if (registration != null) {
                final ServiceControllerImpl<?> instance = registration.getInstance();
                if (instance != null) {
                    return instance.dumpServiceDetails();
                }
            }
            return null;
        }
    };

    ServiceContainerImpl(String name, int coreSize, long timeOut, TimeUnit timeOutUnit, final boolean autoShutdown) {
        this.autoShutdown = autoShutdown;
        final int serialNo = SERIAL.getAndIncrement();
        if (name == null) {
            name = String.format("anonymous-%d", Integer.valueOf(serialNo));
        }
        this.name = name;
        executor = new ContainerExecutor(coreSize, coreSize, timeOut, timeOutUnit);
        ObjectName objectName = null;
        MBeanServer mBeanServer = null;
        try {
            objectName = new ObjectName("jboss.msc", ObjectProperties.properties(property("type", "container"), property("name", name)));
            mBeanServer = ManagementFactory.getPlatformMBeanServer();
            mBeanServer.registerMBean(containerMXBean, objectName);
        } catch (Exception e) {
            ServiceLogger.ROOT.mbeanFailed(e);
        }
        this.mBeanServer = mBeanServer;
        this.objectName = objectName;
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
        if (objectName != null && mBeanServer != null) {
            addTerminateListener(new TerminateListener() {
                public void handleTermination(final Info info) {
                    try {
                        ServiceContainerImpl.this.mBeanServer.unregisterMBean(ServiceContainerImpl.this.objectName);
                    } catch (Exception ignored) {
                    }
                }
            });
        }
    }

    void removeProblem(ServiceController<?> controller) {
        synchronized (lock) {
            problems.remove(controller);
        }
    }

    void removeFailed(ServiceController<?> controller) {
        synchronized (lock) {
            failed.remove(controller);
        }
    }

    void incrementUnstableServices() {
        synchronized (lock) {
            unstableServices++;
        }
    }

    void addProblem(ServiceController<?> controller) {
        synchronized (lock) {
            problems.add(controller);
        }
    }

    void addFailed(ServiceController<?> controller) {
        synchronized (lock) {
            failed.add(controller);
        }
    }

    void decrementUnstableServices() {
        synchronized (lock) {
            if (--unstableServices == 0) {
                lock.notifyAll();
            }
            assert unstableServices >= 0; 
        }
    }

    boolean isAutoShutdown() {
        return autoShutdown;
    }

    public String getName() {
        return name;
    }

    Writer getProfileOutput() {
        return profileOutput;
    }

    long getStart() {
        return start;
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

    @Override
    public void awaitStability() throws InterruptedException {
        awaitStability(null, null);
    }

    @Override
    public boolean awaitStability(final long timeout, final TimeUnit unit) throws InterruptedException {
        return awaitStability(timeout, unit, null, null);
    }

    @Override
    public void awaitStability(Set<? super ServiceController<?>> failed, Set<? super ServiceController<?>> problem) throws InterruptedException {
        synchronized (lock) {
            while (unstableServices != 0) {
                lock.wait();
            }
            if (failed != null) {
                failed.addAll(this.failed);
            }
            if (problem != null) {
                problem.addAll(this.problems);
            }
        }
    }

    @Override
    public boolean awaitStability(final long timeout, final TimeUnit unit, Set<? super ServiceController<?>> failed, Set<? super ServiceController<?>> problem) throws InterruptedException {
        long now = System.nanoTime();
        long remaining = unit.toNanos(timeout);
        synchronized (lock) {
            while (unstableServices != 0) {
                if (remaining <= 0L) {
                    return false;
                }
                lock.wait(remaining / 1000000L, (int) (remaining % 1000000L));
                remaining -= (-now + (now = System.nanoTime()));
            }
            if (failed != null) {
                failed.addAll(this.failed);
            }
            if (problem != null) {
                problem.addAll(this.problems);
            }
            return true;
        }
    }

    @Override
    public ServiceRegistry getServiceRegistry() {
        return this;
    }

    boolean isShutdown() {
        return down;
    }

    public void shutdown() {
        final MultipleRemoveListener<Runnable> shutdownListener;
        synchronized(this) {
            if (down){
                return;
            }
            down = true;
            shutdownInitiated = System.nanoTime();
        }
        shutdownListener = MultipleRemoveListener.create(new Runnable() {
            public void run() {
                executor.shutdown();
            }
        });
        final HashSet<ServiceControllerImpl<?>> done = new HashSet<ServiceControllerImpl<?>>();
        for (ServiceRegistrationImpl registration : registry.values()) {
            ServiceControllerImpl<?> serviceInstance = registration.getInstance();
            if (serviceInstance != null && serviceInstance.getSubstate() != Substate.CANCELLED && done.add(serviceInstance)) {
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
        out.printf("Services for %s:\n", getName());
        final Map<ServiceName, ServiceRegistrationImpl> registry = this.registry;
        if (registry.isEmpty()) {
            out.printf("(Registry is empty)\n");
        } else {
            int i = 0;
            Set<ServiceControllerImpl<?>> set = new HashSet<ServiceControllerImpl<?>>();
            for (ServiceName name : new TreeSet<ServiceName>(registry.keySet())) {
                final ServiceRegistrationImpl registration = registry.get(name);
                if (registration != null) {
                    final ServiceControllerImpl<?> instance = registration.getInstance();
                    if (instance != null && set.add(instance)) {
                        i++;
                        out.printf("%s\n", instance.getStatus());
                    }
                }
            }
            out.printf("%s services displayed\n", Integer.valueOf(i));
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

        LatchListener(int count) {
            super(count);
        }

        @Override
        public void handleTermination(Info info) {
            countDown();
        }
    }

    Executor getExecutor() {
        return executor;
    }

    /**
     * Atomically get or create a registration.
     *
     * @param name the service name
     * @return the registration
     */
    private ServiceRegistrationImpl getOrCreateRegistration(final ServiceName name) {
        final ConcurrentMap<ServiceName, ServiceRegistrationImpl> registry = this.registry;
        ServiceRegistrationImpl registration;
        registration = registry.get(name);
        if (registration == null) {
            registration = new ServiceRegistrationImpl(this, name);
            ServiceRegistrationImpl existing = registry.putIfAbsent(name, registration);
            if(existing != null) {
                return existing;
            } else {
                return registration;
            }
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
        final ServiceRegistrationImpl registration = registry.get(serviceName);
        return registration == null ? null : registration.getInstance();
    }

    @Override
    public List<ServiceName> getServiceNames() {
        final List<ServiceName> result = new ArrayList<ServiceName>(registry.size());
        for (Map.Entry<ServiceName, ServiceRegistrationImpl> registryEntry: registry.entrySet()) {
            if (registryEntry.getValue().getInstance() != null) {
                result.add(registryEntry.getKey());
            }
        }
        return result;
    }

    void apply(ServiceBuilderImpl<?> builder, ServiceControllerImpl<?> parent, boolean first) {
        final ServiceControllerImpl<?> parentParent;
        synchronized (parent) {
            // this ugly hack is sadly necessary.  Only ServiceListener<Object>s can be inherited, but Java doesn't know that.  So just do it the quick & dirty way!
            @SuppressWarnings("unchecked")
            final Map<ServiceListener<Object>, ServiceListener.Inheritance> genericListeners = (Map<ServiceListener<Object>, ServiceListener.Inheritance>) (Map) parent.getListeners();
            final List<ServiceListener<Object>> inherited = new ArrayList<ServiceListener<Object>>(4);
            final List<ServiceListener<Object>> once = first ? new ArrayList<ServiceListener<Object>>(4) : null;
            for (ServiceListener<Object> listener : genericListeners.keySet()) {
                final ServiceListener.Inheritance inheritance = genericListeners.get(listener);
                switch (inheritance) {
                    case ONCE: if (first) once.add(listener); break;
                    case ALL: inherited.add(listener); break;
                    case NONE: // fall thru!
                    default: break;
                }
            }
            if (first) builder.addListenerNoCheck(ServiceListener.Inheritance.NONE, once);
            builder.addListenerNoCheck(ServiceListener.Inheritance.ALL, inherited);
            Set<StabilityMonitor> monitors = parent.getMonitors();
            for (final StabilityMonitor monitor : monitors) {
                builder.addMonitorNoCheck(monitor);
            }
            parentParent = parent.getParent();
        }
        if (parentParent != null) {
            apply(builder, parentParent, false);
        }
    }

    void apply(ServiceBuilderImpl<?> builder) {
        // Apply listeners from the target, first
        super.apply(builder);
        // Now apply inherited listeners from the parent
        final ServiceControllerImpl<?> parent = builder.getParent();
        if (parent != null) {
            apply(builder, parent, true);
        }
    }

    @Override
    <T> ServiceController<T> install(final ServiceBuilderImpl<T> serviceBuilder) throws DuplicateServiceException {
        if (down) {
            throw new IllegalStateException ("Container is down");
        }
        apply(serviceBuilder);

        // Get names & aliases
        final ServiceName name = serviceBuilder.getName();
        final ServiceName[] aliases = serviceBuilder.getAliases();
        final int aliasCount = aliases.length;

        // Create registrations
        final ServiceRegistrationImpl primaryRegistration = getOrCreateRegistration(name);
        final ServiceRegistrationImpl[] aliasRegistrations = new ServiceRegistrationImpl[aliasCount];

        for (int i = 0; i < aliasCount; i++) {
            aliasRegistrations[i] = getOrCreateRegistration(aliases[i]);
        }

        // Create the list of dependencies
        final Map<ServiceName, ServiceBuilderImpl.Dependency> dependencyMap = serviceBuilder.getDependencies();
        final int dependencyCount = dependencyMap.size();
        final Dependency[] dependencies = new Dependency[dependencyCount];
        final List<ValueInjection<?>> valueInjections = serviceBuilder.getValueInjections();
        final List<ValueInjection<?>> outInjections = new ArrayList<ValueInjection<?>>();
        // set up outInjections with an InjectedValue
        final InjectedValue<T> serviceValue = new InjectedValue<T>();
        for (final Injector<? super T> outInjection : serviceBuilder.getOutInjections()) {
            outInjections.add(new ValueInjection<T>(serviceValue, outInjection));
        }

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
        final ValueInjection<?>[] valueInjectionArray = valueInjections.toArray(new ValueInjection<?>[valueInjections.size()]);
        final ValueInjection<?>[] outInjectionArray = outInjections.toArray(new ValueInjection<?>[outInjections.size()]);

        // Next create the actual controller
        final ServiceControllerImpl<T> instance = new ServiceControllerImpl<T>(serviceBuilder.getServiceValue(),
                dependencies, valueInjectionArray, outInjectionArray, primaryRegistration, aliasRegistrations,
                serviceBuilder.getMonitors(), serviceBuilder.getListeners(), serviceBuilder.getParent());
        boolean ok = false;
        try {
            serviceValue.setValue(instance);
            instance.startInstallation();
            // detect circularity before committing
            detectCircularity(instance);
            instance.commitInstallation(serviceBuilder.getInitialMode());
            ok = true;
            return instance;
        } finally {
            if (! ok) {
                instance.rollbackInstallation();
            }
        }
    }

    /**
     * Detects if installation of {@code instance} results in dependency cycles.
     *
     * @param instance                     the service being installed
     * @throws CircularDependencyException if a dependency cycle involving {@code instance} is detected
     */
    private <T> void detectCircularity(ServiceControllerImpl<T> instance) throws CircularDependencyException {
        final Set<ServiceControllerImpl<?>> visited = new IdentityHashSet<ServiceControllerImpl<?>>();
        final Deque<ServiceName> visitStack = new ArrayDeque<ServiceName>();
        final ServiceRegistrationImpl reg = instance.getPrimaryRegistration();
        IdentityHashSet<Dependent> dependents;
        synchronized (reg) {
            visitStack.push(instance.getName());
            dependents = reg.getDependents();
            synchronized (dependents) {
                detectCircularity(reg.getDependents(), instance, visited, visitStack);
            }
            synchronized (instance) {
                detectCircularity(instance.getChildren(), instance, visited, visitStack);
            }
        }
        for (ServiceRegistrationImpl alias: instance.getAliasRegistrations()) {
            synchronized (alias) {
                dependents = alias.getDependents();
                synchronized (dependents) {
                    detectCircularity(dependents, instance, visited, visitStack);
                }
            }
        }
    }

    private void detectCircularity(IdentityHashSet<? extends Dependent> dependents, ServiceControllerImpl<?> instance, Set<ServiceControllerImpl<?>> visited,  Deque<ServiceName> visitStack) {
        for (Dependent dependent: dependents) {
            final ServiceControllerImpl<?> controller = dependent.getController();
            if (controller == instance) {
                // change cycle from dependent order to dependency order
                ServiceName[] cycle = new ServiceName[visitStack.size()];
                visitStack.toArray(cycle);
                int j = cycle.length -1;
                for (int i = 0; i < j; i++, j--) {
                    ServiceName temp = cycle[i];
                    cycle[i] = cycle[j];
                    cycle[j] = temp;
                }
                throw new CircularDependencyException("Container " + name + " has a circular dependency: " + Arrays.asList(cycle), cycle);
            }
            if (visited.add(controller)) {
                synchronized (controller) {
                    if (controller.getSubstateLocked() == Substate.CANCELLED) {
                        continue;
                    }
                }
                ServiceRegistrationImpl reg = controller.getPrimaryRegistration();
                synchronized(reg) {
                    // concurrent removal, skip this one entirely
                    if (reg.getInstance() == null) {
                        continue;
                    }
                    visitStack.push(controller.getName());
                    IdentityHashSet<? extends Dependent> controllerDependents = reg.getDependents();
                    synchronized(controllerDependents) {
                        detectCircularity(reg.getDependents(), instance, visited, visitStack);
                    }
                    synchronized(controller) {
                        detectCircularity(controller.getChildren(), instance, visited, visitStack);
                    }
                }
                for (ServiceRegistrationImpl alias: controller.getAliasRegistrations()) {
                    synchronized (alias) {
                        IdentityHashSet<? extends Dependent> controllerDependents = alias.getDependents();
                        synchronized (controllerDependents) {
                            detectCircularity(controllerDependents, instance, visited, visitStack);
                        }
                    }
                }
                visitStack.poll();
            }
        }
    }

    private static final AtomicInteger executorSeq = new AtomicInteger(1);
    private static final Thread.UncaughtExceptionHandler HANDLER = new Thread.UncaughtExceptionHandler() {
        public void uncaughtException(final Thread t, final Throwable e) {
            ServiceLogger.ROOT.uncaughtException(e, t);
        }
    };
    private static final ThreadPoolExecutor.CallerRunsPolicy POLICY = new ThreadPoolExecutor.CallerRunsPolicy();

    static class ServiceThread extends Thread {
        private final ServiceContainerImpl container;

        ServiceThread(final Runnable runnable, final ServiceContainerImpl container) {
            super(runnable);
            this.container = container;
        }

        ServiceContainerImpl getContainer() {
            return container;
        }
    }

    final class ContainerExecutor extends ThreadPoolExecutor {

        ContainerExecutor(final int corePoolSize, final int maximumPoolSize, final long keepAliveTime, final TimeUnit unit) {
            super(corePoolSize, maximumPoolSize, keepAliveTime, unit, new LinkedBlockingQueue<Runnable>(), new ThreadFactory() {
                private final int id = executorSeq.getAndIncrement();
                private final AtomicInteger threadSeq = new AtomicInteger(1);
                public Thread newThread(final Runnable r) {
                    Thread thread = new ServiceThread(r, ServiceContainerImpl.this);
                    thread.setName(String.format("MSC service thread %d-%d", Integer.valueOf(id), Integer.valueOf(threadSeq.getAndIncrement())));
                    thread.setUncaughtExceptionHandler(HANDLER);
                    return thread;
                }
            }, POLICY);
        }

        protected void afterExecute(final Runnable r, final Throwable t) {
            super.afterExecute(r, t);
            if (t != null) {
                HANDLER.uncaughtException(Thread.currentThread(), t);
            }
        }

        protected void terminated() {
            shutdownComplete(shutdownInitiated);
        }
    }
}
