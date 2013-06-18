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
import static org.jboss.msc._private.Bits.anyAreSet;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

import org.jboss.msc.Version;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceContainer;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceRegistry;
import org.jboss.msc.txn.DeadlockException;
import org.jboss.msc.txn.Executable;
import org.jboss.msc.txn.InvalidTransactionStateException;
import org.jboss.msc.txn.Listener;
import org.jboss.msc.txn.Problem;
import org.jboss.msc.txn.ProblemReport;
import org.jboss.msc.txn.ServiceContext;
import org.jboss.msc.txn.TaskBuilder;
import org.jboss.msc.txn.Transaction;
import org.jboss.msc.txn.TransactionRolledBackException;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
public final class TransactionImpl extends Transaction implements ServiceContext {

    static {
        MSCLogger.ROOT.greeting(Version.getVersionString());
    }

    private static final Listener<Object> NOTHING_LISTENER = new Listener<Object>() {
        public void handleEvent(final Object subject) {
        }
    };

    private static final int FLAG_ROLLBACK_REQ = 1 << 3; // set if rollback of the current txn was requested
    private static final int FLAG_PREPARE_REQ  = 1 << 4; // set if prepare of the current txn was requested
    private static final int FLAG_COMMIT_REQ   = 1 << 5; // set if commit of the current txn was requested

    private static final int FLAG_DO_PREPARE_LISTENER = 1 << 6;
    private static final int FLAG_DO_COMMIT_LISTENER = 1 << 7;
    private static final int FLAG_DO_ROLLBACK_LISTENER = 1 << 8;

    private static final int FLAG_SEND_VALIDATE_REQ = 1 << 9;
    private static final int FLAG_SEND_COMMIT_REQ = 1 << 10;
    private static final int FLAG_SEND_ROLLBACK_REQ = 1 << 11;

    private static final int FLAG_CLEAN_UP = 1 << 12;

    private static final int FLAG_USER_THREAD = 1 << 31;

    private static final int STATE_ACTIVE           = 0x0; // adding tasks and subtransactions; counts = # added
    private static final int STATE_PREPARING        = 0x1; // preparing all our tasks
    private static final int STATE_PREPARED         = 0x2; // prepare finished, wait for commit/abort decision from user or parent
    private static final int STATE_ROLLBACK         = 0x3; // rolling back all our tasks; count = # remaining
    private static final int STATE_COMMITTING       = 0x4; // performing commit actions
    private static final int STATE_ROLLED_BACK      = 0x5; // "dead" state
    private static final int STATE_COMMITTED        = 0x6; // "success" state

    private static final int STATE_MASK = 0x07;

    private static final int PERSISTENT_STATE = STATE_MASK | FLAG_ROLLBACK_REQ | FLAG_PREPARE_REQ | FLAG_COMMIT_REQ;

    private static final int T_NONE = 0;

    private static final int T_ACTIVE_to_PREPARING  = 1;
    private static final int T_ACTIVE_to_ROLLBACK   = 2;

    private static final int T_PREPARING_to_PREPARED        = 3;
    private static final int T_PREPARING_to_ROLLBACK = 4;

    private static final int T_PREPARED_to_COMMITTING   = 5;
    private static final int T_PREPARED_to_ROLLBACK     = 6;

    private static final int T_ROLLBACK_to_ROLLED_BACK  = 7;

    private static final int T_COMMITTING_to_COMMITTED  = 8;

    private final long startTime = System.nanoTime();
    private final Executor taskExecutor;
    private final List<TaskControllerImpl<?>> topLevelTasks = new ArrayList<TaskControllerImpl<?>>();
    private final ProblemReport problemReport = new ProblemReport();
    private final TaskParent topParent = new TaskParent() {
        public void childExecutionFinished(final boolean userThread) {
            doChildExecutionFinished(userThread);
        }

        public void childValidationFinished(final boolean userThread) {
            doChildValidationFinished(userThread);
        }

        public void childTerminated(final boolean userThread) {
            doChildTerminated(userThread);
        }

        public void childAdded(final TaskChild child, final boolean userThread) throws InvalidTransactionStateException {
            doChildAdded(child, userThread);
        }

        public TransactionImpl getTransaction() {
            return TransactionImpl.this;
        }
    };
    private final Problem.Severity maxSeverity;

    private long endTime;
    private int state;
    private int unfinishedChildren;
    private int unvalidatedChildren;
    private int unterminatedChildren;

