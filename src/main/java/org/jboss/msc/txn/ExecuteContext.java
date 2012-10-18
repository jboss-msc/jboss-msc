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
 * Context for a task that may succeed or fail which may also produce a consumable result.
 *
 * @param <T> the result type of the associated task
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public interface ExecuteContext<T> extends TransactionalContext, FailableContext, CancellableContext {

    /**
     * Register the completion of this task.  This method returns without blocking.
     *
     * @param result the result of the task, or {@code null} if the execution type is {@link Void}
     */
    void complete(T result);
}
