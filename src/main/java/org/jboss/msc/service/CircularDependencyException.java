/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2022, Red Hat, Inc., and individual contributors
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
 * Exception used to indicate there was a circular dependency discovered during resolution.
 * 
 * @author John Bailey
 */
public class CircularDependencyException extends ServiceRegistryException {

    private static final long serialVersionUID = -4826336558749601678L;

    private ServiceName[] cycle;

    /**
     * Constructs a {@code CircularDependencyException} with the specified detail message. The cause is not initialized, and
     * may subsequently be initialized by a call to {@link #initCause(Throwable) initCause}.
     *
     * @param msg the detail message
     */
    public CircularDependencyException(final String msg, ServiceName[] cycle) {
        super(msg);
        this.cycle = cycle;
    }

    /**
     * Returns a cycle found during service installation.
     * 
     * @return an array formed by the service names involved in the cycle, in dependency order. Last name in the array
     *         has a dependency on the name in the first position, thus completing the cycle.
     */
    public ServiceName[] getCycle() {
        return cycle;
    }
}
