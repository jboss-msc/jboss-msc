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

import static java.lang.Thread.holdsLock;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;

import org.jboss.msc.service.management.ServiceStatus;
import org.jboss.msc.value.Value;

/**
 * The service controller implementation.
 *
 * @param <S> the service type
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 * @author <a href="mailto:flavia.rainone@jboss.com">Flavia Rainone</a>
 */
final class ServiceControllerImpl<S> implements ServiceController<S>, Dependent {

    private static final String ILLEGAL_CONTROLLER_STATE = "Illegal controller state";

    /**
     * The service itself.
     */
    private final Value<? extends Service<? extends S>> serviceValue;
    /**
     * The source location in which this service was defined.
     */
    private final Location location;
    /**
     * The dependencies of this service.
     */
    private final Dependency[] dependencies;
    /**
     * The injections of this service.
     */
    private final ValueInjection<?>[] injections;
    /**
     * The out injections of this service.
     */
    private final ValueInjection<?>[] outInjections;
    /**
     * The set of registered service listeners.
     */
    private final IdentityHashSet<ServiceListener<? super S>> listeners;
    /**
     * The primary registration of this service.
     */
    private final ServiceRegistrationImpl primaryRegistration;
    /**
     * The alias registrations of this service.
     */
    private final ServiceRegistrationImpl[] aliasRegistrations;
    /**
     * The parent of this service.
     */
    private final ServiceControllerImpl<?> parent;
    /**
     * The children of this service (only valid during {@link State#UP}).
     */
    private final IdentityHashSet<ServiceControllerImpl<?>> children;
    /**
     * The immediate missing dependencies of this service.
     */
    private final IdentityHashSet<ServiceName> immediateMissingDependencies;
    /**
     * The start exception.
     */
    private StartException startException;
    /**
     * The controller mode.
     */
    private ServiceController.Mode mode = ServiceController.Mode.NEVER;
    /**
     * The controller state.
     */
    private Substate state = Substate.NEW;
    /**
     * The number of registrations which place a demand-to-start on this
     * instance. If this value is >0, propagate a demand up to all parent
     * dependents. If this value is >0 and mode is ON_DEMAND, put a load of +1
     * on {@code upperCount}.
     */
    private int demandedByCount;
    /**
     * Semaphore count for bringing this dep up. If the value is <= 0, the
     * service is stopped. Each unstarted dependency will put a load of -1 on
     * this value. A mode of AUTOMATIC or IMMEDIATE will put a load of +1 on
     * this value. A mode of NEVER will cause this value to be ignored. A mode
     * of ON_DEMAND will put a load of +1 on this value <b>if</b>
     * {@link #demandedByCount} is >0.
     */
    private int upperCount;
    /**
     * The number of dependents that are currently running. The deployment will
     * not execute the {@code stop()} method (and subsequently leave the
     * {@link org.jboss.msc.service.ServiceController.State#STOPPING} state)
     * until all running dependents (and listeners) are stopped.
     */
    private int runningDependents;
    /**
     * Count for failure notification. It indicates how many services have
     * failed to start and are not recovered so far. This count monitors
     * failures that happen when starting this service, and dependency related
     * failures as well. When incremented from 0 to 1, it is time to notify
     * dependents and listeners that a failure occurred. When decremented from 1
     * to 0, the dependents and listeners are notified that the affected
     * services are retrying to start. Values larger than 1 are ignored to avoid
     * multiple notifications.
     */
    private int failCount;
    /**
     * Indicates if this service has one or more immediate dependencies that
     * are not installed. Count for notification of missing (uninstalled)
     * dependencies. Its value indicates how many immediate dependencies are
     * missing. When incremented from 0 to 1, dependents and listeners are
     * notified of the missing dependency. When decremented from 1 to 0, a
     * notification that the missing dependencies are now installed is sent to
     * dependents and listeners. Values larger than 1 are ignored to avoid
     * multiple notifications.
     */
    private int immediateMissingDepCount;
    /**
     * Indicates if this service has one or more transitive dependencies that
     * are not installed. Count for notification of missing (uninstalled)
     * dependencies. Its value indicates how many transitive dependencies are
     * missing. When incremented from 0 to 1, dependents and listeners are
     * notified of the missing dependency. When decremented from 1 to 0, a
     * notification that the missing dependencies are now installed is sent to
     * dependents and listeners. Values larger than 1 are ignored to avoid
     * multiple notifications.
     */
    private int transitiveMissingDepCount;
    /**
     * The number of asynchronous tasks that are currently running. This
     * includes listeners, start/stop methods, outstanding asynchronous
     * start/stops, and internal tasks.
     */
    private int asyncTasks;
    /**
     * The service target for adding child services (can be {@code null} if none
     * were added).
     */
    private volatile ChildServiceTarget childTarget;
    /**
     * The system nanotime of the moment in which the last lifecycle change was
     * initiated.
     */
    private volatile long lifecycleTime;

    private static final Dependent[] NO_DEPENDENTS = new Dependent[0];
    private static final String[] NO_STRINGS = new String[0];

    ServiceControllerImpl(final Value<? extends Service<? extends S>> serviceValue, final Location location, final Dependency[] dependencies, final ValueInjection<?>[] injections, final ValueInjection<?>[] outInjections, final ServiceRegistrationImpl primaryRegistration, final ServiceRegistrationImpl[] aliasRegistrations, final Set<? extends ServiceListener<? super S>> listeners, final ServiceControllerImpl<?> parent) {
        this.serviceValue = serviceValue;
        this.location = location;
        this.dependencies = dependencies;
        this.injections = injections;
        this.outInjections = outInjections;
        this.primaryRegistration = primaryRegistration;
        this.aliasRegistrations = aliasRegistrations;
        this.listeners =  new IdentityHashSet<ServiceListener<? super S>>(listeners);
        this.parent = parent;
        int depCount = dependencies.length;
        upperCount = parent == null ? -depCount : -depCount - 1;
        children = new IdentityHashSet<ServiceControllerImpl<?>>();
        immediateMissingDependencies = new IdentityHashSet<ServiceName>();
    }

    Substate getSubstateLocked() {
        return state;
    }

    void addAsyncTask() {
        asyncTasks++;
    }

    void addAsyncTasks(final int size) {
        asyncTasks += size;
    }

    void removeAsyncTask() {
        asyncTasks--;
    }

    void removeAsyncTasks(final int size) {
        asyncTasks -= size;
    }

    /**
     * Start this service installation, connecting it to its parent and dependencies. Also,
     * set the instance in primary and alias registrations.
     * <p>
     * All notifications from dependencies, parents, and registrations will be ignored until the
     * installation is {@link #commitInstallation(org.jboss.msc.service.ServiceController.Mode) committed}.
     */
    void startInstallation() {
        for (Dependency dependency : dependencies) {
            dependency.addDependent(this);
        }
        if (parent != null) parent.addChild(this);

        // Install the controller in each registration
        primaryRegistration.setInstance(this);

        for (ServiceRegistrationImpl aliasRegistration: aliasRegistrations) {
            aliasRegistration.setInstance(this);
        }
    }

