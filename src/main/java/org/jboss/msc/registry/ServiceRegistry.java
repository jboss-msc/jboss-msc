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

import java.util.Map;
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
     * @param serviceBatch The service batch to install
     * @throws ServiceRegistryException If any problems occur resolving the dependencies or adding to the registry.
     */
    void install(final ServiceBatch serviceBatch) throws ServiceRegistryException {
        try {
            resolve(serviceBatch.getBatchEntries());
        } catch (ResolutionException e) {
            throw new ServiceRegistryException("Failed to resolve dependencies", e);
        }
    }

    /**
     * Recursive depth-first resolution
     *
     * @param services The list of items to be resolved
     * @throws ResolutionException if any problem occur during resolution
     */
    private void resolve(final Map<ServiceName, ServiceBatch.BatchEntry> services) throws ServiceRegistryException {
        for (ServiceBatch.BatchEntry batchEntry : services.values()) {
            if(!batchEntry.isProcessed())
                iterResolve(batchEntry, services);
        }
    }

    private ServiceController<?> resolve(final ServiceBatch.BatchEntry entry, final Map<ServiceName, ServiceBatch.BatchEntry> services) throws ServiceRegistryException {
        final ServiceDefinition serviceDefinition = entry.getServiceDefinition();
        entry.setProcessed(true);
        final ServiceName name = serviceDefinition.getName();
        if (entry.isVisited())
            throw new CircularDependencyException("Circular dependency discovered: " + serviceDefinition);

        entry.setVisited(true);

        try {
            final ConcurrentMap<ServiceName, ServiceController<?>> registry = this.registry;
            final ServiceBuilder<Service> builder = serviceContainer.buildService(serviceDefinition.getService());

            for (String dependency : serviceDefinition.getDependenciesDirect()) {
                final ServiceName dependencyName = ServiceName.create(dependency);

                ServiceController<?> dependencyController = registry.get(dependencyName);
                if (dependencyController == null) {
                    final ServiceBatch.BatchEntry dependencyEntry = services.get(dependencyName);
                    if (dependencyEntry == null)
                        throw new MissingDependencyException("Missing dependency: " + name + " depends on " + dependencyName + " which can not be found");
                    dependencyController = resolve(dependencyEntry, services);
                }
                builder.addDependency(dependencyController);
            }

            // We are resolved.  Lets install
            builder.addListener(new ServiceUnregisterListener(name));

            final ServiceController<?> serviceController = builder.create();
            if (registry.putIfAbsent(name, serviceController) != null) {
                throw new DuplicateServiceException("Duplicate service name provided: " + name);
            }
            return serviceController;
        } finally {
            entry.setVisited(false);
        }
    }
    
    public void iterResolve(ServiceBatch.BatchEntry entry, final Map<ServiceName, ServiceBatch.BatchEntry> services)  throws ServiceRegistryException
    {
        outer:
        while (entry != null) {
            if (entry.isVisited())
                throw new CircularDependencyException("Circular dependency discovered: " + entry.getServiceDefinition());
            entry.setVisited(true);
            
            if (entry.builder == null)
                entry.builder = serviceContainer.buildService(entry.getServiceDefinition().getService());
            
            final String[] deps = entry.getServiceDefinition().getDependenciesDirect();
            while (entry.i < deps.length)
            {
                final ServiceName dependencyName = ServiceName.create(deps[entry.i]);
        
                ServiceController<?> dependencyController = registry.get(dependencyName);     
                if (dependencyController == null){
                    final ServiceBatch.BatchEntry dependencyEntry = services.get(dependencyName);
                    if (dependencyEntry == null)
                        throw new MissingDependencyException("Missing dependency: " + entry.getServiceDefinition().getName() + " depends on " + dependencyName + " which can not be found");
                 
                    // Backup the last position, so that we can unroll
                    assert dependencyEntry.prev == null;
                    dependencyEntry.prev = entry;
                    entry = dependencyEntry;
                    
                    continue outer;
                }
                
                // Either the dep already exists, or we are unrolling and just created it
                entry.builder.addDependency(dependencyController);
                entry.i++;
            }
            
            // We are resolved.  Lets install
            entry.builder.addListener(new ServiceUnregisterListener(entry.getServiceDefinition().getName()));

            final ServiceController<?> serviceController = entry.builder.create();
            if (registry.putIfAbsent(entry.getServiceDefinition().getName(), serviceController) != null) {
                throw new DuplicateServiceException("Duplicate service name provided: " + entry.getServiceDefinition().getName());
            }
            
            // Unroll!
            entry.setProcessed(true);
            entry = entry.prev;
        }
    }

    private class ServiceUnregisterListener extends AbstractServiceListener<Service> {
        private final ServiceName serviceName;

        private ServiceUnregisterListener(ServiceName serviceName) {
            this.serviceName = serviceName;
        }

        @Override
        public void serviceRemoved(ServiceController serviceController) {
            if(!registry.remove(serviceName, serviceController))
                throw new RuntimeException("Removed service [" + serviceName + "] was not unregistered");
        }
    }
}
