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

package org.jboss.msc.service;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import java.util.concurrent.Future;

import org.jboss.msc.service.ServiceController.Mode;
import org.jboss.msc.service.ServiceController.State;
import org.jboss.msc.service.util.FailToStartService;
import org.jboss.msc.util.TestServiceListener;
import org.junit.Before;
import org.junit.Test;

/**
 * Test notifications sent to listeners related to dependency operations.
 * 
 * @author <a href="mailto:flavia.rainone@jboss.com">Flavia Rainone</a>
 * @see ServiceListener#dependencyFailed(ServiceController)
 * @see ServiceListener#dependencyFailureCleared(ServiceController)
 * @see ServiceListener#dependencyInstalled(ServiceController)
 * @see ServiceListener#dependencyUninstalled(ServiceController)
 */
public class DependencyListenersTestCase extends AbstractServiceTest {

    private static final ServiceName firstServiceName = ServiceName.of("firstService");
    private static final ServiceName secondServiceName = ServiceName.of("secondService");
    private static final ServiceName thirdServiceName = ServiceName.of("thirdService");
    private static final ServiceName fourthServiceName = ServiceName.of("fourthService");
    private static final ServiceName fifthServiceName = ServiceName.of("fifthService");
    private static final ServiceName sixthServiceName = ServiceName.of("sixthService");

    private TestServiceListener testListener;

    @Before
    public void setUpTestListener() {
        testListener = new TestServiceListener();
    }

    @Test
    public void testMissingDependencies() throws Exception {
        Future<ServiceController<?>> missingDependency = testListener.expectImmediateDependencyUninstall(firstServiceName);
        // add firstService with dependency on missing secondService
        final ServiceController<?> firstController = serviceContainer.addService(firstServiceName, Service.NULL)
            .addDependency(secondServiceName)
            .addListener(testListener)
            .install();
        assertController(firstServiceName, firstController);
        // uninstalled dependency notification expected
        assertController(firstController, missingDependency);
        // first controller should provide secondServiceName when requested about its immediate missing dependencies
        assertImmediateMissingDependencies(firstController, secondServiceName);

        final Future<ServiceController<?>> installDependency = testListener.expectImmediateDependencyInstall(firstServiceName);
        // install missing secondService
        final ServiceController<?> secondController = serviceContainer.addService(secondServiceName, Service.NULL).install();
        assertController(secondServiceName, secondController);
        // dependency installed notification expected
        assertController(firstController, installDependency);
        // no immediate missing dependency this time
        assertImmediateMissingDependencies(firstController);

        missingDependency = testListener.expectImmediateDependencyUninstall(firstServiceName);
        // remove secondService
        serviceContainer.getService(secondServiceName).setMode(Mode.REMOVE);
        // uninstalled dependency notification expected again
        assertController(firstController, missingDependency);
        // and yet again, second service is a missing dependency
        assertImmediateMissingDependencies(firstController, secondServiceName);
    }

    @Test
    public void testTransitiveMissingDependencies() throws Exception {
        Future<ServiceController<?>> firstServiceImmMissingDependency = testListener.expectImmediateDependencyUninstall(firstServiceName);
        // add firstService with dependency on missing secondService
        final ServiceController<?> firstController = serviceContainer.addService(firstServiceName, Service.NULL)
            .addDependency(secondServiceName)
            .addListener(testListener)
            .install();
        // uninstalled dependency notification expected
        assertController(firstServiceName, firstController);
        assertController(firstController, firstServiceImmMissingDependency);
        // first controller should provide secondServiceName when requested about its immediate missing dependencies
        assertImmediateMissingDependencies(firstController, secondServiceName);

        Future<ServiceController<?>> firstServiceImmInstalledDependency = testListener.expectImmediateDependencyInstall(firstServiceName);
        Future<ServiceController<?>> firstServiceTransMissingDependency = testListener.expectTransitiveDependencyUninstall(firstServiceName);
        Future<ServiceController<?>> secondServiceImmMissingDependency = testListener.expectImmediateDependencyUninstall(secondServiceName);
        // add secondService with dependency on missing thirdService
        final ServiceController<?> secondController = serviceContainer.addService(secondServiceName, Service.NULL)
            .addDependency(thirdServiceName)
            .addListener(testListener)
            .install();
        assertController(secondServiceName, secondController);
        assertController(firstController, firstServiceImmInstalledDependency);
        assertController(firstController, firstServiceTransMissingDependency);
        // uninstalled dependency notification expected from secondService
        assertController(secondController, secondServiceImmMissingDependency);
        // no immediate missing dependency this time for first service
        assertImmediateMissingDependencies(firstController);
        // but second controller should provide thirdServiceName when requested about its immediate missing dependencies
        assertImmediateMissingDependencies(secondController, thirdServiceName);

        Future<ServiceController<?>> firstServiceTransInstalledDependency = testListener.expectTransitiveDependencyInstall(firstServiceName);
        Future<ServiceController<?>> secondServiceImmInstalledDependency = testListener.expectImmediateDependencyInstall(secondServiceName);
        // install missing thirdService
        serviceContainer.addService(thirdServiceName, Service.NULL).install();
        // dependency installed notification expected
        assertController(secondController, secondServiceImmInstalledDependency);
        // dependency installed notification also expected from firstService
        assertController(firstController, firstServiceTransInstalledDependency);
        // no immediate missing dependency again for first service
        assertImmediateMissingDependencies(firstController);
        // no immediate missing dependency for second service
        assertImmediateMissingDependencies(secondController);

        firstServiceTransMissingDependency = testListener.expectTransitiveDependencyUninstall(firstServiceName);
        secondServiceImmMissingDependency = testListener.expectImmediateDependencyUninstall(secondServiceName);
        // remove thirdService
        serviceContainer.getService(thirdServiceName).setMode(Mode.REMOVE);
        // uninstalled dependency notification expected again
        assertController(secondController, secondServiceImmMissingDependency);
        // uninstalled dependency notification also expected from the rest of the dependent chain
        assertController(firstController, firstServiceTransMissingDependency);
        // no immediate missing dependency again for first service
        assertImmediateMissingDependencies(firstController);
        // but second controller should again provide thirdServiceName as a missing immediate dependency
        assertImmediateMissingDependencies(secondController, thirdServiceName);
    }

