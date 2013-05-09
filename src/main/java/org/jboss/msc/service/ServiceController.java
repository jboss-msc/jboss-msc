/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jboss.msc.service;

import org.jboss.msc.service.ServiceMode.Demand;
import org.jboss.msc.txn.ServiceContext;
import org.jboss.msc.txn.TaskController;
import org.jboss.msc.txn.Transaction;

/**
 * A controller for a single service instance.
 *
 * @param <S> the service type
 * 
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 * @author <a href="mailto:frainone@redhat.com">Flavia Rainone</a>
 */
final class ServiceController<T> extends TransactionalObject {

    /**
     * The service itself.
     */
    private final Injector<Service<T>> value = new Injector<Service<T>>();
    /**
     * The primary registration of this service.
     */
    private final Registration primaryRegistration;
    /**
     * The alias registrations of this service.
     */
    private final Registration[] aliasRegistrations;
    /**
     * The dependencies of this service.
     */
    private final Dependency<?>[] dependencies;
    /**
     * The controller mode.
     */
    private final ServiceMode mode;
    /**
     * The controller state.
     */
    private volatile State state = null;
    /**
     * Indicates if service is enabled.
     */
    private volatile boolean enabled = true;
    /**
     * The number of dependencies that are not satisfied.
     */
    private int unsatisfiedDependencies;
    /**
     * Indicates if this service is demanded to start. Has precedence over {@link downDemanded}.
     */
    private int upDemandedByCount;
    /**
     * Indicates if this service is demanded to stop.
     */
    private int downDemandedByCount;
    /**
     * The number of dependents that are currently running. The deployment will
     * not execute the {@code stop()} method (and subsequently leave the
     * {@link State#STOPPING} state) until all running dependents (and listeners) are stopped.
     */
    private int runningDependents;
    /**
     * Info enabled only when this service is write locked during a transaction.
     */
    // will be non null iff write locked
    private volatile TransactionalInfo transactionalInfo = null;

    /**
     * When created, the service controller is automatically installed.
     * 
     * @param transaction         the active transaction
     * @param dependencies        the service dependencies
     * @param aliasRegistrations  the alias registrations
     * @param primaryRegistration the primary registration
     */
    ServiceController(final Transaction transaction, final Dependency<?>[] dependencies, final Registration[] aliasRegistrations, final Registration primaryRegistration, final ServiceMode mode) {
        this.dependencies = dependencies;
        this.aliasRegistrations = aliasRegistrations;
        this.primaryRegistration = primaryRegistration;
        state = State.DOWN;
        enabled = false;
        this.mode = mode;
        lockWrite(transaction);
        for (Dependency<?> dependency: dependencies) {
            dependency.setDependent(transaction, transaction, this);
        }
    }

    Registration getPrimaryRegistration() {
        return primaryRegistration;
    }

    Registration[] getAliasRegistrations() {
        return aliasRegistrations;
    }

    Dependency<?>[] getDependencies() {
        return dependencies;
    }

    private static final ServiceName[] NO_NAMES = new ServiceName[0];

    public ServiceName[] getAliases() {
        final Registration[] aliasRegistrations = this.aliasRegistrations;
        final int len = aliasRegistrations.length;
        if (len == 0) {
            return NO_NAMES;
        }
        final ServiceName[] names = new ServiceName[len];
        for (int i = 0; i < len; i++) {
            names[i] = aliasRegistrations[i].getServiceName();
        }
        return names;
    }

    /**
     * Get the service value.
     *
     * @return the service value
     */
    public Injector<Service<T>> getValue() {
        return value;
    }

    /**
     * Remove this service.<p>
     * All dependent services will be automatically stopped as the result of this operation.
     * 
     * @param  transaction the active transaction
     * @return the task that completes removal. Once this task is executed, the service will be at the
     *         {@code REMOVED} state.
     */
    public TaskController<Void> remove(Transaction transaction) {
        lockWrite(transaction);
        synchronized (this) {
            return transactionalInfo.scheduleRemoval(transaction);
        }
    }

    /**
     * Retry a failed service.  Does nothing if the state is not {@link State#FAILED}.
     * 
     * @param transaction the active transaction
     */
    public void retry(Transaction transaction) {
        lockWrite(transaction);
        // TODO
    }

