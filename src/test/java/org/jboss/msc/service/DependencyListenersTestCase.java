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

import java.util.concurrent.Future;

import org.jboss.msc.service.ServiceController.Mode;
import org.jboss.msc.service.ServiceController.State;
import org.jboss.msc.util.TestServiceListener;
import org.junit.Test;

/**
 * Test notifications sent to listeners related to dependency operations.
 * 
 * @author <a href="mailto:flavia.rainone@jboss.com">Flavia Rainone</a>
 * @see ServiceListener#dependencyFailed(ServiceController)
 * @see ServiceListener#dependencyRetrying(ServiceController)
 * @see ServiceListener#transitiveDependenciesInstalled(ServiceController)
 * @see ServiceListener#transitiveDependencyUninstalled(ServiceController)
 */
public class DependencyListenersTestCase extends AbstractServiceTest {

    @Test
    public void testMissingDependencies() throws Exception {
        
        final TestServiceListener testListener = new TestServiceListener();

        ServiceName firstServiceName = ServiceName.of("firstService");
        ServiceName secondServiceName = ServiceName.of("secondService");
        Future<ServiceController<?>> missingDependency = testListener.expectTransitiveDependencyUninstall(firstServiceName);

        // add firstService with dependency on missing secondService
        serviceContainer.addService(firstServiceName, Service.NULL)
            .addDependency(secondServiceName)
            .addListener(testListener)
            .install();
        // uninstalled dependency notification expected
        ServiceController<?> controller = missingDependency.get();
        assertNotNull(controller );
        assertEquals(serviceContainer.getService(firstServiceName), controller );

        Future<ServiceController<?>> installDependency = testListener.expectTransitiveDependencyInstall(firstServiceName);
        // install missing secondService
        serviceContainer.addService(secondServiceName, Service.NULL).install();
        // dependency installed notification expected
        ServiceController<?> controller2 = installDependency.get();
        assertNotNull(controller2);
        assertSame(controller , controller2);

        missingDependency = testListener.expectTransitiveDependencyUninstall(firstServiceName);
        // remove secondService
        serviceContainer.getService(secondServiceName).setMode(Mode.REMOVE);
        // uninstalled dependency notification expected again
        controller2 = missingDependency.get();
        assertNotNull(controller2);
        assertSame(controller , controller2);
    }

    @Test
    public void testTransitiveMissingDependencies() throws Exception {
        
        final TestServiceListener testListener = new TestServiceListener();

        ServiceName firstServiceName = ServiceName.of("firstService");
        ServiceName secondServiceName = ServiceName.of("secondService");
        ServiceName thirdServiceName = ServiceName.of("thirdService");
        Future<ServiceController<?>> firstServiceMissingDependency = testListener.expectTransitiveDependencyUninstall(firstServiceName);

        // add firstService with dependency on missing secondService
        serviceContainer.addService(firstServiceName, Service.NULL)
            .addDependency(secondServiceName)
            .addListener(testListener)
            .install();
        // uninstalled dependency notification expected
        ServiceController<?> firstController= firstServiceMissingDependency.get();
        assertNotNull(firstController);
        assertSame(serviceContainer.getService(firstServiceName), firstController);

        Future<ServiceController<?>> firstServiceInstalledDependency = testListener.expectTransitiveDependencyInstall(firstServiceName);
        Future<ServiceController<?>> firstServiceUninstalledDependency = testListener.expectTransitiveDependencyUninstall(firstServiceName);
        Future<ServiceController<?>> secondServiceMissingDependency = testListener.expectTransitiveDependencyUninstall(secondServiceName);
        // add secondService with dependency on missing thirdService
        serviceContainer.addService(secondServiceName, Service.NULL)
            .addDependency(thirdServiceName)
            .addListener(testListener)
            .install();
        // installed dependency notification expected
        assertSame(firstController, firstServiceInstalledDependency.get());
        // uninstalled dependency notification expected from secondService
        ServiceController<?> secondController = secondServiceMissingDependency.get();
        assertNotNull(secondController);
        assertSame(serviceContainer.getService(secondServiceName), secondController);
        // uninstalled dependency notification expected also expected from firstService
        assertSame(firstController, firstServiceUninstalledDependency.get());

        firstServiceInstalledDependency = testListener.expectTransitiveDependencyInstall(firstServiceName);
        Future<ServiceController<?>> secondServiceInstalledDependency = testListener.expectTransitiveDependencyInstall(secondServiceName);
        // install missing thirdService
        serviceContainer.addService(thirdServiceName, Service.NULL).install();
        // dependency installed notification expected
        assertSame(secondController, secondServiceInstalledDependency.get());
        // dependency installed notification also expected from firstService
        assertSame(firstController, firstServiceInstalledDependency.get());

        firstServiceMissingDependency = testListener.expectTransitiveDependencyUninstall(firstServiceName);
        secondServiceMissingDependency = testListener.expectTransitiveDependencyUninstall(secondServiceName);
        // remove thirdService
        serviceContainer.getService(thirdServiceName).setMode(Mode.REMOVE);
        // uninstalled dependency notification expected again
        assertSame(secondController, secondServiceMissingDependency.get());
        // uninstalled dependency notification also expected from the rest of the dependent chain
        assertSame(firstController, firstServiceMissingDependency.get());
    }

