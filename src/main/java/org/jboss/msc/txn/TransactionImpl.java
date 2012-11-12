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
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import org.jboss.msc.value.Listener;

import static java.lang.Thread.currentThread;
import static java.util.concurrent.locks.LockSupport.park;
import static java.util.concurrent.locks.LockSupport.unpark;
import static org.jboss.msc.txn.Bits.allAreSet;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
final class TransactionImpl extends Transaction implements TaskDependency {

    private final Object lock = new Object();

    private final long startTime = System.nanoTime();
    private long endTime;
    private final Executor taskExecutor;

    private final List<TaskControllerImpl<?>> dependencylessSubtasks = new ArrayList<TaskControllerImpl<?>>();
    private final Set<TaskControllerImpl<?>> dependentlessSubtasks = new LittleIdentitySet<TaskControllerImpl<?>>();

    private long state;

    private static final long MASK_STATE        = 0x000000000000000fL;
    private static final long MASK_SUBTASKS     = 0x0000000ffffffff0L;
    private static final long ONE_SUBTASK       = 0x0000000000000010L;

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

    private static final long FLAG_SPIN_LOCK = 1L << 29; // single-invocation spin lock; must never block or talk to other threads while holding this lock
    private static final long FLAG_ROLLBACK_REQ = 1L << 30; // set if rollback of the current txn was requested
    private static final long FLAG_PREPARE_REQ = 1L << 31; // set if prepare of the current txn was requested
    private static final long FLAG_COMMIT_REQ = 1L << 32; // set if commit of the current txn was requested

    TransactionImpl(final Executor taskExecutor) {
        this.taskExecutor = taskExecutor;
    }

    private static void invokeTransactionListener(final Listener<Transaction> completionListener) {
        try {
            if (completionListener != null) completionListener.handleEvent(this);
        } catch (Throwable ignored) {}
    }

    public Executor getExecutor() {
        return taskExecutor;
    }

    public long getDuration(TimeUnit unit) {
        synchronized (lock) {
            // todo: query txn state
            long endTime = false ? this.endTime : System.nanoTime();
            return unit.convert(endTime - startTime, TimeUnit.NANOSECONDS);
        }
    }

    public boolean prepare(Listener<Transaction> completionListener) throws TransactionRolledBackException {
        return false;
    }

    public boolean commit(Listener<Transaction> completionListener) throws InvalidTransactionStateException, TransactionRolledBackException {
        return false;
    }

    public final <T> TaskBuilder<T> newTask(Executable<T> task) throws IllegalStateException {
        return new TaskBuilder<T>(this, task);
    }

    public TaskBuilder<Void> newTask() throws IllegalStateException {
        return new TaskBuilder<Void>(this);
    }

    public boolean rollback(Listener<Transaction> completionListener) {
        return false;
    }

    public void waitFor(final Transaction other) throws InterruptedException, DeadlockException {
    }

    public void dependentExecutionFinished() {
    }

    public void dependentValidationFinished() {
    }

    public void dependentRollbackFinished() {
    }

    public void dependentCommitFinished() {
    }
}
