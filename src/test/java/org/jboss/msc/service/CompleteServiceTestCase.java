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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.concurrent.Future;

import org.jboss.msc.inject.FieldInjector;
import org.jboss.msc.inject.SetMethodInjector;
import org.jboss.msc.service.ServiceBuilder.DependencyType;
import org.jboss.msc.service.ServiceController.Mode;
import org.jboss.msc.service.ServiceController.State;
import org.jboss.msc.util.TestServiceListener;
import org.jboss.msc.value.Value;
import org.jboss.msc.value.Values;
import org.junit.Test;

/**
 * Tests scenarios involving services with non-null values, optional dependencies, and injections.
 * <p>
 * Plus, in this test case the container is not shutdown on tear down. This enables the automatic shutdown process
 * triggered by a shutdown hook defined in {@link ServiceContainerImpl}. 
 * 
 * @author <a href="mailto:flavia.rainone@jboss.com">Flavia Rainone</a>
 */
public class CompleteServiceTestCase extends AbstractServiceTest {
    private static final ServiceName serviceNameA = ServiceName.of("service", "A");
    private static final ServiceName serviceNameB = ServiceName.of("service", "B");
    private static final ServiceName serviceNameD = ServiceName.of("service", "D");
    private static final ServiceName fooServiceName = ServiceName.of("foo", "service");

    @Override
    public void tearDown() {
        // do not shutdown the container
    }

    @Test
    public void test1() throws Exception {
        final TestServiceListener testListener = new TestServiceListener();
        final ServiceA serviceA = new ServiceA();
        final ServiceB serviceB = new ServiceB();
        final ServiceBWrapper serviceBWrapper = new ServiceBWrapper();
        final ServiceC serviceC = new ServiceC();
        final ServiceD serviceD = new ServiceD();

        final Future<ServiceController<?>> serviceDStart = testListener.expectServiceStart(serviceNameD);
        final ServiceController<?> serviceDController = serviceContainer.addService(serviceNameD, serviceD)
            .addInjection(new SetMethodInjector<SomeService>(Values.immediateValue(serviceD), getMethod(ServiceD.class,"setSomeService", SomeService.class)), serviceC)
            .addInjection(new SetMethodInjector<ServiceD>(Values.immediateValue(serviceD), ServiceD.class.getDeclaredMethod("setService", ServiceD.class)))
            .addListener(testListener)
            .install();
        assertController(serviceNameD, serviceDController);
        assertController(serviceDController, serviceDStart);
        assertSame(serviceD, serviceDController.getValue());
        assertSame(serviceC, serviceD.service);
        assertSame(serviceD, serviceD.getService());

        final Future<ServiceController<?>> serviceBStart = testListener.expectServiceStart(serviceNameB);
        final ServiceController<?> serviceBController = serviceContainer.addService(serviceNameB, serviceBWrapper)
        .addInjection(
                new SetMethodInjector<ServiceB>(Values.immediateValue(serviceBWrapper), getMethod(ServiceBWrapper.class, "setValue", ServiceB.class)), serviceB)
                .addListener(testListener)
        .install();
        assertController(serviceNameB, serviceBController);
        assertController(serviceBController, serviceBStart);
        assertSame(serviceB, serviceBController.getValue());

        final Value<ServiceA> serviceAValue = Values.immediateValue(serviceA);
        final Future<ServiceController<?>> serviceAStart = testListener.expectServiceStart(serviceNameA);
        final ServiceController<?> serviceAController = serviceContainer.addService(serviceNameA, serviceA)
                .addInjection(new FieldInjector<String>(serviceAValue, getField(ServiceA.class, "description")), "serviceA")
                .addInjection(new FieldInjector<Integer>(serviceAValue, getField(ServiceA.class, "initialParameter")), 2215)
                .addDependency(serviceNameB,
                        new FieldInjector<Object>(serviceAValue, getField(ServiceA.class, "serviceB")))
                .addDependency(serviceNameD, 
                new SetMethodInjector<Object>(Values.immediateValue(serviceA), getMethod(ServiceA.class, "setServiceD", ServiceD.class)))
                .addListener(testListener)
                        .install();
        assertController(serviceNameA, serviceAController);
        assertController(serviceAController, serviceAStart);
        assertSame(serviceA, serviceAController.getValue()); 
        assertEquals("serviceA", serviceA.description);
        assertEquals(2215, serviceA.initialParameter);
        assertSame(serviceB, serviceA.serviceB);
        assertSame(serviceC, serviceA.serviceC);

        final Future<ServiceController<?>> fooStart = testListener.expectServiceStart(fooServiceName);
        ServiceController<?> fooController = serviceContainer.addService(fooServiceName, Service.NULL)
            .addDependency(serviceNameB).addListener(testListener).install();
        assertController(fooServiceName, fooController);
        assertController(fooController, fooStart);

        final Future<ServiceController<?>> serviceAStop = testListener.expectServiceStop(serviceNameA);
        serviceAController.setMode(Mode.NEVER);
        assertController(serviceAController, serviceAStop);
        assertSame(serviceA, serviceAController.getValue());
        assertNull(serviceA.description);
        assertEquals(0, serviceA.initialParameter);;
        assertNull(serviceA.serviceB);
        assertNull(serviceA.serviceC);

        serviceBController.setMode(Mode.ON_DEMAND);
        // b doesn't go down, as there is still a dependent running (foo service)
        assertSame(State.UP, serviceBController.getState());

        final Future<ServiceController<?>> fooServiceStop = testListener.expectServiceStop(fooServiceName);
        final Future<ServiceController<?>> serviceBStop = testListener.expectServiceStop(serviceNameB);
        // now b can go down
        fooController.setMode(Mode.NEVER);
        assertController(fooController, fooServiceStop);
        assertController(serviceBController, serviceBStop);
        try {
            serviceBController.getValue();
            fail ("IllegalStateException expected");
        } catch (IllegalStateException e) {}
    }

