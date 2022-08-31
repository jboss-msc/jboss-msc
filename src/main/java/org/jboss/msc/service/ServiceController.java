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

import java.util.Collection;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.jboss.msc.value.Value;

/**
 * A controller for a single service instance.
 *
 * @param <S> the service type
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
public interface ServiceController<S> extends Value<S> {

    /**
     * Get this service's parent service, or {@code null} if there is none.
     *
     * @return the parent service or {@code null} if this service has no parent
     */
    ServiceController<?> getParent();

    /**
     * Get the service container associated with this controller.
     *
     * @return the container
     */
    ServiceContainer getServiceContainer();

    /**
     * Get the service controller's current mode.
     *
     * @return the controller mode
     */
    Mode getMode();

    /**
     * Compare the current mode against {@code expected}; if it matches, change it to {@code newMode}.  The
     * return value is {@code true} when the mode was matched and changed.
     *
     * @param expected the expected mode
     * @param newMode the new mode
     * @return {@code true} if the mode was changed
     */
    boolean compareAndSetMode(Mode expected, Mode newMode);

    /**
     * Change the service controller's current mode.  Might result in the service starting or stopping.  The mode
     * may only be changed if it was not already set to {@link Mode#REMOVE}.  Calling this method with the controller's
     * current mode has no effect and is always allowed.
     *
     * @param mode the new controller mode
     * @throws IllegalStateException if the mode given is {@code null}, or the caller attempted to change the
     *  service's mode from {@link Mode#REMOVE} to a different mode
     */
    void setMode(Mode mode);

    /**
     * Get the current service controller state.
     *
     * @return the current state
     */
    State getState();

    /**
     * Get the service value.
     *
     * @return the service value
     * @throws IllegalStateException if the service is not available (i.e. it is not up)
     * @deprecated this method will be removed in a future release
     */
    @Deprecated
    S getValue() throws IllegalStateException;

    /**
     * Wait for a service to come up, and then return its value.
     *
     * @return the service value
     * @throws IllegalStateException if the service is not available (i.e. it was removed or failed)
     * @throws InterruptedException if the wait operation was interrupted
     * @deprecated this method will be removed in a future release
     */
    @Deprecated
    S awaitValue() throws IllegalStateException, InterruptedException;

    /**
     * Wait for a service to come up for a certain amount of time, and then return its value.
     *
     * @param time the amount of time to wait
     * @param unit the unit of time to wait
     * @return the service value
     * @throws IllegalStateException if the service is not available (i.e. it was removed or failed)
     * @throws InterruptedException if the wait operation was interrupted
     * @deprecated this method will be removed in a future release
     */
    @Deprecated
    S awaitValue(long time, TimeUnit unit) throws IllegalStateException, InterruptedException, TimeoutException;

    /**
     * Get the service.
     *
     * @return the service
     * @throws IllegalStateException if the service is not available (i.e. it is not up)
     * @deprecated this method will be removed in a future release
     */
    @Deprecated
    Service<S> getService() throws IllegalStateException;

    /**
     * Get the name of this service, if any.
     *
     * @return the name, or {@code null} if none was specified.
     */
    ServiceName getName();

    /**
     * Get other names this service is known as.
     *
     * @return the aliases
     */
    ServiceName[] getAliases();

    /**
     * Add a service lifecycle listener.
     *
     * @param listener the lifecycle listener
     */
    void addListener(LifecycleListener listener);

    /**
     * Remove a lifecycle listener.
     *
     * @param listener the lifecycle listener to remove
     */
    void removeListener(LifecycleListener listener);

    /**
     * Get the reason why the last start failed.
     *
     * @return the last start exception, or {@code null} if the last start succeeded or the service has not yet started
     */
    StartException getStartException();

    /**
     * Retry a failed service.  Does nothing if the state is not {@link State#START_FAILED}.
     */
    void retry();

    /**
     * Get the complete list of dependencies that are unavailable.
     *
     * @return a set containing the names of all unavailable dependencies
     */
    Collection<ServiceName> getUnavailableDependencies();

    /**
     * A possible state for a service controller.
     */
    enum State {

        /**
         * Down.  All dependents are down.
         */
        DOWN,
        /**
         * Service is starting.  Dependencies may not enter the {@code DOWN} state.  This state may not be left until
         * the {@code start} method has finished or failed.
         */
        STARTING,
        /**
         * Start failed, or was cancelled.  From this state, the start may be retried or the service may enter the
         * {@code DOWN} state.
         */
        START_FAILED,
        /**
         * Up.
         */
        UP,
        /**
         * Service is stopping.  Dependents may not enter the {@code STARTING} state.  This state may not be left until
         * all dependents are {@code DOWN} and the {@code stop} method has finished.
         */
        STOPPING,
        /**
         * Service was removed from the container.
         */
        REMOVED,
        ;

        /**
         * Determine if this state is one of the given states.
         *
         * @param states the states to check
         * @return {@code true} if this state is in the set; {@code false} otherwise
         */
        public boolean in(State... states) {
            for (State test : states) {
                if (this == test) {
                    return true;
                }
            }
            return false;
        }
    }

    /**
     * The controller mode for a service.
     */
    enum Mode {

        /**
         * Remove this service and all of its dependents.
         */
        REMOVE,
        /**
         * Do not start; in addition, ignore demands from dependents.
         */
        NEVER,
        /**
         * Only come up if all dependencies are satisfied <b>and</b> at least one dependent demands to start.
         */
        ON_DEMAND,
        /**
         * Only come up if all dependencies are satisfied <b>and</b> at least one dependent demands to start.
         * Once in the {@link State#UP UP} state, it will remain that way regardless of demands from dependents.
         */
        LAZY,
        /**
         * Come up automatically as soon as all dependencies are satisfied.
         */
        PASSIVE,
        /**
         * Demand to start, recursively demanding dependencies.  This is the default mode.
         */
        ACTIVE,
        ;

        /**
         * Determine if this mode is one of the given modes.
         *
         * @param modes the modes to check
         * @return {@code true} if this mode is in the set; {@code false} otherwise
         */
        public boolean in(Mode... modes) {
            for (Mode test : modes) {
                if (this == test) {
                    return true;
                }
            }
            return false;
        }
    }

}
