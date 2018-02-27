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

import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

import java.util.concurrent.Future;

import org.jboss.msc.service.ServiceController.Mode;
import org.jboss.msc.service.ServiceController.State;
import org.jboss.msc.service.util.FailToStartService;
import org.jboss.msc.util.TestServiceListener;
import org.junit.Test;

/**
 * Test to verify ServiceController behavior.
 *
 * @author John E. Bailey
 */
public class ServiceControllerTestCase extends AbstractServiceTest {
    @Test
    public void testStartModes() throws Exception {
        final TestServiceListener listener = new TestServiceListener();
        serviceContainer.addListener(listener);

        final Future<ServiceController<?>> automaticServiceFuture = listener.expectServiceStart(ServiceName.of("automatic"));
        final Future<ServiceController<?>> immediateServiceFuture = listener.expectServiceStart(ServiceName.of("immediate"));
        final ServiceController<?> automaticServiceController = serviceContainer.addService(ServiceName.of("automatic"), Service.NULL).setInitialMode(ServiceController.Mode.PASSIVE).install();
        serviceContainer.addService(ServiceName.of("never"), Service.NULL).setInitialMode(ServiceController.Mode.NEVER).install();
        final ServiceController<?> immediateServiceController = serviceContainer.addService(ServiceName.of("immediate"), Service.NULL).setInitialMode(ServiceController.Mode.ACTIVE).install();
        serviceContainer.addService(ServiceName.of("on_demand"), Service.NULL).setInitialMode(ServiceController.Mode.ON_DEMAND).install();

        assertController(ServiceName.of("automatic"), automaticServiceController);
        assertController(automaticServiceController, automaticServiceFuture);
        assertController(ServiceName.of("immediate"), immediateServiceController);
        assertController(immediateServiceController, immediateServiceFuture);

        assertEquals(ServiceController.State.UP, automaticServiceController.getState());
        assertEquals(ServiceController.State.UP, immediateServiceController.getState());

        assertState(serviceContainer, ServiceName.of("never"), ServiceController.State.DOWN);
        assertState(serviceContainer, ServiceName.of("on_demand"), ServiceController.State.DOWN);
    }

    @Test
    public void testAutomatic() throws Exception {
        final TestServiceListener listener = new TestServiceListener();
        serviceContainer.addListener(listener);

        serviceContainer.addService(ServiceName.of("serviceOne"), Service.NULL)
            .setInitialMode(ServiceController.Mode.PASSIVE)
                .addDependencies(ServiceName.of("serviceTwo"))
                    .install();
        serviceContainer.addService(ServiceName.of("serviceTwo"), Service.NULL).setInitialMode(ServiceController.Mode.NEVER).install();

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
        final TestServiceListener listener = new TestServiceListener();

        serviceContainer.addService(ServiceName.of("serviceOne"), Service.NULL).setInitialMode(ServiceController.Mode.ON_DEMAND)
            .install();

        assertState(serviceContainer, ServiceName.of("serviceOne"), ServiceController.State.DOWN);

        final ServiceController<?> serviceTwoController = serviceContainer.addService(ServiceName.of("serviceTwo"), Service.NULL)
            .setInitialMode(ServiceController.Mode.PASSIVE)
            .addDependencies(ServiceName.of("serviceOne"))
            .addListener(listener)
            .install();

        assertState(serviceContainer, ServiceName.of("serviceTwo"), ServiceController.State.DOWN);

        final Future<ServiceController<?>> serviceFuture = listener.expectServiceStart(ServiceName.of("serviceThree"));
        final Future<ServiceController<?>> serviceTwoStart = listener.expectServiceStart(ServiceName.of("serviceTwo"));
        final ServiceController<?> serviceController = serviceContainer.addService(ServiceName.of("serviceThree"), Service.NULL)
            .setInitialMode(ServiceController.Mode.ACTIVE)
            .addDependencies(ServiceName.of("serviceOne"))
            .addListener(listener)
            .install();

        assertController(ServiceName.of("serviceThree"), serviceController);
        assertController(serviceController, serviceFuture);
        assertController(serviceTwoController, serviceTwoStart);

        assertEquals(ServiceController.State.UP, serviceController.getState());
        assertState(serviceContainer, ServiceName.of("serviceTwo"), ServiceController.State.UP);
        assertState(serviceContainer, ServiceName.of("serviceOne"), ServiceController.State.UP);
    }

