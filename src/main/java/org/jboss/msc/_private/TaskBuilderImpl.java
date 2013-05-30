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

package org.jboss.msc._private;

import static org.jboss.msc._private.MSCLogger.TXN;

import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

import org.jboss.msc.txn.Committable;
import org.jboss.msc.txn.Executable;
import org.jboss.msc.txn.Revertible;
import org.jboss.msc.txn.TaskBuilder;
import org.jboss.msc.txn.TaskController;
import org.jboss.msc.txn.Transaction;
import org.jboss.msc.txn.Validatable;

/**
 * A builder for subtasks.  Subtasks may be configured with dependencies and injections before being installed.
 * Dependency tasks must be associated with the same transaction as the subtask being built, or a parent thereof.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
final class TaskBuilderImpl<T> implements TaskBuilder<T> {

    @SuppressWarnings("rawtypes")
    private static final TaskControllerImpl[] NO_TASKS = new TaskControllerImpl[0];
    private final Transaction transaction;
    private final TaskParent parent;
    private final Set<TaskControllerImpl<?>> dependencies = Collections.newSetFromMap(new IdentityHashMap<TaskControllerImpl<?>, Boolean>());
    private ClassLoader classLoader;
    private Executable<T> executable;
    private Validatable validatable;
    private Revertible revertible;
    private Committable committable;

    TaskBuilderImpl(final Transaction transaction, final TaskParent parent, final Executable<T> executable) {
        this.transaction = transaction;
        this.parent = parent;
        this.executable = executable;
        if (executable instanceof Validatable) validatable = (Validatable) executable;
        if (executable instanceof Revertible) revertible = (Revertible) executable;
        if (executable instanceof Committable) committable = (Committable) executable;
    }

    TaskBuilderImpl(final Transaction transaction, final TaskParent parent) {
        this(transaction, parent, null);
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
     * @return this task builder
     */
    public TaskBuilderImpl<T> setExecutable(final Executable<T> executable) {
        this.executable = executable;
        return this;
    }

    /**
     * Set the validatable part of this task, or {@code null} to prevent the validation phase from running for this task.
     *
     * @param validatable the validatable part
     * @return this task builder
     */
    public TaskBuilderImpl<T> setValidatable(final Validatable validatable) {
        this.validatable = validatable;
        return this;
    }

    /**
     * Set the revertible part of this task, or {@code null} if the task should not support rollback.
     *
     * @param revertible the revertible part
     * @return this task builder
     */
    public TaskBuilderImpl<T> setRevertible(final Revertible revertible) {
        this.revertible = revertible;
        return this;
    }

    /**
     * Set the committable part of this task, or {@code null} if this task should not support commit operations.
     *
     * @param committable the committable part
     * @return this task builder
     */
    public TaskBuilderImpl<T> setCommittable(final Committable committable) {
        this.committable = committable;
        return this;
    }

    /**
     * todo - doc
     *
     * @param task
     * @return this task builder
     */
    public TaskBuilderImpl<T> setTraits(final Object task) {
        if (task instanceof Committable) committable = (Committable) task;
        if (task instanceof Revertible) revertible = (Revertible) task;
        if (task instanceof Validatable) validatable = (Validatable) task;
        return this;
    }

    /**
     * Set the class loader to use for this task.
     *
     * @param classLoader the class loader
     * @return this task builder
     */
    public TaskBuilderImpl<T> setClassLoader(final ClassLoader classLoader) {
        this.classLoader = classLoader;
        return this;
    }

    /**
     * Add dependencies, if this subtask has not yet been executed.
     *
     * @param dependencies the dependencies to add
     * @return this builder
     */
    public TaskBuilderImpl<T> addDependencies(final TaskController<?>... dependencies) throws IllegalStateException {
        if (dependencies == null) {
            throw TXN.methodParameterIsNull("dependencies");
        }
        for (final TaskController<?> dependency : dependencies) {
            addDependency(dependency);
        }
        return this;
    }

    /**
     * Add dependencies, if this subtask has not yet been executed.
     *
     * @param dependencies the dependencies to add
     * @return this builder
     */
    public TaskBuilderImpl<T> addDependencies(final Collection<TaskController<?>> dependencies) throws IllegalStateException {
        if (dependencies == null) {
            throw TXN.methodParameterIsNull("dependencies");
        }
        for (final TaskController<?> dependency : dependencies) {
            addDependency(dependency);
        }
        return this;
    }

    /**
     * Add a single dependency, if this subtask has not yet been executed.
     *
     * @param dependency the dependency to add
     * @return this builder
     */
    public TaskBuilderImpl<T> addDependency(final TaskController<?> dependency) throws IllegalStateException {
        if (dependency == null) {
            throw TXN.methodParameterIsNull("dependency");
        }
        dependencies.add((TaskControllerImpl<?>) dependency);
        return this;
    }

    /**
     * Release this task to begin execution.  The given listener is called upon completion or failure, or immediately
     * if this task was already released.
     *
     * @return the new controller
     */
    public TaskControllerImpl<T> release() {
        @SuppressWarnings("rawtypes")
        final TaskControllerImpl[] dependenciesArray = dependencies.isEmpty() ? NO_TASKS : dependencies.toArray(new TaskControllerImpl[dependencies.size()]);
        final TaskControllerImpl<T> controller = new TaskControllerImpl<T>(parent, dependenciesArray, executable, revertible, validatable, committable, classLoader);
        controller.install();
        return controller;
    }
}
