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

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;

/**
 * Test to verify the functionality of batch level dependencies.
 *
 * @author John Bailey
 */
public class BatchLevelDependenciesTest {

    private static Field dependenciesField;

    @BeforeClass
    public static void initDependenciesField() throws Exception {
        dependenciesField = ServiceControllerImpl.class.getDeclaredField("dependencies");
        dependenciesField.setAccessible(true);
    }

    @Test
    public void testBatchLevel() throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);
        final TimingServiceListener listener = new TimingServiceListener(new TimingServiceListener.FinishListener() {
            public void done(final TimingServiceListener timingServiceListener) {
                latch.countDown();
            }
        });

        final ServiceContainer serviceContainer = ServiceContainer.Factory.create();

        final BatchBuilder builder = serviceContainer.batchBuilder();
        builder.addService(ServiceName.of("firstService"), Service.NULL).addListener(listener);
        builder.addService(ServiceName.of("secondService"), Service.NULL).addListener(listener);
        builder.addService(ServiceName.of("thirdService"), Service.NULL).addListener(listener);
        builder.addService(ServiceName.of("fourthService"), Service.NULL).addListener(listener);

        builder.addDependency(ServiceName.of("fourthService"));

        builder.install();
        listener.finishBatch();
        latch.await();

        final ServiceController<?> fourthController = serviceContainer.getService(ServiceName.of("fourthService"));

        List<ServiceControllerImpl<?>> dependencies = getServiceDependencies(serviceContainer, ServiceName.of("firstService"));
        Assert.assertTrue(dependencies.contains(fourthController));

        dependencies = getServiceDependencies(serviceContainer, ServiceName.of("secondService"));
        Assert.assertTrue(dependencies.contains(fourthController));

        dependencies = getServiceDependencies(serviceContainer, ServiceName.of("thirdService"));
        Assert.assertTrue(dependencies.contains(fourthController));

        dependencies = getServiceDependencies(serviceContainer, ServiceName.of("fourthService"));
        Assert.assertFalse(dependencies.contains(fourthController));
    }

    @Test
    public void testSubBatchLevel() throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);
        final TimingServiceListener listener = new TimingServiceListener(new TimingServiceListener.FinishListener() {
            public void done(final TimingServiceListener timingServiceListener) {
                latch.countDown();
            }
        });

        final ServiceContainer serviceContainer = ServiceContainer.Factory.create();

        final BatchBuilder builder = serviceContainer.batchBuilder();
        builder.addService(ServiceName.of("firstService"), Service.NULL).addListener(listener);
        final SubBatchBuilder subBatchBuilder = builder.subBatchBuilder();
        subBatchBuilder.addService(ServiceName.of("secondService"), Service.NULL).addListener(listener);
        subBatchBuilder.addService(ServiceName.of("thirdService"), Service.NULL).addListener(listener);
        subBatchBuilder.addService(ServiceName.of("fourthService"), Service.NULL).addListener(listener);

        subBatchBuilder.addDependency(ServiceName.of("firstService"));
        subBatchBuilder.addDependency(ServiceName.of("fourthService"));

        builder.install();
        listener.finishBatch();
        latch.await();

        final ServiceController<?> firstController = serviceContainer.getService(ServiceName.of("firstService"));
        final ServiceController<?> fourthController = serviceContainer.getService(ServiceName.of("fourthService"));

        List<ServiceControllerImpl<?>> dependencies = getServiceDependencies(serviceContainer, ServiceName.of("secondService"));
        Assert.assertTrue(dependencies.contains(firstController));
        Assert.assertTrue(dependencies.contains(fourthController));

        dependencies = getServiceDependencies(serviceContainer, ServiceName.of("thirdService"));
        Assert.assertTrue(dependencies.contains(firstController));
        Assert.assertTrue(dependencies.contains(fourthController));

        dependencies = getServiceDependencies(serviceContainer, ServiceName.of("fourthService"));
        Assert.assertTrue(dependencies.contains(firstController));
        Assert.assertFalse(dependencies.contains(fourthController));
    }


    private List<ServiceControllerImpl<?>> getServiceDependencies(ServiceContainer serviceContainer, final ServiceName serviceName) throws IllegalAccessException {
        ServiceControllerImpl<?> controller = (ServiceControllerImpl)serviceContainer.getService(serviceName);
        ServiceControllerImpl<?>[] deps = (ServiceControllerImpl<?>[])dependenciesField.get(controller);
        return Arrays.asList(deps);
    }
}