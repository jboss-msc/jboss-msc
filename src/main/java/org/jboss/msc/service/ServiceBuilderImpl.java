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
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jboss.msc.inject.Injector;
import org.jboss.msc.inject.Injectors;
import org.jboss.msc.value.ImmediateValue;
import org.jboss.msc.value.Value;

/**
 * {@link ServiceBuilder} implementation.
 * 
 * 
* @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
*/
class ServiceBuilderImpl<T> implements ServiceBuilder<T> {

    private final AbstractServiceTarget serviceTarget;
    private final Value<? extends Service<T>> serviceValue;
    private final ServiceName serviceName;
    private final boolean ifNotExist;
    private Location location;
    private ServiceController.Mode initialMode;
    private final Set<ServiceName> aliases = new HashSet<ServiceName>(0);
    private final Map<ServiceName, Dependency> dependencies = new HashMap<ServiceName, Dependency>(0);
    private final Set<ServiceListener<? super T>> listeners = new IdentityHashSet<ServiceListener<? super T>>(0);
    private final List<ValueInjection<?>> valueInjections = new ArrayList<ValueInjection<?>>(0);
    private boolean installed = false;

    static final class Dependency {
        private final ServiceName name;
        private boolean optional;
        private List<Injector<Object>> injectorList = new ArrayList<Injector<Object>>(0);

        Dependency(final ServiceName name, final boolean optional) {
            this.name = name;
            this.optional = optional;
        }

        ServiceName getName() {
            return name;
        }

        List<Injector<Object>> getInjectorList() {
            return injectorList;
        }

        boolean isOptional() {
            return optional;
        }

        void setOptional(final boolean optional) {
            this.optional = optional;
        }
    }

    ServiceBuilderImpl(AbstractServiceTarget serviceTarget, final Value<? extends Service<T>> serviceValue, final ServiceName serviceName, final boolean ifNotExist) {
        if(serviceTarget == null) throw new IllegalArgumentException("ServiceTarget can not be null");
        this.serviceTarget = serviceTarget;
        if(serviceValue == null) throw new IllegalArgumentException("ServiceValue can not be null");
        this.serviceValue = serviceValue;
        if(serviceName == null) throw new IllegalArgumentException("ServiceName can not be null");
        this.serviceName = serviceName;
        this.ifNotExist = ifNotExist;
    }

    @Override
    public ServiceBuilder<T> addAliases(ServiceName... aliases) {
        for(ServiceName alias : aliases) {
            if(!alias.equals(serviceName)) {
                this.aliases.add(alias);
            }
        }
        return this;
    }

    @Override
    public ServiceBuilderImpl<T> setLocation() {
        checkAlreadyInstalled();
        final StackTraceElement element = new Throwable().getStackTrace()[1];
        final String fileName = element.getFileName();
        final int lineNumber = element.getLineNumber();
        return setLocation(new Location(fileName, lineNumber, -1, null));
    }

    @Override
    public ServiceBuilderImpl<T> setLocation(final Location location) {
        checkAlreadyInstalled();
        this.location = location;
        return this;
    }

    @Override
    public ServiceBuilderImpl<T> setInitialMode(final ServiceController.Mode mode) {
        checkAlreadyInstalled();
        initialMode = mode;
        return this;
    }

    @Override
    public ServiceBuilder<T> addDependencies(final ServiceName... newDependencies) {
        return addDependencies(false, newDependencies);
    }

    @Override
    public ServiceBuilder<T> addOptionalDependencies(final ServiceName... newDependencies) {
        return addDependencies(true, newDependencies);
    }

    private ServiceBuilder<T> addDependencies(final boolean optional, final ServiceName... newDependencies) {
        checkAlreadyInstalled();
        for (ServiceName dependency : newDependencies) {
            if(!serviceName.equals(dependency)) {
                doAddDependency(dependency, optional);
            }
        }
        return this;
    }

    @Override
    public ServiceBuilder<T> addDependencies(final Iterable<ServiceName> newDependencies) {
        return addDependencies(newDependencies, false);
    }

    ServiceBuilder<T> addDependenciesNoCheck(final Iterable<ServiceName> newDependencies) {
        return addDependenciesNoCheck(newDependencies, false);
    }

    @Override
    public ServiceBuilder<T> addOptionalDependencies(final Iterable<ServiceName> newDependencies) {
        return addDependencies(newDependencies, true);
    }

    private ServiceBuilder<T> addDependencies(final Iterable<ServiceName> newDependencies, final boolean optional) {
        checkAlreadyInstalled();
        return addDependenciesNoCheck(newDependencies, optional);
    }

