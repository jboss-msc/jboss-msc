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
 * A task which may be cancelled.  If threads executing on
 * behalf of the corresponding task are interrupted, the {@link #isCancelRequested()} method should be checked to
 * see if the task should be cancelled.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public interface CancellableContext {

    /**
     * Determine if this task has been requested to be cancelled.
     *
     * @return {@code true} if cancel was requested, {@code false} otherwise
     */
    boolean isCancelRequested();

    /**
     * Acknowledge the cancellation of this task.
     */
    void cancelled();

}
