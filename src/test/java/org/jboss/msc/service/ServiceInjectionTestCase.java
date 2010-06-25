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

import org.jboss.msc.value.ImmediateValue;

import java.lang.reflect.Field;


/**
 * Test case used to ensure basic service injection functionality.
 *
 * @author John Bailey
 */
public class ServiceInjectionTestCase {
        public void testBasicInjection() throws Exception {
        final BatchBuilder batch = ServiceContainer.Factory.create().batchBuilder();

        final TestObject testObject = new TestObject();
        final TestObjectService service = new TestObjectService(testObject);
        final Object injectedValue = new Object();
        final Object otherInjectedValue = new Object();

        final Field field = TestObject.class.getDeclaredField("test");
        field.setAccessible(true);

        final BatchServiceBuilder<TestObject> serviceBuilder = batch.addService(ServiceName.of("testService"), service);
        serviceBuilder.addInjection(injectedValue).toFieldValue(new ImmediateValue<Field>(field));
        serviceBuilder.addInjection(otherInjectedValue).toProperty("other");
        batch.install();
    }

    public static class TestObject {
        private Object test;
        private Object other;

        public Object getOther() {
            return other;
        }

        public void setOther(Object other) {
            this.other = other;
        }

        @Override
        public String toString() {
            return "TestObject{" +
                    "test=" + test +
                    ", other=" + other +
                    '}';
        }
    }

    private static class TestObjectService implements Service<TestObject> {

        private final TestObject value;

        private TestObjectService(TestObject value) {
            this.value = value;
        }

        @Override
        public void start(StartContext context) throws StartException {
            System.out.println("Injected: " + value);
        }

        @Override
        public void stop(StopContext context) {
        }

        @Override
        public TestObject getValue() throws IllegalStateException {
            return null;
        }
    }
}
