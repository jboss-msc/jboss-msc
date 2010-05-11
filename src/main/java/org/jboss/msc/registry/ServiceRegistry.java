package org.jboss.msc.registry;

import org.jboss.msc.resolver.*;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * ServiceRegistry -
 *
 * @author John Bailey
 */
public class ServiceRegistry {
    private final ConcurrentMap<ServiceName, ServiceDefinition> registry = new ConcurrentHashMap<ServiceName, ServiceDefinition>();

    void install(Collection<ServiceDefinition> services) throws ServiceRegistryException {
        try {
            resolve(toMap(services));
        } catch (ResolutionException e) {
            throw new ServiceRegistryException("Failed to resolve dependencies", e);
        }
    }

    private void addToRegistry(final ServiceDefinition serviceDefinition) {
        if (registry.putIfAbsent(serviceDefinition.getName(), serviceDefinition) != null) {
            throw new RuntimeException("Duplicate service name provided: " + serviceDefinition.getName());
        }
    }

    private Map<ServiceName, ServiceDefinition> toMap(Collection<ServiceDefinition> serviceDefinitions) {
        final Map<ServiceName, ServiceDefinition> allServiceDefinitions = new HashMap<ServiceName, ServiceDefinition>();
        for (ServiceDefinition serviceDefinition : serviceDefinitions)
            allServiceDefinitions.put(serviceDefinition.getName(), serviceDefinition);
        return allServiceDefinitions;
    }


    /**
     * Iterative depth-first resolution
     *
     * @param serviceDefinitions The list of serviceDefinitions to be resolved
     * @throws org.jboss.msc.resolver.ResolutionException
     *          if any problem occur during resolution
     */
    private void resolve(Map<ServiceName, ServiceDefinition> serviceDefinitions) throws ResolutionException {
        final Deque<ServiceDefinition> toResolve = new ArrayDeque<ServiceDefinition>(100);
        
        final Set<ServiceName> processed = new HashSet<ServiceName>();
        final Set<ServiceName> visited = new HashSet<ServiceName>();

        for (ServiceDefinition serviceDefinition : serviceDefinitions.values()) {
            if(processed.contains(serviceDefinition.getName()))
               continue; 

            toResolve.clear();
            toResolve.addFirst(serviceDefinition);

            while (!toResolve.isEmpty()) {
                final ServiceDefinition serviceToResolve = toResolve.getFirst();
                visited.add(serviceToResolve.getName());
                processed.add(serviceDefinition.getName());
                boolean dependenciesResolved = true;

                for (String dependency : serviceToResolve.getDependencies()) {
                    final ServiceName dependencyName = new ServiceName(dependency);
                    // See if it is already in the registry.  If so just move to the next dependency
                    if (registry.containsKey(dependencyName))
                        continue;

                    dependenciesResolved = false;
                    final ServiceDefinition dependencyDefinition = serviceDefinitions.get(dependencyName);
                    if (dependencyDefinition == null)
                        throw new MissingDependencyException("Missing dependency: " + serviceDefinition.getName() + " depends on " + dependency + " which can not be found");

                    if (visited.contains(dependencyName))
                        throw new CircularDependencyException("Circular dependency: " + visited);

                    if (!processed.contains(dependencyName)) {
                        toResolve.addFirst(dependencyDefinition);
                    }
                }

                if (dependenciesResolved) {
                    toResolve.removeFirst();
                    visited.remove(serviceToResolve.getName());
                    addToRegistry(serviceToResolve);
                }
            }
        }
    }
}
