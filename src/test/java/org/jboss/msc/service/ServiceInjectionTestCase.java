/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat Middleware LLC, and individual contributors
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

import org.jboss.msc.service.util.LatchedFinishListener;
import org.jboss.msc.value.ClassOfValue;
import org.jboss.msc.value.Value;
import org.jboss.msc.value.Values;
import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;

/**
 * Test case used to ensure basic service injection functionality.
 *
 * @author John Bailey
 */
public class ServiceInjectionTestCase extends AbstractServiceTest {

    private final TestObjectService service = new TestObjectService();
    private final TestObjectService serviceTwo = new TestObjectService();
    private final TestObjectService serviceThree = new TestObjectService();
    private final TestObjectService serviceFour = new TestObjectService();

    private final Object injectedValue = new Object();

    @Test
    public void testFieldBasedInjection() throws Exception {

        perfromTest(new ServiceTestInstance() {
            @Override
            public List<BatchBuilder> initializeBatches(ServiceContainer serviceContainer, LatchedFinishListener finishListener) throws Exception {
                final Field field = TestObjectService.class.getField("test");
                final BatchBuilder batch = serviceContainer.batchBuilder().addListener(finishListener);
                BatchServiceBuilder<TestObjectService> serviceBuilder = batch.addService(ServiceName.of("testService"), service);
                serviceBuilder.addInjection(injectedValue).toField(field);
                serviceBuilder = batch.addService(ServiceName.of("testServiceTwo"), serviceTwo);
                serviceBuilder.addInjection(injectedValue).toFieldValue(Values.immediateValue(field));
                serviceBuilder = batch.addService(ServiceName.of("testServiceThree"), serviceThree);
                serviceBuilder.addInjection(injectedValue).toField("test");
                return Collections.singletonList(batch);
            }

            @Override
            public void performAssertions(ServiceContainer serviceContainer) throws Exception {
                Assert.assertNotNull(service.test);
                Assert.assertEquals(injectedValue, service.test);
                Assert.assertNotNull(serviceTwo.test);
                Assert.assertEquals(injectedValue, serviceTwo.test);
                Assert.assertNotNull(serviceThree.test);
                Assert.assertEquals(injectedValue, serviceThree.test);
            }
        });
    }

    @Test
    public void testMethodBasedInjection() throws Exception {
        perfromTest(new ServiceTestInstance() {
            @Override
            public List<BatchBuilder> initializeBatches(ServiceContainer serviceContainer, LatchedFinishListener finishListener) throws Exception {
                final Method method = TestObjectService.class.getMethod("setTest", Object.class);
                final BatchBuilder batch = serviceContainer.batchBuilder().addListener(finishListener);
                BatchServiceBuilder<TestObjectService> serviceBuilder = batch.addService(ServiceName.of("testService"), service);
                serviceBuilder.addInjection(injectedValue).toMethod(method, Collections.singletonList(Values.immediateValue(injectedValue)));
                serviceBuilder = batch.addService(ServiceName.of("testServiceTwo"), serviceTwo);
                serviceBuilder.addInjection(injectedValue).toMethodValue(Values.immediateValue(method), Collections.singletonList(Values.immediateValue(injectedValue)));
                serviceBuilder = batch.addService(ServiceName.of("testServiceThree"), serviceThree);
                serviceBuilder.addInjection(injectedValue).toMethod("setTest", Collections.singletonList(new ClassOfValue<Object>(Values.immediateValue(injectedValue))), Collections.singletonList(Values.immediateValue(injectedValue)));
                serviceBuilder = batch.addService(ServiceName.of("testServiceFour"), serviceFour);
                serviceBuilder.addInjection(injectedValue).toMethod("setTest");
                return Collections.singletonList(batch);
            }

            @Override
            public void performAssertions(ServiceContainer serviceContainer) throws Exception {
                Assert.assertNotNull(service.test);
                Assert.assertEquals(injectedValue, service.test);
                Assert.assertNotNull(serviceTwo.test);
                Assert.assertEquals(injectedValue, serviceTwo.test);
                Assert.assertNotNull(serviceThree.test);
                Assert.assertEquals(injectedValue, serviceThree.test);
                Assert.assertNotNull(serviceFour.test);
                Assert.assertEquals(injectedValue, serviceFour.test);
            }
        });
    }

