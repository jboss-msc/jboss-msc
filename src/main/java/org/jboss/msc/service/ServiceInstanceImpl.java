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

import java.io.IOException;
import java.io.Writer;
import java.util.Arrays;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;

import org.jboss.msc.value.Value;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 * @author <a href="mailto:flavia.rainone@jboss.com">Flavia Rainone</a>
 */
final class ServiceInstanceImpl<S> implements ServiceController<S>, Dependent {

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
     * The set of registered service listeners.
     */
    private final IdentityHashSet<ServiceListener<? super S>> listeners;
    /**
     * The set of dependents on this instance.
     */
    private final IdentityHashSet<Dependent> dependents = new IdentityHashSet<Dependent>(0);
    /**
     * The primary registration of this service.
     */
    private final ServiceRegistrationImpl primaryRegistration;
    /**
     * The alias registrations of this service.
     */
    private final ServiceRegistrationImpl[] aliasRegistrations;
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
     * The number of registrations which place a demand-to-start on this instance.  If this value is >0, propagate a demand
     * up to all parent dependents.  If this value is >0 and mode is ON_DEMAND, put a load of +1 on {@code upperCount}.
     */
    private int demandedByCount;
    /**
     * Semaphore count for bringing this dep up.  If the value is <= 0, the service is stopped.  Each unstarted
     * dependency will put a load of -1 on this value.  A mode of AUTOMATIC or IMMEDIATE will put a load of +1 on this
     * value.  A mode of NEVER will cause this value to be ignored.  A mode of ON_DEMAND will put a load of +1 on this
     * value <b>if</b> {@link #demandedByCount} is >0.
     */
    private int upperCount;
    /**
     * The number of dependents that are currently running.  The deployment will not execute the {@code stop()} method
     * (and subsequently leave the {@link org.jboss.msc.service.ServiceController.State#STOPPING} state) until all running dependents (and listeners) are stopped.
     */
    private int runningDependents;
    /**
     * Count for failure notification. It indicates how many services have failed to start and are not recovered so far.
     * This count monitors failures that happen when starting this service, and dependency related failures as well.
     * When incremented from 0 to 1, it is time to notify dependents and listeners that a failure occurred. When
     * decremented from 1 to 0, the dependents and listeners are notified that the affected services are retrying to
     * start. Values larger than 1 are ignored to avoid multiple notifications. 
     */
    private int failCount;

    /**
     * Count for notification of missing (uninstalled) dependencies. Its value indicates how many dependencies are
     * missing. When incremented from 0 to 1, dependents and listeners are notified of the missing dependency. When
     * decremented from 1 to 0, a notification that the missing dependencies are now installed is sent to dependents and
     * listeners. Values larger than 1 are ignored to avoid multiple notifications. 
     */
    private int missingDependencyCount;

    /**
     * The number of asynchronous tasks that are currently running.  This includes listeners, start/stop methods,
     * outstanding asynchronous start/stops, and internal tasks.
     */
    private int asyncTasks;

    private static final ServiceRegistrationImpl[] NO_REGISTRATIONS = new ServiceRegistrationImpl[0];
    private static final Dependent[] NO_DEPENDENTS = new Dependent[0];
    private static final ValueInjection<?>[] NO_INJECTIONS = new ValueInjection<?>[0];

    ServiceInstanceImpl(final Value<? extends Service<? extends S>> serviceValue, final Location location, final Dependency[] dependencies, final ValueInjection<?>[] injections, final ServiceRegistrationImpl primaryRegistration, final ServiceRegistrationImpl[] aliasRegistrations, final Set<? extends ServiceListener<? super S>> listeners) {
        this.serviceValue = serviceValue;
        this.location = location;
        this.dependencies = dependencies;
        this.injections = injections;
        this.primaryRegistration = primaryRegistration;
        this.aliasRegistrations = aliasRegistrations;
        this.listeners =  new IdentityHashSet<ServiceListener<? super S>>(listeners);
        upperCount = - dependencies.length;
    }

    ServiceInstanceImpl(final Value<? extends Service<? extends S>> serviceValue, final ServiceRegistrationImpl primaryRegistration) {
        this.serviceValue = serviceValue;
        this.primaryRegistration = primaryRegistration;
        location = null;
        dependencies = NO_REGISTRATIONS;
        injections = NO_INJECTIONS;
        aliasRegistrations = NO_REGISTRATIONS;
        listeners = new IdentityHashSet<ServiceListener<? super S>>(0);
        upperCount = - dependencies.length;
    }

    /**
     * Determine whether the lock is currently held.
     *
     * @return {@code true} if the lock is held
     */
    boolean lockHeld() {
        return Thread.holdsLock(this);
    }

