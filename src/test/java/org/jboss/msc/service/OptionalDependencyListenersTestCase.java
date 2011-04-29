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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import java.util.concurrent.Future;

import org.jboss.msc.service.ServiceBuilder.DependencyType;
import org.jboss.msc.service.ServiceController.Mode;
import org.jboss.msc.service.ServiceController.State;
import org.jboss.msc.service.util.FailToStartService;
import org.jboss.msc.util.TestServiceListener;
import org.junit.Before;
import org.junit.Test;

/**
 * Test notifications sent to listeners related to dependency operations with scenarios that involve one or more
 * optional dependencies.
 * 
 * @author <a href="mailto:flavia.rainone@jboss.com">Flavia Rainone</a>
 * @see ServiceListener#dependencyFailed(ServiceController)
 * @see ServiceListener#dependencyFailureCleared(ServiceController)
 * @see ServiceListener#dependencyInstalled(ServiceController)
 * @see ServiceListener#dependencyUninstalled(ServiceController)
 */
public class OptionalDependencyListenersTestCase extends AbstractServiceTest {
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
    public void testNotNotifiedOptionalFailedDependencyUninstalled() throws Exception {
        final Future<ServiceController<?>> firstServiceStart = testListener.expectServiceStart(firstServiceName);
        // install first service, with an optional dependency on missing second service
        serviceContainer.addService(firstServiceName, Service.NULL)
            .addListener(testListener)
            .addDependency(DependencyType.OPTIONAL, secondServiceName)
            .install();
        // first service is expected to start
        final ServiceController<?> firstController = assertController(firstServiceName, firstServiceStart);
        // no immediate missing dependency should be provided by first service
        assertImmediateUnavailableDependencies(firstController);

        final Future<StartException> secondServiceFailed = testListener.expectServiceFailure(secondServiceName);
        // install the missing second service, a fail to start service set to fail on the first attempt to start
        serviceContainer.addService(secondServiceName, new FailToStartService(true))
            .addListener(testListener)
            .install();
        // second service is expected to fail
        final ServiceController<?> secondController = assertFailure(secondServiceName, secondServiceFailed);
        // still no immediate missing dependencies for first service
        assertImmediateUnavailableDependencies(firstController);

        final Future<ServiceController<?>> firstServiceDependencyUninstalled = testListener.expectNoImmediateDependencyUnavailable(firstServiceName);
        // remove second service
        secondController.setMode(Mode.REMOVE);
        // no missing dependency notification is expected
        assertNull(firstServiceDependencyUninstalled.get());
        // first service continues to ignore the existence of second service as an immediate dependency
        assertImmediateUnavailableDependencies(firstController);
    }

    @Test
    public void testOptionalFailedDependencyUninstalledOnNeverMode() throws Exception {
        final Future<StartException> secondServiceFailed = testListener.expectServiceFailure(secondServiceName);
        final Future<ServiceController<?>> firstServiceNoDependencyFailed = testListener.expectNoDependencyFailure(firstServiceName);
        final Future<ServiceController<?>> firstServiceNoDependencyProblem = testListener.expectNoDependencyProblem(firstServiceName);

        //install second service, a fail to start service, set to fail at the first attempt to start
        serviceContainer.addService(secondServiceName, new FailToStartService(true))
            .addListener(testListener)
            .install();
        // second service is expected to fail
        assertFailure(secondServiceName, secondServiceFailed);

        // and install first service on initialMode, with a dependency on second service
        final ServiceController<?> firstController = serviceContainer.addService(firstServiceName, Service.NULL)
            .addListener(testListener)
            .addDependency(DependencyType.OPTIONAL, secondServiceName)
            .setInitialMode(Mode.NEVER)
            .install();
        // a dep failure notification should not be send by first service, since it is NEVER mode
        assertNull(firstServiceNoDependencyFailed.get());
        // no immediate missing dependency should be provided by first service, since second service is available
        assertImmediateUnavailableDependencies(firstController);

        final Future<ServiceController<?>> firstServiceNoDependencyFailureCleared = testListener.expectNoDependencyFailureCleared(firstServiceName);
        final Future<ServiceController<?>> firstServiceNoDependencyUninstalled = testListener.expectNoImmediateDependencyUnavailable(firstServiceName);
        // remove second service
        serviceContainer.getService(secondServiceName).setMode(Mode.REMOVE);
        // the dep failure should be cleared only internally, without listener notifications
        assertNull(firstServiceNoDependencyFailureCleared.get());
        // and a no missing dep notification is expected either
        assertNull(firstServiceNoDependencyUninstalled.get());
        // still no immediate missing dependency for first service, as second service was an optional dependency
        assertImmediateUnavailableDependencies(firstController);
        // no dependency problem should have been notified
        assertNull(firstServiceNoDependencyProblem.get());
    }

    @Test
    public void testOptionalFailedDependencyUninstalled() throws Exception {
        final Future<StartException> secondServiceFailed = testListener.expectServiceFailure(secondServiceName);
        final Future<ServiceController<?>> firstServiceDependencyFailed = testListener.expectDependencyFailure(firstServiceName);
        final Future<ServiceController<?>> firstServiceDependencyProblem = testListener.expectDependencyProblem(firstServiceName);

        //install second service, a fail to start service, set to fail at the first attempt to start
        serviceContainer.addService(secondServiceName, new FailToStartService(true))
            .addListener(testListener)
            .install();
        // second service is expected to fail
        assertFailure(secondServiceName, secondServiceFailed);

        // and install first service on initialMode, with a dependency on second service
        serviceContainer.addService(firstServiceName, Service.NULL)
            .addListener(testListener)
            .addDependency(DependencyType.OPTIONAL, secondServiceName)
            .install();
        // a dep failure notification should be send by first service
        final ServiceController<?> firstController = assertController(firstServiceName, firstServiceDependencyFailed);
        assertController(firstController, firstServiceDependencyProblem);
        // no immediate missing dependency should be provided by first service, since second service is available
        assertImmediateUnavailableDependencies(firstController);

        final Future<ServiceController<?>> firstServiceDependencyFailureCleared = testListener.expectDependencyFailureCleared(firstServiceName);
        final Future<ServiceController<?>> firstServiceDependencyProblemCleared = testListener.expectDependencyProblemCleared(firstServiceName);
        final Future<ServiceController<?>> firstServiceDependencyUninstalled = testListener.expectNoImmediateDependencyUnavailable(firstServiceName);
        // remove second service
        serviceContainer.getService(secondServiceName).setMode(Mode.REMOVE);
        // the dep failure should be cleared
        assertController(firstController, firstServiceDependencyFailureCleared);
        assertController(firstController, firstServiceDependencyProblemCleared);
        // and a new missing dep notification is expected
        assertNull(firstServiceDependencyUninstalled.get());
        // still no immediate missing dependency for first service, as second service was an optional dependency
        assertImmediateUnavailableDependencies(firstController);
    }

