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

package org.jboss.msc.service;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Collection;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.logging.Logger;

import org.junit.After;
import org.junit.Before;

/**
 * Test base used for service test cases.
 *
 * @author John E. Bailey
 * @author <a href="mailto:flavia.rainone@jboss.com">Flavia Rainone</a>
 */
public class AbstractServiceTest {

    protected volatile ServiceContainer serviceContainer;
    private boolean shutdownOnTearDown;

    @Before
    public void setUp() throws Exception {
        Logger.getLogger("").fine("Setting up test " + getClass());
        serviceContainer = ServiceContainer.Factory.create();
        shutdownOnTearDown = true;
    }

    @After
    public void tearDown() throws Exception {
        Logger.getLogger("").fine("Tearing down test " + getClass());
        if (shutdownOnTearDown) {
            serviceContainer.shutdown();
            serviceContainer.awaitTermination();
        }
        serviceContainer = null;
    }

    /**
     * Shutdowns the container.
     */
    public void shutdownContainer() {
        serviceContainer.shutdown();
        try {
            serviceContainer.awaitTermination();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        shutdownOnTearDown = false;
    }

    /**
     * Asserts that {@code controller} is not null and that it is the controller identified by {@code serviceName},
     * installed in {@link serviceContainer}.
     * 
     * @param serviceName  the name of the service
     * @param controller   the controller to be asserted. It should be the same as the ServiceController identified by {@code
     *                     serviceName}, installed into {@code serviceContainer}
     * @return             {@code controller}
     */
    protected final ServiceController<?> assertController(ServiceName serviceName, ServiceController<?> controller) {
        // retrieve the service controller only after future returns
        assertNotNull(controller);
        ServiceController<?> expectedServiceController = serviceContainer.getService(serviceName);
        assertController(expectedServiceController, controller);
        return controller;
    }

    /**
     * Asserts that {@code futureController.get()} returns the controller identified by {@code serviceName}, installed in 
     * {@link serviceContainer}.
     * 
     * @param serviceName            the name of the service
     * @param futureController       future that should compute the ServiceController identified by {@code serviceName},
     *                               installed into {@code serviceContainer}
     * @return                       the {@code ServiceController} returned by {@code futureController}
     * @throws InterruptedException  exception that may be thrown by {@code futureController.get()} method
     * @throws ExecutionException    exception that may be thrown by {@code futureController.get()} method
     * @throws StartException        if {@code ServiceController} contains a StartException, the exception is thrown,
     *                               thus making it easier to identify unexpected failures in the test. For asserting
     *                               expected StartExceptions, see {@link #assertFailure(ServiceName, Future)} and
     *                               {@link #assertFailure(ServiceController, Future)}
     */
    protected final ServiceController<?> assertController(ServiceName serviceName, Future<ServiceController<?>> futureController) throws InterruptedException, ExecutionException, StartException {
        ServiceController<?> serviceController = futureController.get();
        // retrieve the service controller only after future returns
        ServiceController<?> expectedServiceController = serviceContainer.getService(serviceName);
        assertController(expectedServiceController, serviceController);
        return serviceController;
    }

    /**
     * Asserts that {@code futureController.get()} returns the {@code serviceController}, a controller that should have
     * been obtained by a previous call to {@link #assertController(ServiceName, Future)}, or by a previous service
     * lookup in a {@link ServiceRegistry} instance.
     *
     * @param serviceController      the expected result of {@code futureController.get()}
     * @param futureController       future that should compute a ServiceController
     * @throws InterruptedException  exception that may be thrown by {@code futureController.get()} method
     * @throws ExecutionException    exception that may be thrown by {@code futureController.get()} method
     * @throws RuntimeException      if {@code futureController} returns {@code null}, a check for whether
     *                               {@code ServiceController} contains a StartException is made. If found, the
     *                               exception is thrown wrapped in a RuntimeException, thus making it easier to
     *                               identify unexpected failures in the test. For asserting expected StartExceptions,
     *                               see {@link #assertFailure(ServiceName, Future)} and
     *                                {@link #assertFailure(ServiceController, Future)}
     */
    protected final void assertController(ServiceController<?> serviceController, Future<ServiceController<?>> futureController) throws InterruptedException, ExecutionException{
        ServiceController<?> newServiceController = futureController.get();
        assertController(serviceController, newServiceController);
    }

    private final void assertController(ServiceController<?> expectedServiceController, ServiceController<?> serviceController) {
        if (serviceController == null) {
            if (expectedServiceController.getStartException() != null) {
                throw new RuntimeException("Container obtained from futureController is null. ", expectedServiceController.getStartException());
            }
            fail("Controller obtained from futureController is null. State of expectedServiceController is " + expectedServiceController.getState());
        }
        assertSame(expectedServiceController, serviceController);
    }

    /**
     * Asserts that {@code serviceFailure.get()} returns a {@code StartException} occurred at the start attempt
     * performed by a service named {@code serviceName} installed at {@link serviceContainer}.
     * 
     * @param serviceName     the name of the service that is expected to have failed to start
     * @param serviceFailure  should compute the {@code StartException} thrown by the service
     * @return                the controller of the service
     * @throws InterruptedException  exception that may be thrown by {@code futureController.get()} method
     * @throws ExecutionException    exception that may be thrown by {@code futureController.get()} method
     */
    protected final ServiceController<?> assertFailure(ServiceName serviceName, Future<StartException> serviceFailure) throws InterruptedException, ExecutionException {
        ServiceController<?> serviceController = serviceContainer.getService(serviceName);
        assertNotNull(serviceController);
        assertFailure(serviceController, serviceFailure);
        return serviceController;
    }

    /**
     * Asserts that {@code serviceFailure.get()} returns a {@code StartException} occurred at the start attempt
     * performed by a {@code serviceController}.
     * 
     * @param serviceController controller of the service expected to have failed
     * @param serviceFailure  should compute the {@code StartException} thrown by the service
     * @throws InterruptedException  exception that may be thrown by {@code futureController.get()} method
     * @throws ExecutionException    exception that may be thrown by {@code futureController.get()} method
     */
    protected final void assertFailure(ServiceController<?> serviceController, Future<StartException> serviceFailure) throws InterruptedException, ExecutionException {
        StartException exception = serviceFailure.get();
        assertNotNull("Exception is null, current service state is "  + serviceController.getState(), exception);
        assertSame(serviceController.getStartException(), exception);
    }

    /**
     * Two opposite notifications are expected from {@code serviceController}. Depending on the order that 
     * asynchronous tasks are performed, the triggering events of such notifications may occur in an order opposite
     * from expected. In this case, no notification occurs, as the events nullify each other.
     * 
     * This method asserts that either both notifications were sent, or that none of them were sent.
     * 
     * @param serviceController the controller of the service
     * @param notification1     one of the dependency notifications
     * @param notification2     one of the dependency notifications
     */
    protected final void assertOppositeNotifications(ServiceController<?> serviceController,
            Future<ServiceController<?>> notification1, Future<ServiceController<?>> notification2) throws Exception {
        ServiceController<?> serviceController1 = notification1.get();
        ServiceController<?> serviceController2 = notification2.get();
        assertTrue("Notification1 resulted in " + serviceController1 + " while notification2 resulted in " + serviceController2,
                (serviceController == serviceController1 && serviceController == serviceController2) ||
                (serviceController1 == null && serviceController2 == null));
    }

    /**
     * Asserts that {@code serviceController} has the specified immediate unavailable dependencies.
     *
     * @param serviceController   the service controller to test
     * @param missingDependencies a complete list of all unavailable dependencies expected. The assertion will succeed
     *                            only if {@link ServiceController#getImmediateUnavailableDependencies()
     *                            serviceController.getImmediateUnavailableDependencies} returns a set containing all
     *                            service names specified by this parameter. If no unavailable dependency is provided,
     *                            the test asserts that {@code serviceController} has no immediate unavailable
     *                            dependencies
     */
    protected final void assertImmediateUnavailableDependencies(ServiceController<?> serviceController, ServiceName... missingDependencies) {
        assertNotNull(serviceController);
        Collection<ServiceName> result = serviceController.getImmediateUnavailableDependencies();
        assertEquals(missingDependencies.length, result.size());
        for (ServiceName missingDependency: missingDependencies) {
            assertTrue("Unavailable dependency " + missingDependency + " not found in controller missing immediate dependency set",
                    result.contains(missingDependency));
        }
    }

    /**
     * @author Stuart Douglas
     */
    private static final PrivilegedAction<ClassLoader> GET_TCCL_ACTION = new PrivilegedAction<ClassLoader>() {
        public ClassLoader run() {
            return Thread.currentThread().getContextClassLoader();
        }
    };

    /**
     * Asserts that the current Thread context class loader is the same as {@code expected}.
     * 
     * @param expected the expected context class loader of current thread
     */
    public static void assertContextClassLoader(ClassLoader expected) {
        final SecurityManager sm = System.getSecurityManager();
        final ClassLoader currentClassLoader = sm == null? GET_TCCL_ACTION.run(): AccessController.doPrivileged(GET_TCCL_ACTION);
        assertSame(expected, currentClassLoader);
    }
}
