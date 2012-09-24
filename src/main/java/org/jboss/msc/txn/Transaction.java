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

import static org.jboss.msc.txn.Bits.allAreSet;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public abstract class Transaction extends AbstractAttachable {

    /**
     * Create a new subtask transaction.
     *
     * @param executor the executor to use to run subtasks
     * @return the transaction
     */
    public static Transaction create(Executor executor) {
        return new RootTransaction(executor);
    }

    /**
     * Create a nested transaction within this transaction.
     *
     * @return the nested transaction
     */
    public final Transaction createNested() {
        return new ChildTransaction(this);
    }

    abstract <T> TaskBuilder<T> newSubtask(Executable<T> executable, Object subtask, Transaction owner) throws IllegalStateException;

    /**
     * Add a subtask to this transaction.
     *
     * @param subtask the subtask
     * @return the subtask builder
     * @throws IllegalStateException if the transaction is not open
     */
    public final <T> TaskBuilder<T> newSubtask(Executable<T> subtask) throws IllegalStateException {
        return newSubtask(subtask, subtask, this);
    }

    public final TaskBuilder<Void> newSubtask(Object subtask) throws IllegalStateException {
        return newSubtask(Executable.NULL, subtask, this);
    }

    /**
     * Roll back this transaction, undoing all work executed up until this time.
     *
     * @param completionListener the listener to call when the rollback is complete
     * @return {@code true} if rollback was initiated, or {@code false} if another thread has already committed or rolled back
     */
    public boolean rollback(TxnListener completionListener) {
    }

    /**
     * Determine whether this transaction is a parent or ancestor of another transaction.
     *
     * @param transaction the possible child transaction
     * @return {@code true} if this transaction is equal to, or is an ancestor of, the given transaction
     */
    public final boolean isParentOf(Transaction transaction) {
        return this == transaction || transaction instanceof ChildTransaction && isParentOf(((ChildTransaction) transaction).getParent());
    }

    /**
     * Determine whether this transaction is a child or descendant of another transaction.
     *
     * @param transaction the possible parent transaction
     * @return {@code true} if this transaction is equal to, or is a descendant of, the given transaction
     */
    public final boolean isChildOf(Transaction transaction) {
        return transaction != null && transaction.isParentOf(this);
    }

    public abstract Executor getExecutor();

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