    @Test
    public void testOptionalDependencyUninstalled() throws Exception {
        final Future<ServiceController<?>> secondServiceImmDependencyMissing = testListener.expectImmediateDependencyUnavailable(secondServiceName);
        final Future<ServiceController<?>> secondServiceDependencyProblem = testListener.expectDependencyProblem(secondServiceName);
        // install second service, that depends on the missing third service
        serviceContainer.addService(secondServiceName, Service.NULL)
            .addListener(testListener)
            .addDependency(thirdServiceName)
            .install();
        // a missing dependency notification is expected from second services
        final ServiceController<?> secondController = assertController(secondServiceName, secondServiceImmDependencyMissing);
        assertController(secondController, secondServiceDependencyProblem);

        final Future<ServiceController<?>> firstServiceTransDependencyMissing = testListener.expectTransitiveDependencyUnavailable(firstServiceName);
        final Future<ServiceController<?>> firstServiceDependencyProblem = testListener.expectDependencyProblem(firstServiceName);
        // install first service, which has an optional dependency on second service
        serviceContainer.addService(firstServiceName, Service.NULL)
            .addListener(testListener)
            .addDependency(DependencyType.OPTIONAL, secondServiceName)
            .install();
        // a missing dependency notification is also expected from first service
        final ServiceController<?> firstController = assertController(firstServiceName, firstServiceTransDependencyMissing);
        assertController(firstController, firstServiceDependencyProblem);
        // no immediate missing dependency should be provided by first service
        assertImmediateUnavailableDependencies(firstController);

        final Future<ServiceController<?>> firstServiceTransDependencyInstall = testListener.expectTransitiveDependencyAvailable(firstServiceName);
        final Future<ServiceController<?>> firstServiceDependencyProblemCleared = testListener.expectDependencyProblemCleared(firstServiceName);
        final Future<ServiceController<?>> firstServiceStart = testListener.expectServiceStart(firstServiceName);
        // removing second service is expected to cause its disconnection from the optional dependent first service
        secondController.setMode(Mode.REMOVE);
        // thus resulting in a dependency install notification, and in first service starting
        assertController(firstController, firstServiceTransDependencyInstall);
        assertController(firstController, firstServiceDependencyProblemCleared);
        assertController(firstController, firstServiceStart);
        // still no immediate missing dependency for first service, as second service was an optional dependency
        assertImmediateUnavailableDependencies(firstController);
    }

    @Test
    public void testOptionalDependencyInstalledFirst2() throws Exception {
        Future<StartException> secondServiceFailed = testListener.expectServiceFailure(secondServiceName);
        Future<ServiceController<?>> thirdServiceStart = testListener.expectServiceStart(thirdServiceName);
        // install thirdService, a failToStart service, set to fail on the first attempt to start,
        // and dependent on third service
        serviceContainer.addService(secondServiceName, new FailToStartService(true))
            .addDependency(thirdServiceName)
            .addListener(testListener)
            .install();
        serviceContainer.addService(thirdServiceName, Service.NULL).addListener(testListener).install();
        final ServiceController<?> thirdController = assertController(thirdServiceName, thirdServiceStart);
        // secondService fails
        final ServiceController<?> secondController = assertFailure(secondServiceName, secondServiceFailed);
        // second Controller has no immediate missing dependencies
        assertImmediateUnavailableDependencies(secondController);

        final Future<ServiceController<?>> thirdServiceStop = testListener.expectServiceStop(thirdServiceName);
        final Future<ServiceController<?>> thirdServiceRemoval = testListener.expectServiceRemoval(thirdServiceName);
        final Future<ServiceController<?>> secondServiceImmDependencyMissing = testListener.expectImmediateDependencyUnavailable(secondServiceName);
        final Future<ServiceController<?>> secondServiceDependencyProblem = testListener.expectDependencyProblem(secondServiceName);
        // remove thirdService
        thirdController.setMode(Mode.REMOVE);
        // third service should stop, and second service should send a missing dep notification
        assertController(thirdController, thirdServiceStop);
        assertController(thirdController, thirdServiceRemoval);
        assertController(secondController, secondServiceImmDependencyMissing);
        assertController(secondController, secondServiceDependencyProblem);
        // second Controller now has one immediate missing dependency
        assertImmediateUnavailableDependencies(secondController, thirdServiceName);

        final Future<ServiceController<?>> firstServiceTransDependencyMissing = testListener.expectTransitiveDependencyUnavailable(firstServiceName);
        final Future<ServiceController<?>> firstServiceDependencyProblem = testListener.expectDependencyProblem(firstServiceName);
        // install firstService, a dependent on second service
        serviceContainer.addService(firstServiceName, Service.NULL)
            .addListener(testListener)
            .addDependency(DependencyType.OPTIONAL, secondServiceName)
            .install();
        // first service is expected to send a notification of missing dependency
        final ServiceController<?> firstController = assertController(firstServiceName, firstServiceTransDependencyMissing);
        assertController(firstController, firstServiceDependencyProblem);
        assertImmediateUnavailableDependencies(firstController); // first service nas no immediate dependencies

        thirdServiceStart = testListener.expectServiceStart(thirdServiceName);
        final Future<ServiceController<?>> secondServiceImmDependencyInstalled = testListener.expectImmediateDependencyAvailable(secondServiceName);
        final Future<ServiceController<?>> firstServiceTransDependencyInstalled = testListener.expectTransitiveDependencyAvailable(firstServiceName);
        final Future<ServiceController<?>> secondServiceDependencyProblemCleared = testListener.expectDependencyProblemCleared(secondServiceName);
        final Future<ServiceController<?>> firstServiceDependencyProblemCleared = testListener.expectDependencyProblemCleared(firstServiceName);
        final Future<ServiceController<?>> firstServiceStart = testListener.expectServiceStart(firstServiceName);
        final Future<ServiceController<?>> secondServiceStart = testListener.expectServiceStart(secondServiceName);
        // install third service
        serviceContainer.addService(thirdServiceName, Service.NULL)
            .addListener(testListener)
            .install();
        // third service is expected to start without issues
        assertController(thirdServiceName, thirdServiceStart);
        // second and first service should send a dep installed notification...
        assertController(secondController, secondServiceImmDependencyInstalled);
        assertController(secondController, secondServiceDependencyProblemCleared);
        assertController(firstController, firstServiceTransDependencyInstalled);
        assertController(firstController, firstServiceDependencyProblemCleared);
        // and start
        assertController(secondController, secondServiceStart);
        assertController(firstController, firstServiceStart);
        // no dependencies missing
        assertImmediateUnavailableDependencies(firstController);
        assertImmediateUnavailableDependencies(secondController);

        final Future<ServiceController<?>> firstServiceStopped = testListener.expectServiceRemoval(firstServiceName);
        firstController.setMode(Mode.REMOVE);
        assertSame(firstController, firstServiceStopped.get());
    }

