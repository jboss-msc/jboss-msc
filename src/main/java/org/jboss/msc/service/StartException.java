/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2009, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.msc.service;

/**
 * A start exception, thrown when a service fails to start.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public class StartException extends Exception {

    private static final long serialVersionUID = 239274385917008839L;

    private final Location location;
    private volatile ServiceName serviceName;

    /**
     * Constructs a {@code StartException} with no detail message. The cause is not initialized, and may subsequently be
     * initialized by a call to {@link #initCause(Throwable) initCause}.
     */
    public StartException() {
        location = null;
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
        location = null;
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
        location = null;
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
        location = null;
        serviceName = null;
    }

    public StartException(final Location location) {
        this.location = location;
        serviceName = null;
    }

    public StartException(final String message, final Location location) {
        super(message);
        this.location = location;
        serviceName = null;
    }

    public StartException(final String message, final Throwable cause, final Location location) {
        super(message, cause);
        this.location = location;
        serviceName = null;
    }

    public StartException(final Throwable cause, final Location location) {
        super(cause);
        this.location = location;
        serviceName = null;
    }

    public StartException(final String message, final Throwable cause, final Location location, final ServiceName serviceName) {
        super(message, cause);
        this.location = location;
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
            b.append(" in service ").append(serviceName);
        } else {
            b.append(" in anonymous service");
        }
        final String m = getLocalizedMessage();
        if (m != null) {
            b.append(": ").append(m);
        }
        final Location location = this.location;
        if (location != null) {
            b.append("\n\tAt file ").append(location);
        }
        return b.toString();
    }
}