    private Listener<? super Transaction> validationListener;
    private Listener<? super Transaction> commitListener;
    private Listener<? super Transaction> rollbackListener;

    private volatile boolean isRollbackRequested;

    private TransactionImpl(final Executor taskExecutor, final Problem.Severity maxSeverity) {
        this.taskExecutor = taskExecutor;
        this.maxSeverity = maxSeverity;
    }

    @Override
    public <T> ServiceBuilder<T> addService(ServiceRegistry registry, ServiceName name) {
        if (registry == null) {
            throw new IllegalArgumentException("registry is null");
        }
        if (name == null) {
            throw new IllegalArgumentException("name is null");
        }
        return new ServiceBuilderImpl<T>(registry, name, this);
    }

    @Override
    public void disableService(ServiceRegistry registry, ServiceName name) {
        ((ServiceRegistryImpl) registry).getRequiredServiceController(name).disable(this);
    }

    @Override
    public void enableService(ServiceRegistry registry, ServiceName name) {
        ((ServiceRegistryImpl) registry).getRequiredServiceController(name).enable(this);
    }

    @Override
    public void removeService(ServiceRegistry registry, ServiceName name) {
        if (registry == null) {
            throw new IllegalArgumentException("registry is null");
        }
        if (name == null) {
            throw new IllegalArgumentException("name is null");
        }
        final Registration registration = ((ServiceRegistryImpl) registry).getRegistration(name);
        if (registration == null) {
            return;
        }
        final ServiceController<?> controller = registration.getController();
        if (controller == null) {
            return;
        }
        controller.remove(this);
    }

    public void disableRegistry(final ServiceRegistry registry) {
        ((ServiceRegistryImpl)registry).disable(this);
    }

    public void enableRegistry(final ServiceRegistry registry) {
        ((ServiceRegistryImpl)registry).enable(this);
    }

    public void removeRegistry(final ServiceRegistry registry) {
        ((ServiceRegistryImpl)registry).remove(this);
    }

    public void shutdownContainer(final ServiceContainer container) {
        ((ServiceContainerImpl)container).shutdown(this);
    }

    public Executor getExecutor() {
        return taskExecutor;
    }

    public long getDuration(TimeUnit unit) {
        assert ! holdsLock(this);
        synchronized (this) {
            if (stateIsIn(state, STATE_COMMITTED, STATE_ROLLED_BACK)) {
                return unit.convert(endTime - startTime, TimeUnit.NANOSECONDS);
            } else {
                return unit.convert(System.nanoTime() - startTime, TimeUnit.NANOSECONDS);
            }
        }
    }

    public ProblemReport getProblemReport() {
        return problemReport;
    }

    /**
     * Calculate the transition to take from the current state.
     *
     * @param state the current state
     * @return the transition to take
     */
    private int getTransition(int state) {
        assert holdsLock(this);
        int sid = stateOf(state);
        switch (sid) {
            case STATE_ACTIVE: {
                if (allAreSet(state, FLAG_ROLLBACK_REQ) && unfinishedChildren == 0) {
                    return T_ACTIVE_to_ROLLBACK;
                } else if (anyAreSet(state, FLAG_PREPARE_REQ | FLAG_COMMIT_REQ) && unfinishedChildren == 0) {
                    return T_ACTIVE_to_PREPARING;
                } else {
                    return T_NONE;
                }
            }
            case STATE_PREPARING: {
                if (allAreSet(state, FLAG_ROLLBACK_REQ)) {
                    return T_PREPARING_to_ROLLBACK;
                } else if (unvalidatedChildren == 0) {
                    return T_PREPARING_to_PREPARED;
                } else {
                    return T_NONE;
                }
            }
            case STATE_PREPARED: {
                if (allAreSet(state, FLAG_ROLLBACK_REQ)) {
                    return T_PREPARED_to_ROLLBACK;
                } else if (allAreSet(state, FLAG_COMMIT_REQ)) {
                    return T_PREPARED_to_COMMITTING;
                } else {
                     return T_NONE;
                }
            }
            case STATE_ROLLBACK: {
                if (unterminatedChildren == 0) {
                    return T_ROLLBACK_to_ROLLED_BACK;
                } else {
                    return T_NONE;
                }
            }
            case STATE_COMMITTING: {
                if (unterminatedChildren == 0) {
                    return T_COMMITTING_to_COMMITTED;
                } else {
                    return T_NONE;
                }
            }
            case STATE_ROLLED_BACK: {
                return T_NONE;
            }
            case STATE_COMMITTED: {
                return T_NONE;
            }
            default: {
                throw new IllegalStateException();
            }
        }
    }

