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

import org.jboss.msc.Version;
import org.jboss.msc.value.Value;

import java.util.Arrays;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;

/**
 * The service controller implementation.  Instances of this class follow a strict state table.
 *  <style type="text/css">
 *      table.state {
 *          table-layout: fixed;
 *          border-collapse: collapse;
 *          border-width: 1px;
 *          border-style: solid;
 *      }
 *
 *      table.state th {
 *          border-collapse: collapse;
 *          border-width: 1px;
 *          border-style: solid;
 *      }
 *
 *      table.state td {
 *          border-collapse: collapse;
 *          border-width: 1px;
 *          border-style: solid;
 *      }
 *
 *      table.state li {
 *          
 *      }
 *  </style>
 *  <table class="state">
 *      <thead>
 *          <tr>
 *              <th colspan="1">State: <b>REMOVED</b></th>
 *              <th colspan="2">State: <b>DOWN</b></th>
 *              <th colspan="1">State: <b>STOPPING</b></th>
 *              <th colspan="2">State: <b>STOP_REQUESTED</b></th>
 *              <th colspan="1">State: <b>UP</b></th>
 *              <th colspan="2">State: <b>START_FAILED</b></th>
 *              <th colspan="2">State: <b>STARTING</b></th>
 *              <th colspan="2">State: <b>START_REQUESTED</b></th>
 *          </tr>
 *      </thead>
 *      <tbody>
 *          <tr class="transition">
 *              <td></td>
 *              <td>Go to: <b>START_REQUESTED</b></td>
 *              <td>Go to: <b>REMOVED</b></td>
 *              <td>Go to: <b>DOWN</b></td>
 *              <td>Go to: <b>UP</b></td>
 *              <td>Go to: <b>STOPPING</b></td>
 *              <td>Go to: <b>STOP_REQUESTED</b></td>
 *              <td>Go to: <b>STARTING</b></td>
 *              <td>Go to: <b>DOWN</b></td>
 *              <td>Go to: <b>UP</b></td>
 *              <td>Go to: <b>START_FAILED</b></td>
 *              <td>Go to: <b>STARTING</b></td>
 *              <td>Go to: <b>DOWN</b></td>
 *          </tr>
 *          <tr class="criteria">
 *              <td></td>
 *              <td><ul><li>A = 0</li><li>U > 0</li><li>MODE ≠ NEVER</li><li>MODE ≠ REMOVE</li></ul></td>
 *              <td><ul><li>A = 0</li><li>MODE = REMOVE</li></ul></td>
 *              <td><ul><li>A = 0</li></ul></td>
 *              <td><ul><li>A = 0</li><li>U > 0</li></ul></td>
 *              <td><ul><li>A = 0</li><li>R = 0</li></ul></td>
 *              <td><ul><li>A = 0</li><li>U ≤ 0</li></ul></td>
 *              <td><ul><li>A = 0</li><li>Retry = TRUE</li></ul></td>
 *              <td><ul><li>A = 0</li><li>U ≤ 0</li></ul></td>
 *              <td><ul><li>A = 0</li><li>No exception</li></ul></td>
 *              <td><ul><li>A = 0</li><li>Exception</li></ul></td>
 *              <td><ul><li>A = 0</li><li>U > 0</li></ul></td>
 *              <td><ul><li>A = 0</li><li>U ≤ 0</li></ul></td>
 *          </tr>
 *          <tr class="operations"/>
 *              <td></td>
 *              <td><ul><li>R(parents) + 1 (async task)</li></ul></td>
 *              <td><ul><li>Call listeners (async task)</li><li>Remove dependents</ul></td>
 *              <td><ul><li>Call listeners (async task)</li><li>R(parents) - 1 (async task)</ul></td>
 *              <td><ul><li>U(dependents) + 1 (async task)</il></ul></td>
 *              <td><ul><li>Call listeners, then stop, then uninject (async task)</li></ul></td>
 *              <td><ul><li>U(dependents) + 1 (async task)</li></ul></td>
 *              <td><ul><li>Call listeners, then start (async task)</li></ul></td>
 *              <td><ul><li>Call uninject, then listeners (stopped) (async task)</li><li>R(parents) - 1 (async task)</li></ul></td>
 *              <td><ul><li>Call listeners (async task)</li><li>U(dependents) + 1 (async task)</li></ul></td>
 *              <td><ul><li>Call listeners (async task)</li></ul></td>
 *              <td><ul><li>Call inject, then start (async task)</li><li>Call listeners (async task)</li></ul></td>
 *              <td><ul><li>R(parents) - 1 (async task)</li></ul></td>
 *          </tr>
 *      </tbody>
 *  </table>
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
final class ServiceControllerImpl<S> implements ServiceController<S> {

    private static final String ILLEGAL_CONTROLLER_STATE = "Illegal controller state";
    private static final String SERVICE_REMOVED = "Service has been removed";

    private static final ServiceControllerImpl<?>[] NO_DEPENDENTS = new ServiceControllerImpl[0];

    static {
        ServiceLogger.INSTANCE.greeting(Version.getVersionString());
    }

    /**
     * The service container which contains this instance.
     */
    private final ServiceContainerImpl container;
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
    private final ServiceControllerImpl<?>[] dependencies;
    /**
     * The injections of this service.
     */
    private final ValueInjection<?>[] injections;
    /**
     * The set of registered service listeners.
     */
    private final IdentityHashSet<ServiceListener<? super S>> listeners = new IdentityHashSet<ServiceListener<? super S>>(0);
    /**
     * The set of dependents.
     */
    private final IdentityHashSet<ServiceControllerImpl<?>> dependents = new IdentityHashSet<ServiceControllerImpl<?>>(0);
    /**
     * The service name.
     */
    private final ServiceName serviceName;
    /**
     * The service's aliases, if any.
     */
    private final ServiceName[] serviceAliases;
    /**
     * The start exception.
     */
    private StartException startException;
    /**
     * The controller mode.
     */
    private Mode mode = Mode.NEVER;
    /**
     * The controller state.
     */
    private Substate state = Substate.DOWN;
    /**
     * The number of dependents which place a demand-to-start on this dependency.  If this value is >0, propagate a demand
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
     * (and subsequently leave the {@link State#STOPPING} state) until all running dependents (and listeners) are stopped.
     */
    private int runningDependents;
    /**
     * The number of asynchronous tasks that are currently running.  This includes listeners, start/stop methods,
     * outstanding asynchronous start/stops, and internal tasks.
     */
    private int asyncTasks;

    ServiceControllerImpl(final ServiceContainerImpl container, final Value<? extends Service<? extends S>> serviceValue, final Location location, final ServiceControllerImpl<?>[] dependencies, final ValueInjection<?>[] injections, final ServiceName serviceName, final ServiceName[] serviceAliases) {
        this.container = container;
        this.serviceValue = serviceValue;
        this.location = location;
        this.dependencies = dependencies;
        this.injections = injections;
        this.serviceName = serviceName;
        this.serviceAliases = serviceAliases;
        upperCount = - dependencies.length;
    }

    void initialize() {
        for (ServiceControllerImpl<?> controller : dependencies) {
            controller.addDependent(this);
        }
    }

    public ServiceContainer getServiceContainer() {
        return container;
    }

    public State getState() {
        return state.getState();
    }

    public S getValue() throws IllegalStateException {
        return serviceValue.getValue().getValue();
    }

    public ServiceName getName() {
        return serviceName;
    }

    public ServiceName[] getAliases() {
        final ServiceName[] names = serviceAliases;
        return names.length == 0 ? names : names.clone();
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
                    throw new IllegalArgumentException("Listener " + listener + " already present on controller for " + serviceName);
                }
            }
            asyncTasks++;
        }
        invokeListener(listener, null);
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
            if (state.getState() != State.START_FAILED) {
                return;
            }
            startException = null;
            tasks = transition();
        }
        doExecute(tasks);
    }

    public Mode getMode() {
        synchronized (this) {
            return mode;
        }
    }

    public void setMode(final Mode newMode) {
        assert !lockHeld();
        final Runnable[] tasks;
        Runnable specialTask = null;
        synchronized (this) {
            final Mode oldMode = mode;
            mode = newMode;
            switch (oldMode) {
                case REMOVE: {
                    switch (newMode) {
                        case REMOVE: {
                            return;
                        }
                        default: {
                            throw new IllegalStateException(SERVICE_REMOVED);
                        }
                    }
                }
                case NEVER: {
                    switch (newMode) {
                        case NEVER: {
                            return;
                        }
                        case ON_DEMAND: {
                            if (demandedByCount > 0) {
                                upperCount++;
                            }
                            break;
                        }
                        case AUTOMATIC: {
                            upperCount++;
                            break;
                        }
                        case IMMEDIATE: {
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
                        case ON_DEMAND: {
                            return;
                        }
                        case AUTOMATIC: {
                            if (demandedByCount == 0) {
                                upperCount++;
                            }
                            break;
                        }
                        case IMMEDIATE: {
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
                case AUTOMATIC: {
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
                        case AUTOMATIC: {
                            return;
                        }
                        case IMMEDIATE: {
                            specialTask = new DemandParentsTask();
                            asyncTasks++;
                            break;
                        }
                    }
                    break;
                }
                case IMMEDIATE: {
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
                        case AUTOMATIC: {
                            specialTask = new UndemandParentsTask();
                            asyncTasks++;
                            break;
                        }
                        case IMMEDIATE: {
                            return;
                        }
                    }
                    break;
                }
            }
            tasks = transition();
        }
        doExecute(tasks);
        doExecute(specialTask);
    }

    /**
     * Add a dependent to this controller.
     *
     * @param dependent the dependent to add
     */
    private void addDependent(final ServiceControllerImpl<?> dependent) {
        assert !lockHeld();
        assert !dependent.lockHeld();
        final Substate state;
        synchronized (this) {
            state = this.state;
            if (state != Substate.REMOVED) {
                final boolean result = dependents.add(dependent);
                assert result;
            }
            if (state != Substate.UP) return;
            asyncTasks++;
        }
        if (state == Substate.UP) {
            dependent.dependencyUp();
        }
        final Runnable[] tasks;
        synchronized (this) {
            asyncTasks--;
            tasks = transition();
        }
        doExecute(tasks);
    }

    /**
     * Remove a dependent from this controller.
     *
     * @param dependent the dependent to remove
     */
    private void removeDependent(final ServiceControllerImpl<?> dependent) {
        assert !lockHeld();
        assert !dependent.lockHeld();
        synchronized (this) {
            dependents.remove(dependent);
        }
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
                if (mode == Mode.REMOVE) {
                    return Transition.DOWN_to_REMOVED;
                } else if (mode != Mode.NEVER) {
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
    private Runnable[] transition() {
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
                tasks = getListenerTasks(transition.getAfter().getState());
                break;
            }
            case START_FAILED_to_STARTING: {
                tasks = getListenerTasks(transition.getAfter().getState(), new StartTask(false));
                break;
            }
            case START_FAILED_to_DOWN: {
                startException = null;
                tasks = getListenerTasks(transition.getAfter().getState(), new StopTask(true), new DependentStoppedTask());
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
            case DOWN_to_REMOVED: {
                container.remove(serviceName, ServiceControllerImpl.this);
                for (ServiceName name : serviceAliases) {
                    container.remove(name, ServiceControllerImpl.this);
                }
                tasks = getListenerTasks(transition.getAfter().getState(), new RemoveTask(dependents.toScatteredArray(NO_DEPENDENTS)));
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

    private Runnable[] getListenerTasks(final State newState, final Runnable extraTask1, final Runnable extraTask2) {
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

    private Runnable[] getListenerTasks(final State newState, final Runnable extraTask) {
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

    private Runnable[] getListenerTasks(final State newState) {
        final IdentityHashSet<ServiceListener<? super S>> listeners = this.listeners;
        final int size = listeners.size();
        final Runnable[] tasks = new Runnable[size];
        int i = 0;
        for (ServiceListener<? super S> listener : listeners) {
            tasks[i++] = new ListenerTask(listener, newState);
        }
        return tasks;
    }

    /**
     * Determine whether the lock is currently held.
     *
     * @return {@code true} if the lock is held
     */
    private boolean lockHeld() {
        return Thread.holdsLock(this);
    }

    private void invokeListener(final ServiceListener<? super S> listener, final State state) {
        assert !lockHeld();
        try {
            if (state == null) {
                listener.listenerAdded(this);
            } else switch (state) {
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

    enum ContextState {
        SYNC,
        ASYNC,
        COMPLETE,
        FAILED,
    }

    private void doExecute(final Runnable task) {
        assert ! lockHeld();
        if (task == null) return;
        try {
            container.getExecutor().execute(task);
        } catch (RejectedExecutionException e) {
            task.run();
        }
    }

    private void doExecute(final Runnable... tasks) {
        assert ! lockHeld();
        if (tasks == null) return;
        final Executor executor = container.getExecutor();
        for (Runnable task : tasks) {
            try {
                executor.execute(task);
            } catch (RejectedExecutionException e) {
                task.run();
            }
        }
    }

    private static <T> void doInject(final ValueInjection<T> injection) {
        injection.getTarget().inject(injection.getSource().getValue());
    }

    private void doDemandParents() {
        assert ! lockHeld();
        for (ServiceControllerImpl<?> dependency : dependencies) {
            dependency.addDemand();
        }
    }

    private void doUndemandParents() {
        assert ! lockHeld();
        for (ServiceControllerImpl<?> dependency : dependencies) {
            dependency.removeDemand();
        }
    }

    void addDemand() {
        assert ! lockHeld();
        final Runnable[] tasks;
        final boolean propagate;
        synchronized (this) {
            final int cnt = demandedByCount++;
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
    
    void dependencyUp() {
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

    void dependencyDown() {
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

    private class StartContextImpl implements StartContext {

        private ContextState state = ContextState.SYNC;

        public void failed(final StartException reason) throws IllegalStateException {
            final Runnable[] tasks;
            synchronized (ServiceControllerImpl.this) {
                if (state != ContextState.ASYNC) {
                    throw new IllegalStateException(ILLEGAL_CONTROLLER_STATE);
                }
                state = ContextState.FAILED;
                startException = reason;
                asyncTasks--;
                tasks = transition();
            }
            doExecute(tasks);
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
                    tasks = transition();
                }
            }
            doExecute(tasks);
        }

        public ServiceController<?> getController() {
            return ServiceControllerImpl.this;
        }
    }

    private class StopContextImpl implements StopContext {

        private ContextState state = ContextState.SYNC;

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
                tasks = transition();
            }
            doExecute(tasks);
        }

        public ServiceController<?> getController() {
            return ServiceControllerImpl.this;
        }
    }

    enum Substate {
        DOWN(State.DOWN),
        START_REQUESTED(State.DOWN),
        STARTING(State.STARTING),
        START_FAILED(State.START_FAILED),
        UP(State.UP),
        STOP_REQUESTED(State.UP),
        STOPPING(State.STOPPING),
        REMOVED(State.REMOVED),
        ;
        private final State state;

        Substate(final State state) {
            this.state = state;
        }

        public State getState() {
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
        DOWN_to_REMOVED(Substate.DOWN, Substate.REMOVED),
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

    private class StartTask implements Runnable {

        private final boolean doInjection;

        StartTask(final boolean doInjection) {
            this.doInjection = doInjection;
        }

        public void run() {
            assert !lockHeld();
            final StartContextImpl context = new StartContextImpl();
            try {
                if (doInjection) {
                    final ValueInjection<?>[] injections = ServiceControllerImpl.this.injections;
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
                synchronized (ServiceControllerImpl.this) {
                    if (context.state != ContextState.SYNC) {
                        return;
                    }
                    context.state = ContextState.COMPLETE;
                    asyncTasks--;
                    tasks = transition();
                }
                doExecute(tasks);
            } catch (StartException e) {
                e.setServiceName(serviceName);
                final Runnable[] tasks;
                synchronized (ServiceControllerImpl.this) {
                    final ContextState oldState = context.state;
                    if (oldState != ContextState.SYNC && oldState != ContextState.ASYNC) {
                        ServiceLogger.INSTANCE.exceptionAfterComplete(e, serviceName);
                        return;
                    }
                    context.state = ContextState.FAILED;
                    asyncTasks--;
                    startException = e;
                    tasks = transition();
                }
                doExecute(tasks);
            } catch (Throwable t) {
                final Runnable[] tasks;
                synchronized (ServiceControllerImpl.this) {
                    final ContextState oldState = context.state;
                    if (oldState != ContextState.SYNC && oldState != ContextState.ASYNC) {
                        ServiceLogger.INSTANCE.exceptionAfterComplete(t, serviceName);
                        return;
                    }
                    context.state = ContextState.FAILED;
                    asyncTasks--;
                    startException = new StartException("Failed to start service", t, location, serviceName);
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
            final StopContextImpl context = new StopContextImpl();
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
                synchronized (ServiceControllerImpl.this) {
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
                synchronized (ServiceControllerImpl.this) {
                    asyncTasks--;
                    tasks = transition();
                }
                doExecute(tasks);
            }
        }
    }

    private class ListenerTask implements Runnable {

        private final ServiceListener<? super S> listener;
        private final State state;

        ListenerTask(final ServiceListener<? super S> listener, final State state) {
            this.listener = listener;
            this.state = state;
        }

        public void run() {
            assert !lockHeld();
            invokeListener(listener, state);
        }
    }

    private class DependentStoppedTask implements Runnable {

        public void run() {
            try {
                for (ServiceControllerImpl<?> controller : dependencies) {
                    controller.dependentStopped();
                }
                final Runnable[] tasks;
                synchronized (ServiceControllerImpl.this) {
                    asyncTasks--;
                    tasks = transition();
                }
                doExecute(tasks);
            } catch (Throwable t) {
                ServiceLogger.INSTANCE.internalServiceError(t, serviceName);
            }
        }
    }

    private class DependentStartedTask implements Runnable {

        public void run() {
            try {
                for (ServiceControllerImpl<?> controller : dependencies) {
                    controller.dependentStarted();
                }
                final Runnable[] tasks;
                synchronized (ServiceControllerImpl.this) {
                    asyncTasks--;
                    tasks = transition();
                }
                doExecute(tasks);
            } catch (Throwable t) {
                ServiceLogger.INSTANCE.internalServiceError(t, serviceName);
            }
        }
    }

    private class DependencyStartedTask implements Runnable {

        private final ServiceControllerImpl<?>[] dependents;

        DependencyStartedTask(final ServiceControllerImpl<?>[] dependents) {
            this.dependents = dependents;
        }

        public void run() {
            try {
                for (ServiceControllerImpl<?> dependent : dependents) {
                    if (dependent != null) dependent.dependencyUp();
                }
                final Runnable[] tasks;
                synchronized (ServiceControllerImpl.this) {
                    asyncTasks--;
                    tasks = transition();
                }
                doExecute(tasks);
            } catch (Throwable t) {
                ServiceLogger.INSTANCE.internalServiceError(t, serviceName);
            }
        }
    }

    private class DependencyStoppedTask implements Runnable {

        private final ServiceControllerImpl<?>[] dependents;

        DependencyStoppedTask(final ServiceControllerImpl<?>[] dependents) {
            this.dependents = dependents;
        }

        public void run() {
            try {
                for (ServiceControllerImpl<?> dependent : dependents) {
                    if (dependent != null) dependent.dependencyDown();
                }
                final Runnable[] tasks;
                synchronized (ServiceControllerImpl.this) {
                    asyncTasks--;
                    tasks = transition();
                }
                doExecute(tasks);
            } catch (Throwable t) {
                ServiceLogger.INSTANCE.internalServiceError(t, serviceName);
            }
        }
    }

    private class RemoveTask implements Runnable {
        private final ServiceControllerImpl<?>[] dependents;

        RemoveTask(final ServiceControllerImpl<?>[] dependents) {
            this.dependents = dependents;
        }

        public void run() {
            try {
                assert getMode() == Mode.REMOVE;
                assert getState() == State.REMOVED;
                for (ServiceControllerImpl<?> dependent : dependents) {
                    if (dependent != null) dependent.setMode(Mode.REMOVE);
                }
                for (ServiceControllerImpl<?> dependency : dependencies) {
                    dependency.removeDependent(ServiceControllerImpl.this);
                }
                synchronized (ServiceControllerImpl.this) {
                    Arrays.fill(dependencies, null);
                    asyncTasks--;
                }
            } catch (Throwable t) {
                ServiceLogger.INSTANCE.internalServiceError(t, serviceName);
            }
        }
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
                ServiceLogger.INSTANCE.internalServiceError(t, serviceName);
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
                ServiceLogger.INSTANCE.internalServiceError(t, serviceName);
            }
        }
    }
}
