/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2021, Red Hat, Inc., and individual contributors
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

import org.jboss.msc.value.Value;

/**
 * Dependencies of a service.
 *
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 * @author <a href="mailto:flavia.rainone@jboss.com">Flavia Rainone</a>
 * @see Dependent
 */
interface Dependency extends Value<Object> {

    /**
     * Add dependent to this dependency.
     *
     * @param dependent added dependent
     */
    void addDependent(final Dependent dependent);

    /**
     * Remove dependent from this dependency
     *
     * @param dependent removed dependent
     * @return true if registration must be removed from registry
     */
    boolean removeDependent(final Dependent dependent);

    /**
     * Add demand on this dependency.
     */
    void addDemand();

    /**
     * Remove demand from this dependency.
     */
    void removeDemand();

    /**
     * Notify this dependency that one of its dependents is starting.
     */
    void dependentStarted();

    /**
     * Notify this dependency that one of its dependents is stopping.
     */
    void dependentStopped();

    /**
     * Get the installed instance value, if any exists.
     *
     * @return the installed service value
     * @throws IllegalStateException if an error occurs
     */
    Object getValue() throws IllegalStateException;

    /**
     * Get the name of this dependency.
     *
     * @return the name
     */
    ServiceName getName();

    /**
     * Get the controller for this dependency, or {@code null} if there is none currently.
     *
     * @return the controller, or {@code null} for none
     */
    ServiceControllerImpl<?> getDependencyController();

    /**
     * Get R/W lock associated with this dependency.
     *
     * @return internal R/W lock
     */
    Lockable getLock();

}
