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

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Service registry capable of installing batches of services and enforcing dependency order. 
 *
 * @author John Bailey
 */
public class ServiceRegistry {
    private final ConcurrentMap<ServiceName, ServiceDefinition> registry = new ConcurrentHashMap<ServiceName, ServiceDefinition>();

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

    private void addToRegistry(final ServiceDefinition serviceDefinition) throws ServiceRegistryException {
        if (registry.putIfAbsent(serviceDefinition.getName(), serviceDefinition) != null) {
            throw new ServiceRegistryException("Duplicate service name provided: " + serviceDefinition.getName());
        }
    }

    /**
     * Recursive depth-first resolution
     *
     * @param serviceDefinitions The list of items to be resolved
     * @throws ResolutionException if any problem occur during resolution
     */
    private void resolve(final Map<ServiceName, ServiceDefinition> serviceDefinitions) throws ServiceRegistryException {
        final Set<ServiceName> processed = new HashSet<ServiceName>(serviceDefinitions.size());
        final Set<ServiceName> visited = new HashSet<ServiceName>();
        for (ServiceDefinition serviceDefinition : serviceDefinitions.values()) {
            resolve(serviceDefinition, serviceDefinitions, processed, visited);
        }
    }

    private void resolve(final ServiceDefinition serviceDefinition, final Map<ServiceName, ServiceDefinition> serviceDefinitions, final Set<ServiceName> processed, final Set<ServiceName> visited) throws ServiceRegistryException {
        if (visited.contains(serviceDefinition.getName()))
            throw new CircularDependencyException("Circular dependency discovered: " + visited);
        visited.add(serviceDefinition.getName());
        try {
            if (!processed.contains(serviceDefinition.getName())) {
                processed.add(serviceDefinition.getName());
                for (String dependency : serviceDefinition.getDependencies()) {
                    final ServiceName dependencyName = ServiceName.create(dependency);
                    if(registry.containsKey(dependencyName))
                        continue;
                    
                    final ServiceDefinition dependencyDefinition = serviceDefinitions.get(dependencyName);
                    if (dependencyDefinition == null)
                        throw new MissingDependencyException("Missing dependency: " + serviceDefinition.getName() + " depends on " + dependencyName + " which can not be found");

                    resolve(dependencyDefinition, serviceDefinitions, processed, visited);
                }
                addToRegistry(serviceDefinition);
            }
        } finally {
            visited.remove(serviceDefinition.getName());
        }
    }
}