    @Test
    public void testMethodBasedInjectionWithTargetOverride() throws Exception {
        final AlternateObject target = new AlternateObject();
        final AlternateObject targetTwo = new AlternateObject();
        final AlternateObject targetThree = new AlternateObject();
        final AlternateObject targetFour = new AlternateObject();

        final Value<AlternateObject> targetValue = Values.immediateValue(target);
        final Value<AlternateObject> targetValueTwo = Values.immediateValue(targetTwo);
        final Value<AlternateObject> targetValueThree = Values.immediateValue(targetThree);
        final Value<AlternateObject> targetValueFour = Values.immediateValue(targetFour);

        final Method method = AlternateObject.class.getMethod("setTest", Object.class);

        perfromTest(new ServiceTestInstance() {
            @Override
            public List<BatchBuilder> initializeBatches(ServiceContainer serviceContainer, LatchedFinishListener finishListener) throws Exception {
                final BatchBuilder batch = serviceContainer.batchBuilder().addListener(finishListener);
                BatchServiceBuilder<TestObjectService> serviceBuilder = batch.addService(ServiceName.of("testService"), service);
                serviceBuilder.addInjection(injectedValue).toMethod(method, targetValue, Collections.singletonList(Values.immediateValue(injectedValue)));
                serviceBuilder = batch.addService(ServiceName.of("testServiceTwo"), serviceTwo);
                serviceBuilder.addInjection(injectedValue).toMethodValue(Values.immediateValue(method), targetValueTwo, Collections.singletonList(Values.immediateValue(injectedValue)));
                serviceBuilder = batch.addService(ServiceName.of("testServiceThree"), serviceThree);
                serviceBuilder.addInjection(injectedValue).toMethod("setTest", targetValueThree, Collections.singletonList(new ClassOfValue<Object>(Values.immediateValue(injectedValue))), Collections.singletonList(Values.immediateValue(injectedValue)));
                serviceBuilder = batch.addService(ServiceName.of("testServiceFour"), serviceFour);
                serviceBuilder.addInjection(injectedValue).toMethod("setTest", targetValueFour);
                return Collections.singletonList(batch);
            }

            @Override
            public void performAssertions(ServiceContainer serviceContainer) throws Exception {
                Assert.assertNotNull(target.test);
                Assert.assertEquals(injectedValue, target.test);
                Assert.assertNotNull(targetTwo.test);
                Assert.assertEquals(injectedValue, targetTwo.test);
                Assert.assertNotNull(targetThree.test);
                Assert.assertEquals(injectedValue, targetThree.test);
                Assert.assertNotNull(targetFour.test);
                Assert.assertEquals(injectedValue, targetFour.test);
            }
        });
    }

    @Test
    public void testPropertyBasedInjection() throws Exception {
        perfromTest(new ServiceTestInstance() {
            @Override
            public List<BatchBuilder> initializeBatches(ServiceContainer serviceContainer, LatchedFinishListener finishListener) throws Exception {
                final BatchBuilder batch = serviceContainer.batchBuilder().addListener(finishListener);

                BatchServiceBuilder<TestObjectService> serviceBuilder = batch.addService(ServiceName.of("testService"), service);
                serviceBuilder.addInjection(injectedValue).toProperty("test");

                serviceBuilder = batch.addService(ServiceName.of("testServiceTwo"), serviceTwo);
                serviceBuilder.addInjection(injectedValue).toProperty("other");
                return Collections.singletonList(batch);
            }

            @Override
            public void performAssertions(ServiceContainer serviceContainer) throws Exception {
                Assert.assertNotNull(service.test);
                Assert.assertEquals(injectedValue, service.test);
                Assert.assertNotNull(serviceTwo.test);
                Assert.assertEquals(injectedValue, serviceTwo.test);
            }
        });
    }

//    @Test
//    public void testPropertyBasedInjectionFromProperty() throws Exception {
//    perfromTest(new ServiceTestInstance() {
//            @Override
//            public List<BatchBuilder> initializeBatches(ServiceContainer serviceContainer, LatchedFinishListener finishListener) throws Exception {
//            }
//
//            @Override
//            public void performAssertions(ServiceContainer serviceContainer) throws Exception {
//            }
//        });
//        final ServiceContainer container = getServiceContainer();
//        final LatchedFinishListener listener = new LatchedFinishListener();
//        final BatchBuilder batch = container.batchBuilder();
//        batch.addListener(listener);
//
//        final TestObjectService service = new TestObjectService();
//
//        final ObjectSource injectedValue = new ObjectSource("testVal");
//
//        BatchServiceBuilder<TestObjectService> serviceBuilder = batch.addService(ServiceName.of("testService"), service);
//        serviceBuilder.addInjection(injectedValue).fromProperty("test").toProperty("test");
//
//        batch.install();
//
//        listener.await();
//
//        Assert.assertNotNull(service.test);
//        Assert.assertEquals("testVal", service.test);
//    }

    public static class TestObjectService implements Service<TestObjectService> {
        public Object test;
        public Object otherField;

        @Override
        public void start(StartContext context) throws StartException {
        }

        @Override
        public void stop(StopContext context) {
        }

        @Override
        public TestObjectService getValue() throws IllegalStateException {
            return this;
        }

        public void setTest(Object test) {
            this.test = test;
        }

        public void setOther(Object test) {
            this.test = test;
        }
    }

    public static class AlternateObject {
        public Object test;

        public void setTest(Object test) {
            this.test = test;
        }
    }

    public static class ObjectSource {
        private String test;

        public ObjectSource(String test) {
            this.test = test;
        }

        public String getTest() {
            return test;
        }
    }
}