    /**
     * Perform any necessary/possible transition.
     *
     * @param state the current state
     * @return the new state
     */
    private int transition(int state) {
        assert holdsLock(this);
        for (;;) {
            int t = getTransition(state);
            switch (t) {
                case T_NONE: return state;
                case T_ACTIVE_to_PREPARING: {
                    state = newState(STATE_PREPARING, state | FLAG_SEND_VALIDATE_REQ);
                    continue;
                }
                case T_ACTIVE_to_ROLLBACK: {
                    state = newState(STATE_ROLLBACK, state | FLAG_SEND_ROLLBACK_REQ);
                    continue;
                }
                case T_PREPARING_to_PREPARED: {
                    state = newState(STATE_PREPARED, state | FLAG_DO_PREPARE_LISTENER);
                    continue;
                }
                case T_PREPARING_to_ROLLBACK: {
                    state = newState(STATE_ROLLBACK, state | FLAG_SEND_ROLLBACK_REQ);
                    continue;
                }
                case T_PREPARED_to_COMMITTING: {
                    state = newState(STATE_COMMITTING, state | FLAG_SEND_COMMIT_REQ);
                    continue;
                }
                case T_PREPARED_to_ROLLBACK: {
                    state = newState(STATE_ROLLBACK, state | FLAG_SEND_ROLLBACK_REQ);
                    continue;
                }
                case T_COMMITTING_to_COMMITTED: {
                    state = newState(STATE_COMMITTED, state | FLAG_DO_COMMIT_LISTENER | FLAG_CLEAN_UP);
                    continue;
                }
                case T_ROLLBACK_to_ROLLED_BACK: {
                    state = newState(STATE_ROLLED_BACK, state | FLAG_DO_ROLLBACK_LISTENER | FLAG_CLEAN_UP);
                    continue;
                }
                default: throw new IllegalStateException();
            }
        }
    }

    private void executeTasks(final int state) {
        final boolean userThread = allAreSet(state, FLAG_USER_THREAD);
        if (allAreSet(state, FLAG_SEND_VALIDATE_REQ)) {
            for (TaskControllerImpl<?> task : topLevelTasks) {
                task.childInitiateValidate(userThread);
            }
        }
        if (allAreSet(state, FLAG_SEND_COMMIT_REQ)) {
            for (TaskControllerImpl<?> task : topLevelTasks) {
                task.childInitiateCommit(userThread);
            }
        }
        if (allAreSet(state, FLAG_SEND_ROLLBACK_REQ)) {
            for (TaskControllerImpl<?> task : topLevelTasks) {
                task.childInitiateRollback(userThread);
            }
        }
        if (allAreSet(state, FLAG_CLEAN_UP)) {
            Transactions.unregister(this);
        }
        if (userThread) {
            if (allAreSet(state, FLAG_DO_COMMIT_LISTENER)) {
                safeExecute(new AsyncTask(FLAG_DO_COMMIT_LISTENER));
            }
            if (allAreSet(state, FLAG_DO_PREPARE_LISTENER)) {
                safeExecute(new AsyncTask(FLAG_DO_PREPARE_LISTENER));
            }
            if (allAreSet(state, FLAG_DO_ROLLBACK_LISTENER)) {
                safeExecute(new AsyncTask(FLAG_DO_ROLLBACK_LISTENER));
            }
        } else {
            if (allAreSet(state, FLAG_DO_COMMIT_LISTENER)) {
                callCommitListener();
            }
            if (allAreSet(state, FLAG_DO_PREPARE_LISTENER)) {
                callValidationListener();
            }
            if (allAreSet(state, FLAG_DO_ROLLBACK_LISTENER)) {
                callRollbackListener();
            }
        }
    }

    private void safeExecute(final Runnable command) {
        try {
            taskExecutor.execute(command);
        } catch (Throwable t) {
            MSCLogger.ROOT.runnableExecuteFailed(t, command);
        }
    }

