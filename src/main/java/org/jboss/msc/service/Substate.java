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

/**
 * A fine-grained substate of the more general basic controller {@link ServiceController.State}s.  The list of possible
 * substates may change over time, so users should not rely on its permanence.
 * @deprecated this class will be removed in a future release
 */
@Deprecated
enum Substate {
    /**
     * New controller being installed.
     */
    NEW(ServiceController.State.DOWN, true),
    /**
     * Cancelled controller installation due to duplicate or other problem.
     */
    CANCELLED(ServiceController.State.REMOVED, true),
    /**
     * Controller is down.
     */
    DOWN(ServiceController.State.DOWN, true),
    /**
     * Controller is configured not to start.
     */
    WONT_START(ServiceController.State.DOWN, true),
    /**
     * Controller cannot start due to a problem with a dependency or transitive dependency.
     */
    PROBLEM(ServiceController.State.DOWN, true),
    /**
     * A stopped controller has been requested to start.
     */
    START_REQUESTED(ServiceController.State.DOWN, false),
    /**
     * First phase of start processing.
     */
    START_INITIATING(ServiceController.State.STARTING, false),
    /**
     * Second phase of start processing ({@link Service#start(StartContext) start()} method invoked).
     */
    STARTING(ServiceController.State.STARTING, false),
    /**
     * Start failed.
     */
    START_FAILED(ServiceController.State.START_FAILED, true),
    /**
     * Service is up.
     */
    UP(ServiceController.State.UP, true),
    /**
     * Service is up but has been requested to stop.
     */
    STOP_REQUESTED(ServiceController.State.UP, false),
    /**
     * Service is stopping.
     */
    STOPPING(ServiceController.State.STOPPING, false),
    /**
     * Service has been removed.
     */
    REMOVED(ServiceController.State.REMOVED, false),
    /**
     * Service has been terminated.
     */
    TERMINATED(ServiceController.State.REMOVED, true),
    ;
    private final ServiceController.State state;
    private final boolean restState;

    Substate(final ServiceController.State state, final boolean restState) {
        this.state = state;
        this.restState = restState;
    }

    /**
     * Determine whether this is a "rest" state.
     *
     * @return {@code true} if it is a rest state, {@code false} otherwise
     */
    public boolean isRestState() {
        return restState;
    }

    /**
     * Get the state corresponding to this sub-state.
     *
     * @return the state
     */
    public ServiceController.State getState() {
        return state;
    }

    /**
     * Determine if this substate is one of the given substates.
     *
     * @param substates the substates to check
     * @return {@code true} if this substate is in the set; {@code false} otherwise
     */
    public boolean in(Substate... substates) {
        for (Substate test : substates) {
            if (this == test) {
                return true;
            }
        }
        return false;
    }
}
