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
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Basic dependency resolution.  Uses a depth-first topological sort.  The default implementation
 * is using an iterative and not recursive algorithm to avoid stack frame issues.  
 *
 * @author John Bailey
 */
public class Resolver {

   /**
    * Resolve the provided items and pass each resolved item to the handler. 
    *
    * @param items The map of items to be resolved
    * @param handler The callback usd to handle resolved items
    * @throws ResolutionException if any problem occur during resolution
    */
   public void resolve(final Map<String, Item> items, final ResolveHandler handler) throws ResolutionException {
      iterativeResolve(items, handler);
   }

   /**
    * Recursive depth-first resolution
    * 
    * @param items The list of items to be resolved
    * @param handler The callback usd to handle resolved items
    * @throws ResolutionException if any problem occur during resolution
    */
   private void recursiveResolve(final Map<String, Item> items, final ResolveHandler handler) throws ResolutionException {
      final List<Item> resolved = new ArrayList<Item>(items.size());
      final Set<Item> visited = new HashSet<Item>();
      for(Item item : items.values()) {
         resolve(item, items, visited, handler);
      }
   }

   private void resolve(final Item item, final Map<String, Item> allItems, final Set<Item> visited, final ResolveHandler handler) throws ResolutionException {
      if(visited.contains(item))
            throw new CircularDependencyException("Circular dependency discovered: " + visited);
      visited.add(item);
      try {
         if(!item.isVisited()) {
            item.setVisited(true);
            for(String dep : item.getDependencies()) {
               final Item depItem = allItems.get(dep);
               if(depItem == null)
                  throw new MissingDependencyException("Missing dependency: " + item.getName() + " depends on " + dep + " which can not be found");
               resolve(depItem, allItems, visited, handler);
            }
            handler.resolve(item);
         }
      } finally {
         visited.remove(item);
      }
   }

   /**
    * Iterative depth-first resolution
    *
    * @param items The list of items to be resolved
    * @param handler The callback usd to handle resolved items
    * @throws ResolutionException if any problem occur during resolution
    */
   private void iterativeResolve(Map<String, Item> items, final ResolveHandler handler) throws ResolutionException {
      final Deque<Item> toResolve = new ArrayDeque<Item>(100);
      final Set<Item> visited = new HashSet<Item>();

      for(Item item : items.values()) {
         toResolve.clear();
         toResolve.addFirst(item);

         while(!toResolve.isEmpty()) {
            final Item itemToResolve = toResolve.getFirst();
            visited.add(itemToResolve);
            itemToResolve.setVisited(true);
            boolean dependenciesResolved = true;

            for(String dependency : itemToResolve.getDependencies()) {
               final Item dependencyItem = items.get(dependency);
               if(dependencyItem == null)
                  throw new MissingDependencyException("Missing dependency: " + item.getName() + " depends on " + dependency + " which can not be found");

               if(!dependencyItem.isResolved()) {
                  if(visited.contains(dependencyItem))
                     throw new CircularDependencyException("Circular dependency: " + visited);
                  dependenciesResolved = false;
               }

               if(!dependencyItem.isVisited()) {
                  toResolve.addFirst(dependencyItem);
               }
            }

            if(dependenciesResolved) {
               toResolve.removeFirst();
               visited.remove(itemToResolve);
               if(!itemToResolve.isResolved()) {
                  itemToResolve.setResolved(true);
                  handler.resolve(itemToResolve);
               }
            }
         }
      }
   }

   public static interface ResolveHandler {
      void resolve(Item item);
   }
}
