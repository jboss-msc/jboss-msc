/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2021, Red Hat, Inc., and individual contributors
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

package org.jboss.msc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

import java.util.function.Consumer;

import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceContainer;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceController.Mode;
import org.jboss.msc.service.ServiceController.State;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.junit.Test;

/**
 * Test to verify ServiceController behavior.
 *
 * @author John E. Bailey
 */
public class ServiceControllerTestCase extends AbstractServiceTest {
    @Test
    public void testStartModes() throws Exception {
        ServiceBuilder<?> sb = serviceContainer.addService();
        Consumer<String> providedValue = sb.provides(ServiceName.of("automatic"));
        sb.setInstance(Service.newInstance(providedValue, "automatic"));
        sb.setInitialMode(Mode.PASSIVE);
        final ServiceController<?> automaticServiceController = sb.install();

        sb = serviceContainer.addService();
        providedValue = sb.provides(ServiceName.of("never"));
        sb.setInstance(Service.newInstance(providedValue, "never"));
        sb.setInitialMode(Mode.NEVER);
        sb.install();

        sb = serviceContainer.addService();
        providedValue = sb.provides(ServiceName.of("immediate"));
        sb.setInstance(Service.newInstance(providedValue, "immediate"));
        sb.setInitialMode(Mode.ACTIVE);
        final ServiceController<?> immediateServiceController = sb.install();

        sb = serviceContainer.addService();
        providedValue = sb.provides(ServiceName.of("on_demand"));
        sb.setInstance(Service.newInstance(providedValue, "on_demand"));
        sb.setInitialMode(Mode.ON_DEMAND);
        sb.install();

        serviceContainer.awaitStability();

        assertEquals(ServiceController.State.UP, automaticServiceController.getState());
        assertEquals(ServiceController.State.UP, immediateServiceController.getState());

        assertState(serviceContainer, ServiceName.of("never"), ServiceController.State.DOWN);
        assertState(serviceContainer, ServiceName.of("on_demand"), ServiceController.State.DOWN);
    }

    @Test
    public void testAutomatic() throws Exception {
        ServiceBuilder<?> sb = serviceContainer.addService();
        Consumer<String> providedValue = sb.provides(ServiceName.of("serviceOne"));
        sb.setInstance(Service.newInstance(providedValue, "serivceOne"));
        sb.setInitialMode(ServiceController.Mode.PASSIVE);
        sb.requires(ServiceName.of("serviceTwo"));
        sb.install();

        sb = serviceContainer.addService();
        providedValue = sb.provides(ServiceName.of("serviceTwo"));
        sb.setInstance(Service.newInstance(providedValue, "serviceTwo"));
        sb.setInitialMode(ServiceController.Mode.NEVER);
        sb.install();

        serviceContainer.awaitStability();

        assertState(serviceContainer, ServiceName.of("serviceOne"), ServiceController.State.DOWN);
        assertState(serviceContainer, ServiceName.of("serviceTwo"), ServiceController.State.DOWN);

        serviceContainer.getService(ServiceName.of("serviceTwo")).setMode(ServiceController.Mode.ACTIVE);

        serviceContainer.awaitStability();

        assertState(serviceContainer, ServiceName.of("serviceTwo"), ServiceController.State.UP);
    }

    @Test
    public void testOnDemand() throws Exception {
        ServiceBuilder<?> sb = serviceContainer.addService();
        Consumer<String> providedValue = sb.provides(ServiceName.of("serviceOne"));
        sb.setInstance(Service.newInstance(providedValue, "ServiceOne"));
        sb.setInitialMode(ServiceController.Mode.ON_DEMAND);
        sb.install();

        serviceContainer.awaitStability();

        assertState(serviceContainer, ServiceName.of("serviceOne"), ServiceController.State.DOWN);

        sb = serviceContainer.addService();
        providedValue = sb.provides(ServiceName.of("serviceTwo"));
        sb.setInstance(Service.newInstance(providedValue, "serviceTwo"));
        sb.setInitialMode(ServiceController.Mode.PASSIVE);
        sb.requires(ServiceName.of("serviceOne"));
        final ServiceController<?> serviceTwoController = sb.install();

        serviceContainer.awaitStability();

        assertState(serviceContainer, ServiceName.of("serviceTwo"), ServiceController.State.DOWN);

        sb = serviceContainer.addService();
        providedValue = sb.provides(ServiceName.of("serviceThree"));
        sb.setInstance(Service.newInstance(providedValue, "serviceThree"));
        sb.setInitialMode(ServiceController.Mode.ACTIVE);
        sb.requires(ServiceName.of("serviceOne"));
        final ServiceController<?> serviceController = sb.install();

        serviceContainer.awaitStability();

        assertEquals(ServiceController.State.UP, serviceController.getState());
        assertState(serviceContainer, ServiceName.of("serviceTwo"), ServiceController.State.UP);
        assertState(serviceContainer, ServiceName.of("serviceOne"), ServiceController.State.UP);
    }