    @Test
    public void test2() throws Exception {
        final TestServiceListener testListener = new TestServiceListener();
        final ServiceA serviceA = new ServiceA();
        final ServiceB serviceB = new ServiceB();
        final ServiceBWrapper serviceBWrapper = new ServiceBWrapper();
        final ServiceC serviceC = new ServiceC();
        final ServiceD serviceD = new ServiceD();
        final ServiceE serviceE = new ServiceE();

        // install service D, with service C injected
        final Future<ServiceController<?>> serviceDStart = testListener.expectServiceStart(serviceNameD);
        final ServiceController<?> serviceDController = serviceContainer.addService(serviceNameD, serviceD)
            .addInjection(new SetMethodInjector<SomeService>(Values.immediateValue(serviceD), getMethod(ServiceD.class,"setSomeService", SomeService.class)), serviceC)
            .addListener(testListener)
            .install();
        assertController(serviceNameD, serviceDController);
        assertController(serviceDController, serviceDStart);
        assertSame(serviceC, serviceD.service);

        // install serviceE as foo service, with service D injected in it, and with the listener below
        final Future<ServiceController<?>> serviceEStart = testListener.expectServiceStart(fooServiceName);
        final ServiceController<?> serviceEController = serviceContainer.addService(fooServiceName, serviceE)
            .addDependency(serviceNameD, ServiceD.class, new SetMethodInjector<ServiceD>(
                    Values.immediateValue(serviceE), getMethod(ServiceE.class, "initialize", ServiceD.class)))
            .addListener(testListener)
            .addListener(new AbstractServiceListener<Void>() {
                public void transition(final ServiceController<? extends Void> controller, final ServiceController.Transition transition) {
                    if (transition.getAfter() == ServiceController.Substate.REMOVED) {
                        // when service E is removed, install service A as foo service
                        final Value<ServiceA> serviceAValue = Values.immediateValue(serviceA);
                        try {
                            // install service A as foo service, with a description injected, an initial parameter
                            // plus service D and service B injected
                            serviceContainer.addService(fooServiceName, serviceA).addInjection(new FieldInjector<String>(serviceAValue, getField(ServiceA.class, "description")), "foo")
                                    .addInjection(new FieldInjector<Integer>(serviceAValue, getField(ServiceA.class, "initialParameter")), 2218)
                                    .addDependency(serviceNameB, new FieldInjector<Object>(serviceAValue, getField(ServiceA.class, "serviceB")))
                                        .addDependency(serviceNameD, new SetMethodInjector<Object>(Values.immediateValue(serviceA), getMethod(ServiceA.class, "setServiceD", ServiceD.class)))
                                            .addListener(testListener)
                                            .install();
                        } catch (IllegalArgumentException e) {
                            throw new RuntimeException(e);
                        } catch (ServiceRegistryException e) {
                            throw new RuntimeException(e);
                        } catch (NoSuchFieldException e) {
                            throw new RuntimeException(e);
                        }
                    }
                 }
             }).install();
        assertController(fooServiceName, serviceEController);
        assertController(serviceEController, serviceEStart);
        assertSame(serviceD, serviceE.serviceD);

        final Future<ServiceController<?>> serviceEStop = testListener.expectServiceStop(fooServiceName);
        // disable service E
        serviceEController.setMode(Mode.NEVER);
        assertController(serviceEController, serviceEStop);

        // remove service E, thus triggering the listener above
        final Future<ServiceController<?>> serviceERemoval = testListener.expectServiceRemoval(fooServiceName);
        final Future<ServiceController<?>> serviceAInstalledAsServiceE = testListener.expectListenerAdded(fooServiceName);
        final Future<ServiceController<?>> serviceADependencyMissing = testListener.expectImmediateDependencyUnavailable(fooServiceName);
        serviceEController.setMode(Mode.REMOVE);
        assertController(serviceEController, serviceERemoval);
        assertNull(serviceE.serviceD);
        final ServiceController<?> serviceAController = assertController(fooServiceName, serviceAInstalledAsServiceE);
        assertController(serviceAController, serviceADependencyMissing);

        // install service B, which will allow service A to start
        final Future<ServiceController<?>> serviceBStart = testListener.expectServiceStart(serviceNameB);
        final Future<ServiceController<?>> serviceAStart = testListener.expectServiceStart(fooServiceName);
        serviceContainer.addService(serviceNameB, serviceBWrapper)
            .addInjection(
                new SetMethodInjector<ServiceB>(Values.immediateValue(serviceBWrapper), getMethod(ServiceBWrapper.class, "setValue", ServiceB.class)), serviceB)
                .addListener(testListener)
        .install();
        final ServiceController<?> serviceBController = assertController(serviceNameB, serviceBStart);
        assertSame(serviceB, serviceBController.getValue());
        assertController(serviceAController, serviceAStart);
        assertSame(serviceA, serviceAController.getValue());
        assertEquals("foo", serviceA.description);
        assertEquals(2218, serviceA.initialParameter);
        assertSame(serviceB, serviceA.serviceB);
        assertNotNull(serviceA.serviceC);
    }

