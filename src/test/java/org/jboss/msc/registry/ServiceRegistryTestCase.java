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

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceContainer;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.ImmediateValue;
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
        final BatchBuilder builder = ServiceRegistry.Factory.create(ServiceContainer.Factory.create()).batchBuilder();
        builder.addService(ServiceName.of("7"), Service.NULL).addDependencies(ServiceName.of("11"), ServiceName.of("8"));
        builder.addService(ServiceName.of("7"), Service.NULL).addDependencies(ServiceName.of("11"), ServiceName.of("8"));
        builder.addService(ServiceName.of("5"), Service.NULL).addDependencies(ServiceName.of("11"));
        builder.addService(ServiceName.of("3"), Service.NULL).addDependencies(ServiceName.of("11"), ServiceName.of("9"));
        builder.addService(ServiceName.of("11"), Service.NULL).addDependencies(ServiceName.of("2"), ServiceName.of("9"), ServiceName.of("10"));
        builder.addService(ServiceName.of("8"), Service.NULL).addDependencies(ServiceName.of("9"));
        builder.addService(ServiceName.of("2"), Service.NULL);
        builder.addService(ServiceName.of("9"), Service.NULL);
        builder.addService(ServiceName.of("10"), Service.NULL);
        builder.install();
    }

    @Test
    public void testResolvableWithPreexistingDeps() throws Exception {
        final ServiceRegistry registry = ServiceRegistry.Factory.create(ServiceContainer.Factory.create());
        final BatchBuilder builder1 = registry.batchBuilder();
        builder1.addService(ServiceName.of("2"), Service.NULL);
        builder1.addService(ServiceName.of("9"), Service.NULL);
        builder1.addService(ServiceName.of("10"), Service.NULL);
        builder1.install();

        final BatchBuilder builder2 = registry.batchBuilder();

        builder2.addService(ServiceName.of("7"), Service.NULL).addDependencies(ServiceName.of("11"), ServiceName.of("8"));
        builder2.addService(ServiceName.of("5"), Service.NULL).addDependencies(ServiceName.of("11"));
        builder2.addService(ServiceName.of("3"), Service.NULL).addDependencies(ServiceName.of("11"), ServiceName.of("9"));
        builder2.addService(ServiceName.of("11"), Service.NULL).addDependencies(ServiceName.of("2"), ServiceName.of("9"), ServiceName.of("10"));
        builder2.addService(ServiceName.of("8"), Service.NULL).addDependencies(ServiceName.of("9"));
        builder2.install();
    }


    @Test
    public void testMissingDependency() throws Exception {
        try {
            final BatchBuilder builder = ServiceRegistry.Factory.create(ServiceContainer.Factory.create()).batchBuilder();
            builder.addService(ServiceName.of("7"), Service.NULL).addDependencies(ServiceName.of("11"), ServiceName.of("8"));
            builder.addService(ServiceName.of("5"), Service.NULL).addDependencies(ServiceName.of("11"));
            builder.addService(ServiceName.of("3"), Service.NULL).addDependencies(ServiceName.of("11"), ServiceName.of("9"));
            builder.addService(ServiceName.of("11"), Service.NULL).addDependencies(ServiceName.of("2"), ServiceName.of("9"), ServiceName.of("10"));
            builder.addService(ServiceName.of("8"), Service.NULL).addDependencies(ServiceName.of("9"));
            builder.addService(ServiceName.of("2"), Service.NULL).addDependencies(ServiceName.of("1"));
            builder.addService(ServiceName.of("9"), Service.NULL);
            builder.addService(ServiceName.of("10"), Service.NULL);
            builder.install();
            fail("Should have thrown missing dependency exception");
        } catch (ServiceRegistryException expected) {
        }
    }


    @Test
    public void testCircular() throws Exception {

        try {
            final BatchBuilder builder = ServiceRegistry.Factory.create(ServiceContainer.Factory.create()).batchBuilder();
            builder.addService(ServiceName.of("7"), Service.NULL).addDependencies(ServiceName.of("5"));
            builder.addService(ServiceName.of("5"), Service.NULL).addDependencies(ServiceName.of("11"));
            builder.addService(ServiceName.of("11"), Service.NULL).addDependencies(ServiceName.of("7"));
            builder.install();
            fail("SHould have thrown circular dependency exception");
        } catch (ServiceRegistryException expected) {
        }
    }

    @Test
    public void testMonster() throws Exception {
        BatchBuilder batch = ServiceRegistry.Factory.create(ServiceContainer.Factory.create()).batchBuilder();

        final int totalServiceDefinitions = 100000;

        for (int i = 0; i < totalServiceDefinitions; i++) {
            List<ServiceName> deps = new ArrayList<ServiceName>();
            int numDeps = Math.min(10, totalServiceDefinitions - i - 1);

            for (int j = 1; j < numDeps + 1; j++) {
                deps.add(ServiceName.of(("test" + (i + j)).intern()));
            }
            batch.addService(ServiceName.of(("test" + i).intern()), Service.NULL).addDependencies(deps.toArray(new ServiceName[deps.size()]));
        }

        long start = System.currentTimeMillis();
        batch.install();
        long end = System.currentTimeMillis();
        System.out.println("Time: " + (end - start));
        
        batch = null;
        System.gc();
        Thread.sleep(10000);
        System.out.println(Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory());
    }


    @Test
    public void testLargeNoDeps() throws Exception {
        BatchBuilder batch = ServiceRegistry.Factory.create(ServiceContainer.Factory.create()).batchBuilder();

        final int totalServiceDefinitions = 10000;

        for (int i = 0; i < totalServiceDefinitions; i++) {
            batch.addService(ServiceName.of("test" + i), Service.NULL);
        }

        long start = System.currentTimeMillis();
        batch.install();
        long end = System.currentTimeMillis();
        System.out.println("Time: " + (end - start));
    }

    public void testBasicInjection() throws Exception {
        BatchBuilder batch = ServiceRegistry.Factory.create(ServiceContainer.Factory.create()).batchBuilder();

        final TestObject testObject = new TestObject();
        final TestObjectService service = new TestObjectService(testObject);
        final Object injectedValue = new Object();
        final Object otherInjectedValue = new Object();

        final Field field = TestObject.class.getDeclaredField("test");
        field.setAccessible(true);

        final BatchServiceBuilder<TestObject> serviceBuilder = batch.addService(ServiceName.of("testService"), service);
        serviceBuilder.addInjection(injectedValue).toFieldValue(new ImmediateValue<Field>(field));
        serviceBuilder.addInjection(otherInjectedValue).toProperty("other");
        batch.install();
    }

    public static class TestObject {
        private Object test;
        private Object other;

        public Object getOther() {
            return other;
        }

        public void setOther(Object other) {
            this.other = other;
        }

        @Override
        public String toString() {
            return "TestObject{" +
                    "test=" + test +
                    ", other=" + other +
                    '}';
        }
    }

    private static class TestObjectService implements Service<TestObject> {

        private final TestObject value;

        private TestObjectService(TestObject value) {
            this.value = value;
        }

        @Override
        public void start(StartContext context) throws StartException {
            System.out.println("Injected: " + value);
        }

        @Override
        public void stop(StopContext context) {
        }

        @Override
        public TestObject getValue() throws IllegalStateException {
            return null;
        }
    }
}
