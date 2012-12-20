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
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicLongFieldUpdater;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;
import org.jboss.msc.value.Listener;

import static java.lang.Math.min;
import static java.lang.System.arraycopy;
import static java.lang.Thread.currentThread;
import static java.util.Arrays.copyOf;
import static java.util.Arrays.copyOfRange;
import static java.util.concurrent.locks.LockSupport.getBlocker;
import static java.util.concurrent.locks.LockSupport.park;
import static java.util.concurrent.locks.LockSupport.unpark;
import static org.jboss.msc.txn.Bits.allAreClear;
import static org.jboss.msc.txn.Bits.allAreSet;
import static org.jboss.msc.txn.Bits.longBitMask;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
final class TransactionImpl extends Transaction implements TaskTarget {

    private static final Listener<Object> NOTHING_LISTENER = new Listener<Object>() {
        public void handleEvent(final Object subject) {
        }
    };

    private static final Thread[] NO_THREADS = new Thread[0];
    private static final Thread[] DEAD = new Thread[0];

    private static final AtomicLong ID_HOLDER = new AtomicLong();

    private static final AtomicReferenceFieldUpdater<TransactionImpl, Listener> validationListenerUpdater = AtomicReferenceFieldUpdater.newUpdater(TransactionImpl.class, Listener.class, "validationListener");
    private static final AtomicReferenceFieldUpdater<TransactionImpl, Listener> commitListenerUpdater = AtomicReferenceFieldUpdater.newUpdater(TransactionImpl.class, Listener.class, "commitListener");
    private static final AtomicReferenceFieldUpdater<TransactionImpl, Listener> rollbackListenerUpdater = AtomicReferenceFieldUpdater.newUpdater(TransactionImpl.class, Listener.class, "rollbackListener");
    private static final AtomicReferenceFieldUpdater<TransactionImpl, Thread[]> waitersUpdater = AtomicReferenceFieldUpdater.newUpdater(TransactionImpl.class, Thread[].class, "waiters");

    private static final AtomicLongFieldUpdater<TransactionImpl> stateUpdater = AtomicLongFieldUpdater.newUpdater(TransactionImpl.class, "state");

    private static final int  COUNT_SHIFT       = 0;
    private static final int  COUNT_BITS        = 30;
    private static final long COUNT_ONE         = 1L << COUNT_SHIFT;
    private static final long COUNT_MAX         = longBitMask(0, COUNT_BITS - 1);
    private static final long COUNT_MASK        = COUNT_MAX << COUNT_SHIFT;

    private static final int  STATE_SHIFT       = COUNT_SHIFT + COUNT_BITS;
    private static final int  STATE_BITS        = 3;
    private static final long STATE_MAX         = longBitMask(0, STATE_BITS - 1);
    private static final long STATE_MASK        = STATE_MAX << STATE_SHIFT;

    private static final int  FLAGS_SHIFT       = STATE_SHIFT + STATE_BITS;
    private static final int  FLAGS_BITS        = 3;

    @SuppressWarnings("unused")
    private static final long LAST_SHIFT        = FLAGS_SHIFT + FLAGS_BITS;

    private static final long FLAG_ROLLBACK_REQ = 1L << FLAGS_SHIFT + 0; // set if rollback of the current txn was requested
    private static final long FLAG_PREPARE_REQ  = 1L << FLAGS_SHIFT + 1; // set if prepare of the current txn was requested
    private static final long FLAG_COMMIT_REQ   = 1L << FLAGS_SHIFT + 2; // set if commit of the current txn was requested

    private static final int ACTIVE                = 0x0; // adding tasks and subtransactions; counts = # added
    private static final int PREPARING             = 0x1; // preparing all our tasks
    private static final int PREPARED              = 0x2; // prepare finished, wait for commit/abort decision from user or parent
    private static final int PREPARE_UNDOING       = 0x3; // prepare failed, undoing all work via abort or rollback
    private static final int ROLLBACK              = 0x4; // rolling back all our tasks; count = # remaining
    private static final int COMMITTING            = 0x5; // performing commit actions
    private static final int ROLLED_BACK           = 0x6; // "dead" state
    private static final int COMMITTED             = 0x7; // "success" state

    private final int id;
    private final long startTime = System.nanoTime();
    private final Executor taskExecutor;
    private final List<TaskControllerImpl<?>> topLevelTasks = new ArrayList<TaskControllerImpl<?>>();
    private final ProblemReport problemReport = new ProblemReport();
    private final TaskParent topParent = new TaskParent() {
        public void childExecutionFinished(TaskChild child) {
            doChildExecutionFinished();
        }

        public void childValidationFinished(TaskChild child) {
            doChildValidationFinished();
        }

        public void childRollbackFinished(TaskChild child) {
            doChildRollbackFinished();
        }

        public void childCommitFinished(TaskChild child) {
            doChildCommitFinished();
        }

        public TransactionImpl getTransaction() {
            return TransactionImpl.this;
        }
    };

