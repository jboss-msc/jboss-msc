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

package org.jboss.msc.bench;

import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceContainer;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.service.TimingServiceListener;
import org.jboss.msc.service.util.LatchedFinishListener;

import java.util.concurrent.CountDownLatch;

public class OneToFourForwardInjectionsBench {

    // MSC-63
    public static void main(String[] args) throws Exception {
//        final int totalServiceDefinitions = Integer.parseInt(args[0]);
//
//        final ServiceContainer container = ServiceContainer.Factory.create();
//        BatchBuilder batch = container.batchBuilder();
//
//        final LatchedFinishListener listener = new LatchedFinishListener();
//
//        for (int i = 0; i < totalServiceDefinitions; i++) {
//            final TestObject testObject = new TestObject("test" + i);
//            final TestObjectService service = new TestObjectService(testObject);
//            
//            final ServiceBuilder<TestObject> builder = batch.addService(ServiceName.of(("test" + i).intern()), service);
//
//            final Object injectedValue = new Object();
////            builder.addInjection(injectedValue);//.toField("test");
//
//            int nextDivByFive = (5 - (i % 5)) + i;
//            int numDeps = Math.min(nextDivByFive - i, totalServiceDefinitions - i - 1);
//            for (int j = 0; j < numDeps; j++) {
//                int depId = i + j + 1;
//                if(depId % 5 ==0)
//                    continue;
//
//                builder.addDependency(ServiceName.of(("test" + depId).intern()));
////                    .toMethod("setOther" + (i));
//            }
//        }
//
//        batch.install();
//        listener.await();
//        System.out.println(totalServiceDefinitions + " : " + listener.getElapsedTime() / 1000.0);
//        container.shutdown();
    }

    public static class TestObject {
        public String name;
        public Object test;
        public TestObject other0;
        public TestObject other1;
        public TestObject other2;
        public TestObject other3;
        public TestObject other4;

        public TestObject(String name) {
            this.name = name;
        }

        public void setOther0(TestObject other0) {
            this.other0 = other0;
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

        @Override
        public String toString() {
            return "TestObject{" +
                    "name=" + name +
                    ", other0=" + other0 +
                    ", other1=" + other1 +
                    ", other2=" + other2 +
                    ", other3=" + other3 +
                    ", other4=" + other4 +
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