    @Test
    public void test3() throws Exception {
        final TestServiceListener testListener = new TestServiceListener();
        final ServiceA serviceA = new ServiceA();
        final ServiceB serviceB = new ServiceB();
        final ServiceC serviceC = new ServiceC();
        final ServiceD serviceD = new ServiceD();
        final ServiceE serviceE = new ServiceE();
        final ServiceBWrapper serviceBWrapper = new ServiceBWrapper();

        final Future<ServiceController<?>> serviceAStart = testListener.expectServiceStart(fooServiceName);
        final Future<ServiceController<?>> serviceBStart = testListener.expectServiceStart(serviceNameB);
        final Future<ServiceController<?>> serviceDStart = testListener.expectServiceStart(serviceNameD);

        // install service B, in wrapper
        final ServiceController<?> serviceBController = serviceContainer.addService(serviceNameB, serviceBWrapper)
            .addInjection(
                new SetMethodInjector<ServiceB>(Values.immediateValue(serviceBWrapper), getMethod(ServiceBWrapper.class, "setValue", ServiceB.class)), serviceB)
                .addListener(testListener)
                .install();
        // install service D, and inject service C in it
        final ServiceController<?> serviceDController = serviceContainer.addService(serviceNameD, serviceD)
            .addInjection(new SetMethodInjector<SomeService>(Values.immediateValue(serviceD), getMethod(ServiceD.class,"setSomeService", SomeService.class)), serviceC)
            .addListener(testListener)
            .install();
        // install service A, inject description, initialParameter, service B and service D
        final Value<ServiceA> serviceAValue = Values.immediateValue(serviceA);
        final ServiceController<?> serviceAController = serviceContainer.addService(fooServiceName, serviceA).addInjection(new FieldInjector<String>(serviceAValue, getField(ServiceA.class, "description")), "foo service_")
                .addInjection(new FieldInjector<Integer>(serviceAValue, getField(ServiceA.class, "initialParameter")), 137)
                .addDependency(serviceNameB, new FieldInjector<Object>(serviceAValue, getField(ServiceA.class, "serviceB")))
                .addDependency(serviceNameD, new SetMethodInjector<Object>(Values.immediateValue(serviceA), getMethod(ServiceA.class, "setServiceD", ServiceD.class)))
                .addListener(testListener)
                .addListener(new AbstractServiceListener<ServiceA>() {
                    @Override
                    public void immediateDependencyUnavailable(final ServiceController<? extends ServiceA> controller) {
                        // remove foo service as soon as its dep is uninstalled
                        controller.setMode(Mode.REMOVE);
                    }

                    @Override
                    public void transition(final ServiceController<? extends ServiceA> controller, final ServiceController.Transition transition) {
                        if (transition.getAfter() == ServiceController.Substate.REMOVED) {
                            try {
                                // replace the removed service by service E
                                serviceContainer.addService(fooServiceName, serviceE).addDependency(DependencyType.OPTIONAL, serviceNameD, ServiceD.class, new SetMethodInjector<ServiceD>(Values.immediateValue(serviceE), getMethod(ServiceE.class, "initialize", ServiceD.class))).addListener(testListener).install();
                            } catch (IllegalArgumentException e) {
                                throw new RuntimeException(e);
                            } catch (ServiceRegistryException e) {
                                throw new RuntimeException(e);
                            }
                        }
                    }
             }).install();

        assertController(fooServiceName, serviceAController);
        assertController(serviceAController, serviceAStart);
        assertController(serviceNameB, serviceBController);
        assertController(serviceBController, serviceBStart);
        assertController(serviceNameD, serviceDController);
        assertController(serviceDController, serviceDStart);
        assertSame(serviceA, serviceAController.getValue());
        assertEquals("foo service_", serviceA.description);
        assertEquals(137, serviceA.initialParameter);
        assertSame(serviceB, serviceA.serviceB);
        assertSame(serviceC, serviceA.serviceC);

        // stop service B
        final Future<ServiceController<?>> serviceBStop = testListener.expectServiceStop(serviceNameB);
        final Future<ServiceController<?>> serviceAStop = testListener.expectServiceStop(fooServiceName);
        final Future<ServiceController<?>> serviceARemoval = testListener.expectServiceRemoval(fooServiceName);
        Future<ServiceController<?>> serviceEStart = testListener.expectServiceStart(fooServiceName);
        serviceBController.setMode(Mode.NEVER);
        assertController(serviceBController, serviceBStop);
        assertController(serviceAController, serviceAStop);
        assertController(serviceAController, serviceARemoval);
        assertSame(serviceA, serviceAController.getValue());
        assertNull(serviceA.description);
        assertEquals(0, serviceA.initialParameter);
        assertNull(serviceA.serviceB);
        assertNull(serviceA.serviceC);
        ServiceController<?> serviceEController = assertController(fooServiceName, serviceEStart);
        assertNull(serviceEController.getValue());
        assertSame(serviceD, serviceE.serviceD);

        final Future<ServiceController<?>> serviceBRemoval = testListener.expectServiceRemoval(serviceNameB);
        serviceBController.setMode(Mode.REMOVE);
        assertController(serviceBController, serviceBRemoval);

        final Future<ServiceController<?>> serviceDStop = testListener.expectServiceStop(serviceNameD);
        final Future<ServiceController<?>> serviceEStop = testListener.expectServiceStop(fooServiceName);
        serviceEStart = testListener.expectServiceStart(fooServiceName);
        serviceDController.setMode(Mode.REMOVE);
        assertController(serviceDController, serviceDStop);
        assertOppositeNotifications(serviceEController, serviceEStop, serviceEStart);
        assertNull(serviceE.serviceD);
    }

