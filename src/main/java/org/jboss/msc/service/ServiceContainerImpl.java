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
import static org.jboss.msc.service.ServiceProperty.FAILED_TO_START;
import static org.jboss.msc.service.ServiceProperty.UNINSTALLED;

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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentLinkedQueue;
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
import org.jboss.msc.service.management.ServiceContainerMXBean;
import org.jboss.msc.service.management.ServiceStatus;
import org.jboss.msc.value.InjectedValue;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 * @author <a href="mailto:flavia.rainone@jboss.com">Flavia Rainone</a>
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
        ServiceLogger.INSTANCE.greeting(Version.getVersionString());
    }

    private final Map<ServiceName, ServiceRegistrationImpl> registry = new UnlockedReadHashMap<ServiceName, ServiceRegistrationImpl>(512);

    private final long start = System.nanoTime();
    private long shutdownInitiated;

    private final List<TerminateListener> terminateListeners = new ArrayList<TerminateListener>(1);

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

    // list of cycles that have been detected
    private volatile Collection<List<ServiceName>> cycles;
    // latch to avoid that cycles are returned before they are detected
    private CountDownLatch cycleDetectionLatch;
    // indicates when a notifier task is postponed
    private boolean scheduleNotifierTaskPostponed = false;
    // dependent notification task that has been scheduled
    private DependentNotifierSchedule notificationScheduled = DependentNotifierSchedule.NOT_SCHEDULED;

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
            final ArrayList<String> list = new ArrayList<String>(names.size());
            for (ServiceName serviceName : names) {
                list.add(serviceName.getCanonicalName());
            }
            Collections.sort(list);
            return list;
        }

        public List<ServiceStatus> queryServiceStatuses() {
            final Collection<ServiceRegistrationImpl> registrations = registry.values();
            final ArrayList<ServiceStatus> list = new ArrayList<ServiceStatus>(registrations.size());
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
            PrintStream ps = null;
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
    };

    ServiceContainerImpl(String name, int coreSize, int maxSize, long timeOut, TimeUnit timeOutUnit) {
        super(null);
        final int serialNo = SERIAL.getAndIncrement();
        if (name == null) {
            name = String.format("anonymous-%d", Integer.valueOf(serialNo));
        }
        this.name = name;
        executor = new ContainerExecutor(coreSize, maxSize, timeOut, timeOutUnit);
        ObjectName objectName = null;
        MBeanServer mBeanServer = null;
        try {
            objectName = new ObjectName("jboss.msc", ObjectProperties.properties(property("type", "container"), property("name", name)));
            mBeanServer = ManagementFactory.getPlatformMBeanServer();
            mBeanServer.registerMBean(containerMXBean, objectName);
        } catch (Exception e) {
            ServiceLogger.INSTANCE.mbeanFailed(e);
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

        public LatchListener(int count) {
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
     * Checks the dependency graph structure formed by installed services, detecting missing dependencies.
     * All affected dependents are notified.
     * <p>
     * This method should be called when the dependency graph structure is changed.
     */
    void checkMissingDependencies() {
        boolean scheduleRunnable;
        synchronized (this) {
            switch(notificationScheduled) {
                case NOT_SCHEDULED:
                    notificationScheduled = DependentNotifierSchedule.MISSING_DEPENDENCY_CHECK;
                    cycleDetectionLatch = new CountDownLatch(1);
                    scheduleRunnable = true;
                    break;
                case CYCLE_VISIT_CHECK:
                    notificationScheduled = DependentNotifierSchedule.MISSING_DEPENDENCY_CHECK;
                    scheduleRunnable = false;
                    break;
                case FAILED_DEPENDENCY_CHECK:
                    notificationScheduled = DependentNotifierSchedule.MISSING_AND_FAILED_DEPENDENCIES_CHECK;
                default:
                    scheduleRunnable = false;
            }
            scheduleRunnable = scheduleRunnable || scheduleNotifierTaskPostponed;
            scheduleNotifierTaskPostponed = false;
        }
        if (scheduleRunnable) {
            NotifierTaskExecutor.executeTask(new DependentNotifierTask());
        }
    }

    /**
     * Notifies dependents affected by dependency failures.<p>
     * This method should be called whenever a dependent should be notified of a dependency failure or of
     * a dependency failure that has been cleared. This will happen every time a service failure occurs, when a service
     * failure is cleared, and when a new dependent is added to a failed service.
     * 
     * @param postponeExecution indicates if this request will be followed by a request to check for missing
     *                          dependencies. When this value is {@code true}, the task to check dependencies will be
     *                          scheduled only when the next call to {@link #checkMissingDependencies()} is made.
     */
    void checkFailedDependencies(boolean postponeExecution) {
        final boolean scheduleRunnable;
        synchronized (this) {
            // the installation of services is always followed by a dependency availability check
            // for that reason, any request to check for failures on installation should be postponed
            // to the moment when installation is complete, avoiding multiple traversals 
            switch(notificationScheduled) {
                case NOT_SCHEDULED:
                    notificationScheduled = DependentNotifierSchedule.FAILED_DEPENDENCY_CHECK;
                    scheduleRunnable = !postponeExecution;
                    scheduleNotifierTaskPostponed = postponeExecution;
                    cycleDetectionLatch = new CountDownLatch(1);
                    break;
                case CYCLE_VISIT_CHECK:
                    notificationScheduled = DependentNotifierSchedule.FAILED_DEPENDENCY_CHECK;
                    scheduleRunnable = false;
                    break;
                case MISSING_DEPENDENCY_CHECK:
                    notificationScheduled = DependentNotifierSchedule.MISSING_AND_FAILED_DEPENDENCIES_CHECK;
                default:
                    scheduleRunnable = false;
            }
        }
        if (scheduleRunnable) {
            NotifierTaskExecutor.executeTask(new DependentNotifierTask());
        }
    }

    private <S> ServiceControllerImpl<S> doInstall(final ServiceBuilderImpl<S> serviceBuilder) throws DuplicateServiceException {
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
        final List<Injector<? super S>> outInjections = serviceBuilder.getOutInjections();

        final InjectedValue<S> serviceValue = new InjectedValue<S>();
        for (final Injector<? super S> outInjection : outInjections) {
            valueInjections.add(new ValueInjection<S>(serviceValue, outInjection));
        }

        // Dependencies
        int i = 0;
        for (ServiceName serviceName : dependencyMap.keySet()) {
            Dependency registration = getOrCreateRegistration(serviceName);
            final ServiceBuilderImpl.Dependency dependency = dependencyMap.get(serviceName);
            if (dependency.getDependencyType() == ServiceBuilder.DependencyType.OPTIONAL) {
                registration = new OptionalDependency(this, registration);
            }
            dependencies[i++] = registration;
            for (Injector<Object> injector : dependency.getInjectorList()) {
                valueInjections.add(new ValueInjection<Object>(registration, injector));
            }
        }
        final ValueInjection<?>[] injections = valueInjections.toArray(new ValueInjection<?>[valueInjections.size()]);

        // Next create the actual controller
        final ServiceControllerImpl<S> instance = new ServiceControllerImpl<S>(serviceBuilder.getServiceValue(), serviceBuilder.getLocation(), dependencies, injections, primaryRegistration, aliasRegistrations, serviceBuilder.getListeners(), serviceBuilder.getParent());
        serviceValue.setValue(instance);

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
    private void commit(final ServiceController.Mode initialMode, final ServiceControllerImpl<?> instance) {
        // Go!
        instance.setMode(initialMode == null ? ServiceController.Mode.ACTIVE : initialMode);
    }

    /**
     * Roll back a service install.
     *
     * @param instance the instance
     */
    private void rollback(final ServiceControllerImpl<?> instance) {
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

    @Override
    <T> ServiceController<T> install(final ServiceBuilderImpl<T> serviceBuilder) throws DuplicateServiceException {
        if (down) {
            throw new IllegalStateException ("Container is down");
        }
        ServiceControllerImpl<T> instance = null;
        boolean ok = false;
        try {
            instance = doInstall(serviceBuilder);
            ok = true;
            return instance;
        } finally {
            if (! ok) {
                if (instance != null) {
                    rollback(instance);
                }
            } else {
                commit(serviceBuilder.getInitialMode(), instance);
                checkMissingDependencies(); // verify if there are new missing dependencies or
                // new cycles to notify after service installation
            }
        }
    }

    @Override
    public Collection<List<ServiceName>> detectCircularity() {
        final CountDownLatch cycleCountDown;
        synchronized(this) {
            cycleCountDown = cycleDetectionLatch;
        }
        if (cycleCountDown != null) {
            try {
                cycleCountDown.await();
            } catch (InterruptedException e) {}
        }
        return cycles;
    }

    private static enum DependentNotifierSchedule {
        NOT_SCHEDULED() {
            DependentNotifier createDependentNotifier() {
                return null;
            }
        },
        CYCLE_VISIT_CHECK () {
            DependentNotifier createDependentNotifier() {
                return new DependentNotifier();
            }
        },
        MISSING_DEPENDENCY_CHECK () {
            DependentNotifier createDependentNotifier() {
                return new DependentNotifier(UNINSTALLED);
            }
        },
        FAILED_DEPENDENCY_CHECK () {
            DependentNotifier createDependentNotifier() {
                return new DependentNotifier(FAILED_TO_START);
            }
        },
        MISSING_AND_FAILED_DEPENDENCIES_CHECK () {
            DependentNotifier createDependentNotifier() {
                return new DependentNotifier(UNINSTALLED, FAILED_TO_START);
            }
        };
        
        abstract DependentNotifier createDependentNotifier();
    }

    private class DependentNotifierTask implements Runnable {

        @Override
        public void run() {
            final DependentNotifierSchedule scheduleToExecute;
            final CountDownLatch cycleCountDown;
            synchronized(ServiceContainerImpl.this) {
                if (down) {
                    return;
                }
                cycleCountDown = cycleDetectionLatch;
                scheduleToExecute = notificationScheduled;
                notificationScheduled = DependentNotifierSchedule.NOT_SCHEDULED;
            }
            final DependentNotifier visitor = scheduleToExecute.createDependentNotifier();
            for (ServiceRegistrationImpl service: registry.values()) {
                service.accept(visitor);
            }
            synchronized(ServiceContainerImpl.this) {
                if (cycleDetectionLatch == cycleCountDown) {
                    cycleDetectionLatch = null;
                }
                cycles = visitor.getDetectedCycles();
            }
            cycleCountDown.countDown();
            visitor.finish();
        }
    }

    private static class NotifierTaskExecutor  extends Thread implements Executor {

        private static NotifierTaskExecutor instance;

        public static void executeTask(DependentNotifierTask notifierTask) {
            if (instance == null) {
                instance = new NotifierTaskExecutor();
                instance.start();
            }
            instance.execute(notifierTask);
        }

        private Queue<Runnable> taskQueue = new ConcurrentLinkedQueue<Runnable>();

        public NotifierTaskExecutor() {
            setDaemon(true);
        }

        @Override
        public void run() {
            do {
                while (taskQueue.isEmpty()) {
                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException e) {}
                }
                taskQueue.remove().run();
            } while(true);
        }

        @Override
        public void execute(Runnable command) {
            taskQueue.add(command);
        }
    }

    private static final AtomicInteger executorSeq = new AtomicInteger(1);
    private static final Thread.UncaughtExceptionHandler HANDLER = new Thread.UncaughtExceptionHandler() {
        public void uncaughtException(final Thread t, final Throwable e) {
            ServiceLogger.INSTANCE.uncaughtException(e, t);
        }
    };
    private static final ThreadPoolExecutor.CallerRunsPolicy POLICY = new ThreadPoolExecutor.CallerRunsPolicy();

    final class ContainerExecutor extends ThreadPoolExecutor {

        ContainerExecutor(final int corePoolSize, final int maximumPoolSize, final long keepAliveTime, final TimeUnit unit) {
            super(corePoolSize, maximumPoolSize, keepAliveTime, unit, new LinkedBlockingQueue<Runnable>(), new ThreadFactory() {
                private final int id = executorSeq.getAndIncrement();
                private final AtomicInteger threadSeq = new AtomicInteger(1);
                public Thread newThread(final Runnable r) {
                    Thread thread = new Thread(r);
                    thread.setName(String.format("MSC service thread %d-%d", Integer.valueOf(id), Integer.valueOf(threadSeq.getAndIncrement())));
                    thread.setUncaughtExceptionHandler(HANDLER);
                    return thread;
                }
            }, POLICY);
        }

        protected void terminated() {
            shutdownComplete(shutdownInitiated);
        }
    }
}
