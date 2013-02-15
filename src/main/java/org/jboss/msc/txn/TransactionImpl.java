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

package org.jboss.msc.txn;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;
import org.jboss.msc.Version;
import org.jboss.msc._private.MSCLogger;
import org.jboss.msc.value.Listener;

import static java.lang.Math.min;
import static java.lang.System.arraycopy;
import static java.lang.Thread.currentThread;
import static java.lang.Thread.holdsLock;
import static java.util.Arrays.copyOf;
import static java.util.Arrays.copyOfRange;
import static java.util.concurrent.locks.LockSupport.getBlocker;
import static java.util.concurrent.locks.LockSupport.park;
import static java.util.concurrent.locks.LockSupport.unpark;
import static org.jboss.msc.txn.Bits.allAreClear;
import static org.jboss.msc.txn.Bits.allAreSet;
import static org.jboss.msc.txn.Bits.anyAreSet;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
final class TransactionImpl extends Transaction implements TaskTarget {

    static {
        MSCLogger.ROOT.greeting(Version.getVersionString());
    }

    private static final Listener<Object> NOTHING_LISTENER = new Listener<Object>() {
        public void handleEvent(final Object subject) {
        }
    };

    private static final Thread[] NO_THREADS = new Thread[0];
    private static final Thread[] DEAD = new Thread[0];

    private static final AtomicReferenceFieldUpdater<TransactionImpl, Thread[]> waitersUpdater = AtomicReferenceFieldUpdater.newUpdater(TransactionImpl.class, Thread[].class, "waiters");

    private static final AtomicLong ID_HOLDER = new AtomicLong();

    private static final int FLAG_ROLLBACK_REQ = 1 << 3; // set if rollback of the current txn was requested
    private static final int FLAG_PREPARE_REQ  = 1 << 4; // set if prepare of the current txn was requested
    private static final int FLAG_COMMIT_REQ   = 1 << 5; // set if commit of the current txn was requested

    private static final int FLAG_PREPARE_DONE = 1 << 6;
    private static final int FLAG_COMMIT_DONE = 1 << 7;
    private static final int FLAG_ROLLBACK_DONE = 1 << 8;

    private static final int FLAG_DO_PREPARE_LISTENER = 1 << 9;
    private static final int FLAG_DO_COMMIT_LISTENER = 1 << 10;
    private static final int FLAG_DO_ROLLBACK_LISTENER = 1 << 11;

    private static final int FLAG_SEND_VALIDATE_REQ = 1 << 12;
    private static final int FLAG_SEND_COMMIT_REQ = 1 << 13;
    private static final int FLAG_SEND_ROLLBACK_REQ = 1 << 14;

    private static final int FLAG_WAKE_UP_WAITERS = 1 << 15;

    private static final int FLAG_USER_THREAD = 1 << 31;

    private static final int STATE_ACTIVE           = 0x0; // adding tasks and subtransactions; counts = # added
    private static final int STATE_PREPARING        = 0x1; // preparing all our tasks
    private static final int STATE_PREPARED         = 0x2; // prepare finished, wait for commit/abort decision from user or parent
    private static final int STATE_PREPARE_UNDOING  = 0x3; // prepare failed, undoing all work via abort or rollback
    private static final int STATE_ROLLBACK         = 0x4; // rolling back all our tasks; count = # remaining
    private static final int STATE_COMMITTING       = 0x5; // performing commit actions
    private static final int STATE_ROLLED_BACK      = 0x6; // "dead" state
    private static final int STATE_COMMITTED        = 0x7; // "success" state

    private static final int STATE_MASK = 0x07;

    private static final int PERSISTENT_STATE = STATE_MASK | FLAG_ROLLBACK_REQ | FLAG_PREPARE_REQ | FLAG_COMMIT_REQ;

    private static final int T_NONE = 0;

    private static final int T_ACTIVE_to_PREPARING  = 1;
    private static final int T_ACTIVE_to_ROLLBACK   = 2;

    private static final int T_PREPARING_to_PREPARED        = 3;
    private static final int T_PREPARING_to_PREPARE_UNDOING = 4;

    private static final int T_PREPARED_to_COMMITTING   = 5;
    private static final int T_PREPARED_to_ROLLBACK     = 6;

    private static final int T_PREPARE_UNDOING_to_ROLLBACK = 7;

    private static final int T_ROLLBACK_to_ROLLED_BACK  = 8;

    private static final int T_COMMITTING_to_COMMITTED  = 9;

