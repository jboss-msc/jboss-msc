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

import org.jboss.msc.inject.CastingInjector;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.value.ImmediateValue;
import org.jboss.msc.value.Value;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.jboss.msc.service.BatchBuilderImpl.alreadyInstalled;

/**
* @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
*/
final class BatchServiceBuilderImpl<T> implements BatchServiceBuilder<T> {
    private final BatchBuilderImpl batchBuilder;
    private final Value<? extends Service<T>> serviceValue;
    private final ServiceName serviceName;
    private final boolean ifNotExist;
    private Location location;
    private ServiceController.Mode initialMode;
    private final Set<ServiceName> aliases = new HashSet<ServiceName>(0);
    private final Map<ServiceName, ServiceDependency> dependencies = new HashMap<ServiceName, ServiceDependency>(0);
    private final List<ServiceListener<? super T>> listeners = new ArrayList<ServiceListener<? super T>>(0);
    private final List<ValueInjection<?>> valueInjections = new ArrayList<ValueInjection<?>>(0);

    // Resolver state
    boolean processed;
    boolean visited;
    BatchServiceBuilderImpl<?> prev;
    int i;
    ServiceBuilder<T> builder;

    BatchServiceBuilderImpl(final BatchBuilderImpl batchBuilder, final Value<? extends Service<T>> serviceValue, final ServiceName serviceName, final boolean ifNotExist) {
        if(batchBuilder == null) throw new IllegalArgumentException("BatchBuilder can not be null");
        this.batchBuilder = batchBuilder;
        if(serviceValue == null) throw new IllegalArgumentException("ServiceValue can not be null");
        this.serviceValue = serviceValue;
        if(serviceName == null) throw new IllegalArgumentException("ServiceName can not be null");
        this.serviceName = serviceName;
        this.ifNotExist = ifNotExist;
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
        return addDependencies(false, newDependencies);
    }

    public BatchServiceBuilder<T> addOptionalDependencies(final ServiceName... newDependencies) {
        return addDependencies(true, newDependencies);
    }

    private BatchServiceBuilder<T> addDependencies(final boolean optional, final ServiceName... newDependencies) {
        if (batchBuilder.isDone()) {
            throw alreadyInstalled();
        }
        for (ServiceName dependency : newDependencies) {
            if(!serviceName.equals(dependency)) {
                addDependency(dependency, (NamedInjection)null, optional);
            }
        }
        return this;
    }

    public BatchServiceBuilder<T> addDependencies(final Iterable<ServiceName> newDependencies) {
        return addDependencies(newDependencies, false);
    }

    public BatchServiceBuilder<T> addOptionalDependencies(final Iterable<ServiceName> newDependencies) {
        return addDependencies(newDependencies, true);
    }

    private BatchServiceBuilder<T> addDependencies(final Iterable<ServiceName> newDependencies, final boolean optional) {
        if (batchBuilder.isDone()) {
            throw alreadyInstalled();
        }
        for (ServiceName dependency : newDependencies) {
            if(!serviceName.equals(dependency)) {
                addDependency(dependency, (NamedInjection)null, optional);
            }
        }
        return this;
    }

    public BatchServiceBuilder<T> addDependency(final ServiceName dependency) {
        return addDependency(dependency, false);
    }

    public BatchServiceBuilder<T> addOptionalDependency(final ServiceName dependency) {
        return addDependency(dependency, false);
    }

    private BatchServiceBuilder<T> addDependency(final ServiceName dependency, final boolean optional) {
        if (batchBuilder.isDone()) {
            throw alreadyInstalled();
        }
        if(!serviceName.equals(dependency)) {
            addDependency(dependency, (NamedInjection)null, optional);
        }
        return this;
    }

    public BatchServiceBuilder<T> addDependency(final ServiceName dependency, final Injector<Object> target) {
        return addDependency(dependency, target, false);
    }

    public BatchServiceBuilder<T> addOptionalDependency(final ServiceName dependency, final Injector<Object> target) {
        return addDependency(dependency, target, true);
    }

    private BatchServiceBuilder<T> addDependency(final ServiceName dependency, final Injector<Object> target, final boolean optional) {
        if (batchBuilder.isDone()) {
            throw alreadyInstalled();
        }
        final NamedInjection injection = new NamedInjection(dependency, target);
        if(!serviceName.equals(dependency)) {
            addDependency(dependency, injection, optional);
        }
        return this;
    }

    public <I> BatchServiceBuilder<T> addDependency(final ServiceName dependency, final Class<I> type, final Injector<I> target) {
        return addDependency(dependency, type, target, false);
    }

    public <I> BatchServiceBuilder<T> addOptionalDependency(final ServiceName dependency, final Class<I> type, final Injector<I> target) {
        return addDependency(dependency, type, target, true);
    }

    private <I> BatchServiceBuilder<T> addDependency(final ServiceName dependency, final Class<I> type, final Injector<I> target, final boolean optional) {
        if (batchBuilder.isDone()) {
            throw alreadyInstalled();
        }
        final NamedInjection injection = new NamedInjection(dependency, new CastingInjector<I>(target, type));
        if(!serviceName.equals(dependency)) {
            addDependency(dependency, injection, optional);
        }
        return this;
    }

    private void addDependency(final ServiceName dependency, final NamedInjection namedInjection, final boolean optional) {
        if(dependencies.containsKey(dependency)) {
            final ServiceDependency serviceDependency = dependencies.get(dependency);
            serviceDependency.setOptional(optional);
            if(namedInjection != null)
                serviceDependency.addNamedInjection(namedInjection);
        } else {
            dependencies.put(dependency, new ServiceDependency(dependency, optional, namedInjection));
        }
    }

    public <I> BatchServiceBuilder<T> addInjection(final Injector<? super I> target, final I value) {
        return addInjectionValue(target, new ImmediateValue<I>(value));
    }

    public <I> BatchServiceBuilder<T> addInjectionValue(final Injector<? super I> target, final Value<I> value) {
        if (batchBuilder.isDone()) {
            throw alreadyInstalled();
        }
        valueInjections.add(new ValueInjection<I>(value, target));
        return this;
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

    ServiceDependency[] getDependencies() {
        return new ArrayList<ServiceDependency>(dependencies.values()).toArray(new ServiceDependency[dependencies.size()]);
    }

    Iterable<? extends ServiceListener<? super T>> getListeners() {
        return listeners;
    }

    List<ValueInjection<?>> getValueInjections() {
        return valueInjections;
    }

    ServiceController.Mode getInitialMode() {
        return initialMode;
    }

    Location getLocation() {
        return location;
    }

    boolean isIfNotExist() {
        return ifNotExist;
    }
}