    ServiceBuilder<T> addDependenciesNoCheck(final Iterable<ServiceName> newDependencies, final boolean optional) {
        for (ServiceName dependency : newDependencies) {
            if(!serviceName.equals(dependency)) {
                doAddDependency(dependency, optional);
            }
        }
        return this;
    }

    @Override
    public ServiceBuilder<T> addDependency(final ServiceName dependency) {
        return addDependency(dependency, false);
    }

    @Override
    public ServiceBuilder<T> addOptionalDependency(final ServiceName dependency) {
        return addDependency(dependency, true);
    }

    private ServiceBuilder<T> addDependency(final ServiceName dependency, final boolean optional) {
        checkAlreadyInstalled();
        if(!serviceName.equals(dependency)) {
            doAddDependency(dependency, optional);
        }
        return this;
    }

    @Override
    public ServiceBuilder<T> addDependency(final ServiceName dependency, final Injector<Object> target) {
        return addDependency(dependency, target, false);
    }

    @Override
    public ServiceBuilder<T> addOptionalDependency(final ServiceName dependency, final Injector<Object> target) {
        return addDependency(dependency, target, true);
    }

    private ServiceBuilder<T> addDependency(final ServiceName dependency, final Injector<Object> target, boolean optional) {
        checkAlreadyInstalled();
        doAddDependency(dependency, optional).getInjectorList().add(target);
        return this;
    }

    @Override
    public <I> ServiceBuilder<T> addDependency(final ServiceName dependency, final Class<I> type, final Injector<I> target) {
        return addDependency(dependency, type, target, false);
    }

    @Override
    public <I> ServiceBuilder<T> addOptionalDependency(final ServiceName dependency, final Class<I> type, final Injector<I> target) {
        return addDependency(dependency, type, target, true);
    }

    private <I> ServiceBuilder<T> addDependency(final ServiceName dependency, final Class<I> type, final Injector<I> target, final boolean optional) {
        checkAlreadyInstalled();
        doAddDependency(dependency, optional).getInjectorList().add(Injectors.cast(target, type));
        return this;
    }

    private Dependency doAddDependency(final ServiceName name, final boolean optional) {
        final Dependency existing = dependencies.get(name);
        if (existing != null) {
            if (!optional) existing.setOptional(false);
            return existing;
        }
        final Dependency newDep = new Dependency(name, optional);
        dependencies.put(name, newDep);
        return newDep;
    }

    @Override
    public <I> ServiceBuilder<T> addInjection(final Injector<? super I> target, final I value) {
        return addInjectionValue(target, new ImmediateValue<I>(value));
    }

    @Override
    public <I> ServiceBuilder<T> addInjectionValue(final Injector<? super I> target, final Value<I> value) {
        checkAlreadyInstalled();
        valueInjections.add(new ValueInjection<I>(value, target));
        return this;
    }

    @Override
    public ServiceBuilderImpl<T> addListener(final ServiceListener<? super T> listener) {
        checkAlreadyInstalled();
        listeners.add(listener);
        return this;
    }

    @Override
    public ServiceBuilderImpl<T> addListener(final ServiceListener<? super T>... serviceListeners) {
        checkAlreadyInstalled();
        for (ServiceListener<? super T> listener : serviceListeners) {
            final Set<ServiceListener<? super T>> listeners = this.listeners;
            listeners.add(listener);
        }
        return this;
    }

    @Override
    public ServiceBuilderImpl<T> addListener(final Collection<? extends ServiceListener<? super T>> serviceListeners) {
        checkAlreadyInstalled();
        return addListenerNoCheck(serviceListeners);
    }

    ServiceBuilderImpl<T> addListenerNoCheck(final Collection<? extends ServiceListener<? super T>> serviceListeners) {
        listeners.addAll(serviceListeners);
        return this;
    }

    private void checkAlreadyInstalled() {
        if (installed) {
            throw new IllegalStateException("ServiceBuilder already installed");
        }
    }

    @Override
    public void install() throws ServiceRegistryException {
        if (!installed) {
            // mark it before perform the installation,
            // so we avoid ServiceRegistryException being thrown multiple times
            installed = true;
            serviceTarget.install(this);
        }
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

    Map<ServiceName, Dependency> getDependencies() {
        return dependencies;
    }

    Set<? extends ServiceListener<? super T>> getListeners() {
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

    ServiceTarget getTarget() {
        return serviceTarget;
    }
}