    @Test
    public void testMissingDependenciesNotifiedToNewDependent() throws Exception {
        Future<ServiceController<?>> firstServiceImmMissingDependency = testListener.expectImmediateDependencyUninstall(firstServiceName);
        // add firstService with dependency on missing secondService
        final ServiceController<?> firstController = serviceContainer.addService(firstServiceName, Service.NULL)
            .addDependency(secondServiceName)
            .addListener(testListener)
            .install();
        assertController(firstServiceName, firstController);
        // uninstalled dependency notification expected
        assertController(firstController, firstServiceImmMissingDependency);
        assertImmediateMissingDependencies(firstController, secondServiceName);

        Future<ServiceController<?>> fourthServiceTransMissingDependency = testListener.expectTransitiveDependencyUninstall(fourthServiceName);
        final ServiceController<?> fourthController = serviceContainer.addService(fourthServiceName, Service.NULL)
            .addDependency(firstServiceName)
            .addListener(testListener)
            .install();
        assertController(fourthServiceName, fourthController);
        assertController(fourthController, fourthServiceTransMissingDependency);
        // no immediate missing dependency for fourth service
        assertImmediateMissingDependencies(fourthController);

        Future<ServiceController<?>> firstServiceImmInstalledDependency = testListener.expectImmediateDependencyInstall(firstServiceName);
        Future<ServiceController<?>> firstServiceTransMissingDependency = testListener.expectTransitiveDependencyUninstall(firstServiceName);
        Future<ServiceController<?>> fourthServiceTransInstalledDependency = testListener.expectNoImmediateDependencyInstall(fourthServiceName);
        Future<ServiceController<?>> fourthServiceTransUninstalledDependency = testListener.expectNoImmediateDependencyUninstall(fourthServiceName);
        Future<ServiceController<?>> secondServiceImmMissingDependency = testListener.expectImmediateDependencyUninstall(secondServiceName);
        // add secondService with dependency on missing thirdService
        final ServiceController<?> secondController = serviceContainer.addService(secondServiceName, Service.NULL)
            .addDependency(thirdServiceName)
            .addListener(testListener)
            .install();
        assertController(secondServiceName, secondController);
        assertController(firstController, firstServiceImmInstalledDependency);
        assertController(firstController, firstServiceTransMissingDependency);
        // installed and uninstalled dependency notifications expected
        assertOppositeNotifications(fourthController, fourthServiceTransInstalledDependency, fourthServiceTransUninstalledDependency);
        // uninstalled dependency notification expected from secondService
        assertController(secondController, secondServiceImmMissingDependency);
        // only second service has an immediate missing dependency
        assertImmediateMissingDependencies(fourthController);
        assertImmediateMissingDependencies(firstController);
        assertImmediateMissingDependencies(secondController, thirdServiceName);

        Future<ServiceController<?>> firstServiceTransInstalledDependency = testListener.expectTransitiveDependencyInstall(firstServiceName);
        fourthServiceTransInstalledDependency = testListener.expectTransitiveDependencyInstall(fourthServiceName);
        final Future<ServiceController<?>> secondServiceImmInstalledDependency = testListener.expectImmediateDependencyInstall(secondServiceName);
        final Future<ServiceController<?>> thirdServiceStart = testListener.expectServiceStart(thirdServiceName);
        // install missing thirdService
        final ServiceController<?> thirdController = serviceContainer.addService(thirdServiceName, Service.NULL)
            .addListener(testListener).install();
        assertController(thirdServiceName, thirdController);
        // dependency installed notification expected
        assertController(secondController, secondServiceImmInstalledDependency);
        // dependency installed notification also expected from firstService
        assertController(firstController, firstServiceTransInstalledDependency);
        // and from fourthService
        assertController(fourthController, fourthServiceTransInstalledDependency);
        // thirdService is expected to start
        assertController(thirdController, thirdServiceStart);
        // no service has immediate dependencies this time
        assertImmediateMissingDependencies(firstController);
        assertImmediateMissingDependencies(fourthController);
        assertImmediateMissingDependencies(secondController);
        assertImmediateMissingDependencies(thirdController);

        fourthServiceTransMissingDependency = testListener.expectTransitiveDependencyUninstall(fourthServiceName);
        firstServiceTransMissingDependency = testListener.expectTransitiveDependencyUninstall(firstServiceName);
        secondServiceImmMissingDependency = testListener.expectImmediateDependencyUninstall(secondServiceName);
        // remove thirdService
        thirdController.setMode(Mode.REMOVE);
        // uninstalled dependency notification expected again
        assertController(secondController, secondServiceImmMissingDependency);
        // uninstalled dependency notification also expected from the rest of the dependent chain
        assertController(firstController, firstServiceTransMissingDependency);
        assertController(fourthController, fourthServiceTransMissingDependency);
        // now, second service has to provide thirdServiceName as an immediate missing dependency
        assertImmediateMissingDependencies(fourthController);
        assertImmediateMissingDependencies(firstController);
        assertImmediateMissingDependencies(secondController, thirdServiceName);
    }

    @Test
    public void testDependenciesFailed() throws Exception {
        Future<ServiceController<?>> firstServiceDependencyFailure = testListener.expectDependencyFailure(firstServiceName);
        Future<ServiceController<?>> secondServiceDependencyFailure = testListener.expectNoDependencyFailure(secondServiceName);
        // install firstService and secondService
        serviceContainer.addService(firstServiceName, Service.NULL)
            .addDependency(secondServiceName)
            .addListener(testListener)
            .install();
        // secondService will throw a StartException at first attempt to start
        serviceContainer.addService(secondServiceName, new FailToStartService(true)).install();

        // dependencyFailure notification expected from firstService
        final ServiceController<?> controller= assertController(firstServiceName, firstServiceDependencyFailure);
        // dependencyFailure notification not expected from secondService
        assertNull(secondServiceDependencyFailure.get());

        firstServiceDependencyFailure = testListener.expectNoDependencyFailure(firstServiceName);
        final Future<ServiceController<?>> firstServiceDependencyFailureClear = testListener.expectDependencyFailureCleared(firstServiceName);
        // retry to start service
        serviceContainer.getService(secondServiceName).setMode(Mode.NEVER);
        // dependencyFailureClear expected
        assertController(controller, firstServiceDependencyFailureClear);

        serviceContainer.getService(secondServiceName).setMode(Mode.ACTIVE);
        // no dependencyFailure expected this time
        assertNull(firstServiceDependencyFailure.get());
    }