    private final int id;
    private final long startTime = System.nanoTime();
    private final Executor taskExecutor;
    private final List<TaskControllerImpl<?>> topLevelTasks = new ArrayList<TaskControllerImpl<?>>();
    private final ProblemReport problemReport = new ProblemReport();
    private final TaskParent topParent = new TaskParent() {
        public void childExecutionFinished(TaskChild child, final boolean userThread) {
            doChildExecutionFinished(userThread);
        }

        public void childValidationFinished(TaskChild child, final boolean userThread) {
            doChildValidationFinished(userThread);
        }

        public void childRollbackFinished(TaskChild child, final boolean userThread) {
            doChildRollbackFinished(userThread);
        }

        public void childCommitFinished(TaskChild child, final boolean userThread) {
            doChildCommitFinished(userThread);
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
    private int uncommittedChildren;
    private int unrevertedChildren;

    private Listener<? super Transaction> validationListener;
    private Listener<? super Transaction> commitListener;
    private Listener<? super Transaction> rollbackListener;

    private volatile Thread[] waiters = NO_THREADS;

    private TransactionImpl(final int id, final Executor taskExecutor, final Problem.Severity maxSeverity) {
        this.id = id;
        this.taskExecutor = taskExecutor;
        this.maxSeverity = maxSeverity;
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
     * Annotate an unlikely condition.
     *
     * @param cond the condition
     * @return the condition
     */
    private static boolean unlikely(boolean cond) { return cond; }

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
                    return T_PREPARING_to_PREPARE_UNDOING;
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
            case STATE_PREPARE_UNDOING: {
                // todo
                if (false) {
                    return T_PREPARE_UNDOING_to_ROLLBACK;
                } else {
                    return T_NONE;
                }
            }
            case STATE_ROLLBACK: {
                if (unrevertedChildren == 0) {
                    return T_ROLLBACK_to_ROLLED_BACK;
                } else {
                    return T_NONE;
                }
            }
            case STATE_COMMITTING: {
                if (uncommittedChildren == 0) {
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
                    if (validationListener == null) {
                        state = newState(STATE_PREPARED, state | FLAG_PREPARE_DONE);
                        continue;
                    } else {
                        return newState(STATE_PREPARED, state | FLAG_DO_PREPARE_LISTENER);
                    }
                }
                case T_PREPARING_to_PREPARE_UNDOING: {
                    // todo
                    throw new UnsupportedOperationException();
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
                    if (commitListener == null) {
                        state = newState(STATE_COMMITTED, state | FLAG_COMMIT_DONE | FLAG_WAKE_UP_WAITERS);
                        continue;
                    } else {
                        return newState(STATE_COMMITTED, state | FLAG_DO_COMMIT_LISTENER | FLAG_WAKE_UP_WAITERS);
                    }
                }
                case T_ROLLBACK_to_ROLLED_BACK: {
                    if (rollbackListener == null) {
                        state = newState(STATE_ROLLED_BACK, state | FLAG_ROLLBACK_DONE | FLAG_WAKE_UP_WAITERS);
                        continue;
                    } else {
                        return newState(STATE_ROLLED_BACK, state | FLAG_DO_ROLLBACK_LISTENER | FLAG_WAKE_UP_WAITERS);
                    }
                }
                default: throw new IllegalStateException();
            }
        }
    }

    private void executeTasks(int state) {
        if (allAreSet(state, FLAG_SEND_VALIDATE_REQ)) {
            for (TaskControllerImpl<?> task : topLevelTasks) {
                task.childInitiateValidate(allAreSet(state, FLAG_USER_THREAD));
            }
        }
        if (allAreSet(state, FLAG_SEND_COMMIT_REQ)) {
            for (TaskControllerImpl<?> task : topLevelTasks) {
                task.childInitiateCommit(allAreSet(state, FLAG_USER_THREAD));
            }
        }
        if (allAreSet(state, FLAG_SEND_ROLLBACK_REQ)) {
            for (TaskControllerImpl<?> task : topLevelTasks) {
                task.childInitiateRollback(allAreSet(state, FLAG_USER_THREAD));
            }
        }
        if (allAreSet(state, FLAG_WAKE_UP_WAITERS)) {
            unparkWaiters();
        }

        if (allAreSet(state, FLAG_USER_THREAD)) {
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

    public void prepare(Listener<? super Transaction> completionListener) throws TransactionRolledBackException, InvalidTransactionStateException {
        assert ! holdsLock(this);
        int state;
        synchronized (this) {
            state = this.state | FLAG_USER_THREAD;
            if (stateOf(state) == STATE_ACTIVE) {
                if (allAreSet(state, FLAG_PREPARE_REQ)) {
                    throw new InvalidTransactionStateException("Prepare already called");
                }
                state |= FLAG_PREPARE_REQ;
            } else if (stateIsIn(state, STATE_ROLLBACK, STATE_ROLLED_BACK)) {
                throw new TransactionRolledBackException("Transaction was rolled back");
            } else {
                throw new InvalidTransactionStateException("Wrong transaction state for prepare");
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

    public void commit(Listener<? super Transaction> completionListener) throws InvalidTransactionStateException, TransactionRolledBackException {
        assert ! holdsLock(this);
        int state;
        synchronized (this) {
            state = this.state | FLAG_USER_THREAD;
            if (stateIsIn(state, STATE_ACTIVE, STATE_PREPARING, STATE_PREPARED) && canCommit()) {
                if (allAreSet(state, FLAG_COMMIT_REQ)) {
                    throw new InvalidTransactionStateException("Commit already called");
                }
                state |= FLAG_COMMIT_REQ;
            } else if (stateIsIn(state, STATE_ROLLBACK, STATE_ROLLED_BACK)) {
                throw new TransactionRolledBackException("Transaction was rolled back");
            } else {
                throw new InvalidTransactionStateException("Transaction cannot be committed");
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
                state |= FLAG_ROLLBACK_REQ;
            } else if (stateIsIn(state, STATE_ROLLBACK, STATE_ROLLED_BACK)) {
                return;
            } else {
                throw new InvalidTransactionStateException("Transaction cannot be rolled back");
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

    public boolean canCommit() throws InvalidTransactionStateException {
        return problemReport.getMaxSeverity().compareTo(maxSeverity) <= 0;
    }

    public final <T> TaskBuilder<T> newTask(Executable<T> task) throws IllegalStateException {
        return new TaskBuilder<T>(this, topParent, task);
    }

    public TaskBuilder<Void> newTask() throws IllegalStateException {
        return new TaskBuilder<Void>(this, topParent);
    }

    public void waitFor(final Transaction other) throws InterruptedException, DeadlockException {
        if (other instanceof TransactionImpl) {
            final TransactionImpl otherTxn = (TransactionImpl) other;
            int idx1 = addWaiter();
            int idx2 = otherTxn.addWaiter();
            try {
                int ourState = stateOf(state);
                int otherState = stateOf(otherTxn.state);
                while (! (otherState == STATE_COMMITTED || otherState == STATE_ROLLED_BACK || ourState == STATE_COMMITTED || ourState == STATE_ROLLED_BACK)) {
                    checkDeadlock(0, 0);
                    park(other);
                    if (Thread.interrupted()) {
                        throw new InterruptedException();
                    }
                    ourState = stateOf(state);
                    otherState = stateOf(otherTxn.state);
                }
            } finally {
                removeWaiter(idx1);
                otherTxn.removeWaiter(idx2);
            }
        } else {
            throw new IllegalArgumentException(); // todo i18n
        }
    }

    protected void finalize() {
        try {
            rollback(null);
        } catch (Throwable ignored) {
        } finally {
            try {
                super.finalize();
            } catch (Throwable ignored) {
            }
        }
    }

    /**
     * Check to see (recursively) if the given waiter is deadlocked with this transaction.
     *
     * @param matched the matched transaction ID set
     * @param checked the checked transaction ID set
     * @return the new checked transaction ID set
     * @throws DeadlockException if there is a deadlock
     */
    private long checkDeadlock(long matched, long checked) throws DeadlockException {
        final long bit = 1L << id;
        if (allAreSet(matched, bit)) {
            throw new DeadlockException();
        }
        matched |= bit;
        checked |= bit;
        Thread[] waiters = this.waiters;
        for (Thread thread : waiters) {
            final Object blocker = getBlocker(thread);
            // might be null or something else
            if (blocker instanceof TransactionImpl) {
                final TransactionImpl transaction = (TransactionImpl) blocker;
                final int id = transaction.id;
                if (allAreClear(checked, 1L << id)) {
                    checked = transaction.checkDeadlock(matched, checked);
                }
            }
        }
        return checked;
    }

    static TransactionImpl createTransactionImpl(final Executor taskExecutor, final Problem.Severity maxSeverity) {
        int id;
        long oldVal, newVal;
        long bit;
        do {
            oldVal = ID_HOLDER.get();
            bit = Long.lowestOneBit(~oldVal);
            id = Long.numberOfTrailingZeros(bit);
            if (id == 64) {
                throw new IllegalStateException("Too many transactions");
            }
            newVal = oldVal | bit;
        } while (! ID_HOLDER.compareAndSet(oldVal, newVal));
        return new TransactionImpl(id, taskExecutor, maxSeverity);
    }

    /**
     * Add the current thread as a waiter on this transaction.
     *
     * @return the index of the thread; on removal, the thread's index will be no higher than this
     */
    private int addWaiter() {
        final Thread thread = currentThread();
        Thread[] oldVal, newVal;
        int length;
        do {
            oldVal = waiters;
            if (oldVal == DEAD) {
                return 0;
            }
            length = oldVal.length;
            for (int i = 0; i < length; i++) {
                if (oldVal[i] == thread) {
                    return i;
                }
            }
            newVal = copyOf(oldVal, length + 1);
            newVal[length] = thread;
        } while (! waitersUpdater.compareAndSet(this, oldVal, newVal));
        return length;
    }

    /**
     * Remove the current thread as a waiter on this transaction.
     *
     * @param maxIndex the maximum index the current thread will be found at
     */
    private void removeWaiter(int maxIndex) {
        final Thread thread = currentThread();
        Thread[] oldVal, newVal;
        int length;
        cas: do {
            oldVal = waiters;
            length = oldVal.length;
            if (length == 0) {
                return;
            } else if (length == 1) {
                if (oldVal[0] == thread) {
                    newVal = NO_THREADS;
                } else {
                    return;
                }
            } else if (oldVal[0] == thread) {
                newVal = copyOfRange(oldVal, 1, length);
            } else if (oldVal[length - 1] == thread) {
                newVal = copyOf(oldVal, length - 1);
            } else {
                // we've already checked 0 and length-1
                for (int i = min(length - 2, maxIndex - 1); i > 0; i--) {
                    if (oldVal[i] == thread) {
                        newVal = new Thread[length - 1];
                        arraycopy(oldVal, 0, newVal, 0, i);
                        arraycopy(oldVal, i+1, newVal, i, length - i - 1);
                        continue cas;
                    }
                }
                // not found
                return;
            }
        } while (! waitersUpdater.compareAndSet(this, oldVal, newVal));
    }

    private void unparkWaiters() {
        safeUnpark(waitersUpdater.getAndSet(this, DEAD));
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

    private void doChildCommitFinished(final boolean userThread) {
        assert ! holdsLock(this);
        int state;
        synchronized (this) {
            state = this.state;
            if (userThread) state |= FLAG_USER_THREAD;
            uncommittedChildren--;
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

    private void doChildRollbackFinished(final boolean userThread) {
        assert ! holdsLock(this);
        int state;
        synchronized (this) {
            state = this.state;
            if (userThread) state |= FLAG_USER_THREAD;
            unrevertedChildren--;
            state = transition(state);
            this.state = state & PERSISTENT_STATE;
        }
        executeTasks(state);
    }

    private void doChildAdded(TaskChild child, final boolean userThread) throws InvalidTransactionStateException {
        assert ! holdsLock(this);
        int state;
        synchronized (this) {
            state = this.state;
            if (stateOf(state) != STATE_ACTIVE || anyAreSet(state, FLAG_COMMIT_REQ | FLAG_PREPARE_REQ | FLAG_ROLLBACK_REQ)) {
                throw new InvalidTransactionStateException("Transaction is not active");
            }
            if (userThread) state |= FLAG_USER_THREAD;
            topLevelTasks.add((TaskControllerImpl<?>) child);
            unfinishedChildren++;
            unrevertedChildren++;
            unvalidatedChildren++;
            uncommittedChildren++;
            state = transition(state);
            this.state = state & PERSISTENT_STATE;
        }
        executeTasks(state);
    }

    private static void safeUnpark(final Thread[] threads) {
        if (threads != null) for (Thread thread : threads) {
            unpark(thread);
        }
    }

    private void safeCall(final Listener<? super Transaction> listener) {
        if (listener != null) try {
            listener.handleEvent(this);
        } catch (Throwable ignored) {
            MSCLogger.ROOT.listenerFailed(ignored, listener);
        }
    }

    private static int stateOf(final int val) {
        return val & 0x07;
    }

    private static int newState(int sid, int oldState) {
        return sid & 0x07 | oldState & ~0x07;
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
