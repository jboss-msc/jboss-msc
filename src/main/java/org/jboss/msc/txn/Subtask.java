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
 * A transactional subtask.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public interface Subtask {

    /**
     * Execute the primary work of this task.
     *
     * @throws Exception if this task fails to execute for some reason
     */
    void execute(SubtaskContext context);

    /**
     * Revert the work done by {@link #execute(SubtaskContext)}.
     */
    void rollback(SubtaskContext context);

    /**
     * Prepare for a commit of this task.
     *
     * @throws Exception if the prepare fails for some reason
     */
    void prepare(SubtaskContext context);

    /**
     * Revert the work done by {@link #prepare(SubtaskContext)}.
     */
    void abort(SubtaskContext context);

    /**
     * Commit the work done by {@link #prepare(SubtaskContext)}.
     */
    void commit(SubtaskContext context);

    /**
     * The possible states of a subtask.
     */
    enum State {
        WAITING,
        RUNNING,
        COMPLETE,
        ROLLED_BACK,
        FAILED,
        ABORTED,
        COMMITTED,
        ;
    }
}