    @Test
    public void testTransitiveDependenciesFailed() throws Exception {
        Future<ServiceController<?>> firstServiceDependencyFailure = testListener.expectDependencyFailure(firstServiceName);
        Future<ServiceController<?>> secondServiceDependencyFailure = testListener.expectDependencyFailure(secondServiceName);
        // install firstService and secondService
        serviceContainer.addService(firstServiceName, Service.NULL)
            .addDependency(secondServiceName)
            .addListener(testListener)
            .install();
        serviceContainer.addService(secondServiceName, Service.NULL)
        .addDependency(thirdServiceName)
        .addListener(testListener)
        .install();
        // thirdService will throw a StartException at first attempt to start
        serviceContainer.addService(thirdServiceName, new FailToStartService(true)).addListener(testListener).install();

        // dependencyFailure notification expected from secondService
        final ServiceController<?> secondController = assertController(secondServiceName, secondServiceDependencyFailure);
        // dependencyFailure notification also expected from firstService
        final ServiceController<?> firstController = assertController(firstServiceName, firstServiceDependencyFailure);

        secondServiceDependencyFailure = testListener.expectNoDependencyFailure(firstServiceName);
        final Future<ServiceController<?>> firstServiceDependencyFailureClear = testListener.expectDependencyFailureCleared(firstServiceName);
        final Future<ServiceController<?>> secondServiceDependencyFailureClear = testListener.expectDependencyFailureCleared(secondServiceName);
        // retry to start service
        serviceContainer.getService(thirdServiceName).setMode(Mode.NEVER);
        // dependencyFailureClear expected from both second and first services
        assertController(secondController, secondServiceDependencyFailureClear);
        assertController(firstController , firstServiceDependencyFailureClear);

        final Future<ServiceController<?>> firstServiceStart = testListener.expectServiceStart(firstServiceName);
        final Future<ServiceController<?>> secondServiceStart = testListener.expectServiceStart(secondServiceName);
        final Future<ServiceController<?>> thirdServiceStart = testListener.expectServiceStart(thirdServiceName);
        // retry to start service
        serviceContainer.getService(thirdServiceName).setMode(Mode.ACTIVE);
        assertController(thirdServiceName, thirdServiceStart);
        assertSame(secondController, secondServiceStart.get());
        assertSame(firstController, firstServiceStart.get());

        // no dependencyFailure expected this time
        assertNull(secondServiceDependencyFailure.get());
    }

    @Test
    public void testFailedServiceWithFailedDependencies() throws Throwable {
        final FailToStartService secondService = new FailToStartService(true);
        final FailToStartService thirdService = new FailToStartService(true);

        // install third service, a service set to fail on start
        Future<StartException> thirdServiceFailure = testListener.expectServiceFailure(thirdServiceName); 
        serviceContainer.addService(thirdServiceName, thirdService).addListener(testListener).install();
        final ServiceController<?> thirdController = assertFailure(thirdServiceName, thirdServiceFailure);

        // install second service, set to fail on start, and with a dependency on third service
        // second service is expected to notify the dependency failure
        final Future<ServiceController<?>> secondServiceInstall = testListener.expectListenerAdded(secondServiceName); 
        Future<ServiceController<?>> secondServiceDepFailed = testListener.expectDependencyFailure(secondServiceName);
        serviceContainer.addService(secondServiceName, secondService).addListener(testListener).addDependency(thirdServiceName).install();
        final ServiceController<?> secondController = assertController(secondServiceName, secondServiceInstall);
        assertController(secondController, secondServiceDepFailed);

        // install first service, with a dependency on second service; it is expected to notify the failed dependency
        Future<ServiceController<?>> firstServiceDepFailed =testListener.expectDependencyFailure(firstServiceName);
        serviceContainer.addService(firstServiceName, Service.NULL).addListener(testListener).addDependency(secondServiceName).install();
        final ServiceController<?> firstController = assertController(firstServiceName, firstServiceDepFailed);

        // set third service mode to NEVER, the dep failure will be cleared
        Future<ServiceController<?>> secondServiceDepFailureCleared = testListener.expectDependencyFailureCleared(secondServiceName);
        Future<ServiceController<?>> firstServiceDepFailureCleared = testListener.expectDependencyFailureCleared(firstServiceName);
        thirdController.setMode(Mode.NEVER);
        assertController(secondController, secondServiceDepFailureCleared);
        assertController(firstController, firstServiceDepFailureCleared);

        // set third service mode to ACTIVE, it should start without problems this time
        // however, second service will fail
        final Future<ServiceController<?>> thirdServiceStart = testListener.expectServiceStart(thirdServiceName);
        final Future<StartException> secondServiceFailure = testListener.expectServiceFailure(secondServiceName);
        firstServiceDepFailed = testListener.expectDependencyFailure(firstServiceName);
        thirdController.setMode(Mode.ACTIVE);
        assertController(thirdController, thirdServiceStart);
        assertFailure(secondController, secondServiceFailure);
        assertController(firstController, firstServiceDepFailed);

        // set third service mode to NEVER, it should stop
        final Future<ServiceController<?>> thirdServiceStop = testListener.expectServiceStop(thirdServiceName);
        thirdController.setMode(Mode.NEVER);
        assertController(thirdController, thirdServiceStop);

        // set third service to fail next attempt to start, and set its mode to ACTIVE; it will fail to start
        thirdService.failNextTime();
        secondServiceDepFailed = testListener.expectDependencyFailure(secondServiceName);
        thirdServiceFailure = testListener.expectServiceFailure(thirdServiceName);
        thirdController.setMode(Mode.ACTIVE);
        assertFailure(thirdController, thirdServiceFailure);
        assertController(secondController, secondServiceDepFailed);

        // the main goal of this test:
        // we don't expect a dependency failure cleared notification to firstController
        // because secondController failCount is still > 0
        secondController.setMode(Mode.NEVER);
        Thread.sleep(5000); // sleep because there is no way to be notified a service failure cleared

        // set third service mode to NEVER, now second controller will have a failCount of 0, and will
        // notify of the dep failure cleared
        secondServiceDepFailureCleared = testListener.expectDependencyFailureCleared(secondServiceName);
        firstServiceDepFailureCleared = testListener.expectDependencyFailureCleared(firstServiceName);
        thirdController.setMode(Mode.NEVER);
        assertController(secondServiceName, secondServiceDepFailureCleared);
        assertController(firstServiceName, firstServiceDepFailureCleared);
    }