    @Test
    public void testOptionalDependencyWithFailuresAndMissingDeps() throws Exception {
        Future<ServiceController<?>> firstServiceStart = testListener.expectServiceStart(firstServiceName);
        // install first service with an optional dependency on second service
        serviceContainer.addService(firstServiceName, Service.NULL)
            .addListener(testListener)
            .addDependency(DependencyType.OPTIONAL, secondServiceName)
            .install();
        // first service is expected to start
        final ServiceController<?> firstController = assertController(firstServiceName, firstServiceStart);
        // first service is not aware of any missing immediate dependencies
        assertImmediateUnavailableDependencies(firstController);

        Future<ServiceController<?>> secondServiceImmDependencyMissing = testListener.expectImmediateDependencyUnavailable(secondServiceName);
        Future<ServiceController<?>> secondServiceDependencyProblem = testListener.expectDependencyProblem(secondServiceName);
        // install second service with dependencies on the missing third and fourth services
        serviceContainer.addService(secondServiceName, Service.NULL)
            .addListener(testListener)
            .addDependencies(thirdServiceName, fourthServiceName)
            .install();
        // a dep missing notification is expected from second service
        ServiceController<?> secondController = assertController(secondServiceName, secondServiceImmDependencyMissing);
        assertController(secondController, secondServiceDependencyProblem);
        // this are the services that second service is missing as immediate dependencies
        assertImmediateUnavailableDependencies(secondController, thirdServiceName, fourthServiceName);
        // while first controller continues disconnected from second service, and hence is still in the up state
        assertSame(State.UP, firstController.getState());
        // first service does not miss any immediate dependency this time
        assertImmediateUnavailableDependencies(firstController);

        Future<ServiceController<?>> firstServiceStop = testListener.expectServiceStop(firstServiceName);
        Future<ServiceController<?>> firstServiceNoTransDependencyMissing = testListener.expectNoTransitiveDependencyUnavailable(firstServiceName);
        // disable first controller
        firstController.setMode(Mode.NEVER);
        assertController(firstController, firstServiceStop);
        // this should enable its connection with the optional dependency on second service, which wouldn't result in a 
        // dependency missing notification
        assertImmediateUnavailableDependencies(firstController); // still no immediate dependency missing

        final Future<ServiceController<?>> secondServiceRemoved = testListener.expectServiceRemoval(secondServiceName);
        final Future<ServiceController<?>> firstServiceNoTransDependencyInstall = testListener.expectNoTransitiveDependencyAvailable(firstServiceName);
        // remove second service
        secondController.setMode(Mode.REMOVE);
        assertController(secondController, secondServiceRemoved);
        // the result is that first service is disconnected from its optional dependency and sends a dependency install
        // notification
        assertNull(firstServiceNoTransDependencyInstall.get());
        // and, as second service is an optional dependency, first service ignores it has a missing dependency
        assertImmediateUnavailableDependencies(firstController);

        firstServiceStart = testListener.expectServiceStart(firstServiceName);
        // enable first service
        firstController.setMode(Mode.ACTIVE);
        // which results in the service start
        assertController(firstController, firstServiceStart);

        final Future<StartException> fourthServiceFailure = testListener.expectServiceFailure(fourthServiceName);
        final Future<StartException> fifthServiceFailure = testListener.expectServiceFailure(fifthServiceName);
        // install the missing fourth and fifth services
        // both set to fail on the first attempt to start
        serviceContainer.addService(fourthServiceName, new FailToStartService(true))
            .addListener(testListener)
            .install();
        serviceContainer.addService(fifthServiceName, new FailToStartService(true))
            .addListener(testListener)
            .install();
        assertFailure(fourthServiceName, fourthServiceFailure);
        assertFailure(fifthServiceName, fifthServiceFailure);

        final Future<ServiceController<?>> secondServiceDependencyFailure = testListener.expectDependencyFailure(secondServiceName);
        secondServiceImmDependencyMissing = testListener.expectImmediateDependencyUnavailable(secondServiceName);
        secondServiceDependencyProblem = testListener.expectDependencyProblem(secondServiceName);
        // reinstall second service
        serviceContainer.addService(secondServiceName, Service.NULL)
            .addListener(testListener)
            .addDependencies(thirdServiceName, fourthServiceName)
            .install();
        // a dependency failure notification is expected now from second service
        secondController = assertController(secondServiceName, secondServiceDependencyFailure);
        assertController(secondController, secondServiceImmDependencyMissing);
        assertController(secondController, secondServiceDependencyProblem);
        assertImmediateUnavailableDependencies(secondController, thirdServiceName);

        firstServiceStop = testListener.expectServiceStop(firstServiceName);
        final Future<ServiceController<?>> firstServiceNoDependencyFailure = testListener.expectNoDependencyFailure(firstServiceName);
        // disable first service
        firstController.setMode(Mode.NEVER);
        assertController(firstController, firstServiceStop);
        // thus causing it to connect with its optional dependency...
        // the result is not a dependency failure notification, regarding fourth and fifth service start failures
        // since first service is in never mode
        assertNull(firstServiceNoDependencyFailure.get());
        assertNull(firstServiceNoTransDependencyMissing.get());
    }

    @Test
    public void testOptionalDependencyInstalledFirst() throws Exception {
        final Future<ServiceController<?>> thirdServiceDependencyFailed = testListener.expectDependencyFailure(thirdServiceName);
        Future<ServiceController<?>> thirdServiceDependencyProblem = testListener.expectDependencyProblem(thirdServiceName);
        final Future<StartException> fourthServiceFailed = testListener.expectServiceFailure(fourthServiceName);
        // install thirdService and fourthService
        // thirdService depends on fourthService...
        serviceContainer.addService(thirdServiceName, Service.NULL)
            .addDependency(fourthServiceName)
            .setInitialMode(Mode.ACTIVE)
            .addListener(testListener)
            .install();
        // ... a failToStart service, set to fail on the first attempt to start it
        serviceContainer.addService(fourthServiceName, new FailToStartService(true)).addListener(testListener).setInitialMode(Mode.ON_DEMAND).install();
        // fourthService fails
        assertFailure(fourthServiceName, fourthServiceFailed);
        // a dependencyFailure notification is expected by thirdService
        ServiceController<?> thirdController = assertController(thirdServiceName, thirdServiceDependencyFailed);
        assertController(thirdController, thirdServiceDependencyProblem);

        final Future<ServiceController<?>> secondServiceNoDependencyFailed = testListener.expectNoDependencyFailure(secondServiceName);
        final Future<ServiceController<?>> firstServiceNoDependencyFailed = testListener.expectNoDependencyFailure(firstServiceName);
        // add firstService with a dependency on secondService
        final ServiceController<?> firstController = serviceContainer.addService(firstServiceName, Service.NULL)
            .addDependency(secondServiceName).setInitialMode(Mode.NEVER)
            .addListener(testListener)
            .install();
        // add secondService with an optional dependency on thirdService
        final ServiceController<?> secondController = serviceContainer.addService(secondServiceName, Service.NULL)
        .addDependency(DependencyType.OPTIONAL, thirdServiceName)
        .setInitialMode(Mode.NEVER)
        .addListener(testListener)
        .install();
        // and the first dependency failed message is expected to reach the entire dependent chain, including firstService
        // but first and second services won't report it as they are in NEVER mode
        assertNull(secondServiceNoDependencyFailed.get()); 
        assertNull(firstServiceNoDependencyFailed.get());

        final Future<ServiceController<?>> thirdServiceDependencyFailureCleared = testListener.expectDependencyFailureCleared(thirdServiceName);
        Future<ServiceController<?>> thirdServiceDependencyProblemCleared = testListener.expectDependencyProblemCleared(thirdServiceName);
        // set third service mode to never
        thirdController.setMode(Mode.NEVER);
        // the failure is expected to be cleared
        assertController(thirdController, thirdServiceDependencyFailureCleared);
        assertController(thirdController, thirdServiceDependencyProblemCleared);

        Future<ServiceController<?>> thirdServiceStart = testListener.expectServiceStart(thirdServiceName);
        Future<ServiceController<?>> fourthServiceStart = testListener.expectServiceStart(fourthServiceName);
        // set third service to active mode
        thirdController.setMode(Mode.ACTIVE);
        // third and fourth service must start at this point
        ServiceController<?> fourthController = assertController(fourthServiceName, fourthServiceStart);
        assertController(thirdController, thirdServiceStart);

        Future<ServiceController<?>> secondServiceStart = testListener.expectServiceStart(secondServiceName);
        // set second service mode to active
        secondController.setMode(Mode.ACTIVE);
        // second service is expected to start
        assertController(secondController, secondServiceStart);

        Future<ServiceController<?>> thirdServiceStop = testListener.expectServiceStop(thirdServiceName);
        Future<ServiceController<?>> thirdServiceImmMissingDependency = testListener.expectImmediateDependencyUnavailable(thirdServiceName);
        thirdServiceDependencyProblem = testListener.expectDependencyProblem(thirdServiceName);
        Future<ServiceController<?>> secondServiceStop = testListener.expectServiceStop(secondServiceName);
        Future<ServiceController<?>> secondServiceTransMissingDependency = testListener.expectTransitiveDependencyUnavailable(secondServiceName);
        final Future<ServiceController<?>> secondServiceDependencyProblem = testListener.expectDependencyProblem(secondServiceName);
        final Future<ServiceController<?>> fourthServiceRemoved = testListener.expectServiceRemoval(fourthServiceName);
        // remove fourth service
        fourthController.setMode(Mode.REMOVE);
        assertController(thirdController, thirdServiceStop);
        // the missing dependency is expected to reach the entire dependency chain
        assertController(thirdController, thirdServiceImmMissingDependency);
        assertController(thirdController, thirdServiceDependencyProblem);
        assertController(secondController, secondServiceTransMissingDependency);
        assertController(secondController, secondServiceDependencyProblem);
        // and second service should stop
        assertController(secondController, secondServiceStop);
        assertController(fourthController, fourthServiceRemoved);
        // only third service has an immediate missing dependency
        assertImmediateUnavailableDependencies(thirdController, fourthServiceName);
        assertImmediateUnavailableDependencies(secondController);
        assertImmediateUnavailableDependencies(firstController);

        Future<ServiceController<?>> secondServiceTransDependencyInstalled = testListener.expectTransitiveDependencyAvailable(secondServiceName);
        final Future<ServiceController<?>> secondServiceDependencyProblemCleared = testListener.expectDependencyProblemCleared(secondServiceName);
        Future<ServiceController<?>> thirdServiceImmDependencyInstalled = testListener.expectImmediateDependencyAvailable(thirdServiceName);
        thirdServiceDependencyProblemCleared = testListener.expectDependencyProblemCleared(thirdServiceName);
        fourthServiceStart = testListener.expectServiceStart(fourthServiceName);
        thirdServiceStart = testListener.expectServiceStart(thirdServiceName);
        secondServiceStart = testListener.expectServiceStart(secondServiceName);
        // install fourth service
        serviceContainer.addService(fourthServiceName, Service.NULL).addListener(testListener).install();
        fourthController = assertController(fourthServiceName, fourthServiceStart);
        // all dep chain must send the dependency installed notification
        assertController(thirdController, thirdServiceImmDependencyInstalled);
        assertController(thirdController, thirdServiceDependencyProblemCleared);
        assertController(secondController, secondServiceTransDependencyInstalled);
        assertController(secondController, secondServiceDependencyProblemCleared);
        // and second and third service should both start
        assertController(thirdController, thirdServiceStart);
        assertController(secondController, secondServiceStart);
        // no immediate missing dependencies for any service
        assertImmediateUnavailableDependencies(thirdController);
        assertImmediateUnavailableDependencies(secondController);
        assertImmediateUnavailableDependencies(firstController);

        secondServiceStop = testListener.expectServiceStop(secondServiceName);
        secondServiceStart = testListener.expectServiceStart(secondServiceName);
        final Future<ServiceController<?>> thirdServiceRemoved = testListener.expectServiceRemoval(thirdServiceName);
        // remove third service
        thirdController.setMode(Mode.REMOVE);
        // second service is expected to stop as the result of third service being stopped
        assertController(secondController, secondServiceStop);
        // and expected to start again as the third service removal results in the second service disconnection from
        // the optional dependency
        assertController(secondController, secondServiceStart);
        // second service ignores it has an immediate missing dep, given it is an optional dependency
        assertImmediateUnavailableDependencies(secondController);
        assertController(thirdController, thirdServiceRemoved);

        final Future<ServiceController<?>> fourthServiceRemoval = testListener.expectServiceRemoval(fourthServiceName);
        // remove fourth service
        fourthController.setMode(Mode.REMOVE);
        assertController(fourthController, fourthServiceRemoval);

        final Future<ServiceController<?>> thirdServiceImmDependencyMissing = testListener.expectImmediateDependencyUnavailable(thirdServiceName);
        thirdServiceDependencyProblem = testListener.expectDependencyProblem(thirdServiceName);
        final Future<ServiceController<?>> secondServiceTransDependencyMissing = testListener.expectNoTransitiveDependencyUnavailable(secondServiceName);
        // reinstall third service with dependency on missing fourth service
        serviceContainer.addService(thirdServiceName, Service.NULL)
            .addDependency(fourthServiceName)
            .setInitialMode(Mode.ACTIVE)
            .addListener(testListener)
            .install();
        // dependency missing notification expected
        thirdController = assertController(thirdServiceName, thirdServiceImmDependencyMissing);
        assertController(thirdController, thirdServiceDependencyProblem);
        // dependency missing notification is not expected from second service, as it is still disconnected from its
        // optional dependency
        assertNull(secondServiceTransDependencyMissing.get());
        // third service has again an immediate missing dependency
        assertImmediateUnavailableDependencies(thirdController, fourthServiceName);

        thirdServiceImmDependencyInstalled = testListener.expectImmediateDependencyAvailable(thirdServiceName);
        thirdServiceDependencyProblemCleared = testListener.expectDependencyProblemCleared(thirdServiceName);
        fourthServiceStart = testListener.expectServiceStart(fourthServiceName);
        // reinstall fourth service
        serviceContainer.addService(fourthServiceName, Service.NULL)
            .addListener(testListener)
            .install();
        // fourth service is expected to start
        fourthController = assertController(fourthServiceName, fourthServiceStart);
        // and third service should send a notification that the missing deps are now installed
        assertController(thirdController, thirdServiceImmDependencyInstalled);
        assertController(thirdController, thirdServiceDependencyProblemCleared);
        // third service has no longer an immediate missing dependency
        assertImmediateUnavailableDependencies(thirdController);
    }

