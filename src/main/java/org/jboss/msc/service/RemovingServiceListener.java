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
 * A service listener which removes the given service as soon as it is down, used to "kill" a service.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class RemovingServiceListener extends AbstractServiceListener<Object> {

    /** {@inheritDoc} */
    public void listenerAdded(final ServiceController<?> serviceController) {
        serviceController.setMode(ServiceController.Mode.NEVER);
    }

    /** {@inheritDoc} */
    public void serviceStarting(final ServiceController<?> serviceController) {
        serviceController.setMode(ServiceController.Mode.NEVER);
    }

    /** {@inheritDoc} */
    public void serviceStarted(final ServiceController<?> serviceController) {
        serviceController.setMode(ServiceController.Mode.NEVER);
    }

    /** {@inheritDoc} */
    public void serviceFailed(final ServiceController<?> serviceController, final StartException reason) {
        serviceController.setMode(ServiceController.Mode.NEVER);
    }

    /** {@inheritDoc} */
    public void serviceStopping(final ServiceController<?> serviceController) {
        serviceController.setMode(ServiceController.Mode.NEVER);
    }

    /** {@inheritDoc} */
    public void serviceStopped(final ServiceController<?> serviceController) {
        serviceController.setMode(ServiceController.Mode.NEVER);
        serviceController.remove();
    }
}
