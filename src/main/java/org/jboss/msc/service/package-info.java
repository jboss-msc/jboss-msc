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

/**
 * The service container implementation itself.  The service container is what coordinates the registry of services and
 * manages their installation and execution.  To create a service container, see {@link org.jboss.msc.service.ServiceContainer.Factory#create()}.
 * To create services, implement the {@link org.jboss.msc.Service} interface.
 * <p>
 * Individual service instances are controlled using the {@link org.jboss.msc.service.ServiceController} interface.  Service controllers follow
 * this strict internal state machine (separated into two views for better illustration).
 * <p>
 * First view shows substates and tasks executed on every transition from one substate to another.
 * Listener tasks are executed as last on every transition.
 * <img src="doc-files/Substates-Transition-Tasks.png" alt="State machine transition tasks">
 * <p>
 * Second view shows substates and conditions between them causing transition from one substate to another.
 * The variables are what determine when a transition may occur; any time a variable is changed, the conditions are
 * checked to see if a transition can occur.
 * <p>
 * <img src="doc-files/Substates-Transition-Conditions.png" alt="State machine transition conditions">
 * <p>
 * Where <em>mode</em> variable may hold one of possible controller {@link org.jboss.msc.service.ServiceController.Mode mode values}.
 */
package org.jboss.msc.service;
