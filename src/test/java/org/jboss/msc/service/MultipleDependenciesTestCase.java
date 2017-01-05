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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.lang.reflect.Field;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.jboss.msc.util.TestServiceListener;
import org.jboss.msc.value.Values;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Test to verify the behavior of multiple dependencies.
 *
 * @author John Bailey
 * @author <a href="mailto:flavia.rainone@jboss.com">Flavia Rainone</a>
 */
public class MultipleDependenciesTestCase extends AbstractServiceTest {

    public static final String MODULE = "module";
    private static Field dependenciesField;
    private static final ServiceName firstServiceName = ServiceName.of("firstService");
    private static final ServiceName secondServiceName = ServiceName.of("secondService");
    private static final ServiceName thirdServiceName = ServiceName.of("thirdService");
    private static final ServiceName fourthServiceName = ServiceName.of("fourthService");
    private TestServiceListener listener;

    @Before
    public void setUpTestListener() {
        listener = new TestServiceListener();
    }

    @BeforeClass
    public static void initDependenciesField() throws Exception {
        dependenciesField = ServiceControllerImpl.class.getDeclaredField("dependencies");
        dependenciesField.setAccessible(true);
    }

    @Test
    public void testSomeThings2() throws Exception {
        for(int i = 0; i < 1000; ++i) {

            ServiceName s3 = ServiceName.JBOSS.append("s3");
            ServiceName s2 = ServiceName.JBOSS.append("s2");
            ServiceName s1 = ServiceName.JBOSS.append("s1");
            ServiceController<Void> c3 = serviceContainer.addService(s3, new RootService(s3, s2.append(MODULE)))
                    .install();
            ServiceController<Void> c2 = serviceContainer.addService(s2, new RootService(s2, s1.append(MODULE)))
                    .install();
            ServiceController<Void> c1 = serviceContainer.addService(s1, new RootService(s1))
                    .install();
            serviceContainer.awaitStability();
            final CountDownLatch latch = new CountDownLatch(1);
            c1.addListener(new AbstractServiceListener<Void>() {
                @Override
                public void transition(ServiceController<? extends Void> controller, ServiceController.Transition transition) {
                    if (transition.getAfter() == ServiceController.Substate.REMOVED) {
                        latch.countDown();
                    }
                }
            });
            c1.setMode(ServiceController.Mode.REMOVE);
            latch.await();
            c1 = serviceContainer.addService(s1, new RootService(s1))
                    .install();
            if(!serviceContainer.awaitStability(2, TimeUnit.SECONDS)) {
                serviceContainer.dumpServices();
                Assert.fail();
            }
            c1.setMode(ServiceController.Mode.REMOVE);
            c2.setMode(ServiceController.Mode.REMOVE);
            c3.setMode(ServiceController.Mode.REMOVE);
            if(!serviceContainer.awaitStability(2, TimeUnit.SECONDS)) {
                serviceContainer.dumpServices();
                Assert.fail();
            }
        }
    }

    private class RootService extends AbstractService<Void> {
        final ServiceName baseName;
        private final ServiceName[] serviceNames;

        private RootService(ServiceName baseName, ServiceName... serviceNames) {
            this.baseName = baseName;
            this.serviceNames = serviceNames;
        }

        @Override
        public void start(final StartContext context) throws StartException {
            ServiceName module = baseName.append(MODULE);
            context.getChildTarget().addService(baseName.append("firstModuleUse"), new FirstModuleUseService())
                    .addDependency(module)
                    .install();
            context.getChildTarget().addService(module, Service.NULL)
                    .addDependencies(serviceNames)
                    .install();
        }
    }

    private class FirstModuleUseService extends AbstractService<Void> {

        volatile boolean first = true;

        @Override
        public void start(final StartContext context) throws StartException {
            if(first) {
                first = false;
            } else {
                first = true;
                context.getController().getParent().addListener(new AbstractServiceListener() {
                    @Override
                    public void transition(ServiceController controller, ServiceController.Transition transition) {
                        if(transition.getAfter() == ServiceController.Substate.DOWN) {
                            controller.setMode(ServiceController.Mode.ACTIVE);
                            controller.removeListener(this);
                        }
                    }
                });
                context.getController().getParent().setMode(ServiceController.Mode.NEVER);
            }
        }
    }