    @Test
    public void test4() throws Exception {
        final TestServiceListener testListener = new TestServiceListener();
        final ServiceB serviceB = new ServiceB();
        final ServiceBWrapper serviceBWrapper = new ServiceBWrapper();
        final ServiceC serviceC = new ServiceC();
        final ServiceD serviceD = new ServiceD();

        final Future<ServiceController<?>> serviceDStart = testListener.expectServiceStart(serviceNameD);
        final ServiceController<?> serviceDController = serviceContainer.addService(serviceNameD, serviceD)
            .addInjection(new SetMethodInjector<SomeService>(Values.immediateValue(serviceD), getMethod(ServiceD.class,"setSomeService", SomeService.class)), serviceC)
            .addListener(testListener)
            .install();
        assertController(serviceNameD, serviceDController);
        assertController(serviceDController, serviceDStart);
        assertSame(serviceD, serviceDController.getValue());
        assertSame(serviceC, serviceD.service);

        final Future<ServiceController<?>> serviceBStart = testListener.expectServiceStart(serviceNameB);
        final ServiceController<?> serviceBController = serviceContainer.addService(serviceNameB, serviceBWrapper)
        .addInjection(
                new SetMethodInjector<ServiceB>(Values.immediateValue(serviceBWrapper), getMethod(ServiceBWrapper.class, "setValue", ServiceB.class)), serviceB)
                .addListener(testListener)
        .install();
        assertController(serviceNameB, serviceBController);
        assertController(serviceBController, serviceBStart);
        assertSame(serviceB, serviceBController.getValue());
    }

