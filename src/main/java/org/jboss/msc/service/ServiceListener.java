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
 * A listener for service lifecycle events. The associated controller will not leave its current state until
 * all listeners finish running.
 *
 * @param <S> the service type
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 * @author <a href="mailto:flavia.rainone@jboss.com">Flavia Rainone</a>
 */
public interface ServiceListener<S> {

    /**
     * The listener has been added to a controller.
     *
     * @param controller the controller that this listener was added to
     */
    void listenerAdded(ServiceController<? extends S> controller);

    /**
     * The service has transitioned to a new sub-state.
     *
     * @param controller the controller
     * @param transition the transition that occurred
     */
    void transition(ServiceController<? extends S> controller, ServiceController.Transition transition);


    /**
     * The service is going to be removed.  Called when the service mode is changed to {@code REMOVE}.
     *
     * @param controller the controller
     */
    void serviceRemoveRequested(ServiceController<? extends S> controller);

    /**
     * The service removal is canceled. Called when the service mode is changed from {@code REMOVE} to any other
     * mode.
     * Such a mode change can only be successfully performed if {@code setMode} is called before the service is removed.
     *
     * @param controller the controller.
     */
    void serviceRemoveRequestCleared(ServiceController<? extends S> controller);

    /**
     * A dependency of the service has failed. Called after the dependency state transitions from {@code STARTING} to {@code START_FAILED}.
     * <p> Dependency failures that occur after the notified failure do not result in new {@code dependencyFailed}
     * notifications. A new call to this method will be made to notify new failures only if the previous failures have
     * been {@link #dependencyFailureCleared(ServiceController) cleared}.
     * <p> This method is invoked to notify both immediate and transitive dependency failures.
     *
     * @param controller the controller
     */
    void dependencyFailed(ServiceController<? extends S> controller);

    /**
     * A dependency of the service is retrying to start. Called after the dependency state transitions from {@code START_FAILED} to {@code STARTING}.
     *
     * @param controller the controller
     */
    void dependencyFailureCleared(ServiceController<? extends S> controller);

    /**
     * An immediate dependency of the service is uninstalled or administratively {@link ServiceController.Mode#NEVER
     * disabled}.
     * <p> Immediate dependencies that are subsequently unavailable do not result in new {@code
     * immediateDependencyUnavailable} notifications. A new call to this method will only be made to notify newly found
     * unavailable dependencies if the previously unavailable dependencies have been {@link
     * #immediateDependencyAvailable(ServiceController) cleared}.
     *
     * @param controller the controller
     */
    void immediateDependencyUnavailable(ServiceController<? extends S> controller);

    /**
     * All {@link #immediateDependencyUnavailable(ServiceController) unavailable} immediate dependencies of the service
     * are now available, i.e., they are installed and are not administratively  {@link ServiceController.Mode#NEVER
     * disabled}.
     * <br>This method will be invoked only after {@link #immediateDependencyUnavailable(ServiceController)} is called.
     *
     * @param controller the controller
     */
    void immediateDependencyAvailable(ServiceController<? extends S> controller);

    /**
     * A transitive dependency of the service is uninstalled or administratively {@link ServiceController.Mode#NEVER
     * disabled}.
     * <p> Transitive dependencies that are subsequently unavailable do not result in new {@code
     * transitiveDependencyUnavailable} notifications. A new call to this method will only be made to notify newly found
     * unavailable dependencies if the previously unavailable dependencies have all become {@link
     * #transitiveDependencyAvailable(ServiceController) available}.
     *
     * @param controller the controller
     */
    void transitiveDependencyUnavailable(ServiceController<? extends S> controller);

    /**
     * All {@link #transitiveDependencyUnavailable(ServiceController) unavailable} transitive dependencies of the
     * service are now available (installed and not administratively {@link ServiceController.Mode#NEVER disabled}).
     * <br>This method will be invoked only after {@link #transitiveDependencyUnavailable(ServiceController)} is called.
     *
     * @param controller the controller
     */
    void transitiveDependencyAvailable(ServiceController<? extends S> controller);

    /**
     * The inheritance type for a listener.
     *
     * @deprecated Listeners inherently degrade performance.  Inheritance only compounds the problem.  Ultimately listener inheritance will be eliminated.
     */
    @Deprecated
    enum Inheritance {
        /**
         * This listener is never inherited.
         */
        NONE,
        /**
         * This listener is inherited only to one level.
         */
        ONCE,
        /**
         * This listener is inherited to all levels.
         */
        ALL,
        ;
    }
}