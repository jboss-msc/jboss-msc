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
 * A builder for subtasks.  Subtasks may be configured with dependencies and injections before being installed.
 * Dependency tasks must be associated with the same transaction as the subtask being built, or a parent thereof.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class TaskBuilder<T> {

    private final Transaction transaction;
    private Executable<T> executable;
    private Validatable validatable;
    private Revertible revertible;
    private Committable committable;

    TaskBuilder(final Transaction transaction, final Executable<T> executable) {
        this.transaction = transaction;
        this.executable = executable;
        if (executable instanceof Validatable) validatable = (Validatable) executable;
        if (executable instanceof Revertible) revertible = (Revertible) executable;
        if (executable instanceof Committable) committable = (Committable) executable;
    }

    TaskBuilder(final Transaction transaction) {
        this(transaction, null);
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
     * Change the executable part of this task, or {@code null} to prevent the executable part from running.
     *
     * @param executable the new executable part
     */
    public void setExecutable(final Executable<T> executable) {
        this.executable = executable;
    }

    /**
     * Set the validatable part of this task, or {@code null} to prevent the validation phase from running for this task.
     *
     * @param validatable the validatable part
     */
    public void setValidatable(final Validatable validatable) {
        this.validatable = validatable;
    }

    /**
     * Set the revertible part of this task, or {@code null} if the task should not support rollback.
     *
     * @param revertible the revertible part
     */
    public void setRevertible(final Revertible revertible) {
        this.revertible = revertible;
    }

    /**
     * Set the committable part of this task, or {@code null} if this task should not support commit operations.
     *
     * @param committable the committable part
     */
    public void setCommittable(final Committable committable) {
        this.committable = committable;
    }

    /**
     * todo - doc
     *
     * @param task
     */
    public void setTraits(final Object task) {
        if (task instanceof Committable) committable = (Committable) task;
        if (task instanceof Revertible) revertible = (Revertible) task;
        if (task instanceof Validatable) validatable = (Validatable) task;
    }

    /**
     * Set the class loader to use for this task.
     *
     * @param classLoader the class loader
     * @return this task builder
     */
    public TaskBuilder<T> setClassLoader(ClassLoader classLoader) {
        return this;
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
        return this;
    }

    /**
     * Release this task to begin execution.  The given listener is called upon completion or failure, or immediately
     * if this task was already released.
     *
     * @return the new controller
     */
    public TaskController<T> release() {
        return null;
    }

}