    /**
     * Management operation for disabling a service. As a result, this service will stop if it is {@code UP}.
     */
    void disable(Transaction transaction) {
        lockWrite(transaction);
        synchronized(this) {
            enabled = false;
        }
        transactionalInfo.transition(transaction);
    }

    /**
     * Management operation for enabling a service. The service may start as a result, according to its {@link
     * ServiceMode mode} rules.
     * <p> Services are enabled by default.
     */
    void enable(Transaction transaction) {
        lockWrite(transaction);
        synchronized(this) {
            enabled = false;
        }
        transactionalInfo.transition(transaction);
    }

    /**
     * Completes service installation.
     *
     * @param transaction the active transaction
     */
    void install(Transaction transaction) {
        // TODO check if container is shutting down
        lockWrite(transaction);
        synchronized (this) {
            enabled = true;
        }
        if (mode.shouldDemandDependencies() == Demand.ALWAYS) {
            DemandDependenciesTask.create(transaction, transaction, this);
        }
        primaryRegistration.setController(transaction,  this);
        for (Registration alias: aliasRegistrations) {
            alias.setController(transaction, this);
        }
        transactionalInfo.transition(transaction);
    }

    public synchronized ServiceMode getMode() {
        return this.mode;
    }

    /**
     * Get the current service controller state.
     *
     * @return the current state
     */
    public State getState() {
        return state;
    }

    synchronized boolean isUpDemanded() {
        return upDemandedByCount > 0;
    }

    synchronized boolean isDownDemanded() {
        return downDemandedByCount > 0 && upDemandedByCount == 0;
    }

    void upDemanded(Transaction transaction, ServiceContext context) {
        lockWrite(transaction, context);
        final boolean propagate;
        synchronized (this) {
            if (upDemandedByCount ++ > 0) {
                return;
            }
            propagate = mode.shouldDemandDependencies() == Demand.PROPAGATE;
        }
        if (propagate) {
            DemandDependenciesTask.create(transaction, context, this);
        }
        transition(context);
    }

    void downDemanded(Transaction transaction, ServiceContext context) {
        lockWrite(transaction, context);
        final boolean propagate;
        synchronized (this) {
            if(downDemandedByCount ++ > 0 ) {
                return;
            }
            propagate = upDemandedByCount == 0 && mode.shouldDemandDependencies() == Demand.ALWAYS;
        }
        if (propagate) {
            UndemandDependenciesTask.create(transaction, context, this);
        }
        transition(context);
    }

    void upUndemanded(Transaction transaction, ServiceContext context) {
        lockWrite(transaction, context);
        final boolean propagate;
        synchronized (this) {
            if (-- upDemandedByCount > 0) {
                return;
            }
            propagate = mode.shouldDemandDependencies() == Demand.PROPAGATE || (downDemandedByCount > 0 && mode.shouldDemandDependencies() == Demand.ALWAYS);
        }
        if (propagate) {
            UndemandDependenciesTask.create(transaction, context, this);
        }
        transition(context);
    }

    void downUndemanded(Transaction transaction, ServiceContext context) {
        lockWrite(transaction, context);
        final boolean propagate;
        synchronized (this) {
            if (-- downDemandedByCount > 0) {
                return;
            }
            propagate = upDemandedByCount == 0 && mode.shouldDemandDependencies() == Demand.ALWAYS;
        }
        if (propagate) {
            DemandDependenciesTask.create(transaction, context, this);
        }
        transition(context);
    }

    void dependentStarted(Transaction transaction) {
        lockWrite(transaction);
        synchronized (this) {
            runningDependents++;
        }
    }

    void dependentStopped(Transaction transaction, ServiceContext context) {
        lockWrite(transaction, context);
        synchronized (this) {
            if (--runningDependents == 0) {
                transition(context);
            }
        }
    }

    TaskController<?> dependencySatisfied(Transaction transaction, ServiceContext context) {
        lockWrite(transaction, context);
        synchronized (this) {
            if (-- unsatisfiedDependencies == 0) {
                return transition(context);
            }
        }
        return null;
    }

    TaskController<?> dependencyUnsatisfied(Transaction transaction, ServiceContext context) {
        lockWrite(transaction, context);
        synchronized (this) {
           if (++ unsatisfiedDependencies == 1) {
                return transition(context);
            }
        }
        return null;
    }