    public void prepare(final Listener<? super Transaction> completionListener) throws TransactionRolledBackException, InvalidTransactionStateException {
        assert ! holdsLock(this);
        int state;
        synchronized (this) {
            state = this.state | FLAG_USER_THREAD;
            if (stateOf(state) == STATE_ACTIVE) {
                if (allAreSet(state, FLAG_PREPARE_REQ)) {
                    throw new InvalidTransactionStateException("Prepare already called");
                }
                state |= FLAG_PREPARE_REQ;
            } else if (stateIsIn(state, STATE_PREPARING, STATE_PREPARED)) {
                throw new InvalidTransactionStateException("Transaction was prepared");
            } else if (stateIsIn(state, STATE_ROLLBACK, STATE_ROLLED_BACK)) {
                throw new TransactionRolledBackException("Transaction was rolled back");
            } else if (stateIsIn(state, STATE_COMMITTING, STATE_COMMITTED)) {
                throw new InvalidTransactionStateException("Transaction was committed");
            }
            if (completionListener == null) {
                validationListener = NOTHING_LISTENER;
            } else {
                validationListener = completionListener;
            }
            state = transition(state);
            this.state = state & PERSISTENT_STATE;
        }
        executeTasks(state);
    }

    public void commit(final Listener<? super Transaction> completionListener) throws InvalidTransactionStateException, TransactionRolledBackException {
        assert ! holdsLock(this);
        int state;
        synchronized (this) {
            state = this.state | FLAG_USER_THREAD;
            if (stateIsIn(state, STATE_ACTIVE, STATE_PREPARING, STATE_PREPARED) && reportIsCommittable()) {
                if (allAreSet(state, FLAG_COMMIT_REQ)) {
                    throw new InvalidTransactionStateException("Commit already called");
                }
                state |= FLAG_COMMIT_REQ;
            } else if (stateIsIn(state, STATE_ROLLBACK, STATE_ROLLED_BACK)) {
                throw new TransactionRolledBackException("Transaction was rolled back");
            } else if (stateIsIn(state, STATE_COMMITTING, STATE_COMMITTED)) {
                throw new InvalidTransactionStateException("Transaction was committed");
            }
            if (completionListener == null) {
                commitListener = NOTHING_LISTENER;
            } else {
                commitListener = completionListener;
            }
            state = transition(state);
            this.state = state & PERSISTENT_STATE;
        }
        executeTasks(state);
    }

    public void rollback(final Listener<? super Transaction> completionListener) throws InvalidTransactionStateException {
        assert ! holdsLock(this);
        int state;
        synchronized (this) {
            state = this.state | FLAG_USER_THREAD;
            if (stateIsIn(state, STATE_ACTIVE, STATE_PREPARING, STATE_PREPARED)) {
                if (allAreSet(state, FLAG_ROLLBACK_REQ)) {
                    throw new InvalidTransactionStateException("Rollback already called");
                }
                state |= FLAG_ROLLBACK_REQ;
                isRollbackRequested = true;
            } else if (stateIsIn(state, STATE_ROLLBACK, STATE_ROLLED_BACK)) {
                throw new TransactionRolledBackException("Transaction was rolled back");
            } else if (stateIsIn(state, STATE_COMMITTING, STATE_COMMITTED)) {
                throw new InvalidTransactionStateException("Transaction was committed");
            }
            if (completionListener == null) {
                rollbackListener = NOTHING_LISTENER;
            } else {
                rollbackListener = completionListener;
            }
            state = transition(state);
            this.state = state & PERSISTENT_STATE;
        }
        executeTasks(state);
    }

    boolean isRollbackRequested() {
        return isRollbackRequested;
    }

    public boolean canCommit() throws InvalidTransactionStateException {
        assert ! holdsLock(this);
        synchronized (this) {
            if (! stateIsIn(state, STATE_ACTIVE, STATE_PREPARING, STATE_PREPARED)) {
                return false;
            }
        }
        return reportIsCommittable();
    }

    private boolean reportIsCommittable() {
        return problemReport.getMaxSeverity().compareTo(maxSeverity) <= 0;
    }

    public final <T> TaskBuilder<T> newTask(Executable<T> task) throws IllegalStateException {
        return new TaskBuilderImpl<T>(this, topParent, task);
    }

    public TaskBuilder<Void> newTask() throws IllegalStateException {
        return new TaskBuilderImpl<Void>(this, topParent);
    }

    public void waitFor(final Transaction other) throws InterruptedException, DeadlockException {
        if (other instanceof TransactionImpl) {
            Transactions.waitFor(this,  other);
        } else {
            throw new IllegalArgumentException(); // todo i18n
        }
    }

