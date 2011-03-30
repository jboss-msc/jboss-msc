/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Future;

import org.jboss.msc.inject.FieldInjector;
import org.jboss.msc.inject.SetMethodInjector;
import org.jboss.msc.service.ServiceBuilder.DependencyType;
import org.jboss.msc.service.ServiceController.Mode;
import org.jboss.msc.service.ServiceController.State;
import org.jboss.msc.util.TestServiceListener;
import org.jboss.msc.value.Values;
import org.junit.Test;

/**
 * Test for {@link ServiceBuilder} implementations.
 * 
 * @author <a href="mailto:flavia.rainone@jboss.com">Flavia Rainone</a>
 */
@SuppressWarnings("deprecation")
public abstract class AbstractServiceBuilderTest extends AbstractServiceTest {

    private final TestServiceListener testListener = new TestServiceListener();
    private final ServiceName serviceName = ServiceName.of("service");
    private final ServiceName dummyServiceName = ServiceName.of("dummy", "service");
    private final ServiceName anotherServiceName = ServiceName.of("any", "other", "dummy", "service");
    private final ServiceName uninstalledServiceName = ServiceName.of("uninstalled", "service");
    private final ServiceName descriptionServiceName = ServiceName.of("service", "description");
    private final ServiceName helperServiceName = ServiceName.of("service", "helper");
    private final ServiceName serviceNameAlias1 = ServiceName.of("same", "alias");
    private final ServiceName serviceNameAlias2 = ServiceName.of("same", "alias"); // on purpose, two equal aliases
    private final ServiceName serviceNameAlias3 = ServiceName.of("service", "alias");
    private final ServiceName serviceNameAlias4 = ServiceName.of("another", "alias");

    /**
     * Returns the ServiceBuilder that should be tested.
     * 
     * @param serviceBuilder a serviceBuilder 
     * @return  a servicer builder that delegates to {@code serviceBuilder}, providing ServiceBuilders that
     *          will be installed into the same target as {@code serviceBuilder}
     */
    protected abstract <T> ServiceBuilder<T> getServiceBuilder(ServiceBuilder<T> serviceBuilder);

    @Test
    // most test methods add services with alias, listeners, dependencies and location
    public void addServiceWithoutInjection() throws Exception {
        // create dummyManager and dummyService
        final DummyManager dummyManager = new DummyManager();
        final Service<DummyManager> service = new DummyService(dummyManager);
        final Location location = createLocation(null, 50, 23, null);

        // install dummy service
        ServiceBuilder<?> serviceBuilder = getServiceBuilder(serviceContainer.addService(serviceName, service));
        // with aliases
        serviceBuilder.addAliases(dummyServiceName);
        serviceBuilder.addAliases();
        // with dependencies on another and uninstalled services
        serviceBuilder.addDependencies(DependencyType.REQUIRED, anotherServiceName);
        // uninstalled service is an optional dependency
        // adding service as its own dependent should be ignored
        serviceBuilder.addDependencies(DependencyType.OPTIONAL, uninstalledServiceName, serviceName);
        // add test listener
        serviceBuilder.addListener(testListener);
        // add a location
        serviceBuilder.setLocation(location);

        Future<ServiceController<?>> dummyServiceListenerAdded = testListener.expectListenerAdded(ServiceName.of("service"));
        ServiceController<?> dummyController = serviceBuilder.install();
        assertController(serviceName, dummyController);
        assertController(dummyController, dummyServiceListenerAdded);
        assertEquals(location,  dummyController.getLocation());
        ServiceName[] aliases = dummyController.getAliases();
        assertNotNull(aliases);
        assertEquals(1, aliases.length);
        assertEquals(dummyServiceName, aliases[0]);
        assertNull(dummyManager.getDescription());// dummyManager is not initialized yet

        Future<ServiceController<?>> dummyServiceStart = testListener.expectServiceStart(serviceName);
        Future<ServiceController<?>> anotherDummyServiceStart = testListener.expectServiceStart(anotherServiceName);
        // install the missing non-optional dependency another service
        serviceBuilder = getServiceBuilder(serviceContainer.addService(anotherServiceName, Service.NULL)).addListener(testListener);
        ServiceController<?> anotherController = serviceBuilder.install();
        assertController(anotherServiceName, anotherController);
        assertController(anotherController, anotherDummyServiceStart);
        // dummy service is expected to start
        assertController(dummyController, dummyServiceStart);
        // so description of dummy manager should have been initialized
        assertEquals("", dummyManager.getDescription());
        // dummy service should have dummy manager as its value
        assertSame(dummyManager, dummyController.getValue());
    }

