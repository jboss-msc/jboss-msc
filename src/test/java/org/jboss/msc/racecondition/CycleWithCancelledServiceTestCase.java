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

import static junit.framework.Assert.assertNotNull;
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
 * Creates a scenario where a cycle detection should fail to detect a cycle because one of the services
 * involved in the cycle is in the CANCELLED state.
 * 
 * @author <a href="mailto:flavia.rainone@jboss.com">Flavia Rainone</a>
 */
@RunWith(BMUnitRunner.class)
@BMScript(dir="src/test/resources")
public class CycleWithCancelledServiceTestCase extends AbstractRaceConditionTest {

    private static final TestServiceListener testListener = new TestServiceListener();
    private static final ServiceName serviceAName = ServiceName.of("A");
    private static final ServiceName serviceBName = ServiceName.of("B");
    private static final ServiceName serviceCName = ServiceName.of("C");

    /* Cycle scenario: A->B,C; B->A; C->A
     * Service B is first installed with success.
     * Next, ServiceA installation is canceled. The race condition is to hold service A in canceled state while
     * another Thread installs C with success. In order for C installation to run ok, the cycle detection should skip
     * A as it is a canceled service.
     */
    @Test
    public void test() throws Exception {
        final Future<ServiceController<?>> serviceBInstall = testListener.expectListenerAdded(serviceBName);
        serviceContainer.addService(serviceBName, Service.NULL).addDependency(serviceAName).addListener(testListener).install();
        assertController(serviceBName, serviceBInstall);

        InstallServiceA installA = new InstallServiceA();
        Thread installAThread = new Thread(null, installA, "Install A");
        InstallServiceC installC = new InstallServiceC();
        Thread installCThread = new Thread(null, installC, "Install C");
        installAThread.start();
        installCThread.start();
        installAThread.join();
        installCThread.join();
        installA.assertCycleDetected();
        installC.assertNoCycleDetected();
    }

    private class InstallServiceA implements Runnable {
        private CircularDependencyException cycleDetected;

        @Override
        public void run() {
            try {
                serviceContainer.addService(serviceAName, Service.NULL).addListener(testListener)
                .addDependencies(serviceBName, serviceCName).install();
            } catch (CircularDependencyException e) {
                cycleDetected = e;
            }
        }

        public void assertCycleDetected() {
            assertNotNull(cycleDetected);
        }
    }

    public class InstallServiceC implements Runnable {
        private CircularDependencyException cycleDetected;

        @Override
        public void run() {
            try {
                serviceContainer.addService(serviceCName, Service.NULL).addListener(testListener)
                .addDependency(serviceAName).install();
            } catch (CircularDependencyException e) {
                cycleDetected = e;
            }
        }

        public void assertNoCycleDetected() {
            assertNull(cycleDetected);
        }
    }
}
