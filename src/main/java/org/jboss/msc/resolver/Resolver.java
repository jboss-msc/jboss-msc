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

package org.jboss.msc.resolver;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Basic dependency resolution.  Uses a depth-first topological sort.  The default implementation
 * is using an iterative and not recursive algorithm to avoid stack frame issues.  
 *
 * @author John Bailey
 * @author Jason T. Greene
 */
public class Resolver {

   /**
    * Resolve the provided items and pass each resolved item to the callback.
    *
    * @param serviceDefinitions The map of items to be resolved
    * @param callback The callback usd to handle resolved items
    * @throws ResolutionException if any problem occur during resolution
    */
   public void resolve(final Map<String, ServiceDefinition> serviceDefinitions, final ResolveCallback callback) throws ResolutionException {
      iterativeResolve(serviceDefinitions, callback);
   }

   /**
    * Recursive depth-first resolution
    * 
    * @param serviceDefinitions The list of items to be resolved
    * @param callback The callback usd to handle resolved items
    * @throws ResolutionException if any problem occur during resolution
    */
   private void recursiveResolve(final Map<String, ServiceDefinition> serviceDefinitions, final ResolveCallback callback) throws ResolutionException {
      final Set<String> visited = new HashSet<String>();
      for(ServiceDefinition serviceDefinition : serviceDefinitions.values()) {
         resolve(serviceDefinition, serviceDefinitions, visited, callback);
      }
   }

   private void resolve(final ServiceDefinition serviceDefinition, final Map<String, ServiceDefinition> serviceDefinitions, final Set<String> visited, final ResolveCallback callback) throws ResolutionException {
      if(visited.contains(serviceDefinition.getName()))
            throw new CircularDependencyException("Circular dependency discovered: " + visited);
      visited.add(serviceDefinition.getName());
      try {
         if(!serviceDefinition.isProcessed()) {
            serviceDefinition.setProcessed(true);
            for(String dep : serviceDefinition.getDependencies()) {
               final ServiceDefinition dependencyDefinition = serviceDefinitions.get(dep);
               if(dependencyDefinition == null)
                  throw new MissingDependencyException("Missing dependency: " + serviceDefinition.getName() + " depends on " + dep + " which can not be found");
               resolve(dependencyDefinition, serviceDefinitions, visited, callback);
            }
            callback.resolve(serviceDefinition);
         }
      } finally {
         visited.remove(serviceDefinition);
      }
   }

   /**
    * Iterative depth-first resolution
    *
    * @param serviceDefinitions The list of serviceDefinitions to be resolved
    * @param callback The callback usd to handle resolved serviceDefinitions
    * @throws ResolutionException if any problem occur during resolution
    */
   private void iterativeResolve(Map<String, ServiceDefinition> serviceDefinitions, final ResolveCallback callback) throws ResolutionException {
      final Deque<ServiceDefinition> toResolve = new ArrayDeque<ServiceDefinition>(100);
      final Set<String> visited = new HashSet<String>();

      for(ServiceDefinition serviceDefinition : serviceDefinitions.values()) {
         toResolve.clear();
         toResolve.addFirst(serviceDefinition);

         while(!toResolve.isEmpty()) {
            final ServiceDefinition serviceToResolve = toResolve.getFirst();
            visited.add(serviceToResolve.getName());
            serviceToResolve.setProcessed(true);
            boolean dependenciesResolved = true;

            for(String dependency : serviceToResolve.getDependencies()) {
               final ServiceDefinition dependencyDefinition = serviceDefinitions.get(dependency);
               if(dependencyDefinition == null)
                  throw new MissingDependencyException("Missing dependency: " + serviceDefinition.getName() + " depends on " + dependency + " which can not be found");

               if(!dependencyDefinition.isResolved()) {
                  if(visited.contains(dependencyDefinition.getName()))
                     throw new CircularDependencyException("Circular dependency: " + visited);
                  dependenciesResolved = false;
               }

               if(!dependencyDefinition.isProcessed()) {
                  toResolve.addFirst(dependencyDefinition);
               }
            }

            if(dependenciesResolved) {
               toResolve.removeFirst();
               visited.remove(serviceToResolve);
               if(!serviceToResolve.isResolved()) {
                  serviceToResolve.setResolved(true);
                  callback.resolve(serviceToResolve);
               }
            }
         }
      }
   }

   public static interface ResolveCallback {
      void resolve(ServiceDefinition serviceDefinition);
   }
}
