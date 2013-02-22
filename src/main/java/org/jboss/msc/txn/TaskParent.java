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
 * Internal interface for task parent operations.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
interface TaskParent {

    /**
     * Indicate that a child's execution has finished.
     *
     * @param child the child
     * @param userThread {@code true} if executed from a user thread
     */
    void childExecutionFinished(TaskChild child, boolean userThread);

    /**
     * Indicate that a child's validation has finished.
     *
     * @param child the child
     * @param userThread {@code true} if executed from a user thread
     */
    void childValidationFinished(TaskChild child, boolean userThread);

    /**
     * Indicate that a child is terminated.
     *
     * @param child the child
     * @param userThread {@code true} if executed from a user thread
     */
    void childTerminated(TaskChild child, boolean userThread);

    /**
     * Indicate to a parent that a child was added.
     *
     * @param child the child to add
     * @param userThread {@code true} if executed from a user thread
     */
    void childAdded(TaskChild child, boolean userThread) throws InvalidTransactionStateException;

    /**
     * Get the transaction implementation for this parent.
     *
     * @return the transaction implementation
     */
    TransactionImpl getTransaction();
}
