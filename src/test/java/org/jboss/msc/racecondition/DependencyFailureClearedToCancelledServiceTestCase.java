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

import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.util.concurrent.Future;

import org.jboss.byteman.contrib.bmunit.BMScript;
import org.jboss.byteman.contrib.bmunit.BMUnitRunner;
import org.jboss.msc.service.CircularDependencyException;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceController.Mode;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.util.FailToStartService;
import org.jboss.msc.util.TestServiceListener;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Creates a scenario where a canceled service receives a dependencyFailureCleared notification. The
 * notification should be ignored as the service is canceled and will be removed at any minute.
 * 
 * @author <a href="mailto:flavia.rainone@jboss.com">Flavia Rainone</a>
 *
 */
@RunWith(BMUnitRunner.class)
@BMScript(dir="src/test/resources")
public class DependencyFailureClearedToCancelledServiceTestCase  extends AbstractRaceConditionTest {
    /**
     * This test forces a dependencyFailureCleared() call on a ServiceControllerImpl with CANCELLED status.
     */
    @Test
    public void test() throws Throwable {
        System.out.println(System.getProperty("racecondition.dir"));
        final TestServiceListener testListener = new TestServiceListener();
        serviceContainer.addListener(testListener);
        final ServiceName cancelledServiceName = ServiceName.of("cancelled");
        final ServiceName dependencyServiceName = ServiceName.of("dependency");
        final ServiceName dependentServiceName = ServiceName.of("dependent");

        final Future<ServiceController<?>> dependentMissingDependency = testListener.expectDependencyUninstall(dependentServiceName);
        serviceContainer.addService(dependentServiceName, Service.NULL).addDependencies(cancelledServiceName, dependencyServiceName).install();
        final ServiceController<?> dependentController = assertController(dependentServiceName, dependentMissingDependency);

        final Future<StartException> dependencyFailure = testListener.expectServiceFailure(dependencyServiceName);
        serviceContainer.addService(dependencyServiceName, new FailToStartService(true)).install();
        final ServiceController<?> dependencyController = assertFailure(dependencyServiceName, dependencyFailure);

        final Future<ServiceController<?>> noDependencyFailureCleared = testListener.expectNoDependencyFailureCleared(cancelledServiceName);
        final Future<ServiceController<?>> dependencyFailureCleared = testListener.expectDependencyFailureCleared(dependentServiceName);
        final SetModeRunnable setDependencyMode = new SetModeRunnable(dependencyController, Mode.NEVER);
        final Thread t = new Thread(setDependencyMode);
        t.start();

        try {
            serviceContainer.addService(cancelledServiceName, Service.NULL).addDependencies(dependentServiceName, dependencyServiceName).install();
            fail ("CircularDependencyException expected");
        } catch (CircularDependencyException e) {}

        t.join();
        assertNull(setDependencyMode.getSetModeException());
        assertController(dependentController, dependencyFailureCleared);
        assertNull(noDependencyFailureCleared.get());
    }

    private static class SetModeRunnable implements Runnable {
        private final ServiceController<?> serviceController;
        private final Mode newMode;
        private Throwable setModeException;
        
        
        public SetModeRunnable(ServiceController<?> serviceController, Mode newMode) {
            this.serviceController = serviceController;
            this.newMode = newMode;
        }

        @Override
        public void run() {
            try {
                serviceController.setMode(newMode);
            } catch (Throwable t) {
                setModeException = t;
            }
        }

        public Throwable getSetModeException() {
            return setModeException;
        }

    }
}
