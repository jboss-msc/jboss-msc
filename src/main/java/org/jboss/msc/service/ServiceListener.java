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
     * The (currently DOWN) service was requested to start.
     *
     * @param controller the controller
     */
    void serviceStartRequested(ServiceController<? extends S> controller);

    /**
     * The (currently DOWN) service was requested to not start after all.
     *
     * @param controller the controller
     */
    void serviceStartRequestCleared(ServiceController<? extends S> controller);

    /**
     * The service is starting.  Called after the state transitions from {@code DOWN} to {@code STARTING}.
     *
     * @param controller the controller
     */
    void serviceStarting(ServiceController<? extends S> controller);

    /**
     * The service is starting after a failure.  Called after the state transitions from {@code START_FAILED} to {@code STARTING}.
     *
     * @param controller the controller
     */
    void failedServiceStarting(final ServiceController<? extends S> controller);

    /**
     * The service is started (up).  Called after the state transitions from {@code STARTING} to {@code UP}.
     *
     * @param controller the controller
     */
    void serviceStarted(ServiceController<? extends S> controller);

    /**
     * The service start has failed.  Called after the state transitions from {@code STARTING} to {@code START_FAILED}.
     *
     * @param controller the controller
     * @param reason the reason for failure
     */
    void serviceFailed(ServiceController<? extends S> controller, StartException reason);

    /**
     * The (currently UP) service was requested to stop.
     *
     * @param controller the controller
     */
    void serviceStopRequested(ServiceController<? extends S> controller);

    /**
     * The (currently UP) service was requested to not stop after all.
     *
     * @param controller the controller
     */
    void serviceStopRequestCleared(ServiceController<? extends S> controller);

    /**
     * The service is stopping.  Called after the state transitions from {@code UP} to {@code STOPPING}.
     *
     * @param controller the controller
     */
    void serviceStopping(ServiceController<? extends S> controller);

    /**
     * The service is stopped (down).  Called after the state transitions from {@code STOPPING} to {@code DOWN}.
     *
     * @param controller the controller
     */
    void serviceStopped(ServiceController<? extends S> controller);

    /**
     * The (failed) service is stopped (down).  Called after the state transitions from {@code START_FAILED} to {@code DOWN}.
     *
     * @param controller the controller
     */
    void failedServiceStopped(ServiceController<? extends S> controller);

    /**
     * The service is waiting for its dependencies to be {@link ServiceController.State#UP UP}.<P> Only services  in {@link
     * ServiceController.Mode#PASSIVE PASSIVE} and {@link ServiceController.Mode#ON_DEMAND ON_DEMAND} mode wait
     * for dependencies to start before being {@link #serviceStartRequested(ServiceController) requested to start}. For
     * all the other modes, a service will be requested to start as soon as it is installed, regardless of its
     * dependencies status. If one of the dependencies have any blocking issues preventing it from start, the service will notify a {@link
     * #dependencyProblem(ServiceController) dependency problem} instead of {@code serviceWaiting}.
     *
     * @param controller the controller
     */
    void serviceWaiting(ServiceController<? extends S> controller);

    /**
     * The service is no longer waiting for its dependencies to be {@link ServiceController.State#UP UP}.<p>This
     * notification can be sent in two occasions. The first one is when all down dependencies have started. A service
     * will also notify it is no longer waiting for its dependencies whenever its mode is set to a value different than
     * {@link ServiceController.Mode#PASSIVE PASSIVE} and {@link ServiceController.Mode#ON_DEMAND ON_DEMAND}.
     *
     * @param controller the controller
     */
    void serviceWaitingCleared(ServiceController<? extends S> controller);

    /**
     * The service is on {@link ServiceController.Mode.NEVER NEVER} mode and hence will not start.
     *
     * @param controller the controller
     */
    void serviceWontStart(ServiceController<? extends S> controller);

    /**
     * The service is no longer on {@link ServiceController.Mode.NEVER NEVER} mode.
     *
     * @param controller the controller
     */
    void serviceWontStartCleared(ServiceController<? extends S> controller);

    /**
     * The service is going to be removed.  Called when the service mode is changed to {@code REMOVE}.
     *
     * @param controller the controller
     */
    void serviceRemoveRequested(ServiceController<? extends S> controller);

    /**
     * The service removal is canceled. Called when the service mode is changed from {@code REMOVE} to any other
     * mode.
     * Such a mode change can only be succesfully performed if {@code setMode} is called before the service is removed.
     * 
     * @param controller the controller.
     */
    void serviceRemoveRequestCleared(ServiceController<? extends S> controller);

    /**
     * The service has been removed.  The listener will automatically be unregistered after this call.  Called
     * after the state transitions from {@code DOWN} to {@code REMOVED}.
     *
     * @param controller the controller
     */
    void serviceRemoved(ServiceController<? extends S> controller);

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
     * A problem involving a service dependency occurred.
     * The possible dependency problems are: {@link #dependencyFailed(ServiceController) start failures}, and
     * unavailability (both {@link #immediateDependencyUnavailable(ServiceController) immediate} and {@link
     * #transitiveDependencyUnavailable(ServiceController) transitive)}.
     * <p>Dependency problems that are occur after this notification do not result in new {@code
     * dependencyProblem} notifications. A new call to this method will only be made to notify newly found
     * dependency problems if the previous problems have all been {@link #dependencyProblemCleared(ServiceController)
     * cleared}.
     * 
     * @param controller the controller
     */
    void dependencyProblem(ServiceController<? extends S> controller);

    /**
     * All dependency problems are now cleared.
     * <br>This method will be invoked only after {@link #dependencyProblem(ServiceController)} is called.
     * 
     * @param controller the controller
     */
    void dependencyProblemCleared(ServiceController<? extends S> controller);
}
