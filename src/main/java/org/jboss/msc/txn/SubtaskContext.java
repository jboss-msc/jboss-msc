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

import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;

import static org.jboss.msc.txn.Bits.allAreSet;

/**
 * The context for a subtask work unit.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class SubtaskContext {
    private final Transaction transaction;
    private final SubtaskController controller;
    private final Subtask subtask;

    private volatile int state;

    private static final AtomicIntegerFieldUpdater<SubtaskContext> stateUpdater = AtomicIntegerFieldUpdater.newUpdater(SubtaskContext.class, "state");

    private static final int FLAG_CANCEL_REQ = 1 << 31;

    private static final int STATE_UNRELEASED = 0;
    private static final int STATE_WAIT_DEPS = 1;
    private static final int STATE_EXECUTE = 2;
    private static final int STATE_EXECUTE_CANCELLED = 3;
    private static final int STATE_EXECUTE_COMPLETE = 4;
    private static final int STATE_ROLLBACK = 5;
    private static final int STATE_ROLLBACK_COMPLETE = 6;
    private static final int STATE_PREPARE = 7;
    private static final int STATE_PREPARE_COMPLETE = 8;
    private static final int STATE_ABORT = 10;
    private static final int STATE_ABORT_COMPLETE = 11;
    private static final int STATE_COMMIT = 12;
    private static final int STATE_COMMIT_COMPLETE = 13;

    SubtaskContext(final Transaction transaction, final SubtaskController controller, final Subtask subtask) {
        this.transaction = transaction;
        this.controller = controller;
        this.subtask = subtask;
    }

    /**
     * Get the current transaction.
     *
     * @return the current transaction
     */
    public Transaction getTransaction() {
        return transaction;
    }

    /**
     * Begin doing possibly long-running work on behalf of this subtask.  Calling this method may interrupt the current
     * thread if the task has been cancelled.  Calling this method is not necessary to do work on behalf of the subtask
     * and can even needlessly degrade performance for very small units of work; only work that is expected to be long-
     * running should call this method.
     */
    public void begin() {
    }

    /**
     * End doing work on behalf of this subtask.
     */
    public void end() {
    }

    /**
     * Finish this subtask's work.  This method is idempotent.
     */
    public void executeComplete() {
    }

    /**
     * Cause the subtask to be considered 'failed'.
     *
     * @param cause the failure reason
     */
    public void executeFailed(SubtaskFailure cause) {
    }

    /**
     * Complete cancellation of the current task.
     */
    public void executeCancelled() {
    }

    public void rollbackComplete() {
    }

    public void prepareComplete() {
    }

    public void prepareFailed(SubtaskFailure cause) {

    }

    public void abortComplete() {
    }

    public void commitComplete() {
    }

    public boolean cancelRequested() {
        return allAreSet(state, FLAG_CANCEL_REQ);
    }

    Subtask.State getState() {
        return null;
    }

    Subtask getSubtask() {
        return subtask;
    }

    public SubtaskController getController() {
        return controller;
    }
}
