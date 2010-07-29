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

import org.jboss.msc.value.Value;

/**
 * A controller for a single service instance.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public interface ServiceController<S> extends Value<S> {

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
     * Change the service controller's current mode.  Might result in the service starting or stopping.
     *
     * @param mode the new controller mode
     */
    void setMode(Mode mode);

    /**
     * Get the current service state.
     *
     * @return the current state
     */
    State getState();

    /**
     * Get the service.
     *
     * @return the service
     * @throws IllegalStateException if the service is not available
     */
    S getValue() throws IllegalStateException;

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
     * Add a service listener.  The method corresponding to the current service state is called.
     *
     * @param serviceListener the service listener
     */
    void addListener(ServiceListener<? super S> serviceListener);

    /**
     * Remove a service listener.
     *
     * @param serviceListener the service listener to remove
     */
    void removeListener(ServiceListener<? super S> serviceListener);

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
         * Removed from the container.
         */
        REMOVED,
        ;
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
         * Come up automatically as soon as all dependencies are satisfied.  This is the default mode.
         */
        AUTOMATIC,
        /**
         * Demand to start, recursively demanding dependencies.
         */
        IMMEDIATE,
        ;
    }
}