    private volatile long endTime;

    /**
     * Threads currently waiting for this transaction *or* for another transaction from this one.
     */
    @SuppressWarnings("unused")
    private volatile Thread[] waiters;
    @SuppressWarnings("unused")
    private volatile long state;
    @SuppressWarnings("unused")
    private volatile Listener<? super Transaction> validationListener;
    @SuppressWarnings("unused")
    private volatile Listener<? super Transaction> commitListener;
    @SuppressWarnings("unused")
    private volatile Listener<? super Transaction> rollbackListener;

    private TransactionImpl(final int id, final Executor taskExecutor) {
        this.id = id;
        this.taskExecutor = taskExecutor;
    }

    public Executor getExecutor() {
        return taskExecutor;
    }

    public long getDuration(TimeUnit unit) {
        if (stateOf(state) == COMMITTED || stateOf(state) == ROLLED_BACK) {
            return unit.convert(endTime - startTime, TimeUnit.NANOSECONDS);
        } else {
            return unit.convert(System.nanoTime() - startTime, TimeUnit.NANOSECONDS);
        }
    }

    public ProblemReport getProblemReport() {
        return problemReport;
    }

    public void prepare(Listener<? super Transaction> completionListener) throws TransactionRolledBackException, InvalidTransactionStateException {
        if (completionListener == null) completionListener = NOTHING_LISTENER;
        validationListenerUpdater.compareAndSet(this, null, completionListener);
        long oldVal, newVal;
        do {
            oldVal = state;
            if (allAreSet(oldVal, FLAG_ROLLBACK_REQ) || stateOf(oldVal) == PREPARE_UNDOING || stateOf(oldVal) == ROLLBACK || stateOf(oldVal) == ROLLED_BACK) {
                throw new TransactionRolledBackException("Transaction was rolled back");
            } else if (allAreSet(oldVal, FLAG_PREPARE_REQ) || stateOf(oldVal) == PREPARING || stateOf(oldVal) == PREPARED) {
                throw new IllegalThreadStateException("Prepare already called");
            } else if (allAreSet(oldVal, FLAG_COMMIT_REQ) || stateOf(oldVal) == COMMITTING || stateOf(oldVal) == COMMITTED) {
                throw new IllegalThreadStateException("Commit already called");
            }
            assert stateOf(oldVal) == ACTIVE;
            if (allAreClear(oldVal, COUNT_MASK)) {
                final int size = topLevelTasks.size();
                newVal = size == 0 ? newState(oldVal | FLAG_PREPARE_REQ, PREPARED, 0) : newState(oldVal | FLAG_PREPARE_REQ, PREPARING, size);
            } else {
                newVal = oldVal | FLAG_PREPARE_REQ;
            }
        } while (! stateUpdater.compareAndSet(this, oldVal, newVal));
        if (stateOf(newVal) == PREPARING) {
            initiatePrepare();
        } else if (stateOf(newVal) == PREPARED) {
            callValidationListener();
        }
    }

    public void commit(Listener<? super Transaction> completionListener) throws InvalidTransactionStateException, TransactionRolledBackException {
        if (completionListener == null) completionListener = NOTHING_LISTENER;
        commitListenerUpdater.compareAndSet(this, null, completionListener);
        long oldVal, newVal;
        do {
            oldVal = state;
            if (allAreSet(oldVal, FLAG_ROLLBACK_REQ) || stateOf(oldVal) == PREPARE_UNDOING || stateOf(oldVal) == ROLLBACK || stateOf(oldVal) == ROLLED_BACK) {
                throw new TransactionRolledBackException("Transaction was rolled back");
            } else if (allAreClear(oldVal, FLAG_PREPARE_REQ) && (stateOf(oldVal) == ACTIVE || stateOf(oldVal) == PREPARING)) {
                throw new IllegalThreadStateException("Prepare not yet called");
            } else if (allAreSet(oldVal, FLAG_COMMIT_REQ) || stateOf(oldVal) == COMMITTING || stateOf(oldVal) == COMMITTED) {
                throw new IllegalThreadStateException("Commit already called");
            }
            assert stateOf(oldVal) == PREPARED || (stateOf(oldVal) == ACTIVE || stateOf(oldVal) == PREPARING) && allAreSet(oldVal, FLAG_PREPARE_REQ);
            if (stateOf(oldVal) == PREPARED) {
                if (allAreClear(oldVal, COUNT_MASK)) {
                    final int size = topLevelTasks.size();
                    newVal = size == 0 ? newState(oldVal | FLAG_COMMIT_REQ, COMMITTED, 0) : newState(oldVal | FLAG_COMMIT_REQ, COMMITTING, size);
                } else {
                    newVal = oldVal | FLAG_COMMIT_REQ;
                }
            } else {
                newVal = oldVal | FLAG_COMMIT_REQ;
            }
        } while (! stateUpdater.compareAndSet(this, oldVal, newVal));
        if (stateOf(newVal) == COMMITTING) {
            initiateCommit();
        } else if (stateOf(newVal) == COMMITTED) {
            callCommitListener();
        }
    }