    @Test
    // most test methods add services with alias, listeners, dependencies and location
    public void addServiceWithoutInjectionAndWithOptionalDepDoesNotOverride() throws Exception {
        // create dummyManager and dummyService
        final DummyManager dummyManager = new DummyManager();
        final Service<DummyManager> service = new DummyService(dummyManager);
        final Location location = createLocation(null, 50, 23, null);

        // install dummy service
        ServiceBuilder<?> serviceBuilder = getServiceBuilder(serviceContainer.addService(serviceName, service));
        // with aliases
        serviceBuilder.addAliases(dummyServiceName);
        serviceBuilder.addAliases();
        // with dependencies on another and uninstalled services
        Collection<ServiceName> dependencies = new ArrayList<ServiceName>();
        dependencies.add(anotherServiceName);
        dependencies.add(uninstalledServiceName);
        serviceBuilder.addDependencies(dependencies);
        dependencies = new ArrayList<ServiceName>();
        // try to override previous action by making uninstalled service an optional dependency
        // adding service as its own dependent should be ignored
        serviceBuilder.addOptionalDependencies(uninstalledServiceName, serviceName);
        // add optional dependency on helper service, with an injection on helper property
        serviceBuilder.addDependency(DependencyType.OPTIONAL, helperServiceName,
                new SetMethodInjector<Object>(Values.immediateValue(dummyManager),
                        DummyManager.class.getMethod("setHelper", DummyHelper.class)));
        // add test listener
        serviceBuilder.addListener(testListener);
        // add a location
        serviceBuilder.setLocation(location);

        Future<ServiceController<?>> dummyServiceListenerAdded = testListener.expectListenerAdded(ServiceName.of("service"));
        ServiceController<?> dummyController = serviceBuilder.install();
        assertController(serviceName, dummyController);
        assertController(dummyController, dummyServiceListenerAdded);
        assertEquals(location,  dummyController.getLocation());
        ServiceName[] aliases = dummyController.getAliases();
        assertNotNull(aliases);
        assertEquals(1, aliases.length);
        assertEquals(dummyServiceName, aliases[0]);
        assertNull(dummyManager.getDescription());// dummyManager is not initialized yet

        assertSame(dummyController.getState(), State.DOWN);

        Future<ServiceController<?>> uninstalledServiceStart = testListener.expectServiceStart(uninstalledServiceName);
        ServiceController<?> uninstalledController = getServiceBuilder(serviceContainer.addService(uninstalledServiceName, Service.NULL))
            .addListener(testListener).install();
        assertController(uninstalledServiceName, uninstalledController);
        assertController(uninstalledController, uninstalledServiceStart);

        Future<ServiceController<?>> dummyServiceStart = testListener.expectServiceStart(serviceName);
        Future<ServiceController<?>> anotherDummyServiceStart = testListener.expectServiceStart(anotherServiceName);
        // install the missing non-optional dependency another service
        serviceBuilder = getServiceBuilder(serviceContainer.addService(anotherServiceName, Service.NULL)).addListener(testListener);
        ServiceController<?> anotherController = assertController(anotherServiceName, serviceBuilder.install());
        assertController(anotherController, anotherDummyServiceStart);
        // dummy service is expected to start
        assertController(dummyController, dummyServiceStart);
        // so description of dummy manager should have been initialized
        assertEquals("", dummyManager.getDescription());
        // dummy service should have dummy manager as its value
        assertSame(dummyManager, dummyController.getValue());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void addServiceWithDefaultLocationAndInjection() throws Exception {
        Future<ServiceController<?>> anotherServiceStart = testListener.expectServiceStart(anotherServiceName);
        // install another service
        ServiceController<?> anotherServiceController = getServiceBuilder(serviceContainer.addService(anotherServiceName, Service.NULL))
            .addListener(testListener).install();
        assertController(anotherServiceName, anotherServiceController);
        assertController(anotherServiceController, anotherServiceStart);

        // create dummy manager
        DummyManager dummyManager = new DummyManager();
        // and dummy service is a plain value service 
        Service<DummyManager> service = new ValueService<DummyManager>(Values.immediateValue(dummyManager));
        // start installation of dummy service
        ServiceBuilder<DummyManager> serviceBuilder = getServiceBuilder(serviceContainer.addService(serviceName, service));
        // with with 5 aliases. Two of them are equal (alias1 and alias2), and the fifth is the same serviceName
        // used for the primary registration
        serviceBuilder.addAliases(serviceNameAlias1, serviceNameAlias2, serviceNameAlias3, serviceNameAlias4, serviceName);
        // add dependencies on another service, uninstalled service, and service
        // (notice that the last one is the service being installed itself)
        Collection<ServiceName> dependencies = new ArrayList<ServiceName>();
        dependencies.add(anotherServiceName);
        dependencies.add(serviceName);
        Iterable<ServiceName> iterableDependencies = dependencies;
        serviceBuilder.addDependencies(DependencyType.REQUIRED, iterableDependencies);
        // add optional dependencies on uninstalled and service (the dependency on service should be ignored  nonetheless)
        dependencies = new ArrayList<ServiceName>();
        dependencies.add(uninstalledServiceName);
        dependencies.add(serviceName);
        serviceBuilder.addOptionalDependencies(dependencies);
        // add three listeners, with a duplication addition for testListener1
        final TestServiceListener testListener1 = new TestServiceListener();
        final TestServiceListener testListener2 = new TestServiceListener();
        final TestServiceListener testListener3 = new TestServiceListener();
        serviceBuilder.addListener(testListener1, testListener2, testListener3, testListener1);
        // set default location
        serviceBuilder.setLocation();
        // inject a description into description property of dummy manager
        serviceBuilder.addInjection(new SetMethodInjector<String>(dummyManager, DummyManager.class, "setDescription", String.class), "real description");

        Future<ServiceController<?>> dummyServiceStart1 = testListener1.expectServiceStart(serviceName);
        Future<ServiceController<?>> dummyServiceStart2 = testListener2.expectServiceStart(serviceName);
        Future<ServiceController<?>> dummyServiceStart3 = testListener3.expectServiceStart(serviceName);
        // finally install the service
        ServiceController<?> dummyController = assertController(serviceName, serviceBuilder.install());
        // three notifications expected, one from each listener
        assertController(dummyController, dummyServiceStart1);
        assertController(dummyController, dummyServiceStart2);
        assertController(dummyController, dummyServiceStart3);
        // the value of dummy controller should be dummy manager
        assertSame(dummyManager, dummyController.getValue());
        assertNotNull(dummyController.getLocation());
        ServiceName[] aliases = dummyController.getAliases();
        assertEquals(3, aliases.length);
        assertTrue((serviceNameAlias1.equals(aliases[0]) && serviceNameAlias3.equals(aliases[1]) && serviceNameAlias4.equals(aliases[2])) ||
                (serviceNameAlias1.equals(aliases[0]) && serviceNameAlias3.equals(aliases[2]) && serviceNameAlias4.equals(aliases[1])) ||
                (serviceNameAlias1.equals(aliases[1]) && serviceNameAlias3.equals(aliases[0]) && serviceNameAlias4.equals(aliases[2])) ||
                (serviceNameAlias1.equals(aliases[1]) && serviceNameAlias3.equals(aliases[2]) && serviceNameAlias4.equals(aliases[0])) ||
                (serviceNameAlias1.equals(aliases[2]) && serviceNameAlias3.equals(aliases[0]) && serviceNameAlias4.equals(aliases[1])) ||
                (serviceNameAlias1.equals(aliases[2]) && serviceNameAlias3.equals(aliases[1]) && serviceNameAlias4.equals(aliases[0])));
        // that, at this point, should have the injection of description performed
        assertEquals("real description", dummyManager.getDescription());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void addServiceWithDefaultLocationAndInjectionAndWithOptionalDepOverride() throws Exception {
        Future<ServiceController<?>> anotherServiceStart = testListener.expectServiceStart(anotherServiceName);
        // install another service
        ServiceBuilder<?> serviceBuilder = getServiceBuilder(serviceContainer.addService(anotherServiceName, Service.NULL))
            .addListener(testListener);
        ServiceController<?> anotherController = assertController(anotherServiceName, serviceBuilder.install());
        assertController(anotherController, anotherServiceStart);

        // create dummy manager
        DummyManager dummyManager = new DummyManager();
        // and dummy service is a plain value service 
        Service<DummyManager> service = new ValueService<DummyManager>(Values.immediateValue(dummyManager));
        // start installation of dummy service
        serviceBuilder = getServiceBuilder(serviceContainer.addService(serviceName, service));
        // with with 5 aliases. Two of them are equal (alias1 and alias2), and the fifth is the same serviceName
        // used for the primary registration
        serviceBuilder.addAliases(serviceNameAlias1, serviceNameAlias2, serviceNameAlias3, serviceNameAlias4, serviceName);
        // add dependencies on another service, uninstalled service, and service
        // (notice that the last one is the service being installed itself)
        Collection<ServiceName> dependencies = new ArrayList<ServiceName>();
        dependencies.add(uninstalledServiceName);
        dependencies.add(serviceName);
        serviceBuilder.addDependency(anotherServiceName);
        serviceBuilder.addDependencies(dependencies);
        // overwrite dependencies on uninstalled and service as optional ones (the dependency on service should be
        // ignored  nonetheless; but the dependency on uninstalled won't be overriden) 
        serviceBuilder.addDependency(DependencyType.OPTIONAL, uninstalledServiceName);
        serviceBuilder.addOptionalDependency(serviceName);
        // add three listeners, with a duplication addition for testListener1
        final TestServiceListener testListener1 = new TestServiceListener();
        final TestServiceListener testListener2 = new TestServiceListener();
        final TestServiceListener testListener3 = new TestServiceListener();
        serviceBuilder.addListener(testListener1, testListener2, testListener3, testListener1);
        // set default location
        serviceBuilder.setLocation();
        // inject a description into description property of dummy manager
        serviceBuilder.addInjection(new SetMethodInjector<String>(dummyManager, DummyManager.class, "setDescription", String.class), "real description");

        Future<ServiceController<?>> dummyServiceMissingDependency1 = testListener1.expectImmediateDependencyUninstall(serviceName);
        Future<ServiceController<?>> dummyServiceMissingDependency2 = testListener2.expectImmediateDependencyUninstall(serviceName);
        Future<ServiceController<?>> dummyServiceMissingDependency3 = testListener3.expectImmediateDependencyUninstall(serviceName);
        // finally install the service
        ServiceController<?> dummyController = assertController(serviceName, serviceBuilder.install());
        // three notifications expected, one from each listener
        assertController(dummyController, dummyServiceMissingDependency1);
        assertController(dummyController, dummyServiceMissingDependency2);
        assertController(dummyController, dummyServiceMissingDependency3);
        // should have a non null location
        assertNotNull(dummyController.getLocation());
        ServiceName[] aliases = dummyController.getAliases();
        assertNotNull(aliases);
        assertEquals(3, aliases.length);
        assertTrue((serviceNameAlias1.equals(aliases[0]) && serviceNameAlias3.equals(aliases[1]) && serviceNameAlias4.equals(aliases[2])) ||
                (serviceNameAlias1.equals(aliases[0]) && serviceNameAlias3.equals(aliases[2]) && serviceNameAlias4.equals(aliases[1])) ||
                (serviceNameAlias1.equals(aliases[1]) && serviceNameAlias3.equals(aliases[0]) && serviceNameAlias4.equals(aliases[2])) ||
                (serviceNameAlias1.equals(aliases[1]) && serviceNameAlias3.equals(aliases[2]) && serviceNameAlias4.equals(aliases[0])) ||
                (serviceNameAlias1.equals(aliases[2]) && serviceNameAlias3.equals(aliases[0]) && serviceNameAlias4.equals(aliases[1])) ||
                (serviceNameAlias1.equals(aliases[2]) && serviceNameAlias3.equals(aliases[1]) && serviceNameAlias4.equals(aliases[0])));
        //asserTrue((aliases[0].equals(serviceNameAlias1) || ))
        assertNull(dummyManager.getDescription());

        Future<ServiceController<?>> uninstalledServiceStart = testListener.expectServiceStart(uninstalledServiceName);
        Future<ServiceController<?>> dummyServiceStart1 = testListener1.expectServiceStart(serviceName);
        Future<ServiceController<?>> dummyServiceStart2 = testListener2.expectServiceStart(serviceName);
        Future<ServiceController<?>> dummyServiceStart3 = testListener3.expectServiceStart(serviceName);
        serviceBuilder = getServiceBuilder(serviceContainer.addService(uninstalledServiceName, Service.NULL))
            .addListener(testListener);
        ServiceController<?> uninstalledController = assertController(uninstalledServiceName, serviceBuilder.install());
        assertController(uninstalledController, uninstalledServiceStart);
        // three notifications expected again
        assertController(dummyController, dummyServiceStart1);
        assertController(dummyController, dummyServiceStart2);
        assertController(dummyController, dummyServiceStart3);
        // the value of dummy controller should be dummy manager
        assertSame(dummyManager, dummyController.getValue());
        // the value of dummy controller should still be dummy manager
        assertSame(dummyManager, dummyController.getValue());
        // location continues to exist as a non null value
        assertNotNull(dummyController.getLocation());
        // and, at this point, the injection of description should have been performed
        assertEquals("real description", dummyManager.getDescription());
    }

    @Test
    public void addServiceWithDependencyInjection() throws Exception {
        final Location location = createLocation("File", -1, -1, null);

        Future<ServiceController<?>> anotherServiceStart = testListener.expectServiceStart(anotherServiceName);
        // install another service, with a string value
        ServiceBuilder<?> serviceBuilder = serviceContainer.addService(anotherServiceName, new ValueService<String>(
                Values.immediateValue("final description"))).addListener(testListener);
        ServiceController<?> anotherController = assertController(anotherServiceName, serviceBuilder.install());
        assertController(anotherController, anotherServiceStart);

        // create dummy manager
        final DummyManager dummyManager = new DummyManager();
        // and dummy service, a plain value service whose valu is dummy manager
        final Service<DummyManager> service = new ValueService<DummyManager>(Values.immediateValue(dummyManager));
        // start installation of dummy service
        serviceBuilder = getServiceBuilder(serviceContainer.addService(serviceName, service));
        // with with 5 aliases. Two of them are equal (alias1 and alias2), and the fifth is the same serviceName
        // used for the primary registration
        serviceBuilder.addAliases(serviceNameAlias1, serviceNameAlias2, serviceNameAlias3, serviceNameAlias4, serviceName);
        // add dependency on another service with injection... its value should be injected into the description property
        // of dummy manager
        serviceBuilder.addDependency(anotherServiceName, String.class,
                new SetMethodInjector<String>(dummyManager, DummyManager.class, "setDescription", String.class));
        // add dependencies on another service (redundant given previous step), uninstalled service, and service
        // (notice that the last one is the service being installed itself)
        Collection<ServiceName> dependencies = new ArrayList<ServiceName>();
        dependencies.add(anotherServiceName);
        dependencies.add(serviceName);
        serviceBuilder.addDependencies(dependencies);
        // add optional dependency on uninstalled service
        dependencies = new ArrayList<ServiceName>();
        dependencies.add(uninstalledServiceName);
        dependencies.add(serviceName);
        serviceBuilder.addOptionalDependencies(dependencies);
        // add test listener
        serviceBuilder.addListener(testListener);
        // create a location for service
        serviceBuilder.setLocation(location);

        Future<ServiceController<?>> dummyServiceStart = testListener.expectServiceStart(serviceName);
        // finally install dummy service
        ServiceController<?> dummyController = assertController(serviceName, serviceBuilder.install());
        assertController(dummyController, dummyServiceStart);
        assertEquals(location, dummyController.getLocation());
        ServiceName[] aliases = dummyController.getAliases();
        assertNotNull(aliases);
        assertEquals(3, aliases.length);
        assertTrue((serviceNameAlias1.equals(aliases[0]) && serviceNameAlias3.equals(aliases[1]) && serviceNameAlias4.equals(aliases[2])) ||
                (serviceNameAlias1.equals(aliases[0]) && serviceNameAlias3.equals(aliases[2]) && serviceNameAlias4.equals(aliases[1])) ||
                (serviceNameAlias1.equals(aliases[1]) && serviceNameAlias3.equals(aliases[0]) && serviceNameAlias4.equals(aliases[2])) ||
                (serviceNameAlias1.equals(aliases[1]) && serviceNameAlias3.equals(aliases[2]) && serviceNameAlias4.equals(aliases[0])) ||
                (serviceNameAlias1.equals(aliases[2]) && serviceNameAlias3.equals(aliases[0]) && serviceNameAlias4.equals(aliases[1])) ||
                (serviceNameAlias1.equals(aliases[2]) && serviceNameAlias3.equals(aliases[1]) && serviceNameAlias4.equals(aliases[0])));
        // the value of the installed controller should be dummy manager
        assertSame(dummyManager, dummyController.getValue());
        // that should have the value of another service injected in the description property
        assertEquals("final description", dummyManager.getDescription());

        Future<ServiceController<?>> dummyServiceStop = testListener.expectServiceStop(serviceName);
        // stop dummy service
        dummyController.setMode(Mode.NEVER);
        assertController(dummyController, dummyServiceStop);
        // the description should have been uninjected
        assertNull(dummyManager.getDescription());
    }

    @Test
    public void addServiceWithDependencyInjectionAndOptionalDependencyOverride() throws Exception {
        final Location location = createLocation("File", -1, -1, null);

        Future<ServiceController<?>> anotherServiceStart = testListener.expectServiceStart(anotherServiceName);
        // install another service, with a string value
        ServiceBuilder<?> serviceBuilder = getServiceBuilder(serviceContainer.addService(anotherServiceName, new ValueService<String>(
                Values.immediateValue("final description")))).addListener(testListener);
        ServiceController<?> anotherController = assertController(anotherServiceName, serviceBuilder.install());
        assertController(anotherController, anotherServiceStart);

        // create dummy manager
        final DummyManager dummyManager = new DummyManager();
        // and dummy service, a plain value service whose valu is dummy manager
        final Service<DummyManager> service = new ValueService<DummyManager>(Values.immediateValue(dummyManager));
        // start installation of dummy service
        serviceBuilder = getServiceBuilder(serviceContainer.addService(serviceName, service));
        // with 5 aliases. Two of them are equal (alias1 and alias2), and the fifth is the same serviceName
        // used for the primary registration
        serviceBuilder.addAliases(serviceNameAlias1, serviceNameAlias2, serviceNameAlias3, serviceNameAlias4, serviceName);
        // add dependency on another service with injection... its value should be injected into the description property
        // of dummy manager
        serviceBuilder.addDependency(DependencyType.REQUIRED, anotherServiceName, String.class,
                new SetMethodInjector<String>(dummyManager, DummyManager.class, "setDescription", String.class));
        // add dependencies on another service (redundant given previous step), uninstalled service, and service
        // (notice that the last one is the service being installed itself)
        Collection<ServiceName> dependencies = new ArrayList<ServiceName>();
        dependencies.add(anotherServiceName);
        dependencies.add(uninstalledServiceName);
        dependencies.add(serviceName);
        serviceBuilder.addDependencies(dependencies);
        // attempt to override dependency on uninstalled service as an optional one
        dependencies = new ArrayList<ServiceName>();
        dependencies.add(uninstalledServiceName);
        dependencies.add(serviceName);
        serviceBuilder.addOptionalDependencies(dependencies);
        // add test listener
        serviceBuilder.addListener(testListener);
        // create a location for service
        serviceBuilder.setLocation(location);

        Future<ServiceController<?>> dummyServiceStart = testListener.expectServiceStart(serviceName);
        Future<ServiceController<?>> dummyServiceListenerAdded = testListener.expectListenerAdded(serviceName);
        // finally install dummy service
        ServiceController<?> dummyController = assertController(serviceName, serviceBuilder.install());
        assertController(dummyController, dummyServiceListenerAdded);
        // the value of the installed controller should be dummy manager
        assertEquals(location, dummyController.getLocation());
        ServiceName[] aliases = dummyController.getAliases();
        assertNotNull(aliases);
        assertEquals(3, aliases.length);
        assertTrue((serviceNameAlias1.equals(aliases[0]) && serviceNameAlias3.equals(aliases[1]) && serviceNameAlias4.equals(aliases[2])) ||
                (serviceNameAlias1.equals(aliases[0]) && serviceNameAlias3.equals(aliases[2]) && serviceNameAlias4.equals(aliases[1])) ||
                (serviceNameAlias1.equals(aliases[1]) && serviceNameAlias3.equals(aliases[0]) && serviceNameAlias4.equals(aliases[2])) ||
                (serviceNameAlias1.equals(aliases[1]) && serviceNameAlias3.equals(aliases[2]) && serviceNameAlias4.equals(aliases[0])) ||
                (serviceNameAlias1.equals(aliases[2]) && serviceNameAlias3.equals(aliases[0]) && serviceNameAlias4.equals(aliases[1])) ||
                (serviceNameAlias1.equals(aliases[2]) && serviceNameAlias3.equals(aliases[1]) && serviceNameAlias4.equals(aliases[0])));
        // the injection shouldn't have been performed
        assertNull(dummyManager.getDescription());

        Future<ServiceController<?>> uninstalledServiceStart = testListener.expectServiceStart(uninstalledServiceName);
        serviceBuilder = getServiceBuilder(serviceContainer.addService(uninstalledServiceName, Service.NULL)).addListener(testListener);
        ServiceController<?> uninstalledController = assertController(uninstalledServiceName, serviceBuilder.install());
        assertController(uninstalledController, uninstalledServiceStart);
        assertController(dummyController, dummyServiceStart);
        assertSame(dummyManager, dummyController.getValue());
        // the value of the installed controller should still be dummy manager
        assertSame(dummyManager, dummyController.getValue());
        // but now, dummy manager should finally have the value of another service injected in the description property
        assertEquals("final description", dummyManager.getDescription());

        Future<ServiceController<?>> uninstalledServiceRemoval = testListener.expectServiceRemoval(uninstalledServiceName);
        Future<ServiceController<?>> dummyServiceDependencyMissing = testListener.expectImmediateDependencyUninstall(serviceName);
        Future<ServiceController<?>> dummyServiceStop = testListener.expectServiceStop(serviceName);
        // remove uninstalled service
        uninstalledController.setMode(Mode.REMOVE);
        assertController(uninstalledController, uninstalledServiceRemoval);
        assertController(dummyController, dummyServiceStop);
        assertController(dummyController, dummyServiceDependencyMissing);
        // the description should have been uninjected
        assertNull(dummyManager.getDescription());
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Test
    public void addServiceWithDependencyInjections() throws Exception {
        Future<ServiceController<?>> serviceDescriptionStart = testListener.expectServiceStart(descriptionServiceName);
        // add service description, with a String value
        ServiceBuilder<?> serviceBuilder = getServiceBuilder(serviceContainer.addService(descriptionServiceName, new ValueService<String>(
                Values.immediateValue("final description")))).addListener(testListener);
        ServiceController<?> serviceDescriptionController = assertController(descriptionServiceName, serviceBuilder.install());
        assertController(serviceDescriptionController, serviceDescriptionStart);
        assertEquals("final description", serviceDescriptionController.getValue());

        // create dummy helper
        final DummyHelper dummyHelper = new DummyHelper();
        Future<ServiceController<?>> serviceHelperStart = testListener.expectServiceStart(helperServiceName);
        // install dummy helper service, a plain value service with dummy helper as its value
        serviceBuilder = getServiceBuilder(serviceContainer.addService(helperServiceName, new ValueService<DummyHelper>(Values.immediateValue(dummyHelper)))).
                addListener(testListener);
        ServiceController<?> serviceHelperController = assertController(helperServiceName, serviceBuilder.install());
        assertController(serviceHelperController, serviceHelperStart);
        assertSame(dummyHelper, serviceHelperController.getValue());

        // create dummy manager
        final DummyManager dummyManager = new DummyManager();
        // and dummy manager service, a plain value service with dummy manager as its value 
        final Service<DummyManager> service = new ValueService<DummyManager>(Values.immediateValue(dummyManager));
        // target for testing injection of dummyManager
        final InjectionTarget<DummyManager> managerInjected = new InjectionTarget<DummyManager>();
        // target for testing injection of a value
        final InjectionTarget<Integer> integerInjected = new InjectionTarget<Integer>();

        // start installation process
        serviceBuilder = getServiceBuilder(serviceContainer.addService(serviceName, service));
        serviceBuilder.setInitialMode(Mode.PASSIVE);
        // with optional dependency on service description, whereas the service description value should be injected
        // into the description property of dummy service
        serviceBuilder.addOptionalDependency(descriptionServiceName, String.class,
                new SetMethodInjector<String>(dummyManager, DummyManager.class, "setDescription", String.class));
        // add also an optional dependency on service helper, whereas the service value should also be injected
        // into dummy service
        serviceBuilder.addOptionalDependency(helperServiceName, new SetMethodInjector<Object>(Values.immediateValue(dummyManager),
                DummyManager.class.getMethod("setHelper", DummyHelper.class)));
        serviceBuilder.addDependency(serviceName);
        // inject manager into managerInjected
        serviceBuilder.addInjection(new FieldInjector(Values.immediateValue(managerInjected), InjectionTarget.injectedValueField));
        // inject 1000 into valueInjected
        serviceBuilder.addInjectionValue(new FieldInjector(Values.immediateValue(integerInjected), InjectionTarget.injectedValueField),
                Values.immediateValue(1000));
        // add service listener
        List<TestServiceListener> listeners = new ArrayList<TestServiceListener>();
        listeners.add(testListener);
        serviceBuilder.addListener(listeners);
        final Location location = createLocation("~/file.txt", 10, 0, createLocation("~/file.txt", 20, 5, null));
        // create a location
        serviceBuilder.setLocation(location);

        Future<ServiceController<?>> dummyServiceStart = testListener.expectServiceStart(serviceName);
        // finally install
        ServiceController<?> dummyController = assertController(serviceName, serviceBuilder.install());
        assertController(dummyController, dummyServiceStart);
        assertEquals(location, dummyController.getLocation());
        ServiceName[] aliases = dummyController.getAliases();
        assertTrue(aliases == null || aliases.length == 0);
        // expected value of dummy service is dummy manager
        assertSame(dummyManager, dummyController.getValue());
        // the description and helper properties should have been injected, as both optional dependencies were avialable
        // at the moment of installation
        assertEquals("final description", dummyManager.getDescription());
        assertSame(dummyHelper, dummyManager.getHelper());
        assertSame(dummyManager, managerInjected.injectedValue);
        assertEquals(Integer.valueOf(1000), integerInjected.injectedValue);

        Future<ServiceController<?>> dummyServiceStop = testListener.expectServiceStop(serviceName);
        // stop dummy service
        dummyController.setMode(Mode.NEVER);
        assertController(dummyController, dummyServiceStop);
        // both properties should have been uninjected
        assertNull(dummyManager.getDescription());
        assertNull(dummyManager.getHelper());
        assertNull(managerInjected.injectedValue);
        assertNull(integerInjected.injectedValue);

        Future<ServiceController<?>> serviceDescriptionRemoval = testListener.expectServiceRemoval(descriptionServiceName);
        // remove service description
        serviceDescriptionController.setMode(Mode.REMOVE);
        assertSame(serviceDescriptionController, serviceDescriptionRemoval.get());

        dummyServiceStart = testListener.expectServiceStart(serviceName);
        // re-enable dummy service 
        dummyController.setMode(Mode.ACTIVE);
        assertSame(dummyController, dummyServiceStart.get());
        // this time, the service description shouldn't have been injected, as the optional dependency was not installed
        assertNull(dummyManager.getDescription());
        assertSame(dummyHelper, dummyManager.getHelper());

        try {
            dummyController.addListener(testListener);
            fail ("IllegalArgumentException expected");
        } catch (IllegalArgumentException e) {}
    }

    @Test
    public void addServiceWithOptionalDependencyInjections() throws Exception {
        Future<ServiceController<?>> serviceDescriptionStart = testListener.expectServiceStart(descriptionServiceName);
        // install service description, a plain value service with a String value
        ServiceBuilder<?> serviceBuilder = getServiceBuilder(serviceContainer.addService(descriptionServiceName,
                new ValueService<String>(Values.immediateValue("final description")))).addListener(testListener);
        ServiceController<?> serviceDescriptionController = assertController(descriptionServiceName, serviceBuilder.install());
        assertController(serviceDescriptionController, serviceDescriptionStart);
        assertEquals("final description", serviceDescriptionController.getValue());

        // create dummy helper
        final DummyHelper dummyHelper = new DummyHelper();
        Future<ServiceController<?>> serviceHelperStart = testListener.expectServiceStart(helperServiceName);
        // install helper service, a plain value service with dummy helper as its value
        serviceBuilder = serviceContainer.addService(helperServiceName,
                new ValueService<DummyHelper>(Values.immediateValue(dummyHelper))).addListener(testListener);
        ServiceController<?> serviceHelperController = assertController(helperServiceName, serviceBuilder.install());
        assertController(serviceHelperController, serviceHelperStart);
        assertSame(dummyHelper, serviceHelperController.getValue());

        // create dummy manager
        final DummyManager dummyManager = new DummyManager();
        // and dummy service, a plain value service with dummy manager as its value
        Service<DummyManager> service = new ValueService<DummyManager>(Values.immediateValue(dummyManager));

        // setup dummy service installation
        serviceBuilder = getServiceBuilder(serviceContainer.addService(serviceName, service));
        // add optional dependency on description service, with an injection on description property
        serviceBuilder.addOptionalDependency(descriptionServiceName, String.class,
                new SetMethodInjector<String>(dummyManager, DummyManager.class, "setDescription", String.class));
        // add dependency on helper service, with an injection on helper property
        serviceBuilder.addDependency(helperServiceName, new SetMethodInjector<Object>(Values.immediateValue(dummyManager),
                DummyManager.class.getMethod("setHelper", DummyHelper.class)));
        serviceBuilder.addDependencies(serviceName);
        // add test listener
        List<TestServiceListener> listeners = new ArrayList<TestServiceListener>();
        listeners.add(testListener);
        serviceBuilder.addListener(listeners);

        Future<ServiceController<?>> dummyServiceStart = testListener.expectServiceStart(serviceName);
        // finally, install dummy service
        ServiceController<?> dummyController = assertController(serviceName, serviceBuilder.install());
        assertController(dummyController, dummyServiceStart);
        assertSame(dummyManager, dummyController.getValue());
        // description and helper properties should have been injected
        assertEquals("final description", dummyManager.getDescription());
        assertSame(dummyHelper, dummyManager.getHelper());

        Future<ServiceController<?>> dummyServiceStop = testListener.expectServiceStop(serviceName);
        dummyServiceStart = testListener.expectServiceStart(serviceName);
        Future<ServiceController<?>> serviceDescriptionRemoval = testListener.expectServiceRemoval(descriptionServiceName);
        // remove name description service
        serviceDescriptionController.setMode(Mode.REMOVE);
        assertController(serviceDescriptionController, serviceDescriptionRemoval);
        // dummy service should have been disconnected from optional dependency service description,
        // which causes it to start right after it stops
        assertOppositeNotifications(dummyController, dummyServiceStop, dummyServiceStart);
        // description should have been uninjected
        assertNull(dummyManager.getDescription());
        assertSame(dummyHelper, dummyManager.getHelper());
    }

    @Test
    public void editInstalledBuilder() throws Exception {
        Future<ServiceController<?>> serviceStart = testListener.expectServiceStart(serviceName);
        // add service
        ServiceBuilder<?> serviceBuilder = getServiceBuilder(serviceContainer.addService(serviceName, Service.NULL))
            .addListener(testListener);
        // install it
        ServiceController<?> serviceController = assertController(serviceName, serviceBuilder.install());
        assertController(serviceController, serviceStart);
        try {
            // edition of a already installed builder should fail
            serviceBuilder.addListener(testListener);
            fail("IllegalStateException expected");
        } catch (IllegalStateException e) {}
        // try to install again, IllegalStateException expected
        try {
            serviceBuilder.install();
            fail ("IllegalStateException expected");
        } catch (IllegalStateException e) {}
    }

    /**
     * Create and assert location as specified.
     * 
     * @param fileName     the name of the file
     * @param lineNumber   the number of line
     * @param columnNumber the number of column
     * @param parent       parent location if any
     * @return             the created location
     */
    private final Location createLocation(String fileName, int lineNumber, int columnNumber, Location parent) {
        Location location = new Location(fileName, lineNumber, columnNumber, parent);
        if (fileName == null) {
            assertNull(location.getFileName());
        } else {
            assertEquals(fileName, location.getFileName());
        }
        assertEquals(lineNumber, location.getLineNumber());
        assertEquals(columnNumber, location.getColumnNumber());
        assertEquals(location.hashCode(), location.hashCode());
        assertEquals(location, location);
        assertFalse(location.equals(null));
        assertFalse(location.equals((Location) null));
        assertFalse(location.equals("string"));
        assertTrue(location.equals((Object) location));
        assertFalse(location.equals(new Location(null, lineNumber, columnNumber -1, null)));
        assertFalse(location.equals(new Location(fileName == null? "": fileName + " ", lineNumber, columnNumber, null)));
        assertEquals(location, new Location(null, lineNumber, columnNumber, null));
        assertEquals(location, new Location(null, lineNumber, columnNumber, parent));
        assertFalse(location.equals((Object) new Location(fileName, lineNumber, columnNumber, new Location("", 0, 0, null))));
        assertFalse(location.equals(new Location(null, lineNumber + 1, columnNumber -1, null)));
        assertNotNull(location.toString());
        return location;
    }

    /**
     * A service to make {@code DummyManager} available in the container.
     * 
     * @author <a href="mailto:flavia.rainone@jboss.com">Flavia Rainone</a>
     */
    private static final class DummyService implements Service<DummyManager> {
        private DummyManager dummyManager;

        public DummyService(DummyManager dummyManager) {
            this.dummyManager = dummyManager;
        }

        @Override
        public DummyManager getValue() throws IllegalStateException {
            return dummyManager;
        }

        @Override
        public void start(StartContext context) throws StartException {
            dummyManager.initialize("");
        }

        @Override
        public void stop(StopContext context) {
            dummyManager.cleanup();
        }
    }

    /**
     * A dummy functionality that should be made available as a service in the service container.
     * 
     * @author <a href="mailto:flavia.rainone@jboss.com">Flavia Rainone</a>
     */
    public static final class DummyManager {
        private String description;
        private DummyHelper helper;

        public DummyHelper getHelper() {
            return helper;
        }

        public void setHelper(DummyHelper helper) {
            this.helper = helper;
        }

        public void initialize(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public void cleanup() {
            this.description = null;
        }
    }

    /**
     * A secondary helper that could be made available to dummy manager.
     *  
     * @author <a href="mailto:flavia.rainone@jboss.com">Flavia Rainone</a>
     */
    public static final class DummyHelper {}

    public static final class InjectionTarget<T> {
        public static final Field injectedValueField;
        
        static {
            try {
                injectedValueField = InjectionTarget.class.getDeclaredField("injectedValue");
            } catch (SecurityException e) {
                e.printStackTrace();
                throw new RuntimeException("Unexpected exception", e);
            } catch (NoSuchFieldException e) {
                e.printStackTrace();
                throw new RuntimeException("Unexpected exception", e);
            }
        }
        
        public T injectedValue;
    }
}
