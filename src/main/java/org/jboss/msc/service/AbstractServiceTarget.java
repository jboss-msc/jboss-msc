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
import java.util.HashSet;
import java.util.Set;

import org.jboss.msc.value.ImmediateValue;
import org.jboss.msc.value.Value;

/**
 * Abstract base class used for ServiceTargets.
 *
 * @author John Bailey
 * @author <a href="mailto:flavia.rainone@jboss.com">Flavia Rainone</a>
 */
abstract class AbstractServiceTarget implements ServiceTarget {

    private final Set<ServiceListener<Object>> listeners = new HashSet<ServiceListener<Object>>();
    private final Set<ServiceName> dependencies = new HashSet<ServiceName>();

    @Override
    public <T> ServiceBuilder<T> addServiceValue(final ServiceName name, final Value<? extends Service<T>> value) throws IllegalArgumentException {
        return createServiceBuilder(name, value, false);
    }

    private <T> ServiceBuilder<T> createServiceBuilder(final ServiceName name, final Value<? extends Service<T>> value, final boolean ifNotExist) throws IllegalArgumentException {
        validateTargetState();
        if (hasService(name) && ! ifNotExist) {
            throw new IllegalArgumentException("Service named " + name + " is already defined in this batch");
        }
        return new ServiceBuilderImpl<T>(this, value, name, ifNotExist);
    }

    @Override
    public <T> ServiceBuilder<T> addServiceValueIfNotExist(final ServiceName name, final Value<? extends Service<T>> value) throws IllegalArgumentException {
        return createServiceBuilder(name, value, true);
    }

    @Override
    public <T> ServiceBuilder<T> addService(final ServiceName name, final Service<T> service) throws IllegalArgumentException {
        return createServiceBuilder(name, new ImmediateValue<Service<T>>(service), false);
    }

    @Override
    public ServiceTarget addListener(ServiceListener<Object> listener) {
        validateTargetState();
        listeners.add(listener);
        return this;
    }

    @Override
    public ServiceTarget addListener(ServiceListener<Object>... listeners) {
        validateTargetState();
        final Set<ServiceListener<Object>> batchListeners = this.listeners;

        for(ServiceListener<Object> listener : listeners) {
            batchListeners.add(listener);
        }
        return this;
    }

    @Override
    public ServiceTarget addListener(Collection<ServiceListener<Object>> listeners) {
        validateTargetState();
        if (listeners == null)
            throw new IllegalArgumentException("Listeners can not be null");

        final Set<ServiceListener<Object>> batchListeners = this.listeners;

        for(ServiceListener<Object> listener : listeners) {
            batchListeners.add(listener);
        }
        return this;
    }

    @Override
    public ServiceTarget addDependency(ServiceName dependency) {
        validateTargetState();
        dependencies.add(dependency);
        return this;
    }

    @Override
    public ServiceTarget addDependency(ServiceName... dependencies) {
        validateTargetState();
        final Set<ServiceName> batchDependencies = this.dependencies;
        for(ServiceName dependency : dependencies) {
            batchDependencies.add(dependency);
        }
        return this;
    }

    @Override
    public ServiceTarget addDependency(Collection<ServiceName> dependencies) {
        validateTargetState();
        if(dependencies == null) throw new IllegalArgumentException("Dependencies can not be null");
        final Set<ServiceName> batchDependencies = this.dependencies;
        for(ServiceName dependency : dependencies) {
            batchDependencies.add(dependency);
        }
        return this;
    }

    /**
     * Apply listeners and dependencies to {@code serviceBuilders}.
     * 
     * @param serviceBuilders a collection of the ServiceBuilders which the listeners and dependencies
     *                        will be added to.
     */
    void apply(Collection<ServiceBuilderImpl<?>> serviceBuilders) {
        for(ServiceBuilder<?> serviceBuilder : serviceBuilders) {
            serviceBuilder.addListener(listeners);
            serviceBuilder.addDependencies(dependencies);
        }
    }

    /**
     * Apply listeners and dependencies to {@code serviceBuilder}.
     * 
     * @param serviceBuilder serviceBuilder which listeners and dependencies will be added to.
     */
    void apply(ServiceBuilderImpl<?> serviceBuilder) {
        serviceBuilder.addListener(listeners);
        serviceBuilder.addDependencies(dependencies);
    }

    /**
     * Installs {@code serviceBuilder} in this target.
     * 
     * @param serviceBuilder            a serviceBuilder created by this ServiceTarget
     * @throws ServiceRegistryException if a service registry issue occurred during installation
     */
    abstract void install(ServiceBuilderImpl<?> serviceBuilder) throws ServiceRegistryException;

    /**
     * Indicates whether a service with the specified name exists in this target.
     * 
     * @param name the specified name
     * @return {@code true} if the service exists in this target
     */
    abstract boolean hasService(ServiceName name);

    /**
     * Validates whether the state of this target is valid for additions.
     * This method is invoked prior to every new addition of Services, Dependencies and ServiceListeners.
     * 
     * @throws IllegalStateException if the state is not valid for additions
     */
    abstract void validateTargetState() throws IllegalStateException;

    Set<ServiceListener<Object>> getListeners() {
        return listeners;
    }

    Set<ServiceName> getDependencies() {
        return dependencies;
    }

    @Override
    public ServiceTarget subTarget() {
        return new SubTarget(this);
    }
}