    public boolean isTerminated() {
        assert ! holdsLock(this);
        synchronized (this) {
            return stateIsIn(state, STATE_COMMITTED, STATE_ROLLED_BACK);
        }
    }

    protected void finalize() {
        try {
            if (!isTerminated()) {
                rollback(null);
            }
        } catch (Throwable ignored) {
        } finally {
            try {
                super.finalize();
            } catch (Throwable ignored) {
            }
        }
    }

    public static TransactionImpl createTransactionImpl(final Executor taskExecutor, final Problem.Severity maxSeverity) {
        final TransactionImpl txn = new TransactionImpl(taskExecutor, maxSeverity);
        try {
            Transactions.register(txn);
        } catch (final IllegalStateException e) {
            synchronized (txn) {
                txn.state = STATE_ROLLED_BACK;
            }
            throw e;
        }
        return txn;
    }

    private void doChildExecutionFinished(final boolean userThread) {
        assert ! holdsLock(this);
        int state;
        synchronized (this) {
            state = this.state;
            if (userThread) state |= FLAG_USER_THREAD;
            unfinishedChildren--;
            state = transition(state);
            this.state = state & PERSISTENT_STATE;
        }
        executeTasks(state);
    }

    private void doChildValidationFinished(final boolean userThread) {
        assert ! holdsLock(this);
        int state;
        synchronized (this) {
            state = this.state;
            if (userThread) state |= FLAG_USER_THREAD;
            unvalidatedChildren--;
            state = transition(state);
            this.state = state & PERSISTENT_STATE;
        }
        executeTasks(state);
    }

    private void doChildTerminated(final boolean userThread) {
        assert ! holdsLock(this);
        int state;
        synchronized (this) {
            state = this.state;
            if (userThread) state |= FLAG_USER_THREAD;
            unfinishedChildren--;
            unvalidatedChildren--;
            unterminatedChildren--;
            state = transition(state);
            this.state = state & PERSISTENT_STATE;
        }
        executeTasks(state);
    }

    private void callCommitListener() {
        Listener<? super Transaction> listener;
        synchronized (this) {
            endTime = System.nanoTime();
            listener = commitListener;
            commitListener = null;
        }
        safeCall(listener);
    }

    private void callRollbackListener() {
        Listener<? super Transaction> listener;
        synchronized (this) {
            endTime = System.nanoTime();
            listener = rollbackListener;
            rollbackListener = null;
        }
        safeCall(listener);
    }

    private void callValidationListener() {
        Listener<? super Transaction> listener;
        synchronized (this) {
            listener = validationListener;
            validationListener = null;
        }
        safeCall(listener);
    }

    private void doChildAdded(final TaskChild child, final boolean userThread) throws InvalidTransactionStateException {
        assert ! holdsLock(this);
        int state;
        synchronized (this) {
            state = this.state;
            if (stateOf(state) != STATE_ACTIVE) {
                throw new InvalidTransactionStateException("Transaction is not active");
            }
            if (userThread) state |= FLAG_USER_THREAD;
            topLevelTasks.add((TaskControllerImpl<?>) child);
            unfinishedChildren++;
            unvalidatedChildren++;
            unterminatedChildren++;
            state = transition(state);
            this.state = state & PERSISTENT_STATE;
        }
        executeTasks(state);
    }

    private void safeCall(final Listener<? super Transaction> listener) {
        if (listener != null) try {
            listener.handleEvent(this);
        } catch (Throwable ignored) {
            MSCLogger.ROOT.listenerFailed(ignored, listener);
        }
    }

    private static int stateOf(final int val) {
        return val & STATE_MASK;
    }

    private static int newState(int sid, int oldState) {
        return sid & STATE_MASK | oldState & ~STATE_MASK;
    }

    private static boolean stateIsIn(int state, int sid1, int sid2) {
        final int sid = stateOf(state);
        return sid == sid1 || sid == sid2;
    }

    private static boolean stateIsIn(int state, int sid1, int sid2, int sid3) {
        final int sid = stateOf(state);
        return sid == sid1 || sid == sid2 || sid == sid3;
    }

    class AsyncTask implements Runnable {
        private final int state;

        AsyncTask(final int state) {
            this.state = state;
        }

        public void run() {
            executeTasks(state);
        }
    }
}
