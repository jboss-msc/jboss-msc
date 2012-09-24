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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLongFieldUpdater;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

import static java.lang.Thread.currentThread;
import static java.util.concurrent.locks.LockSupport.park;
import static java.util.concurrent.locks.LockSupport.unpark;
import static org.jboss.msc.txn.Bits.*;
import static org.jboss.msc.txn.Log.log;

/**
 * A task transaction.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class RootTransaction extends Transaction {
    private final long startTime = System.nanoTime();
    private volatile long endTime;
    private final Executor subtaskExecutor;
    private final Transaction parent;

    private final List<TaskController<?>> dependencylessSubtasks = new ArrayList<TaskController<?>>();
    private final Set<Transaction> immediateNestedTransactions = new HashSet<Transaction>();

    private volatile long state;
    private volatile Thread waiter;

    private static final AtomicLongFieldUpdater<RootTransaction> stateUpdater = AtomicLongFieldUpdater.newUpdater(RootTransaction.class, "state");
    private static final AtomicReferenceFieldUpdater<RootTransaction, Thread> waiterUpdater = AtomicReferenceFieldUpdater.newUpdater(RootTransaction.class, Thread.class, "waiter");

    private static final long MASK_STATE        = 0x000000000000000fL;
    private static final long MASK_SUBTASKS     = 0x0000000ffffffff0L;
    private static final long ONE_SUBTASK       = 0x0000000000000010L;
    private static final long MASK_TXNS         = 0xffff000000000000L;
    private static final long ONE_TXN           = 0x0001000000000000L;

    private static final int MAX_SPINS;

    private static final int ACTIVE                = 0x0; // adding tasks and subtransactions; counts = # added
    private static final int ROLLED_BACK           = 0x0; // "dead" state
    private static final int COMMITTED             = 0x0; // "success" state
    private static final int ROLLBACK_WAIT         = 0x0; // waiting for subtransactions to roll back; count = # remaining
    private static final int ROLLBACK              = 0x0; // rolling back all our tasks; count = # remaining
    private static final int PREPARE_CHILD_NOTIFY  = 0x0; // notifying children of intent to prepare
    private static final int PREPARE_WAIT          = 0x0; // waiting for parent transaction to prepare and for our children to be resolved
    private static final int PREPARING             = 0x0; // preparing all our tasks
    private static final int PREPARE_CANCEL        = 0x0; // parent transaction cancelled prepare in mid-flight; undoing via abort/rollback
    private static final int PREPARE_CHILD_WAIT    = 0x0; // preparing subtransactions
    private static final int PREPARED              = 0x0; // prepare finished, wait for commit/abort decision from user or parent
    private static final int PREPARE_UNDOING       = 0x0; // prepare failed, undoing all work via abort or rollback
    private static final int COMMIT_WAIT           = 0x0; // waiting for parent to commit (COMMIT_NESTED_WAIT)
    private static final int COMMITTING            = 0x0; // performing commit actions
    private static final int COMMIT_NESTED_WAIT    = 0x0; // waiting for nested to commit

    private static final int STATE_FLAG_ACTIVE      = 1 << 0; // the state is active
    private static final int STATE_FLAG_ROLLBACK    = 1 << 1; // the state is rolling back or aborting/aborted
    private static final int STATE_FLAG_DEAD        = 1 << 2; // transaction is done

    private static final long FLAG_FAILURES = 1L << 28; // set if a task in this transaction failed
    private static final long FLAG_SPIN_LOCK = 1L << 29; // single-invocation spin lock; must never block or talk to other threads while holding this lock
    private static final long FLAG_ROLLBACK_REQ = 1L << 30; // set if rollback of the current txn was requested
    private static final long FLAG_PREPARE_REQ = 1L << 31; // set if prepare of the current txn was requested
    private static final long FLAG_COMMIT_REQ = 1L << 32; // set if commit of the current txn was requested

    private static final int[] STATE_FLAGS;

    static {
        final int[] stateFlags = new int[16];
        stateFlags[ACTIVE]                  = STATE_FLAG_ACTIVE;
        stateFlags[ROLLED_BACK]             = STATE_FLAG_ROLLBACK | STATE_FLAG_DEAD;
        stateFlags[COMMITTED]               = STATE_FLAG_DEAD;
        stateFlags[ROLLBACK_WAIT]           = STATE_FLAG_ROLLBACK;
        stateFlags[ROLLBACK]                = STATE_FLAG_ROLLBACK;
        stateFlags[PREPARE_CHILD_NOTIFY]    = 0;
        stateFlags[PREPARE_WAIT]            = 0;
        stateFlags[PREPARING]               = 0;
        stateFlags[PREPARE_CANCEL]          = STATE_FLAG_ROLLBACK;
        stateFlags[PREPARE_CHILD_WAIT]      = 0;
        stateFlags[PREPARED]                = 0;
        stateFlags[PREPARE_UNDOING]         = STATE_FLAG_ROLLBACK;
        stateFlags[COMMIT_WAIT]             = 0;
        stateFlags[COMMITTING]              = 0;
        stateFlags[COMMIT_NESTED_WAIT]      = 0;
        STATE_FLAGS = stateFlags;
        final int cpus = Runtime.getRuntime().availableProcessors();
        MAX_SPINS = cpus == 1 ? 1 : 500;
    }

    RootTransaction(final Executor subtaskExecutor) {
        parent = null;
        this.subtaskExecutor = subtaskExecutor;
    }

    RootTransaction(final RootTransaction parent) {
        this.parent = parent;
        subtaskExecutor = parent.subtaskExecutor;
    }

    final boolean compareAndSetState(long expect, long update) {
        return stateUpdater.compareAndSet(this, expect, update);
    }

    private static int stateOf(long val) {
        return (int) (val & MASK_STATE);
    }

    private void invokeTransactionListener(final TxnListener completionListener) {
        try {
            if (completionListener != null) completionListener.handleEvent(this);
        } catch (Throwable ignored) {}
    }

    private static long withStateOf(long val, int state) {
        return (val & ~MASK_STATE) | (long)state;
    }

    // locking methods are divided up to increase odds of inlining.

    private void lockPark() {
        Thread waiter = waiterUpdater.getAndSet(this, currentThread());
        try {
            if (allAreSet(state, FLAG_SPIN_LOCK)) {
                park(this);
            }
        } finally {
            safeUnpark(waiter);
        }
    }

    private void unparkWaiter() {
        safeUnpark(waiterUpdater.getAndSet(this, null));
    }

    private static void safeUnpark(Thread parked) {
        if (parked != null) unpark(parked);
    }

    private long lockAndAddTxn() {
        int cnt = 0;
        long oldVal;
        do {
            if (cnt++ > MAX_SPINS) lockPark();
            checkActive(oldVal = state);
        } while (tryLockAndAddTxn(oldVal));
        return oldVal + ONE_TXN | FLAG_SPIN_LOCK;
    }

    private boolean tryLockAndAddTxn(final long oldVal) {
        return allAreSet(oldVal, FLAG_SPIN_LOCK) || ! compareAndSetState(oldVal, oldVal + ONE_TXN | FLAG_SPIN_LOCK);
    }

    private static void checkActive(final long oldVal) {
        if (allAreClear(STATE_FLAGS[stateOf(oldVal)], STATE_FLAG_ACTIVE)) {
            throw log.notActive();
        }
    }

    private void releaseSpinLock(long oldVal) {
        long newVal = oldVal & ~FLAG_SPIN_LOCK;
        while (! compareAndSetState(oldVal, newVal)) {
            oldVal = state;
            newVal = oldVal & ~FLAG_SPIN_LOCK;
        }
        unparkWaiter();
    }

    <T> TaskBuilder<T> newSubtask(final Executable<T> executable, final Object subtask, final Transaction owner) throws IllegalStateException {
        return null;
    }

    public Executor getExecutor() {
        return subtaskExecutor;
    }

    /**
     * Get the duration of the current transaction.
     *
     * @return the duration of the current transaction
     */
    public long getDuration(TimeUnit unit) {
        // todo: query txn state
        long endTime = false ? this.endTime : System.nanoTime();
        return unit.convert(endTime - startTime, TimeUnit.NANOSECONDS);
    }

    /**
     * Prepare this transaction.  It is an error to prepare a transaction with unreleased tasks.
     * Once this method returns, either {@link #commit(TxnListener)} or {@link #rollback(TxnListener)} must be called.
     * If this method throws an exception, the transaction must {@link #rollback(TxnListener)}.  After calling this method (regardless
     * of its outcome), the transaction can not be modified before termination.
     *
     * @param completionListener the listener to call when the prepare is complete or has failed
     */
    public boolean prepare(TxnListener completionListener) {
    }

    /**
     * Commit the work done by {@link #prepare(TxnListener)} and terminate this transaction.
     *
     * @param completionListener the listener to call when the rollback is complete
     */
    public boolean commit(TxnListener completionListener) {
        // CAS state to COMMIT from OPEN or PREPARE

        // CAS state from COMMIT to COMPLETE
    }
}