    /**
     * Commit the service install, kicking off the mode set and listener execution.
     *
     * @param initialMode the initial service mode
     */
    void commitInstallation(Mode initialMode) {
        assert (state == Substate.NEW);
        assert initialMode != null;
        assert ! holdsLock(this);
        final ArrayList<Runnable> listenerAddedTasks = new ArrayList<Runnable>(16);
        final ArrayList<Runnable> tasks = new ArrayList<Runnable>(16);
        synchronized(this) {
            getListenerTasks(ListenerNotification.LISTENER_ADDED, listenerAddedTasks);
            internalSetMode(initialMode, tasks);
            // placeholder async task for running listener added tasks
            asyncTasks += listenerAddedTasks.size() + tasks.size() + 1;
        }
        doExecute(tasks);
        tasks.clear();
        for (Runnable listenerAddedTask : listenerAddedTasks) {
            listenerAddedTask.run();
        }
        synchronized (this) {
            Dependent[][] dependents = getDependents();
            if (failCount > 0) {
                getListenerTasks(ListenerNotification.DEPENDENCY_FAILURE, tasks);
                tasks.add(new DependencyFailedTask(dependents));
            }
            if (immediateMissingDepCount > 0) {
                getListenerTasks(ListenerNotification.IMMEDIATE_MISSING_DEPENDENCY, tasks);
                tasks.add(new DependencyUninstalledTask(dependents));
                if (transitiveMissingDepCount > 0) {
                    getListenerTasks(ListenerNotification.TRANSITIVE_MISSING_DEPENDENCY, tasks);
                }
            } else if (transitiveMissingDepCount > 0) {
                getListenerTasks(ListenerNotification.TRANSITIVE_MISSING_DEPENDENCY, tasks);
                tasks.add(new DependencyUninstalledTask(dependents));
            }
            state = Substate.DOWN;
            // subtract one to compensate for +1 above
            asyncTasks --;
            transition(tasks);
            asyncTasks += tasks.size();
        }
        doExecute(tasks);
    }

    /**
     * Roll back the service install.
     */
    void rollbackInstallation() {
        synchronized(this) {
            mode = Mode.REMOVE;
            asyncTasks ++;
            state = Substate.CANCELLED;
        }
        (new RemoveTask()).run();
    }

    /**
     * Identify the transition to take.  Call under lock.
     *
     * @return the transition or {@code null} if none is needed at this time
     */
    private Transition getTransition() {
        assert holdsLock(this);
        if (asyncTasks != 0) {
            // no movement possible
            return null;
        }
        switch (state) {
            case DOWN: {
                if (mode == ServiceController.Mode.REMOVE) {
                    return Transition.DOWN_to_REMOVING;
                } else if (mode != ServiceController.Mode.NEVER) {
                    if (upperCount > 0) {
                        return Transition.DOWN_to_START_REQUESTED;
                    }
                }
                break;
            }
            case STOPPING: {
                return Transition.STOPPING_to_DOWN;
            }
            case STOP_REQUESTED: {
                if (upperCount > 0) {
                    return Transition.STOP_REQUESTED_to_UP;
                }
                if (runningDependents == 0) {
                    return Transition.STOP_REQUESTED_to_STOPPING;
                }
                break;
            }
            case UP: {
                if (upperCount <= 0) {
                    return Transition.UP_to_STOP_REQUESTED;
                }
                break;
            }
            case START_FAILED: {
                if (upperCount > 0) {
                    if (startException == null) {
                        return Transition.START_FAILED_to_STARTING;
                    }
                } else {
                    return Transition.START_FAILED_to_DOWN;
                }
                break;
            }
            case STARTING: {
                if (startException == null) {
                    return Transition.STARTING_to_UP;
                } else {
                    return Transition.STARTING_to_START_FAILED;
                }
            }
            case START_REQUESTED: {
                if (upperCount > 0) {
                    return Transition.START_REQUESTED_to_STARTING;
                } else {
                    return Transition.START_REQUESTED_to_DOWN;
                }
            }
            case REMOVING: {
                return Transition.REMOVING_to_REMOVED;
            }
            case CANCELLED:
            case REMOVED:
            {
                // no possible actions
                break;
            }
        }
        return null;
    }

    /**
     * Run the locked portion of a transition.  Call under lock.
     *
     * @param tasks the list to which async tasks should be appended
     */
    void transition(final ArrayList<Runnable> tasks) {
        assert holdsLock(this);
        final Transition transition = getTransition();
        if (transition == null) {
            return;
        }
        switch (transition) {
            case STOPPING_to_DOWN: {
                getListenerTasks(transition.getAfter().getState(), tasks);
                tasks.add(new DependentStoppedTask());
                break;
            }
            case START_REQUESTED_to_DOWN: {
                getListenerTasks(ListenerNotification.START_REQUEST_CLEARED, tasks);
                tasks.add(new DependentStoppedTask());
                break;
            }
            case START_REQUESTED_to_STARTING: {
                getListenerTasks(transition.getAfter().getState(), tasks);
                tasks.add(new StartTask(true));
                break;
            }
            case UP_to_STOP_REQUESTED: {
                getListenerTasks(ListenerNotification.STOP_REQUESTED, tasks);
                lifecycleTime = System.nanoTime();
                tasks.add(new DependencyStoppedTask(getDependents()));
                break;
            }
            case STARTING_to_UP: {
                getListenerTasks(transition.getAfter().getState(), tasks);
                tasks.add(new DependencyStartedTask(getDependents()));
                break;
            }
            case STARTING_to_START_FAILED: {
                ChildServiceTarget childTarget = this.childTarget;
                if (childTarget != null) {
                    childTarget.valid = false;
                    this.childTarget = null;
                }
                if (! children.isEmpty()) {
                    // placeholder async task for child removal; last removed child will decrement this count
                    asyncTasks++;
                    for (ServiceControllerImpl<?> child : children) {
                        child.setMode(Mode.REMOVE);
                    }
                }
                getListenerTasks(transition.getAfter().getState(), tasks);
                tasks.add(new DependencyFailedTask(getDependents()));
                break;
            }
            case START_FAILED_to_STARTING: {
                getListenerTasks(ListenerNotification.FAILED_STARTING, tasks);
                tasks.add(new DependencyRetryingTask(getDependents()));
                tasks.add(new StartTask(false));
                break;
            }
            case START_FAILED_to_DOWN: {
                startException = null;
                failCount--;
                assert failCount == 0;
                getListenerTasks(ListenerNotification.FAILED_STOPPED, tasks);
                tasks.add(new DependencyRetryingTask(getDependents()));
                tasks.add(new StopTask(true));
                tasks.add(new DependentStoppedTask());
                break;
            }
            case STOP_REQUESTED_to_UP: {
                getListenerTasks(ListenerNotification.STOP_REQUEST_CLEARED, tasks);
                tasks.add(new DependencyStartedTask(getDependents()));
                break;
            }
            case STOP_REQUESTED_to_STOPPING: {
                ChildServiceTarget childTarget = this.childTarget;
                if (childTarget != null) {
                    childTarget.valid = false;
                    this.childTarget = null;
                }
                if (! children.isEmpty()) {
                    // placeholder async task for child removal; last removed child will decrement this count
                    asyncTasks++;
                    for (ServiceControllerImpl<?> child : children) {
                        child.setMode(Mode.REMOVE);
                    }
                }
                getListenerTasks(transition.getAfter().getState(), tasks);
                tasks.add(new StopTask(false));
                break;
            }
            case DOWN_to_REMOVING: {
                Dependent[][] dependents = getDependents();
                // Clear all dependency uninstalled flags from the target service
                if (immediateMissingDepCount > 0) {
                    getListenerTasks(ListenerNotification.IMMEDIATE_DEPENDENCY_INSTALLED, tasks);
                    if (transitiveMissingDepCount > 0) {
                        getListenerTasks(ListenerNotification.TRANSITIVE_DEPENDENCY_INSTALLED, tasks);
                    }
                    tasks.add(new DependencyInstalledTask(dependents));
                } else if (transitiveMissingDepCount > 0) {
                    getListenerTasks(ListenerNotification.TRANSITIVE_DEPENDENCY_INSTALLED, tasks);
                    tasks.add(new DependencyInstalledTask(dependents));
                }
                if (failCount > 0) {
                    tasks.add(new DependencyRetryingTask(dependents));
                    // Clear all failure counts from the target service
                    getListenerTasks(ListenerNotification.DEPENDENCY_FAILURE_CLEAR, tasks);
                }
                tasks.add(new RemoveTask());
                break;
            }
            case REMOVING_to_REMOVED: {
                getListenerTasks(transition.getAfter().getState(), tasks);
                listeners.clear();
                break;
            }
            case DOWN_to_START_REQUESTED: {
                getListenerTasks(ListenerNotification.START_REQUESTED, tasks);
                lifecycleTime = System.nanoTime();
                tasks.add(new DependentStartedTask());
                break;
            }
            default: {
                throw new IllegalStateException();
            }
        }
        state = transition.getAfter();
    }