    public void rollback(final Listener<? super Transaction> completionListener) throws InvalidTransactionStateException {
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
                while (! (otherState == COMMITTED || otherState == ROLLED_BACK || ourState == COMMITTED || ourState == ROLLED_BACK)) {
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

    static TransactionImpl createTransactionImpl(final Executor taskExecutor) {
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
        return new TransactionImpl(id, taskExecutor);
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

    private void doChildExecutionFinished() {
        long oldVal, newVal;
        do {
            oldVal = state;
            if ((oldVal & STATE_MASK) != ACTIVE) {
                // todo log internal state error
                return;
            }
            if ((oldVal & COUNT_MASK) == 1) {
                final int size = topLevelTasks.size();
                if (allAreSet(oldVal, FLAG_ROLLBACK_REQ)) {
                    // we transition to rollback
                    // safe to query collection sizes because their last modifier has just exited
                    newVal = size == 0 ? newState(oldVal, ROLLED_BACK, 0) : newState(oldVal, ROLLBACK, size);
                } else if (allAreSet(oldVal, FLAG_PREPARE_REQ)) {
                    // we transition to prepare
                    // safe to query collection sizes because their last modifier has just exited
                    newVal = size == 0 ? allAreSet(oldVal, FLAG_COMMIT_REQ) ? newState(oldVal, COMMITTED, 0) : newState(oldVal, PREPARED, 0) : newState(oldVal, PREPARING, size);
                } else {
                    newVal = oldVal - COUNT_ONE;
                }
            } else {
                newVal = oldVal - COUNT_ONE;
            }
        } while (! stateUpdater.compareAndSet(this, oldVal, newVal));
        // Use an if/else tree to get good branch prediction (unlike switch)
        // Put most likely case first
        if (stateOf(newVal) == ACTIVE) {
            return;
        } else if (stateOf(newVal) == PREPARING) {
            initiatePrepare();
            return;
        } else if (stateOf(newVal) == PREPARED) {
            callValidationListener();
            return;
        } else if (stateOf(newVal) == COMMITTED) {
            callValidationListener();
            callCommitListener();
            return;
        } else if (stateOf(newVal) == ROLLBACK) {
            initiateRollback();
            return;
        }
    }

    private void doChildValidationFinished() {
        long oldVal, newVal;
        do {
            oldVal = state;
            if ((oldVal & STATE_MASK) != PREPARING) {
                // todo log internal state error
                return;
            }
            if ((oldVal & COUNT_MASK) == 1) {
                final int size = topLevelTasks.size();
                if (allAreSet(oldVal, FLAG_ROLLBACK_REQ)) {
                    // we transition to rollback
                    // safe to query collection sizes because their last modifier has just exited
                    newVal = size == 0 ? newState(oldVal, ROLLED_BACK, 0) : newState(oldVal, ROLLBACK, size);
                } else if (allAreSet(oldVal, FLAG_COMMIT_REQ)) {
                    // we transition to rollback
                    // safe to query collection sizes because their last modifier has just exited
                    newVal = size == 0 ? newState(oldVal, COMMITTED, 0) : newState(oldVal, COMMITTING, size);
                } else {
                    newVal = newState(oldVal, PREPARED, 0);
                }
            } else {
                newVal = oldVal - COUNT_ONE;
            }
        } while (! stateUpdater.compareAndSet(this, oldVal, newVal));
        // Use an if/else tree to get good branch prediction (unlike switch)
        // Put most likely case first
        if (stateOf(newVal) == PREPARING) {
            return;
        } else if (stateOf(newVal) == PREPARED) {
            callValidationListener();
            return;
        } else if (stateOf(newVal) == COMMITTING) {
            callValidationListener();
            initiateCommit();
            return;
        } else if (stateOf(newVal) == ROLLBACK) {
            return;
        } else if (stateOf(newVal) == ROLLED_BACK) {
            callRollbackListener();
            unparkWaiters();
            return;
        } else if (stateOf(newVal) == COMMITTED) {
            callCommitListener();
            unparkWaiters();
            return;
        }
    }

    private void doChildCommitFinished() {
        long oldVal, newVal;
        do {
            oldVal = state;
            if ((oldVal & STATE_MASK) != COMMITTING) {
                // todo log internal state error
                return;
            }
            if ((oldVal & COUNT_MASK) == 1) {
                newVal = newState(oldVal, COMMITTED, 0);
            } else {
                newVal = oldVal - COUNT_ONE;
            }
        } while (! stateUpdater.compareAndSet(this, oldVal, newVal));
        // Use an if/else tree to get good branch prediction (unlike switch)
        // Put most likely case first
        if (stateOf(newVal) == COMMITTING) {
            return;
        } else if (stateOf(newVal) == COMMITTED) {
            callCommitListener();
            unparkWaiters();
            return;
        }
    }

    @SuppressWarnings("unchecked")
    private void callCommitListener() {
        endTime = System.nanoTime();
        safeCall(commitListenerUpdater.getAndSet(this, null));
    }

    @SuppressWarnings("unchecked")
    private void callRollbackListener() {
        endTime = System.nanoTime();
        safeCall(rollbackListenerUpdater.getAndSet(this, null));
    }

    @SuppressWarnings("unchecked")
    private void callValidationListener() {
        safeCall(validationListenerUpdater.getAndSet(this, null));
    }

    private void doChildRollbackFinished() {
        long oldVal, newVal;
        do {
            oldVal = state;
            if ((oldVal & STATE_MASK) != ROLLBACK) {
                // todo log internal state error
                return;
            }
            if ((oldVal & COUNT_MASK) == 1) {
                newVal = newState(oldVal, ROLLED_BACK, 0);
            } else {
                newVal = oldVal - COUNT_ONE;
            }
        } while (! stateUpdater.compareAndSet(this, oldVal, newVal));
        // Use an if/else tree to get good branch prediction (unlike switch)
        // Put most likely case first
        if (stateOf(newVal) == ROLLED_BACK) {
            unparkWaiters();
            callRollbackListener();
            return;
        } else if (stateOf(newVal) == ROLLBACK) {
            return;
        }
    }

    private static void safeUnpark(final Thread[] threads) {
        if (threads != null) for (Thread thread : threads) {
            unpark(thread);
        }
    }

    private void safeCall(final Listener<? super Transaction> listener) {
        if (listener != null) try {
            listener.handleEvent(this);
        } catch (Throwable ignored) {}
    }

    private void initiatePrepare() {
        Iterator<TaskControllerImpl<?>> iterator = topLevelTasks.iterator();
        if (iterator.hasNext()) {
            final TaskControllerImpl<?> controller = iterator.next();
            while (iterator.hasNext()) {
                final TaskControllerImpl<?> execController = iterator.next();
                taskExecutor.execute(new InitiateValidationTask(execController));
            }
            controller.childInitiateValidate();
        }
    }

    private void initiateCommit() {
        Iterator<TaskControllerImpl<?>> iterator = topLevelTasks.iterator();
        if (iterator.hasNext()) {
            final TaskControllerImpl<?> controller = iterator.next();
            while (iterator.hasNext()) {
                final TaskControllerImpl<?> execController = iterator.next();
                taskExecutor.execute(new InitiateCommitTask(execController));
            }
            controller.childInitiateCommit();
        }
    }

    private void initiateRollback() {
        Iterator<TaskControllerImpl<?>> iterator = topLevelTasks.iterator();
        if (iterator.hasNext()) {
            final TaskControllerImpl<?> controller = iterator.next();
            while (iterator.hasNext()) {
                final TaskControllerImpl<?> execController = iterator.next();
                taskExecutor.execute(new InitiateRollbackTask(execController));
            }
            controller.childInitiateRollback();
        }
    }

    private static int stateOf(final long val) {
        return (int) ((val & STATE_MASK) >> STATE_SHIFT);
    }

    private static long newState(long oldVal, long state, int newCount) {
        return oldVal & ~STATE_MASK | state | (long)newCount << COUNT_SHIFT;
    }

    private static class InitiateValidationTask implements Runnable {

        private final TaskControllerImpl<?> controller;

        public InitiateValidationTask(final TaskControllerImpl<?> controller) {
            this.controller = controller;
        }

        public void run() {
            controller.childInitiateValidate();
        }
    }

    private static class InitiateCommitTask implements Runnable {

        private final TaskControllerImpl<?> controller;

        public InitiateCommitTask(final TaskControllerImpl<?> controller) {
            this.controller = controller;
        }

        public void run() {
            controller.childInitiateCommit();
        }
    }

    private static class InitiateRollbackTask implements Runnable {

        private final TaskControllerImpl<?> controller;

        public InitiateRollbackTask(final TaskControllerImpl<?> controller) {
            this.controller = controller;
        }

        public void run() {
            controller.childInitiateRollback();
        }
    }
}
