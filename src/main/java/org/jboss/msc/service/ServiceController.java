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

import org.jboss.msc.value.Value;

/**
 *
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
     * Add a service listener.  The method corresponding to the current service state is called.
     *
     * @param serviceListener the service listener
     */
    void addListener(ServiceListener<? super S> serviceListener);

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
     * Demand that this service start immediately.  If the mode is {@code IMMEDIATE}, this has no effect.  If the mode
     * is {@code AUTOMATIC} or {@code ON_DEMAND}, then until this handle is closed, the service will act as though it
     * has a mode of {@code IMMEDIATE}.  If the mode is {@code NEVER}, then until the mode changes, this method will
     * have no effect.
     */
    Handle<S> demand();

    /**
     * A possible state for a service controller.
     */
    enum State {

        /**
         * Down.  All dependents are down.  This state may not be left until all dependencies are {@code UP}.
         * Dependents may not enter the {@code STARTING} state.
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

    /**
     * A handle to demand a service start.
     */
    interface Handle<S> extends Value<ServiceController<S>> {
        void close();
    }
}
