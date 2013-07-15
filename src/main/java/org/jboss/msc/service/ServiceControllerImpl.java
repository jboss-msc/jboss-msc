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
import java.security.AccessController;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.jboss.msc.service.management.ServiceStatus;
import org.jboss.msc.value.Value;

/**
 * The service controller implementation.
 *
 * @param <S> the service type
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 * @author <a href="mailto:flavia.rainone@jboss.com">Flavia Rainone</a>
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
final class ServiceControllerImpl<S> implements ServiceController<S>, Dependent {

    private static final String ILLEGAL_CONTROLLER_STATE = "Illegal controller state";

    /**
     * The service itself.
     */
    private final Value<? extends Service<S>> serviceValue;
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
     * The set of registered stability monitors.
     */
    private final IdentityHashSet<StabilityMonitor> monitors;
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
     * The immediate unavailable dependencies of this service.
     */
    private final IdentityHashSet<ServiceName> immediateUnavailableDependencies;
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
     * dependents. If this value is >0 and mode is ON_DEMAND, we should start.
     */
    private int demandedByCount;
    /**
     * The number of dependencies which are not yet started.  This service cannot start until
     * this count reaches zero.
     */
    private int unstartedDependencies;
    /**
     * Count for dependencies that are trying to stop.  If this count is greater than zero then
     * dependents will be notified that a stop is necessary.
     */
    private int stoppingDependencies;
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
     * Indicates if this service has one or more transitive dependencies that
     * are not available. Count for notification of unavailable dependencies.
     * Its value indicates how many transitive dependencies are unavailable.
     * When incremented from 0 to 1, dependents are notified of the unavailable
     * dependency unless immediateUnavailableDependencies is not empty. When
     * decremented from 1 to 0, a notification that the unavailable dependencies
     * are now available is sent to dependents, unless immediateUnavailableDependencies
     * is not empty. Values larger than 1 are ignored to avoid multiple
     * notifications.
     */
    private int transitiveUnavailableDepCount;
    /**
     * Indicates whether dependencies have been demanded.
     */
    private boolean dependenciesDemanded = false;
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
    @SuppressWarnings("VolatileLongOrDoubleField")
    private volatile long lifecycleTime;

    private static final Dependent[] NO_DEPENDENTS = new Dependent[0];
    private static final ServiceControllerImpl<?>[] NO_CONTROLLERS = new ServiceControllerImpl<?>[0];
    private static final String[] NO_STRINGS = new String[0];

    static final int MAX_DEPENDENCIES = (1 << 14) - 1;

    ServiceControllerImpl(final Value<? extends Service<S>> serviceValue, final Dependency[] dependencies, final ValueInjection<?>[] injections, final ValueInjection<?>[] outInjections, final ServiceRegistrationImpl primaryRegistration, final ServiceRegistrationImpl[] aliasRegistrations, final Set<StabilityMonitor> monitors, final Set<? extends ServiceListener<? super S>> listeners, final ServiceControllerImpl<?> parent) {
        assert dependencies.length <= MAX_DEPENDENCIES;
        this.serviceValue = serviceValue;
        this.dependencies = dependencies;
        this.injections = injections;
        this.outInjections = outInjections;
        this.primaryRegistration = primaryRegistration;
        this.aliasRegistrations = aliasRegistrations;
        this.listeners = new IdentityHashSet<ServiceListener<? super S>>(listeners);
        this.monitors = new IdentityHashSet<StabilityMonitor>(monitors);
        // We also need to register this controller with monitors explicitly.
        // This allows inherited monitors to have registered all child controllers
        // and later to remove them when inherited stability monitor is cleared.
        for (final StabilityMonitor monitor : monitors) {
            monitor.addControllerNoCallback(this);
        }
        this.parent = parent;
        int depCount = dependencies.length;
        unstartedDependencies = 0;
        stoppingDependencies = parent == null? depCount : depCount + 1;
        children = new IdentityHashSet<ServiceControllerImpl<?>>();
        immediateUnavailableDependencies = new IdentityHashSet<ServiceName>();
    }

    Substate getSubstateLocked() {
        return state;
    }

    void addAsyncTasks(final int size) {
        asyncTasks += size;
    }

    void removeAsyncTask() {
        asyncTasks--;
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
        assert !holdsLock(this);
        final ArrayList<Runnable> listenerAddedTasks = new ArrayList<Runnable>(16);
        final ArrayList<Runnable> tasks = new ArrayList<Runnable>(16);

        synchronized (this) {
            final boolean leavingRestState = isStableRestState();
            getListenerTasks(ListenerNotification.LISTENER_ADDED, listenerAddedTasks);
            internalSetMode(initialMode, tasks);
            // placeholder async task for running listener added tasks
            asyncTasks += listenerAddedTasks.size() + tasks.size() + 1;
            updateStabilityState(leavingRestState);
        }
        doExecute(tasks);
        tasks.clear();
        for (Runnable listenerAddedTask : listenerAddedTasks) {
            listenerAddedTask.run();
        }
        synchronized (this) {
            final boolean leavingRestState = isStableRestState();
            for (Map.Entry<ServiceName, Dependent[]> dependentEntry : getDependentsByDependencyName().entrySet()) {
                ServiceName serviceName = dependentEntry.getKey();
                for (Dependent dependent : dependentEntry.getValue()) {
                    if (dependent != null) dependent.immediateDependencyAvailable(serviceName);
                }
            }
            Dependent[][] dependents = getDependents();
            if (!immediateUnavailableDependencies.isEmpty() || transitiveUnavailableDepCount > 0) {
                for (Dependent[] dependentArray : dependents) {
                    for (Dependent dependent : dependentArray) {
                        if (dependent != null) {
                            dependent.transitiveDependencyUnavailable();
                        }
                    }
                }
            }
            if (failCount > 0) {
                tasks.add(new DependencyFailedTask(dependents, false));
            }
            state = Substate.DOWN;
            // subtract one to compensate for +1 above
            asyncTasks--;
            transition(tasks);
            asyncTasks += tasks.size();
            updateStabilityState(leavingRestState);
        }
        doExecute(tasks);
    }

    /**
     * Roll back the service install.
     */
    void rollbackInstallation() {
        synchronized(this) {
            final boolean leavingRestState = isStableRestState();
            mode = Mode.REMOVE;
            asyncTasks ++;
            state = Substate.CANCELLED;
            updateStabilityState(leavingRestState);
        }
        (new RemoveTask()).run();
    }

    /**
     * Return {@code true} only if this service controller installation is committed.
     *
     * @return true if this service controller installation is committed
     */
    boolean isInstallationCommitted() {
        assert holdsLock(this);
        // should not be NEW nor CANCELLED
        return state.compareTo(Substate.CANCELLED) > 0;
    }

    /**
     * Determine whether a stopped controller should start.
     *
     * @return {@code true} if so
     */
    private boolean shouldStart() {
        assert holdsLock(this);
        return unstartedDependencies == 0 && (mode == Mode.ACTIVE || mode == Mode.PASSIVE || demandedByCount > 0 && (mode == Mode.ON_DEMAND || mode == Mode.LAZY));
    }

    /**
     * Determine whether a running controller should stop.
     *
     * @return {@code true} if so
     */
    private boolean shouldStop() {
        assert holdsLock(this);
        return unstartedDependencies > 0 || (mode == Mode.NEVER || mode == Mode.REMOVE || demandedByCount == 0 && mode == Mode.ON_DEMAND);
    }

    /**
     * Returns true if controller is in rest state and no async tasks are running, false otherwise.
     * @return true if stable rest state, false otherwise
     */
    boolean isStableRestState() {
        assert holdsLock(this);
        return asyncTasks == 0 && state.isRestState();
    }

    void updateStabilityState(final boolean leavingStableRestState) {
        assert holdsLock(this);
        final boolean enteringStableRestState = state.isRestState() && asyncTasks == 0;
        if (leavingStableRestState) {
            if (!enteringStableRestState) {
                primaryRegistration.getContainer().incrementUnstableServices();
                for (StabilityMonitor monitor : monitors) {
                    monitor.incrementUnstableServices();
                }
            }
        } else {
            if (enteringStableRestState) {
                primaryRegistration.getContainer().decrementUnstableServices();
                for (StabilityMonitor monitor : monitors) {
                    monitor.decrementUnstableServices();
                }
            }
        }
    }


    /**
     * Identify the transition to take.  Call under lock.
     *
     * @return the transition or {@code null} if none is needed at this time
     */
    private Transition getTransition() {
        assert holdsLock(this);
        switch (state) {
            case DOWN: {
                if (mode == ServiceController.Mode.REMOVE) {
                    return Transition.DOWN_to_REMOVING;
                } else if (mode == ServiceController.Mode.NEVER) {
                    return Transition.DOWN_to_WONT_START;
                } else if (shouldStart() && (mode != Mode.PASSIVE || stoppingDependencies == 0)) {
                    return Transition.DOWN_to_START_REQUESTED;
                } else {
                    // mode is either LAZY or ON_DEMAND with demandedByCount == 0, or mode is PASSIVE and downDep > 0
                    return Transition.DOWN_to_WAITING;
                }
            }
            case WAITING: {
                if (((mode != Mode.ON_DEMAND && mode != Mode.LAZY) || demandedByCount > 0) &&
                        (mode != Mode.PASSIVE || stoppingDependencies == 0)) {
                    return Transition.WAITING_to_DOWN;
                }
                break;
            }
            case WONT_START: {
                if (mode != ServiceController.Mode.NEVER){
                    return Transition.WONT_START_to_DOWN;
                }
                break;
            }
            case STOPPING: {
                return Transition.STOPPING_to_DOWN;
            }
            case STOP_REQUESTED: {
                if (shouldStart() && stoppingDependencies == 0) {
                    return Transition.STOP_REQUESTED_to_UP;
                }
                if (runningDependents == 0) {
                    return Transition.STOP_REQUESTED_to_STOPPING;
                }
                break;
            }
            case UP: {
                if (shouldStop() || stoppingDependencies > 0) {
                    return Transition.UP_to_STOP_REQUESTED;
                }
                break;
            }
            case START_FAILED: {
                if (shouldStart()) {
                    if (stoppingDependencies == 0) {
                        if (startException == null) {
                            return Transition.START_FAILED_to_STARTING;
                        }
                    } else {
                        return Transition.START_FAILED_to_DOWN;
                    }
                } else {
                    return Transition.START_FAILED_to_DOWN;
                }
                break;
            }
            case START_INITIATING: {
                return Transition.START_INITIATING_to_STARTING;
            }
            case STARTING: {
                if (startException == null) {
                    return Transition.STARTING_to_UP;
                } else {
                    return Transition.STARTING_to_START_FAILED;
                }
            }
            case START_REQUESTED: {
                if (shouldStart()) {
                    if (mode == Mode.PASSIVE && stoppingDependencies > 0) {
                        return Transition.START_REQUESTED_to_DOWN;
                    }
                    if (!immediateUnavailableDependencies.isEmpty() || transitiveUnavailableDepCount > 0 || failCount > 0) {
                        return Transition.START_REQUESTED_to_PROBLEM;
                    }
                    else if (stoppingDependencies == 0) {
                        return Transition.START_REQUESTED_to_START_INITIATING;
                    }
                } else {
                    return Transition.START_REQUESTED_to_DOWN;
                }
                break;
            }
            case PROBLEM: {
                if (! shouldStart() || (immediateUnavailableDependencies.isEmpty() && transitiveUnavailableDepCount == 0 && failCount == 0) || mode == Mode.PASSIVE) {
                    return Transition.PROBLEM_to_START_REQUESTED;
                }
                break;
            }
            case REMOVING: {
                if (mode == Mode.REMOVE) {
                    return Transition.REMOVING_to_REMOVED;
                } else {
                    return Transition.REMOVING_to_DOWN;
                }
            }
            case CANCELLED:{
                if (mode == Mode.REMOVE){
                    return Transition.CANCELLED_to_REMOVED;
                }
            }
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
        Transition transition;
        do {
            if (asyncTasks != 0) {
                // no movement possible
                return;
            }
            // first of all, check if parents should be demanded/undemanded
            switch (mode) {
                case NEVER:
                case REMOVE:
                    if (dependenciesDemanded) {
                        tasks.add(new UndemandDependenciesTask());
                        dependenciesDemanded = false;
                    }
                    break;
                case LAZY: {
                    if (state.getState() == State.UP && state != Substate.STOP_REQUESTED) {
                        if (!dependenciesDemanded) {
                            tasks.add(new DemandDependenciesTask());
                            dependenciesDemanded = true;
                        }
                        break;
                    }
                    // fall thru!
                }
                case ON_DEMAND:
                case PASSIVE: {
                    if (demandedByCount > 0 && !dependenciesDemanded) {
                        tasks.add(new DemandDependenciesTask());
                        dependenciesDemanded = true;
                    } else if (demandedByCount == 0 && dependenciesDemanded) {
                        tasks.add(new UndemandDependenciesTask());
                        dependenciesDemanded = false;
                    }
                    break;
                }
                case ACTIVE: {
                    if (!dependenciesDemanded) {
                        tasks.add(new DemandDependenciesTask());
                        dependenciesDemanded = true;
                    }
                    break;
                }
            }
            transition = getTransition();
            if (transition == null) {
                return;
            }
            switch (transition) {
                case DOWN_to_WAITING: {
                    getListenerTasks(transition, tasks);
                    break;
                }
                case WAITING_to_DOWN: {
                    getListenerTasks(transition, tasks);
                    break;
                }
                case DOWN_to_WONT_START: {
                    getListenerTasks(transition, tasks);
                    tasks.add(new ServiceUnavailableTask());
                    break;
                }
                case WONT_START_to_DOWN: {
                    getListenerTasks(transition, tasks);
                    tasks.add(new ServiceAvailableTask());
                    break;
                }
                case STOPPING_to_DOWN: {
                    getListenerTasks(transition, tasks);
                    tasks.add(new DependentStoppedTask());
                    break;
                }
                case START_REQUESTED_to_DOWN: {
                    getListenerTasks(transition, tasks);
                    break;
                }
                case START_REQUESTED_to_START_INITIATING: {
                    tasks.add(new DependentStartedTask());
                    break;
                }
                case START_REQUESTED_to_PROBLEM: {
                    getPrimaryRegistration().getContainer().addProblem(this);
                    for (StabilityMonitor monitor : monitors) {
                        monitor.addProblem(this);
                    }
                    if (!immediateUnavailableDependencies.isEmpty()) {
                        getListenerTasks(ListenerNotification.IMMEDIATE_DEPENDENCY_UNAVAILABLE, tasks);
                    }
                    if (transitiveUnavailableDepCount > 0) {
                        getListenerTasks(ListenerNotification.TRANSITIVE_DEPENDENCY_UNAVAILABLE, tasks);
                    }
                    if (failCount > 0) {
                        getListenerTasks(ListenerNotification.DEPENDENCY_FAILURE, tasks);
                    }
                    getListenerTasks(transition, tasks);
                    break;
                }
                case UP_to_STOP_REQUESTED: {
                    if (mode == Mode.LAZY && demandedByCount == 0) {
                        assert dependenciesDemanded;
                        tasks.add(new UndemandDependenciesTask());
                        dependenciesDemanded = false;
                    }
                    getListenerTasks(transition, tasks);
                    lifecycleTime = System.nanoTime();
                    tasks.add(new DependencyStoppedTask(getDependents()));
                    break;
                }
                case STARTING_to_UP: {
                    getListenerTasks(transition, tasks);
                    tasks.add(new DependencyStartedTask(getDependents()));
                    break;
                }
                case STARTING_to_START_FAILED: {
                    getPrimaryRegistration().getContainer().addFailed(this);
                    for (StabilityMonitor monitor : monitors) {
                        monitor.addFailed(this);
                    }
                    ChildServiceTarget childTarget = this.childTarget;
                    if (childTarget != null) {
                        childTarget.valid = false;
                        this.childTarget = null;
                    }
                    getListenerTasks(transition, tasks);
                    tasks.add(new DependencyFailedTask(getDependents(), true));
                    break;
                }
                case START_FAILED_to_STARTING: {
                    getPrimaryRegistration().getContainer().removeFailed(this);
                    for (StabilityMonitor monitor : monitors) {
                        monitor.removeFailed(this);
                    }
                    getListenerTasks(transition, tasks);
                    tasks.add(new DependencyRetryingTask(getDependents()));
                    tasks.add(new DependentStartedTask());
                    break;
                }
                case START_INITIATING_to_STARTING: {
                    getListenerTasks(transition, tasks);
                    tasks.add(new StartTask(true));
                    break;
                }
                case START_FAILED_to_DOWN: {
                    getPrimaryRegistration().getContainer().removeFailed(this);
                    for (StabilityMonitor monitor : monitors) {
                        monitor.removeFailed(this);
                    }
                    startException = null;
                    failCount--;
                    getListenerTasks(transition, tasks);
                    tasks.add(new DependencyRetryingTask(getDependents()));
                    tasks.add(new StopTask(true));
                    tasks.add(new DependentStoppedTask());
                    break;
                }
                case STOP_REQUESTED_to_UP: {
                    getListenerTasks(transition, tasks);
                    tasks.add(new DependencyStartedTask(getDependents()));
                    break;
                }
                case STOP_REQUESTED_to_STOPPING: {
                    ChildServiceTarget childTarget = this.childTarget;
                    if (childTarget != null) {
                        childTarget.valid = false;
                        this.childTarget = null;
                    }
                    getListenerTasks(transition, tasks);
                    tasks.add(new StopTask(false));
                    break;
                }
                case DOWN_to_REMOVING: {
                    tasks.add(new ServiceUnavailableTask());
                    Dependent[][] dependents = getDependents();
                    // Clear all dependency uninstalled flags from dependents
                    if (!immediateUnavailableDependencies.isEmpty() || transitiveUnavailableDepCount > 0) {
                        for (Dependent[] dependentArray : dependents) {
                            for (Dependent dependent : dependentArray) {
                                if (dependent != null) dependent.transitiveDependencyAvailable();
                            }
                        }
                    }
                    if (failCount > 0) {
                        tasks.add(new DependencyRetryingTask(dependents));
                    }
                    tasks.add(new RemoveTask());
                    break;
                }
                case CANCELLED_to_REMOVED:
                case REMOVING_to_REMOVED: {
                    getListenerTasks(transition, tasks);
                    listeners.clear();
                    for (final StabilityMonitor monitor : monitors) {
                        monitor.removeControllerNoCallback(this);
                    }
                    break;
                }
                case REMOVING_to_DOWN: {
                    break;
                }
                case DOWN_to_START_REQUESTED: {
                    getListenerTasks(transition, tasks);
                    break;
                }
                case PROBLEM_to_START_REQUESTED: {
                    getPrimaryRegistration().getContainer().removeProblem(this);
                    for (StabilityMonitor monitor : monitors) {
                        monitor.removeProblem(this);
                    }
                    if (!immediateUnavailableDependencies.isEmpty()) {
                        getListenerTasks(ListenerNotification.IMMEDIATE_DEPENDENCY_AVAILABLE, tasks);
                    }
                    if (transitiveUnavailableDepCount > 0) {
                        getListenerTasks(ListenerNotification.TRANSITIVE_DEPENDENCY_AVAILABLE, tasks);
                    }
                    if (failCount > 0) {
                        getListenerTasks(ListenerNotification.DEPENDENCY_FAILURE_CLEAR, tasks);
                    }
                    getListenerTasks(transition, tasks);
                    lifecycleTime = System.nanoTime();
                    break;
                }
                default: {
                    throw new IllegalStateException();
                }
            }
            state = transition.getAfter();
        } while (tasks.isEmpty());
        // Notify waiters that a transition occurred
        notifyAll();
    }

    private void getListenerTasks(final Transition transition, final ArrayList<Runnable> tasks) {
        for (ServiceListener<? super S> listener : listeners) {
            tasks.add(new ListenerTask(listener, transition));
        }
    }

    private void getListenerTasks(final ListenerNotification notification, final ArrayList<Runnable> tasks) {
        for (ServiceListener<? super S> listener : listeners) {
            tasks.add(new ListenerTask(listener, notification));
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
            final boolean leavingRestState = isStableRestState();
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
            updateStabilityState(leavingRestState);
        }
        doExecute(tasks);
        return true;
    }

    private void internalSetMode(final Mode newMode, final ArrayList<Runnable> taskList) {
        assert holdsLock(this);
        final ServiceController.Mode oldMode = mode;
        if (oldMode == Mode.REMOVE) {
            if (state.compareTo(Substate.REMOVING) >= 0) {
                throw new IllegalStateException("Service already removed");
            }
            getListenerTasks(ListenerNotification.REMOVE_REQUEST_CLEARED, taskList);
        }
        if (newMode == Mode.REMOVE) {
            getListenerTasks(ListenerNotification.REMOVE_REQUESTED, taskList);
        }
        mode = newMode;
    }

    @Override
    public void immediateDependencyAvailable(ServiceName dependencyName) {
        final ArrayList<Runnable> tasks;
        synchronized (this) {
            final boolean leavingRestState = isStableRestState();
            assert immediateUnavailableDependencies.contains(dependencyName);
            immediateUnavailableDependencies.remove(dependencyName);
            if (!immediateUnavailableDependencies.isEmpty() || state.compareTo(Substate.CANCELLED) <= 0 || state.compareTo(Substate.REMOVING) >= 0) {
                return;
            }
            // we dropped it to 0
            tasks = new ArrayList<Runnable>(16);
            if (state == Substate.PROBLEM) {
                getListenerTasks(ListenerNotification.IMMEDIATE_DEPENDENCY_AVAILABLE, tasks);
            }
            // both unavailable dep counts are 0
            if (transitiveUnavailableDepCount == 0) {
                transition(tasks);
                propagateTransitiveAvailability();
            }
            asyncTasks += tasks.size();
            updateStabilityState(leavingRestState);
        }
        doExecute(tasks);
    }

    @Override
    public void immediateDependencyUnavailable(ServiceName dependencyName) {
        final ArrayList<Runnable> tasks;
        synchronized (this) {
            final boolean leavingRestState = isStableRestState();
            immediateUnavailableDependencies.add(dependencyName);
            if (immediateUnavailableDependencies.size() != 1 || state.compareTo(Substate.CANCELLED) <= 0 || state.compareTo(Substate.REMOVING) >= 0) {
                return;
            }
            // we raised it to 1
            tasks = new ArrayList<Runnable>(16);
            if (state == Substate.PROBLEM) {
                getListenerTasks(ListenerNotification.IMMEDIATE_DEPENDENCY_UNAVAILABLE, tasks);
            }
            // if this is the first unavailable dependency, we need to notify dependents;
            // otherwise, they have already been notified
            if (transitiveUnavailableDepCount == 0) {
                transition(tasks);
                propagateTransitiveUnavailability();
            }
            asyncTasks += tasks.size();
            updateStabilityState(leavingRestState);
        }
        doExecute(tasks);
    }

    private void propagateTransitiveUnavailability() {
        assert Thread.holdsLock(this);
        for (Dependent[] dependentArray : getDependents()) {
            for (Dependent dependent : dependentArray) {
                if (dependent != null) dependent.transitiveDependencyUnavailable();
            }
        }
    }

    private void propagateTransitiveAvailability() {
        for (Dependent[] dependentArray : getDependents()) {
            for (Dependent dependent : dependentArray) {
                if (dependent != null) dependent.transitiveDependencyAvailable();
            }
        }
    }

    @Override
    public void transitiveDependencyAvailable() {
        final ArrayList<Runnable> tasks;
        synchronized (this) {
            final boolean leavingRestState = isStableRestState();
            if (-- transitiveUnavailableDepCount != 0 || state.compareTo(Substate.CANCELLED) <= 0 || state.compareTo(Substate.REMOVING) >= 0) {
                return;
            }
            // we dropped it to 0
            tasks = new ArrayList<Runnable>(16);
            if (state == Substate.PROBLEM) {
                getListenerTasks(ListenerNotification.TRANSITIVE_DEPENDENCY_AVAILABLE, tasks);
            }
            // there are no immediate nor transitive unavailable dependencies
            if (immediateUnavailableDependencies.isEmpty()) {
                transition(tasks);
                propagateTransitiveAvailability();
            }
            asyncTasks += tasks.size();
            updateStabilityState(leavingRestState);
        }
        doExecute(tasks);
    }

    @Override
    public void transitiveDependencyUnavailable() {
        final ArrayList<Runnable> tasks;
        synchronized (this) {
            final boolean leavingRestState = isStableRestState();
            if (++ transitiveUnavailableDepCount != 1 || state.compareTo(Substate.CANCELLED) <= 0 || state.compareTo(Substate.REMOVING) >= 0) {
                return;
            }
            // we raised it to 1
            tasks = new ArrayList<Runnable>(16);
            if (state == Substate.PROBLEM) {
                getListenerTasks(ListenerNotification.TRANSITIVE_DEPENDENCY_UNAVAILABLE, tasks);
            }
            //if this is the first unavailable dependency, we need to notify dependents;
            // otherwise, they have already been notified
            if (immediateUnavailableDependencies.isEmpty()) {
                transition(tasks);
                propagateTransitiveUnavailability();
            }
            asyncTasks += tasks.size();
            updateStabilityState(leavingRestState);
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
            final boolean leavingRestState = isStableRestState();
            if (--stoppingDependencies != 0) {
                return;
            }
            // we dropped it to 0
            tasks = new ArrayList<Runnable>();
            transition(tasks);
            asyncTasks += tasks.size();
            updateStabilityState(leavingRestState);
        }
        doExecute(tasks);
    }

    @Override
    public void immediateDependencyDown() {
        final ArrayList<Runnable> tasks;
        synchronized (this) {
            final boolean leavingRestState = isStableRestState();
            if (++stoppingDependencies != 1) {
                return;
            }
            // we dropped it below 0
            tasks = new ArrayList<Runnable>();
            transition(tasks);
            asyncTasks += tasks.size();
            updateStabilityState(leavingRestState);
        }
        doExecute(tasks);
    }

    @Override
    public void dependencyFailed() {
        final ArrayList<Runnable> tasks;
        synchronized (this) {
            final boolean leavingRestState = isStableRestState();
            if (++failCount != 1 || state.compareTo(Substate.CANCELLED) <= 0) {
                return;
            }
            // we raised it to 1
            tasks = new ArrayList<Runnable>();
            if (state == Substate.PROBLEM) {
                getListenerTasks(ListenerNotification.DEPENDENCY_FAILURE, tasks);
            }
            tasks.add(new DependencyFailedTask(getDependents(), false));
            asyncTasks += tasks.size();
            updateStabilityState(leavingRestState);
        }
        doExecute(tasks);
    }

    @Override
    public void dependencyFailureCleared() {
        final ArrayList<Runnable> tasks;
        synchronized (this) {
            final boolean leavingRestState = isStableRestState();
            if (--failCount != 0 || state == Substate.CANCELLED) {
                return;
            }
            // we dropped it to 0
            tasks = new ArrayList<Runnable>();
            if (state == Substate.PROBLEM) {
                getListenerTasks(ListenerNotification.DEPENDENCY_FAILURE_CLEAR, tasks);
            }
            tasks.add(new DependencyRetryingTask(getDependents()));
            asyncTasks += tasks.size();
            updateStabilityState(leavingRestState);
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
            final boolean leavingRestState = isStableRestState();
            if (--runningDependents != 0) {
                return;
            }
            tasks = new ArrayList<Runnable>();
            transition(tasks);
            asyncTasks += tasks.size();
            updateStabilityState(leavingRestState);
        }
        doExecute(tasks);
    }

    void newDependent(final ServiceName dependencyName, final Dependent dependent) {
        assert holdsLock(this);
        if (failCount > 0 && state != Substate.STARTING) {
            // if starting and failCount is 1, dependents have not been notified yet...
            // hence, skip it to avoid duplicate notification
            dependent.dependencyFailed();
        }
        if (!immediateUnavailableDependencies.isEmpty() || transitiveUnavailableDepCount > 0) {
            dependent.transitiveDependencyUnavailable();
        }
        if (state == Substate.WONT_START) {
            dependent.immediateDependencyUnavailable(dependencyName);
        } else if (state.getState() == State.UP && state != Substate.STOP_REQUESTED) {
            dependent.immediateDependencyUp();
        }
    }

    private void doDemandDependencies() {
        assert !holdsLock(this);
        for (Dependency dependency : dependencies) {
            dependency.addDemand();
        }
        final ServiceControllerImpl<?> parent = this.parent;
        if (parent != null) parent.addDemand();
    }

    private void doUndemandDependencies() {
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
            final boolean leavingRestState = isStableRestState();
            final int cnt = this.demandedByCount;
            this.demandedByCount += demandedByCount;
            boolean notStartedLazy = mode == Mode.LAZY && !(state.getState() == State.UP && state != Substate.STOP_REQUESTED);
            propagate = cnt == 0 && (mode == Mode.ON_DEMAND || notStartedLazy || mode == Mode.PASSIVE);
            if (propagate) {
                transition(tasks);
            }
            asyncTasks += tasks.size();
            updateStabilityState(leavingRestState);
        }
        doExecute(tasks);
    }

    void removeDemand() {
        assert !holdsLock(this);
        final ArrayList<Runnable> tasks = new ArrayList<Runnable>();
        final boolean propagate;
        synchronized (this) {
            final boolean leavingRestState = isStableRestState();
            final int cnt = --demandedByCount;
            boolean notStartedLazy = mode == Mode.LAZY && !(state.getState() == State.UP && state != Substate.STOP_REQUESTED);
            propagate = cnt == 0 && (mode == Mode.ON_DEMAND || notStartedLazy || mode == Mode.PASSIVE);
            if (propagate) {
                transition(tasks);
            }
            asyncTasks += tasks.size();
            updateStabilityState(leavingRestState);
        }
        doExecute(tasks);
    }

    void addChild(ServiceControllerImpl<?> child) {
        assert !holdsLock(this);
        synchronized (this) {
            switch (state) {
                case START_INITIATING:
                case STARTING:
                case UP:
                case STOP_REQUESTED: {
                    children.add(child);
                    newDependent(primaryRegistration.getName(), child);
                    break;
                }
                default: throw new IllegalStateException("Children cannot be added in state " + state.getState());
            }
        }
    }

    void removeChild(ServiceControllerImpl<?> child) {
        assert !holdsLock(this);
        final ArrayList<Runnable> tasks;
        synchronized (this) {
            final boolean leavingRestState = isStableRestState();
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
            updateStabilityState(leavingRestState);
        }
        doExecute(tasks);
    }

    IdentityHashSet<ServiceControllerImpl<?>> getChildren() {
        assert holdsLock(this);
        return children;
    }

    public ServiceControllerImpl<?> getParent() {
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

    public S awaitValue() throws IllegalStateException, InterruptedException {
        assert !holdsLock(this);
        synchronized (this) {
            for (;;) switch (state.getState()) {
                case UP: {
                    return serviceValue.getValue().getValue();
                }
                case START_FAILED: {
                    throw new IllegalStateException("Failed to start service", startException);
                }
                case REMOVED: {
                    throw new IllegalStateException("Service was removed");
                }
                default: {
                    wait();
                }
            }
        }
    }

    public S awaitValue(final long time, final TimeUnit unit) throws IllegalStateException, InterruptedException, TimeoutException {
        assert !holdsLock(this);
        long now;
        long then = System.nanoTime();
        long remaining = unit.toNanos(time);
        synchronized (this) {
            do {
                switch (state.getState()) {
                    case UP: {
                        return serviceValue.getValue().getValue();
                    }
                    case START_FAILED: {
                        throw new IllegalStateException("Failed to start service", startException);
                    }
                    case REMOVED: {
                        throw new IllegalStateException("Service was removed");
                    }
                    default: {
                        wait(remaining / 1000000L, (int) (remaining % 1000000L));
                    }
                }
                // When will then be now?
                now = System.nanoTime();
                remaining -= now - then;
                // soon...
                then = now;
            } while (remaining > 0L);
            throw new TimeoutException("Operation timed out");
        }
    }

    public Service<S> getService() throws IllegalStateException {
        return serviceValue.getValue();
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

    public void addListener(final ServiceListener<? super S> listener) {
        assert !holdsLock(this);
        final Substate state;
        synchronized (this) {
            final boolean leavingRestState = isStableRestState();
            state = this.state;
            // Always run listener if removed.
            if (state != Substate.REMOVED) {
                if (listeners.contains(listener)) {
                    // Duplicates not allowed
                    throw new IllegalArgumentException("Listener " + listener + " already present on controller for " + primaryRegistration.getName());
                }
                listeners.add(listener);
                asyncTasks ++;
            } else {
                asyncTasks += 2;
            }
            updateStabilityState(leavingRestState);
        }
        invokeListener(listener, ListenerNotification.LISTENER_ADDED, null);
        if (state == Substate.REMOVED) {
            invokeListener(listener, ListenerNotification.TRANSITION, Transition.REMOVING_to_REMOVED);
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
            final boolean leavingRestState = isStableRestState();
            if (state.getState() != ServiceController.State.START_FAILED) {
                return;
            }
            failCount--;
            assert failCount == 0;
            startException = null;
            transition(tasks = new ArrayList<Runnable>());
            asyncTasks += tasks.size();
            updateStabilityState(leavingRestState);
        }
        doExecute(tasks);
    }

    @Override
    public synchronized Set<ServiceName> getImmediateUnavailableDependencies() {
        return immediateUnavailableDependencies.clone();
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
            StartException startException = this.startException;
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
                    startException != null ? startException.toString() : null,
                    !immediateUnavailableDependencies.isEmpty() || transitiveUnavailableDepCount != 0
            );
        }
    }

    String dumpServiceDetails() {
        final StringBuilder b = new StringBuilder();
        IdentityHashSet<Dependent> dependents;
        synchronized (primaryRegistration) {
            dependents = primaryRegistration.getDependents();
            synchronized (dependents) {
                dependents = dependents.clone();
            }
        }
        b.append("Service Name: ").append(primaryRegistration.getName().toString()).append(" - Dependents: ").append(dependents.size()).append('\n');
        for (Dependent dependent : dependents) {
            final ServiceControllerImpl<?> controller = dependent.getController();
            synchronized (controller) {
                b.append("        ").append(controller.getName().toString()).append(" - State: ").append(controller.state.getState()).append(" (Substate: ").append(controller.state).append(")\n");
            }
        }
        b.append("Service Aliases: ").append(aliasRegistrations.length).append('\n');
        for (ServiceRegistrationImpl registration : aliasRegistrations) {
            synchronized (registration) {
                dependents = registration.getDependents();
                synchronized (dependents) {
                    dependents = dependents.clone();
                }
            }
            b.append("    ").append(registration.getName().toString()).append(" - Dependents: ").append(dependents.size()).append('\n');
            for (Dependent dependent : dependents) {
                final ServiceControllerImpl<?> controller = dependent.getController();
                b.append("        ").append(controller.getName().toString()).append(" - State: ").append(controller.state.getState()).append(" (Substate: ").append(controller.state).append(")\n");
            }
        }
        synchronized (this) {
            b.append("Children: ").append(children.size()).append('\n');
            for (ServiceControllerImpl<?> child : children) {
                synchronized (child) {
                    b.append("    ").append(child.getName().toString()).append(" - State: ").append(child.state.getState()).append(" (Substate: ").append(child.state).append(")\n");
                }
            }
            final Substate state = this.state;
            b.append("State: ").append(state.getState()).append(" (Substate: ").append(state).append(")\n");
            if (parent != null) {
                b.append("Parent Name: ").append(parent.getPrimaryRegistration().getName().toString()).append('\n');
            }
            b.append("Service Mode: ").append(mode).append('\n');
            if (startException != null) {
                b.append("Start Exception: ").append(startException.getClass().getName()).append(" (Message: ").append(startException.getMessage()).append(")\n");
            }
            String serviceValueString = "(indeterminate)";
            try {
                serviceValueString = serviceValue.toString();
            } catch (Throwable ignored) {}
            b.append("Service Value: ").append(serviceValueString).append('\n');
            String serviceObjectString = "(indeterminate)";
            Object serviceObjectClass = "(indeterminate)";
            try {
                Object serviceObject = serviceValue.getValue();
                if (serviceObject != null) {
                    serviceObjectClass = serviceObject.getClass();
                    serviceObjectString = serviceObject.toString();
                }
            } catch (Throwable ignored) {}
            b.append("Service Object: ").append(serviceObjectString).append('\n');
            b.append("Service Object Class: ").append(serviceObjectClass).append('\n');
            b.append("Demanded By: ").append(demandedByCount).append('\n');
            b.append("Unstarted Dependencies: ").append(unstartedDependencies).append('\n');
            b.append("Stopping Dependencies: ").append(stoppingDependencies).append('\n');
            b.append("Running Dependents: ").append(runningDependents).append('\n');
            b.append("Fail Count: ").append(failCount).append('\n');
            b.append("Immediate Unavailable Dep Count: ").append(immediateUnavailableDependencies.size()).append('\n');
            for (ServiceName name : immediateUnavailableDependencies) {
                b.append("    ").append(name.toString()).append('\n');
            }
            b.append("Transitive Unavailable Dep Count: ").append(transitiveUnavailableDepCount).append('\n');
            b.append("Dependencies Demanded: ").append(dependenciesDemanded ? "yes" : "no").append('\n');
            b.append("Async Tasks: ").append(asyncTasks).append('\n');
            if (lifecycleTime != 0L) {
                final long elapsedNanos = System.nanoTime() - lifecycleTime;
                final long now = System.currentTimeMillis();
                final long stamp = now - (elapsedNanos / 1000000L);
                b.append("Lifecycle Timestamp: ").append(lifecycleTime).append(String.format(" = %tb %<td %<tH:%<tM:%<tS.%<tL%n", stamp));
            }
        }
        b.append("Dependencies: ").append(dependencies.length).append('\n');
        for (int i = 0; i < dependencies.length; i ++) {
            final Dependency dependency = dependencies[i];
            final ServiceControllerImpl<?> controller = dependency.getDependencyController();
            b.append("    ").append(dependency.getName().toString());
            if (controller == null) {
                b.append(" (missing)\n");
            } else {
                synchronized (controller) {
                    b.append(" - State: ").append(controller.state.getState()).append(" (Substate: ").append(controller.state).append(")\n");
                }
            }
        }
        return b.toString();
    }

    void addMonitor(final StabilityMonitor stabilityMonitor) {
        assert !holdsLock(this);
        synchronized (this) {
            if (monitors.add(stabilityMonitor) && !isStableRestState()) {
                stabilityMonitor.incrementUnstableServices();
                if (state == Substate.START_FAILED) {
                    stabilityMonitor.addFailed(this);
                } else if (state == Substate.PROBLEM) {
                    stabilityMonitor.addProblem(this);
                }
            }
        }
    }

    void removeMonitor(final StabilityMonitor stabilityMonitor) {
        assert !holdsLock(this);
        synchronized (this) {
            if (monitors.remove(stabilityMonitor) && !isStableRestState()) {
                stabilityMonitor.removeProblem(this);
                stabilityMonitor.removeFailed(this);
                stabilityMonitor.decrementUnstableServices();
            }
        }
    }

    void removeMonitorNoCallback(final StabilityMonitor stabilityMonitor) {
        assert !holdsLock(this);
        synchronized (this) {
            monitors.remove(stabilityMonitor);
        }
    }

    Set<StabilityMonitor> getMonitors() {
        assert holdsLock(this);
        return monitors;
    }

    private enum ListenerNotification {
        /** Notify the listener that is has been added. */
        LISTENER_ADDED,
        /** Notifications related to the current state.  */
        TRANSITION,
        /** Notify the listener that a dependency failure occurred. */
        DEPENDENCY_FAILURE,
        /** Notify the listener that all dependency failures are cleared. */
        DEPENDENCY_FAILURE_CLEAR,
        /** Notify the listener that an immediate dependency is unavailable. */
        IMMEDIATE_DEPENDENCY_UNAVAILABLE,
        /** Notify the listener that all previously unavailable immediate dependencies are now available. */
        IMMEDIATE_DEPENDENCY_AVAILABLE,
        /** Notify the listener a transitive dependency is unavailable. */
        TRANSITIVE_DEPENDENCY_UNAVAILABLE,
        /** Notify the listener that all previously unavailable transitive dependencies are now available. */
        TRANSITIVE_DEPENDENCY_AVAILABLE,
        /** Notify the listener that the service is going to be removed. */
        REMOVE_REQUESTED,
        /** Notify the listener that the service is no longer going to be removed. */
        REMOVE_REQUEST_CLEARED
    }

    /**
     * Invokes the listener, performing the notification specified.
     *
     * @param listener      listener to be invoked
     * @param notification  specified notification
     * @param transition    the transition to be notified, only relevant if {@code notification} is
     *                      {@link ListenerNotification#TRANSITION}
     */
    private void invokeListener(final ServiceListener<? super S> listener, final ListenerNotification notification, final Transition transition) {
        assert !holdsLock(this);
        // first set the TCCL
        final ClassLoader contextClassLoader = setTCCL(listener.getClass().getClassLoader());
        try {
            switch (notification) {
                case TRANSITION: {
                    listener.transition(this, transition);
                    break;
                }
                case LISTENER_ADDED: {
                    listener.listenerAdded(this);
                    break;
                }
                case IMMEDIATE_DEPENDENCY_UNAVAILABLE: {
                    listener.immediateDependencyUnavailable(this);
                    break;
                }
                case IMMEDIATE_DEPENDENCY_AVAILABLE: {
                    listener.immediateDependencyAvailable(this);
                    break;
                }
                case TRANSITIVE_DEPENDENCY_UNAVAILABLE: {
                    listener.transitiveDependencyUnavailable(this);
                    break;
                }
                case TRANSITIVE_DEPENDENCY_AVAILABLE: {
                    listener.transitiveDependencyAvailable(this);
                    break;
                }
                case DEPENDENCY_FAILURE: {
                    listener.dependencyFailed(this);
                    break;
                }
                case DEPENDENCY_FAILURE_CLEAR: {
                    listener.dependencyFailureCleared(this);
                    break;
                }
                case REMOVE_REQUESTED: {
                    listener.serviceRemoveRequested(this);
                    break;
                }
                case REMOVE_REQUEST_CLEARED: {
                    listener.serviceRemoveRequestCleared(this);
                    break;
                }
                default: throw new IllegalStateException();
            }
        } catch (Throwable t) {
            ServiceLogger.SERVICE.listenerFailed(t, listener);
        } finally {
            // reset TCCL
            setTCCL(contextClassLoader);
            // perform transition tasks
            final ArrayList<Runnable> tasks = new ArrayList<Runnable>();
            synchronized (this) {
                final boolean leavingRestState = isStableRestState();
                // Subtract one for this executing listener
                asyncTasks --;
                transition(tasks);
                asyncTasks += tasks.size();
                updateStabilityState(leavingRestState);
            }
            doExecute(tasks);
        }
    }

    public Substate getSubstate() {
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
     * @return an array of dependents, including children
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

    /**
     * Returns a compiled map of all dependents of this service mapped by the dependency name.
     * This map can be used when it is necessary to perform notifications to these dependents that require
     * the name of the dependency issuing notification.
     * <br> The return result does not include children.
     *
     * @return an array of dependents, including children
     */
    // children are not included in this this result
    private Map<ServiceName, Dependent[]> getDependentsByDependencyName() {
        final Map<ServiceName, Dependent[]> dependents = new HashMap<ServiceName, Dependent[]>();
        addDependentsByName(primaryRegistration, dependents);
        for (ServiceRegistrationImpl aliasRegistration: aliasRegistrations) {
            addDependentsByName(aliasRegistration, dependents);
        }
        return dependents;
    }

    private void addDependentsByName(ServiceRegistrationImpl registration, Map<ServiceName, Dependent[]> dependentsByName) {
        IdentityHashSet<Dependent> registrationDependents = registration.getDependents();
        synchronized(registrationDependents) {
            dependentsByName.put(registration.getName(), registrationDependents.toScatteredArray(NO_DEPENDENTS));
        }
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

    private static ClassLoader setTCCL(ClassLoader newTCCL) {
        final SecurityManager sm = System.getSecurityManager();
        final SetTCCLAction setTCCLAction = new SetTCCLAction(newTCCL);
        if (sm != null) {
            return AccessController.doPrivileged(setTCCLAction);
        } else {
            return setTCCLAction.run();
        }
    }

    @Override
    public String toString() {
        return String.format("Controller for %s@%x", getName(), Integer.valueOf(hashCode()));
    }

    private class DemandDependenciesTask implements Runnable {

        public void run() {
            try {
                doDemandDependencies();
                final ArrayList<Runnable> tasks = new ArrayList<Runnable>();
                synchronized (ServiceControllerImpl.this) {
                    final boolean leavingRestState = isStableRestState();
                    // Subtract one for this task
                    asyncTasks --;
                    transition(tasks);
                    asyncTasks += tasks.size();
                    updateStabilityState(leavingRestState);
                }
                doExecute(tasks);
            } catch (Throwable t) {
                ServiceLogger.SERVICE.internalServiceError(t, primaryRegistration.getName());
            }
        }
    }

    private class UndemandDependenciesTask implements Runnable {

        public void run() {
            try {
                doUndemandDependencies();
                final ArrayList<Runnable> tasks = new ArrayList<Runnable>();
                synchronized (ServiceControllerImpl.this) {
                    final boolean leavingRestState = isStableRestState();
                    // Subtract one for this task
                    asyncTasks --;
                    transition(tasks);
                    asyncTasks += tasks.size();
                    updateStabilityState(leavingRestState);
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
                    final boolean leavingRestState = isStableRestState();
                    // Subtract one for this task
                    asyncTasks --;
                    transition(tasks);
                    asyncTasks += tasks.size();
                    updateStabilityState(leavingRestState);
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
                    final boolean leavingRestState = isStableRestState();
                    // Subtract one for this task
                    asyncTasks --;
                    transition(tasks);
                    asyncTasks += tasks.size();
                    updateStabilityState(leavingRestState);
                }
                doExecute(tasks);
            } catch (Throwable t) {
                ServiceLogger.SERVICE.internalServiceError(t, primaryRegistration.getName());
            }
        }
    }

    private class ServiceUnavailableTask implements Runnable {

        private final Map<ServiceName, Dependent[]> dependents;
        private final Dependent[] children;

        ServiceUnavailableTask() {
            dependents = getDependentsByDependencyName();
            children = ServiceControllerImpl.this.children.toScatteredArray(NO_DEPENDENTS);
        }

        public void run() {
            try {
                for (Map.Entry<ServiceName, Dependent[]> dependentEntry: dependents.entrySet()) {
                    ServiceName serviceName = dependentEntry.getKey();
                    for (Dependent dependent: dependentEntry.getValue()) {
                        if (dependent != null) dependent.immediateDependencyUnavailable(serviceName);
                    }
                }
                final ServiceName primaryRegistrationName = primaryRegistration.getName();
                for (Dependent child: children) {
                    if (child != null) child.immediateDependencyUnavailable(primaryRegistrationName);
                }
                final ArrayList<Runnable> tasks = new ArrayList<Runnable>();
                synchronized (ServiceControllerImpl.this) {
                    final boolean leavingRestState = isStableRestState();
                    // Subtract one for this task
                    asyncTasks --;
                    transition(tasks);
                    asyncTasks += tasks.size();
                    updateStabilityState(leavingRestState);
                }
                doExecute(tasks);
            } catch (Throwable t) {
                ServiceLogger.SERVICE.internalServiceError(t, primaryRegistration.getName());
            }
        }
    }

    private class ServiceAvailableTask implements Runnable {

        private final Map<ServiceName, Dependent[]> dependents;
        private final Dependent[] children;

        ServiceAvailableTask() {
            dependents = getDependentsByDependencyName();
            children = ServiceControllerImpl.this.children.toScatteredArray(NO_DEPENDENTS);
        }

        public void run() {
            try {
                for (Map.Entry<ServiceName, Dependent[]> dependentEntry: dependents.entrySet()) {
                    ServiceName serviceName = dependentEntry.getKey();
                    for (Dependent dependent: dependentEntry.getValue()) {
                        if (dependent != null) dependent.immediateDependencyAvailable(serviceName);
                    }
                }
                final ServiceName primaryRegistrationName = primaryRegistration.getName();
                for (Dependent child: children) {
                    if (child != null) child.immediateDependencyAvailable(primaryRegistrationName);
                }
                final ArrayList<Runnable> tasks = new ArrayList<Runnable>();
                synchronized (ServiceControllerImpl.this) {
                    final boolean leavingRestState = isStableRestState();
                    // Subtract one for this task
                    asyncTasks --;
                    transition(tasks);
                    asyncTasks += tasks.size();
                    updateStabilityState(leavingRestState);
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
                startService(service, context);
                final ArrayList<Runnable> tasks = new ArrayList<Runnable>();
                synchronized (ServiceControllerImpl.this) {
                    final boolean leavingRestState = isStableRestState();
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
                    updateStabilityState(leavingRestState);
                }
                performOutInjections(serviceName);
                doExecute(tasks);
            } catch (StartException e) {
                e.setServiceName(serviceName);
                startFailed(e, serviceName, context, startNanos);
            } catch (Throwable t) {
                StartException e = new StartException("Failed to start service", t, serviceName);
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

        private void startService(Service<? extends S> service, StartContext context) throws StartException {
            final ClassLoader contextClassLoader = setTCCL(service.getClass().getClassLoader());
            try {
                service.start(context);
            } finally {
                setTCCL(contextClassLoader);
            }
        }

        private void startFailed(StartException e, ServiceName serviceName, StartContextImpl context, long startNanos) {
            ServiceLogger.FAIL.startFailed(e, serviceName);
            final ArrayList<Runnable> tasks;
            synchronized (ServiceControllerImpl.this) {
                final boolean leavingRestState = isStableRestState();
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
                updateStabilityState(leavingRestState);
            }
            doExecute(tasks);
        }
    }

    private class StopTask implements Runnable {
        private final boolean onlyUninject;
        private final ServiceControllerImpl<?>[] children;

        StopTask(final boolean onlyUninject) {
            this.onlyUninject = onlyUninject;
            if (!onlyUninject && !ServiceControllerImpl.this.children.isEmpty()) {
                synchronized (ServiceControllerImpl.this) {
                    final boolean leavingRestState = isStableRestState();
                    this.children = ServiceControllerImpl.this.children.toScatteredArray(NO_CONTROLLERS);
                    // placeholder async task for child removal; last removed child will decrement this count
                    // see removeChild method to verify when this count is decremented
                    ServiceControllerImpl.this.asyncTasks ++;
                    updateStabilityState(leavingRestState);
                }
            }
            else {
                this.children = null;
            }
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
                        if (children != null) {
                            for (ServiceController<?> child: children) {
                                if (child != null) child.setMode(Mode.REMOVE);
                            }
                        }
                        final Service<? extends S> service = serviceValue.getValue();
                        if (service != null) {
                            stopService(service, context);
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
                    final boolean leavingRestState = isStableRestState();
                    if (ServiceContainerImpl.PROFILE_OUTPUT != null) {
                        writeProfileInfo('X', startNanos, System.nanoTime());
                    }
                    // Subtract one for this task
                    asyncTasks --;
                    transition(tasks = new ArrayList<Runnable>());
                    asyncTasks += tasks.size();
                    updateStabilityState(leavingRestState);
                }
                doExecute(tasks);
            }
        }

        private void stopService(Service<? extends S> service, StopContext context) {
            final ClassLoader contextClassLoader = setTCCL(service.getClass().getClassLoader());
            try {
                service.stop(context);
            } finally {
                setTCCL(contextClassLoader);
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
        private final Transition transition;

        ListenerTask(final ServiceListener<? super S> listener, final Transition transition) {
            this.listener = listener;
            this.transition = transition;
            notification = ListenerNotification.TRANSITION;
        }

        ListenerTask(final ServiceListener<? super S> listener, final ListenerNotification notification) {
            this.listener = listener;
            transition = null;
            this.notification = notification;
        }

        public void run() {
            assert !holdsLock(ServiceControllerImpl.this);
            if (ServiceContainerImpl.PROFILE_OUTPUT != null) {
                final long start = System.nanoTime();
                try {
                    invokeListener(listener, notification, transition);
                } finally {
                    writeProfileInfo('L', start, System.nanoTime());
                }
            } else {
                invokeListener(listener, notification, transition);
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
                    final boolean leavingRestState = isStableRestState();
                    // Subtract one for this task
                    asyncTasks --;
                    transition(tasks);
                    asyncTasks += tasks.size();
                    updateStabilityState(leavingRestState);
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
                    final boolean leavingRestState = isStableRestState();
                    // Subtract one for this task
                    asyncTasks --;
                    transition(tasks);
                    asyncTasks += tasks.size();
                    updateStabilityState(leavingRestState);
                }
                doExecute(tasks);
            } catch (Throwable t) {
                ServiceLogger.SERVICE.internalServiceError(t, primaryRegistration.getName());
            }
        }
    }

    private class DependencyFailedTask implements Runnable {

        private final Dependent[][] dependents;
        private final ServiceControllerImpl<?>[] children;

        DependencyFailedTask(final Dependent[][] dependents, final boolean removeChildren) {
            this.dependents = dependents;
            if (removeChildren && !ServiceControllerImpl.this.children.isEmpty()) {
                synchronized (ServiceControllerImpl.this) {
                    final boolean leavingRestState = isStableRestState();
                    this.children = ServiceControllerImpl.this.children.toScatteredArray(NO_CONTROLLERS);
                    // placeholder async task for child removal; last removed child will decrement this count
                    // see removeChild method to verify when this count is decremented
                    ServiceControllerImpl.this.asyncTasks ++;
                    updateStabilityState(leavingRestState);
                }
            }
            else {
                this.children = null;
            }
        }

        public void run() {
            try {
                if (children != null) {
                    for (ServiceControllerImpl<?> child: children) {
                        if (child != null) child.setMode(Mode.REMOVE);
                    }
                }
                for (Dependent[] dependentArray : dependents) {
                    for (Dependent dependent : dependentArray) {
                        if (dependent != null) dependent.dependencyFailed();
                    }
                }
                final ArrayList<Runnable> tasks = new ArrayList<Runnable>();
                synchronized (ServiceControllerImpl.this) {
                    final boolean leavingRestState = isStableRestState();
                    // Subtract one for this task
                    asyncTasks --;
                    transition(tasks);
                    asyncTasks += tasks.size();
                    updateStabilityState(leavingRestState);
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
                    final boolean leavingRestState = isStableRestState();
                    // Subtract one for this task
                    asyncTasks --;
                    transition(tasks);
                    asyncTasks += tasks.size();
                    updateStabilityState(leavingRestState);
                }
                doExecute(tasks);
            } catch (Throwable t) {
                ServiceLogger.SERVICE.internalServiceError(t, primaryRegistration.getName());
            }
        }
    }

    private class RemoveTask implements Runnable {

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
                    final boolean leavingRestState = isStableRestState();
                    // Subtract one for this task
                    asyncTasks --;
                    transition(tasks);
                    asyncTasks += tasks.size();
                    updateStabilityState(leavingRestState);
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
                final boolean leavingRestState = isStableRestState();
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
                updateStabilityState(leavingRestState);
            }
            doExecute(tasks);
        }

        public ServiceTarget getChildTarget() {
            synchronized (ServiceControllerImpl.this) {
                if (state == ContextState.COMPLETE || state == ContextState.FAILED) {
                    throw new IllegalStateException("Lifecycle context is no longer valid");
                }
                if (childTarget == null) {
                    childTarget = new ChildServiceTarget(getServiceContainer());
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
                final boolean leavingRestState = isStableRestState();
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
                updateStabilityState(leavingRestState);
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
            final ClassLoader contextClassLoader = setTCCL(command.getClass().getClassLoader());
            try {
                command.run();
            } finally {
                setTCCL(contextClassLoader);
            }
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
                final boolean leavingRestState = isStableRestState();
                if (ServiceContainerImpl.PROFILE_OUTPUT != null) {
                    writeProfileInfo('X', startNanos, System.nanoTime());
                }
                // Subtract one for this task
                asyncTasks --;
                transition(tasks);
                asyncTasks += tasks.size();
                updateStabilityState(leavingRestState);
            }
            doExecute(tasks);
        }

        public ServiceController<?> getController() {
            return ServiceControllerImpl.this;
        }

        public void execute(final Runnable command) {
            final ClassLoader contextClassLoader = setTCCL(command.getClass().getClassLoader());
            try {
                command.run();
            } finally {
                setTCCL(contextClassLoader);
            }
        }

        public long getElapsedTime() {
            return System.nanoTime() - lifecycleTime;
        }
    }
}
