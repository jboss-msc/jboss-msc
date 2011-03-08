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
     * Indicates if this service has one or more (possibly transitive)
     * dependencies that are not installed. Count for notification of missing
     * (uninstalled) dependencies. Its value indicates how many dependencies are
     * missing. When incremented from 0 to 1, dependents and listeners are
     * notified of the missing dependency. When decremented from 1 to 0, a
     * notification that the missing dependencies are now installed is sent to
     * dependents and listeners. Values larger than 1 are ignored to avoid
     * multiple notifications.
     */
    private int missingDepCount;
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
     * Start this service installation, connecting it to its parent and dependencies. Also,
     * set the instance in primary and alias registrations.
     * <p>
     * All notifications from dependencies, parents, and registrations will be ignored until the
     * installation is {@link #commitInstallation(org.jboss.msc.service.ServiceController.Mode) committed}.
     */
    final void startInstallation() {
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
    final void commitInstallation(Mode initialMode) {
        assert (state == Substate.NEW);
        final Runnable[] listenerAddedTasks;
        final Runnable specialTask;
        synchronized(this) {
            listenerAddedTasks = getListenerTasks(ListenerNotification.LISTENER_ADDED);
            asyncTasks += listenerAddedTasks.length + 1;
            specialTask = internalSetMode(initialMode == null ? ServiceController.Mode.ACTIVE : initialMode);
        }
        doExecute(specialTask);
        for (Runnable listenerAddedTask : listenerAddedTasks) {
            listenerAddedTask.run();
        }
        final Runnable[] asyncListenerTasks;
        final Runnable[] transitionTasks;
        synchronized (this) {
            if (failCount > 0) {
                if (missingDepCount > 0) {
                    asyncListenerTasks = getListenerTasks(new ListenerNotification[] { ListenerNotification.DEPENDENCY_FAILURE,
                            ListenerNotification.MISSING_DEPENDENCY},
                            new DependencyFailedTask(getDependents()), new DependencyUninstalledTask(getDependents()));
                    asyncTasks += asyncListenerTasks.length;
                }
                else {
                    asyncListenerTasks = getListenerTasks(ListenerNotification.DEPENDENCY_FAILURE, new DependencyFailedTask(getDependents()));
                    asyncTasks += asyncListenerTasks.length;
                }
            }
            else if (missingDepCount > 0) {
                asyncListenerTasks = getListenerTasks(ListenerNotification.MISSING_DEPENDENCY, new DependencyUninstalledTask(getDependents()));
                asyncTasks += asyncListenerTasks.length;
            }
            else {
                asyncListenerTasks = null;
            }
            state = Substate.DOWN;
            asyncTasks --;
            transitionTasks = transition();
        }
        doExecute(asyncListenerTasks);
        doExecute(transitionTasks);
    }

    /**
     * Roll back the service install.
     *
     */
    final void rollbackInstallation() {
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
            case NEW:
                throw new IllegalStateException("Service " + getName() + " in NEW state");
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
     * @return the async tasks to start when the lock is not held, {@code null} for none
     */
    Runnable[] transition() {
        assert holdsLock(this);
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
                lifecycleTime = System.nanoTime();
                tasks = new Runnable[] { new DependencyStoppedTask(getDependents()) };
                break;
            }
            case STARTING_to_UP: {
                tasks = getListenerTasks(transition.getAfter().getState(), new DependencyStartedTask(getDependents()));
                break;
            }
            case STARTING_to_START_FAILED: {
                ChildServiceTarget childTarget = this.childTarget;
                if (childTarget != null) {
                    childTarget.valid = false;
                    this.childTarget = null;
                }
                if (! children.isEmpty()) {
                    asyncTasks++;
                    for (ServiceControllerImpl<?> child : children) {
                        child.setMode(Mode.REMOVE);
                    }
                }
                tasks = getListenerTasks(transition.getAfter().getState(), new DependencyFailedTask(getDependents()));
                break;
            }
            case START_FAILED_to_STARTING: {
                tasks = getListenerTasks(transition.getAfter().getState(), new DependencyRetryingTask(getDependents()),
                        new StartTask(false));
                break;
            }
            case START_FAILED_to_DOWN: {
                startException = null;
                failCount--;
                if (failCount == 0) {
                    tasks = getListenerTasks(transition.getAfter().getState(), new DependencyRetryingTask(getDependents()),
                        new StopTask(true), new DependentStoppedTask());
                }
                else {
                    tasks = new Runnable[] {new StopTask(true), new DependentStoppedTask()};
                }
                break;
            }
            case STOP_REQUESTED_to_UP: {
                tasks = new Runnable[] { new DependencyStartedTask(getDependents()) };
                break;
            }
            case STOP_REQUESTED_to_STOPPING: {
                ChildServiceTarget childTarget = this.childTarget;
                if (childTarget != null) {
                    childTarget.valid = false;
                    this.childTarget = null;
                }
                if (! children.isEmpty()) {
                    asyncTasks++;
                    for (ServiceControllerImpl<?> child : children) {
                        child.setMode(Mode.REMOVE);
                    }
                }
                tasks = getListenerTasks(transition.getAfter().getState(), new StopTask(false));
                break;
            }
            case DOWN_to_REMOVING: {
                Dependent[][] dependents = getDependents();
                if (failCount > 0) {
                    if (missingDepCount > 0) {
                        tasks = new Runnable[3];
                        tasks[1] = new DependencyInstalledTask(dependents);
                    }
                    else {
                        tasks = new Runnable[2];
                    }
                    tasks[0] = new DependencyRetryingTask(dependents);
                }
                else if (missingDepCount > 0) {
                    tasks = new Runnable[2];
                    tasks [0] = new DependencyInstalledTask(dependents);
                }
                else {
                    tasks = new Runnable[1];
                }
                tasks[tasks.length -1] = new RemoveTask();
                break;
            }
            case REMOVING_to_REMOVED: {
                tasks = getListenerTasks(transition.getAfter().getState());
                listeners.clear();
                break;
            }
            case DOWN_to_START_REQUESTED: {
                lifecycleTime = System.nanoTime();
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

    private Runnable[] getListenerTasks(final ServiceController.State newState, final Runnable... extraTasks) {
        final IdentityHashSet<ServiceListener<? super S>> listeners = this.listeners;
        final int size = listeners.size();
        final Runnable[] tasks = new Runnable[size + extraTasks.length];
        int i = 0;
        for (ServiceListener<? super S> listener : listeners) {
            tasks[i++] = new ListenerTask(listener, newState);
        }
        for (Runnable extraTask: extraTasks) {
            tasks[i++] = extraTask;
        }
        return tasks;
    }

    private Runnable[] getListenerTasks(final ListenerNotification notification, final Runnable extraTask) {
        final IdentityHashSet<ServiceListener<? super S>> listeners = this.listeners;
        final Runnable[] tasks = new Runnable[listeners.size() + 1];
        int i = 0;
        for (ServiceListener<? super S> listener : listeners) {
            tasks[i++] = new ListenerTask(listener, notification);
        }
        tasks[i] = extraTask;
        return tasks;
    }

    private Runnable[] getListenerTasks(final ListenerNotification notification) {
        final IdentityHashSet<ServiceListener<? super S>> listeners = this.listeners;
        final Runnable[] tasks = new Runnable[listeners.size()];
        int i = 0;
        for (ServiceListener<? super S> listener : listeners) {
            tasks[i++] = new ListenerTask(listener, notification);
        }
        return tasks;
    }

    private Runnable[] getListenerTasks(final ListenerNotification[] notifications, final Runnable... extraTasks) {
        final IdentityHashSet<ServiceListener<? super S>> listeners = this.listeners;
        final Runnable[] tasks = new Runnable[listeners.size() * notifications.length + extraTasks.length];
        int i = 0;
        for (ListenerNotification notification: notifications) {
            for (ServiceListener<? super S> listener : listeners) {
                tasks[i++] = new ListenerTask(listener, notification);
            }
        }
        for (Runnable extraTask: extraTasks) {
            tasks[i++] = extraTask;
        }
        return tasks;
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

    void doExecute(final Runnable... tasks) {
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
        final Runnable[] tasks;
        Runnable specialTask = null;
        synchronized (this) {
            if (expectedMode != null && expectedMode != mode) {
                return false;
            }
            if (mode == newMode) {
                return true;
            }
            specialTask = internalSetMode(newMode);
            tasks = transition();
        }
        doExecute(tasks);
        doExecute(specialTask);
        return true;
    }

    private Runnable internalSetMode(final ServiceController.Mode newMode) {
        assert holdsLock(this);
        Runnable specialTask = null;
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
                    case ON_DEMAND: {
                        if (demandedByCount > 0) {
                            upperCount++;
                            specialTask = new DemandParentsTask();
                            asyncTasks++;
                        }
                        break;
                    }
                    case PASSIVE: {
                        upperCount++;
                        if (demandedByCount > 0) {
                            specialTask = new DemandParentsTask();
                            asyncTasks++;
                        }
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
                            specialTask = new UndemandParentsTask();
                            asyncTasks++;
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
                        if (demandedByCount > 0) {
                            specialTask = new UndemandParentsTask();
                            asyncTasks++;
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
                        if (demandedByCount == 0) {
                            upperCount--;
                            specialTask = new UndemandParentsTask();
                            asyncTasks++;
                        }
                        break;
                    }
                    case PASSIVE: {
                        if (demandedByCount == 0) {
                            specialTask = new UndemandParentsTask();
                            asyncTasks++;
                        }
                        break;
                    }
                }
                break;
            }
        }
        mode = newMode;
        return specialTask;
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
        final Runnable[] tasks;
        synchronized (this) {
            if (--missingDepCount != 0 || state.compareTo(Substate.CANCELLED) <= 0) {
                return;
            }
            // we dropped it to 0
            tasks = getListenerTasks(ListenerNotification.DEPENDENCY_INSTALLED,
                    new DependencyInstalledTask(getDependents()));
            asyncTasks += tasks.length;
        }
        doExecute(tasks);
    }

    /** {@inheritDoc} */
    public ServiceControllerImpl<?> getController() {
        return this;
    }

    @Override
    public void dependencyUninstalled() {
        final Runnable[] tasks;
        synchronized (this) {
            if (++missingDepCount != 1 || state.compareTo(Substate.CANCELLED) <= 0) {
                return;
            }
            // we raised it to 1
            tasks = getListenerTasks(ListenerNotification.MISSING_DEPENDENCY,
                    new DependencyUninstalledTask(getDependents()));
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
            if (++failCount != 1 || state.compareTo(Substate.CANCELLED) <= 0) {
                return;
            }
            // we raised it to 1
            tasks = getListenerTasks(ListenerNotification.DEPENDENCY_FAILURE, new DependencyFailedTask(getDependents()));
            asyncTasks += tasks.length;
        }
        doExecute(tasks);
    }

    @Override
    public void dependencyFailureCleared() {
        Runnable[] tasks = null;
        synchronized (this) {
            if (--failCount != 0 || state.compareTo(Substate.CANCELLED) <= 0) {
                return;
            }
            // we dropped it to 0
            tasks = getListenerTasks(ListenerNotification.DEPENDENCY_FAILURE_CLEAR,
                    new DependencyRetryingTask(getDependents()));
            asyncTasks += tasks.length;
        }
        doExecute(tasks);
    }

    Dependency[] getDependencies() {
        return dependencies;
    }

    void dependentStarted() {
        assert !holdsLock(this);
        synchronized (this) {
            runningDependents++;
        }
    }

    void dependentStopped() {
        assert !holdsLock(this);
        final Runnable[] tasks;
        synchronized (this) {
            if (--runningDependents != 0) {
                return;
            }
            tasks = transition();
        }
        doExecute(tasks);
    }

    Runnable[] newDependent(final Dependent dependent) {
        assert holdsLock(this);
        final Runnable[] tasks;
        synchronized (this) {
            if (failCount > 0) {
                final Dependent[][] dependents = new Dependent[][] { { dependent } };
                if (missingDepCount > 0) {
                    tasks = new Runnable[2];
                    tasks[1] = new DependencyUninstalledTask(dependents);
                } else {
                    tasks = new Runnable[1];
                }
                tasks[0] = new DependencyFailedTask(dependents);
                asyncTasks += tasks.length;
            } else if (missingDepCount > 0) {
                tasks = new Runnable[] { new DependencyUninstalledTask(new Dependent[][] { { dependent } }) };
                asyncTasks++;
            } else if (state == Substate.UP) {
                tasks = new Runnable[] { new DependencyStartedTask(new Dependent[][] { { dependent } }) };
                asyncTasks++;
            } else {
                tasks = null;
            }
        }
        return tasks;
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
        final Runnable[] tasks;
        final boolean propagate;
        synchronized (this) {
            final int cnt = this.demandedByCount;
            this.demandedByCount += demandedByCount;
            propagate = cnt == 0 && mode.compareTo(Mode.NEVER) > 0;
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
        assert !holdsLock(this);
        final Runnable[] tasks;
        final boolean propagate;
        synchronized (this) {
            final int cnt = --demandedByCount;
            propagate = cnt == 0 && (mode == Mode.ON_DEMAND || mode == Mode.PASSIVE);
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

    void addChild(ServiceControllerImpl<?> child) {
        assert !holdsLock(this);
        final Runnable[] tasks;
        synchronized (this) {
            switch (state) {
                case STARTING:
                case UP:
                case STOP_REQUESTED: {
                    children.add(child);
                    tasks = newDependent(child);
                    break;
                }
                default: throw new IllegalStateException("Children cannot be added in state " + state.getState());
            }
        }
        doExecute(tasks);
    }

    void removeChild(ServiceControllerImpl<?> child) {
        assert !holdsLock(this);
        final Runnable[] tasks;
        synchronized (this) {
            children.remove(child);
            if (children.isEmpty()) {
                switch (state) {
                    case START_FAILED:
                    case STOPPING:
                        asyncTasks--;
                        tasks = transition();
                        break;
                    default:
                        return;
                }
            } else {
                return;
            }
        }
        doExecute(tasks);
    }

    IdentityHashSet<ServiceControllerImpl<?>> getChildren() {
        assert holdsLock(this);
        return this.children;
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

    public StartException getStartException() {
        synchronized (this) {
            return startException;
        }
    }

    public void retry() {
        assert !holdsLock(this);
        final Runnable[] tasks;
        synchronized (this) {
            if (state.getState() != ServiceController.State.START_FAILED) {
                return;
            }
            failCount--;
            startException = null;
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
                    missingDepCount != 0
            );
        }
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
                case MISSING_DEPENDENCY:
                    listener.dependencyUninstalled(this);
                    break;
                case DEPENDENCY_INSTALLED:
                    listener.dependencyInstalled(this);
                    break;
            }
        } catch (Throwable t) {
            ServiceLogger.SERVICE.listenerFailed(t, listener);
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
                final Runnable[] tasks;
                synchronized (ServiceControllerImpl.this) {
                    asyncTasks--;
                    tasks = transition();
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
                final Runnable[] tasks;
                synchronized (ServiceControllerImpl.this) {
                    asyncTasks--;
                    tasks = transition();
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
                final Runnable[] tasks;
                synchronized (ServiceControllerImpl.this) {
                    asyncTasks--;
                    tasks = transition();
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
                final Runnable[] tasks;
                synchronized (ServiceControllerImpl.this) {
                    asyncTasks--;
                    tasks = transition();
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
                final Runnable[] tasks;
                synchronized (ServiceControllerImpl.this) {
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
                performOutInjections(serviceName);
                doExecute(tasks);
            } catch (StartException e) {
                e.setServiceName(serviceName);
                ServiceLogger.FAIL.startFailed(e, serviceName);
                final Runnable[] tasks;
                synchronized (ServiceControllerImpl.this) {
                    final ContextState oldState = context.state;
                    if (oldState != ContextState.SYNC && oldState != ContextState.ASYNC) {
                        ServiceLogger.FAIL.exceptionAfterComplete(e, serviceName);
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
                synchronized (ServiceControllerImpl.this) {
                    final ContextState oldState = context.state;
                    if (oldState != ContextState.SYNC && oldState != ContextState.ASYNC) {
                        ServiceLogger.FAIL.exceptionAfterComplete(t, serviceName);
                        return;
                    }
                    context.state = ContextState.FAILED;
                    asyncTasks--;
                    ServiceLogger.FAIL.startFailed(startException = new StartException("Failed to start service", t, location, serviceName), serviceName);
                    if (ServiceContainerImpl.PROFILE_OUTPUT != null) {
                        writeProfileInfo('F', startNanos, System.nanoTime());
                    }
                    tasks = transition();
                }
                doExecute(tasks);
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
                Runnable[] tasks = null;
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
                    asyncTasks--;
                    if (ServiceContainerImpl.PROFILE_OUTPUT != null) {
                        writeProfileInfo('X', startNanos, System.nanoTime());
                    }
                    tasks = transition();
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
                final Runnable[] tasks;
                synchronized (ServiceControllerImpl.this) {
                    asyncTasks--;
                    tasks = transition();
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
                final Runnable[] tasks;
                synchronized (ServiceControllerImpl.this) {
                    asyncTasks--;
                    tasks = transition();
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
                final Runnable[] tasks;
                synchronized (ServiceControllerImpl.this) {
                    asyncTasks--;
                    tasks = transition();
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
                final Runnable[] tasks;
                synchronized (ServiceControllerImpl.this) {
                    asyncTasks--;
                    tasks = transition();
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
                        if (dependent != null) dependent.dependencyInstalled();
                    }
                }
                final Runnable[] tasks;
                synchronized (ServiceControllerImpl.this) {
                    asyncTasks--;
                    tasks = transition();
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
                        if (dependent != null) dependent.dependencyUninstalled();
                    }
                }
                final Runnable[] tasks;
                synchronized (ServiceControllerImpl.this) {
                    asyncTasks--;
                    tasks = transition();
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
                final Runnable[] tasks;
                synchronized (ServiceControllerImpl.this) {
                    asyncTasks--;
                    tasks = transition();
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

        public void failed(final StartException reason) throws IllegalStateException {
            final Runnable[] tasks;
            synchronized (ServiceControllerImpl.this) {
                if (state != ContextState.ASYNC) {
                    throw new IllegalStateException(ILLEGAL_CONTROLLER_STATE);
                }
                state = ContextState.FAILED;
                final ServiceName serviceName = getName();
                reason.setServiceName(serviceName);
                ServiceLogger.FAIL.startFailed(reason, serviceName);
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
            final Runnable[] tasks;
            synchronized (ServiceControllerImpl.this) {
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
            final Runnable[] tasks;
            synchronized (ServiceControllerImpl.this) {
                asyncTasks--;
                if (ServiceContainerImpl.PROFILE_OUTPUT != null) {
                    writeProfileInfo('X', startNanos, System.nanoTime());
                }
                tasks = transition();
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
