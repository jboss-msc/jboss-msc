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

import org.junit.Test;

import static org.junit.Assert.fail;

/**
 * Test case used to ensure functionality for the Resolver.
 *
 * @author John Bailey
 */
public class ServiceRegistryTestCase {

    @Test
    public void testResolvable() throws Exception {
         new ServiceRegistry().createServiceBatch()
            .add(ServiceDefinition.create("7", "11", "8"))
            .add(ServiceDefinition.create("5", "11"))
            .add(ServiceDefinition.create("3", "11", "9"))
            .add(ServiceDefinition.create("11", "2", "9", "10"))
            .add(ServiceDefinition.create("8", "9"))
            .add(ServiceDefinition.create("2"))
            .add(ServiceDefinition.create("9"))
            .add(ServiceDefinition.create("10"))
            .install();
        //assertInDependencyOrder(handler.getResolved());
    }

    @Test
    public void testResolvableWithPreexistingDeps() throws Exception {
        final ServiceRegistry registry = new ServiceRegistry();
        registry.createServiceBatch()
                .add(ServiceDefinition.create("2"))
                .add(ServiceDefinition.create("9"))
                .add(ServiceDefinition.create("10"))
                .install();

        registry.createServiceBatch()
                .add(ServiceDefinition.create("7", "11", "8"))
                .add(ServiceDefinition.create("5", "11"))
                .add(ServiceDefinition.create("3", "11", "9"))
                .add(ServiceDefinition.create("11", "2", "9", "10"))
                .add(ServiceDefinition.create("8", "9"))
                .install();
    }


    @Test
    public void testMissingDependency() throws Exception {
        try {
             new ServiceRegistry().createServiceBatch()
                .add(ServiceDefinition.create("7", "11", "8"))
                .add(ServiceDefinition.create("5", "11"))
                .add(ServiceDefinition.create("3", "11", "9"))
                .add(ServiceDefinition.create("11", "2", "9", "10"))
                .add(ServiceDefinition.create("8", "9"))
                .add(ServiceDefinition.create("2", "1"))
                .add(ServiceDefinition.create("9"))
                .add(ServiceDefinition.create("10"))
                .install();
            fail("Should have thrown missing dependency exception");
        } catch (ServiceRegistryException expected) {
        }
    }


    @Test
    public void testCircular() throws Exception {

        try {
             new ServiceRegistry().createServiceBatch()
                    .add(ServiceDefinition.create("7", "5"))
                    .add(ServiceDefinition.create("5", "11"))
                    .add(ServiceDefinition.create("11", "7"))
                    .install();
            fail("SHould have thrown circular dependency exception");
        } catch (ServiceRegistryException expected) {
        }
    }

    @Test
    public void testMonster() throws Exception {

        final int totalServiceDefinitions = 10000;

        final List<ServiceDefinition> serviceDefinitions = new ArrayList<ServiceDefinition>(totalServiceDefinitions);
        for (int i = 0; i < totalServiceDefinitions; i++) {
            List<String> deps = new ArrayList<String>();
            int numDeps = Math.min(10, totalServiceDefinitions - i - 1);

            for (int j = 1; j < numDeps + 1; j++) {
                deps.add("test" + (i + j));
            }
            serviceDefinitions.add(ServiceDefinition.create("test" + i, deps.toArray(new String[deps.size()])));
        }

        long start = System.currentTimeMillis();
         new ServiceRegistry().install(toMap(serviceDefinitions));
        long end = System.currentTimeMillis();
        System.out.println("Time: " + (end - start));
        //assertInDependencyOrder(collectingHandler.getResolved());
    }

    private Map<ServiceName, ServiceDefinition> toMap(final Collection<ServiceDefinition> definitions) {
        final Map<ServiceName, ServiceDefinition> definitionMap = new HashMap<ServiceName, ServiceDefinition>();
        for(ServiceDefinition definition : definitions) {
            definitionMap.put(definition.getName(), definition);
        }
        return definitionMap;
    }
}