    @Test
    public void testDependenciesFailed() throws Exception {
        final TestServiceListener testListener = new TestServiceListener();

        ServiceName firstServiceName = ServiceName.of("firstService");
        ServiceName secondServiceName = ServiceName.of("secondService");
        Future<ServiceController<?>> firstServiceDependencyFailure = testListener.expectDependencyFailure(firstServiceName);
        Future<ServiceController<?>> secondServiceDependencyFailure = testListener.expectNoDependencyFailure(secondServiceName);

        // install firstService and secondService on a batch
        BatchBuilder batch = serviceContainer.batchBuilder();
        batch.addService(firstServiceName, Service.NULL)
            .addDependency(secondServiceName)
            .addListener(testListener)
            .install();
        // secondService will throw a StateException at first attempt to start
        batch.addService(secondServiceName, new FailToStartService(true)).install();
        batch.install();

        // dependencyFailure notification expected from firstService
        ServiceController<?> controller= firstServiceDependencyFailure.get();
        assertNotNull(controller);
        assertEquals(serviceContainer.getService(firstServiceName), controller);
        // dependencyFailure notification not expected from secondService
        assertNull(secondServiceDependencyFailure.get());

        firstServiceDependencyFailure = testListener.expectNoDependencyFailure(firstServiceName);
        Future<ServiceController<?>> firstServiceDependencyFailureClear = testListener.expectDependencyRetrying(firstServiceName);
        // retry to start service
        serviceContainer.getService(secondServiceName).setMode(Mode.NEVER); // FIXME
        // dependencyFailureClear expected
        assertSame(controller, firstServiceDependencyFailureClear.get());
        
        serviceContainer.getService(secondServiceName).setMode(Mode.ACTIVE);
        // no dependencyFailure expected this time
        assertNull(firstServiceDependencyFailure.get());
    }

    @Test
    public void testTransitiveDependenciesFailed() throws Exception {
        final TestServiceListener testListener = new TestServiceListener();

        ServiceName firstServiceName = ServiceName.of("firstService");
        ServiceName secondServiceName = ServiceName.of("secondService");
        ServiceName thirdServiceName = ServiceName.of("thirdService");
        Future<ServiceController<?>> firstServiceDependencyFailure = testListener.expectDependencyFailure(firstServiceName);
        Future<ServiceController<?>> secondServiceDependencyFailure = testListener.expectDependencyFailure(secondServiceName);

        // install firstService and secondService on a batch
        BatchBuilder batch = serviceContainer.batchBuilder();
        batch.addService(firstServiceName, Service.NULL)
            .addDependency(secondServiceName)
            .addListener(testListener)
            .install();
        batch.addService(secondServiceName, Service.NULL)
        .addDependency(thirdServiceName)
        .addListener(testListener)
        .install();
        // thirdService will throw a StateException at first attempt to start
        batch.addService(thirdServiceName, new FailToStartService(true)).install();
        batch.install();

        // dependencyFailure notification expected from secondService
        ServiceController<?> secondController = secondServiceDependencyFailure.get();
        assertNotNull(secondController);
        assertEquals(serviceContainer.getService(secondServiceName), secondController);
        // dependencyFailure notification also expected from firstService
        ServiceController<?> firstController = firstServiceDependencyFailure.get();
        assertNotNull(firstController );
        assertEquals(serviceContainer.getService(firstServiceName), firstController );

        secondServiceDependencyFailure = testListener.expectNoDependencyFailure(firstServiceName);
        Future<ServiceController<?>> firstServiceDependencyFailureClear = testListener.expectDependencyRetrying(firstServiceName);
        Future<ServiceController<?>> secondServiceDependencyFailureClear = testListener.expectDependencyRetrying(secondServiceName);
        // retry to start service
        serviceContainer.getService(thirdServiceName).setMode(Mode.NEVER); // FIXME
        // dependencyFailureClear expected from both second and first services
        assertSame(secondController, secondServiceDependencyFailureClear.get());
        assertSame(firstController , firstServiceDependencyFailureClear.get());
        // retry to start service
        serviceContainer.getService(thirdServiceName).setMode(Mode.ACTIVE);
        // serviceDependencyFailureClear expected from secondController...
        assertSame(secondController, secondServiceDependencyFailureClear.get());
        // ... and also expected transitively from firstService
        assertSame(firstController , firstServiceDependencyFailureClear.get());
        // no dependencyFailure expected this time
        assertNull(secondServiceDependencyFailure.get());
    }