    @Test
    public void testFailedOptionalDependency() throws Exception {
        Future<ServiceController<?>> firstServiceStart = testListener.expectServiceStart(firstServiceName);
        final Future<ServiceController<?>> firstServiceDepUninstalled = testListener.expectNoImmediateDependencyUnavailable(firstServiceName);
        // add firstService with an optional dependency on missing secondService
        serviceContainer.addService(firstServiceName, Service.NULL)
            .addDependency(DependencyType.OPTIONAL, secondServiceName)
            .addListener(testListener)
            .install();
        // firstService is expected to start
        final ServiceController<?> firstController = assertController(firstServiceName, firstServiceStart);
        assertImmediateUnavailableDependencies(firstController);

        Future<ServiceController<?>> secondServiceListenerAdded = testListener.expectListenerAdded(secondServiceName);
        Future<ServiceController<?>> secondServiceWaiting = testListener.expectServiceWaiting(secondServiceName);
        // install missing secondService on ON_DEMAND mode
        serviceContainer.addService(secondServiceName, Service.NULL)
            .addListener(testListener)
            .setInitialMode(Mode.ON_DEMAND)
            .install();
        ServiceController<?> secondController =  assertController(secondServiceName, secondServiceListenerAdded);
        // secondService is not expected to start at this point
        assertController(secondController, secondServiceWaiting);

        // move fistService to NEVER mode, and wait till it is stops
        Future<ServiceController<?>> firstServiceStop = testListener.expectServiceStop(firstServiceName);
        firstController.setMode(Mode.NEVER);
        assertController(firstController, firstServiceStop);

        // now it is expected that that firstService is connected with its optional dependency
        Future<ServiceController<?>> secondServiceStart = testListener.expectServiceStart(secondServiceName);
        firstServiceStart = testListener.expectServiceStart(firstServiceName);
        // check this by moving firstService to ACTIVE mode
        firstController.setMode(Mode.ACTIVE);
        assertController(firstController, firstServiceStart);
        // secondService is expected to start
        assertController(secondController, secondServiceStart);

        firstServiceStop = testListener.expectServiceStop(firstServiceName);
        Future<ServiceController<?>> secondServiceStop = testListener.expectServiceStop(secondServiceName);
        firstServiceStart = testListener.expectServiceStart(firstServiceName);
        // move secondService to NEVER mode
        secondController.setMode(Mode.NEVER);
        assertController(secondController, secondServiceStop);
        // firstService is expected to stop
        assertController(firstController, firstServiceStop);
        // first service expected to start
        assertController(firstController, firstServiceStart);
        assertImmediateUnavailableDependencies(firstController);

        Future<ServiceController<?>> secondServiceRemoval = testListener.expectServiceRemoval(secondServiceName);
        // uninstall secondService, thus disconnecting it from firstService as an optionalDependency
        secondController.setMode(Mode.REMOVE);
        // wait for removal to be complete
        assertController(secondController, secondServiceRemoval);
        assertSame(State.UP, firstController.getState());

        secondServiceListenerAdded = testListener.expectListenerAdded(secondServiceName);
        secondServiceWaiting = testListener.expectServiceWaiting(secondServiceName);
        // add a FailToStartService as a secondService, ON_DEMAND mode
        final FailToStartService failService = new FailToStartService(true);
        serviceContainer.addService(secondServiceName, failService)
            .setInitialMode(Mode.ON_DEMAND).addListener(testListener).install();
        secondController = assertController(secondServiceName, secondServiceListenerAdded);
        assertController(secondController, secondServiceWaiting);

        firstServiceStop = testListener.expectServiceStop(firstServiceName);
        // move firstService to NEVER mode, thus connecting it with secondService as an optionalDependency
        firstController.setMode(Mode.NEVER);
        assertController(firstController, firstServiceStop);

        Future<StartException> secondServiceFailed = testListener.expectServiceFailure(secondServiceName);
        Future<ServiceController<?>> firstServiceDependencyFailed = testListener.expectDependencyFailure(firstServiceName);
        Future<ServiceController<?>> firstServiceDependencyProblem = testListener.expectNoDependencyProblem(firstServiceName);
        // move firstService to ACTIVE mode, triggering the start process of secondService
        firstController.setMode(Mode.ACTIVE);
        // secondService is expected to fail
        assertFailure(secondServiceName, secondServiceFailed);
        // dependency failure notification is expected
        assertController(firstController, firstServiceDependencyFailed);
        assertController(firstController, firstServiceDependencyProblem);

        Future<ServiceController<?>> firstServiceDependencyFailureCleared = testListener.expectDependencyFailureCleared(firstServiceName);
        Future<ServiceController<?>> firstServiceDependencyProblemCleared = testListener.expectDependencyProblemCleared(firstServiceName);
        Future<ServiceController<?>> secondServiceFailedStopped = testListener.expectFailedServiceStopped(secondServiceName);
        // retry firstService start
        firstController.setMode(Mode.NEVER);
        // firstService is supposed to receive a dependencyFailureCleared notification
        assertController(firstController, firstServiceDependencyFailureCleared);
        assertController(firstController, firstServiceDependencyProblemCleared);
        assertController(secondController, secondServiceFailedStopped);

        firstServiceStart = testListener.expectServiceStart(firstServiceName);
        secondServiceStart = testListener.expectServiceStart(secondServiceName);
        firstController.setMode(Mode.ACTIVE);
        // no failures this time; firstService is expected to start now
        assertController(firstController, firstServiceStart);
        // as is secondService
        secondController = assertController(secondServiceName, secondServiceStart);

        secondServiceStop = testListener.expectServiceStop(secondServiceName);
        firstServiceStop = testListener.expectServiceStop(firstServiceName);
        firstServiceStart = testListener.expectServiceStart(firstServiceName);
        // move secondService to NEVER mode
        secondController.setMode(Mode.NEVER);
        assertController(secondController, secondServiceStop);
        // at a first moment, first service stops
        assertController(firstController, firstServiceStop);
        // but as soon as OptionalDependency gets the dependencyUnavailable notification, first service
        // disconnects from second service and starts
        assertController(firstController, firstServiceStart);

        // mark failService to fail again next time it is started
        failService.failNextTime();
        // change its mode back to ON_DEMAND
        secondController.setMode(Mode.ON_DEMAND);
        // second service won't fail this time, because it is disconnected from its optional dependent
        assertSame(State.DOWN, secondController.getState());

        // this time second service will start, and will fail, but is still disconnected from first Service
        secondServiceFailed = testListener.expectServiceFailure(secondServiceName);
        final Future<ServiceController<?>> firstServiceNoDependencyFailed = testListener.expectNoDependencyFailure(firstServiceName);
        secondController.setMode(Mode.ACTIVE);
        assertFailure(secondController, secondServiceFailed);
        // dependencyFailure notification is expected
        assertNull(firstServiceNoDependencyFailed.get());

        // move secondController to passive mode, nothing should happen
        secondController.setMode(Mode.PASSIVE);
        Thread.sleep(50);
        assertSame(State.START_FAILED, secondController.getState());

        // move second controller back to ON_DEMAND mode, it should stop because it is disconnected from its optional dependent
        secondServiceStop = testListener.expectFailedServiceStopped(secondServiceName);
        secondController.setMode(Mode.ON_DEMAND);
        assertController(secondController, secondServiceStop);
        assertSame(State.UP, firstController.getState());

        firstServiceStart = testListener.expectServiceStart(firstServiceName);
        firstServiceDependencyFailureCleared = testListener.expectNoDependencyFailureCleared(firstServiceName);
        secondServiceRemoval = testListener.expectServiceRemoval(secondServiceName);
        // remove secondService after the failure
        secondController.setMode(Mode.REMOVE);
        // wait for the removal
        assertController(secondController, secondServiceRemoval);
        // dependencyFailure is expected to be cleared, but first is disconnected from second service and won't notify it
        assertNull(firstServiceDependencyFailureCleared.get());
        // firstService is expected to be still up
        assertSame(State.UP, firstController.getState());

        firstServiceStop = testListener.expectServiceStop(firstServiceName);
        // move firstService to NEVER mode
        firstController.setMode(Mode.NEVER);
        // wait for it to go down
        assertController(firstController, firstServiceStop);

        secondServiceWaiting = testListener.expectServiceWaiting(secondServiceName);
        final Future<ServiceController<?>> secondServiceDependencyFailed = testListener.expectDependencyFailure(secondServiceName);
        firstServiceDependencyFailed = testListener.expectDependencyFailure(firstServiceName);
        firstServiceDependencyProblem = testListener.expectDependencyProblem(firstServiceName);
        // install thridService, a failToStart service, set to fail on the first attempt to start it
        serviceContainer.addService(thirdServiceName, new FailToStartService(true)).install();
        // install secondService, that depends on thirdService...
        serviceContainer.addService(secondServiceName, Service.NULL)
            .addDependency(thirdServiceName)
            .setInitialMode(Mode.ON_DEMAND)
            .addListener(testListener)
            .install();
        assertController(secondServiceName, secondServiceWaiting);
        // at this point, firstService is connected to the new secondService as an optional dependency
        // move it to active mode, to make all three services start
        firstController.setMode(Mode.ACTIVE);
        // a dependencyFailure notification is expected by secondService
        secondController = assertController(secondServiceName, secondServiceDependencyFailed);
        // and it is expected to reach the entire dependent chain, including firstService
        assertController(firstController, firstServiceDependencyFailed);
        assertController(firstController, firstServiceDependencyProblem);

        firstServiceDependencyFailureCleared = testListener.expectDependencyFailureCleared(firstServiceName);
        firstServiceDependencyProblemCleared = testListener.expectDependencyProblemCleared(firstServiceName);
        firstServiceStart = testListener.expectServiceStart(firstServiceName);
        secondServiceRemoval = testListener.expectServiceRemoval(secondServiceName);
        // remove secondService
        secondController.setMode(Mode.REMOVE);
        assertController(secondController, secondServiceRemoval);
        // a dependency retrying is expected
        assertController(firstController, firstServiceDependencyFailureCleared);
        assertController(firstController, firstServiceDependencyProblemCleared);
        // firstService is expected to start
        assertController(firstController, firstServiceStart);

        // during the entire test, firstService is not expected to notify of an uninstalled dependency
        assertNull(firstServiceDepUninstalled.get());
    }

