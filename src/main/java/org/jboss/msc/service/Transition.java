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
 * A transition from one substate to another.  The list of possible transitions may change over time, so users
 * should not rely on its permanence.
 * @deprecated this class will be removed in a future release
 */
@Deprecated
enum Transition {
    // New transitions should be added to the end.  Unused transitions should be retained as "deprecated" for
    // binary compatibility.
    /**
     * Transition from {@link ServiceController.Substate#START_REQUESTED START_REQUESTED} to {@link ServiceController.Substate#DOWN DOWN}.
     */
    START_REQUESTED_to_DOWN(ServiceController.Substate.START_REQUESTED, ServiceController.Substate.DOWN),
    /**
     * Transition from {@link ServiceController.Substate#START_REQUESTED START_REQUESTED} to {@link ServiceController.Substate#PROBLEM PROBLEM}.
     */
    START_REQUESTED_to_PROBLEM(ServiceController.Substate.START_REQUESTED, ServiceController.Substate.PROBLEM),
    /**
     * Transition from {@link ServiceController.Substate#START_REQUESTED START_REQUESTED} to {@link ServiceController.Substate#START_INITIATING START_INITIATING}.
     */
    START_REQUESTED_to_START_INITIATING(ServiceController.Substate.START_REQUESTED, ServiceController.Substate.START_INITIATING),
    /**
     * Transition from {@link ServiceController.Substate#PROBLEM PROBLEM} to {@link ServiceController.Substate#START_REQUESTED START_REQUESTED}.
     */
    PROBLEM_to_START_REQUESTED(ServiceController.Substate.PROBLEM, ServiceController.Substate.START_REQUESTED),
    /**
     * Transition from {@link ServiceController.Substate#START_INITIATING START_INITIATING} to {@link ServiceController.Substate#STARTING STARTING}.
     */
    START_INITIATING_to_STARTING (ServiceController.Substate.START_INITIATING, ServiceController.Substate.STARTING),
    /**
     * Transition from {@link ServiceController.Substate#START_INITIATING START_INITIATING} to {@link ServiceController.Substate#START_REQUESTED START_REQUESTED}.
     */
    START_INITIATING_to_START_REQUESTED (ServiceController.Substate.START_INITIATING, ServiceController.Substate.START_REQUESTED),
    /**
     * Transition from {@link ServiceController.Substate#STARTING STARTING} to {@link ServiceController.Substate#UP UP}.
     */
    STARTING_to_UP(ServiceController.Substate.STARTING, ServiceController.Substate.UP),
    /**
     * Transition from {@link ServiceController.Substate#STARTING STARTING} to {@link ServiceController.Substate#START_FAILED START_FAILED}.
     */
    STARTING_to_START_FAILED(ServiceController.Substate.STARTING, ServiceController.Substate.START_FAILED),
    /**
     * Transition from {@link ServiceController.Substate#START_FAILED START_FAILED} to {@link ServiceController.Substate#START_INITIATING START_INITIATING}.
     */
    START_FAILED_to_STARTING(ServiceController.Substate.START_FAILED, ServiceController.Substate.START_INITIATING),
    /**
     * Transition from {@link ServiceController.Substate#START_FAILED START_FAILED} to {@link ServiceController.Substate#DOWN DOWN}.
     */
    START_FAILED_to_DOWN(ServiceController.Substate.START_FAILED, ServiceController.Substate.DOWN),
    /**
     * Transition from {@link ServiceController.Substate#UP UP} to {@link ServiceController.Substate#STOP_REQUESTED STOP_REQUESTED}.
     */
    UP_to_STOP_REQUESTED(ServiceController.Substate.UP, ServiceController.Substate.STOP_REQUESTED),
    /**
     * Transition from {@link ServiceController.Substate#STOP_REQUESTED STOP_REQUESTED} to {@link ServiceController.Substate#UP UP}.
     */
    STOP_REQUESTED_to_UP(ServiceController.Substate.STOP_REQUESTED, ServiceController.Substate.UP),
    /**
     * Transition from {@link ServiceController.Substate#STOP_REQUESTED STOP_REQUESTED} to {@link ServiceController.Substate#STOPPING STOPPING}.
     */
    STOP_REQUESTED_to_STOPPING(ServiceController.Substate.STOP_REQUESTED, ServiceController.Substate.STOPPING),
    /**
     * Transition from {@link ServiceController.Substate#STOPPING STOPPING} to {@link ServiceController.Substate#DOWN DOWN}.
     */
    STOPPING_to_DOWN(ServiceController.Substate.STOPPING, ServiceController.Substate.DOWN),
    /**
     * Transition from {@link ServiceController.Substate#REMOVING REMOVING} to {@link ServiceController.Substate#REMOVED REMOVED}.
     */
    REMOVING_to_REMOVED(ServiceController.Substate.REMOVING, ServiceController.Substate.REMOVED),
    /**
     * Transition from {@link ServiceController.Substate#REMOVED REMOVED} to {@link ServiceController.Substate#TERMINATED TERMINATED}.
     */
    REMOVED_to_TERMINATED(ServiceController.Substate.REMOVED, ServiceController.Substate.TERMINATED),
    /**
     * Transition from {@link ServiceController.Substate#REMOVING REMOVING} to {@link ServiceController.Substate#DOWN DOWN}.
     * @deprecated was never supposed to work
     */
    @Deprecated
    REMOVING_to_DOWN(ServiceController.Substate.REMOVING, ServiceController.Substate.DOWN),
    /**
     * Transition from {@link ServiceController.Substate#DOWN DOWN} to {@link ServiceController.Substate#REMOVING REMOVING}.
     */
    DOWN_to_REMOVING(ServiceController.Substate.DOWN, ServiceController.Substate.REMOVING),
    /**
     * Transition from {@link ServiceController.Substate#DOWN DOWN} to {@link ServiceController.Substate#START_REQUESTED START_REQUESTED}.
     */
    DOWN_to_START_REQUESTED(ServiceController.Substate.DOWN, ServiceController.Substate.START_REQUESTED),
    /**
     * Transition from {@link ServiceController.Substate#DOWN DOWN} to {@link ServiceController.Substate#WAITING WAITING}.
     */
    DOWN_to_WAITING(ServiceController.Substate.DOWN, ServiceController.Substate.WAITING),
    /**
     * Transition from {@link ServiceController.Substate#DOWN DOWN} to {@link ServiceController.Substate#WONT_START WONT_START}.
     */
    DOWN_to_WONT_START(ServiceController.Substate.DOWN, ServiceController.Substate.WONT_START),
    /**
     * Transition from {@link ServiceController.Substate#WAITING WAITING} to {@link ServiceController.Substate#DOWN DOWN}.
     */
    WAITING_to_DOWN(ServiceController.Substate.WAITING, ServiceController.Substate.DOWN),
    /**
     * Transition from {@link ServiceController.Substate#WONT_START WONT_START} to {@link ServiceController.Substate#DOWN DOWN}.
     */
    WONT_START_to_DOWN(ServiceController.Substate.WONT_START, ServiceController.Substate.DOWN),
    /**
     * Transition from {@link ServiceController.Substate#CANCELLED} to {@link ServiceController.Substate#REMOVED}.
     */
    CANCELLED_to_REMOVED(ServiceController.Substate.CANCELLED, ServiceController.Substate.REMOVED),
    /**
     * Transition from {@link ServiceController.Substate#NEW} to {@link ServiceController.Substate#DOWN}.
     */
    NEW_to_DOWN(ServiceController.Substate.NEW, ServiceController.Substate.DOWN);

    private final ServiceController.Substate before;
    private final ServiceController.Substate after;

    Transition(final ServiceController.Substate before, final ServiceController.Substate after) {
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
    public boolean enters(ServiceController.State state) {
        return before.getState() != state && after.getState() == state;
    }

    /**
     * Determine whether this transition causes exit from the given state.
     *
     * @param state the state
     * @return {@code true} if the state is exited by this transition
     */
    public boolean exits(ServiceController.State state) {
        return before.getState() == state && after.getState() != state;
    }

    /**
     * Determine whether this substate transition retains the same given state before and after transition.
     *
     * @param state the state
     * @return {@code true} if the state is retained
     */
    public boolean retains(ServiceController.State state) {
        return before.getState() == state && after.getState() == state;
    }

    /**
     * Get the source state of this transition.
     *
     * @return the source state
     */
    public ServiceController.Substate getBefore() {
        return before;
    }

    /**
     * Get the target (new) state of this transition.
     *
     * @return the target state
     */
    public ServiceController.Substate getAfter() {
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
