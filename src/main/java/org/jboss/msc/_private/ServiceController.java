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

import static java.lang.Thread.holdsLock;
import static org.jboss.msc._private.Bits.allAreSet;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceMode;
import org.jboss.msc.txn.ServiceContext;
import org.jboss.msc.txn.TaskController;
import org.jboss.msc.txn.Transaction;

/**
 * A service controller.
 *
 * @param <S> the service type
 * 
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 * @author <a href="mailto:frainone@redhat.com">Flavia Rainone</a>
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
final class ServiceController<T> extends TransactionalObject {

    // controller states
    static final byte STATE_NEW        = (byte)0b00000000;
    static final byte STATE_DOWN       = (byte)0b00000001;
    static final byte STATE_STARTING   = (byte)0b00000010;
    static final byte STATE_UP         = (byte)0b00000011;
    static final byte STATE_FAILED     = (byte)0b00000100;
    static final byte STATE_STOPPING   = (byte)0b00000101;
    static final byte STATE_REMOVING   = (byte)0b00000110;
    static final byte STATE_REMOVED    = (byte)0b00000111;
    static final byte STATE_MASK       = (byte)0b00000111;
    // controller disposal flags
    static final byte SERVICE_ENABLED  = (byte)0b00001000;
    static final byte REGISTRY_ENABLED = (byte)0b00010000;
    // controller modes
    static final byte MODE_ACTIVE      = (byte)0b00000000;
    static final byte MODE_LAZY        = (byte)0b00100000;
    static final byte MODE_ON_DEMAND   = (byte)0b01000000;
    static final byte MODE_MASK        = (byte)0b01100000;

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
     * The service value, resulting of service start.
     */
    private T value;
    /**
     * The controller state.
     */
    private byte state = STATE_NEW;
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
        setMode(mode);
        this.dependencies = dependencies;
        this.aliasRegistrations = aliasRegistrations;
        this.primaryRegistration = primaryRegistration;
        lockWrite(transaction, context);
        unsatisfiedDependencies = dependencies.length;
        for (AbstractDependency<?> dependency: dependencies) {
            dependency.setDependent(this, transaction, context);
        }
    }

    private void setMode(final ServiceMode mode) {
        if (mode != null) switch (mode) {
            case ACTIVE: { setMode(MODE_ACTIVE); } break;
            case LAZY: { setMode(MODE_LAZY); } break;
            case ON_DEMAND: { setMode(MODE_ON_DEMAND); } break;
            default: throw new UnsupportedOperationException();
        } else {
            // default mode (if not provided) is ACTIVE
            setMode(MODE_ACTIVE);
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
        // if registry is removed, get an exception right away
        registry.newServiceInstalled(this, transaction);
        primaryRegistration.setController(context, transaction,  this);
        for (Registration alias: aliasRegistrations) {
            alias.setController(context, transaction, this);
        }
        boolean demandDependencies;
        synchronized (this) {
            state |= SERVICE_ENABLED;
            transactionalInfo.setState(STATE_DOWN);
            demandDependencies = isMode(MODE_ACTIVE);
        }
        if (demandDependencies) {
            DemandDependenciesTask.create(this, transaction, context);
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
    synchronized byte getState() {
        return (byte)(state & STATE_MASK);
    }

    /**
     * Management operation for disabling a service. As a result, this service will stop if it is {@code UP}.
     */
    void disableService(Transaction transaction) {
        lockWrite(transaction, transaction);
        synchronized(this) {
            if (!isServiceEnabled()) return;
            state &= ~SERVICE_ENABLED;
            if (!isRegistryEnabled()) return;
        }
        transactionalInfo.transition(transaction, transaction);
    }

    /**
     * Management operation for enabling a service. The service may start as a result, according to its {@link
     * ServiceMode mode} rules.
     * <p> Services are enabled by default.
     */
    void enableService(Transaction transaction) {
        lockWrite(transaction, transaction);
        synchronized(this) {
            if (isServiceEnabled()) return;
            state |= SERVICE_ENABLED;
            if (!isRegistryEnabled()) return;
        }
        transactionalInfo.transition(transaction, transaction);
    }

    private boolean isServiceEnabled() {
        assert holdsLock(this);
        return allAreSet(state, SERVICE_ENABLED);
    }

    void disableRegistry(Transaction transaction) {
        lockWrite(transaction, transaction);
        synchronized (this) {
            if (!isRegistryEnabled()) return;
            state &= ~REGISTRY_ENABLED;
            if (!isServiceEnabled()) return;
        }
        transactionalInfo.transition(transaction, transaction);
    }

    void enableRegistry(Transaction transaction) {
        lockWrite(transaction, transaction);
        synchronized (this) {
            if (isRegistryEnabled()) return;
            state |= REGISTRY_ENABLED;
            if (!isServiceEnabled()) return;
        }
        transactionalInfo.transition(transaction, transaction);
    }

    private boolean isRegistryEnabled() {
        assert holdsLock(this);
        return allAreSet(state, REGISTRY_ENABLED);
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
            propagate = !isMode(MODE_ACTIVE);
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
            propagate = !isMode(MODE_ACTIVE);
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
    void setTransition(byte transactionalState, Transaction transaction, ServiceContext context) {
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
        state = (byte) (transactionalInfo.getState() & STATE_MASK | state & ~STATE_MASK);
        transactionalInfo = null;
    }

    @Override
    public synchronized Object takeSnapshot() {
        // if service is new, no need to retrieve snapshot
        if ((state & STATE_MASK) == STATE_NEW) {
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

    final class TransactionalInfo {
        // current transactional state
        private byte transactionalState = ServiceController.this.currentState();
        // if this service is under transition, this field points to the task that completes the transition
        private TaskController<Void> completeTransitionTask = null;
        // the total number of setTransition calls expected until completeTransitionTask is finished
        private int transitionCount;

        synchronized void setTransition(byte transactionalState, Transaction transaction, ServiceContext context) {
            this.transactionalState = transactionalState;
            assert transitionCount > 0;
            // transition has finally come to an end, and calling task equals completeTransitionTask
            if (-- transitionCount == 0) {
                switch(transactionalState) {
                    case STATE_UP:
                        notifyDependencyAvailable(true, transaction, context);
                        break;
                    case STATE_DOWN:
                        notifyDependencyAvailable(false, transaction, context);
                        break;
                    case STATE_REMOVED:
                        for (AbstractDependency<?> dependency: dependencies) {
                            dependency.clearDependent(transaction, context);
                        }
                        break;
                    case STATE_FAILED:
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
            if (transactionalState != STATE_FAILED) {
                return;
            }
            assert completeTransitionTask == null;
            completeTransitionTask = StartingServiceTasks.createTasks(ServiceController.this, Collections.EMPTY_LIST, transaction, transaction);
        }

        private synchronized TaskController<?> transition(Transaction transaction, ServiceContext context) {
            assert !holdsLock(ServiceController.this);
            switch (transactionalState) {
                case STATE_DOWN:
                    if (unsatisfiedDependencies == 0 && shouldStart()) {
                        final Collection<TaskController<?>> dependentTasks = notifyDependencyUnavailable(transaction, context);
                        transactionalState = STATE_STARTING;
                        completeTransitionTask = StartingServiceTasks.createTasks(ServiceController.this, dependentTasks, transaction, context);
                        transitionCount ++;
                    }
                    break;
                case STATE_STOPPING:
                    if (unsatisfiedDependencies == 0 && !shouldStop()) {
                        // ongoing transition from UP to DOWN, transition to UP just once service is DOWN
                        TaskController<?> setStartingState = transaction.newTask(new SetTransactionalStateTask(ServiceController.this, STATE_STARTING, transaction))
                            .addDependency(completeTransitionTask).release();
                        completeTransitionTask = StartingServiceTasks.createTasks(ServiceController.this, setStartingState, transaction, context);
                        transitionCount += 2;
                    }
                    break;
                case STATE_FAILED:
                    if (unsatisfiedDependencies > 0 || shouldStop()) {
                        transactionalState = STATE_STOPPING;
                        completeTransitionTask = StoppingServiceTasks.createTasks(ServiceController.this, transaction, context);
                        transitionCount ++;
                    }
                    break;
                case STATE_UP:
                    if (unsatisfiedDependencies > 0 || shouldStop()) {
                        final Collection<TaskController<?>> dependentTasks = notifyDependencyUnavailable(transaction, context);
                        transactionalState = STATE_STOPPING;
                        completeTransitionTask = StoppingServiceTasks.createTasks(ServiceController.this, dependentTasks, transaction, context);
                        transitionCount ++;
                    }
                    break;
                case STATE_STARTING:
                    if (unsatisfiedDependencies > 0 || !shouldStart()) {
                        // ongoing transition from DOWN to UP, transition to DOWN just once service is UP
                        TaskController<?> setStoppingState = transaction.newTask(new SetTransactionalStateTask(
                                ServiceController.this, STATE_STOPPING, transaction))
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
                state &= ~SERVICE_ENABLED;
            }
            transition(transaction, context);
            completeTransitionTask = context.newTask(new ServiceRemoveTask(ServiceController.this, transaction)).release();
            transitionCount ++;
            return completeTransitionTask;
        }

        private void setState(final byte sid) {
            transactionalState = sid;
        }

        private byte getState() {
            return transactionalState;
        }
    }

    private final class Snapshot {
        private final byte state;
        private final int upDemandedByCount;
        private final int unsatisfiedDependencies;
        private final int runningDependents;

        // take snapshot
        public Snapshot() {
            assert holdsLock(ServiceController.this);
            state = ServiceController.this.state;
            upDemandedByCount = ServiceController.this.upDemandedByCount;
            unsatisfiedDependencies = ServiceController.this.unsatisfiedDependencies;
            runningDependents = ServiceController.this.runningDependents;
        }

        // revert ServiceController state to what it was when snapshot was taken; invoked on rollback
        public void apply() {
            assert holdsLock(ServiceController.this);
            ServiceController.this.state = state;
            ServiceController.this.upDemandedByCount = upDemandedByCount;
            ServiceController.this.unsatisfiedDependencies = unsatisfiedDependencies;
            ServiceController.this.runningDependents = runningDependents;
        }
    }

    private synchronized boolean shouldStart() {
        return isMode(MODE_ACTIVE) || (upDemandedByCount > 0 && allAreSet(state, SERVICE_ENABLED | REGISTRY_ENABLED));
    }

    private synchronized boolean shouldStop() {
        return (isMode(MODE_ON_DEMAND) && upDemandedByCount == 0) || !allAreSet(state, SERVICE_ENABLED | REGISTRY_ENABLED);
    }

    private void setMode(final byte mid) {
        state = (byte) (mid & MODE_MASK | state & ~MODE_MASK);
    }

    private boolean isMode(final byte mode) {
        assert holdsLock(this);
        return allAreSet(mode, state & MODE_MASK);
    }

    private byte currentState() {
        assert holdsLock(this);
        return (byte)(state & STATE_MASK);
    }
}