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

import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import org.jboss.msc.value.Listener;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public abstract class Transaction extends SimpleAttachable implements TaskTarget {

    /**
     * Create a new task transaction.
     *
     * @param executor the executor to use to run tasks
     * @return the transaction
     */
    public static Transaction create(Executor executor) {
        return new TransactionImpl(executor);
    }

    public abstract Executor getExecutor();

    /**
     * Get the duration of the current transaction.
     *
     * @return the duration of the current transaction
     */
    public abstract long getDuration(TimeUnit unit);

    /**
     * Prepare this transaction.  It is an error to prepare a transaction with unreleased tasks.
     * Once this method returns, either {@link #commit(Listener)} or {@link #rollback(Listener)} must be called.
     * After calling this method (regardless of its outcome), the transaction can not be directly modified before termination.
     *
     *
     * @param completionListener the listener to call when the prepare is complete or has failed
     * @return {@code true} if this thread initiated prepare, or {@code false} if prepare was already initiated
     * @throws org.jboss.msc.txn.TransactionRolledBackException if the transaction was previously rolled back
     */
    public abstract boolean prepare(Listener<Transaction> completionListener) throws TransactionRolledBackException;

    /**
     * Commit the work done by {@link #prepare(org.jboss.msc.value.Listener)} and terminate this transaction.
     *
     * @param completionListener the listener to call when the rollback is complete
     * @return {@code true} if this thread initiated commit, or {@code false} if commit was already initiated
     * @throws org.jboss.msc.txn.InvalidTransactionStateException if the transaction has not been prepared
     * @throws org.jboss.msc.txn.TransactionRolledBackException if the transaction was previously rolled back
     */
    public abstract boolean commit(Listener<Transaction> completionListener) throws InvalidTransactionStateException, TransactionRolledBackException;

    /**
     * Add a task to this transaction.
     *
     * @param task the task
     * @return the subtask builder
     * @throws IllegalStateException if the transaction is not open
     */
    public abstract <T> TaskBuilder<T> newTask(Executable<T> task) throws IllegalStateException;

    /**
     * Roll back this transaction, undoing all work executed up until this time.
     *
     * @param completionListener the listener to call when the rollback is complete
     * @return {@code true} if rollback was initiated, or {@code false} if another thread has already committed or rolled back
     */
    public abstract boolean rollback(Listener<Transaction> completionListener);

    /**
     * Indicate that the current operation on this transaction depends on the completion of the given transaction.
     *
     * @param other the other transaction
     * @throws InterruptedException if the wait was interrupted
     * @throws DeadlockException if this wait has caused a deadlock and this task was selected to break it
     */
    public abstract void waitFor(Transaction other) throws InterruptedException, DeadlockException;

    public enum State {
        OPEN,
        ROLLBACK,
        PREPARE,
        PREPARE_COMPLETE,
        PREPARE_FAILED,
        ABORT,
        COMMIT,
        COMPLETE,
        ;
    }
}