    private void getListenerTasks(final ServiceController.State newState, final ArrayList<Runnable> tasks) {
        final IdentityHashSet<ServiceListener<? super S>> listeners = this.listeners;
        for (ServiceListener<? super S> listener : listeners) {
            tasks.add(new ListenerTask(listener, newState));
        }
    }

    private void getListenerTasks(final ListenerNotification notification, final ArrayList<Runnable> tasks) {
        final IdentityHashSet<ServiceListener<? super S>> listeners = this.listeners;
        for (ServiceListener<? super S> listener : listeners) {
            tasks.add(new ListenerTask(listener, notification));
        }
    }

    void doExecute(final Runnable task) {
        assert !holdsLock(this);
        if (task == null) return;
        try {
            primaryRegistration.getContainer().getExecutor().execute(task);
        } catch (RejectedExecutionException e) {
            task.run();
        }
    }

    void doExecute(final ArrayList<Runnable> tasks) {
        assert !holdsLock(this);
        if (tasks == null) return;
        final Executor executor = primaryRegistration.getContainer().getExecutor();
        for (Runnable task : tasks) {
            try {
                executor.execute(task);
            } catch (RejectedExecutionException e) {
                task.run();
            }
        }
    }

    public void setMode(final ServiceController.Mode newMode) {
        internalSetMode(null, newMode);
    }

    private boolean internalSetMode(final ServiceController.Mode expectedMode, final ServiceController.Mode newMode) {
        assert !holdsLock(this);
        if (newMode == null) {
            throw new IllegalArgumentException("newMode is null");
        }
        if (newMode != Mode.REMOVE && primaryRegistration.getContainer().isShutdown()) {
            throw new IllegalArgumentException("Container is shutting down");
        }
        final ArrayList<Runnable> tasks = new ArrayList<Runnable>(4);
        synchronized (this) {
            final Mode oldMode = mode;
            if (expectedMode != null && expectedMode != oldMode) {
                return false;
            }
            if (oldMode == newMode) {
                return true;
            }
            internalSetMode(newMode, tasks);
            if (tasks.isEmpty()) {
                // if not empty, don't bother since transition should do nothing until tasks are done
                transition(tasks);
            }
            asyncTasks += tasks.size();
        }
        doExecute(tasks);
        return true;
    }

    private void internalSetMode(final Mode newMode, final ArrayList<Runnable> taskList) {
        assert holdsLock(this);
        final ServiceController.Mode oldMode = mode;
        switch (oldMode) {
            case REMOVE: {
                switch (newMode) {
                    case REMOVE: {
                        break;
                    }
                    default: {
                        throw new IllegalStateException("Service removed");
                    }
                }
                break;
            }
            case NEVER: {
                switch (newMode) {
                    case REMOVE: {
                        getListenerTasks(ListenerNotification.REMOVE_REQUESTED, taskList);
                        break;
                    }
                    case ON_DEMAND: {
                        if (demandedByCount > 0) {
                            assert upperCount < 1;
                            upperCount++;
                            taskList.add(new DemandParentsTask());
                        }
                        break;
                    }
                    case PASSIVE: {
                        assert upperCount < 1;
                        upperCount++;
                        if (demandedByCount > 0) {
                            taskList.add(new DemandParentsTask());
                        }
                        break;
                    }
                    case ACTIVE: {
                        taskList.add(new DemandParentsTask());
                        assert upperCount < 1;
                        upperCount++;
                        break;
                    }
                }
                break;
            }
            case ON_DEMAND: {
                switch (newMode) {
                    case REMOVE: {
                        getListenerTasks(ListenerNotification.REMOVE_REQUESTED, taskList);
                        // fall thru!
                    }
                    case NEVER: {
                        if (demandedByCount > 0) {
                            upperCount--;
                            taskList.add(new UndemandParentsTask());
                        }
                        break;
                    }
                    case PASSIVE: {
                        if (demandedByCount == 0) {
                            assert upperCount < 1;
                            upperCount++;
                        }
                        break;
                    }
                    case ACTIVE: {
                        taskList.add(new DemandParentsTask());
                        if (demandedByCount == 0) {
                            assert upperCount < 1;
                            upperCount++;
                        }
                        break;
                    }
                }
                break;
            }
            case PASSIVE: {
                switch (newMode) {
                    case REMOVE: {
                        getListenerTasks(ListenerNotification.REMOVE_REQUESTED, taskList);
                        // fall thru!
                    }
                    case NEVER: {
                        if (demandedByCount > 0) {
                            taskList.add(new UndemandParentsTask());
                        }
                        upperCount--;
                        break;
                    }
                    case ON_DEMAND: {
                        if (demandedByCount == 0) {
                            upperCount--;
                        }
                        break;
                    }
                    case ACTIVE: {
                        taskList.add(new DemandParentsTask());
                        break;
                    }
                }
                break;
            }
            case ACTIVE: {
                switch (newMode) {
                    case REMOVE: {
                        getListenerTasks(ListenerNotification.REMOVE_REQUESTED, taskList);
                        // fall thru!
                    }
                    case NEVER: {
                        taskList.add(new UndemandParentsTask());
                        upperCount--;
                        break;
                    }
                    case ON_DEMAND: {
                        if (demandedByCount == 0) {
                            upperCount--;
                            taskList.add(new UndemandParentsTask());
                        }
                        break;
                    }
                    case PASSIVE: {
                        if (demandedByCount == 0) {
                            taskList.add(new UndemandParentsTask());
                        }
                        break;
                    }
                }
                break;
            }
        }
        mode = newMode;
    }

    @Override
    public void immediateDependencyInstalled(ServiceName dependencyName) {
        final ArrayList<Runnable> tasks;
        synchronized (this) {
            immediateMissingDependencies.remove(dependencyName);
            if (-- immediateMissingDepCount != 0 || state.compareTo(Substate.CANCELLED) <= 0) {
                return;
            }
            // we dropped it to 0
            tasks = new ArrayList<Runnable>(16);
            getListenerTasks(ListenerNotification.IMMEDIATE_DEPENDENCY_INSTALLED, tasks);
            // both missing dep counts are 0
            if (transitiveMissingDepCount == 0) {
                tasks.add(new DependencyInstalledTask(getDependents()));
            }
            asyncTasks += tasks.size();
        }
        doExecute(tasks);
    }

    @Override
    public void immediateDependencyUninstalled(ServiceName dependencyName) {
        final ArrayList<Runnable> tasks;
        synchronized (this) {
            immediateMissingDependencies.add(dependencyName);
            if (++ immediateMissingDepCount != 1 || state.compareTo(Substate.CANCELLED) <= 0) {
                return;
            }
            // we raised it to 1
            tasks = new ArrayList<Runnable>(16);
            getListenerTasks(ListenerNotification.IMMEDIATE_MISSING_DEPENDENCY, tasks);
            // if this is the first missing dependency, we need to notify dependents;
            // otherwise, they have already been notified
            if (transitiveMissingDepCount == 0) {
                tasks.add(new DependencyUninstalledTask(getDependents()));
            }
            asyncTasks += tasks.size();
        }
        doExecute(tasks);
    }

    @Override
    public void transitiveDependencyInstalled() {
        final ArrayList<Runnable> tasks;
        synchronized (this) {
            if (-- transitiveMissingDepCount != 0 || state.compareTo(Substate.CANCELLED) <= 0) {
                return;
            }
            // we dropped it to 0
            tasks = new ArrayList<Runnable>(16);
            getListenerTasks(ListenerNotification.TRANSITIVE_DEPENDENCY_INSTALLED, tasks);
            // both missing dep counts are 0
            if (immediateMissingDepCount == 0) {
                tasks.add(new DependencyInstalledTask(getDependents()));
            }
            asyncTasks += tasks.size();
        }
        doExecute(tasks);
    }