    @Test
    public void testFailedDependencyInstalledFirst() throws Exception {
        final FailToStartService thirdService = new FailToStartService(true);
        final Future<StartException> thirdServiceFailed = testListener.expectServiceFailure(thirdServiceName);
        // thirdService will throw a StartException at first attempt to start
        serviceContainer.addService(thirdServiceName, thirdService).addListener(testListener).install();
        final ServiceController<?> thirdController = assertFailure(thirdServiceName, thirdServiceFailed);

        Future<ServiceController<?>> firstServiceDependencyFailure = testListener.expectDependencyFailure(firstServiceName);
        Future<ServiceController<?>> secondServiceDependencyFailure = testListener.expectDependencyFailure(secondServiceName);
        // now install firstService and secondService
        serviceContainer.addService(firstServiceName, Service.NULL)
            .addDependency(secondServiceName)
            .addListener(testListener)
            .install();
        serviceContainer.addService(secondServiceName, Service.NULL)
        .addDependency(thirdServiceName)
        .addListener(testListener)
        .install();

        // dependencyFailure notification expected from secondService
        final ServiceController<?> secondController = assertController(secondServiceName, secondServiceDependencyFailure);
        // dependencyFailure notification also expected from firstService
        final ServiceController<?> firstController = assertController(firstServiceName, firstServiceDependencyFailure);

        secondServiceDependencyFailure = testListener.expectNoDependencyFailure(firstServiceName);
        Future<ServiceController<?>> firstServiceDependencyFailureClear = testListener.expectDependencyFailureCleared(firstServiceName);
        Future<ServiceController<?>> secondServiceDependencyFailureClear = testListener.expectDependencyFailureCleared(secondServiceName);
        // retry to start service
        thirdController.setMode(Mode.NEVER);
        // dependencyFailureClear expected from both second and first services
        assertController(secondController, secondServiceDependencyFailureClear);
        assertController(firstController , firstServiceDependencyFailureClear);

        firstServiceDependencyFailure = testListener.expectDependencyFailure(firstServiceName);
        secondServiceDependencyFailure = testListener.expectDependencyFailure(secondServiceName);
        // set thirdService to fail again
        thirdService.failNextTime();
        // retry to start service
        thirdController.setMode(Mode.ACTIVE);
        // new serviceDependencyFailure expected from secondController and firstController
        assertController(secondController, secondServiceDependencyFailure);
        assertController(firstController, firstServiceDependencyFailure);

        final Future<ServiceController<?>> fourthServiceDependencyFailure = testListener.expectDependencyFailure(fourthServiceName);
        // install fourthService, dependent on firstService
        serviceContainer.addService(fourthServiceName, Service.NULL).addListener(testListener).addDependency(firstServiceName).install();
        final ServiceController<?> fourthController = assertController(fourthServiceName, fourthServiceDependencyFailure);

        secondServiceDependencyFailure = testListener.expectNoDependencyFailure(firstServiceName);
        firstServiceDependencyFailureClear = testListener.expectDependencyFailureCleared(firstServiceName);
        secondServiceDependencyFailureClear = testListener.expectDependencyFailureCleared(secondServiceName);
        final Future<ServiceController<?>> fourthServiceDependencyFailureClear = testListener.expectDependencyFailureCleared(fourthServiceName);
        // retry to start third service
        thirdController.setMode(Mode.NEVER);
        // dependencyFailureClear expected from both second and first services
        assertController(secondController, secondServiceDependencyFailureClear);
        assertController(firstController , firstServiceDependencyFailureClear);
        assertController(fourthController, fourthServiceDependencyFailureClear);

        final Future<ServiceController<?>> firstServiceStart = testListener.expectServiceStart(firstServiceName);
        final Future<ServiceController<?>> secondServiceStart = testListener.expectServiceStart(secondServiceName);
        final Future<ServiceController<?>> thirdServiceStart = testListener.expectServiceStart(thirdServiceName);
        final Future<ServiceController<?>> fourthServiceStart = testListener.expectServiceStart(fourthServiceName);
        // retry to start service
        thirdController.setMode(Mode.ACTIVE);
        assertController(thirdController, thirdServiceStart);
        assertController(secondController, secondServiceStart);
        assertController(firstController, firstServiceStart);
        assertController(fourthController, fourthServiceStart);
    }

    @Test
    public void testFailedDependencyUninstalled() throws Exception {
        final Future<StartException> secondServiceFailed = testListener.expectServiceFailure(secondServiceName);
        final Future<ServiceController<?>> firstServiceDependencyFailed = testListener.expectDependencyFailure(firstServiceName);
        // install second service, a fail to start service that should fail at the first attempt to start
        serviceContainer.addService(secondServiceName, new FailToStartService(true))
            .addListener(testListener)
            .install();        
        // also install first service, that depends on second service...
        serviceContainer.addService(firstServiceName, Service.NULL)
            .addListener(testListener)
            .addDependency(secondServiceName)
            .install();

        // second service should fail
        final ServiceController<?> secondController = assertFailure(secondServiceName, secondServiceFailed);
        // and first service should send a dep failed notification
        final ServiceController<?> firstController = assertController(firstServiceName, firstServiceDependencyFailed);

        final Future<ServiceController<?>> firstServiceDependencyFailureCleared = testListener.expectDependencyFailureCleared(firstServiceName);
        final Future<ServiceController<?>> firstServiceDependencyUninstalled = testListener.expectImmediateDependencyUninstall(firstServiceName);
        Thread.sleep(200);
        // remove second service
        secondController.setMode(Mode.REMOVE);
        // the failure is expected to have cleared
        assertController(firstController, firstServiceDependencyFailureCleared);
        // and a new missing dep is expected
        assertController(firstController, firstServiceDependencyUninstalled);
        assertImmediateMissingDependencies(firstController, secondServiceName);
    }

