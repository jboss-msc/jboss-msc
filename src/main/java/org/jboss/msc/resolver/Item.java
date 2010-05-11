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

import java.util.HashSet;
import java.util.Set;

/**
 * NONSENSE placeholder to be removed when a formal service definition is available.
 * 
 * @author John Bailey
 */
public class Item {
   private final String name;
   private final Set<String> dependencies = new HashSet<String>();
   private boolean visited;
   private boolean resolved;

   public Item(final String name, final String... dependencies)
   {
      this.name = name;
      for(String dependency : dependencies)
         this.dependencies.add(dependency);
   }

   public String getName() {
      return name;
   }

   public Set<String> getDependencies()
   {
      return dependencies;
   }

   public boolean isVisited()
   {
      return visited;
   }

   public void setVisited(final boolean visited)
   {
      this.visited = visited;
   }

   public boolean isResolved()
   {
      return resolved;
   }

   public void setResolved(final boolean resolved)
   {
      this.resolved = resolved;
   }

   @Override
   public int hashCode()
   {
      return name != null ? name.hashCode() : 0;
   }

   @Override
   public String toString()
   {
      return "Item{" +
         "name='" + name + '\'' +
         ", dependencies=" + dependencies +
         '}';
   }

   @Override
   public boolean equals(final Object o)
   {
      if(this == o) return true;
      if(o == null || getClass() != o.getClass()) return false;

      final Item item = (Item) o;

      if(name != null ? !name.equals(item.name) : item.name != null) return false;

      return true;
   }
}
