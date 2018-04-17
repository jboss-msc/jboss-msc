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

/**
 * The service container implementation itself.  The service container is what coordinates the registry of services and
 * manages their installation and execution.  To create a service container, see {@link org.jboss.msc.service.ServiceContainer.Factory#create()}.
 * To create services, implement the {@link org.jboss.msc.service.Service} interface.
 * <p>
 * Individual service instances are controlled using the {@link org.jboss.msc.service.ServiceController} interface.  Service controllers follow
 * this strict internal state machine:
 * <p>
 * <img src="doc-files/state-machine.svg" alt="State machine image">
 * <p>
 * The green boxes represent states; the red boxes below them represent possible transitions.  The "When:" condition
 * must be satisfied before a transition is taken; if it is, then the asynchronous tasks listed under "Tasks:" are
 * executed.
 * <p>
 * The variables are what determine when a transition may occur; any time a variable is changed, the conditions are
 * checked to see if a transition can occur.  The variables are as follows:
 * <ul>
 * <li><em>A</em>: The number of currently running asynchronous tasks.</li>
 * <li><em>R</em>: The number of running children (dependents).</li>
 * <li><em>D</em>: The number of "demands" from children, used to trigger the start of {@code ON_DEMAND} services.  If greater than zero,
 * a single "demand" is propagated to the dependency set (parents) of this service.</li>
 * <li><em>U</em>: The count, from zero, representing the desire of the service to be "up".  Only services with a positive "up" count will start.</li>
 * <li><em>X</em>: The exception produced by the service {@code start()} method, if any.</li>
 * <li><em>MODE</em>: The controller start mode.  Values can be one of:<ul>
 * <li>{@code ACTIVE} - attempt to start immediately, and request all parents (dependencies) to start as well by incrementing
 * their "demand" count (<em>D</em>).  Puts a load of {@code +1} on <em>U</em> always.</li>
 * <li>{@code PASSIVE} - attempt to start immediately <b>if</b> all dependencies are up.    Puts a load of {@code +1} on <em>U</em> always.</li>
 * <li>{@code ON_DEMAND} - only start a service if demanded.  Puts a load of {@code +1} on <em>U</em> <b>only</b> if <em>D</em> is greater than zero.</li>
 * <li>{@code NEVER} - never start.  The value of <em>U</em> is not affected and not considered.  The value of <em>D</em> is disregarded; if <em>D</em> was
 * greater than zero, then the existing "demand" on the dependency set (parents) is revoked and further "demands" are suppressed until this mode is left.</li>
 * <li>{@code REMOVE} - the same as {@code NEVER}; in addition, remove the service as soon as it is down. The mode may not be changed again after setting
 * this mode.</li>
 * </ul></li>
 * </ul>
 */
package org.jboss.msc.service;
