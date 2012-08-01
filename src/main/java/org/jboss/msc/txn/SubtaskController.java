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

import java.util.Collection;

/**
 * A controller for an installed subtask.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class SubtaskController {

    private final SubtaskContext context;

    SubtaskController(final Transaction transaction, final Subtask subtask) {
        context = new SubtaskContext(transaction, this, subtask);
    }

    /**
     * Get the transaction associated with this controller.
     *
     * @return the transaction associated with this controller
     */
    public Transaction getTransaction() {
        return context.getTransaction();
    }

    /**
     * Roll back this subtask, cancelling it if it is still running.  All dependent subtasks will also be rolled back so
     * use this method with caution.  This method cannot be used to roll back work after the transaction of this
     * controller completes.  The given listener is called upon completion or failure.
     *
     * @param completionListener the listener to call when the rollback finishes, or {@code null} for none
     */
    public void rollback(SubtaskListener completionListener) {

    }

    /**
     * Get the state of this controller.
     *
     * @return the state
     */
    public Subtask.State getState() {
        return context.getState();
    }

    /**
     * Add dependencies, if the subtask has not yet been executed.
     *
     * @param dependencies the dependencies to add
     * @return this controller
     */
    public SubtaskController addDependencies(SubtaskController... dependencies) throws IllegalStateException {
        return this;
    }

    /**
     * Add dependencies, if the subtask has not yet been executed.
     *
     * @param dependencies the dependencies to add
     * @return this controller
     */
    public SubtaskController addDependencies(Collection<SubtaskController> dependencies) throws IllegalStateException {
        return this;
    }

    /**
     * Add a single dependency, if the subtask has not yet been executed.
     *
     * @param dependency the dependency to add
     * @return this controller
     */
    public SubtaskController addDependency(SubtaskController dependency) throws IllegalStateException {
        return this;
    }

    /**
     * Release this task to begin execution.  The given listener is called upon completion or failure, or immediately
     * if this task was already released.
     *
     * @param completionListener the completion listener for this subtask
     * @return this controller
     */
    public SubtaskController release(SubtaskListener completionListener) {
        return this;
    }

    /**
     * Re-stage a failed task to the unreleased state for edition and retry.
     *
     * @return this controller
     */
    public SubtaskController restage() throws IllegalStateException {
        return this;
    }

    /**
     * Get the subtask corresponding to this controller.
     *
     * @return the subtask
     */
    public Subtask getSubtask() {
        return context.getSubtask();
    }
}
