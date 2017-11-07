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

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
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

    private static final int DEPENDENCY_AVAILABLE_TASK = 1;
    private static final int DEPENDENCY_UNAVAILABLE_TASK = 1 << 1;
    private static final int DEPENDENCY_STARTED_TASK = 1 << 2;
    private static final int DEPENDENCY_STOPPED_TASK = 1 << 3;
    private static final int DEPENDENCY_FAILED_TASK = 1 << 4;
    private static final int DEPENDENCY_RETRYING_TASK = 1 << 5;

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
     * Container shutdown listener.
     */
    private ContainerShutdownListener shutdownListener;
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
     * The unavailable dependencies of this service.
     */
    private final IdentityHashSet<ServiceName> unavailableDependencies;
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
     * Tracks which dependent tasks have completed its execution.
     * First 16 bits track if dependent task have been scheduled.
     * Second 16 bits track whether scheduled dependent task finished its execution.
     */
    private int execFlags;
    /**
     * The number of registrations which place a demand-to-start on this
     * instance. If this value is >0, propagate a demand up to all parent
     * dependents. If this value is >0 and mode is ON_DEMAND, we should start.
     */
    private int demandedByCount;
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
     * Indicates whether dependencies have been demanded.
     */
    private boolean dependenciesDemanded = false;
    /**
     * Indicates whether async tasks count should be decremented on last child removal.
     */
    private boolean decrementOnLastChildRemoval;
    /**
     * The number of asynchronous tasks that are currently running. This
     * includes listeners, start/stop methods, outstanding asynchronous
     * start/stops, and internal tasks.
     */
    private int asyncTasks;
    /**
     * Tasks executed last on transition outside the lock.
     */
    private final List<Runnable> listenerTransitionTasks = new ArrayList<Runnable>();
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
        stoppingDependencies = parent == null ? depCount : depCount + 1;
        children = new IdentityHashSet<ServiceControllerImpl<?>>();
        unavailableDependencies = new IdentityHashSet<ServiceName>();
    }

    Substate getSubstateLocked() {
        return state;
    }

    /**
     * Set this instance into primary and alias registrations.
     * <p></p>
     * All notifications from registrations will be ignored until the
     * installation is {@link #commitInstallation(org.jboss.msc.service.ServiceController.Mode) committed}.
     */
    void startInstallation() {
        Lockable lock = primaryRegistration.getLock();
        synchronized (lock) {
            lock.acquireWrite();
            try {
                primaryRegistration.setInstance(this);
            } finally {
                lock.releaseWrite();
            }
        }
        for (ServiceRegistrationImpl aliasRegistration: aliasRegistrations) {
            lock = aliasRegistration.getLock();
            synchronized (lock) {
                lock.acquireWrite();
                try {
                    aliasRegistration.setInstance(this);
                } finally {
                    lock.releaseWrite();
                }
            }
        }
    }

    /**
     * Start this service configuration connecting it to its parent and dependencies.
     * <p></p>
     * All notifications from dependencies and parents will be ignored until the
     * installation is {@link #commitInstallation(org.jboss.msc.service.ServiceController.Mode) committed}.
     */
    void startConfiguration() {
        Lockable lock;
        for (Dependency dependency : dependencies) {
            lock = dependency.getLock();
            synchronized (lock) {
                lock.acquireWrite();
                try {
                    dependency.addDependent(this);
                } finally {
                    lock.releaseWrite();
                }
            }
        }
        if (parent != null) {
            lock = parent.primaryRegistration.getLock();
            synchronized (lock) {
                lock.acquireWrite();
                try {
                    parent.addChild(this);
                } finally {
                    lock.releaseWrite();
                }
            }
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
        final List<Runnable> listenerAddedTasks = new ArrayList<Runnable>();

        synchronized (this) {
            final boolean leavingRestState = isStableRestState();
            getListenerTasks(ListenerNotification.LISTENER_ADDED, listenerAddedTasks);
            internalSetMode(initialMode);
            // placeholder async task for running listener added tasks
            addAsyncTasks(listenerAddedTasks.size() + 1);
            updateStabilityState(leavingRestState);
        }
        for (Runnable listenerAddedTask : listenerAddedTasks) {
            listenerAddedTask.run();
        }
        final List<Runnable> tasks = new ArrayList<Runnable>();
        synchronized (this) {
            final boolean leavingRestState = isStableRestState();
            tasks.add(new DependencyAvailableTask());
            state = Substate.DOWN;
            // subtract one to compensate for +1 above
            decrementAsyncTasks();
            addAsyncTasks(tasks.size());
            updateStabilityState(leavingRestState);
        }
        doExecute(tasks);
    }

    /**
     * Roll back the service install.
     */
    void rollbackInstallation() {
        final Runnable removeTask;
        synchronized (this) {
            final boolean leavingRestState = isStableRestState();
            mode = Mode.REMOVE;
            state = Substate.CANCELLED;
            removeTask = new RemoveTask();
            incrementAsyncTasks();
            updateStabilityState(leavingRestState);
        }
        removeTask.run();
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
     * Controller notifications are ignored (we do not create new tasks on notification) if
     * either controller didn't finish its installation process or controller have been removed.
     *
     * @return true if notification must be ignored, false otherwise
     */
    private boolean ignoreNotification() {
        assert holdsLock(this);
        return state.compareTo(Substate.REMOVING) >= 0 || state.compareTo(Substate.CANCELLED) <= 0;
    }

    /**
     * Determine whether a stopped controller should start.
     *
     * @return {@code true} if so
     */
    private boolean shouldStart() {
        assert holdsLock(this);
        return mode == Mode.ACTIVE || mode == Mode.PASSIVE || demandedByCount > 0 && (mode == Mode.ON_DEMAND || mode == Mode.LAZY);
    }

    /**
     * Determine whether a running controller should stop.
     *
     * @return {@code true} if so
     */
    private boolean shouldStop() {
        assert holdsLock(this);
        return mode == Mode.REMOVE || demandedByCount == 0 && mode == Mode.ON_DEMAND || mode == Mode.NEVER;
    }

    /**
     * Returns true if controller is in rest state and no async tasks are running, false otherwise.
     * @return true if stable rest state, false otherwise
     */
    private boolean isStableRestState() {
        assert holdsLock(this);
        return asyncTasks == 0 && state.isRestState();
    }

    private void updateStabilityState(final boolean leavingStableRestState) {
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
                if (shutdownListener != null && state == Substate.REMOVED) {
                    shutdownListener.controllerDied();
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
                if (shouldStart() && runningDependents == 0 && stoppingDependencies == 0 && failCount == 0) {
                    return Transition.START_INITIATING_to_STARTING;
                } else {
                    // it is possible runningDependents > 0 if this service is optional dependency to some other service
                    return Transition.START_INITIATING_to_START_REQUESTED;
                }
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
                    if (!unavailableDependencies.isEmpty() || failCount > 0) {
                        return Transition.START_REQUESTED_to_PROBLEM;
                    }
                    if (stoppingDependencies == 0 && runningDependents == 0) {
                        // it is possible runningDependents > 0 if this service is optional dependency to some other service
                        return Transition.START_REQUESTED_to_START_INITIATING;
                    }
                } else {
                    return Transition.START_REQUESTED_to_DOWN;
                }
                break;
            }
            case PROBLEM: {
                if (! shouldStart() || (unavailableDependencies.isEmpty() && failCount == 0) || mode == Mode.PASSIVE) {
                    return Transition.PROBLEM_to_START_REQUESTED;
                }
                break;
            }
            case REMOVING: {
                return Transition.REMOVING_to_REMOVED;
            }
            case CANCELLED: {
                return Transition.CANCELLED_to_REMOVED;
            }
            case REMOVED:
            {
                // no possible actions
                break;
            }
        }
        return null;
    }

    private boolean postTransitionTasks(final List<Runnable> tasks) {
        assert holdsLock(this);
        // Listener transition tasks are executed last for ongoing transition and outside of intrinsic lock
        if (listenerTransitionTasks.size() > 0) {
            tasks.addAll(listenerTransitionTasks);
            listenerTransitionTasks.clear();
            return true;
        }
        return false;
    }

    /**
     * Run the locked portion of a transition. Call under the lock.
     *
     * @return returns list of async tasks to execute
     */
    private List<Runnable> transition() {
        assert holdsLock(this);
        if (asyncTasks != 0 || state == Substate.NEW) {
            // no movement possible
            return Collections.EMPTY_LIST;
        }
        final List<Runnable> tasks = new ArrayList<Runnable>();
        if (postTransitionTasks(tasks)) {
            // no movement possible
            return tasks;
        }
        // clean up tasks execution flags
        execFlags = 0;

        Transition transition;
        do {
            // first of all, check if dependencies & parent should be un/demanded
            switch (mode) {
                case NEVER:
                case REMOVE:
                    if (dependenciesDemanded) {
                        tasks.add(new UndemandDependenciesTask());
                        dependenciesDemanded = false;
                    }
                    break;
                case LAZY: {
                    if (state == Substate.UP) {
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
                return tasks;
            }
            getListenerTasks(transition, listenerTransitionTasks);
            switch (transition) {
                case DOWN_to_WAITING: {
                    tasks.add(new DependencyUnavailableTask());
                    break;
                }
                case WAITING_to_DOWN: {
                    tasks.add(new DependencyAvailableTask());
                    break;
                }
                case DOWN_to_WONT_START: {
                    tasks.add(new DependencyUnavailableTask());
                    break;
                }
                case WONT_START_to_DOWN: {
                    tasks.add(new DependencyAvailableTask());
                    break;
                }
                case STOPPING_to_DOWN: {
                    tasks.add(new DependentStoppedTask());
                    break;
                }
                case START_REQUESTED_to_DOWN: {
                    break;
                }
                case START_REQUESTED_to_START_INITIATING: {
                    lifecycleTime = System.nanoTime();
                    tasks.add(new DependentStartedTask());
                    break;
                }
                case START_REQUESTED_to_PROBLEM: {
                    tasks.add(new DependencyUnavailableTask());
                    getPrimaryRegistration().getContainer().addProblem(this);
                    for (StabilityMonitor monitor : monitors) {
                        monitor.addProblem(this);
                    }
                    break;
                }
                case UP_to_STOP_REQUESTED: {
                    lifecycleTime = System.nanoTime();
                    if (mode == Mode.LAZY && demandedByCount == 0) {
                        assert dependenciesDemanded;
                        tasks.add(new UndemandDependenciesTask());
                        dependenciesDemanded = false;
                    }
                    tasks.add(new DependencyStoppedTask());
                    break;
                }
                case STARTING_to_UP: {
                    tasks.add(new DependencyStartedTask());
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
                    tasks.add(new DependencyFailedTask());
                    tasks.add(new RemoveChildrenTask());
                    break;
                }
                case START_FAILED_to_STARTING: {
                    getPrimaryRegistration().getContainer().removeFailed(this);
                    for (StabilityMonitor monitor : monitors) {
                        monitor.removeFailed(this);
                    }
                    tasks.add(new DependencyRetryingTask());
                    break;
                }
                case START_INITIATING_to_STARTING: {
                    tasks.add(new StartTask());
                    break;
                }
                case START_INITIATING_to_START_REQUESTED: {
                    tasks.add(new DependentStoppedTask());
                    break;
                }
                case START_FAILED_to_DOWN: {
                    getPrimaryRegistration().getContainer().removeFailed(this);
                    for (StabilityMonitor monitor : monitors) {
                        monitor.removeFailed(this);
                    }
                    startException = null;
                    tasks.add(new DependencyRetryingTask());
                    tasks.add(new StopTask(true));
                    tasks.add(new DependentStoppedTask());
                    break;
                }
                case STOP_REQUESTED_to_UP: {
                    tasks.add(new DependencyStartedTask());
                    break;
                }
                case STOP_REQUESTED_to_STOPPING: {
                    ChildServiceTarget childTarget = this.childTarget;
                    if (childTarget != null) {
                        childTarget.valid = false;
                        this.childTarget = null;
                    }
                    tasks.add(new StopTask(false));
                    tasks.add(new RemoveChildrenTask());
                    break;
                }
                case DOWN_to_REMOVING: {
                    tasks.add(new DependencyUnavailableTask());
                    break;
                }
                case CANCELLED_to_REMOVED:
                    listeners.clear();
                    for (StabilityMonitor monitor : monitors) {
                        monitor.removeControllerNoCallback(this);
                    }
                    break;
                case REMOVING_to_REMOVED: {
                    tasks.add(new RemoveTask());
                    listeners.clear();
                    for (StabilityMonitor monitor : monitors) {
                        monitor.removeControllerNoCallback(this);
                    }
                    break;
                }
                case DOWN_to_START_REQUESTED: {
                    break;
                }
                case PROBLEM_to_START_REQUESTED: {
                    tasks.add(new DependencyAvailableTask());
                    getPrimaryRegistration().getContainer().removeProblem(this);
                    for (StabilityMonitor monitor : monitors) {
                        monitor.removeProblem(this);
                    }
                    break;
                }
                default: {
                    throw new IllegalStateException();
                }
            }
            state = transition.getAfter();
        } while (tasks.isEmpty() && listenerTransitionTasks.isEmpty());
        // Notify waiters that a transition occurred
        notifyAll();
        if (tasks.size() > 0) {
            // Postponing listener transition tasks
        } else {
            postTransitionTasks(tasks);
        }
        return tasks;
    }

    private void getListenerTasks(final Transition transition, final List<Runnable> tasks) {
        for (ServiceListener<? super S> listener : listeners) {
            tasks.add(new ListenerTask(listener, transition));
        }
    }

    private void getListenerTasks(final ListenerNotification notification, final List<Runnable> tasks) {
        for (ServiceListener<? super S> listener : listeners) {
            tasks.add(new ListenerTask(listener, notification));
        }
    }

    void doExecute(final List<Runnable> tasks) {
        assert !holdsLock(this);
        if (tasks.isEmpty()) return;
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
        final List<Runnable> tasks;
        synchronized (this) {
            final boolean leavingRestState = isStableRestState();
            final Mode oldMode = mode;
            if (expectedMode != null && expectedMode != oldMode) {
                return false;
            }
            if (oldMode == newMode) {
                return true;
            }
            internalSetMode(newMode);
            tasks = transition();
            addAsyncTasks(tasks.size());
            updateStabilityState(leavingRestState);
        }
        doExecute(tasks);
        return true;
    }

    private void internalSetMode(final Mode newMode) {
        assert holdsLock(this);
        final ServiceController.Mode oldMode = mode;
        if (oldMode == Mode.REMOVE) {
            if (state.compareTo(Substate.REMOVING) >= 0) {
                throw new IllegalStateException("Service already removed");
            }
        }
        mode = newMode;
    }

    @Override
    public void dependencyAvailable(final ServiceName dependencyName) {
        final List<Runnable> tasks;
        synchronized (this) {
            final boolean leavingRestState = isStableRestState();
            assert unavailableDependencies.contains(dependencyName);
            unavailableDependencies.remove(dependencyName);
            if (ignoreNotification() || !unavailableDependencies.isEmpty()) return;
            // we dropped it to 0
            tasks = transition();
            addAsyncTasks(tasks.size());
            updateStabilityState(leavingRestState);
        }
        doExecute(tasks);
    }

    @Override
    public void dependencyUnavailable(final ServiceName dependencyName) {
        final List<Runnable> tasks;
        synchronized (this) {
            final boolean leavingRestState = isStableRestState();
            assert !unavailableDependencies.contains(dependencyName);
            unavailableDependencies.add(dependencyName);
            if (ignoreNotification() || unavailableDependencies.size() != 1) return;
            // we raised it to 1
            tasks = transition();
            addAsyncTasks(tasks.size());
            updateStabilityState(leavingRestState);
        }
        doExecute(tasks);
    }

    /** {@inheritDoc} */
    public ServiceControllerImpl<?> getDependentController() {
        return this;
    }

    @Override
    public void dependencyUp() {
        final List<Runnable> tasks;
        synchronized (this) {
            final boolean leavingRestState = isStableRestState();
            assert stoppingDependencies > 0;
            --stoppingDependencies;
            if (ignoreNotification() || stoppingDependencies != 0) return;
            // we dropped it to 0
            tasks = transition();
            addAsyncTasks(tasks.size());
            updateStabilityState(leavingRestState);
        }
        doExecute(tasks);
    }

    @Override
    public void dependencyDown() {
        final List<Runnable> tasks;
        synchronized (this) {
            final boolean leavingRestState = isStableRestState();
            ++stoppingDependencies;
            if (ignoreNotification() || stoppingDependencies != 1) return;
            // we raised it to 1
            tasks = transition();
            addAsyncTasks(tasks.size());
            updateStabilityState(leavingRestState);
        }
        doExecute(tasks);
    }

    @Override
    public void dependencyFailed() {
        final List<Runnable> tasks;
        synchronized (this) {
            final boolean leavingRestState = isStableRestState();
            ++failCount;
            if (ignoreNotification() || failCount != 1) return;
            // we raised it to 1
            tasks = transition();
            addAsyncTasks(tasks.size());
            updateStabilityState(leavingRestState);
        }
        doExecute(tasks);
    }

    @Override
    public void dependencySucceeded() {
        final List<Runnable> tasks;
        synchronized (this) {
            final boolean leavingRestState = isStableRestState();
            assert failCount > 0;
            --failCount;
            if (ignoreNotification() || failCount != 0) return;
            // we dropped it to 0
            tasks = transition();
            addAsyncTasks(tasks.size());
            updateStabilityState(leavingRestState);
        }
        doExecute(tasks);
    }

    void dependentStarted() {
        dependentsStarted(1);
    }

    void dependentsStarted(final int count) {
        assert !holdsLock(this);
        synchronized (this) {
            runningDependents += count;
        }
    }

    void dependentStopped() {
        assert !holdsLock(this);
        final List<Runnable> tasks;
        synchronized (this) {
            final boolean leavingRestState = isStableRestState();
            assert runningDependents > 0;
            --runningDependents;
            if (ignoreNotification() || runningDependents != 0) return;
            // we dropped it to 0
            tasks = transition();
            addAsyncTasks(tasks.size());
            updateStabilityState(leavingRestState);
        }
        doExecute(tasks);
    }

    void newDependent(final ServiceName dependencyName, final Dependent dependent) {
        assert holdsLock(this);
        if (state == Substate.START_FAILED && finishedTask(DEPENDENCY_FAILED_TASK)) {
            dependent.dependencyFailed();
        } else if ((state == Substate.STARTING || state == Substate.DOWN) && unfinishedTask(DEPENDENCY_RETRYING_TASK)) {
            dependent.dependencyFailed();
        }

        if ((state == Substate.WAITING || state == Substate.WONT_START || state == Substate.REMOVING || state == Substate.PROBLEM) && finishedTask(DEPENDENCY_UNAVAILABLE_TASK)) {
            dependent.dependencyUnavailable(dependencyName);
        } else if ((state == Substate.DOWN || state == Substate.START_REQUESTED) && unfinishedTask(DEPENDENCY_AVAILABLE_TASK)) {
            dependent.dependencyUnavailable(dependencyName);
        } else if (state == Substate.NEW || state == Substate.CANCELLED || state == Substate.REMOVED) {
            dependent.dependencyUnavailable(dependencyName);
        } else if (state == Substate.UP && finishedTask(DEPENDENCY_STARTED_TASK)) {
            dependent.dependencyUp();
        } else if (state == Substate.STOP_REQUESTED && unfinishedTask(DEPENDENCY_STOPPED_TASK)) {
            dependent.dependencyUp();
        }
    }

    private boolean unfinishedTask(final int taskFlag) {
        assert holdsLock(this);
        final boolean taskScheduled = (execFlags & (taskFlag << 16)) != 0;
        final boolean taskRunning = (execFlags & taskFlag) == 0;
        return taskScheduled && taskRunning;
    }

    private boolean finishedTask(final int taskFlag) {
        assert holdsLock(this);
        final boolean taskUnscheduled = (execFlags & (taskFlag << 16)) == 0;
        final boolean taskFinished = (execFlags & taskFlag) != 0;
        return taskUnscheduled || taskFinished;
    }

    void addDemand() {
        addDemands(1);
    }

    void addDemands(final int demandedByCount) {
        assert !holdsLock(this);
        final boolean propagate;
        final List<Runnable> tasks;
        synchronized (this) {
            final boolean leavingRestState = isStableRestState();
            final int cnt = this.demandedByCount;
            this.demandedByCount += demandedByCount;
            if (ignoreNotification()) return;
            boolean notStartedLazy = mode == Mode.LAZY && !(state.getState() == State.UP && state != Substate.STOP_REQUESTED);
            propagate = cnt == 0 && (mode == Mode.ON_DEMAND || notStartedLazy || mode == Mode.PASSIVE);
            if (!propagate) return;
            tasks = transition();
            addAsyncTasks(tasks.size());
            updateStabilityState(leavingRestState);
        }
        doExecute(tasks);
    }

    void removeDemand() {
        assert !holdsLock(this);
        final boolean propagate;
        final List<Runnable> tasks;
        synchronized (this) {
            final boolean leavingRestState = isStableRestState();
            assert demandedByCount > 0;
            final int cnt = --demandedByCount;
            if (ignoreNotification()) return;
            boolean notStartedLazy = mode == Mode.LAZY && !(state.getState() == State.UP && state != Substate.STOP_REQUESTED);
            propagate = cnt == 0 && (mode == Mode.ON_DEMAND || notStartedLazy || mode == Mode.PASSIVE);
            if (!propagate) return;
            tasks = transition();
            addAsyncTasks(tasks.size());
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
        final List<Runnable> tasks;
        synchronized (this) {
            final boolean leavingRestState = isStableRestState();
            children.remove(child);
            if (children.isEmpty() && decrementOnLastChildRemoval) {
                switch (state) {
                    case START_FAILED:
                    case STOPPING:
                        // last child was removed; drop async count
                        decrementAsyncTasks();
                        tasks = transition();
                        break;
                    default:
                        return;
                }
            } else {
                return;
            }
            addAsyncTasks(tasks.size());
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

    void addListener(final ContainerShutdownListener listener) {
        assert !holdsLock(this);
        synchronized (this) {
            if (state == Substate.REMOVED && asyncTasks == 0) {
                return; // controller is dead
            }
            if (shutdownListener != null) {
                return; // register listener only once
            }
            shutdownListener = listener;
            shutdownListener.controllerAlive();
        }
    }

    public void addListener(final ServiceListener<? super S> listener) {
        assert !holdsLock(this);
        ListenerTask listenerAddedTask, listenerRemovedTask = null;
        synchronized (this) {
            final boolean leavingRestState = isStableRestState();
            if (listeners.contains(listener)) {
                // Duplicates not allowed
                throw new IllegalArgumentException("Listener " + listener + " already present on controller for " + primaryRegistration.getName());
            }
            listeners.add(listener);
            listenerAddedTask = new ListenerTask(listener, ListenerNotification.LISTENER_ADDED);
            incrementAsyncTasks();
            if (state == Substate.REMOVED) {
                listenerRemovedTask = new ListenerTask(listener, Transition.REMOVING_to_REMOVED);
                incrementAsyncTasks();
            }
            updateStabilityState(leavingRestState);
        }
        try { listenerAddedTask.run(); } finally { if (listenerRemovedTask != null) listenerRemovedTask.run(); }
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
        final List<Runnable> tasks;
        synchronized (this) {
            final boolean leavingRestState = isStableRestState();
            if (failCount > 0 || state.getState() != ServiceController.State.START_FAILED) return;
            startException = null;
            tasks = transition();
            addAsyncTasks(tasks.size());
            updateStabilityState(leavingRestState);
        }
        doExecute(tasks);
    }

    @Override
    public synchronized Set<ServiceName> getImmediateUnavailableDependencies() {
        return unavailableDependencies.clone();
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
                    !unavailableDependencies.isEmpty()
            );
        }
    }

    String dumpServiceDetails() {
        final StringBuilder b = new StringBuilder();
        IdentityHashSet<Dependent> dependents;
        synchronized (primaryRegistration) {
            dependents = primaryRegistration.getDependents().clone();
        }
        b.append("Service Name: ").append(primaryRegistration.getName().toString()).append(" - Dependents: ").append(dependents.size()).append('\n');
        for (Dependent dependent : dependents) {
            final ServiceControllerImpl<?> controller = dependent.getDependentController();
            synchronized (controller) {
                b.append("        ").append(controller.getName().toString()).append(" - State: ").append(controller.state.getState()).append(" (Substate: ").append(controller.state).append(")\n");
            }
        }
        b.append("Service Aliases: ").append(aliasRegistrations.length).append('\n');
        for (ServiceRegistrationImpl registration : aliasRegistrations) {
            synchronized (registration) {
                dependents = registration.getDependents().clone();
            }
            b.append("    ").append(registration.getName().toString()).append(" - Dependents: ").append(dependents.size()).append('\n');
            for (Dependent dependent : dependents) {
                final ServiceControllerImpl<?> controller = dependent.getDependentController();
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
            b.append("Stopping Dependencies: ").append(stoppingDependencies).append('\n');
            b.append("Running Dependents: ").append(runningDependents).append('\n');
            b.append("Fail Count: ").append(failCount).append('\n');
            b.append("Unavailable Dep Count: ").append(unavailableDependencies.size()).append('\n');
            for (ServiceName name : unavailableDependencies) {
                b.append("    ").append(name.toString()).append('\n');
            }
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

    private void performInjections() {
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

    private void performOutInjections() {
        final int injectionsLength = outInjections.length;
        for (int i = 0; i < injectionsLength; i++) {
            final ValueInjection<?> injection = outInjections[i];
            try {
                doInject(injection);
            } catch (Throwable t) {
                ServiceLogger.SERVICE.exceptionAfterComplete(t, primaryRegistration.getName());
            }
        }
    }

    enum ContextState {
        // mid transition states
        SYNC_ASYNC_COMPLETE,
        SYNC_ASYNC_FAILED,
        // final transition states
        SYNC,
        ASYNC,
        COMPLETE,
        FAILED,
    }

    private static <T> void doInject(final ValueInjection<T> injection) {
        injection.getTarget().inject(injection.getSource().getValue());
    }

    private static ClassLoader setTCCL(final ClassLoader newTCCL) {
        final SecurityManager sm = System.getSecurityManager();
        final SetTCCLAction setTCCLAction = new SetTCCLAction(newTCCL);
        if (sm != null) {
            return AccessController.doPrivileged(setTCCLAction);
        } else {
            return setTCCLAction.run();
        }
    }

    private static ClassLoader getCL(final Class<?> clazz) {
        final SecurityManager sm = System.getSecurityManager();
        final GetCLAction getCLAction = new GetCLAction(clazz);
        if (sm != null) {
            return AccessController.doPrivileged(getCLAction);
        } else {
            return getCLAction.run();
        }
    }

    @Override
    public String toString() {
        return String.format("Controller for %s@%x", getName(), Integer.valueOf(hashCode()));
    }

    private abstract class ControllerTask implements Runnable {
        private ControllerTask() {
            assert holdsLock(ServiceControllerImpl.this);
        }

        public final void run() {
            assert !holdsLock(ServiceControllerImpl.this);
            try {
                beforeExecute();
                if (!execute()) return;
                final List<Runnable> tasks;
                synchronized (ServiceControllerImpl.this) {
                    final boolean leavingRestState = isStableRestState();
                    // Subtract one for this task
                    decrementAsyncTasks();
                    tasks = transition();
                    addAsyncTasks(tasks.size());
                    updateStabilityState(leavingRestState);
                }
                doExecute(tasks);
            } catch (Throwable t) {
                ServiceLogger.SERVICE.internalServiceError(t, primaryRegistration.getName());
            } finally {
                afterExecute();
            }
        }

        void afterExecute() {}
        void beforeExecute() {}
        abstract boolean execute();
    }

    private abstract class DependenciesControllerTask extends ControllerTask {
        final boolean execute() {
            Lockable lock;
            for (Dependency dependency : dependencies) {
                lock = dependency.getLock();
                synchronized (lock) {
                    lock.acquireWrite();
                    try {
                        inform(dependency);
                    } finally {
                        lock.releaseWrite();
                    }
                }
            }
            if (parent != null) inform(parent);
            return true;
        }

        abstract void inform(Dependency dependency);
        abstract void inform(ServiceControllerImpl parent);
    }

    private abstract class DependentsControllerTask extends ControllerTask {
        private final int execFlag;

        private DependentsControllerTask(final int execFlag) {
            this.execFlag = execFlag;
            execFlags |= (execFlag << 16);
        }

        final boolean execute() {
            for (Dependent dependent : primaryRegistration.getDependents()) {
                inform(dependent, primaryRegistration.getName());
            }
            for (ServiceRegistrationImpl aliasRegistration : aliasRegistrations) {
                for (Dependent dependent : aliasRegistration.getDependents()) {
                    inform(dependent, aliasRegistration.getName());
                }
            }
            synchronized (ServiceControllerImpl.this) {
                for (Dependent child : children) {
                    inform(child, primaryRegistration.getName());
                }
                execFlags |= execFlag;
            }
            return true;
        }

        void inform(final Dependent dependent, final ServiceName serviceName) { inform(dependent); }
        void inform(final Dependent dependent) {}

        void beforeExecute() {
            Lockable lock = primaryRegistration.getLock();
            synchronized (lock) { lock.acquireRead(); }
            for (ServiceRegistrationImpl aliasRegistration : aliasRegistrations) {
                lock = aliasRegistration.getLock();
                synchronized (lock) { lock.acquireRead(); }
            }
        }

        void afterExecute() {
            Lockable lock = primaryRegistration.getLock();
            synchronized (lock) { lock.releaseRead(); }
            for (ServiceRegistrationImpl aliasRegistration : aliasRegistrations) {
                lock = aliasRegistration.getLock();
                synchronized (lock) { lock.releaseRead(); }
            }
        }
    }

    private final class DemandDependenciesTask extends DependenciesControllerTask {
        void inform(final Dependency dependency) { dependency.addDemand(); }
        void inform(final ServiceControllerImpl parent) { parent.addDemand(); }
    }

    private final class UndemandDependenciesTask extends DependenciesControllerTask {
        void inform(final Dependency dependency) { dependency.removeDemand(); }
        void inform(final ServiceControllerImpl parent) { parent.removeDemand(); }
    }

    private final class DependentStartedTask extends DependenciesControllerTask {
        void inform(final Dependency dependency) { dependency.dependentStarted(); }
        void inform(final ServiceControllerImpl parent) { parent.dependentStarted(); }
    }

    private final class DependentStoppedTask extends DependenciesControllerTask {
        void inform(final Dependency dependency) { dependency.dependentStopped(); }
        void inform(final ServiceControllerImpl parent) { parent.dependentStopped(); }
    }

    private final class DependencyAvailableTask extends DependentsControllerTask {
        DependencyAvailableTask() { super(DEPENDENCY_AVAILABLE_TASK); }
        void inform(final Dependent dependent, final ServiceName name) { dependent.dependencyAvailable(name); }
    }

    private final class DependencyUnavailableTask extends DependentsControllerTask {
        DependencyUnavailableTask() { super(DEPENDENCY_UNAVAILABLE_TASK); }
        void inform(final Dependent dependent, final ServiceName name) { dependent.dependencyUnavailable(name); }
    }

    private final class DependencyStartedTask extends DependentsControllerTask {
        private DependencyStartedTask() { super(DEPENDENCY_STARTED_TASK); }
        void inform(final Dependent dependent) { dependent.dependencyUp(); }
    }

    private final class DependencyStoppedTask extends DependentsControllerTask {
        private DependencyStoppedTask() { super(DEPENDENCY_STOPPED_TASK); }
        void inform(final Dependent dependent) { dependent.dependencyDown(); }
    }

    private final class DependencyFailedTask extends DependentsControllerTask {
        private DependencyFailedTask() { super(DEPENDENCY_FAILED_TASK); }
        void inform(final Dependent dependent) { dependent.dependencyFailed(); }
    }

    private final class DependencyRetryingTask extends DependentsControllerTask {
        private DependencyRetryingTask() { super(DEPENDENCY_RETRYING_TASK); }
        void inform(final Dependent dependent) { dependent.dependencySucceeded(); }
    }

    private final class StartTask extends ControllerTask {
        boolean execute() {
            final ServiceName serviceName = primaryRegistration.getName();
            final StartContextImpl context = new StartContextImpl();
            try {
                performInjections();
                final Service<? extends S> service = serviceValue.getValue();
                if (service == null) {
                    throw new IllegalArgumentException("Service is null");
                }
                startService(service, context);
                synchronized (context.lock) {
                    if (context.state != ContextState.SYNC) {
                        return false;
                    }
                    context.state = ContextState.COMPLETE;
                }
                performOutInjections();
                return true;
            } catch (StartException e) {
                e.setServiceName(serviceName);
                return startFailed(e, serviceName, context);
            } catch (Throwable t) {
                StartException e = new StartException("Failed to start service", t, serviceName);
                return startFailed(e, serviceName, context);
            }
        }

        private void startService(Service<? extends S> service, StartContext context) throws StartException {
            final ClassLoader contextClassLoader = setTCCL(getCL(service.getClass()));
            try {
                service.start(context);
            } finally {
                setTCCL(contextClassLoader);
            }
        }

        private boolean startFailed(StartException e, ServiceName serviceName, StartContextImpl context) {
            ServiceLogger.FAIL.startFailed(e, serviceName);
            synchronized (context.lock) {
                final ContextState oldState = context.state;
                if (oldState != ContextState.SYNC && oldState != ContextState.ASYNC) {
                    ServiceLogger.FAIL.exceptionAfterComplete(e, serviceName);
                    return false;
                }
                context.state = ContextState.FAILED;
            }
            synchronized (ServiceControllerImpl.this) {
                startException = e;
            }
            return true;
        }
    }

    private final class StopTask extends ControllerTask {
        private final boolean onlyUninject;

        StopTask(final boolean onlyUninject) {
            this.onlyUninject = onlyUninject;
        }

        boolean execute() {
            final ServiceName serviceName = primaryRegistration.getName();
            final StopContextImpl context = new StopContextImpl();
            boolean ok = false;
            try {
                if (! onlyUninject) {
                    try {
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
                synchronized (context.lock) {
                    if (ok && context.state != ContextState.SYNC) {
                        // We want to discard the exception anyway, if there was one.  Which there can't be.
                        //noinspection ReturnInsideFinallyBlock
                        return false;
                    }
                    context.state = ContextState.COMPLETE;
                }
                uninject(serviceName, injections);
                uninject(serviceName, outInjections);
                return true;
            }
        }

        private void stopService(Service<? extends S> service, StopContext context) {
            final ClassLoader contextClassLoader = setTCCL(getCL(service.getClass()));
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

    private final class ListenerTask extends ControllerTask {
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

        boolean execute() {
            invokeListener(listener, notification, transition);
            return true;
        }

        private void invokeListener(final ServiceListener<? super S> listener, final ListenerNotification notification, final Transition transition) {
            // first set the TCCL
            final ClassLoader contextClassLoader = setTCCL(getCL(listener.getClass()));
            try {
                switch (notification) {
                    case TRANSITION: {
                        listener.transition(ServiceControllerImpl.this, transition);
                        break;
                    }
                    case LISTENER_ADDED: {
                        listener.listenerAdded(ServiceControllerImpl.this);
                        break;
                    }
                    default: throw new IllegalStateException();
                }
            } catch (Throwable t) {
                ServiceLogger.SERVICE.listenerFailed(t, listener);
            } finally {
                // reset TCCL
                setTCCL(contextClassLoader);
            }
        }
    }

    private final class RemoveChildrenTask extends ControllerTask {
        RemoveChildrenTask() {
            decrementOnLastChildRemoval = false;
        }

        boolean execute() {
            synchronized (ServiceControllerImpl.this) {
                if (!children.isEmpty()) {
                    final boolean leavingRestState = isStableRestState();
                    // placeholder async task for child removal; last removed child will decrement this count
                    // see removeChild method to verify when this count is decremented
                    incrementAsyncTasks();
                    decrementOnLastChildRemoval = true;
                    for (ServiceControllerImpl<?> child : children) {
                        child.setMode(Mode.REMOVE);
                    }
                    updateStabilityState(leavingRestState);
                }
            }
            return true;
        }
    }

    private final class RemoveTask extends ControllerTask {
        boolean execute() {
            assert getMode() == ServiceController.Mode.REMOVE;
            assert getSubstate() == Substate.REMOVED || getSubstate() == Substate.CANCELLED;
            Lockable lock = primaryRegistration.getLock();
            synchronized (lock) {
                lock.acquireWrite();
                try {
                    primaryRegistration.clearInstance(ServiceControllerImpl.this);
                } finally {
                    lock.releaseWrite();
                }
            }
            for (ServiceRegistrationImpl aliasRegistration : aliasRegistrations) {
                lock = aliasRegistration.getLock();
                synchronized (lock) {
                    lock.acquireWrite();
                    try {
                        aliasRegistration.clearInstance(ServiceControllerImpl.this);
                    } finally {
                        lock.releaseWrite();
                    }
                }
            }
            for (Dependency dependency : dependencies) {
                lock = dependency.getLock();
                synchronized (lock) {
                    lock.acquireWrite();
                    try {
                        dependency.removeDependent(ServiceControllerImpl.this);
                    } finally {
                        lock.releaseWrite();
                    }
                }
            }
            if (parent != null) {
                lock = parent.primaryRegistration.getLock();
                synchronized (lock) {
                    lock.acquireWrite();
                    try {
                        parent.removeChild(ServiceControllerImpl.this);
                    } finally {
                        lock.releaseWrite();
                    }
                }
            }
            return true;
        }
    }

    private final class StartContextImpl implements StartContext {

        private ContextState state = ContextState.SYNC;
        private final Object lock = new Object();

        public void failed(StartException reason) throws IllegalStateException {
            synchronized (lock) {
                if (state == ContextState.COMPLETE || state == ContextState.FAILED
                        || state == ContextState.SYNC_ASYNC_COMPLETE || state == ContextState.SYNC_ASYNC_FAILED) {
                    throw new IllegalStateException(ILLEGAL_CONTROLLER_STATE);
                }
                if (state == ContextState.ASYNC) {
                    state = ContextState.FAILED;
                }
                if (state == ContextState.SYNC) {
                    state = ContextState.SYNC_ASYNC_FAILED;
                }
            }
            if (reason == null) {
                reason = new StartException("Start failed, and additionally, a null cause was supplied");
            }
            final ServiceName serviceName = getName();
            reason.setServiceName(serviceName);
            ServiceLogger.FAIL.startFailed(reason, serviceName);
            final List<Runnable> tasks;
            synchronized (ServiceControllerImpl.this) {
                final boolean leavingRestState = isStableRestState();
                startException = reason;
                // Subtract one for this task
                decrementAsyncTasks();
                tasks = transition();
                addAsyncTasks(tasks.size());
                updateStabilityState(leavingRestState);
            }
            doExecute(tasks);
        }

        public ServiceTarget getChildTarget() {
            synchronized (lock) {
                if (state == ContextState.COMPLETE || state == ContextState.FAILED) {
                    throw new IllegalStateException("Lifecycle context is no longer valid");
                }
                synchronized (ServiceControllerImpl.this) {
                    if (childTarget == null) {
                        childTarget = new ChildServiceTarget(getServiceContainer());
                    }
                    return childTarget;
                }
            }
        }

        public void asynchronous() throws IllegalStateException {
            synchronized (lock) {
                if (state == ContextState.SYNC) {
                    state = ContextState.ASYNC;
                } else if (state == ContextState.SYNC_ASYNC_COMPLETE) {
                    state = ContextState.COMPLETE;
                } else if (state == ContextState.SYNC_ASYNC_FAILED) {
                    state = ContextState.FAILED;
                } else if (state == ContextState.ASYNC) {
                    throw new IllegalStateException(ILLEGAL_CONTROLLER_STATE);
                }
            }
        }

        public void complete() throws IllegalStateException {
            synchronized (lock) {
                if (state == ContextState.COMPLETE || state == ContextState.FAILED
                        || state == ContextState.SYNC_ASYNC_COMPLETE || state == ContextState.SYNC_ASYNC_FAILED) {
                    throw new IllegalStateException(ILLEGAL_CONTROLLER_STATE);
                }
                if (state == ContextState.ASYNC) {
                    state = ContextState.COMPLETE;
                }
                if (state == ContextState.SYNC) {
                    state = ContextState.SYNC_ASYNC_COMPLETE;
                }
            }
            performOutInjections();
            final List<Runnable> tasks;
            synchronized (ServiceControllerImpl.this) {
                final boolean leavingRestState = isStableRestState();
                // Subtract one for this task
                decrementAsyncTasks();
                tasks = transition();
                addAsyncTasks(tasks.size());
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
            final ClassLoader contextClassLoader = setTCCL(getCL(command.getClass()));
            try {
                command.run();
            } finally {
                setTCCL(contextClassLoader);
            }
        }
    }

    private final class StopContextImpl implements StopContext {

        private ContextState state = ContextState.SYNC;
        private final Object lock = new Object();

        public void asynchronous() throws IllegalStateException {
            synchronized (lock) {
                if (state == ContextState.SYNC) {
                    state = ContextState.ASYNC;
                } else if (state == ContextState.SYNC_ASYNC_COMPLETE) {
                    state = ContextState.COMPLETE;
                } else if (state == ContextState.ASYNC) {
                    throw new IllegalStateException(ILLEGAL_CONTROLLER_STATE);
                }
            }
        }

        public void complete() throws IllegalStateException {
            synchronized (lock) {
                if (state == ContextState.COMPLETE || state == ContextState.SYNC_ASYNC_COMPLETE) {
                    throw new IllegalStateException(ILLEGAL_CONTROLLER_STATE);
                }
                if (state == ContextState.ASYNC) {
                    state = ContextState.COMPLETE;
                }
                if (state == ContextState.SYNC) {
                    state = ContextState.SYNC_ASYNC_COMPLETE;
                }
            }
            for (ValueInjection<?> injection : injections) {
                injection.getTarget().uninject();
            }
            final List<Runnable> tasks;
            synchronized (ServiceControllerImpl.this) {
                final boolean leavingRestState = isStableRestState();
                // Subtract one for this task
                decrementAsyncTasks();
                tasks = transition();
                addAsyncTasks(tasks.size());
                updateStabilityState(leavingRestState);
            }
            doExecute(tasks);
        }

        public ServiceController<?> getController() {
            return ServiceControllerImpl.this;
        }

        public void execute(final Runnable command) {
            final ClassLoader contextClassLoader = setTCCL(getCL(command.getClass()));
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

    private void addAsyncTasks(final int size) {
        assert holdsLock(this);
        assert size >= 0;
        if (size > 0) asyncTasks += size;
    }

    private void incrementAsyncTasks() {
        assert holdsLock(this);
        asyncTasks++;
    }

    private void decrementAsyncTasks() {
        assert holdsLock(this);
        assert asyncTasks > 0;
        asyncTasks--;
    }

}