    @Test
    public void testDependencyWithFailuresAndMissingDeps() throws Exception {
        Future<ServiceController<?>> firstServiceImmDependencyMissing = testListener.expectImmediateDependencyUninstall(firstServiceName);
        // install first service with a dependency on the missing second service
        serviceContainer.addService(firstServiceName, Service.NULL)
            .addListener(testListener)
            .addDependency(secondServiceName)
            .install();
        // a missing dependency notification is expected
        final ServiceController<?> firstController = assertController(firstServiceName, firstServiceImmDependencyMissing);
        assertImmediateMissingDependencies(firstController, secondServiceName);

        final Future<ServiceController<?>> firstServiceImmDependencyInstalled = testListener.expectImmediateDependencyInstall(firstServiceName);
        final Future<ServiceController<?>> firstServiceTransDependencyMissing = testListener.expectTransitiveDependencyUninstall(firstServiceName);
        final Future<ServiceController<?>> secondServiceDependencyMissing = testListener.expectImmediateDependencyUninstall(secondServiceName);
        // install second service, with dependencies on the missing fourth and fifth services
        serviceContainer.addService(secondServiceName, Service.NULL)
            .addListener(testListener)
            .addDependencies(thirdServiceName, fourthServiceName)
            .install();
        // a missing dependency notification is expected from second service
        ServiceController<?> secondController = assertController(secondServiceName, secondServiceDependencyMissing);
        assertController(firstController, firstServiceImmDependencyInstalled);
        assertController(firstController, firstServiceTransDependencyMissing);
        assertImmediateMissingDependencies(firstController);
        assertImmediateMissingDependencies(secondController, thirdServiceName, fourthServiceName);

        // set mode of first service to never... nothing is expected this time
        firstController.setMode(Mode.NEVER);

        final Future<ServiceController<?>> secondServiceRemoved = testListener.expectServiceRemoval(secondServiceName);
        final Future<ServiceController<?>> firstServiceTransDependencyInstalled = testListener.expectTransitiveDependencyInstall(firstServiceName);
        firstServiceImmDependencyMissing = testListener.expectImmediateDependencyUninstall(firstServiceName);
        // remove second service.
        secondController.setMode(Mode.REMOVE);
        assertController(secondController, secondServiceRemoved);
        assertController(firstController, firstServiceTransDependencyInstalled);
        assertController(firstController, firstServiceImmDependencyMissing);
        assertImmediateMissingDependencies(firstController, secondServiceName);

        firstController.setMode(Mode.ACTIVE);

        final Future<StartException> fourthServiceFailure = testListener.expectServiceFailure(fourthServiceName);
        final Future<StartException> fifthServiceFailure = testListener.expectServiceFailure(fifthServiceName);
        // install fourth and fifth services, both set to fail at the first attempt to start
        serviceContainer.addService(fourthServiceName, new FailToStartService(true))
            .addListener(testListener)
            .install();
        serviceContainer.addService(fifthServiceName, new FailToStartService(true))
            .addListener(testListener)
            .install();
        // fourth and fifth services expected to fail to start
        assertFailure(fourthServiceName, fourthServiceFailure);
        assertFailure(fifthServiceName, fifthServiceFailure);

        final Future<ServiceController<?>> secondServiceDependencyFailure = testListener.expectDependencyFailure(secondServiceName);
        // reinstall second service, with dependencies on the missing third service, and on fourth service 
        serviceContainer.addService(secondServiceName, Service.NULL)
            .addListener(testListener)
            .addDependencies(thirdServiceName, fourthServiceName)
            .install();
        // a dependency failure is expected from second service, regarding the failure of fourth service to start 
        secondController = assertController(secondServiceName, secondServiceDependencyFailure);
        // first service is expected to stay still on the down state
        assertSame(State.DOWN, firstController.getState());
    }

