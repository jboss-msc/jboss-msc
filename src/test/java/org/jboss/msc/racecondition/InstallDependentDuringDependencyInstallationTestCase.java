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

import java.util.concurrent.Future;

import org.jboss.byteman.contrib.bmunit.BMScript;
import org.jboss.byteman.contrib.bmunit.BMUnitRunner;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.util.TestServiceListener;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * This task attempts to install a dependent service while its dependency is on commit installation process.
 * The goal is to check there is no time frame window where dependent will fail to receive an
 * immediateDependencyUnavailable notification followed by an immediateDependencyAvailable notification.
 * <p> For further information, see <a href="https://issues.jboss.org/browse/MSC-100">MSC-100</a>.
 * 
 * @author <a href="mailto:flavia.rainone@jboss.com">Flavia Rainone</a>
 */
@RunWith(BMUnitRunner.class)
@BMScript(dir="src/test/resources")
public class InstallDependentDuringDependencyInstallationTestCase extends AbstractRaceConditionTest {

    private static final TestServiceListener testListener = new TestServiceListener();
    private static final ServiceName serviceAName = ServiceName.of("A");
    private static final ServiceName serviceBName = ServiceName.of("B");

    @Test
    public void test() throws Exception {
        final Future<ServiceController<?>> serviceBInstall = testListener.expectListenerAdded(serviceBName);
        final Future<ServiceController<?>> serviceAInstall = testListener.expectListenerAdded(serviceAName);
        final Future<ServiceController<?>> serviceAStart = testListener.expectServiceStart(serviceAName);
        final Future<ServiceController<?>> serviceBStart = testListener.expectServiceStart(serviceBName);

        InstallServiceA installA = new InstallServiceA();
        Thread installAThread = new Thread(null, installA, "Install A");
        InstallServiceB installB = new InstallServiceB();
        Thread installBThread = new Thread(null, installB, "Install B");
        installAThread.start();
        installBThread.start();
        installAThread.join();
        installBThread.join();
        final ServiceController<?> serviceBController = assertController(serviceBName, serviceBInstall);
        final ServiceController<?> serviceAController = assertController(serviceAName, serviceAInstall);
        assertController(serviceBController, serviceBStart);
        assertController(serviceAController, serviceAStart);
    }

    private class InstallServiceA implements Runnable {
        @Override
        public void run() {
            serviceContainer.addService(serviceAName, Service.NULL).addListener(testListener)
                .addDependency(serviceBName).install();
        }
    }

    public class InstallServiceB implements Runnable {
        @Override
        public void run() {
            serviceContainer.addService(serviceBName, Service.NULL).addListener(testListener).install();
        }
    }
}