    @Test
    public void testAnotherOnDemand() throws Exception {
        final TestServiceListener listener = new TestServiceListener();
        serviceContainer.addListener(listener);

        serviceContainer.addService(ServiceName.of("sbm"), Service.NULL).setInitialMode(ServiceController.Mode.ON_DEMAND).install();
        serviceContainer.addService(ServiceName.of("nic1"), Service.NULL).setInitialMode(ServiceController.Mode.ON_DEMAND).install();

        serviceContainer.addService(ServiceName.of("sb1"), Service.NULL)
            .addDependencies(ServiceName.of("sbm"), ServiceName.of("nic1"))
            .setInitialMode(ServiceController.Mode.ON_DEMAND)
            .install();

        serviceContainer.addService(ServiceName.of("server"), Service.NULL)
            .setInitialMode(ServiceController.Mode.ON_DEMAND)
            .install();

        final Future<ServiceController<?>> connectorFuture = listener.expectServiceStart(ServiceName.of("connector"));

        serviceContainer.addService(ServiceName.of("connector"), Service.NULL)
            .addDependencies(ServiceName.of("sb1"), ServiceName.of("server"))
            .setInitialMode(ServiceController.Mode.ACTIVE)
            .install();

        final ServiceController<?> connectorController = assertController(ServiceName.of("connector"), connectorFuture);

        assertEquals(ServiceController.State.UP, connectorController.getState());
        assertState(serviceContainer, ServiceName.of("sbm"), ServiceController.State.UP);
        assertState(serviceContainer, ServiceName.of("nic1"), ServiceController.State.UP);
        assertState(serviceContainer, ServiceName.of("sb1"), ServiceController.State.UP);
        assertState(serviceContainer, ServiceName.of("server"), ServiceController.State.UP);
    }

    @Test
    public void testStop() throws Exception {
        final TestServiceListener listener = new TestServiceListener();
        serviceContainer.addListener(listener);

        final Future<ServiceController<?>> serviceStartFuture = listener.expectServiceStart(ServiceName.of("serviceOne"));

        final ServiceController<?> serviceOneController = serviceContainer.addService(ServiceName.of("serviceOne"), Service.NULL)
            .addDependencies(ServiceName.of("serviceTwo"))
            .install();
        final ServiceController<?> serviceTwoController = serviceContainer.addService(ServiceName.of("serviceTwo"), Service.NULL)
            .install();

        assertController(ServiceName.of("serviceOne"), serviceOneController);
        assertController(serviceOneController, serviceStartFuture);

        assertEquals(ServiceController.State.UP, serviceOneController.getState());
        assertController(ServiceName.of("serviceTwo"), serviceTwoController);
        assertState(serviceContainer, ServiceName.of("serviceTwo"), ServiceController.State.UP);

        final Future<ServiceController<?>> serviceStopFuture = listener.expectServiceStop(ServiceName.of("serviceTwo"));

        serviceTwoController.setMode(ServiceController.Mode.NEVER);

        assertController(serviceTwoController, serviceStopFuture);

        assertEquals(ServiceController.State.DOWN, serviceTwoController.getState());
        assertState(serviceContainer, ServiceName.of("serviceOne"), ServiceController.State.DOWN);
    }

    @Test
    public void testFailedStart() throws Exception {
        final StartException startException = new StartException("Blahhhh");
        final TestServiceListener listener = new TestServiceListener();
        serviceContainer.addListener(listener);

        final Future<StartException> exceptionFuture = listener.expectServiceFailure(ServiceName.of("serviceOne"));
        serviceContainer.addService(ServiceName.of("serviceOne"), new Service<Void>() {
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

        assertEquals(startException, exceptionFuture.get());
        assertState(serviceContainer, ServiceName.of("serviceOne"), ServiceController.State.START_FAILED);

    }

    @Test
    public void testRetryFailure() throws Exception {
        final TestServiceListener listener = new TestServiceListener();
        final Future<StartException> exceptionFuture = listener.expectServiceFailure(ServiceName.of("service", "one"));
        final ServiceController<?> serviceController = serviceContainer.addService(ServiceName.of("service", "one"),
                new FailToStartService(true)).addListener(listener).install();
        assertController(ServiceName.of("service", "one"), serviceController);
        assertFailure(serviceController, exceptionFuture);
        final ServiceControllerImpl<?> serviceInstance = (ServiceControllerImpl<?>) serviceController;
        
        final Future<ServiceController<?>> serviceStartFuture = listener.expectServiceStart(ServiceName.of("service", "one"));
        serviceInstance.retry();
        assertController(serviceController, serviceStartFuture);
    }

    @Test
    public void testRetryNoFailure() throws Exception {
        final TestServiceListener listener = new TestServiceListener();
        final Future<ServiceController<?>> serviceStartFuture = listener.expectServiceStart(ServiceName.of("service", "one"));
        final ServiceBuilder<?> serviceBuilder = serviceContainer.addService(ServiceName.of("service", "one"), Service.NULL)
            .addListener(listener);
        final ServiceController<?> serviceController = assertController(ServiceName.of("service", "one"), serviceBuilder.install());
        assertController(serviceController, serviceStartFuture);
        ServiceControllerImpl<?> serviceInstance = (ServiceControllerImpl<?>) serviceController;
        assertSame(State.UP, serviceController.getState());

        // retry request should be ignored should be ignored
        serviceInstance.retry();
        assertSame(State.UP, serviceController.getState());

        final Future<ServiceController<?>> serviceStopFuture = listener.expectServiceStop(ServiceName.of("service", "one"));
        serviceController.setMode(Mode.NEVER);
        assertController(serviceController, serviceStopFuture);
        assertSame(State.DOWN, serviceController.getState());

        // again, retry request should be ignored
        serviceInstance.retry();
        assertSame(State.DOWN, serviceController.getState());
    }

    private static void assertState(final ServiceContainer serviceContainer, final ServiceName serviceName, final ServiceController.State state) {
        assertEquals(state, serviceContainer.getService(serviceName).getState());
    }
}
