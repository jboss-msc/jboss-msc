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

/**
 * Depends on one or more dependencies, represented by {@code AbstractDependency}.
 * 
 * @author <a href="mailto:flavia.rainone@jboss.com">Flavia Rainone</a>
 * @see Dependency
 */
interface Dependent {

    /**
     * Notify this dependent that one of its immediate dependencies is available, i.e., it is installed and, if not
     * {@link ServiceController.State#STARTED started}, should start shortly.
     * <p> This method must not be called under a lock.
     *
     * @param dependencyName the name of the immediate dependency that is now available
     */
    void immediateDependencyAvailable(ServiceName dependencyName);

    /**
     * Notify this dependent that one of its immediate dependencies is unavailable.<br>
     * A dependency is unavailable when it is not installed or when it is in {@link ServiceController.Mode#NEVER NEVER}
     * mode.
     * <p> This method must not be called under a lock.
     *
     * @param dependencyName the name of the immediate dependency that is now unavailable
     */
    void immediateDependencyUnavailable(ServiceName dependencyName);

    /**
     * Notify this dependent that one of its immediate dependencies entered {@link ServiceControllerImpl.Substate#UP UP}
     * state.
     * <p> This method must not be called under a lock.
     */
    void immediateDependencyUp();

    /**
     * Notify this dependent that one of its immediate dependencies is leaving the {@link
     * ServiceControllerImpl.Substate#UP UP} state.
     * <p> This method must not be called under a lock.
     */
    void immediateDependencyDown();

    /**
     * Notify this dependent that one of its dependencies (immediate or transitive) failed to start. This method is
     * called after the dependency state transitions from {@code STARTING} to {@code START_FAILED}.
     * <p>
     * Dependency failures that occur after the notified failure do not result in new {@code dependencyFailed}
     * notifications, as the dependent will never receive two or more dependencyFailed calls in a row. A {@code
     * dependencyFailed} notification is only invoked again to notify of new failures if the previous failures have been
     * {@link #dependencyFailureCleared cleared}. 
     * <p> This method must not be called under a lock.
     */
    void dependencyFailed();

    /**
     * Notify this dependent that all dependency failures previously {@link #dependencyFailed() notified} are now
     * cleared. This method is called only after all affected dependencies left {@code START_FAILED} state.
     * <p> This method must not be called under a lock.
     */
    void dependencyFailureCleared();

    /**
     * Notify this dependent that one of its transitive dependencies is unavailable (either uninstalled, or in
     * {@link ServiceController.Mode#NEVER NEVER mode}).
     * <p>
     * New transitive dependencies that become unavailable after the notified one do not result in new {@code
     * dependencyUnavailable} notifications, as the dependent will never receive two or more dependencyUnavailable calls
     * in a row. A {@code dependencyUnavailable} notification is only invoked again to notify of newly found unavailable
     * dependencies if all the previously unavailable dependencies have become {@link #transitiveDependencyAvailable()
     * available}.
     * <p> This method must not be called under a lock.
     */
    void transitiveDependencyUnavailable();

    /**
     * Notify this dependent that all {@link #transitiveDependencyUnavailable() unavailable} transitive dependencies are
     * now available (i.e., they are installed and will perform an attempt to start shortly).
     * <p> This method must not be called under a lock.
     */
    void transitiveDependencyAvailable();

    /**
     * Get the controller of this dependent.
     *
     * @return the controller
     */
    ServiceControllerImpl<?> getController();
}