    @Test
    public void testDisconnectedOptionalDependencyWithFailedAndMissingDependencies() throws Exception {
        final Future<ServiceController<?>> firstServiceStart = testListener.expectServiceStart(firstServiceName);
        final Future<ServiceController<?>> secondServiceStart = testListener.expectServiceStart(secondServiceName);
        final Future<ServiceController<?>> secondServiceDependencyFailed = testListener.expectNoDependencyFailure(secondServiceName);
        final Future<ServiceController<?>> secondServiceImmDependencyMissing = testListener.expectNoImmediateDependencyUnavailable(secondServiceName);
        final Future<ServiceController<?>> secondServiceDependencyProblem = testListener.expectNoDependencyProblem(secondServiceName);
        // install first and second services; first service depends on second service...
        serviceContainer.addService(firstServiceName, Service.NULL)
            .addListener(testListener)
            .addDependency(secondServiceName)
            .install();
        // .. and second service has an optional dependency on third service
        serviceContainer.addService(secondServiceName, Service.NULL)
            .addListener(testListener)
            .addDependency(DependencyType.OPTIONAL, thirdServiceName)
            .install();
        // both services should start normally
        final ServiceController<?> firstController = assertController(firstServiceName, firstServiceStart);
        final ServiceController<?> secondController = assertController(secondServiceName, secondServiceStart);
        // second service does not recognize is has an immediate missing dependency
        assertImmediateUnavailableDependencies(secondController);

        Future<StartException> fourthServiceFailed = testListener.expectServiceFailure(fourthServiceName);
        Future<ServiceController<?>> fifthServiceImmDependencyMissing = testListener.expectImmediateDependencyUnavailable(fifthServiceName);
        Future<ServiceController<?>> fifthServiceDependencyProblem = testListener.expectDependencyProblem(fifthServiceName);
        Future<ServiceController<?>> thirdServiceDependencyFailure = testListener.expectDependencyFailure(thirdServiceName);
        Future<ServiceController<?>> thirdServiceTransDependencyMissing = testListener.expectTransitiveDependencyUnavailable(thirdServiceName);
        Future<ServiceController<?>> thirdServiceDependencyProblem = testListener.expectDependencyProblem(thirdServiceName);
        // install third service, with a dependency on fourth and fifth services
        serviceContainer.addService(thirdServiceName, Service.NULL)
            .addListener(testListener)
            .addDependencies(fourthServiceName, fifthServiceName)
            .install();
        // ... fourth service is a fail to start service, set to fail on the first attempt to start
        serviceContainer.addService(fourthServiceName, new FailToStartService(true))
            .addListener(testListener)
            .install();
        // ... fifth service has a missing dependency on sixth service
        serviceContainer.addService(fifthServiceName, Service.NULL)
            .addListener(testListener)
            .addDependencies(sixthServiceName)
            .install();
        // fourth service should start
        assertNotNull(fourthServiceFailed.get());
        // the missing dependency notification (sixth service) should be sent by both fifth and third services 
        final ServiceController<?> fifthController = assertController(fifthServiceName, fifthServiceImmDependencyMissing);
        assertController(fifthController, fifthServiceDependencyProblem);
        final ServiceController<?> thirdController = assertController(thirdServiceName, thirdServiceTransDependencyMissing);
        // and the dependency failure notification should be send by third service
        assertController(thirdController, thirdServiceDependencyFailure);
        assertController(thirdController, thirdServiceDependencyProblem);
        // third service has no missing dependencies
        assertImmediateUnavailableDependencies(thirdController);
        // and fifth service has one missing dependency
        assertImmediateUnavailableDependencies(fifthController, sixthServiceName);
        // both first and second services must be still in the up state, as second service is currently disconnected
        // from its optional dependency on third service
        assertSame(State.UP, firstController.getState());
        assertSame(State.UP, secondController.getState());

        Future<ServiceController<?>> sixthServiceStart = testListener.expectServiceStart(sixthServiceName);
        Future<ServiceController<?>> fifthServiceImmDependencyInstall = testListener.expectImmediateDependencyAvailable(fifthServiceName);
        final Future<ServiceController<?>> fifthServiceDependencyProblemCleared = testListener.expectDependencyProblemCleared(fifthServiceName);
        Future<ServiceController<?>> fifthServiceStart = testListener.expectServiceStart(fifthServiceName);
        Future<ServiceController<?>> thirdServiceTransDependencyInstall = testListener.expectTransitiveDependencyAvailable(thirdServiceName);
        // install sixth service
        serviceContainer.addService(sixthServiceName, Service.NULL).
            addListener(testListener).
            install();
        final ServiceController<?> sixthController = assertController(sixthServiceName, sixthServiceStart);
        // the dependency install notification is expected from fifth and third service
        assertController(fifthController, fifthServiceImmDependencyInstall);
        assertController(fifthController, fifthServiceDependencyProblemCleared);
        assertController(thirdController, thirdServiceTransDependencyInstall);
        // fifth service can now start
        assertController(fifthController, fifthServiceStart);
        // fifth controller no longer returns sixthServiceName as an immediate missing dependency
        assertImmediateUnavailableDependencies(fifthController);
        // and first and second services must be in the up state, isolated from the other services
        assertSame(State.UP, firstController.getState());
        assertSame(State.UP, secondController.getState());

        final Future<ServiceController<?>> sixthServiceRemoval = testListener.expectServiceRemoval(sixthServiceName);
        fifthServiceImmDependencyMissing = testListener.expectImmediateDependencyUnavailable(fifthServiceName);
        fifthServiceDependencyProblem = testListener.expectDependencyProblem(fifthServiceName);
        thirdServiceTransDependencyMissing = testListener.expectTransitiveDependencyUnavailable(thirdServiceName);
        // remove sixth service
        sixthController.setMode(Mode.REMOVE);
        assertController(sixthController, sixthServiceRemoval);
        // the dependency missing notification is expected from both third and fifth services
        assertController(fifthController, fifthServiceImmDependencyMissing);
        assertController(fifthController, fifthServiceDependencyProblem);
        assertImmediateUnavailableDependencies(fifthController, sixthServiceName);
        assertController(thirdController, thirdServiceTransDependencyMissing);
        assertController(thirdController, thirdServiceDependencyProblem);
        assertImmediateUnavailableDependencies(thirdController);
        // meanwhile, first and second services are in the up state
        assertSame(State.UP, firstController.getState());
        assertSame(State.UP, secondController.getState());
        assertImmediateUnavailableDependencies(firstController);
        assertImmediateUnavailableDependencies(secondController);

        final Future<ServiceController<?>> thirdServiceDependencyFailureCleared = testListener.expectDependencyFailureCleared(thirdServiceName);
        final Future<ServiceController<?>> thirdServiceImmDependencyUnavailable = testListener.expectImmediateDependencyUnavailable(thirdServiceName);
        ServiceController<?> fourthController = serviceContainer.getService(fourthServiceName);
        // move fourth service to never mode, thus clearing the failure to start
        fourthController.setMode(Mode.NEVER);
        // dependency failure cleared expected from third service 
        assertController(thirdController, thirdServiceDependencyFailureCleared);
        assertController(thirdController, thirdServiceImmDependencyUnavailable);
        // first and second services are still in the up state
        assertSame(State.UP, firstController.getState());
        assertSame(State.UP, secondController.getState());

        final Future<ServiceController<?>> thirdServiceImmDependencyAvailable = testListener.expectImmediateDependencyAvailable(thirdServiceName);
        final Future<ServiceController<?>> fourthServiceStart = testListener.expectServiceStart(fourthServiceName);
        // renable fourth service, thus making it start
        fourthController.setMode(Mode.ACTIVE);
        assertController(fourthController, fourthServiceStart);
        assertController(thirdController, thirdServiceImmDependencyAvailable);
        // first and second services still in the up state
        assertSame(State.UP, firstController.getState());
        assertSame(State.UP, secondController.getState());

        thirdServiceTransDependencyInstall = testListener.expectTransitiveDependencyAvailable(thirdServiceName);
        final Future<ServiceController<?>> thirdServiceImmDependencyMissing = testListener.expectImmediateDependencyUnavailable(thirdServiceName);
        final Future<ServiceController<?>> fifthServiceRemoved = testListener.expectServiceRemoval(fifthServiceName);
        // remove fifth service
        fifthController.setMode(Mode.REMOVE);
        assertController(fifthController, fifthServiceRemoved);
        // now, the missing dependency on sixth service no longer exists, but we have a new missing dependency, on fifth service
        assertController(thirdController, thirdServiceTransDependencyInstall);
        assertController(thirdController, thirdServiceImmDependencyMissing);
        // third controller returns fifthServiceName as a missing immediate dependency
        assertImmediateUnavailableDependencies(thirdController, fifthServiceName);
        // first and second services still in the up state
        assertSame(State.UP, firstController.getState());
        assertSame(State.UP, secondController.getState());

        final Future<ServiceController<?>> thirdServiceStart = testListener.expectServiceStart(thirdServiceName);
        fifthServiceStart = testListener.expectServiceStart(fifthServiceName);
        sixthServiceStart = testListener.expectServiceStart(sixthServiceName);
        final Future<ServiceController<?>> thirdServiceImmDependencyInstall = testListener.expectImmediateDependencyAvailable(thirdServiceName);
        Future<ServiceController<?>> thirdServiceDependencyProblemCleared = testListener.expectDependencyProblemCleared(thirdServiceName);
        // reinstall fifth and sixth services
        serviceContainer.addService(fifthServiceName, Service.NULL)
            .addListener(testListener)
            .addDependency(sixthServiceName)
            .install();
        serviceContainer.addService(sixthServiceName, Service.NULL)
            .addListener(testListener)
            .install();
        assertController(sixthServiceName, sixthServiceStart);
        assertController(fifthServiceName, fifthServiceStart);
        // a dependency install notification is expected from third service
        assertController(thirdController, thirdServiceImmDependencyInstall);
        assertController(thirdController, thirdServiceDependencyProblemCleared);
        // third service has no missing dependencies this time
        assertImmediateUnavailableDependencies(thirdController);
        // which can now start
        assertController(thirdController, thirdServiceStart);
        // despite that, nothing has changed with the state of first and second services
        assertSame(State.UP, firstController.getState());
        assertSame(State.UP, secondController.getState());

        final Future<ServiceController<?>> thirdControllerStop = testListener.expectServiceStop(thirdServiceName);
        // disable third service
        thirdController.setMode(Mode.NEVER);
        assertController(thirdController, thirdControllerStop);
        // first and second services still running
        assertSame(State.UP, firstController.getState());
        assertSame(State.UP, secondController.getState());

        // during the entire test, as  second service has been disconnected from its optional dependency all the time,
        // no notification of dependency failures or missing dependencies should have reached second service
        assertNull(secondServiceDependencyFailed.get());
        assertNull(secondServiceImmDependencyMissing.get());
        assertNull(secondServiceDependencyProblem.get());
    }

