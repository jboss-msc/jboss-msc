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
     * Notify this dependent that one of its immediate dependencies is installed.
     * <p> This method must not be called under a lock.
     */
    void immediateDependencyInstalled();

    /**
     * Notify this dependent that one of its immediate dependencies is uninstalled.
     * <p> This method must not be called under a lock.
     */
    void immediateDependencyUninstalled();

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
     * Notify this dependent that one of its transitive dependencies is uninstalled or missing.
     * <p>
     * Dependencies that are uninstalled after the notified one do not result in new {@code dependencyUninstalled}
     * notifications, as the dependent will never receive two or more dependencyUninstalled calls in a row. A {@code
     * dependencyUninstall} notification is only invoked again to notify of newly found uninstalled dependencies if the
     * previously missing dependencies have been {@link #dependencyInstalled() installed}.
     * <p> This method must not be called under a lock.
     */
    void dependencyUninstalled();

    /**
     * Notify this dependent that all {@link #dependencyUninstalled() uninstalled} transitive dependencies are now
     * installed.
     * <p> This method must not be called under a lock.
     */
    void dependencyInstalled();
}