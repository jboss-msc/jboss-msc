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
 * Context for a task that may succeed or fail which may also produce a consumable result.  If threads executing on
 * behalf of the corresponding task are interrupted, the {@link #isCancelRequested()} method should be checked to
 * see if the task should be cancelled.
 *
 * @param <T> the result type of the associated task
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public interface ExecutionContext<T> extends TransactionalContext {

    void complete(T result);

    void failed(Failure reason);

    /**
     * Begin doing work on behalf of this task from the current thread.  Should be followed by a
     * {@code try}/{@code finally} block, wherein the {@link #end()} method is called from the {@code finally}
     * portion.  The initial {@link Executable#execute(ExecutionContext)} method need not invoke this method; it
     * is presumed to be executing on behalf of the task for the duration of the method call.
     */
    void begin();

    /**
     * Finish doing work on behalf of this task from the current thread.
     */
    void end();

    /**
     * Determine if this task has been requested to be cancelled (due to its containing transaction being
     * rolled back during processing).
     *
     * @return {@code true} if cancel was requested, {@code false} otherwise
     */
    boolean isCancelRequested();

    /**
     * Acknowledge the cancellation of this task.
     */
    void cancelled();
}
