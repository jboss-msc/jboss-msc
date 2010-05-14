/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat Middleware LLC, and individual contributors
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
 * A listener for service lifecycle events.  The associated controller will not leave its current state until
 * all listeners finish running.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public interface ServiceListener<S> {

    /**
     * The listener has been added to a controller.
     *
     * @param controller the controller that this listener was added to
     */
    void listenerAdded(ServiceController<? extends S> controller);

    /**
     * The service is starting.  Called after the state transitions from {@code DOWN} to {@code STARTING}.
     *
     * @param controller the controller
     */
    void serviceStarting(ServiceController<? extends S> controller);

    /**
     * The service is started (up).  Called after the state transitions from {@code STARTING} to {@code UP}.
     *
     * @param controller the controller
     */
    void serviceStarted(ServiceController<? extends S> controller);

    /**
     * The service start has failed.  Called after the state transitions from {@code STARTING} to {@code START_FAILED}.
     *
     * @param controller the controller
     * @param reason the reason for failure
     */
    void serviceFailed(ServiceController<? extends S> controller, StartException reason);

    /**
     * The service is stopping.  Called after the state transitions from {@code UP} to {@code STOPPING}.
     *
     * @param controller the controller
     */
    void serviceStopping(ServiceController<? extends S> controller);

    /**
     * The service is stopped (down).  Called after the state transitions from {@code STOPPING} to {@code DOWN}.
     *
     * @param controller the controller
     */
    void serviceStopped(ServiceController<? extends S> controller);

    /**
     * The service has been removed.  The listener will automatically be unregistered after this call.  Called
     * after the state transitions from {@code DOWN} to {@code REMOVED}.
     *
     * @param controller the controller
     */
    void serviceRemoved(ServiceController<? extends S> controller);
}