    @Test
    public void test() throws Exception {
        serviceContainer.addListener(listener);
        serviceContainer.addDependency(fourthServiceName);

        final Set<ServiceListener<Object>> listeners = serviceContainer.getListeners();
        assertNotNull(listeners);
        assertEquals(1, listeners.size());
        assertTrue(listeners.contains(listener));

        final Set<ServiceName> builderDependencies = serviceContainer.getDependencies();
        assertNotNull(builderDependencies);
        assertEquals(1, builderDependencies.size());
        assertTrue(builderDependencies.contains(fourthServiceName));

        final Future<ServiceController<?>> firstService = listener.expectServiceStart(firstServiceName);
        final Future<ServiceController<?>> secondService = listener.expectServiceStart(secondServiceName);
        final Future<ServiceController<?>> thirdService = listener.expectServiceStart(thirdServiceName);
        final Future<ServiceController<?>> fourthService = listener.expectServiceStart(fourthServiceName);

        serviceContainer.addService(firstServiceName, Service.NULL).install();
        serviceContainer.addService(secondServiceName, Service.NULL).install();
        serviceContainer.addService(thirdServiceName, Service.NULL).install();
        serviceContainer.addService(fourthServiceName, Service.NULL).install();

        final ServiceController<?> fourthController = assertController(fourthServiceName, fourthService);
        final ServiceController<?> firstController = assertController(firstServiceName, firstService);

        List<ServiceControllerImpl<?>> dependencies = getServiceDependencies(firstController);
        assertTrue(dependencies.contains(fourthController));

        dependencies = getServiceDependencies(secondService.get());
        assertTrue(dependencies.contains(fourthController));

        dependencies = getServiceDependencies(thirdService.get());
        assertTrue(dependencies.contains(fourthController));

        dependencies = getServiceDependencies(fourthController);
        assertFalse(dependencies.contains(fourthController));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testWithServiceValues() throws Exception {
        final TestServiceListener listener1 = new TestServiceListener();
        final TestServiceListener listener2 = new TestServiceListener();
        serviceContainer.addListener(listener1, listener2);

        final Future<ServiceController<?>> firstService1 = listener1.expectServiceStart(ServiceName.of("firstService"));
        final Future<ServiceController<?>> firstService2 = listener2.expectServiceStart(ServiceName.of("firstService"));
        final Future<ServiceController<?>> secondService1 = listener1.expectServiceStart(ServiceName.of("secondService"));
        final Future<ServiceController<?>> secondService2 = listener2.expectServiceStart(ServiceName.of("secondService"));

        serviceContainer.addServiceValue(firstServiceName, Values.immediateValue(Service.NULL)).install();
        serviceContainer.addServiceValue(secondServiceName, Values.immediateValue(Service.NULL)).install();

        Set<ServiceListener<Object>> listeners = serviceContainer.getListeners();
        assertNotNull(listeners);
        assertEquals(2, listeners.size());
        assertTrue(listeners.contains(listener1));
        assertTrue(listeners.contains(listener2));

        Set<ServiceName> containerDependencies = serviceContainer.getDependencies();
        assertNotNull(containerDependencies);
        assertEquals(0, containerDependencies.size());

        final ServiceController<?> firstController = assertController(firstServiceName, firstService1);
        assertController(firstController, firstService2);

        final ServiceController<?> secondController = assertController(secondServiceName, secondService1);
        assertController(secondController, secondService2);

        List<ServiceControllerImpl<?>> dependencies = getServiceDependencies(firstController);
        assertEquals(0, dependencies.size());

        dependencies = getServiceDependencies(secondController);
        assertEquals(0, dependencies.size());

        final Future<ServiceController<?>> thirdService = listener2.expectServiceStart(ServiceName.of("thirdService"));
        final Future<ServiceController<?>> fourthService = listener2.expectServiceStart(ServiceName.of("fourthService"));

        serviceContainer.addDependency(firstServiceName, secondServiceName, thirdServiceName);
        serviceContainer.addListener(listener2);
        serviceContainer.addServiceValue(thirdServiceName, Values.immediateValue(Service.NULL)).install();
        serviceContainer.addServiceValue(fourthServiceName, Values.immediateValue(Service.NULL)).install();

        listeners = serviceContainer.getListeners();
        assertNotNull(listeners);
        assertEquals(2, listeners.size());
        assertTrue(listeners.contains(listener2));

        containerDependencies = serviceContainer.getDependencies();
        assertNotNull(containerDependencies);
        assertEquals(3, containerDependencies.size());
        assertTrue(containerDependencies.contains(firstServiceName));
        assertTrue(containerDependencies.contains(secondServiceName));
        assertTrue(containerDependencies.contains(thirdServiceName));

        final ServiceController<?> thirdController = assertController(thirdServiceName, thirdService);
        final ServiceController<?> fourthController = assertController(fourthServiceName, fourthService);

        dependencies = getServiceDependencies(thirdController);
        assertNotNull(dependencies);
        assertEquals(2, dependencies.size());
        assertTrue(dependencies.contains(firstController));
        assertTrue(dependencies.contains(secondController));

        dependencies = getServiceDependencies(fourthController);
        assertNotNull(dependencies);
        assertEquals(3, dependencies.size());
        assertTrue(dependencies.contains(firstController));
        assertTrue(dependencies.contains(secondController));
        assertTrue(dependencies.contains(thirdController));
    }

    @Test
    public void installNull() throws Exception {
        /*builder.addDependency((ServiceName) null);
        builder.addService(ServiceName.of("service"), Service.NULL);
        try {
            builder.install();
            fail("NullPointerException expected");
        } catch (NullPointerException e) {}
*/
        // No exception expected
        serviceContainer.addDependency((ServiceName[]) null);
        serviceContainer.addDependency((Collection<ServiceName>) null);
        serviceContainer.addListener((ServiceListener<Object>[]) null);
        serviceContainer.addListener((Collection<ServiceListener<Object>>) null);
        try {
            serviceContainer.addService(null, Service.NULL);
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException e) {}

       try {
            serviceContainer.addServiceValue(null, Values.immediateValue(Service.NULL));
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException e) {}
    }

    private List<ServiceControllerImpl<?>> getServiceDependencies(ServiceController<?> serviceController) throws IllegalAccessException {
        Dependency[] deps = (Dependency[]) dependenciesField.get(serviceController);
        List<ServiceControllerImpl<?>> depInstances = new ArrayList<ServiceControllerImpl<?>>(deps.length);
        for (Dependency dep: deps) {
            ServiceControllerImpl<?> depInstance = (ServiceControllerImpl<?>) ((ServiceRegistrationImpl)dep).getInstance();
            if (depInstance != null) {
                depInstances.add(depInstance);
            }
        }
        return depInstances;
    }
}