    @Test
    public void testOptionalDependencyWithFailedAndMissingDependencies() throws Exception {
        Future<StartException> fourthServiceFailed = testListener.expectServiceFailure(fourthServiceName);
        Future<ServiceController<?>> fifthServiceImmDependencyMissing = testListener.expectImmediateDependencyUnavailable(fifthServiceName);
        Future<ServiceController<?>> fifthServiceDependencyProblem = testListener.expectDependencyProblem(fifthServiceName);
        Future<ServiceController<?>> thirdServiceDependencyFailure = testListener.expectDependencyFailure(thirdServiceName);
        Future<ServiceController<?>> thirdServiceTransDependencyMissing = testListener.expectTransitiveDependencyUnavailable(thirdServiceName);
        Future<ServiceController<?>> thirdServiceDependencyProblem = testListener.expectDependencyProblem(thirdServiceName);
        // install third service with dependency on fourth and fifth services
        serviceContainer.addService(thirdServiceName, Service.NULL)
            .addListener(testListener)
            .addDependencies(fourthServiceName, fifthServiceName)
            .install();
        // fourth service is a fail to start service, set to fail on the first attempt to start
        serviceContainer.addService(fourthServiceName, new FailToStartService(true))
            .addListener(testListener)
            .install();
        // fifth service has a missing dependency on sixth service
        serviceContainer.addService(fifthServiceName, Service.NULL)
            .addListener(testListener)
            .addDependencies(sixthServiceName)
            .install();
        // fourth service is expected to fail
        assertFailure(fourthServiceName, fourthServiceFailed);
        // fifth service should notify of a missing dependency
        final ServiceController<?> fifthController = assertController(fifthServiceName, fifthServiceImmDependencyMissing);
        assertController(fifthController, fifthServiceDependencyProblem);
        // and third service should notify of the dependency failure (fourth service)
        final ServiceController<?> thirdController = assertController(thirdServiceName, thirdServiceDependencyFailure);
        // plus, of the missing dependency (fifth service -> sixth service)
        assertController(thirdController, thirdServiceTransDependencyMissing);
        assertController(thirdController, thirdServiceDependencyProblem);
        // fifth serivce have one immediate missing dependency, and third service has none
        assertImmediateUnavailableDependencies(fifthController, sixthServiceName);
        assertImmediateUnavailableDependencies(thirdController);

        Future<ServiceController<?>> secondServiceDependencyFailed = testListener.expectDependencyFailure(secondServiceName);
        Future<ServiceController<?>> secondServiceTransDependencyMissing = testListener.expectTransitiveDependencyUnavailable(secondServiceName);
        Future<ServiceController<?>> secondServiceDependencyProblem = testListener.expectDependencyProblem(secondServiceName);
        // install second service with a dependency on third service...
        serviceContainer.addService(secondServiceName, Service.NULL)
            .addListener(testListener)
            .addDependency(DependencyType.OPTIONAL, thirdServiceName)
            .install();
        // second service should notify the dependency failure and the missing dependency
        final ServiceController<?> secondController = assertController(secondServiceName, secondServiceDependencyFailed);
        assertController(secondController, secondServiceTransDependencyMissing);
        assertController(secondController, secondServiceDependencyProblem);

        Future<ServiceController<?>> firstServiceDependencyFailed = testListener.expectDependencyFailure(firstServiceName);
        Future<ServiceController<?>> firstServiceTransDependencyMissing = testListener.expectTransitiveDependencyUnavailable(firstServiceName);
        Future<ServiceController<?>> firstServiceDependencyProblem = testListener.expectDependencyProblem(firstServiceName);
        // ... and install first service with a dependency on second service
        serviceContainer.addService(firstServiceName, Service.NULL)
            .addListener(testListener)
            .addDependency(secondServiceName)
            .install();
        // first service should also notify the missing/failed dependencies
        final ServiceController<?> firstController = assertController(firstServiceName, firstServiceDependencyFailed);
        assertController(firstController, firstServiceTransDependencyMissing);
        assertController(firstController, firstServiceDependencyProblem);
        // neither first nor second service has any immediate missing dependencies 
        assertImmediateUnavailableDependencies(firstController);
        assertImmediateUnavailableDependencies(secondController);

        Future<ServiceController<?>> sixthServiceStart = testListener.expectServiceStart(sixthServiceName);
        Future<ServiceController<?>> fifthServiceImmDependencyInstall = testListener.expectImmediateDependencyAvailable(fifthServiceName);
        final Future<ServiceController<?>> fifthServiceDependencyProblemCleared = testListener.expectDependencyProblemCleared(fifthServiceName);
        Future<ServiceController<?>> fifthServiceStart = testListener.expectServiceStart(fifthServiceName);
        Future<ServiceController<?>> thirdServiceTransDependencyInstall = testListener.expectTransitiveDependencyAvailable(thirdServiceName);
        Future<ServiceController<?>> secondServiceTransDependencyInstall = testListener.expectTransitiveDependencyAvailable(secondServiceName);
        Future<ServiceController<?>> firstServiceTransDependencyInstall = testListener.expectTransitiveDependencyAvailable(firstServiceName);
        // install sixth service
        serviceContainer.addService(sixthServiceName, Service.NULL).
            addListener(testListener).
            install();
        final ServiceController<?> sixthController = assertController(sixthServiceName, sixthServiceStart);
        // the dependency install notification is supposed to reach the entire dependent chain
        assertController(fifthController, fifthServiceImmDependencyInstall);
        assertController(fifthController, fifthServiceDependencyProblemCleared);
        assertController(thirdController, thirdServiceTransDependencyInstall);
        assertController(secondController, secondServiceTransDependencyInstall);
        assertController(firstController, firstServiceTransDependencyInstall);
        // and fifth service can finally start
        assertController(fifthController, fifthServiceStart);
        // no service has an immediate missing dependency this time
        assertImmediateUnavailableDependencies(sixthController);
        assertImmediateUnavailableDependencies(fifthController);
        assertImmediateUnavailableDependencies(thirdController);
        assertImmediateUnavailableDependencies(secondController);
        assertImmediateUnavailableDependencies(firstController);

        final Future<ServiceController<?>> sixthServiceRemoval = testListener.expectServiceRemoval(sixthServiceName);
        final Future<ServiceController<?>> fifthServiceStop = testListener.expectServiceStop(fifthServiceName);
        fifthServiceImmDependencyMissing = testListener.expectImmediateDependencyUnavailable(fifthServiceName);
        fifthServiceDependencyProblem = testListener.expectDependencyProblem(fifthServiceName);
        thirdServiceTransDependencyMissing = testListener.expectTransitiveDependencyUnavailable(thirdServiceName);
        secondServiceTransDependencyMissing = testListener.expectTransitiveDependencyUnavailable(secondServiceName);
        firstServiceTransDependencyMissing = testListener.expectTransitiveDependencyUnavailable(firstServiceName);
        // remove sixth service
        sixthController.setMode(Mode.REMOVE);
        assertController(sixthController, sixthServiceRemoval);
        // the entire chain should send the dependency uninstalled notification
        assertController(fifthController, fifthServiceImmDependencyMissing);
        assertController(fifthController, fifthServiceDependencyProblem);
        assertController(thirdController, thirdServiceTransDependencyMissing);
        assertController(secondController, secondServiceTransDependencyMissing);
        assertController(firstController, firstServiceTransDependencyMissing);
        // plus, fifth service should stop because of the uninstalled dep
        assertController(fifthController, fifthServiceStop);
        // fifth service has an immediate missing dependencies
        assertImmediateUnavailableDependencies(fifthController, sixthServiceName);
        // apart from fifth service, all the other services have no immediate missing dependency this time
        assertImmediateUnavailableDependencies(thirdController);
        assertImmediateUnavailableDependencies(secondController);
        assertImmediateUnavailableDependencies(firstController);

        final Future<ServiceController<?>> thirdServiceDependencyFailureCleared = testListener.expectDependencyFailureCleared(thirdServiceName);
        final Future<ServiceController<?>> thirdServiceImmDependencyUnavailable = testListener.expectImmediateDependencyUnavailable(thirdServiceName);
        final Future<ServiceController<?>> secondServiceDependencyFailureCleared = testListener.expectDependencyFailureCleared(secondServiceName);
        final Future<ServiceController<?>> firstServiceDependencyFailureCleared = testListener.expectDependencyFailureCleared(firstServiceName);
        final ServiceController<?> fourthController = serviceContainer.getService(fourthServiceName);
        // move fourth controller to never mode
        fourthController.setMode(Mode.NEVER);
        // the failure should be cleared
        assertController(thirdController, thirdServiceDependencyFailureCleared);
        assertController(thirdController, thirdServiceImmDependencyUnavailable);
        assertController(secondController, secondServiceDependencyFailureCleared);
        assertController(firstController, firstServiceDependencyFailureCleared);

        final Future<ServiceController<?>> fourthServiceStart = testListener.expectServiceStart(fourthServiceName);
        final Future<ServiceController<?>> thirdServiceImmDependencyAvailable = testListener.expectImmediateDependencyAvailable(thirdServiceName);
        // move fourth service back to active mode triggers a new attempt to start
        fourthController.setMode(Mode.ACTIVE);
        // .. that is successful this time
        assertController(fourthController, fourthServiceStart);
        assertController(thirdController, thirdServiceImmDependencyAvailable);

        thirdServiceTransDependencyInstall = testListener.expectTransitiveDependencyAvailable(thirdServiceName);
        final Future<ServiceController<?>> thirdServiceImmDependencyMissing = testListener.expectImmediateDependencyUnavailable(thirdServiceName);
        secondServiceTransDependencyInstall = testListener.expectNoTransitiveDependencyAvailable(secondServiceName);
        firstServiceTransDependencyInstall = testListener.expectNoTransitiveDependencyAvailable(firstServiceName);
        secondServiceTransDependencyMissing = testListener.expectNoTransitiveDependencyUnavailable(secondServiceName);
        firstServiceTransDependencyMissing = testListener.expectNoTransitiveDependencyUnavailable(firstServiceName);
        final Future<ServiceController<?>> fifthServiceRemoved = testListener.expectServiceRemoval(fifthServiceName);
        // remove fifth service
        fifthController.setMode(Mode.REMOVE);
        assertController(fifthController, fifthServiceRemoved);
        // the dependent chain should notify both of the no more uninstalled dependency on sixth service (the service
        // is still uninstalled, but with the removal of fifth service, the dependency no longer exists)
        // and also a notification of the new missing dependency is expected
        assertController(thirdController, thirdServiceTransDependencyInstall);
        assertController(thirdController, thirdServiceImmDependencyMissing);
        assertOppositeNotifications(secondController, secondServiceTransDependencyInstall, secondServiceTransDependencyMissing);
        assertOppositeNotifications(firstController, firstServiceTransDependencyInstall, firstServiceTransDependencyMissing);
        // third service should return fifthServiceName as an immediate missing dependency
        assertImmediateUnavailableDependencies(thirdController, fifthServiceName);
        // but second and first services continue having no immediate missing dependencies
        assertImmediateUnavailableDependencies(secondController);
        assertImmediateUnavailableDependencies(firstController);

        Future<ServiceController<?>> firstServiceStart = testListener.expectServiceStart(firstServiceName);
        Future<ServiceController<?>> secondServiceStart = testListener.expectServiceStart(secondServiceName);
        final Future<ServiceController<?>> thirdServiceStart = testListener.expectServiceStart(thirdServiceName);
        fifthServiceStart = testListener.expectServiceStart(fifthServiceName);
        sixthServiceStart = testListener.expectServiceStart(sixthServiceName);
        final Future<ServiceController<?>> thirdServiceImmDependencyInstall = testListener.expectImmediateDependencyAvailable(thirdServiceName);
        final Future<ServiceController<?>> thirdServiceDependencyProblemCleared = testListener.expectDependencyProblemCleared(thirdServiceName);
        secondServiceTransDependencyInstall = testListener.expectTransitiveDependencyAvailable(secondServiceName);
        final Future<ServiceController<?>> secondServiceDependencyProblemCleared = testListener.expectDependencyProblemCleared(secondServiceName);
        firstServiceTransDependencyInstall = testListener.expectTransitiveDependencyAvailable(firstServiceName);
        final Future<ServiceController<?>> firstServiceDependencyProblemCleared = testListener.expectDependencyProblemCleared(firstServiceName);
        // reinstall fifth and sixth services
        serviceContainer.addService(fifthServiceName, Service.NULL)
            .addListener(testListener)
            .addDependency(sixthServiceName)
            .install();
        serviceContainer.addService(sixthServiceName, Service.NULL)
            .addListener(testListener)
            .install();
        // both services are expected to start
        assertController(sixthServiceName, sixthServiceStart);
        assertController(fifthServiceName, fifthServiceStart);
        // the same goes for the rest of the chain, that should also notify of the dependency install
        assertController(thirdController, thirdServiceImmDependencyInstall);
        assertController(thirdController, thirdServiceDependencyProblemCleared);
        assertController(secondController, secondServiceTransDependencyInstall);
        assertController(secondController, secondServiceDependencyProblemCleared);
        assertController(firstController, firstServiceTransDependencyInstall);
        assertController(firstController, firstServiceDependencyProblemCleared);
        assertController(thirdController, thirdServiceStart);
        assertController(secondController, secondServiceStart);
        assertController(firstController, firstServiceStart);
        // no service have immediate missing dependencies htis time
        assertImmediateUnavailableDependencies(thirdController);
        assertImmediateUnavailableDependencies(secondController);
        assertImmediateUnavailableDependencies(firstController);

        final Future<ServiceController<?>> thirdServiceStop = testListener.expectServiceStop(thirdServiceName);
        final Future<ServiceController<?>> secondServiceStop = testListener.expectServiceStop(secondServiceName);
        secondServiceStart = testListener.expectServiceStart(secondServiceName);
        final Future<ServiceController<?>> firstServiceStop = testListener.expectServiceStop(firstServiceName);
        firstServiceStart = testListener.expectServiceStart(firstServiceName);
        // move third service to never mode
        thirdController.setMode(Mode.NEVER);
        // the dependents of third service are also expected to stop
        assertController(thirdController, thirdServiceStop);
        assertController(secondController, secondServiceStop);
        assertController(firstController, firstServiceStop);
        assertController(secondController, secondServiceStart);
        assertController(firstController, firstServiceStart);
    }
}
