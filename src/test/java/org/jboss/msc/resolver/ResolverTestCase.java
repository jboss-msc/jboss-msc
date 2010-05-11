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

import java.util.*;

import org.junit.Test;

import static org.junit.Assert.fail;
import static org.junit.Assert.assertTrue;

/**
 * Test case used to ensure functionality for the Resolver.
 * 
 * @author John Bailey
 */
public class ResolverTestCase {

   private final Resolver resolver = new Resolver();
   private final Resolver.ResolveCallback NO_OP_CALLBACK = new Resolver.ResolveCallback() {
      @Override
      public void resolve(final ServiceDefinition definition) {
      }
   };

    @Test
   public void testResolvable() throws Exception {
      CollectingCallback handler = new CollectingCallback();
      resolver.resolve(
         toMap(Arrays.asList(
            ServiceDefinition.create("7", "11", "8"),
            ServiceDefinition.create("5", "11"),
            ServiceDefinition.create("3", "11", "9"),
            ServiceDefinition.create("11", "2", "9", "10"),
            ServiceDefinition.create("8", "9"),
            ServiceDefinition.create("2"),
            ServiceDefinition.create("9"),
            ServiceDefinition.create("10")
         )),
         handler
      );
      assertInDependencyOrder(handler.getResolved());
   }

   @Test
   public void testMissingDependency() throws Exception {
      try {
         resolver.resolve(
            toMap(Arrays.asList(
               ServiceDefinition.create("7", "11", "8"),
               ServiceDefinition.create("5", "11"),
               ServiceDefinition.create("3", "11", "9"),
               ServiceDefinition.create("11", "2", "9", "10"),
               ServiceDefinition.create("8", "9"),
               ServiceDefinition.create("2", "1"),
               ServiceDefinition.create("9"),
               ServiceDefinition.create("10")
            )),
            NO_OP_CALLBACK
         );
         fail("Should have thrown missing dependency exception");
      } catch(MissingDependencyException expected) {}
   }


   @Test
   public void testCircular() throws Exception {

      try {
         resolver.resolve(
            toMap(Arrays.asList(
               ServiceDefinition.create("7", "5"),
               ServiceDefinition.create("5", "11"),
               ServiceDefinition.create("11", "7")
            )),
                 NO_OP_CALLBACK
         );
         fail("SHould have thrown circular dependency exception");
      } catch(CircularDependencyException expected) {}
   }


   @Test
   public void testMonster() throws Exception {

      final int totalServiceDefinitions = 10000;

      final List<ServiceDefinition> serviceDefinitions = new ArrayList<ServiceDefinition>(totalServiceDefinitions);
      for(int i = 0; i < totalServiceDefinitions; i++) {
         List<String> deps = new ArrayList<String>();
         int numDeps = Math.min(10, totalServiceDefinitions - i - 1);

         for(int j = 1; j < numDeps + 1; j++) {
            deps.add("test" + (i+j));
         }
         serviceDefinitions.add(ServiceDefinition.create("test" + i, deps.toArray(new String[deps.size()])));
      }

      Map<String, ServiceDefinition> allServiceDefinitions = new HashMap<String, ServiceDefinition>();
      for(ServiceDefinition serviceDefinition : serviceDefinitions)
         allServiceDefinitions.put(serviceDefinition.getName(), serviceDefinition);

      CollectingCallback collectingHandler = new CollectingCallback();
      long start = System.currentTimeMillis();
      resolver.resolve(allServiceDefinitions, collectingHandler);
      long end = System.currentTimeMillis();
      System.out.println("Time: " + (end-start));
      assertInDependencyOrder(collectingHandler.getResolved());
   }

   private Map<String, ServiceDefinition> toMap(List<ServiceDefinition> serviceDefinitions) {
      Map<String, ServiceDefinition> allServiceDefinitions = new HashMap<String, ServiceDefinition>();
      for(ServiceDefinition serviceDefinition : serviceDefinitions)
         allServiceDefinitions.put(serviceDefinition.getName(), serviceDefinition);
      return allServiceDefinitions;
   }

   private void assertInDependencyOrder(final List<ServiceDefinition> serviceDefinitions) {
      Set<String> viewed = new HashSet<String>();
      for(ServiceDefinition serviceDefinition : serviceDefinitions) {
         for(String dep : serviceDefinition.getDependencies()) {
            assertTrue(viewed.contains(dep));
         }
         viewed.add(serviceDefinition.getName());
      }
   }

   private static class CollectingCallback implements Resolver.ResolveCallback {
      private List<ServiceDefinition> resolved = new ArrayList<ServiceDefinition>();
      
      @Override
      public void resolve(final ServiceDefinition serviceDefinition) {
         resolved.add(serviceDefinition);
      }

      public List<ServiceDefinition> getResolved() {
         return resolved;
      }
   };
}
