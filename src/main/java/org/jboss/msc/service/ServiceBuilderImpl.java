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

import org.jboss.msc.value.Value;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
final class ServiceBuilderImpl<T> implements ServiceBuilder<T> {

    private final ServiceContainerImpl container;
    private final Value<? extends Service> service;
    private final Value<T> value;

    private ServiceController<T> controller;
    private Location location;

    ServiceBuilderImpl(final ServiceContainerImpl container, final Value<? extends Service> service, final Value<T> value) {
        this.container = container;
        this.service = service;
        this.value = value;
    }

    public <D extends Service> void addDependency(final ServiceController<D> dependency) {
        // does not accept a Value<> to prevent circularity
    }

    public ServiceBuilder<T> addListener(final ServiceListener<T> listener) {
        return this;
    }

    public ServiceBuilder<T> setInitialMode(final ServiceController.Mode mode) {
        return this;
    }

    public ServiceBuilder<T> setLocation(final Location location) {
        this.location = location;
        return this;
    }

    public ServiceBuilder<T> setLocation() {
        final StackTraceElement element = new Throwable().getStackTrace()[1];
        final String fileName = element.getFileName();
        final int lineNumber = element.getLineNumber();
        return setLocation(new Location(fileName, lineNumber, -1, null));
    }

    public ServiceController<T> getValue() {
        synchronized (this) {
            final ServiceController<T> controller = this.controller;
            return controller != null ? controller : (this.controller = new ServiceControllerImpl<T>(container, service, value, location, null));
        }
    }
}
