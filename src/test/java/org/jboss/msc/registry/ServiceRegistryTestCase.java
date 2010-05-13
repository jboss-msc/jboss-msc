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

import java.util.ArrayList;
import java.util.List;

import junit.framework.Assert;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceContainer;
import org.jboss.msc.service.ServiceName;
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
        ServiceRegistry.Factory.create(ServiceContainer.Factory.create()).batchBuilder()
            .add(ServiceDefinition.build(ServiceName.of("7"), Service.NULL_VALUE).addDependencies("11", "8").create())
            .add(ServiceDefinition.build(ServiceName.of("5"), Service.NULL_VALUE).addDependencies("11").create())
            .add(ServiceDefinition.build(ServiceName.of("3"), Service.NULL_VALUE).addDependencies("11", "9").create())
            .add(ServiceDefinition.build(ServiceName.of("11"), Service.NULL_VALUE).addDependencies("2", "9", "10").create())
            .add(ServiceDefinition.build(ServiceName.of("8"), Service.NULL_VALUE).addDependencies("9").create())
            .add(ServiceDefinition.build(ServiceName.of("2"), Service.NULL_VALUE).create())
            .add(ServiceDefinition.build(ServiceName.of("9"), Service.NULL_VALUE).create())
            .add(ServiceDefinition.build(ServiceName.of("10"), Service.NULL_VALUE).create())
            .install();
    }

    @Test
    public void testResolvableWithPreexistingDeps() throws Exception {
        final ServiceRegistry registry = ServiceRegistry.Factory.create(ServiceContainer.Factory.create());
        registry.batchBuilder()
                .add(ServiceDefinition.build(ServiceName.of("2"), Service.NULL_VALUE).create())
                .add(ServiceDefinition.build(ServiceName.of("9"), Service.NULL_VALUE).create())
                .add(ServiceDefinition.build(ServiceName.of("10"), Service.NULL_VALUE).create())
                .install();

        registry.batchBuilder()
                .add(ServiceDefinition.build(ServiceName.of("7"), Service.NULL_VALUE).addDependencies("11", "8").create())
                .add(ServiceDefinition.build(ServiceName.of("5"), Service.NULL_VALUE).addDependencies("11").create())
                .add(ServiceDefinition.build(ServiceName.of("3"), Service.NULL_VALUE).addDependencies("11", "9").create())
                .add(ServiceDefinition.build(ServiceName.of("11"), Service.NULL_VALUE).addDependencies("2", "9", "10").create())
                .add(ServiceDefinition.build(ServiceName.of("8"), Service.NULL_VALUE).addDependencies("9").create())
                .install();
    }


    @Test
    public void testMissingDependency() throws Exception {
        try {
             ServiceRegistry.Factory.create(ServiceContainer.Factory.create()).batchBuilder()
                .add(ServiceDefinition.build(ServiceName.of("7"), Service.NULL_VALUE).addDependencies("11", "8").create())
                .add(ServiceDefinition.build(ServiceName.of("5"), Service.NULL_VALUE).addDependencies("11").create())
                .add(ServiceDefinition.build(ServiceName.of("3"), Service.NULL_VALUE).addDependencies("11", "9").create())
                .add(ServiceDefinition.build(ServiceName.of("11"), Service.NULL_VALUE).addDependencies("2", "9", "10").create())
                .add(ServiceDefinition.build(ServiceName.of("8"), Service.NULL_VALUE).addDependencies("9").create())
                .add(ServiceDefinition.build(ServiceName.of("2"), Service.NULL_VALUE).addDependencies("1").create())
                .add(ServiceDefinition.build(ServiceName.of("9"), Service.NULL_VALUE).create())
                .add(ServiceDefinition.build(ServiceName.of("10"), Service.NULL_VALUE).create())
                .install();
            fail("Should have thrown missing dependency exception");
        } catch (ServiceRegistryException expected) {
        }
    }


    @Test
    public void testCircular() throws Exception {

        try {
             ServiceRegistry.Factory.create(ServiceContainer.Factory.create()).batchBuilder()
                    .add(ServiceDefinition.build(ServiceName.of("7"), Service.NULL_VALUE).addDependencies("5").create())
                    .add(ServiceDefinition.build(ServiceName.of("5"), Service.NULL_VALUE).addDependencies("11").create())
                    .add(ServiceDefinition.build(ServiceName.of("11"), Service.NULL_VALUE).addDependencies("7").create())
                    .install();
            fail("SHould have thrown circular dependency exception");
        } catch (ServiceRegistryException expected) {
        }
    }

    @Test
    public void testMonster() throws Exception {
        ServiceRegistry.BatchBuilder batch =ServiceRegistry.Factory.create(ServiceContainer.Factory.create()).batchBuilder();

        final int totalServiceDefinitions = 10000;

        for (int i = 0; i < totalServiceDefinitions; i++) {
            List<String> deps = new ArrayList<String>();
            int numDeps = Math.min(10, totalServiceDefinitions - i - 1);

            for (int j = 1; j < numDeps + 1; j++) {
                deps.add("test" + (i + j));
            }
            batch.add(ServiceDefinition.build(ServiceName.of("test" + i), Service.NULL_VALUE).addDependencies(deps.toArray(new String[deps.size()])).create());
        }

        long start = System.currentTimeMillis();
        batch.install();
        long end = System.currentTimeMillis();
        System.out.println("Time: " + (end - start));
    }


    @Test
    public void testLargeNoDeps() throws Exception {
        ServiceRegistry.BatchBuilder batch = ServiceRegistry.Factory.create(ServiceContainer.Factory.create()).batchBuilder();

        final int totalServiceDefinitions = 10000;

        for (int i = 0; i < totalServiceDefinitions; i++) {
            batch.add(ServiceDefinition.build(ServiceName.of("test" + i), Service.NULL_VALUE).create());
        }

        long start = System.currentTimeMillis();
        batch.install();
        long end = System.currentTimeMillis();
        System.out.println("Time: " + (end - start));
    }

    @Test
    public void testServiceName() {
        ServiceName serviceName = ServiceName.of("test", "service");
        Assert.assertEquals("test.service", serviceName.toString());

        ServiceName serviceName2 = serviceName.append("2");
        Assert.assertEquals("test.service.2", serviceName2.toString());

        serviceName = serviceName.append(ServiceName.of("other", "service"));
        Assert.assertEquals("test.service.other.service", serviceName.toString());
    }
}
