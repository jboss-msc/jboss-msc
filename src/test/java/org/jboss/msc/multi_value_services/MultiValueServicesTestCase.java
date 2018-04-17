/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2018, Red Hat, Inc., and individual contributors
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

package org.jboss.msc.multi_value_services;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.AbstractServiceListener;
import org.jboss.msc.service.AbstractServiceTest;
import org.jboss.msc.service.LifecycleEvent;
import org.jboss.msc.service.LifecycleListener;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StabilityMonitor;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.InjectedValue;

import java.util.function.Consumer;
import java.util.function.Supplier;

import org.junit.Test;

/**
 * <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
public class MultiValueServicesTestCase extends AbstractServiceTest {

    private static final ServiceName HTTP_CONFIG = ServiceName.of("http", "config");
    private static final ServiceName HTTP_HOST = ServiceName.of("http", "host");
    private static final ServiceName HTTP_PORT = ServiceName.of("http", "port");
    private static final ServiceName HTTP_SERVER = ServiceName.of("http", "server");
    private static final ServiceName DUMMY_SERVICE = ServiceName.of("dummy", "service");

    @Test
    public void usingJustNewAPI() throws Exception {
        StabilityMonitor monitor = new StabilityMonitor();
        ServiceBuilder<HttpConfigService> sb1 = serviceContainer.addService(HTTP_CONFIG);
        Consumer<String> hostInjector = sb1.provides(HTTP_HOST);
        Consumer<Integer> portInjector = sb1.provides(HTTP_PORT);
        sb1.setInstance(new HttpConfigService(hostInjector, portInjector));
        sb1.addMonitor(monitor);
        ServiceController configController = sb1.install();
        assertNotNull(configController);
        assertEquals(configController.getName(), HTTP_CONFIG);

        ServiceBuilder<HttpServer> sb2 = serviceContainer.addService(HTTP_SERVER);
        Supplier<String> hostValue = sb2.requires(HTTP_HOST);
        Supplier<Integer> portValue = sb2.requires(HTTP_PORT);
        HttpServer server = new HttpServer(hostValue, portValue);
        sb2.setInstance(server);
        sb2.addMonitor(monitor);
        ServiceController serverController = sb2.install();
        assertNotNull(serverController);
        assertEquals(serverController.getName(), HTTP_SERVER);

        monitor.awaitStability();
        assertEquals(server.httpHost.get(), "localhost");
        assertEquals(server.httpPort.get(), new Integer(80));
        assertEquals(configController.getState(), ServiceController.State.UP);
        assertEquals(serverController.getState(), ServiceController.State.UP);
        assertEquals(server.startMsg, "start(httpHost == localhost, httpPort == 80");

        configController.setMode(ServiceController.Mode.REMOVE);
        monitor.awaitStability();
        assertEquals(configController.getState(), ServiceController.State.REMOVED);
        assertEquals(serverController.getState(), ServiceController.State.DOWN);
        assertEquals(server.stopMsg, "stop(httpHost == localhost, httpPort == 80");
    }

    @Test
    public void mixingOldAndNewAPI() throws Exception {
        StabilityMonitor monitor = new StabilityMonitor();
        ServiceBuilder<String> sb1 = serviceContainer.addService(HTTP_HOST, new LegacyHttpHostService());
        sb1.addMonitor(monitor);
        ServiceController legacyHostServiceController = sb1.install();
        assertNotNull(legacyHostServiceController);
        assertEquals(legacyHostServiceController.getName(), HTTP_HOST);

        ServiceBuilder<Integer> sb2 = serviceContainer.addService(HTTP_PORT, new LegacyHttpPortService());
        sb2.addMonitor(monitor);
        ServiceController legacyPortServiceController = sb2.install();
        assertNotNull(legacyPortServiceController);
        assertEquals(legacyPortServiceController.getName(), HTTP_PORT);

        ServiceBuilder<HttpServer> sb3 = serviceContainer.addService(HTTP_SERVER);
        Supplier<String> hostValue = sb3.requires(HTTP_HOST);
        Supplier<Integer> portValue = sb3.requires(HTTP_PORT);
        HttpServer server = new HttpServer(hostValue, portValue);
        sb3.setInstance(server);
        sb3.addMonitor(monitor);
        ServiceController serverController = sb3.install();
        assertNotNull(serverController);
        assertEquals(serverController.getName(), HTTP_SERVER);

        monitor.awaitStability();
        assertEquals(server.httpHost.get(), "localhost");
        assertEquals(server.httpPort.get(), new Integer(80));
        assertEquals(legacyHostServiceController.getState(), ServiceController.State.UP);
        assertEquals(legacyPortServiceController.getState(), ServiceController.State.UP);
        assertEquals(serverController.getState(), ServiceController.State.UP);
        assertEquals(server.startMsg, "start(httpHost == localhost, httpPort == 80");

        legacyHostServiceController.setMode(ServiceController.Mode.REMOVE);
        legacyPortServiceController.setMode(ServiceController.Mode.REMOVE);
        monitor.awaitStability();
        assertEquals(legacyHostServiceController.getState(), ServiceController.State.REMOVED);
        assertEquals(legacyPortServiceController.getState(), ServiceController.State.REMOVED);
        assertEquals(serverController.getState(), ServiceController.State.DOWN);
        assertEquals(server.stopMsg, "stop(httpHost == localhost, httpPort == 80");
    }

    @Test
    public void mixingNewAndOldAPI() throws Exception {
        StabilityMonitor monitor = new StabilityMonitor();
        ServiceBuilder<HttpConfigService> sb1 = serviceContainer.addService(HTTP_CONFIG);
        Consumer<String> hostInjector = sb1.provides(HTTP_HOST);
        Consumer<Integer> portInjector = sb1.provides(HTTP_PORT);
        sb1.setInstance(new HttpConfigService(hostInjector, portInjector));
        sb1.addMonitor(monitor);
        ServiceController configController = sb1.install();
        assertNotNull(configController);
        assertEquals(configController.getName(), HTTP_CONFIG);

        LegacyHttpServer legacyServer = new LegacyHttpServer();
        ServiceBuilder<HttpServer> sb2 = serviceContainer.addService(HTTP_SERVER, legacyServer);
        sb2.addDependency(HTTP_HOST, String.class, legacyServer.getHostInjector());
        sb2.addDependency(HTTP_PORT, Integer.class, legacyServer.getPortInjector());
        sb2.addMonitor(monitor);
        ServiceController serverController = sb2.install();
        assertNotNull(serverController);
        assertEquals(serverController.getName(), HTTP_SERVER);

        monitor.awaitStability();
        assertEquals(legacyServer.httpHost.getValue(), "localhost");
        assertEquals(legacyServer.httpPort.getValue(), new Integer(80));
        assertEquals(configController.getState(), ServiceController.State.UP);
        assertEquals(serverController.getState(), ServiceController.State.UP);
        assertEquals(legacyServer.startMsg, "start(httpHost == localhost, httpPort == 80");

        configController.setMode(ServiceController.Mode.REMOVE);
        monitor.awaitStability();
        assertEquals(configController.getState(), ServiceController.State.REMOVED);
        assertEquals(serverController.getState(), ServiceController.State.DOWN);
        assertEquals(legacyServer.stopMsg, "stop(httpHost == localhost, httpPort == 80");
    }

    @Test
    public void usingJustNewAPIButServiceTargetUsesLegacyStuff() throws Exception {
        serviceTargetUsingLegacyAPIScenario(serviceContainer.subTarget());
    }

    @Test
    public void usingJustNewAPIButServiceContainerUsesLegacyStuff() throws Exception {
        serviceTargetUsingLegacyAPIScenario(serviceContainer);
    }

    private void serviceTargetUsingLegacyAPIScenario(final ServiceTarget target) throws Exception {
        StabilityMonitor monitor = new StabilityMonitor();
        ServiceBuilder<HttpConfigService> sb = serviceContainer.addService(DUMMY_SERVICE);
        sb.addMonitor(monitor);
        sb.setInitialMode(ServiceController.Mode.ON_DEMAND);
        ServiceController dummyServiceController = sb.install();
        assertNotNull(dummyServiceController);
        serviceContainer.awaitStability();
        assertEquals(dummyServiceController.getState(), ServiceController.State.DOWN);
        assertEquals(dummyServiceController.getName(), DUMMY_SERVICE);

        LoggingServiceListener serviceListener = new LoggingServiceListener();
        LoggingLifecycleListener lifecycleListener = new LoggingLifecycleListener();
        target.addDependency(DUMMY_SERVICE);
        target.addMonitor(monitor);
        target.addListener(serviceListener);
        target.addListener(lifecycleListener);

        ServiceBuilder<HttpConfigService> sb1 = target.addService(HTTP_CONFIG);
        Consumer<String> hostInjector = sb1.provides(HTTP_HOST);
        Consumer<Integer> portInjector = sb1.provides(HTTP_PORT);
        sb1.setInstance(new HttpConfigService(hostInjector, portInjector));
        ServiceController configController = sb1.install();
        assertNotNull(configController);
        assertEquals(configController.getName(), HTTP_CONFIG);

        ServiceBuilder<HttpServer> sb2 = target.addService(HTTP_SERVER);
        Supplier<String> hostValue = sb2.requires(HTTP_HOST);
        Supplier<Integer> portValue = sb2.requires(HTTP_PORT);
        HttpServer server = new HttpServer(hostValue, portValue);
        sb2.setInstance(server);
        ServiceController serverController = sb2.install();
        assertNotNull(serverController);
        assertEquals(serverController.getName(), HTTP_SERVER);

        monitor.awaitStability();
        assertEquals(server.httpHost.get(), "localhost");
        assertEquals(server.httpPort.get(), new Integer(80));
        assertEquals(configController.getState(), ServiceController.State.UP);
        assertEquals(serverController.getState(), ServiceController.State.UP);
        assertEquals(dummyServiceController.getState(), ServiceController.State.UP);
        assertEquals(server.startMsg, "start(httpHost == localhost, httpPort == 80");
        assertNotNull(serviceListener.sb.toString());
        assertNotNull(lifecycleListener.sb.toString());

        configController.setMode(ServiceController.Mode.REMOVE);
        serviceContainer.awaitStability();
        assertEquals(configController.getState(), ServiceController.State.REMOVED);
        assertEquals(serverController.getState(), ServiceController.State.DOWN);
        assertEquals(dummyServiceController.getState(), ServiceController.State.UP);
        assertEquals(server.stopMsg, "stop(httpHost == localhost, httpPort == 80");

        serverController.setMode(ServiceController.Mode.REMOVE);
        serviceContainer.awaitStability();
        assertEquals(configController.getState(), ServiceController.State.REMOVED);
        assertEquals(serverController.getState(), ServiceController.State.REMOVED);
        assertEquals(dummyServiceController.getState(), ServiceController.State.DOWN);
    }

    private static final class HttpServer implements org.jboss.msc.Service {

        private final Supplier<String> httpHost;
        private final Supplier<Integer> httpPort;
        private volatile String startMsg;
        private volatile String stopMsg;

        private HttpServer(Supplier<String> httpHost, Supplier<Integer> httpPort) {
            this.httpHost = httpHost;
            this.httpPort = httpPort;
        }

        @Override
        public void start(StartContext context) {
            startMsg = "start(httpHost == " + httpHost.get() + ", httpPort == " + httpPort.get();
        }

        @Override
        public void stop(StopContext context) {
            stopMsg = "stop(httpHost == " + httpHost.get() + ", httpPort == " + httpPort.get();
        }

    }

    private static final class HttpConfigService implements org.jboss.msc.Service {
        private final Consumer<String> httpHost;
        private final Consumer<Integer> httpPort;

        private HttpConfigService(Consumer<String> httpHost, Consumer<Integer> httpPort) {
            this.httpHost = httpHost;
            this.httpPort = httpPort;
        }

        @Override
        public void start(StartContext context) {
            httpHost.accept("localhost");
            httpPort.accept(80);
        }

        @Override
        public void stop(StopContext context) {}
    }

    private static final class LegacyHttpServer implements Service<HttpServer> {
        private final InjectedValue<String> httpHost = new InjectedValue<String>();
        private final InjectedValue<Integer> httpPort = new InjectedValue<Integer>();
        private volatile String startMsg;
        private volatile String stopMsg;

        @Override
        public void start(StartContext context) {
            startMsg = "start(httpHost == " + httpHost.getValue() + ", httpPort == " + httpPort.getValue();
        }

        @Override
        public void stop(StopContext context) {
            stopMsg = "stop(httpHost == " + httpHost.getValue() + ", httpPort == " + httpPort.getValue();
        }

        private Injector<String> getHostInjector() {
            return httpHost;
        }

        private Injector<Integer> getPortInjector() {
            return httpPort;
        }

        @Override
        public HttpServer getValue() {
            throw new UnsupportedOperationException();
        }
    }

    private static final class LegacyHttpHostService implements Service<String> {
        private volatile String httpHost;

        @Override
        public void start(StartContext context) {
            httpHost = "localhost";
        }

        @Override
        public void stop(StopContext context) {
            httpHost = null;
        }

        @Override
        public String getValue() {
            return httpHost;
        }
    }

    private static final class LegacyHttpPortService implements Service<Integer> {
        private volatile Integer httpPort;

        @Override
        public void start(StartContext context) {
            httpPort = 80;
        }

        @Override
        public void stop(StopContext context) {
            httpPort = null;
        }

        @Override
        public Integer getValue() {
            return httpPort;
        }
    }

    private static final class LoggingServiceListener extends AbstractServiceListener<Object> {
        private final StringBuilder sb = new StringBuilder();

        @Override
        public synchronized void transition(ServiceController<?> controller, ServiceController.Transition transition) {
            sb.append("ServiceListener ").append(controller.getName()).append(" ").append(transition).append("\n");
        }
    }

    private static final class LoggingLifecycleListener implements LifecycleListener {
        private final StringBuilder sb = new StringBuilder();

        @Override
        public synchronized void handleEvent(ServiceController<?> controller, LifecycleEvent event) {
            sb.append("LifecycleListener ").append(controller.getName()).append(" ").append(event).append("\n");
        }
    }
}
