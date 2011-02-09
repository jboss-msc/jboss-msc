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

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.jboss.msc.value.ImmediateValue;
import org.jboss.msc.value.Value;

/**
 * Abstract base class used for ServiceTargets.
 *
 * @author John Bailey
 * @author <a href="mailto:flavia.rainone@jboss.com">Flavia Rainone</a>
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
class ServiceTargetImpl implements ServiceTarget {

    private final ServiceTargetImpl parent;
    private final Set<ServiceListener<Object>> listeners = Collections.synchronizedSet(new HashSet<ServiceListener<Object>>());
    private final Set<ServiceName> dependencies = Collections.synchronizedSet(new HashSet<ServiceName>());

    ServiceTargetImpl(final ServiceTargetImpl parent) {
        this.parent = parent;
    }

    @Override
    public <T> ServiceBuilder<T> addServiceValue(final ServiceName name, final Value<? extends Service<T>> value) throws IllegalArgumentException {
        return createServiceBuilder(name, value, null);
    }

    protected <T> ServiceBuilder<T> createServiceBuilder(final ServiceName name, final Value<? extends Service<T>> value, final ServiceControllerImpl<?> parent) throws IllegalArgumentException {
        if (name == null) {
            throw new IllegalArgumentException("name is null");
        }
        if (value == null) {
            throw new IllegalArgumentException("value is null");
        }
        return new ServiceBuilderImpl<T>(this, value, name, parent);
    }

    @Override
    public <T> ServiceBuilder<T> addService(final ServiceName name, final Service<T> service) throws IllegalArgumentException {
        return createServiceBuilder(name, new ImmediateValue<Service<T>>(service), null);
    }

    @Override
    public ServiceTarget addListener(ServiceListener<Object> listener) {
        if (listener == null) {
            return this;
        }
        listeners.add(listener);
        return this;
    }

    @Override
    public ServiceTarget addListener(ServiceListener<Object>... listeners) {
        if (listeners == null) {
            return this;
        }
        final Set<ServiceListener<Object>> batchListeners = this.listeners;

        for(ServiceListener<Object> listener : listeners) {
            batchListeners.add(listener);
        }
        return this;
    }

    @Override
    public ServiceTarget addListener(Collection<ServiceListener<Object>> listeners) {
        if (listeners == null) {
            return this;
        }

        final Set<ServiceListener<Object>> batchListeners = this.listeners;

        for(ServiceListener<Object> listener : listeners) {
            batchListeners.add(listener);
        }
        return this;
    }

    @Override
    public ServiceTarget removeListener(final ServiceListener<Object> listener) {
        if (listener == null) {
            return this;
        }
        listeners.remove(listener);
        return this;
    }

    @Override
    public Set<ServiceListener<Object>> getListeners() {
        return Collections.unmodifiableSet(listeners);
    }

    @Override
    public ServiceTarget addDependency(ServiceName dependency) {
        if (dependency == null) {
            return this;
        }
        dependencies.add(dependency);
        return this;
    }

    @Override
    public ServiceTarget addDependency(ServiceName... dependencies) {
        if (dependencies == null) {
            return this;
        }
        final Set<ServiceName> myDependencies = this.dependencies;
        for(ServiceName dependency : dependencies) {
            myDependencies.add(dependency);
        }
        return this;
    }

    @Override
    public ServiceTarget addDependency(Collection<ServiceName> dependencies) {
        if (dependencies == null) {
            return this;
        }
        final Set<ServiceName> myDependencies = this.dependencies;
        for(ServiceName dependency : dependencies) {
            myDependencies.add(dependency);
        }
        return this;
    }

    @Override
    public ServiceTarget removeDependency(final ServiceName dependency) {
        if (dependency == null) {
            return this;
        }
        dependencies.remove(dependency);
        return this;
    }

    @Override
    public Set<ServiceName> getDependencies() {
        return Collections.unmodifiableSet(dependencies);
    }

    /**
     * Apply listeners and dependencies to {@code serviceBuilder}.
     * 
     * @param serviceBuilder serviceBuilder which listeners and dependencies will be added to.
     */
    void apply(ServiceBuilderImpl<?> serviceBuilder) {
        serviceBuilder.addListenerNoCheck(listeners);
        serviceBuilder.addDependenciesNoCheck(dependencies);
    }

    /**
     * Install {@code serviceBuilder} in this target.
     *
     * @param serviceBuilder a serviceBuilder created by this ServiceTarget
     *
     * @return the installed service controller
     *
     * @throws ServiceRegistryException if a service registry issue occurred during installation
     */
    <T> ServiceController<T> install(ServiceBuilderImpl<T> serviceBuilder) throws ServiceRegistryException {
        apply(serviceBuilder);
        return parent.install(serviceBuilder);
    }

    /**
     * Returns the serviceRegistry that contains all services installed by this target.
     * 
     * @return the serviceRegistry containing services installed by this target
     */
    ServiceRegistry getServiceRegistry() {
        return parent.getServiceRegistry();
    }

    @Override
    public ServiceTarget subTarget() {
        return new ServiceTargetImpl(this);
    }

    @Override
    public BatchServiceTarget batchTarget() {
        return new BatchServiceTargetImpl(this, getServiceRegistry());
    }
}