    @Test
    public void testAnotherOnDemand() throws Exception {
        ServiceBuilder<?> sb = serviceContainer.addService();
        Consumer<String> providedValue = sb.provides(ServiceName.of("sbm"));
        sb.setInstance(Service.newInstance(providedValue, "sbm"));
        sb.setInitialMode(ServiceController.Mode.ON_DEMAND);
        sb.install();

        sb = serviceContainer.addService();
        providedValue = sb.provides(ServiceName.of("nic1"));
        sb.setInstance(Service.newInstance(providedValue, "nic1"));
        sb.setInitialMode(ServiceController.Mode.ON_DEMAND);
        sb.install();

        sb = serviceContainer.addService();
        providedValue = sb.provides(ServiceName.of("sb1"));
        sb.setInstance(Service.newInstance(providedValue, "sb1"));
        sb.requires(ServiceName.of("sbm"));
        sb.requires(ServiceName.of("nic1"));
        sb.setInitialMode(ServiceController.Mode.ON_DEMAND);
        sb.install();

        sb = serviceContainer.addService();
        providedValue = sb.provides(ServiceName.of("server"));
        sb.setInstance(Service.newInstance(providedValue, "server"));
        sb.setInitialMode(ServiceController.Mode.ON_DEMAND);
        sb.install();

        sb = serviceContainer.addService();
        providedValue = sb.provides(ServiceName.of("connector"));
        sb.setInstance(Service.newInstance(providedValue, "connector"));
        sb.requires(ServiceName.of("sb1"));
        sb.requires(ServiceName.of("server"));
        sb.setInitialMode(ServiceController.Mode.ACTIVE);
        sb.install();

        serviceContainer.awaitStability();

        assertState(serviceContainer, ServiceName.of("connector"), ServiceController.State.UP);
        assertState(serviceContainer, ServiceName.of("sbm"), ServiceController.State.UP);
        assertState(serviceContainer, ServiceName.of("nic1"), ServiceController.State.UP);
        assertState(serviceContainer, ServiceName.of("sb1"), ServiceController.State.UP);
        assertState(serviceContainer, ServiceName.of("server"), ServiceController.State.UP);
    }

    @Test
    public void testStop() throws Exception {
        ServiceBuilder<?> sb = serviceContainer.addService();
        Consumer<String> providedValue = sb.provides(ServiceName.of("serviceOne"));
        sb.setInstance(Service.newInstance(providedValue, "serviceOne"));
        sb.requires(ServiceName.of("serviceTwo"));
        sb.install();

        sb = serviceContainer.addService();
        providedValue = sb.provides(ServiceName.of("serviceTwo"));
        sb.setInstance(Service.newInstance(providedValue, "serviceTwo"));
        ServiceController<?> serviceTwoController = sb.install();


        serviceContainer.awaitStability();

        assertState(serviceContainer, ServiceName.of("serviceOne"), ServiceController.State.UP);
        assertState(serviceContainer, ServiceName.of("serviceTwo"), ServiceController.State.UP);

        serviceTwoController.setMode(ServiceController.Mode.NEVER);
        serviceContainer.awaitStability();

        assertEquals(ServiceController.State.DOWN, serviceTwoController.getState());
        assertState(serviceContainer, ServiceName.of("serviceOne"), ServiceController.State.DOWN);
    }

    @Test
    public void testFailedStart() throws Exception {
        final StartException startException = new StartException("Blahhhh");
        final ServiceBuilder<?> sb = serviceContainer.addService();
        sb.provides(ServiceName.of("serviceOne"));
        sb.setInstance(new Service() {
            @Override
            public void start(StartContext context) throws StartException {
                throw startException;
            }

            @Override
            public void stop(StopContext context) {
            }

        }).install();
        serviceContainer.awaitStability();
        assertState(serviceContainer, ServiceName.of("serviceOne"), ServiceController.State.START_FAILED);

    }
    @Test
    public void testRetryFailure() throws Exception {
        final ServiceBuilder<?> sb = serviceContainer.addService();
        Consumer<String> providedValue = sb.provides(ServiceName.of("service", "one"));
        sb.setInstance(new FailToStartService(providedValue, true));
        final ServiceController<?> serviceController = sb.install();

        serviceContainer.awaitStability();

        assertState(serviceContainer, ServiceName.of("service", "one"), ServiceController.State.START_FAILED);

        serviceController.retry();
        serviceContainer.awaitStability();

        assertState(serviceContainer, ServiceName.of("service", "one"), ServiceController.State.UP);
    }

    @Test
    public void testRetryNoFailure() throws Exception {
        ServiceBuilder<?> sb = serviceContainer.addService();
        Consumer<String> providedValue = sb.provides(ServiceName.of("serviceOne"));
        sb.setInstance(Service.newInstance(providedValue, "serviceOne"));
        final ServiceController<?> serviceController = sb.install();

        serviceContainer.awaitStability();
        assertSame(State.UP, serviceController.getState());

        // retry request should be ignored should be ignored
        serviceController.retry();
        serviceContainer.awaitStability();
        assertSame(State.UP, serviceController.getState());

        serviceController.setMode(Mode.NEVER);

        serviceContainer.awaitStability();

        assertSame(State.DOWN, serviceController.getState());

        // again, retry request should be ignored
        serviceController.retry();
        serviceContainer.awaitStability();
        assertSame(State.DOWN, serviceController.getState());
    }

    private static void assertState(final ServiceContainer serviceContainer, final ServiceName serviceName, final ServiceController.State state) {
        assertEquals(state, serviceContainer.getService(serviceName).getState());
    }
}
