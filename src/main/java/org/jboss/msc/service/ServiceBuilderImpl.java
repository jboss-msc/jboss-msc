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

package org.jboss.msc.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.value.Value;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
final class ServiceBuilderImpl<S> implements ServiceBuilder<S> {

    private final ServiceContainerImpl container;
    private final List<ServiceControllerImpl<?>> deps = new ArrayList<ServiceControllerImpl<?>>(0);
    private final List<ValueInjection<?>> injections = new ArrayList<ValueInjection<?>>(0);
    private final List<ServiceListener<? super S>> listeners = new ArrayList<ServiceListener<? super S>>(0);
    private final List<ServiceName> aliases = new ArrayList<ServiceName>(0);
    private final Value<? extends Service<? extends S>> service;
    private final ServiceName serviceName;

    private ServiceController.Mode mode = ServiceController.Mode.AUTOMATIC;
    private ServiceControllerImpl<S> controller;
    private Location location;

    ServiceBuilderImpl(final ServiceContainerImpl container, final Value<? extends Service<? extends S>> service, final ServiceName serviceName) {
        this.container = container;
        this.service = service;
        this.serviceName = serviceName;
    }

    public void addDependency(final ServiceController<?> dependency) {
        if (dependency == null) {
            throw new IllegalArgumentException("dependency is null");
        }
        synchronized (this) {
            if (controller != null) throw new IllegalStateException();
            if (! (dependency instanceof ServiceControllerImpl)) {
                throw new IllegalArgumentException("Given dependency has an invalid implementation");
            }
            deps.add((ServiceControllerImpl<?>) dependency);
        }
    }

    public <T> ServiceBuilderImpl<S> addValueInjection(final ValueInjection<T> injection) {
        if (injection == null) {
            throw new IllegalArgumentException("injection is null");
        }
        synchronized (this) {
            if (controller != null) throw new IllegalStateException();
            injections.add(injection);
            return this;
        }
    }

    public <T> ServiceBuilderImpl<S> addValueInjection(final ServiceController<T> dependency, final Injector<T> injector) {
        if (dependency == null) {
            throw new IllegalArgumentException("dependency is null");
        }
        if (injector == null) {
            throw new IllegalArgumentException("injector is null");
        }
        synchronized (this) {
            if (controller != null) throw new IllegalStateException();
            if (! (dependency instanceof ServiceControllerImpl)) {
                throw new IllegalArgumentException("Given dependency has an invalid implementation");
            }
            deps.add((ServiceControllerImpl<?>) dependency);
            injections.add(new ValueInjection<T>(dependency, injector));
            return this;
        }
    }

    public ServiceBuilderImpl<S> addListener(final ServiceListener<? super S> listener) {
        if (listener == null) {
            throw new IllegalArgumentException("listener is null");
        }
        synchronized (this) {
            if (controller != null) throw new IllegalStateException();

            listeners.add(listener);
            return this;
        }
    }

    public ServiceBuilderImpl<S> setInitialMode(final ServiceController.Mode mode) {
        if (mode == null) {
            throw new IllegalArgumentException("mode is null");
        }
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

    public ServiceBuilderImpl<S> addAlias(ServiceName alias) {
        aliases.add(alias);
        return this;
    }

    public ServiceBuilderImpl<S> addAliases(ServiceName... aliases) {
        this.aliases.addAll(Arrays.asList(aliases));
        return this;
    }

    private static final ServiceControllerImpl<?>[] NO_DEPS = new ServiceControllerImpl<?>[0];
    private static final ValueInjection<?>[] NO_INJECTIONS = new ValueInjection<?>[0];
    private static final ServiceName[] NO_ALIASES = new ServiceName[0];

    private ServiceControllerImpl<S> doCreate() {
        synchronized (this) {
            final List<ServiceControllerImpl<?>> deps = this.deps;
            final List<ValueInjection<?>> injections = this.injections;
            final int depsSize = deps.size();
            final int injectionsSize = injections.size();
            final int aliasesSize = aliases.size();
            final ServiceControllerImpl<?>[] depArray = depsSize == 0 ? NO_DEPS : deps.toArray(new ServiceControllerImpl<?>[depsSize]);
            final ValueInjection<?>[] injectionArray = injectionsSize == 0 ? NO_INJECTIONS : injections.toArray(new ValueInjection<?>[injectionsSize]);
            final ServiceName[] aliasNames = aliasesSize == 0 ? NO_ALIASES : aliases.toArray(new ServiceName[aliasesSize]);
            final ServiceControllerImpl<S> controller = this.controller = new ServiceControllerImpl<S>(container, service, location, depArray, injectionArray, serviceName, aliasNames);
            controller.initialize();
            for (ServiceListener<? super S> listener : listeners) {
                controller.addListener(listener);
            }
            controller.setMode(mode);
            return controller;
        }
    }
}
