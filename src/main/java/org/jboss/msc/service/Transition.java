/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2022, Red Hat, Inc., and individual contributors
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
     * Transition from {@link Substate#DOWN DOWN} to {@link Substate#PROBLEM PROBLEM}.
     */
    DOWN_to_PROBLEM(Substate.DOWN, Substate.PROBLEM),
    /**
     * Transition from {@link Substate#DOWN START_REQUESTED} to {@link Substate#START_REQUESTED START_REQUESTED}.
     */
    DOWN_to_START_REQUESTED(Substate.DOWN, Substate.START_REQUESTED),
    /**
     * Transition from {@link Substate#PROBLEM PROBLEM} to {@link Substate#DOWN DOWN}.
     */
    PROBLEM_to_DOWN(Substate.PROBLEM, Substate.DOWN),
    /**
     * Transition from {@link Substate#START_REQUESTED START_REQUESTED} to {@link Substate#STARTING STARTING}.
     */
    START_REQUESTED_to_STARTING (Substate.START_REQUESTED, Substate.STARTING),
    /**
     * Transition from {@link Substate#STARTING STARTING} to {@link Substate#UP UP}.
     */
    STARTING_to_UP(Substate.STARTING, Substate.UP),
    /**
     * Transition from {@link Substate#STARTING STARTING} to {@link Substate#START_FAILED START_FAILED}.
     */
    STARTING_to_START_FAILED(Substate.STARTING, Substate.START_FAILED),
    /**
     * Transition from {@link Substate#START_FAILED START_FAILED} to {@link Substate#STARTING STARTING}.
     */
    START_FAILED_to_STARTING(Substate.START_FAILED, Substate.STARTING),
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
     * Transition from {@link Substate#DOWN DOWN} to {@link Substate#REMOVING REMOVING}.
     */
    DOWN_to_REMOVING(Substate.DOWN, Substate.REMOVING),
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
     * Get the string representation of this transition.
     *
     * @return the string
     */
    public String toString() {
        return "transition from " + before.name() + " to " + after.name();
    }
}
