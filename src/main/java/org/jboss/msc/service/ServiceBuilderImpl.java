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

import java.util.ArrayList;
import java.util.List;
import org.jboss.msc.value.Value;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
final class ServiceBuilderImpl<S> implements ServiceBuilder<S> {

    private final ServiceContainerImpl container;
    private final List<ServiceControllerImpl<?>> deps = new ArrayList<ServiceControllerImpl<?>>();
    private final List<ServiceListener<? super S>> listeners = new ArrayList<ServiceListener<? super S>>();
    private final Value<? extends Service> service;
    private final Value<S> value;

    private ServiceController.Mode mode = ServiceController.Mode.AUTOMATIC;
    private ServiceControllerImpl<S> controller;
    private Location location;

    ServiceBuilderImpl(final ServiceContainerImpl container, final Value<? extends Service> service, final Value<S> value) {
        this.container = container;
        this.service = service;
        this.value = value;
    }

    public void addDependency(final ServiceController<?> dependency) {
        synchronized (this) {
            if (controller != null) throw new IllegalStateException();
            // does not accept a Value<> to prevent circularity
            if (! (dependency instanceof ServiceControllerImpl)) {
                throw new IllegalArgumentException("Given dependency has an invalid implementation");
            }
            deps.add((ServiceControllerImpl<?>) dependency);
        }
    }

    public ServiceBuilderImpl<S> addListener(final ServiceListener<? super S> listener) {
        synchronized (this) {
            if (controller != null) throw new IllegalStateException();

            listeners.add(listener);
            return this;
        }
    }

    public ServiceBuilderImpl<S> setInitialMode(final ServiceController.Mode mode) {
        synchronized (this) {
            if (controller != null) throw new IllegalStateException();

            this.mode = mode;
            return this;
        }
    }

    public ServiceBuilderImpl<S> setLocation(final Location location) {
        synchronized (this) {
            if (controller != null) throw new IllegalStateException();

            this.location = location;
            return this;
        }
    }

    public ServiceBuilderImpl<S> setLocation() {
        final StackTraceElement element = new Throwable().getStackTrace()[1];
        final String fileName = element.getFileName();
        final int lineNumber = element.getLineNumber();
        return setLocation(new Location(fileName, lineNumber, -1, null));
    }

    public ServiceControllerImpl<S> create() {
        synchronized (this) {
            final ServiceControllerImpl<S> controller = this.controller;
            return controller != null ? controller : doCreate();
        }
    }

    private ServiceControllerImpl<S> doCreate() {
        final ServiceControllerImpl<S> controller = this.controller = new ServiceControllerImpl<S>(container, service, value, location, deps.toArray(new ServiceControllerImpl<?>[deps.size()]));
        controller.setMode(mode);
        return controller;
    }
}
