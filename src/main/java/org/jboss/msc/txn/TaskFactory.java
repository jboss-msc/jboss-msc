/*
 * JBoss, Home of Professional Open Source
 *
 * Copyright 2013 Red Hat, Inc. and/or its affiliates.
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
 * A task factory.
 * 
 * @author <a href="mailto:frainone@redhat.com">Flavia Rainone</a>
 *
 */
public interface TaskFactory {

    /**
     * Adds a task with an executable component to {@code transaction}.  If the task implements any of the supplementary
     * interfaces {@link Revertible}, {@link Validatable}, or {@link Committable}, the corresponding
     * builder properties will be pre-initialized.
     * 
     * @param task        the task
     * @param transaction the transaction
     * @param <T> the result value type (may be {@link Void})
     * @return the builder for the task
     * @throws IllegalStateException if this context is not accepting new tasks
     */
    <T> TaskBuilder<T> newTask(Executable<T> task) throws IllegalStateException;

    /**
     * Adds a task without an executable component to {@code transaction}.  All task components will be uninitialized.
     * 
     * @param transaction  the transaction
     * @return the builder for the task
     * @throws IllegalStateException if this context is not accepting new tasks
     */
    TaskBuilder<Void> newTask() throws IllegalStateException;
}