    @Test
    public void testOptionalDependency() throws Exception {
        final TestServiceListener testListener = new TestServiceListener();

        ServiceName firstServiceName = ServiceName.of("firstService");
        ServiceName secondServiceName = ServiceName.of("secondService");
        ServiceName thirdServiceName = ServiceName.of("thirdService");
        Future<ServiceController<?>> firstServiceStart = testListener.expectServiceStart(firstServiceName);
        Future<ServiceController<?>> secondServiceUninstalled = testListener.expectNoTransitiveDependencyUninstall(firstServiceName);

        // add firstService with an optional dependency on missing secondService
        serviceContainer.addService(firstServiceName, Service.NULL)
            .addOptionalDependency(secondServiceName)
            .addListener(testListener)
            .install();
        // firstService is expected to start
        ServiceController<?> firstController = firstServiceStart.get();
        assertNotNull(firstController);
        assertEquals(serviceContainer.getService(firstServiceName), firstController);

        // install missing secondService on ON_DEMAND mode
        serviceContainer.addService(secondServiceName, Service.NULL)
            .addListener(testListener)
            .setInitialMode(Mode.ON_DEMAND)
            .install();
        // secondService is not expected to start at this point
        assertSame(State.DOWN, serviceContainer.getService(secondServiceName).getState());

        // move fistService to NEVER mode, and wait till it is stops
        Future<ServiceController<?>> firstServiceStop = testListener.expectServiceStop(firstServiceName);
        firstController.setMode(Mode.NEVER);
        assertSame(firstController, firstServiceStop.get());

        // now it is expected that that firstService is connected with its optional dependency
        Future<ServiceController<?>> secondServiceStart = testListener.expectServiceStart(secondServiceName);
        // check this by moving firstService to ACTIVE mode
        firstController.setMode(Mode.ACTIVE);
        // secondService is expected to start
        ServiceController<?> secondController = secondServiceStart.get();
        assertNotNull(secondController);
        assertSame(serviceContainer.getService(secondServiceName), secondController);

        firstServiceStop = testListener.expectServiceStop(firstServiceName);
        // move secondService to NEVER mode
        secondController.setMode(Mode.NEVER);
        // firstService is expected to stop
        assertSame(firstController, firstServiceStop.get());

        firstServiceStart = testListener.expectServiceStart(firstServiceName);
        Future<ServiceController<?>> secondServiceRemoval = testListener.expectServiceRemoval(secondServiceName);
        // uninstall secondService, thus disconnecting it from firstService as an optionalDependency
        secondController.setMode(Mode.REMOVE);
        // wait for removal to be complete
        assertSame(secondController, secondServiceRemoval.get());
        // first service expected to start
        assertSame(firstController, firstServiceStart.get());

        // add a FailToStartService as a secondService, ON_DEMAND mode
        FailToStartService failService = new FailToStartService(true);
        serviceContainer.addService(secondServiceName, failService)
            .setInitialMode(Mode.ON_DEMAND).addListener(testListener).install();

        firstServiceStop = testListener.expectServiceStop(firstServiceName);
        // move firstService to NEVER mode, thus connecting it with secondService as an optionalDependency
        firstController.setMode(Mode.NEVER);
        assertSame(firstController, firstServiceStop.get());

        Future<ServiceController<?>> firstServiceDependencyFailed = testListener.expectDependencyFailure(firstServiceName);
        // move firstService to ACTIVE mode, triggering the start process of secondService
        firstController.setMode(Mode.ACTIVE);
        // dependency failure notification is expected
        assertSame(firstController, firstServiceDependencyFailed.get());

        firstServiceStart = testListener.expectServiceStart(firstServiceName);
        Future<ServiceController<?>> firstServiceDependencyFailureCleared = testListener.expectDependencyRetrying(firstServiceName);
        secondServiceStart = testListener.expectServiceStart(secondServiceName);
        // retry firstService start FIXME
        firstController.setMode(Mode.NEVER);
        firstController.setMode(Mode.ACTIVE);
        // no failures this time; firstService is expected to start now
        assertSame(firstController, firstServiceStart.get());
        // firstService is supposed to receive a dependencyFailureCleared notification
        assertSame(firstController, firstServiceDependencyFailureCleared.get());
        // as is secondService
        secondController = secondServiceStart.get();
        assertNotNull(secondController);
        assertSame(serviceContainer.getService(secondServiceName), secondController);
        secondController.setMode(Mode.NEVER);

        firstServiceStop = testListener.expectServiceStop(firstServiceName);
        // move secondService to NEVER mode
        secondController.setMode(Mode.NEVER);
        // firstService is expected to go down
        assertSame(firstController, firstServiceStop.get());
        // FIXME move firstService to NEVER mode 
        firstController.setMode(Mode.NEVER);

        // mark failService to fail again next time it is started
        failService.failNextTime();
        firstServiceDependencyFailed = testListener.expectDependencyFailure(firstServiceName);
        // change its mode back to ON_DEMAND
        secondController.setMode(Mode.ON_DEMAND);
        // trigger start process of secondService again
        firstController.setMode(Mode.ACTIVE);
        // dependencyFailure notification is expected
        assertSame(firstController, firstServiceDependencyFailed.get());

        firstServiceStart = testListener.expectServiceStart(firstServiceName);
        firstServiceDependencyFailureCleared = testListener.expectDependencyRetrying(firstServiceName);
        secondServiceRemoval = testListener.expectServiceRemoval(secondServiceName);
        // remove secondService after the failure
        secondController.setMode(Mode.REMOVE);
        // wait for the removal
        assertSame(secondController, secondServiceRemoval.get());
        // dependencyFailure is expected to be cleared
        assertSame(firstController, firstServiceDependencyFailureCleared.get());
        // firstService is expected to start
        assertSame(firstController, firstServiceStart.get());

        firstServiceStop = testListener.expectServiceStop(firstServiceName);
        // move firstService to NEVEr mode
        firstController.setMode(Mode.NEVER);
        // wait for it to go down
        assertNotNull(firstServiceStop.get());

        // install secondService and thirdService in a batch
        // secondService dependens on thirdService...
        BatchBuilder batch = serviceContainer.batchBuilder();
        batch.addService(secondServiceName, Service.NULL)
            .addDependency(thirdServiceName)
            .setInitialMode(Mode.ON_DEMAND)
            .addListener(testListener)
            .install();
        // ... a failToStart service, set to fail on the first attempt to start it
        batch.addService(thirdServiceName, new FailToStartService(true)).install();
        batch.install();
        Future<ServiceController<?>> secondServiceDependencyFailed = testListener.expectDependencyFailure(secondServiceName);
        firstServiceDependencyFailed = testListener.expectDependencyFailure(firstServiceName);
        // at this point, firstService is connected to the new secondService as an optional dependency
        // move it to active mode, to make all three services to start
        firstController.setMode(Mode.ACTIVE);
        // a dependencyFailure notification is expected by secondService
        secondController = secondServiceDependencyFailed.get();
        assertNotNull(secondController);
        assertSame(serviceContainer.getService(secondServiceName), secondController);
        // and it is expected to reach the entire dependent chain, including firstService
        assertSame(firstController, firstServiceDependencyFailed.get());

        firstServiceDependencyFailureCleared = testListener.expectDependencyRetrying(firstServiceName);
        firstServiceStart = testListener.expectServiceStart(firstServiceName);
        // remove secondService
        secondController.setMode(Mode.REMOVE);
        // a dependency retrying is expected
        assertSame(firstController, firstServiceDependencyFailureCleared.get());
        // firstService is expected to start
        assertSame(firstController, firstServiceStart.get());

        // during the entire test, firstService is not expected to notify of an uninstalled dependency
        assertNull(secondServiceUninstalled.get());
    }

    private static class FailToStartService implements Service<Void> {
        boolean fail = false;

        FailToStartService(boolean fail) {
            this.fail = fail;
        }

        @Override
        public void start(StartContext context) throws StartException {
            if (fail) {
                fail = false;
                throw new StartException("Second service failed");
            }
        }

        @Override
        public void stop(StopContext context) {
        }

        @Override
        public Void getValue() throws IllegalStateException {
            return null;
        }

        public void failNextTime() {
            fail = true;
        }
    }
}