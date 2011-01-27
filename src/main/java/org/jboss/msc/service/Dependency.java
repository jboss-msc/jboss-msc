/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

import org.jboss.msc.service.ServiceController.Mode;
import org.jboss.msc.value.Value;

/**
 * AbstractDependency represents the dependencies of a service.
 * The counterpart of this dependency relation is {@code AbstractDependent}.
 * 
 * @see Dependent
 * @author <a href="mailto:flavia.rainone@jboss.com">Flavia Rainone</a>
 */
interface Dependency extends Value<Object> {
    /**
     * Add a dependent to this dependency, establishing the dependency relation between this dependency and its
     * dependent.  This method must not be called under a lock.
     *
     * @param dependent the dependent to add
     */
    void addDependent(final Dependent dependent);

    /**
     * Remove a dependent from this dependency, breaking the dependency relation between this dependency and its
     * dependent.  This method must not be called under a lock.
     *
     * @param dependent the dependent to remove
     */
    void removeDependent(final Dependent dependent);

    /**
     * Notify that a {@link Dependent dependent} entered {@link Mode#ACTIVE active mode}.
     * This method must not be called under a lock.
     */
    void addDemand();

    /**
     * Notify that a {@link Dependent dependent} left {@link Mode#ACTIVE active mode}.
     * This method must not be called under a lock.
     */
    void removeDemand();

    /**
     * Notify that a {@link Dependent dependent} is starting.
     * This method must not be called under a lock.
     */
    void dependentStarted();

    /**
     * Notify that a {@link Dependent dependent} is stopping.
     * This method must not be called under a lock.
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
     * Accepts visit from {@code visitor}.
     * 
     * @param <T> the visitor return type
     * @param visitor the visitor
     * @return the return from the visitor
     */
    <T> T accept(Visitor<T> visitor);
}