    @Test
    public void test5() throws Exception {
        final TestServiceListener testListener = new TestServiceListener();
        final ServiceC serviceC = new ServiceC();
        final ServiceD serviceD = new ServiceD(true);

        // install service D, with service C injected
        final Future<ServiceController<?>> serviceDStart = testListener.expectServiceStart(serviceNameD);
        final ServiceController<?> serviceDController = serviceContainer.addService(serviceNameD, serviceD)
            .addInjection(new SetMethodInjector<SomeService>(Values.immediateValue(serviceD), getMethod(ServiceD.class,"setSomeService", SomeService.class)), serviceC)
            .addListener(testListener)
            .install();
        assertController(serviceNameD, serviceDController);
        assertController(serviceDController, serviceDStart);
        assertSame(serviceC, serviceD.service);

        // serviceD stop will fail, but this will be ignored
        final Future<ServiceController<?>> serviceDStop = testListener.expectServiceStop(serviceNameD);
        serviceDController.setMode(Mode.NEVER);
        assertController(serviceDController, serviceDStop);
        assertNull(serviceD.service);
    }

    @Test
    public void testServiceWithInvalidInjection() throws Exception {
        final TestServiceListener testListener = new TestServiceListener();
        final ServiceC serviceC = new ServiceC();
        final ServiceD serviceD = new ServiceD(NullPointerException.class);

        // install service D, with service C injected, but with a typo
        final Future<StartException> serviceFailure = testListener.expectServiceFailure(serviceNameD);
        final ServiceController<?> serviceDController = serviceContainer.addService(serviceNameD, serviceD)
            .addInjection(new SetMethodInjector<SomeService>(Values.immediateValue(serviceD), getMethod(ServiceD.class,"setSomeService", SomeService.class)), serviceC)
            .addInjection(new SetMethodInjector(serviceD, ServiceD.class.getDeclaredMethod("setInjectionException", Class.class)), IllegalStateException.class)
            .addListener(testListener)
            .install();
        assertController(serviceNameD, serviceDController);
        assertFailure(serviceDController, serviceFailure);
        assertNull(serviceD.service);

        serviceD.setInjectionException(null);
        final Future<ServiceController<?>> serviceWontStart = testListener.expectServiceWontStart(serviceNameD);
        serviceDController.setMode(Mode.NEVER);
        assertController(serviceDController, serviceWontStart);

        final Future<ServiceController<?>> serviceStart = testListener.expectServiceStart(serviceNameD); 
        serviceDController.setMode(Mode.ACTIVE);
        assertController(serviceDController, serviceStart);
        assertSame(IllegalStateException.class, serviceD.getInjectionException());
        assertSame(serviceC, serviceD.getSomeService());

        final Future<ServiceController<?>> serviceStop = testListener.expectServiceStop(serviceNameD);
        serviceDController.setMode(Mode.NEVER);
        assertController(serviceDController, serviceStop);
        assertNull(serviceD.getInjectionException());
        assertTrue(serviceD.getSomeService() == null || serviceD.getSomeService() == serviceC);
    }

