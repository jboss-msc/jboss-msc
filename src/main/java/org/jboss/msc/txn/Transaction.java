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

import static org.jboss.msc._private.MSCLogger.TXN;

import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

import org.jboss.msc._private.TransactionImpl;

/**
 * A transaction.
 * 
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
public abstract class Transaction extends SimpleAttachable implements Attachable, TaskFactory {

    /**
     * Create a new task transaction.
     *
     * @param executor the executor to use to run tasks
     * @return the transaction
     */
    public static Transaction create(final Executor executor) {
        return TransactionImpl.createTransactionImpl(executor, Problem.Severity.WARNING);
    }

    /**
     * Create a new task transaction.
     *
     * @param executor the executor to use to run tasks
     * @param maxSeverity the maximum severity to allow
     * @return the transaction
     */
    public static Transaction create(final Executor executor, final Problem.Severity maxSeverity) {
        if (executor == null) {
            throw TXN.methodParameterIsNull("executor");
        }
        if (maxSeverity == null) {
            throw TXN.methodParameterIsNull("maxSeverity");
        }
        if (maxSeverity.compareTo(Problem.Severity.CRITICAL) >= 0) {
            throw TXN.illegalSeverity("maxSeverity");
        }
        return TransactionImpl.createTransactionImpl(executor, maxSeverity);
    }

    public abstract Executor getExecutor();

    /**
     * Indicate whether the transaction was terminated.
     * @return {@code true} if the transaction have been committed or reverted, {@code false} otherwise.
     */
    public abstract boolean isTerminated();

    /**
     * Get the duration of the current transaction.
     *
     * @return the duration of the current transaction
     */
    public abstract long getDuration(TimeUnit unit);

    public abstract ProblemReport getProblemReport();

    /**
     * Add a task to this transaction.
     *
     * @param task the task
     * @return the subtask builder
     * @throws IllegalStateException if the transaction is not open
     */
    public abstract <T> TaskBuilder<T> newTask(Executable<T> task) throws IllegalStateException;

    /**
     * Indicate that the current operation on this transaction depends on the completion of the given transaction.
     *
     * @param other the other transaction
     * @throws InterruptedException if the wait was interrupted
     * @throws DeadlockException if this wait has caused a deadlock and this task was selected to break it
     */
    public abstract void waitFor(Transaction other) throws InterruptedException, DeadlockException;
}
