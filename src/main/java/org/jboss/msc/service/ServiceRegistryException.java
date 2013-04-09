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
 * Base Exception class used for service registry issues.
 * 
 * @author John Bailey
 */
class ServiceRegistryException extends RuntimeException {

    private static final long serialVersionUID = -8431095020967798064L;

    /**
     * Constructs a {@code ServiceRegistryException} with no detail message. The cause is not initialized, and may
     * subsequently be initialized by a call to {@link #initCause(Throwable) initCause}.
     */
    public ServiceRegistryException() {
    }

    /**
     * Constructs a {@code ServiceRegistryException} with the specified detail message. The cause is not initialized, and
     * may subsequently be initialized by a call to {@link #initCause(Throwable) initCause}.
     *
     * @param msg the detail message
     */
    public ServiceRegistryException(final String msg) {
        super(msg);
    }

    /**
     * Constructs a {@code ServiceRegistryException} with the specified cause. The detail message is set to:
     * <pre>(cause == null ? null : cause.toString())</pre>
     * (which typically contains the class and detail message of {@code cause}).
     *
     * @param cause the cause (which is saved for later retrieval by the {@link #getCause()} method)
     */
    public ServiceRegistryException(final Throwable cause) {
        super(cause);
    }

    /**
     * Constructs a {@code ServiceRegistryException} with the specified detail message and cause.
     *
     * @param msg the detail message
     * @param cause the cause (which is saved for later retrieval by the {@link #getCause()} method)
     */
    public ServiceRegistryException(final String msg, final Throwable cause) {
        super(msg, cause);
    }
}