    @Test
    public void testMultipleDependenciesWithFailure() throws Exception {
        final Future<StartException> thirdServiceFailure = testListener.expectServiceFailure(thirdServiceName);
        final Future<StartException> fourthServiceFailure = testListener.expectServiceFailure(fourthServiceName);
        final Future<StartException> fifthServiceFailure = testListener.expectServiceFailure(fifthServiceName);
        final Future<ServiceController<?>> secondServiceDependencyFailure = testListener.expectDependencyFailure(secondServiceName);
        final Future<ServiceController<?>> firstServiceDependencyFailure = testListener.expectDependencyFailure(firstServiceName);

        // install first, second, third, fourth, and fifth services
        // first service depends on second service
        serviceContainer.addService(firstServiceName, Service.NULL)
            .addListener(testListener)
            .addDependency(secondServiceName)
            .install();
        // second depends on third, fourth and fifth
        serviceContainer.addService(secondServiceName, Service.NULL)
            .addListener(testListener)
            .addDependencies(thirdServiceName, fourthServiceName, fifthServiceName)
            .install();
        // third service is set to fail on the first attempt to start
        serviceContainer.addService(thirdServiceName, new FailToStartService(true))
            .addListener(testListener)
            .install();
        // fourth service is also set to fail
        serviceContainer.addService(fourthServiceName, new FailToStartService(true))
            .addListener(testListener)
            .install();
        // as is fifth service
        serviceContainer.addService(fifthServiceName, new FailToStartService(true))
            .addListener(testListener)
            .install();

        // third, fourth and fifth services are expected to fail
        final ServiceController<?> thirdController = assertFailure(thirdServiceName, thirdServiceFailure);
        final ServiceController<?> fourthController = assertFailure(fourthServiceName, fourthServiceFailure);
        final ServiceController<?> fifthController = assertFailure(fifthServiceName, fifthServiceFailure);
        // a dependency failure notification is expected from second and first service
        final ServiceController<?> secondController = assertController(secondServiceName, secondServiceDependencyFailure);
        final ServiceController<?> firstController = assertController(firstServiceName, firstServiceDependencyFailure);

        final Future<ServiceController<?>> thirdServiceStop = testListener.expectFailedServiceStopped(thirdServiceName);
        // disable third service
        thirdController.setMode(Mode.NEVER);
        assertController(thirdController, thirdServiceStop);

        final Future<ServiceController<?>> thirdServiceStart = testListener.expectServiceStart(thirdServiceName);
        // renable third service, thus causing it to start
        thirdController.setMode(Mode.ACTIVE);
        assertController(thirdController, thirdServiceStart);

        final Future<ServiceController<?>> fourthServiceStop = testListener.expectFailedServiceStopped(fourthServiceName);
        // disable fourth service
        fourthController.setMode(Mode.NEVER);
        assertController(fourthController, fourthServiceStop);

        final Future<ServiceController<?>> fourthServiceStart = testListener.expectServiceStart(fourthServiceName);
        // re-enable fourth servuce, thus causing it to start
        fourthController.setMode(Mode.ACTIVE);
        assertController(fourthController, fourthServiceStart);

        final Future<ServiceController<?>> fifthServiceStop = testListener.expectFailedServiceStopped(fifthServiceName);
        final Future<ServiceController<?>> secondServiceDependencyFailureCleared = testListener.expectDependencyFailureCleared(secondServiceName);
        final Future<ServiceController<?>> firstServiceDependencyFailureCleared = testListener.expectDependencyFailureCleared(firstServiceName);
        // disable fifth service
        fifthController.setMode(Mode.NEVER);
        assertController(fifthController, fifthServiceStop);
        // with that, all three failures are expected to be cleared now, and, hence, a dep failure cleared notification
        // is expected from dependents
        assertController(secondController, secondServiceDependencyFailureCleared);
        assertController(firstController, firstServiceDependencyFailureCleared);

        final Future<ServiceController<?>> fifthServiceStart = testListener.expectServiceStart(fifthServiceName);
        final Future<ServiceController<?>> secondServiceStart = testListener.expectServiceStart(secondServiceName);
        final Future<ServiceController<?>> firstServiceStart = testListener.expectServiceStart(firstServiceName);
        // re-enable fifth service
        fifthController.setMode(Mode.ACTIVE);
        // as a result, fifth, second and first services are expected to start
        assertController(fifthController, fifthServiceStart);
        assertController(secondController, secondServiceStart);
        assertController(firstController, firstServiceStart);
    }

    @Test
    public void testMultipleMissingDependencies() throws Exception {
        final Future<ServiceController<?>> firstServiceTransDependencyMissing = testListener.expectTransitiveDependencyUninstall(firstServiceName);
        final Future<ServiceController<?>> secondServiceImmDependencyMissing = testListener.expectImmediateDependencyUninstall(secondServiceName);
        final Future<ServiceController<?>> secondServiceTransDependencyMissing = testListener.expectTransitiveDependencyUninstall(secondServiceName);
        final Future<ServiceController<?>> thirdServiceImmDependencyMissing = testListener.expectImmediateDependencyUninstall(thirdServiceName);

        // install first, second and third services
        // first service depends on second service
        serviceContainer.addService(firstServiceName, Service.NULL)
            .addListener(testListener)
            .addDependency(secondServiceName)
            .install();
        // second service depends on third, and on missing fourth and sixth services
        serviceContainer.addService(secondServiceName, Service.NULL)
            .addListener(testListener)
            .addDependencies(thirdServiceName, fourthServiceName, sixthServiceName)
            .install();
        // third service depends o missing fifth service
        serviceContainer.addService(thirdServiceName, Service.NULL)
            .addListener(testListener)
            .addDependencies(fifthServiceName)
            .install();

        // a dependency missing notification is expected from the three installed services
        final ServiceController<?> firstController = assertController(firstServiceName, firstServiceTransDependencyMissing);
        final ServiceController<?> secondController = assertController(secondServiceName, secondServiceImmDependencyMissing);
        assertController(secondController, secondServiceTransDependencyMissing);
        final ServiceController<?> thirdController = assertController(thirdServiceName, thirdServiceImmDependencyMissing);
        assertImmediateMissingDependencies(firstController); // no immediate missing dependencies for first service
        assertImmediateMissingDependencies(secondController, fourthServiceName, sixthServiceName);// second service has fourth and sixth services as missing deps
        assertImmediateMissingDependencies(thirdController, fifthServiceName);// third service has fifth service as missing dep

        final Future<ServiceController<?>> fourthServiceStart = testListener.expectServiceStart(fourthServiceName);
        // install fourth service
        serviceContainer.addService(fourthServiceName, Service.NULL).addListener(testListener).install();
        // which is expected to start immediately
        assertController(fourthServiceName, fourthServiceStart);
        assertImmediateMissingDependencies(secondController, sixthServiceName); // now second service has only sixth service as a missing dep

        final Future<ServiceController<?>> fifthServiceStart = testListener.expectServiceStart(fifthServiceName);
        final Future<ServiceController<?>> thirdServiceImmDependencyInstall = testListener.expectImmediateDependencyInstall(thirdServiceName);
        final Future<ServiceController<?>> thirdServiceStart = testListener.expectServiceStart(thirdServiceName);
        final Future<ServiceController<?>> secondServiceTransDependencyInstall = testListener.expectTransitiveDependencyInstall(secondServiceName);
        // install also fifth service
        serviceContainer.addService(fifthServiceName, Service.NULL).addListener(testListener).install();
        // which is also expected to start immediately
        assertController(fifthServiceName, fifthServiceStart);
        // third service missing dependencies are now all installed
        assertController(thirdController, thirdServiceImmDependencyInstall);
        // and, now, third service can start
        assertController(thirdController, thirdServiceStart);
        // and second service has no longer missing transitive dependencies
        assertController(secondController, secondServiceTransDependencyInstall);
        assertImmediateMissingDependencies(thirdController);// no immediate missing dependencies for third controller

        final Future<ServiceController<?>> sixthServiceStart = testListener.expectServiceStart(sixthServiceName);
        final Future<ServiceController<?>> secondServiceImmDependencyInstall = testListener.expectImmediateDependencyInstall(secondServiceName);
        final Future<ServiceController<?>> firstServiceTransDependencyInstall = testListener.expectTransitiveDependencyInstall(firstServiceName);
        final Future<ServiceController<?>> secondServiceStart = testListener.expectServiceStart(secondServiceName);
        final Future<ServiceController<?>> firstServiceStart = testListener.expectServiceStart(firstServiceName);
        // install the last missing dependency
        serviceContainer.addService(sixthServiceName, Service.NULL).addListener(testListener).install();
        // a dependency install is expected by both dependents
        assertController(secondController, secondServiceImmDependencyInstall);
        assertController(firstController, firstServiceTransDependencyInstall);
        // plus, all services are now expected to have started
        assertController(sixthServiceName, sixthServiceStart);
        assertController(secondController, secondServiceStart);
        assertController(firstController, firstServiceStart);
        assertImmediateMissingDependencies(secondController); // no missing immediate dependencies for second service
    }

