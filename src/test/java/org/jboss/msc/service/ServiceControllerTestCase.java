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

import org.jboss.msc.util.TestServiceListener;
import org.junit.Test;

import java.util.concurrent.Future;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * Test to verify ServiceController behavior.
 *
 * @author John E. Bailey
 */
public class ServiceControllerTestCase extends AbstractServiceTest {

    @Test
    public void testStartModes() throws Exception {
        final BatchBuilder batch = serviceContainer.batchBuilder();
        final TestServiceListener listener = new TestServiceListener();
        batch.addListener(listener);
        batch.addService(ServiceName.of("automatic"), Service.NULL).setInitialMode(ServiceController.Mode.AUTOMATIC);
        batch.addService(ServiceName.of("never"), Service.NULL).setInitialMode(ServiceController.Mode.NEVER);
        batch.addService(ServiceName.of("immediate"), Service.NULL).setInitialMode(ServiceController.Mode.IMMEDIATE);
        batch.addService(ServiceName.of("on_demand"), Service.NULL).setInitialMode(ServiceController.Mode.ON_DEMAND);

        final Future<ServiceController<?>> automaticServiceFuture = listener.expectServiceStart(ServiceName.of("automatic"));
        final Future<ServiceController<?>> immediateServiceFuture = listener.expectServiceStart(ServiceName.of("immediate"));

        batch.install();

        assertEquals(ServiceController.State.UP, automaticServiceFuture.get().getState());
        assertEquals(ServiceController.State.UP, immediateServiceFuture.get().getState());

        assertState(serviceContainer, ServiceName.of("never"), ServiceController.State.DOWN);
        assertState(serviceContainer, ServiceName.of("on_demand"), ServiceController.State.DOWN);
    }

    @Test
    public void testAutomatic() throws Exception {
        final BatchBuilder batch = serviceContainer.batchBuilder();
        final TestServiceListener listener = new TestServiceListener();
        batch.addListener(listener);

        batch.addService(ServiceName.of("serviceOne"), Service.NULL)
            .setInitialMode(ServiceController.Mode.AUTOMATIC)
                .addDependencies(ServiceName.of("serviceTwo"));
        batch.addService(ServiceName.of("serviceTwo"), Service.NULL).setInitialMode(ServiceController.Mode.NEVER);

        batch.install();

        assertState(serviceContainer, ServiceName.of("serviceOne"), ServiceController.State.DOWN);
        assertState(serviceContainer, ServiceName.of("serviceTwo"), ServiceController.State.DOWN);

        final Future<ServiceController<?>> serviceOneFuture = listener.expectServiceStart(ServiceName.of("serviceTwo"));

        serviceContainer.getService(ServiceName.of("serviceTwo")).setMode(ServiceController.Mode.IMMEDIATE);

        assertEquals(ServiceController.State.UP, serviceOneFuture.get().getState());
        assertState(serviceContainer, ServiceName.of("serviceTwo"), ServiceController.State.UP);
    }

    @Test
    public void testOnDemand() throws Exception {
        final BatchBuilder batch = serviceContainer.batchBuilder();
        final TestServiceListener listener = new TestServiceListener();

        batch.addService(ServiceName.of("serviceOne"), Service.NULL).setInitialMode(ServiceController.Mode.ON_DEMAND);

        batch.install();

        assertState(serviceContainer, ServiceName.of("serviceOne"), ServiceController.State.DOWN);

        final BatchBuilder anotherBatch = serviceContainer.batchBuilder();

        anotherBatch.addService(ServiceName.of("serviceTwo"), Service.NULL)
            .setInitialMode(ServiceController.Mode.AUTOMATIC)
            .addDependencies(ServiceName.of("serviceOne"));

        anotherBatch.install();

        assertState(serviceContainer, ServiceName.of("serviceTwo"), ServiceController.State.DOWN);

        final BatchBuilder yetAnotherBatch = serviceContainer.batchBuilder();

        yetAnotherBatch.addService(ServiceName.of("serviceThree"), Service.NULL)
            .setInitialMode(ServiceController.Mode.IMMEDIATE)
            .addDependencies(ServiceName.of("serviceOne"))
            .addListener(listener);

        final Future<ServiceController<?>> serviceFuture = listener.expectServiceStart(ServiceName.of("serviceThree"));

        yetAnotherBatch.install();

        assertEquals(ServiceController.State.UP, serviceFuture.get().getState());
        assertState(serviceContainer, ServiceName.of("serviceTwo"), ServiceController.State.UP);
        assertState(serviceContainer, ServiceName.of("serviceOne"), ServiceController.State.UP);
    }

