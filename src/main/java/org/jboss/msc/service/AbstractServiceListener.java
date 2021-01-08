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
 * An abstract implementation of a service listener whose methods do nothing.
 *
 * @param <S> the service type
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 * @author <a href="mailto:flavia.rainone@jboss.com">Flavia Rainone</a>
 * @deprecated Service listeners are not encouraged for general user use.
 * This class will be removed in a future release.
 */
@Deprecated
public abstract class AbstractServiceListener<S> implements ServiceListener<S> {

    /** {@inheritDoc} */
    public void listenerAdded(final ServiceController<? extends S> controller) {
    }

    /** {@inheritDoc} */
    public void transition(final ServiceController<? extends S> controller, final ServiceController.Transition transition) {
    }

    /** {@inheritDoc} */
    public void serviceRemoveRequested(final ServiceController<? extends S> controller) {
    }

    /** {@inheritDoc} */
    public void serviceRemoveRequestCleared(final ServiceController<? extends S> controller) {
    }

    /** {@inheritDoc} */
    public void dependencyFailed(final ServiceController<? extends S> controller) {
    }

    /** {@inheritDoc} */
    public void dependencyFailureCleared(final ServiceController<? extends S> controller) {
    }

    /** {@inheritDoc} */
    public void immediateDependencyUnavailable(final ServiceController<? extends S> controller) {
    }

    /** {@inheritDoc} */
    public void immediateDependencyAvailable(final ServiceController<? extends S> controller) {
    }

    /** {@inheritDoc} */
    public void transitiveDependencyUnavailable(final ServiceController<? extends S> controller) {
    }

    /** {@inheritDoc} */
    public void transitiveDependencyAvailable(final ServiceController<? extends S> controller) {
    }
}
