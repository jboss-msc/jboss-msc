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

import java.io.Serializable;

/**
 * A description of a subtask execution failure.  Subtask failures should be described without exceptions whenever
 * possible, and should always include a clear and complete message.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class Failure implements Serializable {

    private static final long serialVersionUID = 7378993289655554246L;

    private final TaskController subtask;
    private final String message;
    private final Throwable cause;

    /**
     * Construct a new instance.
     *
     * @param subtask the subtask that failed
     * @param message the error description
     * @param cause the optional exception cause
     */
    public Failure(final TaskController subtask, final String message, final Throwable cause) {
        if (subtask == null) {
            throw new IllegalArgumentException("subtask is null");
        }
        if (message == null) {
            throw new IllegalArgumentException("message is null");
        }
        this.subtask = subtask;
        this.message = message;
        this.cause = cause;
    }

    /**
     * Construct a new instance.
     *
     * @param subtask the subtask that failed
     * @param message the error description
     */
    public Failure(final TaskController subtask, final String message) {
        this(subtask, message, null);
    }

    /**
     * Construct a new instance.  The exception must not be {@code null}.
     *
     * @param subtask the subtask that failed
     * @param cause the exception cause
     */
    public Failure(final TaskController subtask, final Throwable cause) {
        this(subtask, "Task failed due to exception", cause);
        if (cause == null) {
            throw new IllegalArgumentException("cause is null");
        }
    }

    /**
     * Get the subtask that failed.  Will not be {@code null}.
     *
     * @return the subtask that failed
     */
    public TaskController getSubtask() {
        return subtask;
    }

    /**
     * Get the description of the failure.  Will not be {@code null}.
     *
     * @return the description of the failure
     */
    public String getMessage() {
        return message;
    }

    /**
     * Get the exception cause.  May be {@code null} if an exception wasn't the cause of failure or if the exception
     * is unimportant.
     *
     * @return the exception, or {@code null}
     */
    public Throwable getCause() {
        return cause;
    }
}
