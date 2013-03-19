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
 * Internal interface for task child operations.  Methods called from dependencies start with
 * "dependency...".  Methods called from parents start with "child...".
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
interface TaskChild {

    /**
     * Indicate to this child that a dependency's execution has completed.
     *
     * @param userThread {@code true} if executed from a user thread
     */
    void dependencyExecutionComplete(boolean userThread);

    /**
     * Indicate to this child that a dependency's validation has completed.
     *
     * @param userThread {@code true} if executed from a user thread
     */
    void dependencyValidationComplete(boolean userThread);

    /**
     * Indicate to this child that a dependency's commit has completed. Once all dependencies have
     * completed commit, this child may proceed with commit.
     *
     * @param userThread {@code true} if executed from a user thread
     */
    void dependencyCommitComplete(boolean userThread);

    /**
     * Request this child to initiate rollback when possible. Neither {@link #childInitiateValidate(boolean)} nor
     * {@link #childInitiateCommit(boolean)} may be called after this method is called.
     *
     * @param userThread {@code true} if executed from a user thread
     */
    void childInitiateRollback(boolean userThread);

    /**
     * Request this child to initiate validation when possible. May never be called after
     * {@link #childInitiateRollback(boolean)}.  Will always be followed by either {@link #childInitiateRollback(boolean)} or
     * {@link #childInitiateCommit(boolean)}.
     *
     * @param userThread {@code true} if executed from a user thread
     */
    void childInitiateValidate(boolean userThread);

    /**
     * Request this child to initiate commit when possible. Will always be called after
     * {@link #childInitiateValidate(boolean)}. Will never be called after {@link #childInitiateRollback(boolean)}.
     *
     * @param userThread {@code true} if executed from a user thread
     */
    void childInitiateCommit(boolean userThread);

    /**
     * Request this child to initiate cancellation when possible.
     *
     * @param userThread {@code true} if executed from a user thread
     */
    void forceCancel(boolean userThread);
}