    @Override
    public void transitiveDependencyUninstalled() {
        final ArrayList<Runnable> tasks;
        synchronized (this) {
            if (++ transitiveMissingDepCount != 1 || state.compareTo(Substate.CANCELLED) <= 0) {
                return;
            }
            // we raised it to 1
            tasks = new ArrayList<Runnable>(16);
            getListenerTasks(ListenerNotification.TRANSITIVE_MISSING_DEPENDENCY, tasks);
            // if this is the first missing dependency, we need to notify dependents;
            // otherwise, they have already been notified
            if (immediateMissingDepCount == 0) {
                tasks.add(new DependencyUninstalledTask(getDependents()));
            }
            asyncTasks += tasks.size();
        }
        doExecute(tasks);
    }

    /** {@inheritDoc} */
    public ServiceControllerImpl<?> getController() {
        return this;
    }

    @Override
    public void immediateDependencyUp() {
        final ArrayList<Runnable> tasks;
        synchronized (this) {
            assert upperCount < 1;
            if (++upperCount != 1) {
                return;
            }
            // we raised it to 1
            tasks = new ArrayList<Runnable>();
            transition(tasks);
            asyncTasks += tasks.size();
        }
        doExecute(tasks);
    }

    @Override
    public void immediateDependencyDown() {
        final ArrayList<Runnable> tasks;
        synchronized (this) {
            if (--upperCount != 0) {
                return;
            }
            // we dropped it below 0
            tasks = new ArrayList<Runnable>();
            transition(tasks);
            asyncTasks += tasks.size();
        }
        doExecute(tasks);
    }

    @Override
    public void dependencyFailed() {
        final ArrayList<Runnable> tasks;
        synchronized (this) {
            if (++failCount != 1 || state.compareTo(Substate.CANCELLED) <= 0) {
                return;
            }
            // we raised it to 1
            tasks = new ArrayList<Runnable>();
            getListenerTasks(ListenerNotification.DEPENDENCY_FAILURE, tasks);
            tasks.add(new DependencyFailedTask(getDependents()));
            asyncTasks += tasks.size();
        }
        doExecute(tasks);
    }

    @Override
    public void dependencyFailureCleared() {
        final ArrayList<Runnable> tasks;
        synchronized (this) {
            if (--failCount != 0 || state == Substate.CANCELLED) {
                return;
            }
            // we dropped it to 0
            tasks = new ArrayList<Runnable>();
            getListenerTasks(ListenerNotification.DEPENDENCY_FAILURE_CLEAR, tasks);
            tasks.add(new DependencyRetryingTask(getDependents()));
            asyncTasks += tasks.size();
        }
        doExecute(tasks);
    }

    void dependentStarted() {
        assert !holdsLock(this);
        synchronized (this) {
            runningDependents++;
        }
    }

    void dependentStopped() {
        assert !holdsLock(this);
        final ArrayList<Runnable> tasks;
        synchronized (this) {
            if (--runningDependents != 0) {
                return;
            }
            tasks = new ArrayList<Runnable>();
            transition(tasks);
            asyncTasks += tasks.size();
        }
        doExecute(tasks);
    }

    void newDependent(final Dependent dependent, final ArrayList<Runnable> tasks) {
        assert holdsLock(this);
        final Dependent[][] dependents = new Dependent[][] { { dependent } };
        if (failCount > 0) {
            tasks.add(new DependencyFailedTask(dependents));
        }
        if (immediateMissingDepCount > 0 || transitiveMissingDepCount > 0) {
            tasks.add(new DependencyUninstalledTask(dependents));
        } else if (state == Substate.UP) {
            tasks.add(new DependencyStartedTask(dependents));
        }
    }

    private void doDemandParents() {
        assert !holdsLock(this);
        for (Dependency dependency : dependencies) {
            dependency.addDemand();
        }
        final ServiceControllerImpl<?> parent = this.parent;
        if (parent != null) parent.addDemand();
    }

    private void doUndemandParents() {
        assert !holdsLock(this);
        for (Dependency dependency : dependencies) {
            dependency.removeDemand();
        }
        final ServiceControllerImpl<?> parent = this.parent;
        if (parent != null) parent.removeDemand();
    }

    void addDemand() {
        addDemands(1);
    }

    void addDemands(final int demandedByCount) {
        assert !holdsLock(this);
        final ArrayList<Runnable> tasks = new ArrayList<Runnable>();
        final boolean propagate;
        synchronized (this) {
            final int cnt = this.demandedByCount;
            this.demandedByCount += demandedByCount;
            propagate = cnt == 0 && mode.compareTo(Mode.NEVER) > 0;
            if (cnt == 0 && mode == Mode.ON_DEMAND) {
                assert upperCount < 1;
                upperCount++;
                transition(tasks);
            }
            if (propagate) {
                tasks.add(new DemandParentsTask());
            }
            asyncTasks += tasks.size();
        }
        doExecute(tasks);
    }

    void removeDemand() {
        assert !holdsLock(this);
        final ArrayList<Runnable> tasks = new ArrayList<Runnable>();
        final boolean propagate;
        synchronized (this) {
            final int cnt = --demandedByCount;
            propagate = cnt == 0 && (mode == Mode.ON_DEMAND || mode == Mode.PASSIVE);
            if (cnt == 0 && mode == Mode.ON_DEMAND) {
                upperCount--;
                transition(tasks);
            }
            if (propagate) {
                tasks.add(new UndemandParentsTask());
            }
            asyncTasks += tasks.size();
        }
        doExecute(tasks);
    }

    void addChild(ServiceControllerImpl<?> child) {
        assert !holdsLock(this);
        final ArrayList<Runnable> tasks;
        synchronized (this) {
            switch (state) {
                case STARTING:
                case UP:
                case STOP_REQUESTED: {
                    children.add(child);
                    newDependent(child, tasks = new ArrayList<Runnable>());
                    break;
                }
                default: throw new IllegalStateException("Children cannot be added in state " + state.getState());
            }
            asyncTasks += tasks.size();
        }
        doExecute(tasks);
    }

    void removeChild(ServiceControllerImpl<?> child) {
        assert !holdsLock(this);
        final ArrayList<Runnable> tasks;
        synchronized (this) {
            children.remove(child);
            if (children.isEmpty()) {
                switch (state) {
                    case START_FAILED:
                    case STOPPING:
                        // last child was removed; drop async count
                        asyncTasks--;
                        transition(tasks = new ArrayList<Runnable>());
                        break;
                    default:
                        return;
                }
            } else {
                return;
            }
            asyncTasks += tasks.size();
        }
        doExecute(tasks);
    }

    IdentityHashSet<ServiceControllerImpl<?>> getChildren() {
        assert holdsLock(this);
        return children;
    }

    public ServiceController<?> getParent() {
        return parent;
    }

    public ServiceContainerImpl getServiceContainer() {
        return primaryRegistration.getContainer();
    }

    public ServiceController.State getState() {
        return state.getState();
    }

    public S getValue() throws IllegalStateException {
        return serviceValue.getValue().getValue();
    }

    public ServiceName getName() {
        return primaryRegistration.getName();
    }

    private static final ServiceName[] NO_NAMES = new ServiceName[0];

    public ServiceName[] getAliases() {
        final ServiceRegistrationImpl[] aliasRegistrations = this.aliasRegistrations;
        final int len = aliasRegistrations.length;
        if (len == 0) {
            return NO_NAMES;
        }
        final ServiceName[] names = new ServiceName[len];
        for (int i = 0; i < len; i++) {
            names[i] = aliasRegistrations[i].getName();
        }
        return names;
    }

    public Location getLocation() {
        return location;
    }

    public void addListener(final ServiceListener<? super S> listener) {
        assert !holdsLock(this);
        final Substate state;
        synchronized (this) {
            state = this.state;
            // Always run listener if removed.
            if (state != Substate.REMOVED) {
                if (! listeners.add(listener)) {
                    // Duplicates not allowed
                    throw new IllegalArgumentException("Listener " + listener + " already present on controller for " + primaryRegistration.getName());
                }
                if (state == Substate.NEW) {
                    return;
                }
                asyncTasks ++;
            } else {
                asyncTasks += 2;
            }
        }
        invokeListener(listener, ListenerNotification.LISTENER_ADDED, null);
        if (state == Substate.REMOVED) {
            invokeListener(listener, ListenerNotification.STATE, State.REMOVED);
        }
    }

