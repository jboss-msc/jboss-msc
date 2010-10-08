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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Future;

import org.jboss.msc.service.util.LatchedFinishListener;
import org.jboss.msc.util.TestServiceListener;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Test case used to ensure functionality for the Resolver.
 *
 * @author John Bailey
 */
public class ServiceResolverTestCase extends AbstractServiceTest {

    private static Field dependenciesField;

    @BeforeClass
    public static void initDependenciesField() throws Exception {
        dependenciesField = ServiceInstanceImpl.class.getDeclaredField("dependencies");
        dependenciesField.setAccessible(true);
    }

    @Test
    public void testResolvable() throws Exception {
        final OrderedStartListener startListener = new OrderedStartListener();
        final BatchBuilder builder = serviceContainer.batchBuilder();
        final LatchedFinishListener listener = new LatchedFinishListener();
        builder.addListener(listener);
        builder.addService(ServiceName.of("7"), Service.NULL).addDependencies(ServiceName.of("11"), ServiceName.of("8"))
            .install();
        builder.addService(ServiceName.of("5"), Service.NULL).addDependencies(ServiceName.of("11")).install();
        builder.addService(ServiceName.of("3"), Service.NULL).addDependencies(ServiceName.of("11"), ServiceName.of("9"))
            .install();
        builder.addService(ServiceName.of("11"), Service.NULL)
            .addDependencies(ServiceName.of("2"), ServiceName.of("9"), ServiceName.of("10"))
            .install();
        builder.addService(ServiceName.of("8"), Service.NULL).addDependencies(ServiceName.of("9")).install();
        builder.addService(ServiceName.of("2"), Service.NULL).install();
        builder.addService(ServiceName.of("9"), Service.NULL).install();
        builder.addService(ServiceName.of("10"), Service.NULL).install();

        builder.addListener(startListener);

        builder.install();
        listener.await();

        assertEquals(8, startListener.startedControllers.size());
        final List<ServiceController<?>> processed = new ArrayList<ServiceController<?>>(startListener.startedControllers.size());
        for(ServiceController<?> serviceController : startListener.startedControllers) {
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
        final OrderedStartListener startListener = new OrderedStartListener();
        final LatchedFinishListener finishListener = new LatchedFinishListener();
        final BatchBuilder builder1 = serviceContainer.batchBuilder();
        builder1.addListener(finishListener).addListener(startListener);
        builder1.addService(ServiceName.of("2"), Service.NULL).install();
        builder1.addService(ServiceName.of("9"), Service.NULL).install();
        builder1.addService(ServiceName.of("10"), Service.NULL).install();

        final BatchBuilder builder2 = serviceContainer.batchBuilder();
        builder2.addListener(finishListener).addListener(startListener);
        builder2.addService(ServiceName.of("7"), Service.NULL).addDependencies(ServiceName.of("11"), ServiceName.of("8"))
            .install();
        builder2.addService(ServiceName.of("5"), Service.NULL).addDependencies(ServiceName.of("11")).install();
        builder2.addService(ServiceName.of("3"), Service.NULL).addDependencies(ServiceName.of("11"), ServiceName.of("9"))
            .install();
        builder2.addService(ServiceName.of("11"), Service.NULL)
            .addDependencies(ServiceName.of("2"), ServiceName.of("9"), ServiceName.of("10"))
            .install();
        builder2.addService(ServiceName.of("8"), Service.NULL).addDependencies(ServiceName.of("9")).install();

        builder1.install();
        builder2.install();

        finishListener.await();

        assertEquals(8, startListener.startedControllers.size());
        final List<ServiceController<?>> processed = new ArrayList<ServiceController<?>>(startListener.startedControllers.size());
        for(ServiceController<?> serviceController : startListener.startedControllers) {
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
    public void testOptionalDependency() throws Exception {
        final BatchBuilder builder = serviceContainer.batchBuilder();
        final TestServiceListener listener = new TestServiceListener();
        builder.addListener(listener);

        builder.addService(ServiceName.of("7"), Service.NULL)
            .addOptionalDependencies(ServiceName.of("11"), ServiceName.of("8"))
            .install();

        final Future<ServiceController<?>> startFuture = listener.expectServiceStart(ServiceName.of("7"));

        builder.install();

        assertEquals(ServiceController.State.UP, startFuture.get().getState());
    }


    private List<ServiceController<?>> getServiceDependencies(ServiceContainer serviceContainer, final ServiceController<?> serviceController) throws IllegalAccessException {
        AbstractDependency[] deps = (AbstractDependency[]) dependenciesField.get(serviceController);
        List<ServiceController<?>> depInstances = new ArrayList<ServiceController<?>>(deps.length);
        for (AbstractDependency dep: deps) {
            ServiceController<?> depInstance = (ServiceController<?>) ((ServiceRegistrationImpl)dep).getInstance();
            if (depInstance != null) {
                depInstances.add(depInstance);
            }
        }
        return depInstances;
    }

    private static class OrderedStartListener extends AbstractServiceListener<Object> {

        private final List<ServiceController<? extends Object>> startedControllers = Collections.synchronizedList(new ArrayList<ServiceController<? extends Object>>());

        @Override
        public void serviceStarted(ServiceController<? extends Object> serviceController) {
            startedControllers.add(serviceController);
        }
    }
}
