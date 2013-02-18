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
 * An exception indicating that a transaction has deadlocked.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public class TransactionDeadlockException extends RuntimeException {
    private static final long serialVersionUID = -2219639164354591824L;

    /**
     * Constructs a new {@code TransactionDeadlockException} instance.  The message is left blank ({@code null}), and no
     * cause is specified.
     */
    public TransactionDeadlockException() {
    }

    /**
     * Constructs a new {@code TransactionDeadlockException} instance with an initial message.  No cause is specified.
     *
     * @param msg the message
     */
    public TransactionDeadlockException(final String msg) {
        super(msg);
    }

    /**
     * Constructs a new {@code TransactionDeadlockException} instance with an initial cause.  If a non-{@code null} cause is
     * specified, its message is used to initialize the message of this {@code TransactionDeadlockException}; otherwise the
     * message is left blank ({@code null}).
     *
     * @param cause the cause
     */
    public TransactionDeadlockException(final Throwable cause) {
        super(cause);
    }

    /**
     * Constructs a new {@code TransactionDeadlockException} instance with an initial message and cause.
     *
     * @param msg the message
     * @param cause the cause
     */
    public TransactionDeadlockException(final String msg, final Throwable cause) {
        super(msg, cause);
    }
}
