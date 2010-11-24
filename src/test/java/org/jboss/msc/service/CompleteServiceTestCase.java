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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;

import java.util.concurrent.Future;

import org.jboss.msc.inject.FieldInjector;
import org.jboss.msc.inject.SetMethodInjector;
import org.jboss.msc.service.ServiceController.Mode;
import org.jboss.msc.util.TestServiceListener;
import org.jboss.msc.value.LookupFieldValue;
import org.jboss.msc.value.Value;
import org.jboss.msc.value.Values;
import org.junit.Test;

/**
 * Tests scenarios involving services with non-null values, optional dependencies, and injections.
 * 
 * @author <a href="mailto:flavia.rainone@jboss.com">Flavia Rainone</a>
 */
public class CompleteServiceTestCase extends AbstractServiceTest {
    private static final ServiceName serviceNameA = ServiceName.of("service", "A");
    private static final ServiceName serviceNameB = ServiceName.of("service", "B");
    private static final ServiceName serviceNameD = ServiceName.of("service", "D");
    private static final ServiceName fooServiceName = ServiceName.of("foo", "service");

    @Test
    public void test1() throws Exception {
        final TestServiceListener testListener = new TestServiceListener();
        final ServiceA serviceA = new ServiceA();
        final ServiceB serviceB = new ServiceB();
        final ServiceBWrapper serviceBWrapper = new ServiceBWrapper();
        final ServiceC serviceC = new ServiceC();
        final ServiceD serviceD = new ServiceD();

        final Future<ServiceController<?>> serviceDStart = testListener.expectServiceStart(serviceNameD);
        serviceContainer.addService(serviceNameD, serviceD)
            .addInjection(new SetMethodInjector<SomeService>(Values.immediateValue(serviceD), ServiceD.class,"setSomeService", SomeService.class), serviceC)
            .addListener(testListener)
            .install();
        final ServiceController<?> serviceDController = assertController(serviceNameD, serviceDStart);
        assertSame(serviceD, serviceDController.getValue());
        assertSame(serviceC, serviceD.service);

        final Future<ServiceController<?>> serviceBStart = testListener.expectServiceStart(serviceNameB);
        serviceContainer.addService(serviceNameB, serviceBWrapper)
        .addInjection(
                new SetMethodInjector<ServiceB>(serviceBWrapper, ServiceBWrapper.class, "setValue", ServiceB.class), serviceB)
                .addListener(testListener)
        .install();
        final ServiceController<?> serviceBController = assertController(serviceNameB, serviceBStart);
        assertSame(serviceB, serviceBController.getValue());

        final Value<ServiceA> serviceAValue = Values.immediateValue(serviceA);
        final Future<ServiceController<?>> serviceAStart = testListener.expectServiceStart(serviceNameA);
        serviceContainer.addService(serviceNameA, serviceA).addInjection(new FieldInjector<String>(serviceAValue,
                new LookupFieldValue(Values.<Class<?>>immediateValue(ServiceA.class), "description")), "serviceA")
                .addInjection(new FieldInjector<Integer>(serviceAValue,
                        new LookupFieldValue(Values.<Class<?>>immediateValue(ServiceA.class), "initialParameter")), 2215)
                .addDependency(serviceNameB,
                        new FieldInjector<Object>(serviceAValue, new LookupFieldValue(Values.<Class<?>>immediateValue(ServiceA.class), "serviceB")))
                .addDependency(serviceNameD, 
                new SetMethodInjector<Object>(Values.immediateValue(serviceA), ServiceA.class, "setServiceD", ServiceD.class))
                .addListener(testListener)
                        .install();
        final ServiceController<?> serviceAController = assertController(serviceNameA, serviceAStart);
        assertSame(serviceA, serviceAController.getValue()); 
        assertEquals("serviceA", serviceA.description);
        assertEquals(2215, serviceA.initialParameter);
        assertSame(serviceB, serviceA.serviceB);
        assertSame(serviceC, serviceA.serviceC);

        final Future<ServiceController<?>> serviceAStop = testListener.expectServiceStop(serviceNameA);
        serviceAController.setMode(Mode.NEVER);
        assertController(serviceAController, serviceAStop);
        assertSame(serviceA, serviceAController.getValue());
        assertNull(serviceA.description);
        assertEquals(2215, serviceA.initialParameter);;
        assertNull(serviceA.serviceB);
        assertNull(serviceA.serviceC);

        final Future<ServiceController<?>> serviceBStop = testListener.expectServiceStop(serviceNameB);
        serviceBController.setMode(Mode.ON_DEMAND);
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
        serviceContainer.addService(serviceNameD, serviceD)
            .addInjection(new SetMethodInjector<SomeService>(Values.immediateValue(serviceD), ServiceD.class,"setSomeService", SomeService.class), serviceC)
            .addListener(testListener)
            .install();
        assertNotNull(serviceDStart.get());
        assertSame(serviceC, serviceD.service);

        // install serviceE as foo service, with service D injected in it, and with the listener below
        final Future<ServiceController<?>> serviceEStart = testListener.expectServiceStart(fooServiceName);
        serviceContainer.addService(fooServiceName, serviceE)
            .addDependency(serviceNameD, ServiceD.class, new SetMethodInjector<ServiceD>(
                    Values.immediateValue(serviceE), ServiceE.class, "initialize", ServiceD.class))
             .addListener(testListener)
             .addListener(new AbstractServiceListener<Void>() {
                 @Override
                 public void serviceRemoved(final ServiceController<? extends Void> controller) {
                     // when service E is removed, install service A as foo service
                     final Value<ServiceA> serviceAValue = Values.immediateValue(serviceA);
                     try {
                         // install service A as foo service, with a description injected, an initial parameter
                         // plus service D and service B injected
                        serviceContainer.addService(fooServiceName, serviceA).addInjection(new FieldInjector<String>(serviceAValue,
                                 new LookupFieldValue(Values.<Class<?>>immediateValue(ServiceA.class), "description")), "foo")
                                 .addInjection(new FieldInjector<Integer>(serviceAValue,
                                         new LookupFieldValue(Values.<Class<?>>immediateValue(ServiceA.class), "initialParameter")), 2218)
                                 .addDependency(serviceNameB,
                                         new FieldInjector<Object>(serviceAValue, new LookupFieldValue(Values.<Class<?>>immediateValue(ServiceA.class), "serviceB")))
                                 .addDependency(serviceNameD, 
                                 new SetMethodInjector<Object>(Values.immediateValue(serviceA), ServiceA.class, "setServiceD", ServiceD.class))
                                 .addListener(testListener)
                                         .install();
                    } catch (IllegalArgumentException e) {
                        throw new RuntimeException(e);
                    } catch (ServiceRegistryException e) {
                        throw new RuntimeException(e);
                    }
                 }
             }).install();
        final ServiceController<?> serviceControllerE = assertController(fooServiceName, serviceEStart);
        assertSame(serviceD, serviceE.serviceD);

        final Future<ServiceController<?>> serviceEStop = testListener.expectServiceStop(fooServiceName);
        // disable service E
        serviceControllerE.setMode(Mode.NEVER);
        assertController(serviceControllerE, serviceEStop);

        // remove service E, thus triggering the listener above
        final Future<ServiceController<?>> serviceERemoval = testListener.expectServiceRemoval(fooServiceName);
        final Future<ServiceController<?>> serviceAInstalledAsServiceE = testListener.expectListenerAdded(fooServiceName);
        final Future<ServiceController<?>> serviceADependencyMissing = testListener.expectDependencyUninstall(fooServiceName);
        serviceControllerE.setMode(Mode.REMOVE);
        assertController(serviceControllerE, serviceERemoval);
        assertNull(serviceE.serviceD);
        final ServiceController<?> serviceControllerA = assertController(fooServiceName, serviceAInstalledAsServiceE);
        assertController(serviceControllerA, serviceADependencyMissing);

        // install service B, which will allow service A to start
        final Future<ServiceController<?>> serviceBStart = testListener.expectServiceStart(serviceNameB);
        final Future<ServiceController<?>> serviceAStart = testListener.expectServiceStart(fooServiceName);
        serviceContainer.addService(serviceNameB, serviceBWrapper)
            .addInjection(
                new SetMethodInjector<ServiceB>(serviceBWrapper, ServiceBWrapper.class, "setValue", ServiceB.class), serviceB)
                .addListener(testListener)
        .install();
        final ServiceController<?> serviceControllerB = assertController(serviceNameB, serviceBStart);
        assertSame(serviceB, serviceControllerB.getValue());
        assertController(serviceControllerA, serviceAStart);
        assertSame(serviceA, serviceControllerA.getValue());
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
        
        final BatchBuilder builder = serviceContainer.batchBuilder();
        // install service B, in wrapper
        builder.addService(serviceNameB, serviceBWrapper)
            .addInjection(
                new SetMethodInjector<ServiceB>(serviceBWrapper, ServiceBWrapper.class, "setValue", ServiceB.class), serviceB)
                .addListener(testListener)
                .install();
        // install service D, and inject service C in it
        builder.addService(serviceNameD, serviceD)
            .addInjection(new SetMethodInjector<SomeService>(Values.immediateValue(serviceD), ServiceD.class,"setSomeService", SomeService.class), serviceC)
            .addListener(testListener)
            .install();
        // install service A, inject description, initialParameter, service B and service D
        final Value<ServiceA> serviceAValue = Values.immediateValue(serviceA);
        builder.addService(fooServiceName, serviceA).addInjection(new FieldInjector<String>(serviceAValue,
                new LookupFieldValue(Values.<Class<?>>immediateValue(ServiceA.class), "description")), "foo service_")
                .addInjection(new FieldInjector<Integer>(serviceAValue, new LookupFieldValue(Values.<Class<?>>immediateValue(ServiceA.class), "initialParameter")), 137)
                .addDependency(serviceNameB, new FieldInjector<Object>(serviceAValue, new LookupFieldValue(Values.<Class<?>>immediateValue(ServiceA.class), "serviceB")))
                .addDependency(serviceNameD, new SetMethodInjector<Object>(Values.immediateValue(serviceA), ServiceA.class, "setServiceD", ServiceD.class))
                .addListener(testListener)
                .addListener(new AbstractServiceListener<ServiceA>() {
                 @Override
                 public void dependencyUninstalled(final ServiceController<? extends ServiceA> controller) {
                     // remove foo service as soon as its dep is uninstalled
                     controller.setMode(Mode.REMOVE);
                 }
                 
                 @Override
                 public void serviceRemoved(final ServiceController<? extends ServiceA> controller){
                     try {
                         // replace the removed service by service E
                         serviceContainer.addService(fooServiceName, serviceE)
                         .addOptionalDependency(serviceNameD, ServiceD.class, new SetMethodInjector<ServiceD>(
                                 Values.immediateValue(serviceE), ServiceE.class, "initialize", ServiceD.class))
                          .addListener(testListener)
                          .install();
                    } catch (IllegalArgumentException e) {
                        throw new RuntimeException(e);
                    } catch (ServiceRegistryException e) {
                        throw new RuntimeException(e);
                    }
                 }
             })
                .install();

        final Future<ServiceController<?>> serviceAStart = testListener.expectServiceStart(fooServiceName);
        final Future<ServiceController<?>> serviceBStart = testListener.expectServiceStart(serviceNameB);
        final Future<ServiceController<?>> serviceDStart = testListener.expectServiceStart(serviceNameD);
        builder.install();
        final ServiceController<?> serviceControllerA = assertController(fooServiceName, serviceAStart);
        final ServiceController<?> serviceControllerB = assertController(serviceNameB, serviceBStart);
        final ServiceController<?> serviceControllerD = assertController(serviceNameD, serviceDStart);
        assertSame(serviceA, serviceControllerA.getValue());
        assertEquals("foo service_", serviceA.description);
        assertEquals(137, serviceA.initialParameter);
        assertSame(serviceB, serviceA.serviceB);
        assertSame(serviceC, serviceA.serviceC);

        // stop service B
        final Future<ServiceController<?>> serviceAStop = testListener.expectServiceStop(fooServiceName);
        final Future<ServiceController<?>> serviceBStop = testListener.expectServiceStop(serviceNameB);
        serviceControllerB.setMode(Mode.NEVER);
        assertController(serviceControllerA, serviceAStop);
        assertController(serviceControllerB, serviceBStop);
        assertSame(serviceA, serviceControllerA.getValue());
        assertNull(serviceA.description);
        assertEquals(137, serviceA.initialParameter);
        assertNull(serviceA.serviceB);
        assertNull(serviceA.serviceC);

        final Future<ServiceController<?>> serviceBRemoval = testListener.expectServiceRemoval(serviceNameB);
        final Future<ServiceController<?>> serviceARemoval = testListener.expectServiceRemoval(fooServiceName);
        Future<ServiceController<?>> serviceEStart = testListener.expectServiceStart(fooServiceName);
        serviceControllerB.setMode(Mode.REMOVE);
        assertController(serviceControllerB, serviceBRemoval);
        assertController(serviceControllerA, serviceARemoval);
        ServiceController<?> serviceControllerE = assertController(fooServiceName, serviceEStart);
        assertNull(serviceControllerE.getValue());
        assertSame(serviceD, serviceE.serviceD);

        final Future<ServiceController<?>> serviceDStop = testListener.expectServiceStop(serviceNameD);
        final Future<ServiceController<?>> serviceEStop = testListener.expectServiceStop(fooServiceName);
        serviceEStart = testListener.expectServiceStart(fooServiceName);
        serviceControllerD.setMode(Mode.REMOVE);
        assertController(serviceControllerD, serviceDStop);
        assertOppositeNotifications(serviceControllerE, serviceEStop, serviceEStart);
        assertNull(serviceE.serviceD);
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
        public void start(StartContext context) throws StartException {}

        @Override
        public void stop(StopContext context) {}

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
            startedService = true;
        }
        
        public void stop() {
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
            serviceB.start();
        }

        @Override
        public void stop(StopContext context) {
            serviceB.stop();
        } 
        
    }

    public static class ServiceC implements SomeService {}

    public static interface SomeService {}

    public static class ServiceD implements Service<ServiceD> {
        private SomeService service;

        /**
         * @param serviceC the serviceC to set
         */
        public void setSomeService(SomeService service) {
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

        @Override
        public void start(StartContext context) throws StartException {}

        @Override
        public void stop(StopContext context) {}
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
        public void start(StartContext context) throws StartException {}

        @Override
        public void stop(StopContext context) {}
    }
}