    @Test
    public void testServiceWithInvalidUninjection() throws Exception {
        final TestServiceListener testListener = new TestServiceListener();
        final ServiceC serviceC = new ServiceC();
        final ServiceD serviceD = new ServiceD();

        // install service D, with service C injected
        final Future<ServiceController<?>> serviceStart = testListener.expectServiceStart(serviceNameD);
        final ServiceController<?> serviceDController = serviceContainer.addService(serviceNameD, serviceD)
            .addInjection(new SetMethodInjector<SomeService>(Values.immediateValue(serviceD), getMethod(ServiceD.class,"setSomeService", SomeService.class)), serviceC)
            .addListener(testListener)
            .install();
        assertController(serviceNameD, serviceDController);
        assertController(serviceDController, serviceStart);
        assertSame(serviceC, serviceD.service);

        serviceD.setInjectionException(UnknownError.class);
        final Future<ServiceController<?>> serviceStop = testListener.expectServiceStop(serviceNameD);
        serviceDController.setMode(Mode.NEVER);
        assertController(serviceDController, serviceStop);
        assertSame(serviceC, serviceD.service);
    }

    @Test
    public void temporaryServiceValue() throws Throwable {
        final TestServiceListener testListener = new TestServiceListener();
        final ServiceValue<ServiceA> serviceValue = new ServiceValue<ServiceA>(new ServiceA());
        final Future<ServiceController<?>> serviceAStart = testListener.expectServiceStart(serviceNameA);
        serviceContainer.addServiceValue(serviceNameA, serviceValue).addListener(testListener).install();
        final ServiceController<?> serviceAController = assertController(serviceNameA, serviceAStart);

        serviceValue.setService(null);
        final Future<ServiceController<?>> serviceAStop = testListener.expectServiceStop(serviceNameA);
        serviceAController.setMode(Mode.NEVER);
        assertController(serviceAController, serviceAStop);
    }

    public static class ServiceA implements Service<ServiceA> {
        public String description;
        public int initialParameter;
        public ServiceB serviceB;
        public ServiceC serviceC;

        @Override
        public ServiceA getValue() throws IllegalStateException {
            return this;
        }

        @Override
        public void start(StartContext context) throws StartException {
            assertContextClassLoader(this.getClass().getClassLoader());
            assertTrue(context.getElapsedTime() > 0L);
        }

        @Override
        public void stop(StopContext context) {
            assertContextClassLoader(this.getClass().getClassLoader());
            assertTrue(context.getElapsedTime() > 0L);
        }

        public void setServiceD(ServiceD serviceD) {
            if (serviceD == null) {
                serviceC = null;
            }
            else {
                serviceC = (ServiceC) serviceD.getSomeService();
            }
        }
    }

    public static class ServiceB {
        boolean startedService;

        public void start(){
            assertContextClassLoader(this.getClass().getClassLoader());
            startedService = true;
        }

        public void stop() {
            assertContextClassLoader(this.getClass().getClassLoader());
            startedService = false;
        }
        