    public void removeListener(final ServiceListener<? super S> listener) {
        synchronized (this) {
            listeners.remove(listener);
        }
    }

    @Override
    public StartException getStartException() {
        synchronized (this) {
            return startException;
        }
    }

    @Override
    public void retry() {
        assert !holdsLock(this);
        final ArrayList<Runnable> tasks;
        synchronized (this) {
            if (state.getState() != ServiceController.State.START_FAILED) {
                return;
            }
            failCount--;
            assert failCount == 0;
            startException = null;
            transition(tasks = new ArrayList<Runnable>());
            asyncTasks += tasks.size();
        }
        doExecute(tasks);
    }

    @Override
    public synchronized Set<ServiceName> getImmediateMissingDependencies() {
        return immediateMissingDependencies.clone();
    }

    public ServiceController.Mode getMode() {
        synchronized (this) {
            return mode;
        }
    }

    public boolean compareAndSetMode(final Mode expectedMode, final Mode newMode) {
        if (expectedMode == null) {
            throw new IllegalArgumentException("expectedMode is null");
        }
        return internalSetMode(expectedMode, newMode);
    }

    ServiceStatus getStatus() {
        synchronized (this) {
            final String parentName = parent == null ? null : parent.getName().getCanonicalName();
            final String name = primaryRegistration.getName().getCanonicalName();
            final ServiceRegistrationImpl[] aliasRegistrations = this.aliasRegistrations;
            final int aliasLength = aliasRegistrations.length;
            final String[] aliases;
            if (aliasLength == 0) {
                aliases = NO_STRINGS;
            } else {
                aliases = new String[aliasLength];
                for (int i = 0; i < aliasLength; i++) {
                    aliases[i] = aliasRegistrations[i].getName().getCanonicalName();
                }
            }
            String serviceClass = "<unknown>";
            try {
                final Service<? extends S> value = serviceValue.getValue();
                if (value != null) {
                    serviceClass = value.getClass().getName();
                }
            } catch (RuntimeException ignored) {
            }
            final Dependency[] dependencies = this.dependencies;
            final int dependenciesLength = dependencies.length;
            final String[] dependencyNames;
            if (dependenciesLength == 0) {
                dependencyNames = NO_STRINGS;
            } else {
                dependencyNames = new String[dependenciesLength];
                for (int i = 0; i < dependenciesLength; i++) {
                    dependencyNames[i] = dependencies[i].getName().getCanonicalName();
                }
            }
            return new ServiceStatus(
                    parentName,
                    name,
                    aliases,
                    serviceClass,
                    mode.name(),
                    state.getState().name(),
                    state.name(),
                    dependencyNames,
                    failCount != 0,
                    immediateMissingDepCount != 0 || transitiveMissingDepCount != 0
            );
        }
    }

    private enum ListenerNotification {
        /** Notify the listener that is has been added. */
        LISTENER_ADDED,
        /** Notifications related to the current state.  */
        STATE,
        START_REQUESTED,
        START_REQUEST_CLEARED,
        STOP_REQUESTED,
        STOP_REQUEST_CLEARED,
        /** Notify the listener that a dependency failure occurred. */
        DEPENDENCY_FAILURE,
        /** Notify the listener that all dependency failures are cleared. */
        DEPENDENCY_FAILURE_CLEAR,
        /** Notify the listener that an immediate dependency is missing (uninstalled). */
        IMMEDIATE_MISSING_DEPENDENCY,
        /** Notify the listener that all missing immediate dependencies are now installed. */
        IMMEDIATE_DEPENDENCY_INSTALLED,
        /** Notify the listener that a transitive dependency is missing (uninstalled). */
        TRANSITIVE_MISSING_DEPENDENCY,
        /** Notify the listener that all missing transitive dependencies are now installed. */
        TRANSITIVE_DEPENDENCY_INSTALLED,
        /** Notify the listener that the service is going to be removed. */
        REMOVE_REQUESTED,
        FAILED_STARTING,
        FAILED_STOPPED
    }

    /**
     * Invokes the listener, performing the notification specified.
     * 
     * @param listener      listener to be invoked
     * @param notification  specified notification
     * @param state         the state to be notified, only relevant if {@code notification} is
     *                      {@link ListenerNotification#STATE}
     */
    private void invokeListener(final ServiceListener<? super S> listener, final ListenerNotification notification, final State state) {
        assert !holdsLock(this);
        try {
            switch (notification) {
                case LISTENER_ADDED:
                    listener.listenerAdded(this);
                    break;
                case STATE:
                    switch (state) {
                        case DOWN: {
                            listener.serviceStopped(this);
                            break;
                        }
                        case STARTING: {
                            listener.serviceStarting(this);
                            break;
                        }
                        case START_FAILED: {
                            listener.serviceFailed(this, startException);
                            break;
                        }
                        case UP: {
                            listener.serviceStarted(this);
                            break;
                        }
                        case STOPPING: {
                            listener.serviceStopping(this);
                            break;
                        }
                        case REMOVED: {
                            listener.serviceRemoved(this);
                            break;
                        }
                    }
                    break;
                case DEPENDENCY_FAILURE:
                    listener.dependencyFailed(this);
                    break;
                case DEPENDENCY_FAILURE_CLEAR:
                    listener.dependencyFailureCleared(this);
                    break;
                case IMMEDIATE_MISSING_DEPENDENCY:
                    listener.immediateDependencyUninstalled(this);
                    break;
                case IMMEDIATE_DEPENDENCY_INSTALLED:
                    listener.immediateDependencyInstalled(this);
                    break;
                case TRANSITIVE_MISSING_DEPENDENCY:
                    listener.transitiveDependencyUninstalled(this);
                    break;
                case TRANSITIVE_DEPENDENCY_INSTALLED:
                    listener.transitiveDependencyInstalled(this);
                    break;
                case REMOVE_REQUESTED:
                    listener.serviceRemoveRequested(this);
                    break;
                case START_REQUESTED:
                    listener.serviceStartRequested(this);
                    break;
                case START_REQUEST_CLEARED:
                    listener.serviceStartRequestCleared(this);
                    break;
                case STOP_REQUESTED:
                    listener.serviceStopRequested(this);
                    break;
                case STOP_REQUEST_CLEARED:
                    listener.serviceStopRequestCleared(this);
                    break;
                case FAILED_STARTING:
                    listener.failedServiceStarting(this);
                    break;
                case FAILED_STOPPED:
                    listener.failedServiceStopped(this);
                    break;
            }
        } catch (Throwable t) {
            ServiceLogger.SERVICE.listenerFailed(t, listener);
        } finally {
            final ArrayList<Runnable> tasks = new ArrayList<Runnable>();
            synchronized (this) {
                // Subtract one for this executing listener
                asyncTasks --;
                transition(tasks);
                asyncTasks += tasks.size();
            }
            doExecute(tasks);
        }
    }

    Substate getSubstate() {
        synchronized (this) {
            return state;
        }
    }

    ServiceRegistrationImpl getPrimaryRegistration() {
        return primaryRegistration;
    }

    ServiceRegistrationImpl[] getAliasRegistrations() {
        return aliasRegistrations;
    }

    /**
     * Returns a compiled array of all dependents of this service instance.
     * 
     * @return an array of dependents
     */
    private Dependent[][] getDependents() {
        IdentityHashSet<Dependent> dependentSet = primaryRegistration.getDependents();
        if (aliasRegistrations.length == 0) {
            synchronized (dependentSet) {
                return new Dependent[][] { dependentSet.toScatteredArray(NO_DEPENDENTS),
                        children.toScatteredArray(NO_DEPENDENTS)};
            }
        }
        Dependent[][] dependents = new Dependent[aliasRegistrations.length + 2][];
        synchronized (dependentSet) {
            dependents[0] = dependentSet.toScatteredArray(NO_DEPENDENTS);
        }
        dependents[1] = children.toScatteredArray(NO_DEPENDENTS);
        for (int i = 0; i < aliasRegistrations.length; i++) {
            final ServiceRegistrationImpl alias = aliasRegistrations[i];
            final IdentityHashSet<Dependent> aliasDependentSet = alias.getDependents();
            synchronized (aliasDependentSet) {
                dependents[i + 2] = aliasDependentSet.toScatteredArray(NO_DEPENDENTS);
            }
        }
        return dependents;
    }

