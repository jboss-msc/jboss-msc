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
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.jboss.msc.value.ImmediateValue;
import org.jboss.msc.value.Value;

import static org.jboss.msc.service.BatchBuilderImpl.alreadyInstalled;

/**
* @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
*/
final class BatchServiceBuilderImpl<T> implements BatchServiceBuilder<T> {
    private final BatchBuilderImpl batchBuilder;
    private final Value<? extends Service<T>> serviceValue;
    private final ServiceName serviceName;
    private Location location;
    private ServiceController.Mode initialMode;
    private final Set<ServiceName> aliases = new HashSet<ServiceName>(0);
    private final Set<ServiceName> dependencies = new HashSet<ServiceName>(0);
    private final List<ServiceListener<? super T>> listeners = new ArrayList<ServiceListener<? super T>>(0);
    private final List<BatchInjectionBuilderImpl> injectionItems = new ArrayList<BatchInjectionBuilderImpl>(0);

    // Resolver state
    boolean processed;
    boolean visited;
    BatchServiceBuilderImpl<?> prev;
    int i;
    ServiceBuilder<T> builder;

    BatchServiceBuilderImpl(final BatchBuilderImpl batchBuilder, final Value<? extends Service<T>> serviceValue, final ServiceName serviceName) {
        if(batchBuilder == null) throw new IllegalArgumentException("BatchBuilder can not be null");
        this.batchBuilder = batchBuilder;
        if(serviceValue == null) throw new IllegalArgumentException("ServiceValue can not be null");
        this.serviceValue = serviceValue;
        if(serviceName == null) throw new IllegalArgumentException("ServiceName can not be null");
        this.serviceName = serviceName;
    }

    @Override
    public BatchServiceBuilder<T> addAliases(ServiceName... aliases) {
        for(ServiceName alias : aliases) {
            if(!alias.equals(serviceName)) {
                this.aliases.add(alias);
            }
        }
        return this;
    }

    public BatchServiceBuilderImpl<T> setLocation() {
        if (batchBuilder.isDone()) {
            throw alreadyInstalled();
        }
        final StackTraceElement element = new Throwable().getStackTrace()[1];
        final String fileName = element.getFileName();
        final int lineNumber = element.getLineNumber();
        return setLocation(new Location(fileName, lineNumber, -1, null));
    }

    public BatchServiceBuilderImpl<T> setLocation(final Location location) {
        if (batchBuilder.isDone()) {
            throw alreadyInstalled();
        }
        this.location = location;
        return this;
    }

    public BatchServiceBuilderImpl<T> setInitialMode(final ServiceController.Mode mode) {
        if (batchBuilder.isDone()) {
            throw alreadyInstalled();
        }
        initialMode = mode;
        return this;
    }

    public BatchServiceBuilder<T> addDependencies(final ServiceName... newDependencies) {
        if (batchBuilder.isDone()) {
            throw alreadyInstalled();
        }
        final Set<ServiceName> dependencies = this.dependencies;
        for (ServiceName dependency : newDependencies) {
            if(!serviceName.equals(dependency)) {
                dependencies.add(dependency);
            }
        }
        return this;
    }

    public BatchServiceBuilder<T> addDependencies(final Iterable<ServiceName> newDependencies) {
        if (batchBuilder.isDone()) {
            throw alreadyInstalled();
        }
        final Set<ServiceName> dependencies = this.dependencies;
        for (ServiceName dependency : newDependencies) {
            if(!serviceName.equals(dependency)) {
                dependencies.add(dependency);
            }
        }
        return this;
    }

    public BatchInjectionBuilderImpl addDependency(final ServiceName dependency) {
        if (batchBuilder.isDone()) {
            throw alreadyInstalled();
        }
        if(!serviceName.equals(dependency)) {
            dependencies.add(dependency);
        }
        return new BatchInjectionBuilderImpl(this, new ServiceInjectionSource(dependency), batchBuilder);
    }

    public BatchInjectionBuilderImpl addInjectionValue(final Value<?> value) {
        if (batchBuilder.isDone()) {
            throw alreadyInstalled();
        }
        return new BatchInjectionBuilderImpl(this, new ValueInjectionSource(value), batchBuilder);
    }

    public BatchInjectionBuilderImpl addInjection(final Object value) {
        if (batchBuilder.isDone()) {
            throw alreadyInstalled();
        }
        return new BatchInjectionBuilderImpl(this, new ValueInjectionSource(new ImmediateValue<Object>(value)), batchBuilder);
    }

    public BatchServiceBuilderImpl<T> addListener(final ServiceListener<? super T> listener) {
        if (batchBuilder.isDone()) {
            throw alreadyInstalled();
        }
        listeners.add(listener);
        return this;
    }

    public BatchServiceBuilderImpl<T> addListener(final ServiceListener<? super T>... serviceListeners) {
        if (batchBuilder.isDone()) {
            throw alreadyInstalled();
        }
        for (ServiceListener<? super T> listener : serviceListeners) {
            final List<ServiceListener<? super T>> listeners = this.listeners;
            listeners.add(listener);
        }
        return this;
    }

    public BatchServiceBuilderImpl<T> addListener(final Collection<? extends ServiceListener<? super T>> serviceListeners) {
        if (batchBuilder.isDone()) {
            throw alreadyInstalled();
        }
        listeners.addAll(serviceListeners);
        return this;
    }

    Value<? extends Service<T>> getServiceValue() {
        return serviceValue;
    }

    ServiceName getName() {
        return serviceName;
    }

    ServiceName[] getAliases() {
        return aliases.toArray(new ServiceName[aliases.size()]);
    }

    ServiceName[] getDependencies() {
        return dependencies.toArray(new ServiceName[dependencies.size()]);
    }

    Iterable<? extends ServiceListener<? super T>> getListeners() {
        return listeners;
    }

    List<BatchInjectionBuilderImpl> getInjections() {
        return injectionItems;
    }

    ServiceController.Mode getInitialMode() {
        return initialMode;
    }

    Location getLocation() {
        return location;
    }
}
