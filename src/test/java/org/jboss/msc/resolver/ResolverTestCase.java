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
   private final Resolver.ResolveHandler NO_OP_HANDLER = new Resolver.ResolveHandler() {
      @Override
      public void resolve(final Item item) {
      }
   };

   @Test
   public void testResolvable() throws Exception {
      CollectingHandler handler = new CollectingHandler();
      resolver.resolve(
         toMap(Arrays.asList(
            new Item("7", "11", "8"),
            new Item("5", "11"),
            new Item("3", "11", "9"),
            new Item("11", "2", "9", "10"),
            new Item("8", "9"),
            new Item("2"),
            new Item("9"),
            new Item("10")
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
               new Item("7", "11", "8"),
               new Item("5", "11"),
               new Item("3", "11", "9"),
               new Item("11", "2", "9", "10"),
               new Item("8", "9"),
               new Item("2", "1"),
               new Item("9"),
               new Item("10")
            )),
            NO_OP_HANDLER
         );
         fail("Should have thrown missing dependency exception");
      } catch(MissingDependencyException expected) {}
   }


   @Test
   public void testCircular() throws Exception {

      try {
         resolver.resolve(
            toMap(Arrays.asList(
               new Item("7", "5"),
               new Item("5", "11"),
               new Item("11", "7")
            )),
            NO_OP_HANDLER
         );
         fail("SHould have thrown circular dependency exception");
      } catch(CircularDependencyException expected) {}
   }


   @Test
   public void testMonster() throws Exception {

      final int totalItems = 10000;

      final List<Item> items = new ArrayList<Item>(totalItems);
      for(int i = 0; i < totalItems; i++) {
         List<String> deps = new ArrayList<String>();
         int numDeps = Math.min(10, totalItems - i - 1);

         for(int j = 1; j < numDeps + 1; j++) {
            deps.add("test" + (i+j));
         }
         items.add(new Item("test" + i, deps.toArray(new String[deps.size()])));
      }

      Map<String, Item> allItems = new HashMap<String, Item>();
      for(Item item : items)
         allItems.put(item.getName(), item);

      CollectingHandler collectingHandler = new CollectingHandler();
      long start = System.currentTimeMillis();
      resolver.resolve(allItems, collectingHandler);
      long end = System.currentTimeMillis();
      System.out.println("Time: " + (end-start));
      assertInDependencyOrder(collectingHandler.getResolved());
   }

   private Map<String, Item> toMap(List<Item> items) {
      Map<String, Item> allItems = new HashMap<String, Item>();
      for(Item item : items)
         allItems.put(item.getName(), item);
      return allItems;
   }

   private void assertInDependencyOrder(final List<Item> items) {
      Set<String> viewed = new HashSet<String>();
      for(Item item : items) {
         for(String dep : item.getDependencies()) {
            assertTrue(viewed.contains(dep));
         }
         viewed.add(item.getName());
      }
   }

   private static class CollectingHandler implements Resolver.ResolveHandler {
      private List<Item> resolved = new ArrayList<Item>();
      
      @Override
      public void resolve(final Item item) {
         resolved.add(item);
      }

      public List<Item> getResolved() {
         return resolved;
      }
   };
}