    void setTransition(TransactionalState transactionalState, ServiceContext context) {
        assert isWriteLocked();
        transactionalInfo.setTransition(transactionalState, context);
    }

    private TaskController<?> transition(ServiceContext context) {
        assert isWriteLocked();
        return transactionalInfo.transition(context);
    }

    @Override
    protected void writeLocked(Transaction transaction) {
        transactionalInfo = new TransactionalInfo();
    }

    @Override
    protected void writeUnlocked() {
        transactionalInfo = null;
    }

    @Override
    public Object takeSnapshot() {
        return new Snapshot();
    }

    @SuppressWarnings("unchecked")
    @Override
    public void revert(Object snapshot) {
        ((Snapshot) snapshot).apply();
    }

    private final class Snapshot {
        private final State state;
        private final int upDemandedByCount;
        private final int downDemandedByCount;
        private final int unsatisfiedDependencies;
        private final int runningDependents;

        // take snapshot
        public Snapshot() {
            state = ServiceController.this.state;
            upDemandedByCount = ServiceController.this.upDemandedByCount;
            downDemandedByCount = ServiceController.this.downDemandedByCount;
            unsatisfiedDependencies = ServiceController.this.unsatisfiedDependencies;
            runningDependents = ServiceController.this.runningDependents;
        }

        // revert ServiceController state to what it was when snapshot was taken; invoked on rollback
        public void apply() {
            ServiceController.this.state = state;
            ServiceController.this.upDemandedByCount = upDemandedByCount;
            ServiceController.this.downDemandedByCount = downDemandedByCount;
            ServiceController.this.unsatisfiedDependencies = unsatisfiedDependencies;
            ServiceController.this.runningDependents = runningDependents;
        }
    }

    final class TransactionalInfo {
        // current transactional state
        private TransactionalState transactionalState = TransactionalState.getTransactionalState(ServiceController.this.state);
        // if this service is under transition (STARTING or STOPPING), this field points to the task that completes the transition
        private TaskController<Void> completeTransitionTask = null;
        // perform another transition after current transition completes
        private boolean performTransition = false;

        synchronized void setTransition(TransactionalState transactionalState, ServiceContext context) {
            assert isWriteLocked();
            completeTransitionTask = null;
            this.transactionalState = transactionalState;
            state = transactionalState.getState();
            if (performTransition) {
                transition(context);
            }
        }

        private synchronized TaskController<?> transition(ServiceContext context) {
            // TODO keep track of multiple toggle transitions from UP to DOWN and vice-versa... if 
            // too  many transitions of this type are performed, a check for cycle involving this service
            // must be performed. Cycle detected will result in service removal, besides adding a problem to the
            // transaction
            final boolean enabled;
            synchronized (ServiceController.this) {
                enabled = ServiceController.this.enabled;
            }
            final Transaction transaction = getCurrentTransaction();
            switch (transactionalState) {
                case DOWN:
                    if (unsatisfiedDependencies == 0 && mode.shouldStart(ServiceController.this) && enabled) {
                        transactionalState = TransactionalState.STARTING;
                        completeTransitionTask = StartingServiceTasks.createTasks(transaction, context, ServiceController.this);
                        if (mode.shouldDemandDependencies() == Demand.SERVICE_UP) {
                            DemandDependenciesTask.create(transaction, context, ServiceController.this, completeTransitionTask);
                        }
                    }
                    break;
                case STOPPING:
                    // discuss this on IRC channel before proceeding
                    if (unsatisfiedDependencies == 0 && !mode.shouldStop(ServiceController.this) && enabled) {
                        // ongoing transition from UP to DOWN, schedule a transition once service is DOWN
                        scheduleTransition(context);
                    }
                    break;
                case FAILED:
                    if (unsatisfiedDependencies > 0 || mode.shouldStop(ServiceController.this) || !enabled) {
                        transactionalState = TransactionalState.STOPPING;
                        completeTransitionTask = StoppingServiceTasks.createTasks(transaction, context, ServiceController.this);
                    }
                    break;
                case UP:
                    if (unsatisfiedDependencies > 0 || mode.shouldStop(ServiceController.this) || !enabled) {
                        transactionalState = TransactionalState.STOPPING;
                        completeTransitionTask = StoppingServiceTasks.createTasks(transaction, context, ServiceController.this);
                    }
                    break;
                case STARTING:
                    // discuss this on IRC channel before proceeding
                    if (unsatisfiedDependencies > 0 || !mode.shouldStart(ServiceController.this) || !enabled) {
                        // ongoing transition from DOWN to UP, schedule a transition once service is UP
                        scheduleTransition(context);
                    }
                    break;
                default:
                    break;

            }
            return completeTransitionTask;
        }