    enum ContextState {
        SYNC,
        ASYNC,
        COMPLETE,
        FAILED,
    }

    private static <T> void doInject(final ValueInjection<T> injection) {
        injection.getTarget().inject(injection.getSource().getValue());
    }

    @Override
    public String toString() {
        return String.format("Controller for %s@%x", getName(), Integer.valueOf(hashCode()));
    }

    private class DemandParentsTask implements Runnable {

        public void run() {
            try {
                doDemandParents();
                final ArrayList<Runnable> tasks = new ArrayList<Runnable>();
                synchronized (ServiceControllerImpl.this) {
                    // Subtract one for this task
                    asyncTasks --;
                    transition(tasks);
                    asyncTasks += tasks.size();
                }
                doExecute(tasks);
            } catch (Throwable t) {
                ServiceLogger.SERVICE.internalServiceError(t, primaryRegistration.getName());
            }
        }
    }

    private class UndemandParentsTask implements Runnable {

        public void run() {
            try {
                doUndemandParents();
                final ArrayList<Runnable> tasks = new ArrayList<Runnable>();
                synchronized (ServiceControllerImpl.this) {
                    // Subtract one for this task
                    asyncTasks --;
                    transition(tasks);
                    asyncTasks += tasks.size();
                }
                doExecute(tasks);
            } catch (Throwable t) {
                ServiceLogger.SERVICE.internalServiceError(t, primaryRegistration.getName());
            }
        }
    }

    private class DependentStoppedTask implements Runnable {

        public void run() {
            try {
                for (Dependency dependency : dependencies) {
                    dependency.dependentStopped();
                }
                final ServiceControllerImpl<?> parent = ServiceControllerImpl.this.parent;
                if (parent != null) {
                    parent.dependentStopped();
                }
                final ArrayList<Runnable> tasks = new ArrayList<Runnable>();
                synchronized (ServiceControllerImpl.this) {
                    // Subtract one for this task
                    asyncTasks --;
                    transition(tasks);
                    asyncTasks += tasks.size();
                }
                doExecute(tasks);
            } catch (Throwable t) {
                ServiceLogger.SERVICE.internalServiceError(t, primaryRegistration.getName());
            }
        }
    }

    private class DependentStartedTask implements Runnable {

        public void run() {
            try {
                for (Dependency dependency : dependencies) {
                    dependency.dependentStarted();
                }
                final ServiceControllerImpl<?> parent = ServiceControllerImpl.this.parent;
                if (parent != null) {
                    parent.dependentStarted();
                }
                final ArrayList<Runnable> tasks = new ArrayList<Runnable>();
                synchronized (ServiceControllerImpl.this) {
                    // Subtract one for this task
                    asyncTasks --;
                    transition(tasks);
                    asyncTasks += tasks.size();
                }
                doExecute(tasks);
            } catch (Throwable t) {
                ServiceLogger.SERVICE.internalServiceError(t, primaryRegistration.getName());
            }
        }
    }

    private class StartTask implements Runnable {

        private final boolean doInjection;

        StartTask(final boolean doInjection) {
            this.doInjection = doInjection;
        }

        public void run() {
            assert !holdsLock(ServiceControllerImpl.this);
            final ServiceName serviceName = primaryRegistration.getName();
            final long startNanos = System.nanoTime();
            final StartContextImpl context = new StartContextImpl(startNanos);
            try {
                performInjections();
                final Service<? extends S> service = serviceValue.getValue();
                if (service == null) {
                    throw new IllegalArgumentException("Service is null");
                }
                service.start(context);
                final ArrayList<Runnable> tasks = new ArrayList<Runnable>();
                synchronized (ServiceControllerImpl.this) {
                    if (context.state != ContextState.SYNC) {
                        return;
                    }
                    context.state = ContextState.COMPLETE;
                    if (ServiceContainerImpl.PROFILE_OUTPUT != null) {
                        writeProfileInfo('S', startNanos, System.nanoTime());
                    }
                    // Subtract one for this task
                    asyncTasks --;
                    transition(tasks);
                    asyncTasks += tasks.size();
                }
                performOutInjections(serviceName);
                doExecute(tasks);
            } catch (StartException e) {
                e.setServiceName(serviceName);
                startFailed(e, serviceName, context, startNanos);
            } catch (Throwable t) {
                StartException e = new StartException("Failed to start service", t, location, serviceName);
                startFailed(e, serviceName, context, startNanos);
            }
        }

        private void performInjections() {
            if (doInjection) {
                final int injectionsLength = injections.length;
                boolean ok = false;
                int i = 0;
                try {
                    for (; i < injectionsLength; i++) {
                        final ValueInjection<?> injection = injections[i];
                        doInject(injection);
                    }
                    ok = true;
                } finally {
                    if (! ok) {
                        for (; i >= 0; i--) {
                            injections[i].getTarget().uninject();
                        }
                    }
                }
            }
        }

        private void performOutInjections(final ServiceName serviceName) {
            if (doInjection) {
                final int injectionsLength = outInjections.length;
                int i = 0;
                for (; i < injectionsLength; i++) {
                    final ValueInjection<?> injection = outInjections[i];
                    try {
                        doInject(injection);
                    } catch (Throwable t) {
                        ServiceLogger.SERVICE.exceptionAfterComplete(t, serviceName);
                    }
                }
            }
        }

        private void startFailed(StartException e, ServiceName serviceName, StartContextImpl context, long startNanos) {
            ServiceLogger.FAIL.startFailed(e, serviceName);
            final ArrayList<Runnable> tasks;
            synchronized (ServiceControllerImpl.this) {
                final ContextState oldState = context.state;
                if (oldState != ContextState.SYNC && oldState != ContextState.ASYNC) {
                    ServiceLogger.FAIL.exceptionAfterComplete(e, serviceName);
                    return;
                }
                context.state = ContextState.FAILED;
                startException = e;
                if (ServiceContainerImpl.PROFILE_OUTPUT != null) {
                    writeProfileInfo('F', startNanos, System.nanoTime());
                }
                failCount++;
                // Subtract one for this task
                asyncTasks --;
                transition(tasks = new ArrayList<Runnable>());
                asyncTasks += tasks.size();
            }
            doExecute(tasks);
        }
    }

    private class StopTask implements Runnable {
        private final boolean onlyUninject;

        StopTask(final boolean onlyUninject) {
            this.onlyUninject = onlyUninject;
        }

        public void run() {
            assert !holdsLock(ServiceControllerImpl.this);
            final ServiceName serviceName = primaryRegistration.getName();
            final long startNanos = System.nanoTime();
            final StopContextImpl context = new StopContextImpl(startNanos);
            boolean ok = false;
            try {
                if (! onlyUninject) {
                    try {
                        final Service<? extends S> service = serviceValue.getValue();
                        if (service != null) {
                            service.stop(context);
                            ok = true;
                        } else {
                            ServiceLogger.ROOT.stopServiceMissing(serviceName);
                        }
                    } catch (Throwable t) {
                        ServiceLogger.FAIL.stopFailed(t, serviceName);
                    }
                }
            } finally {
                final ArrayList<Runnable> tasks;
                synchronized (ServiceControllerImpl.this) {
                    if (ok && context.state != ContextState.SYNC) {
                        // We want to discard the exception anyway, if there was one.  Which there can't be.
                        //noinspection ReturnInsideFinallyBlock
                        return;
                    }
                    context.state = ContextState.COMPLETE;
                }
                uninject(serviceName, injections);
                uninject(serviceName, outInjections);
                synchronized (ServiceControllerImpl.this) {
                    if (ServiceContainerImpl.PROFILE_OUTPUT != null) {
                        writeProfileInfo('X', startNanos, System.nanoTime());
                    }
                    // Subtract one for this task
                    asyncTasks --;
                    transition(tasks = new ArrayList<Runnable>());
                    asyncTasks += tasks.size();
                }
                doExecute(tasks);
            }
        }

