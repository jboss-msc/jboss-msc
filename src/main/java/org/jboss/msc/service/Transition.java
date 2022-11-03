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
 * A transition from one substate to another.
 */
enum Transition {
    // New transitions should be added to the end.
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
     * Transition from {@link Substate#REMOVING REMOVING} to {@link Substate#TERMINATED TERMINATED}.
     */
    REMOVING_to_TERMINATED(Substate.REMOVING, Substate.TERMINATED),
    /**
     * Transition from {@link Substate#DOWN DOWN} to {@link Substate#REMOVING REMOVING}.
     */
    DOWN_to_REMOVING(Substate.DOWN, Substate.REMOVING),
    /**
     * Transition from {@link Substate#DOWN DOWN} to {@link Substate#START_REQUESTED START_REQUESTED}.
     */
    DOWN_to_START_REQUESTED(Substate.DOWN, Substate.START_REQUESTED),
    /**
     * Transition from {@link Substate#NEW} to {@link Substate#DOWN}.
     */
    NEW_to_DOWN(Substate.NEW, Substate.DOWN);

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
