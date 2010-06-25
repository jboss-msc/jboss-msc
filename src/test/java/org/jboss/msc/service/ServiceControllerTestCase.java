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
import org.junit.Assert;
import org.junit.Test;

/**
 * Test to verify ServiceController behavior.
 * 
 * @author John E. Bailey
 */
public class ServiceControllerTestCase {

    @Test
    public void testStartModes() throws Exception {
        final LatchedFinishListener listener = new LatchedFinishListener();
        final ServiceContainer serviceContainer = ServiceContainer.Factory.create();

        final BatchBuilder batch = serviceContainer.batchBuilder();

        batch.addService(ServiceName.of("automatic"), Service.NULL).setInitialMode(ServiceController.Mode.AUTOMATIC).addListener(listener);
        batch.addService(ServiceName.of("never"), Service.NULL).setInitialMode(ServiceController.Mode.NEVER);
        batch.addService(ServiceName.of("immediate"), Service.NULL).setInitialMode(ServiceController.Mode.IMMEDIATE).addListener(listener);
        batch.addService(ServiceName.of("on_demand"), Service.NULL).setInitialMode(ServiceController.Mode.ON_DEMAND);
        batch.install();
        listener.await();

        assertState(serviceContainer, ServiceName.of("automatic"), ServiceController.State.UP);
        assertState(serviceContainer, ServiceName.of("never"), ServiceController.State.DOWN);
        assertState(serviceContainer, ServiceName.of("immediate"), ServiceController.State.UP);
        assertState(serviceContainer, ServiceName.of("on_demand"), ServiceController.State.DOWN);
        serviceContainer.shutdown();
    }

    @Test
    public void testAutomatic() throws Exception {
        final ServiceContainer serviceContainer = ServiceContainer.Factory.create();

        final BatchBuilder batch = serviceContainer.batchBuilder();

        batch.addService(ServiceName.of("automatic"), Service.NULL)
              .setInitialMode(ServiceController.Mode.AUTOMATIC)
              .addDependencies(ServiceName.of("never"));
        batch.addService(ServiceName.of("never"), Service.NULL).setInitialMode(ServiceController.Mode.NEVER);
        
        batch.install();
        Thread.sleep(50);
        
        assertState(serviceContainer, ServiceName.of("automatic"), ServiceController.State.DOWN);
        assertState(serviceContainer, ServiceName.of("never"), ServiceController.State.DOWN);

        serviceContainer.getService(ServiceName.of("never")).setMode(ServiceController.Mode.IMMEDIATE);

        Thread.sleep(50);

        assertState(serviceContainer, ServiceName.of("automatic"), ServiceController.State.UP);
        assertState(serviceContainer, ServiceName.of("never"), ServiceController.State.UP);

        serviceContainer.shutdown();
    }

    @Test
    public void testOnDemand() throws Exception {
        final ServiceContainer serviceContainer = ServiceContainer.Factory.create();

        final BatchBuilder batch = serviceContainer.batchBuilder();

        batch.addService(ServiceName.of("on_demand"), Service.NULL).setInitialMode(ServiceController.Mode.ON_DEMAND);

        batch.install();
        Thread.sleep(50);

        assertState(serviceContainer, ServiceName.of("on_demand"), ServiceController.State.DOWN);

        final BatchBuilder anotherBatch = serviceContainer.batchBuilder();

        anotherBatch.addService(ServiceName.of("automatic"), Service.NULL)
              .setInitialMode(ServiceController.Mode.AUTOMATIC)
              .addDependencies(ServiceName.of("on_demand"));

        anotherBatch.install();
        Thread.sleep(50);

        assertState(serviceContainer, ServiceName.of("on_demand"), ServiceController.State.DOWN);

        final BatchBuilder yetAnotherBatch = serviceContainer.batchBuilder();

        yetAnotherBatch.addService(ServiceName.of("immediate"), Service.NULL)
              .setInitialMode(ServiceController.Mode.IMMEDIATE)
              .addDependencies(ServiceName.of("on_demand"));

        yetAnotherBatch.install();
        Thread.sleep(50);

        assertState(serviceContainer, ServiceName.of("on_demand"), ServiceController.State.UP);
        assertState(serviceContainer, ServiceName.of("automatic"), ServiceController.State.UP);

        serviceContainer.shutdown();
    }

    @Test
    public void testStop() throws Exception {
        final LatchedFinishListener listener = new LatchedFinishListener();
        final ServiceContainer serviceContainer = ServiceContainer.Factory.create();

        final BatchBuilder batch = serviceContainer.batchBuilder();

        batch.addService(ServiceName.of("serviceOne"), Service.NULL)
            .addListener(listener)
            .addDependencies(ServiceName.of("serviceTwo"));
        batch.addService(ServiceName.of("serviceTwo"), Service.NULL).addListener(listener);

        batch.install();
        listener.await();

        assertState(serviceContainer, ServiceName.of("serviceOne"), ServiceController.State.UP);
        assertState(serviceContainer, ServiceName.of("serviceTwo"), ServiceController.State.UP);

        serviceContainer.getService(ServiceName.of("serviceTwo")).setMode(ServiceController.Mode.NEVER);

        Thread.sleep(50);

        assertState(serviceContainer, ServiceName.of("immediate"), ServiceController.State.DOWN);
        assertState(serviceContainer, ServiceName.of("on_demand"), ServiceController.State.DOWN);
        serviceContainer.shutdown();
    }

    private void assertState(final ServiceContainer serviceContainer, final ServiceName serviceName, final ServiceController.State state) {
        Assert.assertEquals(state, serviceContainer.getService(serviceName).getState());
    }
}
