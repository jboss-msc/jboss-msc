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

    /**
     * A fine-grained substate of the more general basic controller {@link State}s.  The list of possible
     * substates may change over time, so users should not rely on its permanence.
     * @deprecated this class will be removed in a future release
     */
    @Deprecated
    enum Substate {
        /**
         * New controller being installed.
         */
        NEW(State.DOWN, true),
        /**
         * Cancelled controller installation due to duplicate or other problem.
         */
        CANCELLED(State.REMOVED, true),
        /**
         * Controller is down.
         */
        DOWN(State.DOWN, false),
        /**
         * Controller is waiting for an external condition to start, such as a dependent demand.
         */
        WAITING(State.DOWN, true),
        /**
         * Controller is configured not to start.
         */
        WONT_START(State.DOWN, true),
        /**
         * Controller cannot start due to a problem with a dependency or transitive dependency.
         */
        PROBLEM(State.DOWN, true),
        /**
         * A stopped controller has been requested to start.
         */
        START_REQUESTED(State.DOWN, false),
        /**
         * First phase of start processing.
         */
        START_INITIATING(State.STARTING, false),
        /**
         * Second phase of start processing ({@link Service#start(StartContext) start()} method invoked).
         */
        STARTING(State.STARTING, false),
        /**
         * Start failed.
         */
        START_FAILED(State.START_FAILED, true),
        /**
         * Service is up.
         */
        UP(State.UP, true),
        /**
         * Service is up but has been requested to stop.
         */
        STOP_REQUESTED(State.UP, false),
        /**
         * Service is stopping.
         */
        STOPPING(State.STOPPING, false),
        /**
         * Service is being removed.
         */
        REMOVING(State.DOWN, false),
        /**
         * Service has been removed.
         */
        REMOVED(State.REMOVED, false),
        /**
         * Service has been terminated.
         */
        TERMINATED(State.REMOVED, true),
        ;
        private final State state;
        private final boolean restState;

        Substate(final State state, final boolean restState) {
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
        public State getState() {
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

    /**
     * A transition from one substate to another.  The list of possible transitions may change over time, so users
     * should not rely on its permanence.
     * @deprecated this class will be removed in a future release
     */
    @Deprecated
    enum Transition {
        // New transitions should be added to the end.  Unused transitions should be retained as "deprecated" for
        // binary compatibility.
        /**
         * Transition from {@link Substate#START_REQUESTED START_REQUESTED} to {@link Substate#DOWN DOWN}.
         */
        START_REQUESTED_to_DOWN(Substate.START_REQUESTED, Substate.DOWN),
        /**
         * Transition from {@link Substate#START_REQUESTED START_REQUESTED} to {@link Substate#PROBLEM PROBLEM}.
         */
        START_REQUESTED_to_PROBLEM(Substate.START_REQUESTED, Substate.PROBLEM),
        /**
         * Transition from {@link Substate#START_REQUESTED START_REQUESTED} to {@link Substate#START_INITIATING START_INITIATING}.
         */
        START_REQUESTED_to_START_INITIATING(Substate.START_REQUESTED, Substate.START_INITIATING),
        /**
         * Transition from {@link Substate#PROBLEM PROBLEM} to {@link Substate#START_REQUESTED START_REQUESTED}.
         */
        PROBLEM_to_START_REQUESTED(Substate.PROBLEM, Substate.START_REQUESTED),
        /**
         * Transition from {@link Substate#START_INITIATING START_INITIATING} to {@link Substate#STARTING STARTING}.
         */
        START_INITIATING_to_STARTING (Substate.START_INITIATING, Substate.STARTING),
        /**
         * Transition from {@link Substate#START_INITIATING START_INITIATING} to {@link Substate#START_REQUESTED START_REQUESTED}.
         */
        START_INITIATING_to_START_REQUESTED (Substate.START_INITIATING, Substate.START_REQUESTED),
        /**
         * Transition from {@link Substate#STARTING STARTING} to {@link Substate#UP UP}.
         */
        STARTING_to_UP(Substate.STARTING, Substate.UP),
        /**
         * Transition from {@link Substate#STARTING STARTING} to {@link Substate#START_FAILED START_FAILED}.
         */
        STARTING_to_START_FAILED(Substate.STARTING, Substate.START_FAILED),
        /**
         * Transition from {@link Substate#START_FAILED START_FAILED} to {@link Substate#START_INITIATING START_INITIATING}.
         */
        START_FAILED_to_STARTING(Substate.START_FAILED, Substate.START_INITIATING),
        /**
         * Transition from {@link Substate#START_FAILED START_FAILED} to {@link Substate#DOWN DOWN}.
         */
        START_FAILED_to_DOWN(Substate.START_FAILED, Substate.DOWN),
        /**
         * Transition from {@link Substate#UP UP} to {@link Substate#STOP_REQUESTED STOP_REQUESTED}.
         */
        UP_to_STOP_REQUESTED(Substate.UP, Substate.STOP_REQUESTED),
        /**
         * Transition from {@link Substate#STOP_REQUESTED STOP_REQUESTED} to {@link Substate#UP UP}.
         */
        STOP_REQUESTED_to_UP(Substate.STOP_REQUESTED, Substate.UP),
        /**
         * Transition from {@link Substate#STOP_REQUESTED STOP_REQUESTED} to {@link Substate#STOPPING STOPPING}.
         */
        STOP_REQUESTED_to_STOPPING(Substate.STOP_REQUESTED, Substate.STOPPING),
        /**
         * Transition from {@link Substate#STOPPING STOPPING} to {@link Substate#DOWN DOWN}.
         */
        STOPPING_to_DOWN(Substate.STOPPING, Substate.DOWN),
        /**
         * Transition from {@link Substate#REMOVING REMOVING} to {@link Substate#REMOVED REMOVED}.
         */
        REMOVING_to_REMOVED(Substate.REMOVING, Substate.REMOVED),
        /**
         * Transition from {@link Substate#REMOVED REMOVED} to {@link Substate#TERMINATED TERMINATED}.
         */
        REMOVED_to_TERMINATED(Substate.REMOVED, Substate.TERMINATED),
        /**
         * Transition from {@link Substate#REMOVING REMOVING} to {@link Substate#DOWN DOWN}.
         * @deprecated was never supposed to work
         */
        @Deprecated
        REMOVING_to_DOWN(Substate.REMOVING, Substate.DOWN),
        /**
         * Transition from {@link Substate#DOWN DOWN} to {@link Substate#REMOVING REMOVING}.
         */
        DOWN_to_REMOVING(Substate.DOWN, Substate.REMOVING),
        /**
         * Transition from {@link Substate#DOWN DOWN} to {@link Substate#START_REQUESTED START_REQUESTED}.
         */
        DOWN_to_START_REQUESTED(Substate.DOWN, Substate.START_REQUESTED),
        /**
         * Transition from {@link Substate#DOWN DOWN} to {@link Substate#WAITING WAITING}.
         */
        DOWN_to_WAITING(Substate.DOWN, Substate.WAITING),
        /**
         * Transition from {@link Substate#DOWN DOWN} to {@link Substate#WONT_START WONT_START}.
         */
        DOWN_to_WONT_START(Substate.DOWN, Substate.WONT_START),
        /**
         * Transition from {@link Substate#WAITING WAITING} to {@link Substate#DOWN DOWN}.
         */
        WAITING_to_DOWN(Substate.WAITING, Substate.DOWN),
        /**
         * Transition from {@link Substate#WONT_START WONT_START} to {@link Substate#DOWN DOWN}.
         */
        WONT_START_to_DOWN(Substate.WONT_START, Substate.DOWN),
        /**
         * Transition from {@link Substate#CANCELLED} to {@link Substate#REMOVED}.
         */
        CANCELLED_to_REMOVED(Substate.CANCELLED,Substate.REMOVED),
        /**
         * Transition from {@link Substate#NEW} to {@link Substate#DOWN}.
         */
        NEW_to_DOWN(Substate.NEW,Substate.DOWN);

        private final Substate before;
        private final Substate after;

        Transition(final Substate before, final Substate after) {
            this.before = before;
            this.after = after;
        }

        /**
         * Determine whether this transition causes movement from a rest state to a non-rest state.
         *
         * @return {@code true} if this transition leaves a rest state
         */
        public boolean leavesRestState() {
            return before.isRestState() && ! after.isRestState();
        }

        /**
         * Determine whether this transition causes movement from a non-rest state to a rest state.
         *
         * @return {@code true} if this transition enters a rest state
         */
        public boolean entersRestState() {
            return ! before.isRestState() && after.isRestState();
        }

        /**
         * Determine whether this transition causes entry into the given state.
         *
         * @param state the state
         * @return {@code true} if the state is entered by this transition
         */
        public boolean enters(State state) {
            return before.getState() != state && after.getState() == state;
        }

        /**
         * Determine whether this transition causes exit from the given state.
         *
         * @param state the state
         * @return {@code true} if the state is exited by this transition
         */
        public boolean exits(State state) {
            return before.getState() == state && after.getState() != state;
        }

        /**
         * Determine whether this substate transition retains the same given state before and after transition.
         *
         * @param state the state
         * @return {@code true} if the state is retained
         */
        public boolean retains(State state) {
            return before.getState() == state && after.getState() == state;
        }

        /**
         * Get the source state of this transition.
         *
         * @return the source state
         */
        public Substate getBefore() {
            return before;
        }

        /**
         * Get the target (new) state of this transition.
         *
         * @return the target state
         */
        public Substate getAfter() {
            return after;
        }

        /**
         * Determine if this transition is one of the given transitions.
         *
         * @param transitions the transitions to check
         * @return {@code true} if this transition is in the set; {@code false} otherwise
         */
        public boolean in(Transition... transitions) {
            for (Transition test : transitions) {
                if (this == test) {
                    return true;
                }
            }
            return false;
        }

        /**
         * Get the string representation of this transition.
         *
         * @return the string
         */
        public String toString() {
            return "transition from " + before.name() + " to " + after.name();
        }
    }
}
