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

/**
 * A task target, which can be used to add new tasks.  A transaction will accept tasks until it is marked for
 * prepare.  A task controller will accept tasks until that task has completed execution.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public interface TaskTarget {

    /**
     * Add a task with an executable component.  If the task implements any of the supplementary
     * interfaces {@link Revertible}, {@link Validatable}, or {@link Committable}, the corresponding
     * builder properties will be pre-initialized.
     *
     * @param task the task
     * @param <T> the result value type (may be {@link Void})
     * @return the builder for the task
     * @throws IllegalStateException if this target is not accepting new tasks
     */
    <T> TaskBuilder<T> newTask(Executable<T> task) throws IllegalStateException;

    /**
     * Add a task without an executable component.  All task components will be uninitialized.
     *
     * @return the builder for the task
     * @throws IllegalStateException if this target is not accepting new tasks
     */
    TaskBuilder<Void> newTask() throws IllegalStateException;
}
