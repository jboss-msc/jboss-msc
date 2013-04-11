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
public final class Problem implements Serializable {

    private static final long serialVersionUID = 7378993289655554246L;

    private final TaskController taskController;
    private final String message;
    private final Throwable cause;
    private final Severity severity;
    private final Location location;

    /**
     * Construct a new instance.
     *
     * @param taskController the task that failed
     * @param message the error description
     * @param cause the optional exception cause
     * @param severity the severity of the problem
     * @param location the location description of the problem
     */
    public Problem(final TaskController taskController, final String message, final Throwable cause, final Severity severity, final Location location) {
        if (taskController == null) {
            throw new IllegalArgumentException("task is null");
        }
        if (message == null) {
            throw new IllegalArgumentException("message is null");
        }
        if (severity == null) {
            throw new IllegalArgumentException("severity is null");
        }
        this.taskController = taskController;
        this.message = message;
        this.cause = cause;
        this.severity = severity;
        this.location = location;
    }

    /**
     * Construct a new instance.
     *
     * @param taskController the task that failed
     * @param message the error description
     * @param cause the optional exception cause
     * @param severity the severity of the problem
     */
    public Problem(final TaskController taskController, final String message, final Throwable cause, final Severity severity) {
        if (taskController == null) {
            throw new IllegalArgumentException("task is null");
        }
        if (message == null) {
            throw new IllegalArgumentException("message is null");
        }
        if (severity == null) {
            throw new IllegalArgumentException("severity is null");
        }
        this.severity = severity;
        this.cause = cause;
        this.message = message;
        this.taskController = taskController;
        location = null;
    }

    /**
     * Construct a new instance.
     *
     * @param task the task that failed
     * @param message the error description
     * @param cause the optional exception cause
     */
    public Problem(final TaskController task, final String message, final Throwable cause) {
        this(task, message, cause, Severity.ERROR);
    }

    /**
     * Construct a new instance.
     *
     * @param task the task that failed
     * @param message the error description
     * @param cause the optional exception cause
     * @param location the location description of the problem
     */
    public Problem(final TaskController task, final String message, final Throwable cause, final Location location) {
        this(task, message, cause, Severity.ERROR, location);
    }

    /**
     * Construct a new instance.
     *
     * @param task the task that failed
     * @param message the error description
     * @param severity the severity of the problem
     */
    public Problem(final TaskController task, final String message, final Severity severity) {
        this(task, message, null, severity);
    }

    /**
     * Construct a new instance.
     *
     * @param task the task that failed
     * @param message the error description
     * @param severity the severity of the problem
     * @param location the location description of the problem
     */
    public Problem(final TaskController task, final String message, final Severity severity, final Location location) {
        this(task, message, null, severity, location);
    }

    /**
     * Construct a new instance.
     *
     * @param task the task that failed
     * @param message the error description
     */
    public Problem(final TaskController task, final String message) {
        this(task, message, null, Severity.ERROR);
    }

    /**
     * Construct a new instance.
     *
     * @param task the task that failed
     * @param message the error description
     * @param location the location description of the problem
     */
    public Problem(final TaskController task, final String message, final Location location) {
        this(task, message, null, Severity.ERROR, location);
    }

    /**
     * Construct a new instance.  The exception must not be {@code null}.
     *
     * @param task the task that failed
     * @param cause the exception cause
     */
    public Problem(final TaskController task, final Throwable cause) {
        this(task, "Task failed due to exception", cause);
        if (cause == null) {
            throw new IllegalArgumentException("cause is null");
        }
    }

    /**
     * Construct a new instance.  The exception must not be {@code null}.
     *
     * @param task the task that failed
     * @param cause the exception cause
     * @param location the location description of the problem
     */
    public Problem(final TaskController task, final Throwable cause, final Location location) {
        this(task, "Task failed due to exception", cause, location);
        if (cause == null) {
            throw new IllegalArgumentException("cause is null");
        }
    }

    /**
     * Get the task that failed.  Will not be {@code null}.
     *
     * @return the task that failed
     */
    public TaskController getTask() {
        return taskController;
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

    /**
     * Get the severity of this problem.
     *
     * @return the severity of this problem
     */
    public Severity getSeverity() {
        return severity;
    }

    /**
     * Get the source location of the problem, if any.
     *
     * @return the source location, or {@code null} if there is none
     */
    public Location getLocation() {
        return location;
    }

    /**
     * The severity of a problem.
     */
    public enum Severity {
        /**
         * This problem will not cause adverse effects, but it is something the user should be made aware of.  Such
         * problems will never cause a transaction to fail by themselves.  Examples include deprecation or indication
         * of a possible configuration problem.
         */
        INFO,
        /**
         * This problem could possibly cause adverse effects now or in the future.  The transaction may be configured
         * to fail in this condition, though by default it will not.
         */
        WARNING,
        /**
         * This problem will likely cause adverse effects now or in the future.  The transaction may be configured
         * to succeed even in this condition, though by default it will fail.  Examples include a failed service or a
         * missing service dependency.
         */
        ERROR,
        /**
         * This problem will cause irreparable damage to the integrity of the transaction.  The transaction will always
         * roll back in this case.  Examples include resource exhaustion.
         */
        CRITICAL,
        ;

        /**
         * Determine if this severity is contained in the given list.
         *
         * @param severities the list
         * @return {@code true} if this is found, {@code false} otherwise
         */
        public boolean in(Severity... severities) {
            for (Severity severity : severities) {
                if (this == severity) return true;
            }
            return false;
        }
    }
}
