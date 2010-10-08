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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;

import org.jboss.msc.util.TestServiceListener;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Test to verify the functionality of batch level dependencies.
 *
 * @author John Bailey
 */
public class BatchLevelDependenciesTestCase extends AbstractServiceTest {

    private static Field dependenciesField;

    @BeforeClass
    public static void initDependenciesField() throws Exception {
        dependenciesField = ServiceInstanceImpl.class.getDeclaredField("dependencies");
        dependenciesField.setAccessible(true);
    }

    @Test
    public void testBatchLevel() throws Exception {
        final BatchBuilder builder = serviceContainer.batchBuilder();
        final TestServiceListener listener = new TestServiceListener();
        builder.addListener(listener);

        builder.addService(ServiceName.of("firstService"), Service.NULL).install();
        builder.addService(ServiceName.of("secondService"), Service.NULL).install();
        builder.addService(ServiceName.of("thirdService"), Service.NULL).install();
        builder.addService(ServiceName.of("fourthService"), Service.NULL).install();

        builder.addDependency(ServiceName.of("fourthService"));

        final Future<ServiceController<?>> firstService = listener.expectServiceStart(ServiceName.of("firstService"));
        final Future<ServiceController<?>> secondService = listener.expectServiceStart(ServiceName.of("secondService"));
        final Future<ServiceController<?>> thirdService = listener.expectServiceStart(ServiceName.of("thirdService"));
        final Future<ServiceController<?>> fourthService = listener.expectServiceStart(ServiceName.of("fourthService"));

        builder.install();

        final ServiceController<?> fourthController = fourthService.get();
        assertNotNull(fourthController);

        final ServiceController<?> firstController = firstService.get();
        assertNotNull(firstController);

        List<ServiceInstanceImpl<?>> dependencies = getServiceDependencies(firstController);
        assertTrue(dependencies.contains(fourthController));

        dependencies = getServiceDependencies(secondService.get());
        assertTrue(dependencies.contains(fourthController));

        dependencies = getServiceDependencies(thirdService.get());
        assertTrue(dependencies.contains(fourthController));

        dependencies = getServiceDependencies(fourthController);
        assertFalse(dependencies.contains(fourthController));
    }

    @Test
    public void testSubBatchLevel() throws Exception {
        final BatchBuilder builder = serviceContainer.batchBuilder();
        final TestServiceListener listener = new TestServiceListener();
        builder.addListener(listener);

        builder.addService(ServiceName.of("firstService"), Service.NULL).install();
        final ServiceTarget subBatchBuilder = builder.subTarget();

        subBatchBuilder.addDependency(ServiceName.of("firstService"));
        subBatchBuilder.addDependency(ServiceName.of("fourthService"));

        subBatchBuilder.addService(ServiceName.of("secondService"), Service.NULL).install();
        subBatchBuilder.addService(ServiceName.of("thirdService"), Service.NULL).install();
        subBatchBuilder.addService(ServiceName.of("fourthService"), Service.NULL).install();

        final Future<ServiceController<?>> firstService = listener.expectServiceStart(ServiceName.of("firstService"));
        final Future<ServiceController<?>> secondService = listener.expectServiceStart(ServiceName.of("secondService"));
        final Future<ServiceController<?>> thirdService = listener.expectServiceStart(ServiceName.of("thirdService"));
        final Future<ServiceController<?>> fourthService = listener.expectServiceStart(ServiceName.of("fourthService"));

        builder.install();

        final ServiceController<?> firstController = firstService.get();
        assertNotNull(firstController);
        final ServiceController<?> fourthController = fourthService.get();
        assertNotNull(fourthController);

        List<ServiceInstanceImpl<?>> dependencies = getServiceDependencies(secondService.get());
        assertTrue(dependencies.contains(firstController));
        assertTrue(dependencies.contains(fourthController));

        dependencies = getServiceDependencies(thirdService.get());
        assertTrue(dependencies.contains(firstController));
        assertTrue(dependencies.contains(fourthController));

        dependencies = getServiceDependencies(fourthController);
        assertTrue(dependencies.contains(firstController));
        assertFalse(dependencies.contains(fourthController));
    }


    private List<ServiceInstanceImpl<?>> getServiceDependencies(ServiceController<?> serviceController) throws IllegalAccessException {
        AbstractDependency[] deps = (AbstractDependency[]) dependenciesField.get(serviceController);
        List<ServiceInstanceImpl<?>> depInstances = new ArrayList<ServiceInstanceImpl<?>>(deps.length);
        for (AbstractDependency dep: deps) {
            ServiceInstanceImpl<?> depInstance = (ServiceInstanceImpl<?>) ((ServiceRegistrationImpl)dep).getInstance();
            if (depInstance != null) {
                depInstances.add(depInstance);
            }
        }
        return depInstances;
    }
}