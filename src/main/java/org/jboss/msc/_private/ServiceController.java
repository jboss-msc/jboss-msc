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

package org.jboss.msc._private;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jboss.msc._private.ServiceModeBehavior.Demand;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceMode;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.txn.AttachmentKey;
import org.jboss.msc.txn.Factory;
import org.jboss.msc.txn.Problem;
import org.jboss.msc.txn.Problem.Severity;
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

    private static final byte SERVICE_ENABLED = 1 << 0x00;    // service is {@link ServiceController#enable enabled}
    private static final byte REGISTRY_ENABLED  = 1 << 0x01;  // registry is {@link ServiceController#registryEnabled enabled}

    /**
     * Number defined as the limit of successive transitions in a single transaction before the service reaches the
     * final state. If there is more than such number of transitions, a check for a cycle is performed, thus preventing
     * the service from entering a loop.
     */
    private final int TRANSITION_LIMIT = 8;

    /**
     * The service itself.
     */
    private final Service<T> service;
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
    private final AbstractDependency<?>[] dependencies;
    /**
     * The controller mode.
     */
    private final ServiceModeBehavior mode;
    /**
     * The service value, resulting of service start.
     */
    private T value;
    /**
     * The controller state.
     */
    private State state = State.NEW;
    /**
     * Indicates if service is enabled.
     */
    private byte enabled;
    /**
     * The number of dependencies that are not satisfied.
     */
    private int unsatisfiedDependencies;
    /**
     * Indicates if this service is demanded to start. Has precedence over {@link downDemanded}.
     */
    private int upDemandedByCount;
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
     * Creates the service controller, thus beginning installation.
     * 
     * @param primaryRegistration the primary registration
     * @param aliasRegistrations  the alias registrations
     * @param service             the service itself
     * @param mode                the service mode
     * @param dependencies        the service dependencies
     * @param transaction         the active transaction
     * @param context             the service context
     */
    ServiceController(final Registration primaryRegistration, final Registration[] aliasRegistrations, final Service<T> service,
            final org.jboss.msc.service.ServiceMode mode, final AbstractDependency<?>[] dependencies, final Transaction transaction,
            final ServiceContext context) {
        this.service = service;
        this.mode = ServiceModeBehavior.getInstance(mode);
        this.dependencies = dependencies;
        this.aliasRegistrations = aliasRegistrations;
        this.primaryRegistration = primaryRegistration;
        lockWrite(transaction, context);
        unsatisfiedDependencies = dependencies.length;
        for (AbstractDependency<?> dependency: dependencies) {
            dependency.setDependent(this, transaction, context);
        }
    }

    /**
     * Completes service installation, enabling the service and installing it into registrations.
     *
     * @param transaction the active transaction
     * @param context     the service context
     */
    void install(ServiceRegistryImpl registry, Transaction transaction, ServiceContext context) {
        assert isWriteLocked(transaction);
        if (mode.shouldDemandDependencies() == Demand.ALWAYS) {
            DemandDependenciesTask.create(this, transaction, context);
        }
        primaryRegistration.setController(context, transaction,  this);
        for (Registration alias: aliasRegistrations) {
            alias.setController(context, transaction, this);
        }
        registry.newServiceInstalled(this, transaction);
        synchronized (this) {
            // enable service only now
            enabled = (byte) (enabled | SERVICE_ENABLED);
            state = State.DOWN;
        }
        transactionalInfo.transition(transaction, context);
    }

    /**
     * Gets the primary registration.
     */
    Registration getPrimaryRegistration() {
        return primaryRegistration;
    }

    /**
     * Gets the alias registrations.
     */
    Registration[] getAliasRegistrations() {
        return aliasRegistrations;
    }

    /**
     * Gets the service mode.
     */
    ServiceModeBehavior getMode() {
        return this.mode;
    }

    /**
     * Gets the dependencies.
     */
    AbstractDependency<?>[] getDependencies() {
        return dependencies;
    }

    /**
     * Gets the service.
     */
    Service<T> getService() {
        return service;
    }

    T getValue() {
        return value;
    }

    void setValue(T value) {
        this.value = value;
    }
    
    /**
     * Gets the current service controller state.
     */
    synchronized State getState() {
        return state;
    }

    /**
     * Management operation for disabling a service. As a result, this service will stop if it is {@code UP}.
     */
    void disable(Transaction transaction) {
        lockWrite(transaction, transaction);
        synchronized(this) {
            enabled = (byte) (enabled & ~SERVICE_ENABLED);
            // if service was already disabled, return
            if (enabled != REGISTRY_ENABLED) {
                return;
            }
        }
        transactionalInfo.transition(transaction, transaction);
    }

    /**
     * Management operation for enabling a service. The service may start as a result, according to its {@link
     * ServiceMode mode} rules.
     * <p> Services are enabled by default.
     */
    void enable(Transaction transaction) {
        lockWrite(transaction, transaction);
        synchronized(this) {
            final byte oldEnabled = enabled;
            enabled = (byte) (enabled | SERVICE_ENABLED);
            // if service has not been enabled, return
            if (oldEnabled != REGISTRY_ENABLED) {
                return;
            }
        }
        transactionalInfo.transition(transaction, transaction);
    }

    void registryDisabled(Transaction transaction) {
        lockWrite(transaction, transaction);
        synchronized (this) {
            enabled = (byte) (enabled & ~REGISTRY_ENABLED);
            // if service was already disabled, return
            if (enabled != SERVICE_ENABLED) {
                return;
            }
        }
        transactionalInfo.transition(transaction, transaction);
    }

    void registryEnabled(Transaction transaction) {
        lockWrite(transaction, transaction);
        synchronized (this) {
            final byte oldEnabled = enabled;
            enabled = (byte) (enabled | REGISTRY_ENABLED);
            // if service has not been enabled, return
            if (oldEnabled != SERVICE_ENABLED) {
                return;
            }
        }
        transactionalInfo.transition(transaction, transaction);
    }

    private boolean isEnabled() {
        // service is only enabled when both flags are set
        return Bits.allAreSet(enabled, SERVICE_ENABLED | REGISTRY_ENABLED);
    }

    /**
     * Retries a failed service.  Does nothing if the state is not {@link State#FAILED}.
     * 
     * @param transaction the active transaction
     */
    void retry(Transaction transaction) {
        lockWrite(transaction, transaction);
        transactionalInfo.retry(transaction);
    }

    /**
     * Removes this service.<p>
     * All dependent services will be automatically stopped as the result of this operation.
     * 
     * @param  transaction the active transaction
     * @return the task that completes removal. Once this task is executed, the service will be at the
     *         {@code REMOVED} state.
     */
    TaskController<Void> remove(Transaction transaction) {
        lockWrite(transaction, transaction);
        return transactionalInfo.scheduleRemoval(transaction, transaction);
    }

    /**
     * Notifies this service that it is up demanded (demanded to be UP) by one of its incoming dependencies.
     * 
     * @param transaction the active transaction
     * @param context     the service context
     */
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
            DemandDependenciesTask.create(this, transaction, context);
        }
        transition(transaction, context);
    }

    /**
     * Notifies this service that it is no longer up demanded by one of its incoming dependencies (invoked when incoming
     * dependency is being disabled or removed).
     * 
     * @param transaction the active transaction
     * @param context     the service context
     */
    void upUndemanded(Transaction transaction, ServiceContext context) {
        lockWrite(transaction, context);
        final boolean propagate;
        synchronized (this) {
            if (-- upDemandedByCount > 0) {
                return;
            }
            propagate = mode.shouldDemandDependencies() == Demand.PROPAGATE;
        }
        if (propagate) {
            UndemandDependenciesTask.create(this, transaction, context);
        }
        transition(transaction, context);
    }

    /**
     * Indicates if this service is demanded to start by one or more of its incoming dependencies.
     * @return
     */
    synchronized boolean isUpDemanded() {
        return upDemandedByCount > 0;
    }

    /**
     * Notifies that a incoming dependency has started.
     * 
     * @param transaction the active transaction
     * @param context     the service context
     */
    void dependentStarted(Transaction transaction, ServiceContext context) {
        lockWrite(transaction, context);
        synchronized (this) {
            runningDependents++;
        }
    }

    /**
     * Notifies that a incoming dependency has stopped.
     * 
     * @param transaction the active transaction
     * @param context     the service context
     */
    void dependentStopped(Transaction transaction, ServiceContext context) {
        lockWrite(transaction, context);
        synchronized (this) {
            if (--runningDependents > 0) {
                return;
            }
        }
        transition(transaction, context);
    }

    /**
     * Notifies that one of the dependencies of this service is satisfied (during installation, all dependencies are
     * considered unsatisfied until a dependencySatisfied notification is received for each dependency).
     * 
     * @param transaction the active transaction
     * @param context     the service context
     * @return the transition task resulting of this notification, if any
     */
    TaskController<?> dependencySatisfied(Transaction transaction, ServiceContext context) {
        lockWrite(transaction, context);
        synchronized (this) {
            if (-- unsatisfiedDependencies > 0) {
                return null;
            }
        }
        return transition(transaction, context);
    }


    /**
     * Notifies that one of the dependencies of this service is no longer satisfied.
     * 
     * @param transaction the active transaction
     * @param context     the service context
     * @return the transition task resulting of this notification, if any
     */
    TaskController<?> dependencyUnsatisfied(Transaction transaction, ServiceContext context) {
        lockWrite(transaction, context);
        synchronized (this) {
           if (++ unsatisfiedDependencies > 1) {
               return null;
            }
        }
        return transition(transaction, context);
    }

    /**
     * Sets the new transactional state of this service.
     * 
     * @param transactionalState the transactional state
     * @param context            the service context
     */
    void setTransition(TransactionalState transactionalState, Transaction transaction, ServiceContext context) {
        assert isWriteLocked();
        transactionalInfo.setTransition(transactionalState, transaction, context);
    }

    private TaskController<?> transition(Transaction transaction, ServiceContext context) {
        assert isWriteLocked();
        return transactionalInfo.transition(transaction, context);
    }

    @Override
    protected synchronized void writeLocked(Transaction transaction) {
        transactionalInfo = new TransactionalInfo();
    }

    @Override
    protected synchronized void writeUnlocked() {
        transactionalInfo = null;
    }

    @Override
    public synchronized Object takeSnapshot() {
        // no need to take snapshot of newly created objects
        if (state == State.NEW) {
            return null;
        }
        return new Snapshot();
    }

    @SuppressWarnings("unchecked")
    @Override
    public synchronized void revert(Object snapshot) {
        if (snapshot != null) {
            ((Snapshot) snapshot).apply();
        }
    }

    /**
     * A set containing all service controllers involved in dependency cycles that have been found during current
     * transaction.
     * This set avoids duplicate cycle detection and, most importantly, duplicate problem reports for the same cycle.
     */
    private static final AttachmentKey<HashSet<ServiceController<?>>> dependencyCycles = AttachmentKey.create(
            new Factory<HashSet<ServiceController<?>>>() {

                @Override
                public HashSet<ServiceController<?>> create() {
                    return new HashSet<ServiceController<?>>();
                }
        
    });


    final class TransactionalInfo {
        // current transactional state
        private TransactionalState transactionalState = TransactionalState.getTransactionalState(ServiceController.this.state);
        // if this service is under transition, this field points to the task that completes the transition
        private TaskController<Void> completeTransitionTask = null;
        // the total number of setTransition calls expected until completeTransitionTask is finished
        private int transitionCount;

        synchronized void setTransition(TransactionalState transactionalState, Transaction transaction, ServiceContext context) {
            this.transactionalState = transactionalState;
            state = transactionalState.getState();
            assert transitionCount > 0;
            // transition has finally come to an end, and calling task equals completeTransitionTask
            if (-- transitionCount == 0) {
                switch(transactionalState) {
                    case UP:
                        notifyDependencyAvailable(true, transaction, context);
                        break;
                    case DOWN:
                        notifyDependencyAvailable(false, transaction, context);
                        break;
                    case REMOVED:
                        for (AbstractDependency<?> dependency: dependencies) {
                            dependency.clearDependent(transaction, context);
                        }
                        break;
                    case FAILED:
                        // ok
                        break;
                    default:
                        throw new IllegalStateException("Illegal state for finishing transition: " + transactionalState);
                }
                completeTransitionTask = null;
            }
        }

        @SuppressWarnings("unchecked")
        private synchronized void retry(Transaction transaction) {
            if (transactionalState != TransactionalState.FAILED) {
                return;
            }
            assert completeTransitionTask == null;
            completeTransitionTask = StartingServiceTasks.createTasks(ServiceController.this, Collections.EMPTY_LIST, transaction, transaction);
        }

        private synchronized TaskController<?> transition(Transaction transaction, ServiceContext context) {
            assert !Thread.holdsLock(ServiceController.this);
            // keep track of multiple toggle transitions from UP to DOWN and vice-versa... if 
            // too  many transitions of this type are performed, a check for cycle involving this service
            // must be performed. Cycle detected will result in service removal, besides adding a problem to the
            // transaction
            if (transitionCount >= TRANSITION_LIMIT && cycleFound(transaction)) {
                return null;
            }
            // keep track of multiple toggle transitions from UP to DOWN and vice-versa... if 
            // too  many transitions of this type are performed, a check for cycle involving this service
            // must be performed. Cycle detected will result in service removal, besides adding a problem to the
            // transaction
            final boolean enabled;
            synchronized (ServiceController.this) {
                enabled = isEnabled();
            }
            switch (transactionalState) {
                case DOWN:
                    if (unsatisfiedDependencies == 0 && mode.shouldStart(ServiceController.this) && enabled) {
                        final Collection<TaskController<?>> dependentTasks = notifyDependencyUnavailable(transaction, context);
                        transactionalState = TransactionalState.STARTING;
                        completeTransitionTask = StartingServiceTasks.createTasks(ServiceController.this, dependentTasks, transaction, context);
                        transitionCount ++;
                    }
                    break;
                case STOPPING:
                    if (unsatisfiedDependencies == 0 && !mode.shouldStop(ServiceController.this) && enabled) {
                        // ongoing transition from UP to DOWN, transition to UP just once service is DOWN
                        TaskController<?> setStartingState = transaction.newTask(new SetTransactionalStateTask(ServiceController.this, TransactionalState.STARTING, transaction))
                            .addDependency(completeTransitionTask).release();
                        completeTransitionTask = StartingServiceTasks.createTasks(ServiceController.this, setStartingState, transaction, context);
                        transitionCount += 2;
                    }
                    break;
                case FAILED:
                    if (unsatisfiedDependencies > 0 || mode.shouldStop(ServiceController.this) || !enabled) {
                        transactionalState = TransactionalState.STOPPING;
                        completeTransitionTask = StoppingServiceTasks.createTasks(ServiceController.this, transaction, context);
                        transitionCount ++;
                    }
                    break;
                case UP:
                    if (unsatisfiedDependencies > 0 || mode.shouldStop(ServiceController.this) || !enabled) {
                        final Collection<TaskController<?>> dependentTasks = notifyDependencyUnavailable(transaction, context);
                        transactionalState = TransactionalState.STOPPING;
                        completeTransitionTask = StoppingServiceTasks.createTasks(ServiceController.this, dependentTasks, transaction, context);
                        transitionCount ++;
                    }
                    break;
                case STARTING:
                    if (unsatisfiedDependencies > 0 || !mode.shouldStart(ServiceController.this) || !enabled) {
                        // ongoing transition from DOWN to UP, transition to DOWN just once service is UP
                        TaskController<?> setStoppingState = transaction.newTask(new SetTransactionalStateTask(
                                ServiceController.this, TransactionalState.STOPPING, transaction))
                                .addDependency(completeTransitionTask).release();
                        completeTransitionTask = StoppingServiceTasks.createTasks(ServiceController.this, setStoppingState, transaction, context);
                        transitionCount +=2;
                    }
                    break;
                default:
                    break;

            }
            return completeTransitionTask;
        }

        private boolean cycleFound(Transaction transaction) {
            final Set<ServiceController<?>> dependencyCyclesSet = transaction.getAttachment(dependencyCycles);
            if (dependencyCyclesSet.contains(ServiceController.this)) {
                return true;
            }
            final Deque<ServiceName> depPath = new ArrayDeque<ServiceName>();
            depPath.add(primaryRegistration.getServiceName());
            if (checkCycle(ServiceController.this, depPath, dependencyCyclesSet)) {
                transaction.getProblemReport().addProblem(new Problem(completeTransitionTask,
                        MSCLogger.SERVICE.dependencyCycle(depPath.toArray(new ServiceName[depPath.size()])), Severity.ERROR));
                transaction.rollback(null);
                return true;
            }
            return false;
        }

        private boolean checkCycle(ServiceController<?> serviceController, Deque<ServiceName> path, Set<ServiceController<?>> cycleFound) {
            for (AbstractDependency<?> dependency: serviceController.dependencies) {
                final Registration dependencyRegistration = dependency.getDependencyRegistration();
                path.add(dependencyRegistration.getServiceName());
                final ServiceController<?> dependencyController = dependencyRegistration.getController();
                if (dependencyController != null) {
                    if (dependencyController == ServiceController.this || checkCycle(dependencyController, path, cycleFound)) {
                        cycleFound.add(dependencyController);
                        return true;
                    }
                    path.removeLast();
                }
            }
            return false;
        }

        private void notifyDependencyAvailable(boolean up, Transaction transaction, ServiceContext context) {
            notifyDependencyAvailable(up, primaryRegistration, transaction, context);
            for (Registration registration: aliasRegistrations) {
                notifyDependencyAvailable(up, registration, transaction, context);
            }
        }

        private void notifyDependencyAvailable (boolean up, Registration serviceRegistration, Transaction transaction, ServiceContext context) {
            for (AbstractDependency<?> incomingDependency : serviceRegistration.getIncomingDependencies()) {
                incomingDependency.dependencyAvailable(up, transaction, context);
            }
        }

        private Collection<TaskController<?>> notifyDependencyUnavailable(Transaction transaction, ServiceContext context) {
            final List<TaskController<?>> tasks = new ArrayList<TaskController<?>>();
            notifyDependencyUnavailable(primaryRegistration, tasks, transaction, context);
            for (Registration registration: aliasRegistrations) {
                notifyDependencyUnavailable(registration, tasks, transaction, context);
            }
            return tasks;
        }

        private void notifyDependencyUnavailable (Registration serviceRegistration, List<TaskController<?>> tasks, Transaction transaction, ServiceContext context) {
            for (AbstractDependency<?> incomingDependency : serviceRegistration.getIncomingDependencies()) {
                TaskController<?> task = incomingDependency.dependencyUnavailable(transaction, context);
                if (task != null) {
                    tasks.add(task);
                }
            }
        }

        private synchronized TaskController<Void> scheduleRemoval(Transaction transaction, ServiceContext context) {
            synchronized (ServiceController.this) {
                enabled = (byte) (enabled & ~SERVICE_ENABLED);
            }
            transition(transaction, context);
            completeTransitionTask = context.newTask(new ServiceRemoveTask(ServiceController.this, transaction)).release();
            transitionCount ++;
            return completeTransitionTask;
        }

    }

    private final class Snapshot {
        private final State state;
        private final int upDemandedByCount;
        private final int unsatisfiedDependencies;
        private final int runningDependents;

        // take snapshot
        public Snapshot() {
            assert Thread.holdsLock(ServiceController.this);
            state = ServiceController.this.state;
            upDemandedByCount = ServiceController.this.upDemandedByCount;
            unsatisfiedDependencies = ServiceController.this.unsatisfiedDependencies;
            runningDependents = ServiceController.this.runningDependents;
        }

        // revert ServiceController state to what it was when snapshot was taken; invoked on rollback
        public void apply() {
            assert Thread.holdsLock(ServiceController.this);
            ServiceController.this.state = state;
            ServiceController.this.upDemandedByCount = upDemandedByCount;
            ServiceController.this.unsatisfiedDependencies = unsatisfiedDependencies;
            ServiceController.this.runningDependents = runningDependents;
        }
    }

    /**
     * A possible state for a service controller.
     */
    public enum State {
        /**
         * New. Service is being installed.
         */
        NEW,
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
                case NEW:
                    // fall thru!
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