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

import org.jboss.msc.inject.Injector;
import org.jboss.msc.value.Value;

/**
 * A service listener which injects a value into an injector.  The listener
 * should be added to the controller corresponding to the service into which the value will be injected.  If
 * the injection comes from another service, that service should typically be a dependency of the destination service.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class InjectingServiceListener<T> implements ServiceListener<Object> {
    private final Injector<? super T> injector;
    private final Value<? extends T> source;

    /**
     * Construct a new instance.
     *
     * @param injector the injector to receive the value
     * @param source the source for the injected value
     */
    public InjectingServiceListener(final Injector<? super T> injector, final Value<? extends T> source) {
        this.injector = injector;
        this.source = source;
    }

    /** {@inheritDoc} */
    public void serviceStarting(final ServiceController<?> controller) {
        injector.inject(source.getValue());
    }

    /** {@inheritDoc} */
    public void serviceStarted(final ServiceController<?> controller) {
    }

    /** {@inheritDoc} */
    public void serviceFailed(final ServiceController<?> controller, final StartException reason) {
    }

    /** {@inheritDoc} */
    public void serviceStopping(final ServiceController<?> controller) {
        injector.uninject();
    }

    /** {@inheritDoc} */
    public void serviceStopped(final ServiceController<?> controller) {
    }

    /** {@inheritDoc} */
    public void serviceRemoved(final ServiceController<?> serviceController) {
    }
}
