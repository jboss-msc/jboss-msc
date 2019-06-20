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

import static java.security.AccessController.doPrivileged;
import static org.jboss.modules.management.ObjectProperties.property;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.lang.management.ManagementFactory;
import java.security.PrivilegedAction;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
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
import org.jboss.msc.service.management.ServiceContainerMXBean;
import org.jboss.msc.service.management.ServiceStatus;
import org.jboss.msc.value.InjectedValue;
import org.jboss.threads.EnhancedQueueExecutor;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 * @author <a href="mailto:flavia.rainone@jboss.com">Flavia Rainone</a>
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
final class ServiceContainerImpl extends ServiceTargetImpl implements ServiceContainer {

    private static final AtomicInteger SERIAL = new AtomicInteger(1);

    static {
        ServiceLogger.ROOT.greeting(Version.getVersionString());
    }

    private final ConcurrentMap<ServiceName, ServiceRegistrationImpl> registry = new ConcurrentHashMap<>(512);
    private final long start = System.nanoTime();

    private final Set<ServiceController<?>> problems = new IdentityHashSet<>();
    private final Set<ServiceController<?>> failed = new IdentityHashSet<>();
    private final Object lock = new Object();

    private int unstableServices;
    private long shutdownInitiated;

    private final List<TerminateListener> terminateListeners = new ArrayList<>(1);
    private final boolean autoShutdown;

    private static final class ShutdownHookHolder {
        private static final Set<Reference<ServiceContainerImpl, Void>> containers;
        private static boolean down = false;