        private void uninject(final ServiceName serviceName, ValueInjection<?>[] injections) {
            for (ValueInjection<?> injection : injections) try {
                injection.getTarget().uninject();
            } catch (Throwable t) {
                ServiceLogger.ROOT.uninjectFailed(t, serviceName, injection);
            }
        }
    }

    private class ListenerTask implements Runnable {

        private final ListenerNotification notification;
        private final ServiceListener<? super S> listener;
        private final ServiceController.State state;

        ListenerTask(final ServiceListener<? super S> listener, final ServiceController.State state) {
            this.listener = listener;
            this.state = state;
            notification = ListenerNotification.STATE;
        }

        ListenerTask(final ServiceListener<? super S> listener, final ListenerNotification notification) {
            this.listener = listener;
            state = null;
            this.notification = notification;
        }

        public void run() {
            assert !holdsLock(ServiceControllerImpl.this);
            if (ServiceContainerImpl.PROFILE_OUTPUT != null) {
                final long start = System.nanoTime();
                try {
                    invokeListener(listener, notification, state);
                } finally {
                    writeProfileInfo('L', start, System.nanoTime());
                }
            } else {
                invokeListener(listener, notification, state);
            }
        }
    }

    private class DependencyStartedTask implements Runnable {

        private final Dependent[][] dependents;

        DependencyStartedTask(final Dependent[][] dependents) {
            this.dependents = dependents;
        }

        public void run() {
            try {
                for (Dependent[] dependentArray : dependents) {
                    for (Dependent dependent : dependentArray) {
                        if (dependent != null) dependent.immediateDependencyUp();
                    }
                }
                final ArrayList<Runnable> tasks = new ArrayList<Runnable>();
                synchronized (ServiceControllerImpl.this) {
                    // Subtract one for this task
                    asyncTasks --;
                    transition(tasks);
                    asyncTasks += tasks.size();
                }
                doExecute(tasks);
            } catch (Throwable t) {
                ServiceLogger.SERVICE.internalServiceError(t, primaryRegistration.getName());
            }
        }
    }

    private class DependencyStoppedTask implements Runnable {

        private final Dependent[][] dependents;

        DependencyStoppedTask(final Dependent[][] dependents) {
            this.dependents = dependents;
        }

        public void run() {
            try {
                for (Dependent[] dependentArray : dependents) {
                    for (Dependent dependent : dependentArray) {
                        if (dependent != null) dependent.immediateDependencyDown();
                    }
                }
                final ArrayList<Runnable> tasks = new ArrayList<Runnable>();
                synchronized (ServiceControllerImpl.this) {
                    // Subtract one for this task
                    asyncTasks --;
                    transition(tasks);
                    asyncTasks += tasks.size();
                }
                doExecute(tasks);
            } catch (Throwable t) {
                ServiceLogger.SERVICE.internalServiceError(t, primaryRegistration.getName());
            }
        }
    }

    private class DependencyFailedTask implements Runnable {

        private final Dependent[][] dependents;

        DependencyFailedTask(final Dependent[][] dependents) {
            this.dependents = dependents;
        }

        public void run() {
            try {
                for (Dependent[] dependentArray : dependents) {
                    for (Dependent dependent : dependentArray) {
                        if (dependent != null) dependent.dependencyFailed();
                    }
                }
                final ArrayList<Runnable> tasks = new ArrayList<Runnable>();
                synchronized (ServiceControllerImpl.this) {
                    // Subtract one for this task
                    asyncTasks --;
                    transition(tasks);
                    asyncTasks += tasks.size();
                }
                doExecute(tasks);
            } catch (Throwable t) {
                ServiceLogger.SERVICE.internalServiceError(t, primaryRegistration.getName());
            }
        }
    }

    private class DependencyRetryingTask implements Runnable {

        private final Dependent[][] dependents;

        DependencyRetryingTask(final Dependent[][] dependents) {
            this.dependents = dependents;
        }

        public void run() {
            try {
                for (Dependent[] dependentArray : dependents) {
                    for (Dependent dependent : dependentArray) {
                        if (dependent != null) dependent.dependencyFailureCleared();
                    }
                }
                final ArrayList<Runnable> tasks = new ArrayList<Runnable>();
                synchronized (ServiceControllerImpl.this) {
                    // Subtract one for this task
                    asyncTasks --;
                    transition(tasks);
                    asyncTasks += tasks.size();
                }
                doExecute(tasks);
            } catch (Throwable t) {
                ServiceLogger.SERVICE.internalServiceError(t, primaryRegistration.getName());
            }
        }
    }

    private class DependencyInstalledTask implements Runnable {

        private final Dependent[][] dependents;

        DependencyInstalledTask(final Dependent[][] dependents) {
            this.dependents = dependents;
        }

        public void run() {
            try {
                for (Dependent[] dependentArray : dependents) {
                    for (Dependent dependent : dependentArray) {
                        if (dependent != null) dependent.transitiveDependencyInstalled();
                    }
                }
                final ArrayList<Runnable> tasks = new ArrayList<Runnable>();
                synchronized (ServiceControllerImpl.this) {
                    // Subtract one for this task
                    asyncTasks --;
                    transition(tasks);
                    asyncTasks += tasks.size();
                }
                doExecute(tasks);
            } catch (Throwable t) {
                ServiceLogger.SERVICE.internalServiceError(t, primaryRegistration.getName());
            }
        }

    }

    private class DependencyUninstalledTask implements Runnable {

        private final Dependent[][] dependents;

        DependencyUninstalledTask(final Dependent[][] dependents) {
            this.dependents = dependents;
        }

        public void run() {
            try {
                for (Dependent[] dependentArray : dependents) {
                    for (Dependent dependent : dependentArray) {
                        if (dependent != null) dependent.transitiveDependencyUninstalled();
                    }
                }
                final ArrayList<Runnable> tasks = new ArrayList<Runnable>();
                synchronized (ServiceControllerImpl.this) {
                    // Subtract one for this task
                    asyncTasks --;
                    transition(tasks);
                    asyncTasks += tasks.size();
                }
                doExecute(tasks);
            } catch (Throwable t) {
                ServiceLogger.SERVICE.internalServiceError(t, primaryRegistration.getName());
            }
        }
    }

    private class RemoveTask implements Runnable {

        RemoveTask() {
        }

        public void run() {
            try {
                assert getMode() == ServiceController.Mode.REMOVE;
                assert getSubstate() == Substate.REMOVING || getSubstate() == Substate.CANCELLED;
                primaryRegistration.clearInstance(ServiceControllerImpl.this);
                for (ServiceRegistrationImpl registration : aliasRegistrations) {
                    registration.clearInstance(ServiceControllerImpl.this);
                }
                for (Dependency dependency : dependencies) {
                    dependency.removeDependent(ServiceControllerImpl.this);
                }
                final ServiceControllerImpl<?> parent = ServiceControllerImpl.this.parent;
                if (parent != null) parent.removeChild(ServiceControllerImpl.this);
                final ArrayList<Runnable> tasks = new ArrayList<Runnable>();
                synchronized (ServiceControllerImpl.this) {
                    // Subtract one for this task
                    asyncTasks --;
                    transition(tasks);
                    asyncTasks += tasks.size();
                }
                doExecute(tasks);
            } catch (Throwable t) {
                ServiceLogger.SERVICE.internalServiceError(t, primaryRegistration.getName());
            }
        }
    }

    private class StartContextImpl implements StartContext {

        private ContextState state = ContextState.SYNC;

        private final long startNanos;