    @Test
    public void testDependencyWithFailedDependenciesAndMissingDependencies() throws Exception {
        Future<StartException> fourthServiceFailed = testListener.expectServiceFailure(fourthServiceName);
        Future<ServiceController<?>> fifthServiceImmDependencyMissing = testListener.expectImmediateDependencyUninstall(fifthServiceName);
        Future<ServiceController<?>> thirdServiceDependencyFailure = testListener.expectDependencyFailure(thirdServiceName);
        Future<ServiceController<?>> thirdServiceTransDependencyMissing = testListener.expectTransitiveDependencyUninstall(thirdServiceName);
        // install fourth service, a fail to start service, set to fail at the first attempt to fail
        serviceContainer.addService(fourthServiceName, new FailToStartService(true))
            .addListener(testListener)
            .install();
        // fourth service is expected to have failed
        assertFailure(fourthServiceName, fourthServiceFailed);
        // install fifth service, with a dependency on the missing sixth service
        serviceContainer.addService(fifthServiceName, Service.NULL)
            .addListener(testListener)
            .addDependencies(sixthServiceName)
            .install();
        // fifth service should send a notification of a missing dep
        final ServiceController<?> fifthController = assertController(fifthServiceName, fifthServiceImmDependencyMissing);
        // ... and third service with dependencies on fourth and fifth services
        serviceContainer.addService(thirdServiceName, Service.NULL)
            .addListener(testListener)
            .addDependencies(fourthServiceName, fifthServiceName)
            .install();
        // the fourth service failure should reach third service
        final ServiceController<?> thirdController = assertController(thirdServiceName, thirdServiceDependencyFailure);
        // the same goes with the missing dependency
        assertController(thirdController, thirdServiceTransDependencyMissing);
        assertImmediateMissingDependencies(thirdController);// third service has no immediate missing dependencies
        assertImmediateMissingDependencies(fifthController, sixthServiceName); // fifth service has missing dependency sixth service

        Future<ServiceController<?>> secondServiceDependencyFailed = testListener.expectDependencyFailure(secondServiceName);
        Future<ServiceController<?>> secondServiceTransDependencyMissing = testListener.expectTransitiveDependencyUninstall(secondServiceName);
        // and second service, that depends on third service
        serviceContainer.addService(secondServiceName, Service.NULL)
            .addListener(testListener)
            .addDependency(thirdServiceName)
            .install();
        final ServiceController<?> secondController = assertController(secondServiceName, secondServiceDependencyFailed);
        assertController(secondController, secondServiceTransDependencyMissing);
        Future<ServiceController<?>> firstServiceDependencyFailed = testListener.expectDependencyFailure(firstServiceName);
        Future<ServiceController<?>> firstServiceTransDependencyMissing = testListener.expectTransitiveDependencyUninstall(firstServiceName);
        // install first service, that depends on second service
        serviceContainer.addService(firstServiceName, Service.NULL)
            .addListener(testListener)
            .addDependency(secondServiceName)
            .install();
        // both first and second services should receive the dep failed and missing dep notifications
        final ServiceController<?> firstController = assertController(firstServiceName, firstServiceDependencyFailed);
        assertController(firstController, firstServiceTransDependencyMissing);
        // neither first service nor second service have immediate missing dependencies
        assertImmediateMissingDependencies(firstController);
        assertImmediateMissingDependencies(secondController);

        Future<ServiceController<?>> sixthServiceStart = testListener.expectServiceStart(sixthServiceName);
        Future<ServiceController<?>> fifthServiceImmDependencyInstall = testListener.expectImmediateDependencyInstall(fifthServiceName);
        Future<ServiceController<?>> fifthServiceStart = testListener.expectServiceStart(fifthServiceName);
        Future<ServiceController<?>> thirdServiceTransDependencyInstall = testListener.expectTransitiveDependencyInstall(thirdServiceName);
        Future<ServiceController<?>> secondServiceTransDependencyInstall = testListener.expectTransitiveDependencyInstall(secondServiceName);
        Future<ServiceController<?>> firstServiceTransDependencyInstall = testListener.expectTransitiveDependencyInstall(firstServiceName);
        // install sixth service
        serviceContainer.addService(sixthServiceName, Service.NULL).
            addListener(testListener).
            install();
        // sixth service is expected to start
        final ServiceController<?> sixthController = assertController(sixthServiceName, sixthServiceStart);
        // and all services in the chain are expected to notify their listeners that the missing dependency is now
        // installed
        assertController(fifthController, fifthServiceImmDependencyInstall);
        assertController(fifthController, fifthServiceStart);
        assertController(thirdController, thirdServiceTransDependencyInstall);
        assertController(secondController, secondServiceTransDependencyInstall);
        assertController(firstController, firstServiceTransDependencyInstall);
        assertImmediateMissingDependencies(fifthController); // fifth service no longer has immediate missing deps

        final Future<ServiceController<?>> sixthServiceRemoval = testListener.expectServiceRemoval(sixthServiceName);
        fifthServiceImmDependencyMissing = testListener.expectImmediateDependencyUninstall(fifthServiceName);
        thirdServiceTransDependencyMissing = testListener.expectTransitiveDependencyUninstall(thirdServiceName);
        secondServiceTransDependencyMissing = testListener.expectTransitiveDependencyUninstall(secondServiceName);
        firstServiceTransDependencyMissing = testListener.expectTransitiveDependencyUninstall(firstServiceName);
        // remove sixth service
        Thread.sleep(100);
        sixthController.setMode(Mode.REMOVE);
        assertController(sixthController, sixthServiceRemoval);
        // all services in the dep chain of sixth service should send a missing dep notification
        assertController(fifthController, fifthServiceImmDependencyMissing);
        assertController(thirdController, thirdServiceTransDependencyMissing);
        assertController(secondController, secondServiceTransDependencyMissing);
        assertController(firstController, firstServiceTransDependencyMissing);
        assertImmediateMissingDependencies(fifthController, sixthServiceName);

        final Future<ServiceController<?>> thirdServiceDependencyFailureCleared = testListener.expectDependencyFailureCleared(thirdServiceName);
        final Future<ServiceController<?>> secondServiceDependencyFailureCleared = testListener.expectDependencyFailureCleared(secondServiceName);
        final Future<ServiceController<?>> firstServiceDependencyFailureCleared = testListener.expectDependencyFailureCleared(firstServiceName);
        final ServiceController<?> fourthController = serviceContainer.getService(fourthServiceName);
        // change fourth service to never mode
        fourthController.setMode(Mode.NEVER);
        // the dependency failure must be cleared in all dependents now
        assertController(thirdController, thirdServiceDependencyFailureCleared);
        assertController(secondController, secondServiceDependencyFailureCleared);
        assertController(firstController, firstServiceDependencyFailureCleared);

        final Future<ServiceController<?>> fourthServiceStart = testListener.expectServiceStart(fourthServiceName);
        // change fourth service to active mode
        fourthController.setMode(Mode.ACTIVE);
        // thus making the service start
        assertController(fourthController, fourthServiceStart);

        thirdServiceTransDependencyInstall = testListener.expectTransitiveDependencyInstall(thirdServiceName);
        final Future<ServiceController<?>> thirdServiceImmDependencyMissing = testListener.expectImmediateDependencyUninstall(thirdServiceName);
        secondServiceTransDependencyInstall = testListener.expectNoTransitiveDependencyInstall(secondServiceName);
        secondServiceTransDependencyMissing = testListener.expectNoTransitiveDependencyUninstall(secondServiceName);
        firstServiceTransDependencyInstall = testListener.expectNoTransitiveDependencyInstall(firstServiceName);
        firstServiceTransDependencyMissing = testListener.expectNoTransitiveDependencyUninstall(firstServiceName);
        final Future<ServiceController<?>> fifthServiceRemoved = testListener.expectServiceRemoval(fifthServiceName);
        // remove fifth service
        fifthController.setMode(Mode.REMOVE);
        assertController(fifthController, fifthServiceRemoved);
        assertController(thirdController, thirdServiceTransDependencyInstall);
        assertController(thirdController, thirdServiceImmDependencyMissing);
        // the missing dependency from fifth service to sixth service is now not missing anymore, but on the other hand
        // the fifth service is missing
        assertOppositeNotifications(secondController, secondServiceTransDependencyInstall, secondServiceTransDependencyMissing);
        assertOppositeNotifications(firstController, firstServiceTransDependencyInstall, firstServiceTransDependencyMissing);
        // third service has a new immediate missing dependency: fifthServiceName
        assertImmediateMissingDependencies(thirdController, fifthServiceName);

        final Future<ServiceController<?>> firstServiceStart = testListener.expectServiceStart(firstServiceName);
        final Future<ServiceController<?>> secondServiceStart = testListener.expectServiceStart(secondServiceName);
        final Future<ServiceController<?>> thirdServiceStart = testListener.expectServiceStart(thirdServiceName);
        fifthServiceStart = testListener.expectServiceStart(fifthServiceName);
        sixthServiceStart = testListener.expectServiceStart(sixthServiceName);
        final Future<ServiceController<?>> thirdServiceImmDependencyInstall = testListener.expectImmediateDependencyInstall(thirdServiceName);
        secondServiceTransDependencyInstall = testListener.expectTransitiveDependencyInstall(secondServiceName);
        firstServiceTransDependencyInstall = testListener.expectTransitiveDependencyInstall(firstServiceName);
        // install fifth service, that depends on sixth service
        serviceContainer.addService(fifthServiceName, Service.NULL)
            .addListener(testListener)
            .addDependency(sixthServiceName)
            .install();
        // a service without dependencies
        serviceContainer.addService(sixthServiceName, Service.NULL)
            .addListener(testListener)
            .install();
        // both services are expected to start
        assertController(sixthServiceName, sixthServiceStart);
        assertController(fifthServiceName, fifthServiceStart);
        // and the entire dependent chain is expected to notify of the installed dependencies
        assertController(thirdController, thirdServiceImmDependencyInstall);
        assertController(secondController, secondServiceTransDependencyInstall);
        assertController(firstController, firstServiceTransDependencyInstall);
        // and to start
        assertController(thirdController, thirdServiceStart);
        assertController(secondController, secondServiceStart);
        assertController(firstController, firstServiceStart);
        assertImmediateMissingDependencies(thirdController); // third service has no longer an immediate missing dependency

        final Future<ServiceController<?>> thirdServiceStop = testListener.expectServiceStop(thirdServiceName);
        final Future<ServiceController<?>> secondServiceStop = testListener.expectServiceStop(secondServiceName);
        final Future<ServiceController<?>> firstServiceStop = testListener.expectServiceStop(firstServiceName);
        // stop third service
        thirdController.setMode(Mode.NEVER);
        // third service and its dependents are expected to stop
        assertController(thirdController, thirdServiceStop);
        assertController(secondController, secondServiceStop);
        assertController(firstController, firstServiceStop);
    }

}