        static {
            containers = new HashSet<>();
            doPrivileged(new PrivilegedAction<Void>() {
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
                                    if (container == null || !container.isAutoShutdown()) {
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

    private volatile TerminateListener.Info terminateInfo;

    private volatile boolean down;

    private final ContainerExecutor executor;

    private final String name;
    private final MBeanServer mBeanServer;
    private final ObjectName objectName;

    private final ServiceContainerMXBean containerMXBean = new ServiceContainerMXBean() {
        public ServiceStatus getServiceStatus(final String name) {
            final ServiceRegistrationImpl registration = registry.get(ServiceName.parse(name));
            if (registration != null) {
                final ServiceControllerImpl<?> instance = registration.getDependencyController();
                if (instance != null) {
                    return instance.getStatus();
                }
            }
            return null;
        }

        public List<String> queryServiceNames() {
            final Set<ServiceName> names = registry.keySet();
            final List<String> list = new ArrayList<>(names.size());
            for (ServiceName serviceName : names) {
                list.add(serviceName.getCanonicalName());
            }
            Collections.sort(list);
            return list;
        }

        public List<ServiceStatus> queryServiceStatuses() {
            final Collection<ServiceRegistrationImpl> registrations = registry.values();
            final List<ServiceStatus> list = new ArrayList<>(registrations.size());
            for (ServiceRegistrationImpl registration : registrations) {
                final ServiceControllerImpl<?> instance = registration.getDependencyController();
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
                final ServiceControllerImpl<?> instance = registration.getDependencyController();
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

        public String dumpServicesToGraphDescription() {
            final List<ServiceStatus> statuses = queryServiceStatuses();
            final Map<String, String> aliases = new HashMap<>();
            final StringBuilder builder = new StringBuilder();
            builder.append("digraph Services {\n    node [shape=record];\n    graph [rankdir=\"RL\"];\n");
            for (ServiceStatus status : statuses) {
                final String serviceName = status.getServiceName();
                final String[] aliasesStrings = status.getAliases();
                if (aliasesStrings != null) for (String alias : aliasesStrings) {
                    aliases.put(alias, serviceName);
                    aliases.put(serviceName, serviceName);
                }
                builder.append("    ");
                final String quoted = serviceName.replace("\"", "\\\"");
                builder.append('"').append(quoted).append('"');
                builder.append(' ');
                builder.append("[label=\"");
                builder.append(quoted);
                builder.append('|');
                builder.append(status.getStateName()).append("\\ (").append(status.getSubstateName()).append(")");
                builder.append("\"]");
                builder.append(";\n");
            }
            builder.append('\n');
            for (ServiceStatus status : statuses) {
                final String serviceName = status.getServiceName();
                final String[] dependencies = status.getDependencies();
                final Set<String> filteredDependencies = new HashSet<>(Arrays.asList(dependencies));
                final String parentName = status.getParentName();
                if (parentName != null) filteredDependencies.add(parentName);
                for (String dependency : filteredDependencies) {
                    builder.append("    ").append('"').append(serviceName.replace("\"", "\\\"")).append('"');
                    String dep = aliases.get(dependency);
                    if (dep == null) dep = dependency;
                    builder.append(" -> \"").append(dep.replace("\"", "\\\"")).append('"');
                    builder.append(";\n");
                }
            }
            builder.append("}\n");
            return builder.toString();
        }

        public String dumpServiceDetails(final String serviceName) {
            final ServiceRegistrationImpl registration = registry.get(ServiceName.parse(serviceName));
            if (registration != null) {
                final ServiceControllerImpl<?> instance = registration.getDependencyController();
                if (instance != null) {
                    return instance.dumpServiceDetails();
                }
            }
            return null;
        }

        @Override
        public void dumpServicesByStatus(String status) {
            System.out.printf("Services for %s with status:%s\n", getName(), status);
            Collection<ServiceStatus> services = this.queryServicesByStatus(status);
            if (services.isEmpty()) {
                System.out.printf("There are no services with status: %s\n", status);
            } else {
                this.printServiceStatus(services, System.out);
            }
        }

        @Override
        public String dumpServicesToStringByStatus(String status) {
            Collection<ServiceStatus> services = this.queryServicesByStatus(status);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PrintStream ps = null;
            try {
                ps = new PrintStream(baos, false, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                throw new IllegalStateException(e);
            }
            ps.printf("Services for %s with status:%s\n", getName(), status);
            if (services.isEmpty()) {
                ps.printf("There are no services with status: %s\n", status);
            } else {
                this.printServiceStatus(services, ps);
            }
            ps.flush();
            try {
                return new String(baos.toByteArray(), "UTF-8");
            } catch (UnsupportedEncodingException e) {
                throw new IllegalStateException(e);
            }
        }

        /**
         * Returns a collection of {@link ServiceStatus} of services, whose {@link org.jboss.msc.service.management.ServiceStatus#getStateName() status}
         * matches the passed <code>status</code>. Returns an empty collection if there's no such services.
         * @param status The status that we are interested in.
         * @return
         */
        private Collection<ServiceStatus> queryServicesByStatus(String status) {
            final Collection<ServiceRegistrationImpl> registrations = registry.values();
            final List<ServiceStatus> list = new ArrayList<>(registrations.size());
            for (ServiceRegistrationImpl registration : registrations) {
                final ServiceControllerImpl<?> instance = registration.getDependencyController();
                if (instance != null) {
                    ServiceStatus serviceStatus = instance.getStatus();
                    if (serviceStatus.getStateName().equals(status)) {
                        list.add(serviceStatus);
                    }
                }
            }
            return list;
        }

        /**
         * Print the passed {@link ServiceStatus}es to the {@link PrintStream}
         * @param serviceStatuses
         * @param out
         */
        private void printServiceStatus(Collection<ServiceStatus> serviceStatuses, PrintStream out) {
            if (serviceStatuses == null || serviceStatuses.isEmpty()) {
                return;
            }
            for (ServiceStatus status : serviceStatuses) {
                out.printf("%s\n", status);
            }
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
        synchronized (set) {
            // if the shutdown hook was triggered, then no services can ever come up in any new containers.
            if (ShutdownHookHolder.down) {
                terminateInfo = new TerminateListener.Info(System.nanoTime(), System.nanoTime());
                down = true;
            }
            //noinspection ThisEscapedInObjectConstruction
            else {
                //noinspection ThisEscapedInObjectConstruction
                set.add(new WeakReference<>(this, null, new Reaper<ServiceContainerImpl, Void>() {
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

    long getStart() {
        return start;
    }

    @Override
    public void addTerminateListener(final TerminateListener listener) {
        if (terminateInfo == null) {
            synchronized (this) {
                if (terminateInfo == null) {
                    terminateListeners.add(listener);
                    return;
                }
            }
        }
        try {
            listener.handleTermination(terminateInfo);
        } catch (final Throwable t) {
            // ignored
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

    public boolean isShutdown() {
        return down;
    }

    public void shutdown() {
        synchronized (this) {
            if (down) return;
            down = true;
            shutdownInitiated = System.nanoTime();
        }
        final ContainerShutdownListener shutdownListener = new ContainerShutdownListener(new Runnable() {
            public void run() {
                executor.shutdown();
            }
        });
        ServiceControllerImpl<?> controller;
        for (ServiceRegistrationImpl registration : registry.values()) {
            controller = registration.getDependencyController();
            if (controller != null) {
                controller.addListener(shutdownListener);
                try {
                    controller.setMode(Mode.REMOVE);
                } catch (IllegalArgumentException ignored) {
                    // controller removed in the meantime
                }
            }
        }
        shutdownListener.done();
    }

    public boolean isShutdownComplete() {
        return terminateInfo != null;
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
            Set<ServiceControllerImpl<?>> set = new HashSet<>();
            for (ServiceName name : new TreeSet<>(registry.keySet())) {
                final ServiceRegistrationImpl registration = registry.get(name);
                if (registration != null) {
                    final ServiceControllerImpl<?> instance = registration.getDependencyController();
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

    private void shutdownComplete(final long started) {
        synchronized (this) {
            terminateInfo = new TerminateListener.Info(started, System.nanoTime());
        }
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
    ServiceRegistrationImpl getOrCreateRegistration(final ServiceName name) {
        final ConcurrentMap<ServiceName, ServiceRegistrationImpl> registry = this.registry;
        ServiceRegistrationImpl registration;
        boolean success;
        do {
            registration = registry.get(name);
            if (registration == null) {
                registration = new ServiceRegistrationImpl(name);
                ServiceRegistrationImpl existing = registry.putIfAbsent(name, registration);
                if (existing != null) {
                    registration = existing;
                }
            }
            synchronized (registration) {
                registration.acquireWrite();
                try {
                    success = registration.addPendingDependent();
                } finally {
                    registration.releaseWrite();
                }
            }
        } while (!success);
        return registration;
    }

    void removeRegistration(final ServiceRegistrationImpl registration) {
        registry.remove(registration.getName(), registration);
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
        if (registration != null) {
            final ServiceControllerImpl controller = registration.getDependencyController();
            if (controller != null) {
                if (serviceName.equals(controller.getName())) {
                    // requested service represents service identity
                    return controller;
                }
                for (ServiceName alias : controller.getAliases()) {
                    if (alias.equals(serviceName)) {
                        // requested service represents service alias
                        return controller;
                    }
                }
            } else {
                // it is forbidden to retrieve controller for services defined via ServiceBuilder.provides() method
            }
        }
        return null;
    }

    @Override
    public List<ServiceName> getServiceNames() {
        final List<ServiceName> result = new ArrayList<>(registry.size());
        for (Map.Entry<ServiceName, ServiceRegistrationImpl> registryEntry: registry.entrySet()) {
            if (registryEntry.getValue().getDependencyController() != null) {
                result.add(registryEntry.getKey());
            }
        }
        return result;
    }

    @Override
    <T> ServiceController<T> install(final AbstractServiceBuilder<T> serviceBuilder) throws DuplicateServiceException {
        apply(serviceBuilder);

        // Initialize registrations and injectors map
        final Map<ServiceRegistrationImpl, WritableValueImpl> provides = new LinkedHashMap<>();
        Entry<ServiceName, WritableValueImpl> entry;
        for (Iterator<Entry<ServiceName, WritableValueImpl>> j = serviceBuilder.getProvides().entrySet().iterator(); j.hasNext(); ) {
            entry = j.next();
            provides.put(getOrCreateRegistration(entry.getKey()), entry.getValue());
        }

        // Create the list of dependencies
        final List<ValueInjection<?>> outInjections = new ArrayList<>();
        // set up outInjections with an InjectedValue
        final InjectedValue<T> serviceValue = new InjectedValue<>();
        for (final Injector<? super T> outInjection : serviceBuilder.getOutInjections()) {
            outInjections.add(new ValueInjection<>(serviceValue, outInjection));
        }

        // Dependencies
        final Map<ServiceName, AbstractServiceBuilder.Dependency> dependencyMap = serviceBuilder.getDependencies();
        final Set<Dependency> requires = new HashSet<>();
        final List<ValueInjection<?>> valueInjections = serviceBuilder.getValueInjections();
        ServiceRegistrationImpl dependencyRegistration;
        Dependency dependency;
        AbstractServiceBuilder.Dependency dependencyDefinition;
        for (Entry<ServiceName, AbstractServiceBuilder.Dependency> dependencyEntry : dependencyMap.entrySet()) {
            dependencyRegistration = getOrCreateRegistration(dependencyEntry.getKey());
            dependency = dependencyRegistration;
            dependencyDefinition = dependencyEntry.getValue();
            if (dependencyDefinition.getDependencyType() == ServiceBuilder.DependencyType.OPTIONAL) {
                dependency = new OptionalDependencyImpl(dependency);
            }
            requires.add(dependency);
            for (Injector<Object> injector : dependencyDefinition.getInjectorList()) {
                valueInjections.add(new ValueInjection<>(dependency, injector));
            }
        }
        final ValueInjection<?>[] valueInjectionArray = valueInjections.toArray(new ValueInjection<?>[valueInjections.size()]);
        final ValueInjection<?>[] outInjectionArray = outInjections.toArray(new ValueInjection<?>[outInjections.size()]);
        final Collection<ServiceName> serviceAliases = serviceBuilder.getServiceAliases();
        final ServiceName[] aliases = new ServiceName[serviceAliases.size()];
        int i = 0;
        for (ServiceName alias : serviceAliases) {
            aliases[i++] = alias;
        }

        // Next create the actual controller
        final ServiceControllerImpl<T> instance = new ServiceControllerImpl<>(this, serviceBuilder.getServiceId(), aliases, serviceBuilder.getService(),
                requires, provides, valueInjectionArray, outInjectionArray,
                serviceBuilder.getMonitors(), serviceBuilder.getServiceListeners(), serviceBuilder.getLifecycleListeners(), serviceBuilder.getParent());
        boolean ok = false;
        try {
            serviceValue.setValue(instance);
            synchronized (this) {
                if (down) {
                    ok = true; // do not rollback installation because we didn't install anything
                    throw new IllegalStateException ("Container is down");
                }
                // It is necessary to call startInstallation() under container intrinsic lock.
                // This is the only point in MSC code where ServiceRegistrationImpl.instance
                // field is being set to non null value. So in order for shutdown() method
                // which iterates 'registry' concurrent hash map outside of intrinsic lock
                // to see up to date installed controllers this synchronized section is necessary.
                // Otherwise it may happen container stability will be seriously broken on shutdown.
                instance.startInstallation();
            }
            instance.startConfiguration();
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
        final Set<ServiceControllerImpl<?>> visited = new IdentityHashSet<>();
        final Deque<ServiceName> visitStack = new ArrayDeque<>();
        visitStack.push(instance.getName());
        for (ServiceRegistrationImpl registration : instance.getRegistrations()) {
            synchronized (registration) {
                detectCircularity(registration.getDependents(), instance, visited, visitStack);
            }
        }
    }

    private void detectCircularity(Set<? extends Dependent> dependents, ServiceControllerImpl<?> instance, Set<ServiceControllerImpl<?>> visited,  Deque<ServiceName> visitStack) {
        for (Dependent dependent: dependents) {
            final ServiceControllerImpl<?> controller = dependent.getDependentController();
            if (controller == null) continue; // [MSC-145] optional dependencies may return null
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
                if (controller.getState() == ServiceController.State.REMOVED) continue;
                visitStack.push(controller.getName());
                synchronized(controller) {
                    detectCircularity(controller.getChildren(), instance, visited, visitStack);
                }
                for (ServiceRegistrationImpl registration : controller.getRegistrations()) {
                    if (registration.getDependencyController() == null) continue; // concurrent removal
                    synchronized (registration) {
                        detectCircularity(registration.getDependents(), instance, visited, visitStack);
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

    final class ThreadAction implements PrivilegedAction<ServiceThread> {
        private final Runnable r;
        private final int id;
        private final AtomicInteger threadSeq;

        ThreadAction(final Runnable r, final int id, final AtomicInteger threadSeq) {
            this.r = r;
            this.id = id;
            this.threadSeq = threadSeq;
        }

        public ServiceThread run() {
            ServiceThread thread = new ServiceThread(r, ServiceContainerImpl.this);
            if (thread.isDaemon()) thread.setDaemon(false);
            if (thread.getPriority() != Thread.NORM_PRIORITY) thread.setPriority(Thread.NORM_PRIORITY);
            thread.setName(String.format("MSC service thread %d-%d", Integer.valueOf(id), Integer.valueOf(threadSeq.getAndIncrement())));
            thread.setUncaughtExceptionHandler(HANDLER);
            return thread;
        }
    }


    final class ContainerExecutor implements ExecutorService {

        private final ExecutorService delegate;

        ContainerExecutor(final int corePoolSize, final int maximumPoolSize, final long keepAliveTime, final TimeUnit unit) {
            final ThreadFactory threadFactory = new ThreadFactory() {
                private final int id = executorSeq.getAndIncrement();
                private final AtomicInteger threadSeq = new AtomicInteger(1);

                public Thread newThread(final Runnable r) {
                    return doPrivileged(new ThreadAction(r, id, threadSeq));
                }
            };
            if (EnhancedQueueExecutor.DISABLE_HINT) {
                delegate = new ThreadPoolExecutor(corePoolSize, maximumPoolSize, keepAliveTime, unit, new LinkedBlockingQueue<>(), threadFactory, POLICY) {
                    protected void afterExecute(final Runnable r, final Throwable t) {
                        super.afterExecute(r, t);
                        if (t != null) {
                            HANDLER.uncaughtException(Thread.currentThread(), t);
                        }
                    }

                    protected void terminated() {
                        shutdownComplete(shutdownInitiated);
                    }
                };
            } else {
                delegate = new EnhancedQueueExecutor.Builder()
                    .setCorePoolSize(corePoolSize)
                    .setMaximumPoolSize(maximumPoolSize)
                    .setKeepAliveTime(keepAliveTime, unit)
                    .setTerminationTask(new Runnable() {
                        public void run() {
                            shutdownComplete(shutdownInitiated);
                        }
                    })
                    .setThreadFactory(threadFactory)
                    .build();
            }
        }

        public void shutdown() {
            delegate.shutdown();
        }

        public List<Runnable> shutdownNow() {
            return delegate.shutdownNow();
        }

        public boolean isShutdown() {
            return delegate.isShutdown();
        }

        public boolean isTerminated() {
            return delegate.isTerminated();
        }

        public boolean awaitTermination(final long timeout, final TimeUnit unit) throws InterruptedException {
            return delegate.awaitTermination(timeout, unit);
        }

        public <T> Future<T> submit(final Callable<T> task) {
            return delegate.submit(task);
        }

        public <T> Future<T> submit(final Runnable task, final T result) {
            return delegate.submit(task, result);
        }

        public Future<?> submit(final Runnable task) {
            return delegate.submit(task);
        }

        public <T> List<Future<T>> invokeAll(final Collection<? extends Callable<T>> tasks) throws InterruptedException {
            return delegate.invokeAll(tasks);
        }

        public <T> List<Future<T>> invokeAll(final Collection<? extends Callable<T>> tasks, final long timeout, final TimeUnit unit) throws InterruptedException {
            return delegate.invokeAll(tasks, timeout, unit);
        }

        public <T> T invokeAny(final Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
            return delegate.invokeAny(tasks);
        }

        public <T> T invokeAny(final Collection<? extends Callable<T>> tasks, final long timeout, final TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
            return delegate.invokeAny(tasks, timeout, unit);
        }

        public void execute(final Runnable command) {
            delegate.execute(command);
        }
    }
}