        private void scheduleTransition(ServiceContext context) {
            assert Thread.holdsLock(this);
            assert completeTransitionTask != null;
            performTransition = true;
        }

        private synchronized TaskController<Void> scheduleRemoval(ServiceContext context) {
            final Transaction transaction = getCurrentTransaction();
            switch (transactionalState) {
                case DOWN:
                    transactionalState = TransactionalState.REMOVING;
                    completeTransitionTask = context.newTask(new ServiceRemoveTask(transaction, ServiceController.this)).release();
                    break;
                case FAILED:
                    // fall thru!
                case UP:
                    transactionalState = TransactionalState.STOPPING;
                    final TaskController<Void> stopServiceTask = StoppingServiceTasks.createTasks(transaction, context, ServiceController.this);
                    completeTransitionTask = context.newTask(new ServiceRemoveTask(transaction, ServiceController.this)).addDependency(stopServiceTask).release();
                    break;
                case STARTING:
                    TaskController<?> downServiceTask = StoppingServiceTasks.createTasks(transaction, context, ServiceController.this, completeTransitionTask);
                    TaskController<Void> removingStateTask = context.newTask(new SetTransactionalStateTask(ServiceController.this, TransactionalState.REMOVING)).addDependency(downServiceTask).release();
                    completeTransitionTask = context.newTask(new ServiceRemoveTask(transaction, ServiceController.this)).addDependency(removingStateTask).release();
                    break;
                case STOPPING:
                    removingStateTask = context.newTask(new SetTransactionalStateTask(ServiceController.this, TransactionalState.REMOVING)).addDependency(completeTransitionTask).release();
                    completeTransitionTask = context.newTask(new ServiceRemoveTask(transaction, ServiceController.this)).addDependency(removingStateTask).release();
                    break;
                default:
                    throw new IllegalStateException("Service should not be at this state: " + transactionalState);
                
            }
            enabled = false;
            return completeTransitionTask;
        }
    }

    /**
     * A possible state for a service controller.
     */
    public enum State {
        /**
         * Up. Satisfied dependencies may not change state without causing this service to stop.
         */
        UP,
        /**
         * Down.  All up dependents are down.
         */
        DOWN,
        /**
         * Start failed, or was cancelled.  From this state, the start may be {@link ServiceController#retry retried} or
         * the service may enter the {@code DOWN} state.
         */
        FAILED,
        /**
         * Removed from the container.
         */
        REMOVED,
        ;
    };

    public enum TransactionalState {
        /**
         * Up. Satisfied dependencies may not change state without causing this service to stop.
         */
        UP(State.UP),
        /**
         * Down.  All up dependents are down.
         */
        DOWN(State.DOWN),
        /**
         * Start failed, or was cancelled.  From this state, the start may be {@link ServiceController#retry retried} or
         * the service may enter the {@code DOWN} state.
         */
        FAILED(State.FAILED),
        /**
         * Removed from the container.
         */
        REMOVED(State.REMOVED),
        /**
         * Service is starting.  Satisfied dependencies may not change state.  This state may not be left until
         * the {@code start} method has finished or failed.
         */
        STARTING(State.DOWN),
        /**
         * Service is stopping.  Up dependents are {@code DOWN} and may not enter the {@code STARTING} state. 
         * This state may not be left until the {@code stop} method has finished.
         */
        STOPPING(State.UP),
        /**
         * Service is {@code DOWN} and being removed.
         */
        REMOVING(State.DOWN),
        ;
        private final State state;

        public State getState() {
            return state;
        }

        public static TransactionalState getTransactionalState(State state) {
            switch(state) {
                case UP:
                    return UP;
                case DOWN:
                    return DOWN;
                case FAILED:
                    return FAILED;
                case REMOVED:
                    return REMOVED;
                default:
                    throw new IllegalArgumentException("Unexpected state");
            }
        }

        private TransactionalState(final State state) {
            this.state = state;
        }
    }

}