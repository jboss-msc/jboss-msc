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
 * A context for a task associated with a transaction.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public interface TransactionalContext {
    /**
     * Begin doing work on behalf of this task from the current thread.  Should be followed by a
     * {@code try}/{@code finally} block, wherein the {@link #end()} method is called from the {@code finally}
     * portion.  The initial execution method need not invoke this method; it is presumed to be executing on behalf of
     * the task for the duration of the method call.
     * <p>
     * Calling this method may affect the calling thread in any of the following ways:
     * <ul>
     *     <li>The thread may be interrupted if the associated task is cancellable, and it was cancelled</li>
     *     <li>The thread's current context loader may be set</li>
     *     <li>Management interfaces may be updated to reflect that this task is making progress</li>
     * </ul>
     */
    void begin();

    /**
     * Finish doing work on behalf of this task from the current thread.
     */
    void end();

    /**
     * Get the transaction.
     *
     * @return the transaction
     */
    Transaction getTransaction();
}
