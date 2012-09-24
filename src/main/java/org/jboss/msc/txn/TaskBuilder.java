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
import org.jboss.msc.value.ReadableValue;
import org.jboss.msc.value.WritableValue;

/**
 * A builder for subtasks.  Subtasks may be configured with dependencies and injections before being installed.
 * Dependency tasks must be associated with the same transaction as the subtask being built, or a parent thereof.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class TaskBuilder<T> {

    private final RootTransaction transaction;
    private final Object subtask;

    TaskBuilder(final RootTransaction transaction, final Object subtask) {
        this.transaction = transaction;
        this.subtask = subtask;
    }

    /**
     * Get the transaction associated with this builder.
     *
     * @return the transaction associated with this builder
     */
    public Transaction getTransaction() {
        return transaction;
    }

    /**
     * Add dependencies, if this subtask has not yet been executed.
     *
     * @param dependencies the dependencies to add
     * @return this builder
     */
    public TaskBuilder<T> addDependencies(TaskController<?>... dependencies) throws IllegalStateException {
        return this;
    }

    /**
     * Add dependencies, if this subtask has not yet been executed.
     *
     * @param dependencies the dependencies to add
     * @return this builder
     */
    public TaskBuilder<T> addDependencies(Collection<TaskController<?>> dependencies) throws IllegalStateException {
        return this;
    }

    /**
     * Add a single dependency, if this subtask has not yet been executed.
     *
     * @param dependency the dependency to add
     * @return this builder
     */
    public TaskBuilder<T> addDependency(TaskController<?> dependency) throws IllegalStateException {
        if (! dependency.getTransaction().isParentOf(transaction)) {
            throw new IllegalArgumentException("Dependency transaction must be the same as, or a parent of, this subtask's transaction");
        }
        return this;
    }

    /**
     * Add a single dependency, if this subtask has not yet been executed.
     *
     * @param dependency the dependency to add
     * @param injectTarget a writable target to inject the result of this dependency's execution
     * @return this builder
     */
    public <J> TaskBuilder<T> addDependency(TaskController<J> dependency, WritableValue<? super J> injectTarget) throws IllegalStateException {
        return this;
    }

    /**
     * Add a non-dependency injection, if this subtask has not yet been executed.  The injection is executed when
     * the subtask's dependencies are complete but the task itself has not yet executed.
     *
     * @param source the injection source
     * @param injectTarget the writable target to inject the value
     * @param <J> the value type
     * @return this builder
     */
    public <J> TaskBuilder<T> addInjection(ReadableValue<J> source, WritableValue<? super J> injectTarget) {
        return this;
    }

    /**
     * Release this task to begin execution.  The given listener is called upon completion or failure, or immediately
     * if this task was already released.
     *
     * @param completionListener the completion listener for this subtask
     * @return the new controller
     */
    public TaskController<T> release(TaskListener<? super T> completionListener) {
        return null;
    }

    /**
     * Get the subtask corresponding to this controller.
     *
     * @return the subtask
     */
    public Object getSubtask() {
        return subtask;
    }

    /**
     * Add an outward injection that is emitted when a subtask completes execution.
     *
     * @param target the target into which the value should be written
     * @return this builder
     */
    public TaskBuilder<T> addOutwardInjection(final WritableValue<? super T> target) {
        return this;
    }

}
