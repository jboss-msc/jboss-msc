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

import org.jboss.msc.service.util.LatchedFinishListener;
import org.jboss.msc.value.ClassOfValue;
import org.jboss.msc.value.Value;
import org.jboss.msc.value.Values;
import org.junit.Before;
import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Test case used to ensure basic service injection functionality.
 *
 * @author John Bailey
 */
public class ServiceInjectionTestCase extends AbstractServiceTest {

    private TestObjectService service;
    private TestObjectService serviceTwo;
    private TestObjectService serviceThree;
    private TestObjectService serviceFour;

    private final Object injectedValue = new Object();

    @Before
    public void setup() throws Exception {
        service = new TestObjectService();
        serviceTwo = new TestObjectService();
        serviceThree = new TestObjectService();
        serviceFour = new TestObjectService();
    }

    @Test
    public void testFieldBasedInjection() throws Exception {

        performTest(new ServiceTestInstance() {
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
                assertNotNull(service.test);
                assertEquals(injectedValue, service.test);
                assertNotNull(serviceTwo.test);
                assertEquals(injectedValue, serviceTwo.test);
                assertNotNull(serviceThree.test);
                assertEquals(injectedValue, serviceThree.test);
            }
        });
    }

    @Test
    public void testMethodBasedInjection() throws Exception {
        performTest(new ServiceTestInstance() {
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
                assertNotNull(service.test);
                assertEquals(injectedValue, service.test);
                assertNotNull(serviceTwo.test);
                assertEquals(injectedValue, serviceTwo.test);
                assertNotNull(serviceThree.test);
                assertEquals(injectedValue, serviceThree.test);
                assertNotNull(serviceFour.test);
                assertEquals(injectedValue, serviceFour.test);
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

        performTest(new ServiceTestInstance() {
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
                assertNotNull(target.test);
                assertEquals(injectedValue, target.test);
                assertNotNull(targetTwo.test);
                assertEquals(injectedValue, targetTwo.test);
                assertNotNull(targetThree.test);
                assertEquals(injectedValue, targetThree.test);
                assertNotNull(targetFour.test);
                assertEquals(injectedValue, targetFour.test);
            }
        });
    }

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
        public String test;

        public ObjectSource(String test) {
            this.test = test;
        }

        public String getTest() {
            return test;
        }

        public String getTestWithParam(String param) {
            return test + param;
        }
    }
}