        public boolean isServiceStarted() {
            return startedService;
        }
    }

    public static class ServiceBWrapper implements Service<ServiceB> {

        private ServiceB serviceB;

        public void setValue(ServiceB serviceB) {
            this.serviceB = serviceB;
        }

        @Override
        public ServiceB getValue() throws IllegalStateException {
            if (serviceB == null) {
                throw new IllegalStateException();
            }
            return serviceB;
        }

        @Override
        public void start(StartContext context) throws StartException {
            assertContextClassLoader(this.getClass().getClassLoader());
            serviceB.start();
            assertTrue(context.getElapsedTime() > 0L);
        }

        @Override
        public void stop(StopContext context) {
            assertContextClassLoader(this.getClass().getClassLoader());
            serviceB.stop();
            assertTrue(context.getElapsedTime() > 0L);
        }
        
    }

    public static class ServiceC implements SomeService {}

    public static interface SomeService {}

    public static class ServiceD implements Service<ServiceD> {
        private SomeService service;
        private boolean failToStop = false;
        private Class<? extends Throwable> injectionException = null;
        private ServiceD serviceD; // should contain this 

        public ServiceD() {}

        public ServiceD(boolean failToStop) {
            this.failToStop = failToStop;
        }

        public ServiceD(Class<? extends Throwable> injectionException) {
            this.injectionException = injectionException;
        }

        public Class<? extends Throwable> getInjectionException() {
            return injectionException;
        }

        public void setInjectionException(Class<? extends Throwable> injectionException) {
            this.injectionException = injectionException;
        }

        /**
         * @param serviceC the serviceC to set
         */
        public void setSomeService(SomeService service) throws Throwable {
            if (injectionException != null) {
                try {
                    throw injectionException.newInstance();
                } catch (InstantiationException e) {
                    e.printStackTrace();
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
            this.service = service;
        }

        /**
         * @return the serviceC
         */
        public SomeService getSomeService() {
            return service;
        }

        @Override
        public ServiceD getValue() throws IllegalStateException {
            return this;
        }

        public void setService(ServiceD serviceD) {
            this.serviceD = serviceD;
        }

        public ServiceD getService() {
            return serviceD;
        }

        @Override
        public void start(StartContext context) throws StartException {
            assertContextClassLoader(this.getClass().getClassLoader());
            assertTrue(context.getElapsedTime() > 0L);
        }

        @Override
        public void stop(StopContext context) {
            assertContextClassLoader(this.getClass().getClassLoader());
            if (failToStop) {
                throw new RuntimeException();
            }
            assertTrue(context.getElapsedTime() > 0L);
        }
    }

    public static class ServiceE implements Service<Void> {
        public ServiceD serviceD;

        public void initialize(ServiceD serviceD) {
            this.serviceD = serviceD;
        }

        @Override
        public Void getValue() throws IllegalStateException {
            return null;
        }

        @Override
        public void start(StartContext context) throws StartException {
            assertContextClassLoader(this.getClass().getClassLoader());
            assertTrue(context.getElapsedTime() > 0L);
        }

        @Override
        public void stop(StopContext context) {
            assertContextClassLoader(this.getClass().getClassLoader());
            assertTrue(context.getElapsedTime() > 0L);
        }
    }

    public static class ServiceValue<T> implements Value<Service<T>> {
        private Service<T> service;

        public ServiceValue (Service<T> service) {
            this.service = service;
        }

        @Override
        public Service<T> getValue() throws IllegalStateException, IllegalArgumentException {
            return service;
        }

        public void setService(Service<T> service) {
            this.service = service;
        }
        
    }

    private static Field getField(final Class<?> clazz, final String name) throws NoSuchFieldException {
        return clazz.getField(name);
    }

    private static Method getMethod(final Class<?> clazz, final String methodName, final Class<?> paramType) {
        final Method method;
        try {
            method = clazz.getMethod(methodName, paramType);
        } catch (NoSuchMethodException e) {
            throw new IllegalArgumentException("No such method found '" +  clazz + "." + methodName + "(" +
                    paramType + ")'", e);
        }
        return method;
    }
}