        private StartContextImpl(final long startNanos) {
            this.startNanos = startNanos;
        }

        public void failed(StartException reason) throws IllegalStateException {
            final ArrayList<Runnable> tasks = new ArrayList<Runnable>();
            synchronized (ServiceControllerImpl.this) {
                if (state != ContextState.ASYNC) {
                    throw new IllegalStateException(ILLEGAL_CONTROLLER_STATE);
                }
                if (reason == null) {
                    reason = new StartException("Start failed, and additionally, a null cause was supplied");
                }
                state = ContextState.FAILED;
                final ServiceName serviceName = getName();
                reason.setServiceName(serviceName);
                ServiceLogger.FAIL.startFailed(reason, serviceName);
                startException = reason;
                failCount ++;
                if (ServiceContainerImpl.PROFILE_OUTPUT != null) {
                    writeProfileInfo('F', startNanos, System.nanoTime());
                }
                // Subtract one for this task
                asyncTasks --;
                transition(tasks);
                asyncTasks += tasks.size();
            }
            doExecute(tasks);
        }

        public ServiceTarget getChildTarget() {
            synchronized (ServiceControllerImpl.this) {
                if (state == ContextState.COMPLETE || state == ContextState.FAILED) {
                    throw new IllegalStateException("Lifecycle context is no longer valid");
                }
                if (childTarget == null) {
                    childTarget = new ChildServiceTarget(parent == null ? getServiceContainer() : parent.childTarget);
                }
                return childTarget;
            }
        }

        public void asynchronous() throws IllegalStateException {
            synchronized (ServiceControllerImpl.this) {
                if (state == ContextState.SYNC) {
                    state = ContextState.ASYNC;
                } else {
                    throw new IllegalStateException(ILLEGAL_CONTROLLER_STATE);
                }
            }
        }

        public void complete() throws IllegalStateException {
            final ArrayList<Runnable> tasks = new ArrayList<Runnable>();
            synchronized (ServiceControllerImpl.this) {
                if (state != ContextState.ASYNC) {
                    throw new IllegalStateException(ILLEGAL_CONTROLLER_STATE);
                } else {
                    state = ContextState.COMPLETE;
                    if (ServiceContainerImpl.PROFILE_OUTPUT != null) {
                        writeProfileInfo('S', startNanos, System.nanoTime());
                    }
                    // Subtract one for this task
                    asyncTasks --;
                    transition(tasks);
                    asyncTasks += tasks.size();
                }
            }
            doExecute(tasks);
        }

        public long getElapsedTime() {
            return System.nanoTime() - lifecycleTime;
        }

        public ServiceController<?> getController() {
            return ServiceControllerImpl.this;
        }

        public void execute(final Runnable command) {
            doExecute(command);
        }
    }

    private final class ChildServiceTarget extends ServiceTargetImpl {
        private volatile boolean valid = true;

        private ChildServiceTarget(final ServiceTargetImpl parentTarget) {
            super(parentTarget);
        }

        <T> ServiceController<T> install(final ServiceBuilderImpl<T> serviceBuilder) throws ServiceRegistryException {
            if (! valid) {
                throw new IllegalStateException("Service target is no longer valid");
            }
            return super.install(serviceBuilder);
        }

        protected <T> ServiceBuilder<T> createServiceBuilder(final ServiceName name, final Value<? extends Service<T>> value, final ServiceControllerImpl<?> parent) throws IllegalArgumentException {
            return super.createServiceBuilder(name, value, ServiceControllerImpl.this);
        }

        @Override
        public ServiceTarget subTarget() {
            return new ChildServiceTarget(this);
        }
    }

    private void writeProfileInfo(final char statusChar, final long startNanos, final long endNanos) {
        final ServiceRegistrationImpl primaryRegistration = this.primaryRegistration;
        final ServiceName name = primaryRegistration.getName();
        final ServiceContainerImpl container = primaryRegistration.getContainer();
        final Writer profileOutput = container.getProfileOutput();
        if (profileOutput != null) {
            synchronized (profileOutput) {
                try {
                    final long startOffset = startNanos - container.getStart();
                    final long duration = endNanos - startNanos;
                    profileOutput.write(String.format("%s\t%s\t%d\t%d\n", name.getCanonicalName(), Character.valueOf(statusChar), Long.valueOf(startOffset), Long.valueOf(duration)));
                } catch (IOException e) {
                    // ignore
                }
            }
        }
    }

    private class StopContextImpl implements StopContext {

        private ContextState state = ContextState.SYNC;

        private final long startNanos;

        private StopContextImpl(final long startNanos) {
            this.startNanos = startNanos;
        }

        public void asynchronous() throws IllegalStateException {
            synchronized (ServiceControllerImpl.this) {
                if (state == ContextState.SYNC) {
                    state = ContextState.ASYNC;
                } else {
                    throw new IllegalStateException(ILLEGAL_CONTROLLER_STATE);
                }
            }
        }

        public void complete() throws IllegalStateException {
            synchronized (ServiceControllerImpl.this) {
                if (state != ContextState.ASYNC) {
                    throw new IllegalStateException(ILLEGAL_CONTROLLER_STATE);
                }
                state = ContextState.COMPLETE;
            }
            for (ValueInjection<?> injection : injections) {
                injection.getTarget().uninject();
            }
            final ArrayList<Runnable> tasks = new ArrayList<Runnable>();
            synchronized (ServiceControllerImpl.this) {
                if (ServiceContainerImpl.PROFILE_OUTPUT != null) {
                    writeProfileInfo('X', startNanos, System.nanoTime());
                }
                // Subtract one for this task
                asyncTasks --;
                transition(tasks);
                asyncTasks += tasks.size();
            }
            doExecute(tasks);
        }

        public ServiceController<?> getController() {
            return ServiceControllerImpl.this;
        }

        public void execute(final Runnable command) {
            doExecute(command);
        }

        public long getElapsedTime() {
            return System.nanoTime() - lifecycleTime;
        }
    }

    enum Substate {
        NEW(State.DOWN),
        CANCELLED(State.REMOVED),
        DOWN(State.DOWN),
        START_REQUESTED(State.DOWN),
        STARTING(State.STARTING),
        START_FAILED(State.START_FAILED),
        UP(State.UP),
        STOP_REQUESTED(State.UP),
        STOPPING(State.STOPPING),
        REMOVING(State.DOWN),
        REMOVED(State.REMOVED),
        ;
        private final ServiceController.State state;

        Substate(final ServiceController.State state) {
            this.state = state;
        }

        public ServiceController.State getState() {
            return state;
        }
    }

    enum Transition {
        START_REQUESTED_to_DOWN(Substate.START_REQUESTED, Substate.DOWN),
        START_REQUESTED_to_STARTING(Substate.START_REQUESTED, Substate.STARTING),
        STARTING_to_UP(Substate.STARTING, Substate.UP),
        STARTING_to_START_FAILED(Substate.STARTING, Substate.START_FAILED),
        START_FAILED_to_STARTING(Substate.START_FAILED, Substate.STARTING),
        START_FAILED_to_DOWN(Substate.START_FAILED, Substate.DOWN),
        UP_to_STOP_REQUESTED(Substate.UP, Substate.STOP_REQUESTED),
        STOP_REQUESTED_to_UP(Substate.STOP_REQUESTED, Substate.UP),
        STOP_REQUESTED_to_STOPPING(Substate.STOP_REQUESTED, Substate.STOPPING),
        STOPPING_to_DOWN(Substate.STOPPING, Substate.DOWN),
        REMOVING_to_REMOVED(Substate.REMOVING, Substate.REMOVED),
        DOWN_to_REMOVING(Substate.DOWN, Substate.REMOVING),
        DOWN_to_START_REQUESTED(Substate.DOWN, Substate.START_REQUESTED),
        ;

        private final Substate before;
        private final Substate after;

        Transition(final Substate before, final Substate after) {
            this.before = before;
            this.after = after;
        }

        public Substate getBefore() {
            return before;
        }

        public Substate getAfter() {
            return after;
        }
    }
}
