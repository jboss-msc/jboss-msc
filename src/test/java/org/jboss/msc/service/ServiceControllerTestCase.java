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
import static org.junit.Assert.assertNotNull;
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
        batch.addService(ServiceName.of("automatic"), Service.NULL).setInitialMode(ServiceController.Mode.PASSIVE).install();
        batch.addService(ServiceName.of("never"), Service.NULL).setInitialMode(ServiceController.Mode.NEVER).install();
        batch.addService(ServiceName.of("immediate"), Service.NULL).setInitialMode(ServiceController.Mode.ACTIVE).install();
        batch.addService(ServiceName.of("on_demand"), Service.NULL).setInitialMode(ServiceController.Mode.ON_DEMAND).install();

        final Future<ServiceController<?>> automaticServiceFuture = listener.expectServiceStart(ServiceName.of("automatic"));
        final Future<ServiceController<?>> immediateServiceFuture = listener.expectServiceStart(ServiceName.of("immediate"));

        batch.install();

        final ServiceController<?> automaticServiceController = assertController(ServiceName.of("automatic"), automaticServiceFuture);
        final ServiceController<?> immediateServiceController = assertController(ServiceName.of("immediate"), immediateServiceFuture);

        assertEquals(ServiceController.State.UP, automaticServiceController.getState());
        assertEquals(ServiceController.State.UP, immediateServiceController.getState());

        assertState(serviceContainer, ServiceName.of("never"), ServiceController.State.DOWN);
        assertState(serviceContainer, ServiceName.of("on_demand"), ServiceController.State.DOWN);
    }

    @Test
    public void testAutomatic() throws Exception {
        final BatchBuilder batch = serviceContainer.batchBuilder();
        final TestServiceListener listener = new TestServiceListener();
        batch.addListener(listener);

        batch.addService(ServiceName.of("serviceOne"), Service.NULL)
            .setInitialMode(ServiceController.Mode.PASSIVE)
                .addDependencies(ServiceName.of("serviceTwo"))
                    .install();
        batch.addService(ServiceName.of("serviceTwo"), Service.NULL).setInitialMode(ServiceController.Mode.NEVER).install();

        batch.install();

        assertState(serviceContainer, ServiceName.of("serviceOne"), ServiceController.State.DOWN);
        assertState(serviceContainer, ServiceName.of("serviceTwo"), ServiceController.State.DOWN);

        final Future<ServiceController<?>> serviceOneFuture = listener.expectServiceStart(ServiceName.of("serviceTwo"));

        serviceContainer.getService(ServiceName.of("serviceTwo")).setMode(ServiceController.Mode.ACTIVE);

        final ServiceController<?> serviceOneController = assertController(ServiceName.of("serviceTwo"), serviceOneFuture);

        assertEquals(ServiceController.State.UP, serviceOneController.getState());
        assertState(serviceContainer, ServiceName.of("serviceTwo"), ServiceController.State.UP);
    }

    @Test
    public void testOnDemand() throws Exception {
        final BatchBuilder batch = serviceContainer.batchBuilder();
        final TestServiceListener listener = new TestServiceListener();

        batch.addService(ServiceName.of("serviceOne"), Service.NULL).setInitialMode(ServiceController.Mode.ON_DEMAND)
            .install();

        batch.install();

        assertState(serviceContainer, ServiceName.of("serviceOne"), ServiceController.State.DOWN);

        final BatchBuilder anotherBatch = serviceContainer.batchBuilder();

        anotherBatch.addService(ServiceName.of("serviceTwo"), Service.NULL)
            .setInitialMode(ServiceController.Mode.PASSIVE)
            .addDependencies(ServiceName.of("serviceOne"))
            .install();

        anotherBatch.install();

        assertState(serviceContainer, ServiceName.of("serviceTwo"), ServiceController.State.DOWN);

        final BatchBuilder yetAnotherBatch = serviceContainer.batchBuilder();

        yetAnotherBatch.addService(ServiceName.of("serviceThree"), Service.NULL)
            .setInitialMode(ServiceController.Mode.ACTIVE)
            .addDependencies(ServiceName.of("serviceOne"))
            .addListener(listener)
            .install();

        final Future<ServiceController<?>> serviceFuture = listener.expectServiceStart(ServiceName.of("serviceThree"));

        yetAnotherBatch.install();

        final ServiceController<?> serviceController = assertController(ServiceName.of("serviceThree"), serviceFuture);

        assertEquals(ServiceController.State.UP, serviceController.getState());
        assertState(serviceContainer, ServiceName.of("serviceTwo"), ServiceController.State.UP);
        assertState(serviceContainer, ServiceName.of("serviceOne"), ServiceController.State.UP);
    }

    @Test
    public void testAnotherOnDemand() throws Exception {
        final BatchBuilder batch = serviceContainer.batchBuilder();
        final TestServiceListener listener = new TestServiceListener();
        batch.addListener(listener);

        batch.addService(ServiceName.of("sbm"), Service.NULL).setInitialMode(ServiceController.Mode.ON_DEMAND).install();
        batch.addService(ServiceName.of("nic1"), Service.NULL).setInitialMode(ServiceController.Mode.ON_DEMAND).install();

        batch.addService(ServiceName.of("sb1"), Service.NULL)
            .addDependencies(ServiceName.of("sbm"), ServiceName.of("nic1"))
            .setInitialMode(ServiceController.Mode.ON_DEMAND)
            .install();

        batch.addService(ServiceName.of("server"), Service.NULL)
            .setInitialMode(ServiceController.Mode.ON_DEMAND)
            .install();

        batch.addService(ServiceName.of("connector"), Service.NULL)
            .addDependencies(ServiceName.of("sb1"), ServiceName.of("server"))
            .setInitialMode(ServiceController.Mode.ACTIVE)
            .install();

        final Future<ServiceController<?>> connectorFuture = listener.expectServiceStart(ServiceName.of("connector"));

        batch.install();

        final ServiceController<?> connectorController = assertController(ServiceName.of("connector"), connectorFuture);

        assertEquals(ServiceController.State.UP, connectorController.getState());
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
            .addDependencies(ServiceName.of("serviceTwo"))
            .install();
        batch.addService(ServiceName.of("serviceTwo"), Service.NULL).install();

        final Future<ServiceController<?>> serviceStartFuture = listener.expectServiceStart(ServiceName.of("serviceOne"));

        batch.install();

        final ServiceController<?> serviceStartController = assertController(ServiceName.of("serviceOne"), serviceStartFuture);

        assertEquals(ServiceController.State.UP, serviceStartController.getState());
        assertState(serviceContainer, ServiceName.of("serviceTwo"), ServiceController.State.UP);

        final Future<ServiceController<?>> serviceStopFuture = listener.expectServiceStop(ServiceName.of("serviceTwo"));

        serviceContainer.getService(ServiceName.of("serviceTwo")).setMode(ServiceController.Mode.NEVER);

        final ServiceController<?> serviceStopController = assertController(ServiceName.of("serviceTwo"), serviceStopFuture);

        assertEquals(ServiceController.State.DOWN, serviceStopController.getState());
        assertState(serviceContainer, ServiceName.of("serviceOne"), ServiceController.State.DOWN);
    }

    @Test
    public void testRemove() throws Exception {
        final BatchBuilder batch = serviceContainer.batchBuilder();
        final TestServiceListener listener = new TestServiceListener();
        batch.addListener(listener);

        batch.addService(ServiceName.of("serviceOne"), Service.NULL)
            .addDependencies(ServiceName.of("serviceTwo"))
            .install();
        batch.addService(ServiceName.of("serviceTwo"), Service.NULL)
            .install();

        final Future<ServiceController<?>> startFuture = listener.expectServiceStart(ServiceName.of("serviceOne"));

        batch.install();

        final ServiceController<?> startController = assertController(ServiceName.of("serviceOne"), startFuture);

        assertEquals(ServiceController.State.UP, startController.getState());
        assertState(serviceContainer, ServiceName.of("serviceTwo"), ServiceController.State.UP);

        final Future<ServiceController<?>> removeFutureOne = listener.expectNoServiceRemoval(ServiceName.of("serviceOne"));
        final Future<ServiceController<?>> removeFutureTwo = listener.expectServiceRemoval(ServiceName.of("serviceTwo"));

        serviceContainer.getService(ServiceName.of("serviceTwo")).setMode(ServiceController.Mode.REMOVE);

        ServiceController<?> removeController = removeFutureOne.get();
        assertNull(removeController);

        removeController = removeFutureTwo.get();
        assertNotNull(removeController);
        removeController.addListener(listener); // no errors should occur; the operation is ignored

        assertNull(serviceContainer.getService(ServiceName.of("serviceTwo")));
        assertNotNull(serviceContainer.getService(ServiceName.of("serviceOne")));
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
        }).install();

        final Future<StartException> exceptionFuture = listener.expectServiceFailure(ServiceName.of("serviceOne"));

        batch.install();

        assertEquals(startException, exceptionFuture.get());
        assertState(serviceContainer, ServiceName.of("serviceOne"), ServiceController.State.START_FAILED);

    }

    private static void assertState(final ServiceContainer serviceContainer, final ServiceName serviceName, final ServiceController.State state) {
        assertEquals(state, serviceContainer.getService(serviceName).getState());
    }
}