    Substate getSubstateLocked() {
        return state;
    }

    void addAsyncTask() {
        asyncTasks++;
    }

    void removeAsyncTask() {
        asyncTasks--;
    }

    /**
     * Identify the transition to take.  Call under lock.
     *
     * @return the transition or {@code null} if none is needed at this time
     */
    private Transition getTransition() {
        assert lockHeld();
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
            case REMOVED: {
                // no possible actions
                break;
            }
        }
        return null;
    }

    /**
     * Run the locked portion of a transition.  Call under lock.
     *
     * @return the async tasks to start when the lock is not held, {@code null} for none
     */
    Runnable[] transition() {
        assert lockHeld();
        final Transition transition = getTransition();
        if (transition == null) {
            return null;
        }
        final Runnable[] tasks;
        switch (transition) {
            case STOPPING_to_DOWN: {
                tasks = getListenerTasks(transition.getAfter().getState(), new DependentStoppedTask());
                break;
            }
            case START_REQUESTED_to_DOWN: {
                tasks = new Runnable[] { new DependentStoppedTask() };
                break;
            }
            case START_REQUESTED_to_STARTING: {
                tasks = getListenerTasks(transition.getAfter().getState(), new StartTask(true));
                break;
            }
            case UP_to_STOP_REQUESTED: {
                tasks = new Runnable[] { new DependencyStoppedTask(dependents.toScatteredArray(NO_DEPENDENTS)) };
                break;
            }
            case STARTING_to_UP: {
                tasks = getListenerTasks(transition.getAfter().getState(), new DependencyStartedTask(dependents.toScatteredArray(NO_DEPENDENTS)));
                break;
            }
            case STARTING_to_START_FAILED: {
                tasks = getListenerTasks(transition.getAfter().getState(), new DependencyFailedTask(dependents.toScatteredArray(NO_DEPENDENTS)));
                break;
            }
            case START_FAILED_to_STARTING: {
                tasks = getListenerTasks(transition.getAfter().getState(), new DependencyRetryingTask(dependents.toScatteredArray(NO_DEPENDENTS)), new StartTask(false));
                break;
            }
            case START_FAILED_to_DOWN: {
                startException = null;
                failCount --;
                tasks = getListenerTasks(transition.getAfter().getState(), new DependencyRetryingTask(dependents.toScatteredArray(NO_DEPENDENTS)), new StopTask(true), new DependentStoppedTask());
                break;
            }
            case STOP_REQUESTED_to_UP: {
                tasks = new Runnable[] { new DependencyStartedTask(dependents.toScatteredArray(NO_DEPENDENTS)) };
                break;
            }
            case STOP_REQUESTED_to_STOPPING: {
                tasks = getListenerTasks(transition.getAfter().getState(), new StopTask(false));
                break;
            }
            case DOWN_to_REMOVING: {
                tasks = new Runnable[] { new RemoveTask() };
                break;
            }
            case REMOVING_to_REMOVED: {
                tasks = getListenerTasks(transition.getAfter().getState());
                listeners.clear();
                break;
            }
            case DOWN_to_START_REQUESTED: {
                tasks = new Runnable[] { new DependentStartedTask() };
                break;
            }
            default: {
                throw new IllegalStateException();
            }
        }
        state = transition.getAfter();
        asyncTasks += tasks.length;
        return tasks;
    }

    private Runnable[] getListenerTasks(final ServiceController.State newState, final Runnable extraTask1,
            final Runnable extraTask2, final Runnable extraTask3) {
        final IdentityHashSet<ServiceListener<? super S>> listeners = this.listeners;
        final int size = listeners.size();
        final Runnable[] tasks = new Runnable[size + 3];
        int i = 0;
        for (ServiceListener<? super S> listener : listeners) {
            tasks[i++] = new ListenerTask(listener, newState);
        }
        tasks[i++] = extraTask1;
        tasks[i++] = extraTask2;
        tasks[i] = extraTask3;
        return tasks;
    }

    private Runnable[] getListenerTasks(final ServiceController.State newState, final Runnable extraTask1, final Runnable extraTask2) {
        final IdentityHashSet<ServiceListener<? super S>> listeners = this.listeners;
        final int size = listeners.size();
        final Runnable[] tasks = new Runnable[size + 2];
        int i = 0;
        for (ServiceListener<? super S> listener : listeners) {
            tasks[i++] = new ListenerTask(listener, newState);
        }
        tasks[i++] = extraTask1;
        tasks[i] = extraTask2;
        return tasks;
    }

    private Runnable[] getListenerTasks(final ServiceController.State newState, final Runnable extraTask) {
        final IdentityHashSet<ServiceListener<? super S>> listeners = this.listeners;
        final int size = listeners.size();
        final Runnable[] tasks = new Runnable[size + 1];
        int i = 0;
        for (ServiceListener<? super S> listener : listeners) {
            tasks[i++] = new ListenerTask(listener, newState);
        }
        tasks[i] = extraTask;
        return tasks;
    }

    private Runnable[] getListenerTasks(final ServiceController.State newState) {
        final IdentityHashSet<ServiceListener<? super S>> listeners = this.listeners;
        final int size = listeners.size();
        final Runnable[] tasks = new Runnable[size];
        int i = 0;
        for (ServiceListener<? super S> listener : listeners) {
            tasks[i++] = new ListenerTask(listener, newState);
        }
        return tasks;
    }

    private Runnable[] getListenerTasks(final ListenerNotification notification, final Runnable extraTask) {
        final IdentityHashSet<ServiceListener<? super S>> listeners = this.listeners;
        final int size = listeners.size();
        final Runnable[] tasks = new Runnable[size + 1];
        int i = 0;
        for (ServiceListener<? super S> listener : listeners) {
            tasks[i++] = new ListenerTask(listener, notification);
        }
        tasks[i] = extraTask;
        return tasks;
    }

    void doExecute(final Runnable task) {
        assert ! lockHeld();
        if (task == null) return;
        try {
            primaryRegistration.getContainer().getExecutor().execute(task);
        } catch (RejectedExecutionException e) {
            task.run();
        }
    }

    void doExecute(final Runnable... tasks) {
        assert ! lockHeld();
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
        assert !lockHeld();
        if (newMode == null) {
            throw new IllegalArgumentException("newMode is null");
        }
        Runnable[] bootTasks = null;
        final Runnable[] tasks;
        Runnable specialTask = null;
        synchronized (this) {
            if (expectedMode != null && expectedMode != mode) {
                return false;
            }
            final Substate oldState = state;
            if (oldState == Substate.NEW) {
                state = Substate.DOWN;
                bootTasks = getListenerTasks(ListenerNotification.LISTENER_ADDED, new InstallTask());
                asyncTasks += bootTasks.length;
            }
            final ServiceController.Mode oldMode = mode;
            mode = newMode;
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
                        case ON_DEMAND: {
                            if (demandedByCount > 0) {
                                upperCount++;
                            }
                            break;
                        }
                        case PASSIVE: {
                            upperCount++;
                            break;
                        }
                        case ACTIVE: {
                            specialTask = new DemandParentsTask();
                            asyncTasks++;
                            upperCount++;
                            break;
                        }
                    }
                    break;
                }
                case ON_DEMAND: {
                    switch (newMode) {
                        case REMOVE:
                        case NEVER: {
                            if (demandedByCount > 0) {
                                upperCount--;
                            }
                            break;
                        }
                        case PASSIVE: {
                            if (demandedByCount == 0) {
                                upperCount++;
                            }
                            break;
                        }
                        case ACTIVE: {
                            specialTask = new DemandParentsTask();
                            asyncTasks++;
                            if (demandedByCount == 0) {
                                upperCount++;
                            }
                            break;
                        }
                    }
                    break;
                }
                case PASSIVE: {
                    switch (newMode) {
                        case REMOVE:
                        case NEVER: {
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
                            specialTask = new DemandParentsTask();
                            asyncTasks++;
                            break;
                        }
                    }
                    break;
                }
                case ACTIVE: {
                    switch (newMode) {
                        case REMOVE:
                        case NEVER: {
                            specialTask = new UndemandParentsTask();
                            asyncTasks++;
                            upperCount--;
                            break;
                        }
                        case ON_DEMAND: {
                            specialTask = new UndemandParentsTask();
                            asyncTasks++;
                            if (demandedByCount == 0) {
                                upperCount--;
                            }
                            break;
                        }
                        case PASSIVE: {
                            specialTask = new UndemandParentsTask();
                            asyncTasks++;
                            break;
                        }
                    }
                    break;
                }
            }
            tasks = (oldMode == newMode) ? null : transition();
        }
        if (bootTasks != null) {
            for (Runnable bootTask : bootTasks) {
                bootTask.run();
            }
        }
        doExecute(tasks);
        doExecute(specialTask);
        return true;
    }

    Dependency[] getDependencyLinks() {
        return dependencies;
    }

    @Override
    public void immediateDependencyInstalled() {
        dependencyInstalled();
    }

    @Override
    public void immediateDependencyUninstalled() {
        dependencyUninstalled();
    }

    @Override
    public void dependencyInstalled() {
        Runnable[] tasks = null;
        synchronized (this) {
            if (-- missingDependencyCount != 0) {
                return;
            }
            // we dropped it to 0
            tasks = getListenerTasks(ListenerNotification.DEPENDENCY_INSTALLED,
                    new DependencyInstalledTask(dependents.toScatteredArray(NO_DEPENDENTS)));
            asyncTasks += tasks.length;
        }
        doExecute(tasks);
    }

    @Override
    public void dependencyUninstalled() {
        Runnable[] tasks = null;
        synchronized (this) {
            if (++ missingDependencyCount != 1) {
                return;
            }
            // we raised it to 1
            tasks = getListenerTasks(ListenerNotification.MISSING_DEPENDENCY,
                    new DependencyUninstalledTask(dependents.toScatteredArray(NO_DEPENDENTS)));
            asyncTasks += tasks.length;
        }
        doExecute(tasks);
    }

    @Override
    public void immediateDependencyUp() {
        Runnable[] tasks = null;
        synchronized (this) {
            if (++upperCount != 1) {
                return;
            }
            // we raised it to 1
            tasks = transition();
        }
        doExecute(tasks);
    }

    @Override
    public void immediateDependencyDown() {
        Runnable[] tasks = null;
        synchronized (this) {
            if (--upperCount != 0) {
                return;
            }
            // we dropped it below 0
            tasks = transition();
        }
        doExecute(tasks);
    }

    @Override
    public void dependencyFailed() {
        Runnable[] tasks = null;
        synchronized (this) {
            if (++failCount != 1) {
                return;
            }
            // we raised it to 1
            tasks = getListenerTasks(ListenerNotification.DEPENDENCY_FAILURE,
                    new DependencyFailedTask(dependents.toScatteredArray(NO_DEPENDENTS)));
            asyncTasks += tasks.length;
        }
        doExecute(tasks);
    }

    @Override
    public void dependencyFailureCleared() {
        Runnable[] tasks = null;
        synchronized (this) {
            if (--failCount != 0) {
                return;
            }
            // we dropped it to 0
            tasks = getListenerTasks(ListenerNotification.DEPENDENCY_FAILURE_CLEAR,
                    new DependencyRetryingTask(dependents.toScatteredArray(NO_DEPENDENTS)));
            asyncTasks += tasks.length;
        }
        doExecute(tasks);
    }

    void dependentStarted() {
        assert ! lockHeld();
        synchronized (this) {
            runningDependents++;
        }
    }

    void dependentStopped() {
        assert ! lockHeld();
        final Runnable[] tasks;
        synchronized (this) {
            if (--runningDependents != 0) {
                return;
            }
            tasks = transition();
        }
        doExecute(tasks);
    }

    Runnable[] addDependent(final Dependent dependent) {
        assert lockHeld();
        dependents.add(dependent);
        final Runnable[] tasks;
        if (failCount > 0) {
            final Dependent[] dependents = new Dependent[]{dependent};
            if (missingDependencyCount > 0) {
                tasks = new Runnable[2];
                tasks[1] = new DependencyUninstalledTask(dependents);
            } else {
                tasks = new Runnable[1];
            }
            tasks[0] = new DependencyFailedTask(dependents);
            asyncTasks += tasks.length;
            return tasks;
        } else if (missingDependencyCount > 0) {
            tasks = new Runnable[]{new DependencyUninstalledTask(new Dependent[]{dependent})};
            asyncTasks ++;
        } else {
            tasks = null;
        }
        return tasks;
    }

    void removeDependent(final Dependent dependent) {
        dependents.remove(dependent);
    }

    void addDependents(final IdentityHashSet<Dependent> dependents) {
        this.dependents.addAll(dependents);
        // do not notify dependents of failures and missing dependencies because, at this point,
        // failCount and missingDependencyCount must be 0
    }

    void removeAllDependents(final IdentityHashSet<ServiceInstanceImpl<?>> dependents) {
        this.dependents.removeAll(dependents);
    }

    private void doDemandParents() {
        assert ! lockHeld();
        for (Dependency dependency : dependencies) {
            dependency.addDemand();
        }
    }

    private void doUndemandParents() {
        assert ! lockHeld();
        for (Dependency dependency : dependencies) {
            dependency.removeDemand();
        }
    }

    void addDemand() {
        addDemands(1);
    }

    void addDemands(final int demandedByCount) {
        assert ! lockHeld();
        final Runnable[] tasks;
        final boolean propagate;
        synchronized (this) {
            final int cnt = this.demandedByCount;
            this.demandedByCount += demandedByCount;
            propagate = cnt == 0;
            if (cnt == 0 && mode == Mode.ON_DEMAND) {
                upperCount++;
                tasks = transition();
            } else {
                // no change
                tasks = null;
            }
            if (propagate) asyncTasks++;
        }
        doExecute(tasks);
        if (propagate) doExecute(new DemandParentsTask());
    }

    void removeDemand() {
        assert ! lockHeld();
        final Runnable[] tasks;
        final boolean propagate;
        synchronized (this) {
            final int cnt = --demandedByCount;
            propagate = cnt == 0;
            if (cnt == 0 && mode == Mode.ON_DEMAND) {
                upperCount--;
                tasks = transition();
            } else {
                // no change
                tasks = null;
            }
            if (propagate) asyncTasks++;
        }
        doExecute(tasks);
        if (propagate) doExecute(new UndemandParentsTask());
    }

    public ServiceContainer getServiceContainer() {
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
        assert !lockHeld();
        final Substate state;
        synchronized (this) {
            state = this.state;
            // Always run listener if removed.
            if (state != Substate.REMOVED) {
                if (! listeners.add(listener)) {
                    // Duplicates not allowed
                    throw new IllegalArgumentException("Listener " + listener + " already present on controller for " + primaryRegistration.getName());
                }
            }
            asyncTasks++;
        }
        invokeListener(listener, ListenerNotification.LISTENER_ADDED, null);
    }

    public void removeListener(final ServiceListener<? super S> listener) {
        synchronized (this) {
            listeners.remove(listener);
        }
    }

    public StartException getStartException() {
        synchronized (this) {
            return startException;
        }
    }

    public void retry() {
        assert !lockHeld();
        final Runnable[] tasks;
        synchronized (this) {
            if (state.getState() != ServiceController.State.START_FAILED) {
                return;
            }
            startException = null;
            failCount --;
            tasks = transition();
        }
        doExecute(tasks);
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

    private static enum ListenerNotification {
        /** Notify the listener that is has been added. */
        LISTENER_ADDED,
        /** Notifications related to the current state.  */
        STATE,
        /** Notify the listener that a dependency failure occurred. */
        DEPENDENCY_FAILURE,
        /** Notify the listener that all dependency failures are cleared. */
        DEPENDENCY_FAILURE_CLEAR,
        /** Notify the listener that a dependency is missing (uninstalled). */
        MISSING_DEPENDENCY,
        /** Notify the listener that all missing dependencies are now installed. */
        DEPENDENCY_INSTALLED}

    /**
     * Invokes the listener, performing the notification specified.
     * 
     * @param listener      listener to be invoked
     * @param notification  specified notification
     * @param state         the state to be notified, only relevant if {@code notification} is
     *                      {@link ListenerNotification#STATE}
     */
    private void invokeListener(final ServiceListener<? super S> listener, final ListenerNotification notification, final State state ) {
        assert !lockHeld();
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
                case MISSING_DEPENDENCY:
                    listener.dependencyUninstalled(this);
                    break;
                case DEPENDENCY_INSTALLED:
                    listener.dependencyInstalled(this);
                    break;
            }
        } catch (Throwable t) {
            ServiceLogger.INSTANCE.listenerFailed(t, listener);
        } finally {
            final Runnable[] tasks;
            synchronized (this) {
                asyncTasks--;
                tasks = transition();
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

    enum ContextState {
        SYNC,
        ASYNC,
        COMPLETE,
        FAILED,
    }

    private static <T> void doInject(final ValueInjection<T> injection) {
        injection.getTarget().inject(injection.getSource().getValue());
    }

    private class DemandParentsTask implements Runnable {

        public void run() {
            try {
                doDemandParents();
                final Runnable[] tasks;
                synchronized (ServiceInstanceImpl.this) {
                    asyncTasks--;
                    tasks = transition();
                }
                doExecute(tasks);
            } catch (Throwable t) {
                ServiceLogger.INSTANCE.internalServiceError(t, primaryRegistration.getName());
            }
        }
    }

    private class UndemandParentsTask implements Runnable {

        public void run() {
            try {
                doUndemandParents();
                final Runnable[] tasks;
                synchronized (ServiceInstanceImpl.this) {
                    asyncTasks--;
                    tasks = transition();
                }
                doExecute(tasks);
            } catch (Throwable t) {
                ServiceLogger.INSTANCE.internalServiceError(t, primaryRegistration.getName());
            }
        }
    }

    private class DependentStoppedTask implements Runnable {

        public void run() {
            try {
                for (Dependency dependency : dependencies) {
                    dependency.dependentStopped();
                }
                final Runnable[] tasks;
                synchronized (ServiceInstanceImpl.this) {
                    asyncTasks--;
                    tasks = transition();
                }
                doExecute(tasks);
            } catch (Throwable t) {
                ServiceLogger.INSTANCE.internalServiceError(t, primaryRegistration.getName());
            }
        }
    }

    private class DependentStartedTask implements Runnable {

        public void run() {
            try {
                for (Dependency dependency : dependencies) {
                    dependency.dependentStarted();
                }
                final Runnable[] tasks;
                synchronized (ServiceInstanceImpl.this) {
                    asyncTasks--;
                    tasks = transition();
                }
                doExecute(tasks);
            } catch (Throwable t) {
                ServiceLogger.INSTANCE.internalServiceError(t, primaryRegistration.getName());
            }
        }
    }

    private class StartTask implements Runnable {

        private final boolean doInjection;

        StartTask(final boolean doInjection) {
            this.doInjection = doInjection;
        }

        public void run() {
            assert !lockHeld();
            final ServiceName serviceName = primaryRegistration.getName();
            final long startNanos = System.nanoTime();
            final StartContextImpl context = new StartContextImpl(startNanos);
            try {
                if (doInjection) {
                    final ValueInjection<?>[] injections = ServiceInstanceImpl.this.injections;
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
                final Service<? extends S> service = serviceValue.getValue();
                if (service == null) {
                    throw new IllegalArgumentException("Service is null");
                }
                service.start(context);
                final Runnable[] tasks;
                synchronized (ServiceInstanceImpl.this) {
                    if (context.state != ContextState.SYNC) {
                        return;
                    }
                    context.state = ContextState.COMPLETE;
                    asyncTasks--;
                    if (ServiceContainerImpl.PROFILE_OUTPUT != null) {
                        writeProfileInfo('S', startNanos, System.nanoTime());
                    }
                    tasks = transition();
                }
                doExecute(tasks);
            } catch (StartException e) {
                e.setServiceName(serviceName);
                final Runnable[] tasks;
                synchronized (ServiceInstanceImpl.this) {
                    final ContextState oldState = context.state;
                    if (oldState != ContextState.SYNC && oldState != ContextState.ASYNC) {
                        ServiceLogger.INSTANCE.exceptionAfterComplete(e, serviceName);
                        return;
                    }
                    context.state = ContextState.FAILED;
                    asyncTasks--;
                    startException = e;
                    if (ServiceContainerImpl.PROFILE_OUTPUT != null) {
                        writeProfileInfo('F', startNanos, System.nanoTime());
                    }
                    failCount++;
                    tasks = transition();
                }
                doExecute(tasks);
            } catch (Throwable t) {
                final Runnable[] tasks;
                synchronized (ServiceInstanceImpl.this) {
                    final ContextState oldState = context.state;
                    if (oldState != ContextState.SYNC && oldState != ContextState.ASYNC) {
                        ServiceLogger.INSTANCE.exceptionAfterComplete(t, serviceName);
                        return;
                    }
                    context.state = ContextState.FAILED;
                    asyncTasks--;
                    startException = new StartException("Failed to start service", t, location, serviceName);
                    if (ServiceContainerImpl.PROFILE_OUTPUT != null) {
                        writeProfileInfo('F', startNanos, System.nanoTime());
                    }
                    failCount ++;
                    tasks = transition();
                }
                doExecute(tasks);
            }
        }
    }

    private class StopTask implements Runnable {
        private final boolean onlyUninject;

        StopTask(final boolean onlyUninject) {
            this.onlyUninject = onlyUninject;
        }

        public void run() {
            assert !lockHeld();
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
                            ServiceLogger.INSTANCE.stopServiceMissing(serviceName);
                        }
                    } catch (Throwable t) {
                        ServiceLogger.INSTANCE.stopFailed(t, serviceName);
                    }
                }
            } finally {
                Runnable[] tasks = null;
                synchronized (ServiceInstanceImpl.this) {
                    if (ok && context.state != ContextState.SYNC) {
                        // We want to discard the exception anyway, if there was one.  Which there can't be.
                        //noinspection ReturnInsideFinallyBlock
                        return;
                    }
                    context.state = ContextState.COMPLETE;
                }
                for (ValueInjection<?> injection : injections) try {
                    injection.getTarget().uninject();
                } catch (Throwable t) {
                    ServiceLogger.INSTANCE.uninjectFailed(t, serviceName, injection);
                }
                synchronized (ServiceInstanceImpl.this) {
                    asyncTasks--;
                    if (ServiceContainerImpl.PROFILE_OUTPUT != null) {
                        writeProfileInfo('X', startNanos, System.nanoTime());
                    }
                    tasks = transition();
                }
                doExecute(tasks);
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
            assert !lockHeld();
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

        private final Dependent[] dependents;

        DependencyStartedTask(final Dependent[] dependents) {
            this.dependents = dependents;
        }

        public void run() {
            try {
                for (Dependent dependent : dependents) {
                    if (dependent != null) dependent.immediateDependencyUp();
                }
                final Runnable[] tasks;
                synchronized (ServiceInstanceImpl.this) {
                    asyncTasks--;
                    tasks = transition();
                }
                doExecute(tasks);
            } catch (Throwable t) {
                ServiceLogger.INSTANCE.internalServiceError(t, primaryRegistration.getName());
            }
        }
    }

    private class DependencyStoppedTask implements Runnable {

        private final Dependent[] dependents;

        DependencyStoppedTask(final Dependent[] dependents) {
            this.dependents = dependents;
        }

        public void run() {
            try {
                for (Dependent dependent : dependents) {
                    if (dependent != null) dependent.immediateDependencyDown();
                }
                final Runnable[] tasks;
                synchronized (ServiceInstanceImpl.this) {
                    asyncTasks--;
                    tasks = transition();
                }
                doExecute(tasks);
            } catch (Throwable t) {
                ServiceLogger.INSTANCE.internalServiceError(t, primaryRegistration.getName());
            }
        }
    }

    private class DependencyFailedTask implements Runnable {

        private final Dependent[] dependents;

        DependencyFailedTask(final Dependent[] dependents) {
            this.dependents = dependents;
        }

        public void run() {
            try {
                for (Dependent dependent : dependents) {
                    if (dependent != null) dependent.dependencyFailed();
                }
                final Runnable[] tasks;
                synchronized (ServiceInstanceImpl.this) {
                    asyncTasks--;
                    tasks = transition();
                }
                doExecute(tasks);
            } catch (Throwable t) {
                ServiceLogger.INSTANCE.internalServiceError(t, primaryRegistration.getName());
            }
        }
    }

    private class DependencyRetryingTask implements Runnable {

        private final Dependent[] dependents;

        DependencyRetryingTask(final Dependent[] dependents) {
            this.dependents = dependents;
        }

        public void run() {
            try {
                for (Dependent dependent : dependents) {
                    if (dependent != null) dependent.dependencyFailureCleared();
                }
                final Runnable[] tasks;
                synchronized (ServiceInstanceImpl.this) {
                    asyncTasks--;
                    tasks = transition();
                }
                doExecute(tasks);
            } catch (Throwable t) {
                ServiceLogger.INSTANCE.internalServiceError(t, primaryRegistration.getName());
            }
        }
    }

    private class DependencyInstalledTask implements Runnable {

        private final Dependent[] dependents;

        DependencyInstalledTask(final Dependent[] dependents) {
            this.dependents = dependents;
        }

        public void run() {
            try {
                for (Dependent dependent : dependents) {
                    if (dependent != null) dependent.dependencyInstalled();
                }
                final Runnable[] tasks;
                synchronized (ServiceInstanceImpl.this) {
                    asyncTasks--;
                    tasks = transition();
                }
                doExecute(tasks);
            } catch (Throwable t) {
                ServiceLogger.INSTANCE.internalServiceError(t, primaryRegistration.getName());
            }
        }
    }

    private class DependencyUninstalledTask implements Runnable {

        private final Dependent[] dependents;

        DependencyUninstalledTask(final Dependent[] dependents) {
            this.dependents = dependents;
        }

        public void run() {
            try {
                for (Dependent dependent : dependents) {
                    if (dependent != null) dependent.dependencyUninstalled();
                }
                final Runnable[] tasks;
                synchronized (ServiceInstanceImpl.this) {
                    asyncTasks--;
                    tasks = transition();
                }
                doExecute(tasks);
            } catch (Throwable t) {
                ServiceLogger.INSTANCE.internalServiceError(t, primaryRegistration.getName());
            }
        }
    }

    private class InstallTask implements Runnable {

        private InstallTask() {
        }

        public void run() {
            try {
                for (Dependency dependency : dependencies) {
                    dependency.addDependent(ServiceInstanceImpl.this);
                }
                final Runnable[] tasks;
                synchronized (ServiceInstanceImpl.this) {
                    asyncTasks--;
                    tasks = transition();
                }
                doExecute(tasks);
            } catch (Throwable t) {
                ServiceLogger.INSTANCE.internalServiceError(t, primaryRegistration.getName());
            }
        }
    }

    private class RemoveTask implements Runnable {

        RemoveTask() {
        }

        public void run() {
            try {
                assert getMode() == ServiceController.Mode.REMOVE;
                assert getSubstate() == Substate.REMOVING;
                if (failCount > 0) {
                    for (Dependent dependent: dependents) {
                        dependent.dependencyFailureCleared();
                    }
                }
                if (missingDependencyCount > 0) {
                    for (Dependent dependent: dependents) {
                        dependent.dependencyInstalled();
                    }
                }
                primaryRegistration.clearInstance(ServiceInstanceImpl.this);
                for (ServiceRegistrationImpl registration : aliasRegistrations) {
                    registration.clearInstance(ServiceInstanceImpl.this);
                }
                for (Dependency dependency : dependencies) {
                    dependency.removeDependent(ServiceInstanceImpl.this);
                }
                final Runnable[] tasks;
                synchronized (ServiceInstanceImpl.this) {
                    Arrays.fill(dependencies, null);
                    asyncTasks--;
                    tasks = transition();
                }
                doExecute(tasks);
            } catch (Throwable t) {
                ServiceLogger.INSTANCE.internalServiceError(t, primaryRegistration.getName());
            }
        }
    }

    private class StartContextImpl implements StartContext {

        private ContextState state = ContextState.SYNC;

        private final long startNanos;

        private StartContextImpl(final long startNanos) {
            this.startNanos = startNanos;
        }

        public void failed(final StartException reason) throws IllegalStateException {
            final Runnable[] tasks;
            synchronized (ServiceInstanceImpl.this) {
                if (state != ContextState.ASYNC) {
                    throw new IllegalStateException(ILLEGAL_CONTROLLER_STATE);
                }
                state = ContextState.FAILED;
                startException = reason;
                failCount ++;
                asyncTasks--;
                if (ServiceContainerImpl.PROFILE_OUTPUT != null) {
                    writeProfileInfo('F', startNanos, System.nanoTime());
                }
                tasks = transition();
            }
            doExecute(tasks);
        }

        public void asynchronous() throws IllegalStateException {
            synchronized (ServiceInstanceImpl.this) {
                if (state == ContextState.SYNC) {
                    state = ContextState.ASYNC;
                } else {
                    throw new IllegalStateException(ILLEGAL_CONTROLLER_STATE);
                }
            }
        }

        public void complete() throws IllegalStateException {
            final Runnable[] tasks;
            synchronized (ServiceInstanceImpl.this) {
                if (state != ContextState.ASYNC) {
                    throw new IllegalStateException(ILLEGAL_CONTROLLER_STATE);
                } else {
                    state = ContextState.COMPLETE;
                    asyncTasks--;
                    if (ServiceContainerImpl.PROFILE_OUTPUT != null) {
                        writeProfileInfo('S', startNanos, System.nanoTime());
                    }
                    tasks = transition();
                }
            }
            doExecute(tasks);
        }

        public ServiceController<?> getController() {
            return ServiceInstanceImpl.this;
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
                    profileOutput.write(String.format("%s\t%s\t%d\t%d\n", name, Character.valueOf(statusChar), Long.valueOf(startOffset), Long.valueOf(duration)));
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
            synchronized (ServiceInstanceImpl.this) {
                if (state == ContextState.SYNC) {
                    state = ContextState.ASYNC;
                } else {
                    throw new IllegalStateException(ILLEGAL_CONTROLLER_STATE);
                }
            }
        }

        public void complete() throws IllegalStateException {
            synchronized (ServiceInstanceImpl.this) {
                if (state != ContextState.ASYNC) {
                    throw new IllegalStateException(ILLEGAL_CONTROLLER_STATE);
                }
                state = ContextState.COMPLETE;
            }
            for (ValueInjection<?> injection : injections) {
                injection.getTarget().uninject();
            }
            final Runnable[] tasks;
            synchronized (ServiceInstanceImpl.this) {
                asyncTasks--;
                if (ServiceContainerImpl.PROFILE_OUTPUT != null) {
                    writeProfileInfo('X', startNanos, System.nanoTime());
                }
                tasks = transition();
            }
            doExecute(tasks);
        }

        public ServiceController<?> getController() {
            return ServiceInstanceImpl.this;
        }
    }

    enum Substate {
        NEW(ServiceController.State.DOWN),
        DOWN(ServiceController.State.DOWN),
        START_REQUESTED(ServiceController.State.DOWN),
        STARTING(ServiceController.State.STARTING),
        START_FAILED(ServiceController.State.START_FAILED),
        UP(ServiceController.State.UP),
        STOP_REQUESTED(ServiceController.State.UP),
        STOPPING(ServiceController.State.STOPPING),
        REMOVING(ServiceController.State.DOWN),
        REMOVED(ServiceController.State.REMOVED),
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
