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
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class TransactionRolledBackException extends IllegalStateException {

    private static final long serialVersionUID = -1821836772997407806L;

    /**
     * Constructs a {@code TransactionRolledBackException} with no detail message. The cause is not initialized, and may
     * subsequently be initialized by a call to {@link #initCause(Throwable) initCause}.
     */
    public TransactionRolledBackException() {
    }

    /**
     * Constructs a {@code TransactionRolledBackException} with the specified detail message. The cause is not
     * initialized, and may subsequently be initialized by a call to {@link #initCause(Throwable) initCause}.
     *
     * @param msg the detail message
     */
    public TransactionRolledBackException(final String msg) {
        super(msg);
    }

    /**
     * Constructs a {@code TransactionRolledBackException} with the specified cause. The detail message is set to:
     * <pre>(cause == null ? null : cause.toString())</pre>
     * (which typically contains the class and detail message of {@code cause}).
     *
     * @param cause the cause (which is saved for later retrieval by the {@link #getCause()} method)
     */
    public TransactionRolledBackException(final Throwable cause) {
        super(cause);
    }

    /**
     * Constructs a {@code TransactionRolledBackException} with the specified detail message and cause.
     *
     * @param msg the detail message
     * @param cause the cause (which is saved for later retrieval by the {@link #getCause()} method)
     */
    public TransactionRolledBackException(final String msg, final Throwable cause) {
        super(msg, cause);
    }
}
