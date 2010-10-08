/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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
package org.jboss.msc.service;

import java.util.Collections;
import org.jboss.msc.service.util.LatchedFinishListener;
import org.jboss.msc.util.TestServiceListener;
import org.junit.BeforeClass;
import org.junit.Test;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Future;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Test case used to ensure functionality for the Resolver.
 *
 * @author John Bailey
 */
public class ServiceResolverTestCase extends AbstractServiceTest {

    private static Field dependenciesField;

    @BeforeClass
    public static void initDependenciesField() throws Exception {
        dependenciesField = ServiceControllerImpl.class.getDeclaredField("dependencies");
        dependenciesField.setAccessible(true);
    }

    @Test
    public void testResolvable() throws Exception {
        final BatchBuilder builder = serviceContainer.batchBuilder();
        final LatchedListener listener = new LatchedListener();
        builder.addListener(listener);
        builder.addService(ServiceName.of("7"), Service.NULL).addDependencies(ServiceName.of("11"), ServiceName.of("8"));
        builder.addService(ServiceName.of("5"), Service.NULL).addDependencies(ServiceName.of("11"));
        builder.addService(ServiceName.of("3"), Service.NULL).addDependencies(ServiceName.of("11"), ServiceName.of("9"));
        builder.addService(ServiceName.of("11"), Service.NULL).addDependencies(ServiceName.of("2"), ServiceName.of("9"), ServiceName.of("10"));
        builder.addService(ServiceName.of("8"), Service.NULL).addDependencies(ServiceName.of("9"));
        builder.addService(ServiceName.of("2"), Service.NULL);
        builder.addService(ServiceName.of("9"), Service.NULL);
        builder.addService(ServiceName.of("10"), Service.NULL);

        builder.install();
        listener.await();

        assertEquals(8, listener.startedControllers.size());
        final List<ServiceController<?>> processed = new ArrayList<ServiceController<?>>(listener.startedControllers.size());
        for(ServiceController<?> serviceController : listener.startedControllers) {
            assertEquals(ServiceController.State.UP, serviceController.getState());
            final List<ServiceController<?>> deps = getServiceDependencies(serviceContainer, serviceController);
            for(ServiceController<?> depController : deps) {
                if(depController.getValue() != serviceContainer)
                    assertTrue(processed.contains(depController));
            }
            processed.add(serviceController);
        }
    }

    @Test
    public void testResolvableWithPreexistingDeps() throws Exception {
        final LatchedListener listener = new LatchedListener();
        final BatchBuilder builder1 = serviceContainer.batchBuilder();
        builder1.addListener(listener);
        builder1.addService(ServiceName.of("2"), Service.NULL);
        builder1.addService(ServiceName.of("9"), Service.NULL);
        builder1.addService(ServiceName.of("10"), Service.NULL);

        final BatchBuilder builder2 = serviceContainer.batchBuilder();
        builder2.addListener(listener);
        builder2.addService(ServiceName.of("7"), Service.NULL).addDependencies(ServiceName.of("11"), ServiceName.of("8"));
        builder2.addService(ServiceName.of("5"), Service.NULL).addDependencies(ServiceName.of("11"));
        builder2.addService(ServiceName.of("3"), Service.NULL).addDependencies(ServiceName.of("11"), ServiceName.of("9"));
        builder2.addService(ServiceName.of("11"), Service.NULL)
            .addDependencies(ServiceName.of("2"), ServiceName.of("9"), ServiceName.of("10"));
        builder2.addService(ServiceName.of("8"), Service.NULL).addDependencies(ServiceName.of("9"));

        builder1.install();
        builder2.install();

        listener.await();

        assertEquals(8, listener.startedControllers.size());
        final List<ServiceController<?>> processed = new ArrayList<ServiceController<?>>(listener.startedControllers.size());
        for(ServiceController<?> serviceController : listener.startedControllers) {
            assertEquals(ServiceController.State.UP, serviceController.getState());
            final List<ServiceController<?>> deps = getServiceDependencies(serviceContainer, serviceController);
            for(ServiceController<?> depController : deps) {
                if(depController.getValue() != serviceContainer)
                    assertTrue(processed.contains(depController));
            }
            processed.add(serviceController);
        }
    }


    @Test
    public void testMissingDependency() throws Exception {
        final BatchBuilder builder = serviceContainer.batchBuilder();
        builder.addService(ServiceName.of("7"), Service.NULL).addDependencies(ServiceName.of("11"), ServiceName.of("8"));
        builder.addService(ServiceName.of("5"), Service.NULL).addDependencies(ServiceName.of("11"));
        builder.addService(ServiceName.of("3"), Service.NULL).addDependencies(ServiceName.of("11"), ServiceName.of("9"));
        builder.addService(ServiceName.of("11"), Service.NULL).addDependencies(ServiceName.of("2"), ServiceName.of("9"), ServiceName.of("10"));
        builder.addService(ServiceName.of("8"), Service.NULL).addDependencies(ServiceName.of("9"));
        builder.addService(ServiceName.of("2"), Service.NULL).addDependencies(ServiceName.of("1"));
        builder.addService(ServiceName.of("9"), Service.NULL);
        builder.addService(ServiceName.of("10"), Service.NULL);

        try {
            builder.install();
            fail("Should have thrown a MissingDependencyException");
        } catch(ServiceRegistryException expected) {
            assertTrue(expected.getCause() instanceof MissingDependencyException);
        }
    }

    @Test
    public void testCircular() throws Exception {
        final BatchBuilder builder = serviceContainer.batchBuilder();
        builder.addService(ServiceName.of("7"), Service.NULL).addDependencies(ServiceName.of("5"));
        builder.addService(ServiceName.of("5"), Service.NULL).addDependencies(ServiceName.of("11"));
        builder.addService(ServiceName.of("11"), Service.NULL).addDependencies(ServiceName.of("7"));
        try {
            builder.install();
            fail("Should have thrown circular dependency exception");
        } catch(ServiceRegistryException expected) {
        }
    }


    @Test
    public void testOptionalDependency() throws Exception {
        final BatchBuilder builder = serviceContainer.batchBuilder();
        final TestServiceListener listener = new TestServiceListener();
        builder.addListener(listener);

        builder.addService(ServiceName.of("7"), Service.NULL).addOptionalDependencies(ServiceName.of("11"), ServiceName.of("8"));

        final Future<ServiceController<?>> startFuture = listener.expectServiceStart(ServiceName.of("7"));

        builder.install();

        assertEquals(ServiceController.State.UP, startFuture.get().getState());
    }


    private List<ServiceController<?>> getServiceDependencies(ServiceContainer serviceContainer, final ServiceController<?> serviceController) throws IllegalAccessException {
        ServiceController<?>[] deps = (ServiceControllerImpl<?>[]) dependenciesField.get(serviceController);
        return Arrays.asList(deps);
    }

    private static class LatchedListener extends LatchedFinishListener {

        final List<ServiceController<? extends Object>> startedControllers = Collections.synchronizedList(new ArrayList<ServiceController<? extends Object>>());

        @Override
        public void serviceStarted(ServiceController<? extends Object> serviceController) {
            startedControllers.add(serviceController);
            super.serviceStarted(serviceController);
        }
    }
}
