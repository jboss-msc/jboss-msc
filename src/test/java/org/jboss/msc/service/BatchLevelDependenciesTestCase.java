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

package org.jboss.msc.service;

import org.jboss.msc.service.util.LatchedFinishListener;
import org.junit.BeforeClass;
import org.junit.Test;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Test to verify the functionality of batch level dependencies.
 *
 * @author John Bailey
 */
public class BatchLevelDependenciesTestCase extends AbstractServiceTest {

    private static Field dependenciesField;

    @BeforeClass
    public static void initDependenciesField() throws Exception {
        dependenciesField = ServiceControllerImpl.class.getDeclaredField("dependencies");
        dependenciesField.setAccessible(true);
    }

    @Test
    public void testBatchLevel() throws Exception {
        perfromTest(new ServiceTestInstance() {
            @Override
            public List<BatchBuilder> initializeBatches(ServiceContainer serviceContainer, LatchedFinishListener finishListener) throws Exception {
                final BatchBuilder builder = serviceContainer.batchBuilder();
                builder.addService(ServiceName.of("firstService"), Service.NULL);
                builder.addService(ServiceName.of("secondService"), Service.NULL);
                builder.addService(ServiceName.of("thirdService"), Service.NULL);
                builder.addService(ServiceName.of("fourthService"), Service.NULL);

                builder.addDependency(ServiceName.of("fourthService"));
                builder.addListener(finishListener);
                return Collections.singletonList(builder);
            }

            @Override
            public void performAssertions(ServiceContainer serviceContainer) throws Exception {
                final ServiceController<?> fourthController = serviceContainer.getService(ServiceName.of("fourthService"));

                List<ServiceControllerImpl<?>> dependencies = getServiceDependencies(serviceContainer, ServiceName.of("firstService"));
                assertTrue(dependencies.contains(fourthController));

                dependencies = getServiceDependencies(serviceContainer, ServiceName.of("secondService"));
                assertTrue(dependencies.contains(fourthController));

                dependencies = getServiceDependencies(serviceContainer, ServiceName.of("thirdService"));
                assertTrue(dependencies.contains(fourthController));

                dependencies = getServiceDependencies(serviceContainer, ServiceName.of("fourthService"));
                assertFalse(dependencies.contains(fourthController));
            }
        });
    }

    @Test
    public void testSubBatchLevel() throws Exception {
        perfromTest(new ServiceTestInstance() {
            @Override
            public List<BatchBuilder> initializeBatches(ServiceContainer serviceContainer, LatchedFinishListener finishListener) throws Exception {
                final BatchBuilder builder = serviceContainer.batchBuilder();
                builder.addService(ServiceName.of("firstService"), Service.NULL);
                final SubBatchBuilder subBatchBuilder = builder.subBatchBuilder();
                subBatchBuilder.addService(ServiceName.of("secondService"), Service.NULL);
                subBatchBuilder.addService(ServiceName.of("thirdService"), Service.NULL);
                subBatchBuilder.addService(ServiceName.of("fourthService"), Service.NULL);

                subBatchBuilder.addDependency(ServiceName.of("firstService"));
                subBatchBuilder.addDependency(ServiceName.of("fourthService"));
                return Collections.singletonList(builder);
            }

            @Override
            public void performAssertions(ServiceContainer serviceContainer) throws Exception {
                final ServiceController<?> firstController = serviceContainer.getService(ServiceName.of("firstService"));
                final ServiceController<?> fourthController = serviceContainer.getService(ServiceName.of("fourthService"));

                List<ServiceControllerImpl<?>> dependencies = getServiceDependencies(serviceContainer, ServiceName.of("secondService"));
                assertTrue(dependencies.contains(firstController));
                assertTrue(dependencies.contains(fourthController));

                dependencies = getServiceDependencies(serviceContainer, ServiceName.of("thirdService"));
                assertTrue(dependencies.contains(firstController));
                assertTrue(dependencies.contains(fourthController));

                dependencies = getServiceDependencies(serviceContainer, ServiceName.of("fourthService"));
                assertTrue(dependencies.contains(firstController));
                assertFalse(dependencies.contains(fourthController));
            }
        });
    }


    private List<ServiceControllerImpl<?>> getServiceDependencies(ServiceContainer serviceContainer, final ServiceName serviceName) throws IllegalAccessException {
        ServiceControllerImpl<?> controller = (ServiceControllerImpl) serviceContainer.getService(serviceName);
        ServiceControllerImpl<?>[] deps = (ServiceControllerImpl<?>[]) dependenciesField.get(controller);
        return Arrays.asList(deps);
    }
}