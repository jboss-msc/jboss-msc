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
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Future;

import org.jboss.msc.util.TestServiceListener;
import org.jboss.msc.value.Values;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Test to verify the functionality of batch level dependencies.
 *
 * @author John Bailey
 * @author <a href="mailto:flavia.rainone@jboss.com">Flavia Rainone</a>
 */
public class BatchLevelDependenciesTestCase extends AbstractServiceTest {

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
        dependenciesField = ServiceInstanceImpl.class.getDeclaredField("dependencies");
        dependenciesField.setAccessible(true);
    }

    @Test
    public void testBatchLevel() throws Exception {
        final BatchBuilder builder = serviceContainer.batchBuilder();
        builder.addListener(listener);

        builder.addService(firstServiceName, Service.NULL).install();
        builder.addService(secondServiceName, Service.NULL).install();
        builder.addService(thirdServiceName, Service.NULL).install();
        builder.addService(fourthServiceName, Service.NULL).install();

        builder.addDependency(fourthServiceName);

        final Set<ServiceListener<Object>> listeners = builder.getListeners();
        assertNotNull(listeners);
        assertEquals(1, listeners.size());
        assertTrue(listeners.contains(listener));

        final Set<ServiceName> builderDependencies = builder.getDependencies();
        assertNotNull(builderDependencies);
        assertEquals(1, builderDependencies.size());
        assertTrue(builderDependencies.contains(fourthServiceName));

        final Future<ServiceController<?>> firstService = listener.expectServiceStart(firstServiceName);
        final Future<ServiceController<?>> secondService = listener.expectServiceStart(secondServiceName);
        final Future<ServiceController<?>> thirdService = listener.expectServiceStart(thirdServiceName);
        final Future<ServiceController<?>> fourthService = listener.expectServiceStart(fourthServiceName);

        builder.install();

        final ServiceController<?> fourthController = assertController(fourthServiceName, fourthService);
        final ServiceController<?> firstController = assertController(firstServiceName, firstService);

        List<ServiceInstanceImpl<?>> dependencies = getServiceDependencies(firstController);
        assertTrue(dependencies.contains(fourthController));

        dependencies = getServiceDependencies(secondService.get());
        assertTrue(dependencies.contains(fourthController));

        dependencies = getServiceDependencies(thirdService.get());
        assertTrue(dependencies.contains(fourthController));

        dependencies = getServiceDependencies(fourthController);
        assertFalse(dependencies.contains(fourthController));
    }

    @Test
    public void testBatchLevelWithDuplicateService() throws Exception {
        final BatchBuilder builder = serviceContainer.batchBuilder();
        builder.addListener(listener);

        builder.addService(firstServiceName, Service.NULL).install();
        try {
            builder.addService(firstServiceName, Service.NULL);
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException e) {}
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testBatchLevelWithServiceValues() throws Exception {
        BatchBuilder builder = serviceContainer.batchBuilder();
        final TestServiceListener listener1 = new TestServiceListener();
        final TestServiceListener listener2 = new TestServiceListener();
        builder.addListener(listener1, listener2);

        builder.addServiceValue(firstServiceName, Values.immediateValue(Service.NULL)).install();
        builder.addServiceValue(secondServiceName, Values.immediateValue(Service.NULL)).install();

        Set<ServiceListener<Object>> listeners = builder.getListeners();
        assertNotNull(listeners);
        assertEquals(2, listeners.size());
        assertTrue(listeners.contains(listener1));
        assertTrue(listeners.contains(listener2));

        Set<ServiceName> builderDependencies = builder.getDependencies();
        assertNotNull(builderDependencies);
        assertEquals(0, builderDependencies.size());
        
        final Future<ServiceController<?>> firstService1 = listener1.expectServiceStart(ServiceName.of("firstService"));
        final Future<ServiceController<?>> firstService2 = listener2.expectServiceStart(ServiceName.of("firstService"));
        final Future<ServiceController<?>> secondService1 = listener1.expectServiceStart(ServiceName.of("secondService"));
        final Future<ServiceController<?>> secondService2 = listener2.expectServiceStart(ServiceName.of("secondService"));
        builder.install();

        final ServiceController<?> firstController = assertController(firstServiceName, firstService1);
        assertController(firstController, firstService2);

        final ServiceController<?> secondController = assertController(secondServiceName, secondService1);
        assertController(secondController, secondService2);

        List<ServiceInstanceImpl<?>> dependencies = getServiceDependencies(firstController);
        assertEquals(1, dependencies.size());

        dependencies = getServiceDependencies(secondController);
        assertEquals(1, dependencies.size());

        builder = serviceContainer.batchBuilder();
        builder.addServiceValue(thirdServiceName, Values.immediateValue(Service.NULL)).install();
        builder.addServiceValue(fourthServiceName, Values.immediateValue(Service.NULL)).install();
        builder.addDependency(firstServiceName, secondServiceName, thirdServiceName);
        builder.addListener(listener2);

        listeners = builder.getListeners();
        assertNotNull(listeners);
        assertEquals(1, listeners.size());
        assertTrue(listeners.contains(listener2));

        builderDependencies = builder.getDependencies();
        assertNotNull(builderDependencies);
        assertEquals(3, builderDependencies.size());
        assertTrue(builderDependencies.contains(firstServiceName));
        assertTrue(builderDependencies.contains(secondServiceName));
        assertTrue(builderDependencies.contains(thirdServiceName));

        final Future<ServiceController<?>> thirdService = listener2.expectServiceStart(ServiceName.of("thirdService"));
        final Future<ServiceController<?>> fourthService = listener2.expectServiceStart(ServiceName.of("fourthService"));

        builder.install();

        final ServiceController<?> thirdController = assertController(thirdServiceName, thirdService);
        final ServiceController<?> fourthController = assertController(fourthServiceName, fourthService);

        dependencies = getServiceDependencies(thirdController);
        assertNotNull(dependencies);
        assertEquals(3, dependencies.size());
        assertTrue(dependencies.contains(firstController));
        assertTrue(dependencies.contains(secondController));

        dependencies = getServiceDependencies(fourthController);
        assertNotNull(dependencies);
        assertEquals(4, dependencies.size());
        assertTrue(dependencies.contains(firstController));
        assertTrue(dependencies.contains(secondController));
        assertTrue(dependencies.contains(thirdController));
    }

    @Test
    public void testSubBatchLevel() throws Exception {
        final BatchBuilder builder = serviceContainer.batchBuilder();
        builder.addListener(listener);

        builder.addService(firstServiceName, Service.NULL).install();
        final ServiceTarget subBatchBuilder = builder.subTarget();

        subBatchBuilder.addDependency(firstServiceName);
        subBatchBuilder.addDependency(fourthServiceName);

        subBatchBuilder.addService(secondServiceName, Service.NULL).install();
        subBatchBuilder.addService(thirdServiceName, Service.NULL).install();
        subBatchBuilder.addService(fourthServiceName, Service.NULL).install();

        Set<ServiceListener<Object>> listeners = builder.getListeners();
        assertNotNull(listeners);
        assertEquals(1, listeners.size());
        assertTrue(listeners.contains(listener));

        Set<ServiceName> builderDependencies = builder.getDependencies();
        assertNotNull(builderDependencies);
        assertEquals(0, builderDependencies.size());

        listeners = subBatchBuilder.getListeners();
        assertNotNull(listeners);
        assertEquals(0, listeners.size());

        builderDependencies = subBatchBuilder.getDependencies();
        assertNotNull(builderDependencies);
        assertEquals(2, builderDependencies.size());
        assertTrue(builderDependencies.contains(firstServiceName));
        assertTrue(builderDependencies.contains(fourthServiceName));

        final Future<ServiceController<?>> firstService = listener.expectServiceStart(firstServiceName);
        final Future<ServiceController<?>> secondService = listener.expectServiceStart(secondServiceName);
        final Future<ServiceController<?>> thirdService = listener.expectServiceStart(thirdServiceName);
        final Future<ServiceController<?>> fourthService = listener.expectServiceStart(fourthServiceName);

        builder.install();

        final ServiceController<?> firstController = assertController(firstServiceName, firstService);
        final ServiceController<?> fourthController = assertController(fourthServiceName, fourthService);

        List<ServiceInstanceImpl<?>> dependencies = getServiceDependencies(secondService.get());
        assertTrue(dependencies.contains(firstController));
        assertTrue(dependencies.contains(fourthController));

        dependencies = getServiceDependencies(thirdService.get());
        assertTrue(dependencies.contains(firstController));
        assertTrue(dependencies.contains(fourthController));

        dependencies = getServiceDependencies(fourthController);
        assertTrue(dependencies.contains(firstController));
        assertFalse(dependencies.contains(fourthController));
    }

    @Test
    public void testSubBatchLevelWithInternalBatch() throws Exception {
        final BatchBuilder builder = serviceContainer.batchBuilder();
        final TestServiceListener listener1 = new TestServiceListener();
        final TestServiceListener listener2 = new TestServiceListener();
        Collection<ServiceListener<Object>> listeners = new ArrayList<ServiceListener<Object>>();
        listeners.add(listener1);
        listeners.add(listener2);
        builder.addListener(listeners);

        builder.addService(firstServiceName, Service.NULL).install();
        final ServiceTarget subBatchBuilder = builder.subTarget();

        Collection<ServiceName> dependencies = new ArrayList<ServiceName>();
        dependencies.add(firstServiceName);
        dependencies.add(fourthServiceName);
        subBatchBuilder.addDependency(dependencies);

        subBatchBuilder.addService(secondServiceName, Service.NULL).install();
        
        BatchBuilder internalBuilder = subBatchBuilder.batchBuilder();
        internalBuilder.addService(thirdServiceName, Service.NULL).install();
        internalBuilder.addService(fourthServiceName, Service.NULL).install();
        internalBuilder.install();

        Set<ServiceListener<Object>> builderListeners = builder.getListeners();
        assertNotNull(builderListeners);
        assertEquals(2, listeners.size());
        assertTrue(listeners.contains(listener1));
        assertTrue(listeners.contains(listener2));

        Set<ServiceName> builderDependencies = builder.getDependencies();
        assertNotNull(builderDependencies);
        assertEquals(0, builderDependencies.size());

        builderListeners = subBatchBuilder.getListeners();
        assertNotNull(builderListeners);
        assertEquals(2, listeners.size());
        assertTrue(listeners.contains(listener1));
        assertTrue(listeners.contains(listener2));

        builderDependencies = subBatchBuilder.getDependencies();
        assertNotNull(builderDependencies);
        assertEquals(2, builderDependencies.size());
        assertTrue(builderDependencies.contains(firstServiceName));
        assertTrue(builderDependencies.contains(fourthServiceName));

        builderListeners = internalBuilder.getListeners();
        assertNotNull(builderListeners);
        assertEquals(2, listeners.size());
        assertTrue(listeners.contains(listener1));
        assertTrue(listeners.contains(listener2));

        builderDependencies = internalBuilder.getDependencies();
        assertNotNull(builderDependencies);
        assertEquals(0, builderDependencies.size());

        final Future<ServiceController<?>> firstService1 = listener1.expectServiceStart(firstServiceName);
        final Future<ServiceController<?>> firstService2 = listener2.expectServiceStart(firstServiceName);
        final Future<ServiceController<?>> secondService1 = listener1.expectServiceStart(secondServiceName);
        final Future<ServiceController<?>> secondService2 = listener2.expectServiceStart(secondServiceName);
        final Future<ServiceController<?>> thirdService1 = listener1.expectServiceStart(thirdServiceName);
        final Future<ServiceController<?>> thirdService2 = listener2.expectServiceStart(thirdServiceName);
        final Future<ServiceController<?>> fourthService1 = listener1.expectServiceStart(fourthServiceName);
        final Future<ServiceController<?>> fourthService2 = listener2.expectServiceStart(fourthServiceName);

        builder.install();

        final ServiceController<?> firstController = assertController(firstServiceName, firstService1);
        assertController(firstController, firstService2);
        final ServiceController<?> fourthController = assertController(fourthServiceName, fourthService1);
        assertController(fourthController, fourthService2);

        final ServiceController<?> secondController = assertController(secondServiceName, secondService1);
        assertController(secondController, secondService2);
        List<ServiceInstanceImpl<?>> serviceDependencies = getServiceDependencies(secondController);
        assertTrue(serviceDependencies.contains(firstController));
        assertTrue(serviceDependencies.contains(fourthController));

        final ServiceController<?> thirdController = assertController(thirdServiceName, thirdService1);
        assertController(thirdController, thirdService2);
        serviceDependencies = getServiceDependencies(thirdController);
        assertTrue(serviceDependencies.contains(firstController));
        assertTrue(serviceDependencies.contains(fourthController));

        serviceDependencies = getServiceDependencies(fourthController);
        assertTrue(serviceDependencies.contains(firstController));
        assertFalse(serviceDependencies.contains(fourthController));
        
        try {
            builder.addDependency(ServiceName.of("firstService"));
            fail ("IllegalStateException expected");
        } catch (IllegalStateException e) {}
    }

    @Test
    public void installDuplicateService() throws Exception {
        ServiceName serviceName = ServiceName.of("service");
        serviceContainer.addService(serviceName, Service.NULL);
        serviceContainer.addService(serviceName, Service.NULL).install();
        final BatchBuilder builder = serviceContainer.batchBuilder();
        builder.addService(ServiceName.of("service"), Service.NULL).install();
        try {
            builder.install();
            fail("DuplicateServiceExcpetion expected");
        } catch (DuplicateServiceException e) {}
    }

    @Test
    public void installNull() throws Exception {
        final BatchBuilder builder = serviceContainer.batchBuilder();
        /*builder.addDependency((ServiceName) null);
        builder.addService(ServiceName.of("service"), Service.NULL);
        try {
            builder.install();
            fail("NullPointerException expected");
        } catch (NullPointerException e) {}
*/
        try {
            builder.addDependency((ServiceName[]) null);
            fail("NullPointerException expected");
        } catch (NullPointerException e) {}

        try {
            builder.addDependency((Collection<ServiceName>) null);
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException e) {}

        try {
            builder.addListener((ServiceListener<Object>[]) null);
            fail("NullPointerException expected");
        } catch (NullPointerException e) {}

        try {
            builder.addListener((Collection<ServiceListener<Object>>) null);
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException e) {}

        try {
            builder.addService(null, Service.NULL);
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException e) {}

       try {
            builder.addServiceValue(null, Values.immediateValue(Service.NULL));
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException e) {}
    }

    private List<ServiceInstanceImpl<?>> getServiceDependencies(ServiceController<?> serviceController) throws IllegalAccessException {
        Dependency[] deps = (Dependency[]) dependenciesField.get(serviceController);
        List<ServiceInstanceImpl<?>> depInstances = new ArrayList<ServiceInstanceImpl<?>>(deps.length);
        for (Dependency dep: deps) {
            ServiceInstanceImpl<?> depInstance = (ServiceInstanceImpl<?>) ((ServiceRegistrationImpl)dep).getInstance();
            if (depInstance != null) {
                depInstances.add(depInstance);
            }
        }
        return depInstances;
    }
}