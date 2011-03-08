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

package org.jboss.msc.racecondition;

import static junit.framework.Assert.assertNull;

import java.util.concurrent.Future;

import org.jboss.byteman.contrib.bmunit.BMScript;
import org.jboss.byteman.contrib.bmunit.BMUnitRunner;
import org.jboss.msc.service.CircularDependencyException;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.util.TestServiceListener;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Creates a scenario where a service being visited by cycle detection is concurrently removed.
 * 
 * @author <a href="mailto:flavia.rainone@jboss.com">Flavia Rainone</a>
 */
@RunWith(BMUnitRunner.class)
@BMScript(dir="src/test/resources")
public class RemovalDuringCycleDetectionTest extends AbstractRaceConditionTest {
    private static final TestServiceListener testListener = new TestServiceListener();
    private static final ServiceName serviceAName = ServiceName.of("A");
    private static final ServiceName serviceBName = ServiceName.of("B");

    @Test
    public void test() throws Exception {
        serviceContainer.addService(serviceBName, Service.NULL).addDependency(serviceAName).addListener(testListener).install();
        final InstallServiceA installA = new InstallServiceA();
        final Thread installAThread = new Thread(null, installA, "Install A");
        final RemoveServiceB removeB = new RemoveServiceB();
        final Thread removeBThread = new Thread(null, removeB, "Remove B");
        installAThread.start();
        removeBThread.start();
        installAThread.join();
        removeBThread.join();
        installA.assertNoCycleDetected();
        removeB.assertBRemoval();
    }

    private class InstallServiceA implements Runnable {
        private CircularDependencyException cycleDetected;

        @Override
        public void run() {
            try {
                serviceContainer.addService(serviceAName, Service.NULL).addListener(testListener)
                .addDependencies(serviceBName).install();
            } catch (CircularDependencyException e) {
                cycleDetected = e;
            }
        }

        public void assertNoCycleDetected() {
            assertNull(cycleDetected);
        }
    }

    public class RemoveServiceB implements Runnable {

        private Future<ServiceController<?>> serviceBRemoval = testListener.expectServiceRemoval(serviceBName);
        private ServiceController<?> serviceBController;

        @Override
        public void run() {
            serviceBController = serviceContainer.getService(serviceBName);
            serviceBController.setMode(ServiceController.Mode.REMOVE);
        }

        public void assertBRemoval() throws Exception {
            assertController(serviceBController, serviceBRemoval);
        }
    }

}