    @Test
    public void testAnotherOnDemand() throws Exception {
        final BatchBuilder batch = serviceContainer.batchBuilder();
        final TestServiceListener listener = new TestServiceListener();
        batch.addListener(listener);

        batch.addService(ServiceName.of("sbm"), Service.NULL).setInitialMode(ServiceController.Mode.ON_DEMAND);
        batch.addService(ServiceName.of("nic1"), Service.NULL).setInitialMode(ServiceController.Mode.ON_DEMAND);

        batch.addService(ServiceName.of("sb1"), Service.NULL)
            .addDependencies(ServiceName.of("sbm"), ServiceName.of("nic1"))
            .setInitialMode(ServiceController.Mode.ON_DEMAND);

        batch.addService(ServiceName.of("server"), Service.NULL)
            .setInitialMode(ServiceController.Mode.ON_DEMAND);

        batch.addService(ServiceName.of("connector"), Service.NULL)
            .addDependencies(ServiceName.of("sb1"), ServiceName.of("server"))
            .setInitialMode(ServiceController.Mode.IMMEDIATE);

        final Future<ServiceController<?>> connectorFuture = listener.expectServiceStart(ServiceName.of("connector"));

        batch.install();

        assertEquals(ServiceController.State.UP, connectorFuture.get().getState());
        assertState(serviceContainer, ServiceName.of("sbm"), ServiceController.State.UP);
        assertState(serviceContainer, ServiceName.of("nic1"), ServiceController.State.UP);
        assertState(serviceContainer, ServiceName.of("sb1"), ServiceController.State.UP);
        assertState(serviceContainer, ServiceName.of("server"), ServiceController.State.UP);
    }

    @Test
    public void testStop() throws Exception {
        final BatchBuilder batch = serviceContainer.batchBuilder();
        final TestServiceListener listener = new TestServiceListener();
        batch.addListener(listener);
        
        batch.addService(ServiceName.of("serviceOne"), Service.NULL)
            .addDependencies(ServiceName.of("serviceTwo"));
        batch.addService(ServiceName.of("serviceTwo"), Service.NULL);

        final Future<ServiceController<?>> serviceStartFuture = listener.expectServiceStart(ServiceName.of("serviceOne"));

        batch.install();

        assertEquals(ServiceController.State.UP, serviceStartFuture.get().getState());
        assertState(serviceContainer, ServiceName.of("serviceTwo"), ServiceController.State.UP);

        final Future<ServiceController<?>> serviceStopFuture = listener.expectServiceStop(ServiceName.of("serviceTwo"));

        serviceContainer.getService(ServiceName.of("serviceTwo")).setMode(ServiceController.Mode.NEVER);

        assertEquals(ServiceController.State.DOWN, serviceStopFuture.get().getState());
        assertState(serviceContainer, ServiceName.of("serviceOne"), ServiceController.State.DOWN);
    }

    @Test
    public void testRemove() throws Exception {
        final BatchBuilder batch = serviceContainer.batchBuilder();
        final TestServiceListener listener = new TestServiceListener();
        batch.addListener(listener);

        batch.addService(ServiceName.of("serviceOne"), Service.NULL)
            .addDependencies(ServiceName.of("serviceTwo"));
        batch.addService(ServiceName.of("serviceTwo"), Service.NULL);

        final Future<ServiceController<?>> startFuture = listener.expectServiceStart(ServiceName.of("serviceOne"));

        batch.install();

        assertEquals(ServiceController.State.UP, startFuture.get().getState());
        assertState(serviceContainer, ServiceName.of("serviceTwo"), ServiceController.State.UP);

        final Future<ServiceController<?>> removeFuture = listener.expectServiceRemoval(ServiceName.of("serviceOne"));

        serviceContainer.getService(ServiceName.of("serviceTwo")).setMode(ServiceController.Mode.REMOVE);

        assertEquals(ServiceController.State.REMOVED, removeFuture.get().getState());

        assertNull(serviceContainer.getService(ServiceName.of("serviceTwo")));
        assertNull(serviceContainer.getService(ServiceName.of("serviceOne")));
    }

    @Test
    public void testFailedStart() throws Exception {
        final StartException startException = new StartException("Blahhhh");
        final BatchBuilder batch = serviceContainer.batchBuilder();
        final TestServiceListener listener = new TestServiceListener();
        batch.addListener(listener);

        batch.addService(ServiceName.of("serviceOne"), new Service<Void>() {
            @Override
            public void start(StartContext context) throws StartException {
                throw startException;
            }

            @Override
            public void stop(StopContext context) {
            }

            @Override
            public Void getValue() throws IllegalStateException {
                return null;
            }
        });

        final Future<StartException> exceptionFuture = listener.expectServiceFailure(ServiceName.of("serviceOne"));

        batch.install();

        assertEquals(startException, exceptionFuture.get());
        assertState(serviceContainer, ServiceName.of("serviceOne"), ServiceController.State.START_FAILED);

    }

    private static void assertState(final ServiceContainer serviceContainer, final ServiceName serviceName, final ServiceController.State state) {
        assertEquals(state, serviceContainer.getService(serviceName).getState());
    }
}
