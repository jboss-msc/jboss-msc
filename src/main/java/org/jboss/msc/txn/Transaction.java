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

import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

/**
 * A task transaction.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class Transaction {
    private final long startTime = System.nanoTime();
    private volatile long endTime;
    private final Executor subtaskExecutor;
    private final Queue<SubtaskContext> subtasks = new ConcurrentLinkedQueue<SubtaskContext>();
    private final ConcurrentMap<AttachmentKey, Object> attachments = new ConcurrentHashMap<AttachmentKey, Object>();

    private Transaction(final Executor executor) {
        subtaskExecutor = executor;
    }

    /**
     * Create a new subtask transaction.
     *
     * @param executor the executor to use to run subtasks
     * @return the transaction
     */
    public static Transaction create(Executor executor) {
        return new Transaction(executor);
    }

    /**
     * Add a subtask to this transaction.
     *
     * @param subtask the subtask
     * @return the subtask builder
     * @throws IllegalStateException if the transaction is not open
     */
    public SubtaskController newSubtask(Subtask subtask) throws IllegalStateException {
        return new SubtaskController(this, subtask);
    }

    /**
     * Roll back this transaction, undoing all work executed up until this time.  New work may not be submitted against
     * this transaction once this method has been called.
     *
     * @param completionListener the listener to call when the rollback is complete
     * @throws IllegalStateException if the transaction was already terminated
     */
    public void rollback(TransactionListener completionListener) throws IllegalStateException {

    }

    /**
     * Prepare this transaction.  It is an error to prepare a transaction with unreleased tasks.
     * Once this method returns, either {@link #commit(TransactionListener)} or {@link #rollback(TransactionListener)} must be called.
     * If this method throws an exception, the transaction must {@link #rollback(TransactionListener)}.  After calling this method (regardless
     * of its outcome), the transaction can not be modified before termination.
     *
     * @param completionListener the listener to call when the rollback is complete
     * @throws PrepareFailedException if the transaction cannot be prepared
     */
    public void prepare(TransactionListener completionListener) throws PrepareFailedException {
    }

    /**
     * Commit the work done by {@link #prepare(TransactionListener)} and terminate this transaction.
     *
     * @param completionListener the listener to call when the rollback is complete
     */
    public void commit(TransactionListener completionListener) {
        // CAS state to COMMIT from OPEN or PREPARE
        SubtaskContext context;
        while ((context = subtasks.poll()) != null) {
            // initiate commit
        }
        // CAS state from COMMIT to COMPLETE
    }

    @SuppressWarnings("unchecked")
    public <T> T getAttachment(AttachmentKey<T> key) {
        return (T) attachments.get(key);
    }

    @SuppressWarnings("unchecked")
    public <T> T putAttachment(AttachmentKey<T> key, T newValue) {
        return (T) attachments.put(key, newValue);
    }

    @SuppressWarnings("unchecked")
    public <T> T putAttachmentIfAbsent(AttachmentKey<T> key, T newValue) {
        return (T) attachments.putIfAbsent(key, newValue);
    }

    @SuppressWarnings("unchecked")
    public <T> T removeAttachment(AttachmentKey<T> key) {
        return (T) attachments.remove(key);
    }

    public <T> boolean removeAttachment(AttachmentKey<T> key, T expectedValue) {
        return attachments.remove(key, expectedValue);
    }

    @SuppressWarnings("unchecked")
    public <T> T replaceAttachment(AttachmentKey<T> key, T newValue) {
        return (T) attachments.replace(key, newValue);
    }

    public <T> boolean replaceAttachment(AttachmentKey<T> key, T expectedValue, T newValue) {
        return attachments.replace(key, expectedValue, newValue);
    }

    public boolean containsAttachment(AttachmentKey<?> key) {
        return attachments.containsKey(key);
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
