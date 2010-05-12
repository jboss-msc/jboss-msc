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
package org.jboss.msc.registry;

import org.jboss.msc.service.AbstractServiceListener;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceContainer;
import org.jboss.msc.service.ServiceController;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Service registry capable of installing batches of services and enforcing dependency order. 
 *
 * @author John Bailey
 */
public class ServiceRegistry {
    private final ConcurrentMap<ServiceName, ServiceController<?>> registry = new ConcurrentHashMap<ServiceName, ServiceController<?>>();

    private final ServiceContainer serviceContainer;

    public ServiceRegistry(ServiceContainer serviceContainer) {
        this.serviceContainer = serviceContainer;
    }

    public ServiceBatch create() {
        return new ServiceBatch(this);
    }

    /**
     * Install a collection of service definitions into the registry.  Will install the services
     * in dependency order.
     *
     * @param services The service definitions to install
     * @throws ServiceRegistryException If any problems occur resolving the dependencies or adding to the registry.
     */
    void install(Map<ServiceName, ServiceDefinition> services) throws ServiceRegistryException {
        try {
            resolve(services);
        } catch (ResolutionException e) {
            throw new ServiceRegistryException("Failed to resolve dependencies", e);
        }
    }

    /**
     * Recursive depth-first resolution
     *
     * @param serviceDefinitions The list of items to be resolved
     * @throws ResolutionException if any problem occur during resolution
     */
    private void resolve(final Map<ServiceName, ServiceDefinition> serviceDefinitions) throws ServiceRegistryException {
        final Set<ServiceName> visited = new HashSet<ServiceName>();
        for (ServiceDefinition serviceDefinition : serviceDefinitions.values()) {
            resolve(serviceDefinition, serviceDefinitions, visited);
        }
    }

    private ServiceController<?> resolve(final ServiceDefinition serviceDefinition, final Map<ServiceName, ServiceDefinition> serviceDefinitions, final Set<ServiceName> visited) throws ServiceRegistryException {
        final ServiceName name = serviceDefinition.getName();

        if (!visited.add(name)) {
            throw new CircularDependencyException("Circular dependency discovered: " + visited);
        }

        try {
            final ConcurrentMap<ServiceName, ServiceController<?>> registry = this.registry;
            final ServiceBuilder<Service> builder = serviceContainer.buildService(serviceDefinition.getService());

            for (String dependency : serviceDefinition.getDependencies()) {
                final ServiceName dependencyName = ServiceName.create(dependency);

                ServiceController<?> dependencyController = registry.get(dependencyName);
                if (dependencyController == null) {
                    final ServiceDefinition dependencyDefinition = serviceDefinitions.get(dependencyName);
                    if (dependencyDefinition == null)
                        throw new MissingDependencyException("Missing dependency: " + name + " depends on " + dependencyName + " which can not be found");
                    dependencyController = resolve(dependencyDefinition, serviceDefinitions, visited);
                }
                builder.addDependency(dependencyController);
            }

            // We are resolved.  Lets install
            builder.addListener(new ServiceUnregisterListner(name));

            final ServiceController<?> serviceController = builder.create();
            if (registry.putIfAbsent(name, serviceController) != null) {
                throw new ServiceRegistryException("Duplicate service name provided: " + name);
            }
            return serviceController;
        } finally {
            visited.remove(name);
        }

    }

    private class ServiceUnregisterListner extends AbstractServiceListener<Service> {
        private final ServiceName serviceName;

        private ServiceUnregisterListner(ServiceName serviceName) {
            this.serviceName = serviceName;
        }

        @Override
        public void serviceRemoved(ServiceController serviceController) {
            if(!registry.remove(serviceName, serviceController))
                throw new RuntimeException("Removed service [" + serviceName + "] was not unregistered");
        }
    }
}
