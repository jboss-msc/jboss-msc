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

package org.jboss.msc.service;

/**
 * A start exception, thrown when a service fails to start.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public class StartException extends Exception {

    private static final long serialVersionUID = 239274385917008839L;

    private volatile ServiceName serviceName;

    /**
     * Constructs a {@code StartException} with no detail message. The cause is not initialized, and may subsequently be
     * initialized by a call to {@link #initCause(Throwable) initCause}.
     */
    public StartException() {
        serviceName = null;
    }

    /**
     * Constructs a {@code StartException} with the specified detail message. The cause is not initialized, and may
     * subsequently be initialized by a call to {@link #initCause(Throwable) initCause}.
     *
     * @param msg the detail message
     */
    public StartException(final String msg) {
        super(msg);
        serviceName = null;
    }

    /**
     * Constructs a {@code StartException} with the specified cause. The detail message is set to:
     * <pre>(cause == null ? null : cause.toString())</pre>
     * (which typically contains the class and detail message of {@code cause}).
     *
     * @param cause the cause (which is saved for later retrieval by the {@link #getCause()} method)
     */
    public StartException(final Throwable cause) {
        super(cause);
        serviceName = null;
    }

    /**
     * Constructs a {@code StartException} with the specified detail message and cause.
     *
     * @param msg the detail message
     * @param cause the cause (which is saved for later retrieval by the {@link #getCause()} method)
     */
    public StartException(final String msg, final Throwable cause) {
        super(msg, cause);
        serviceName = null;
    }

    public StartException(final String message, final Throwable cause, final ServiceName serviceName) {
        super(message, cause);
        this.serviceName = serviceName;
    }

    public ServiceName getServiceName() {
        return serviceName;
    }

    public void setServiceName(final ServiceName serviceName) {
        this.serviceName = serviceName;
    }

    public String toString() {
        final StringBuilder b = new StringBuilder(getClass().getName());
        if (serviceName != null) {
            b.append(" in ").append(serviceName);
        } else {
            b.append(" in anonymous service");
        }
        final String m = getLocalizedMessage();
        if (m != null) {
            b.append(": ").append(m);
        }
        return b.toString();
    }
}
