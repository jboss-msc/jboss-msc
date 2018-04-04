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
import static org.junit.Assert.fail;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jboss.msc.util.TestServiceListener;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Test case used to ensure functionality for the Resolver.
 *
 * @author John Bailey
 */
public class ServiceResolverTestCase extends AbstractServiceTest {

    private static Field requiresField;

    @BeforeClass
    public static void initDependenciesField() throws Exception {
        requiresField = ServiceControllerImpl.class.getDeclaredField("requires");
        requiresField.setAccessible(true);
    }

    @Test
    public void testResolvable() throws Exception {
        final TestServiceListener listener = new TestServiceListener();
        serviceContainer.addListener(listener);
        final Set<ServiceController<?>> expected = new HashSet<ServiceController<?>>();
        expected.add(serviceContainer.addService(ServiceName.of("7"), Service.NULL).addDependencies(ServiceName.of("11"), ServiceName.of("8"))
            .install());
        expected.add(serviceContainer.addService(ServiceName.of("5"), Service.NULL).addDependencies(ServiceName.of("11")).install());
        expected.add(serviceContainer.addService(ServiceName.of("3"), Service.NULL).addDependencies(ServiceName.of("11"), ServiceName.of("9"))
            .install());
        expected.add(serviceContainer.addService(ServiceName.of("11"), Service.NULL)
            .addDependencies(ServiceName.of("2"), ServiceName.of("9"), ServiceName.of("10"))
            .install());
        expected.add(serviceContainer.addService(ServiceName.of("8"), Service.NULL).addDependencies(ServiceName.of("9")).install());
        expected.add(serviceContainer.addService(ServiceName.of("2"), Service.NULL).install());
        expected.add(serviceContainer.addService(ServiceName.of("9"), Service.NULL).install());
        expected.add(serviceContainer.addService(ServiceName.of("10"), Service.NULL).install());

        serviceContainer.awaitStability();

        for(ServiceController<?> serviceController : expected) {
            assertEquals(ServiceController.State.UP, serviceController.getState());
            final List<ServiceController<?>> deps = getServiceDependencies(serviceController);
            for(ServiceController<?> depController : deps) {
                assertTrue("Missing dependency " + depController, expected.contains(depController));
            }
        }
    }

    @Test
    public void testResolvableWithPreexistingDeps() throws Exception {
        final TestServiceListener listener = new TestServiceListener();
        serviceContainer.addListener(listener);
        final Set<ServiceController<?>> expected = new HashSet<ServiceController<?>>();
        expected.add(serviceContainer.addService(ServiceName.of("2"), Service.NULL).install());
        expected.add(serviceContainer.addService(ServiceName.of("9"), Service.NULL).install());
        expected.add(serviceContainer.addService(ServiceName.of("10"), Service.NULL).install());

        expected.add(serviceContainer.addService(ServiceName.of("7"), Service.NULL).addDependencies(ServiceName.of("11"), ServiceName.of("8"))
            .install());
        expected.add(serviceContainer.addService(ServiceName.of("5"), Service.NULL).addDependencies(ServiceName.of("11")).install());
        expected.add(serviceContainer.addService(ServiceName.of("3"), Service.NULL).addDependencies(ServiceName.of("11"), ServiceName.of("9"))
            .install());
        expected.add(serviceContainer.addService(ServiceName.of("11"), Service.NULL)
            .addDependencies(ServiceName.of("2"), ServiceName.of("9"), ServiceName.of("10"))
            .install());
        expected.add(serviceContainer.addService(ServiceName.of("8"), Service.NULL).addDependencies(ServiceName.of("9")).install());
        try {
            serviceContainer.addService(ServiceName.of("8"), Service.NULL).addDependencies(ServiceName.of("9")).install();
            fail("DuplicateServiceException expected");
        } catch (DuplicateServiceException e) {}

        serviceContainer.awaitStability();

        for(ServiceController<?> serviceController : expected) {
            assertEquals(ServiceController.State.UP, serviceController.getState());
            final List<ServiceController<?>> deps = getServiceDependencies(serviceController);
            for(ServiceController<?> depController : deps) {
                assertTrue(expected.contains(depController));
            }
        }
    }

    private List<ServiceController<?>> getServiceDependencies(final ServiceController<?> serviceController) throws IllegalAccessException {
        Set<Dependency> deps = (Set<Dependency>) requiresField.get(serviceController);
        List<ServiceController<?>> depInstances = new ArrayList<ServiceController<?>>();
        for (Dependency dep: deps) {
            ServiceController<?> depInstance = dep.getDependencyController();
            if (depInstance != null) {
                depInstances.add(depInstance);
            }
        }
        return depInstances;
    }
}
