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

package org.jboss.msc.bench;

import org.jboss.msc.inject.FieldInjector;
import org.jboss.msc.inject.SetMethodInjector;
import org.jboss.msc.registry.ServiceDefinition;
import org.jboss.msc.registry.ServiceRegistrationBatchBuilder;
import org.jboss.msc.registry.ServiceRegistry;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceContainer;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.service.TimingServiceListener;
import org.jboss.msc.value.CachedValue;
import org.jboss.msc.value.ImmediateValue;
import org.jboss.msc.value.LookupFieldValue;
import org.jboss.msc.value.LookupMethodValue;
import org.jboss.msc.value.Value;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;

public class FiveForwardInjectionsBench {

    public static void main(String[] args) throws Exception {
        final int totalServiceDefinitions = Integer.parseInt(args[0]);

        final ServiceContainer container = ServiceContainer.Factory.create();
        final ServiceRegistry registry = ServiceRegistry.Factory.create(container);
        ServiceRegistrationBatchBuilder batch = registry.batchBuilder();

        final CountDownLatch latch = new CountDownLatch(1);
        final TimingServiceListener listener = new TimingServiceListener(new TimingServiceListener.FinishListener() {
            public void done(final TimingServiceListener timingServiceListener) {
                latch.countDown();
            }
        });

        final Value<Field> testFieldValue = new CachedValue<Field>(new LookupFieldValue(new ImmediateValue<Class<?>>(TestObject.class), "test"));

        final List<Value<Class<?>>> params = Collections.singletonList((Value<Class<?>>)new ImmediateValue<Class<?>>(TestObject.class));
        final List<Value<Method>> setterMethodValues = new ArrayList<Value<Method>>(5);
        for(int i = 0; i < 5; i++)
            setterMethodValues.add(new CachedValue<Method>(new LookupMethodValue(new ImmediateValue<Class<?>>(TestObject.class), "setOther" + (i + 1), params)));

        for (int i = 0; i < totalServiceDefinitions; i++) {
            final TestObject testObject = new TestObject("test" + i);
            final TestObjectService service = new TestObjectService(testObject);
            
            final ServiceDefinition.Builder<TestObject> builder = ServiceDefinition.build(ServiceName.of(("test" + i).intern()), service)
                    .addListener(listener);

            final Object injectedValue = new Object();
            builder.addInjection(
                    new ImmediateValue<Object>(injectedValue),
                    new FieldInjector<Object>(service, testFieldValue)
            );

            int numDeps = Math.min(5, totalServiceDefinitions - i - 1);
            for (int j = 1; j < numDeps + 1; j++) {
                builder.addInjection(
                        ServiceName.of(("test" + (i + j)).intern()),
                        new SetMethodInjector<TestObject>(service,  setterMethodValues.get(j - 1))
                );
            }
            batch.add(builder.create());
        }

        batch.install();
        listener.finishBatch();
        latch.await();
        System.out.println(totalServiceDefinitions + " : " + listener.getElapsedTime() / 1000.0);
        container.shutdown();
    }

    public static class TestObject {
        public String name;
        public Object test;
        public TestObject other1;
        public TestObject other2;
        public TestObject other3;
        public TestObject other4;
        public TestObject other5;

        public TestObject(String name) {
            this.name = name;
        }

        public void setOther1(TestObject other1) {
            this.other1 = other1;
        }

        public void setOther2(TestObject other2) {
            this.other2 = other2;
        }

        public void setOther3(TestObject other3) {
            this.other3 = other3;
        }

        public void setOther4(TestObject other4) {
            this.other4 = other4;
        }

        public void setOther5(TestObject other5) {
            this.other5 = other5;
        }

        @Override
        public String toString() {
            return "TestObject{" +
                    "name=" + name +
                    ", other1=" + other1 +
                    ", other2=" + other2 +
                    ", other3=" + other3 +
                    ", other4=" + other4 +
                    ", other5=" + other5 +
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
        }

        @Override
        public void stop(StopContext context) {
        }

        @Override
        public TestObject getValue() throws IllegalStateException {
            return value;
        }
    }
}