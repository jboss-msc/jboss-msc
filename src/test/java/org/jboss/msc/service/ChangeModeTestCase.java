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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.concurrent.Future;

import org.jboss.msc.service.ServiceController.Mode;
import org.jboss.msc.service.ServiceController.State;
import org.jboss.msc.service.util.FailToStartService;
import org.jboss.msc.util.TestServiceListener;
import org.junit.Before;
import org.junit.Test;

/**
 * Test behavior when the mode of one or more services is changed.
 * 
 * @author <a href="mailto:flavia.rainone@jboss.com">Flavia Rainone</a>
 */
public class ChangeModeTestCase extends AbstractServiceTest {

    private static final ServiceName firstServiceName = ServiceName.of("firstService");
    private static final ServiceName secondServiceName = ServiceName.of("secondService");
    private TestServiceListener testListener;

    @Before
    public void setUpTestListener() {
        testListener = new TestServiceListener();
    }

    /**
     * Installs and removes the controller of a service named {@code firstServiceName}.
     * @return the ServiceController of the removed service.
     */
    private ServiceController<?> getRemovedFirstController() throws Exception {
        final ServiceController<?> firstController = getNeverModeFirstController();
        final Future<ServiceController<?>> firstServiceRemoval = testListener.expectServiceRemoval(firstServiceName);
        assertTrue(firstController.compareAndSetMode(Mode.NEVER, Mode.REMOVE));
        assertController(firstController, firstServiceRemoval);
        return firstController;
    }

    /**
     * Changes the mode of {@code serviceController} to {@code newMode} and asserts that it fails with an
     * {@code IllegalStateException}.
     * 
     * @param serviceController the serviceController whose mode will be changed
     * @param newMode           the new mode to be assigned to {@code serviceController}
     */
    private final void assertChangeModeFails(ServiceController<?> serviceController, Mode newMode) throws Exception {
        try
        {
            serviceController.setMode(newMode);
            fail("Exception should be thrown by setMode(" + newMode + ")");
        } catch (IllegalStateException e) {}
    }

    /**
     * Changes the mode of {@code serviceController} to {@code null} and asserts that it fails with an
     * {@code IllegalArgumentException}. The change of mode is made in two ways: first with a call to
     * {@link ServiceController#setMode(Mode) setMode}, then with a call to
     * {@link ServiceController#compareAndSetMode(Mode, Mode) compareAndSetMode}. Next, the test is performed with a
     * an {@link Mode#ACTIVE ACTIVE} new mode, but a {@code null} expected mode, which should also throw {@code IllegalArgumentException}. 
     * Finally, the change is performed with both expected and new modes {@code null}.
     * 
     * @param serviceController the serviceController whose mode will be changed
     */
    private final void assertChangeModeToNullFails(ServiceController<?> serviceController) throws Exception {
        try
        {
            serviceController.setMode(null);
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException e) {}

        final Mode currentMode = serviceController.getMode();
        try
        {
            serviceController.compareAndSetMode(currentMode, null);
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException e) {}

        try
        {
            serviceController.compareAndSetMode(null, Mode.ACTIVE);
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException e) {}

        try
        {
            serviceController.compareAndSetMode(null, null);
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException e) {}
    }

    @Test
    public void changeRemoveToRemove() throws Exception {
        final ServiceController<?> firstController = getRemovedFirstController();
        firstController.setMode(Mode.REMOVE);
        assertSame(State.REMOVED, firstController.getState());
    }

    @Test
    public void changeRemoveToNever() throws Exception {
        final ServiceController<?> firstController = getRemovedFirstController();
        assertChangeModeFails(firstController, Mode.NEVER);
        assertSame(State.REMOVED, firstController.getState());
    }

    @Test
    public void changeRemoveToOnDemand() throws Exception {
        final ServiceController<?> firstController = getRemovedFirstController();
        assertChangeModeFails(firstController, Mode.ON_DEMAND);
        assertSame(State.REMOVED, firstController.getState());
    }

    @Test
    public void changeRemoveToPassive() throws Exception {
        final ServiceController<?> firstController = getRemovedFirstController();
        assertChangeModeFails(firstController, Mode.PASSIVE);
        assertSame(State.REMOVED, firstController.getState());
    }

    @Test
    public void changeRemoveToActive() throws Exception {
        final ServiceController<?> firstController = getRemovedFirstController();
        assertChangeModeFails(firstController, Mode.ACTIVE);
        assertSame(State.REMOVED, firstController.getState());
    }

    @Test
    public void changeRemoveToNull() throws Exception {
        final ServiceController<?> firstController = getRemovedFirstController();
        assertChangeModeToNullFails(firstController);
    }

    /**
     * Installs and returns the controller of a service named {@code firstServiceName}, with initial mode
     * {@link Mode#NEVER NEVER}
     * @return           the ServiceController of the specified service.
     */
    private final ServiceController<?> getNeverModeFirstController() throws Exception {
        final Future<ServiceController<?>> firstServiceListenerAdded = testListener.expectListenerAdded(firstServiceName);
        final ServiceController<?> firstController = serviceContainer.addService(firstServiceName, Service.NULL)
            .addListener(testListener)
            .setInitialMode(Mode.NEVER)
            .install();
        assertController(firstServiceName, firstController);
        assertController(firstController, firstServiceListenerAdded);
        assertSame(State.DOWN, firstController.getState());
        return firstController;
    }

    @Test
    public void changeNeverToRemove() throws Exception {
        getRemovedFirstController();
    }

    @Test
    public void changeNeverToNever() throws Exception {
        final ServiceController<?> firstController = getNeverModeFirstController();
        assertTrue(firstController.compareAndSetMode(Mode.NEVER, Mode.NEVER));
        assertSame(State.DOWN, firstController.getState());
    }

    @Test
    public void changeNeverToOnDemand() throws Exception {
        final ServiceController<?> firstController = getNeverModeFirstController();
        firstController.setMode(Mode.ON_DEMAND);
        assertSame(State.DOWN, firstController.getState());
    }

    @Test
    public void changeNeverToPassive() throws Exception {
        final ServiceController<?> firstController = getNeverModeFirstController();
        final Future<ServiceController<?>> firstServiceStart = testListener.expectServiceStart(firstServiceName);
        firstController.setMode(Mode.PASSIVE);
        assertController(firstController, firstServiceStart);
    }

    @Test
    public void changeDemandedNeverToPassive() throws Exception {
        final ServiceController<?> firstController = getNeverModeFirstController();
        final Future<ServiceController<?>> secondServiceListenerAdded = testListener.expectListenerAdded(secondServiceName);
        final ServiceController<?> secondController = serviceContainer.addService(secondServiceName, Service.NULL)
            .addDependency(firstServiceName).addListener(testListener).install();
        assertController(secondServiceName, secondController);
        assertController(secondController, secondServiceListenerAdded);
        final Future<ServiceController<?>> firstServiceStart = testListener.expectServiceStart(firstServiceName);
        final Future<ServiceController<?>> secondServiceStart = testListener.expectServiceStart(secondServiceName);
        firstController.setMode(Mode.PASSIVE);
        assertController(firstController, firstServiceStart);
        assertController(secondController, secondServiceStart);
    }

    @Test
    public void changeDemandedNeverWithDependenciesToPassive() throws Exception {
        // ** prepare scenario: dependent service on active mode depends on first service, on never mode,
        // that depends on second service, an on_demand service
        // second controller is ON_DEMAND and depends on first service, an active service in UP state
        ServiceController<?> secondController = getUpOnDemandSecondController();
        ServiceController<?> firstController = serviceContainer.getService(firstServiceName);
        assertNotNull(firstController);
        Future<ServiceController<?>> firstServiceStop = testListener.expectServiceStop(firstServiceName);
        Future<ServiceController<?>> secondServiceStop = testListener.expectServiceStop(secondServiceName);
        firstController.setMode(Mode.NEVER);
        assertController(firstController, firstServiceStop);
        assertController(secondController, secondServiceStop);
        final ServiceName dependentServiceName = ServiceName.of("dependent");
        final Future<ServiceController<?>> dependentServiceListenerAdded = testListener.expectListenerAdded(dependentServiceName);
        final ServiceController<?> dependentController = serviceContainer.addService(dependentServiceName, Service.NULL).addDependency(firstServiceName).addListener(testListener).install();
        assertController(dependentServiceName, dependentController);
        assertController(dependentController, dependentServiceListenerAdded);

        //** now change mode, from never to passive
        final Future<ServiceController<?>> firstServiceStart = testListener.expectServiceStart(firstServiceName);
        final Future<ServiceController<?>> dependentServiceStart = testListener.expectServiceStart(dependentServiceName);
        final Future<ServiceController<?>> secondServiceStart = testListener.expectServiceStart(secondServiceName);
        firstController.setMode(Mode.PASSIVE);
        // notice that it is the demand from dependent that will trigger second service startup
        assertController(firstController, firstServiceStart);
        assertController(dependentController, dependentServiceStart);
        assertController(secondController, secondServiceStart);
    }

    @Test
    public void changeNeverToActive() throws Exception {
        final ServiceController<?> firstController = getNeverModeFirstController();
        final Future<ServiceController<?>> firstServiceStart = testListener.expectServiceStart(firstServiceName);
        firstController.setMode(Mode.ACTIVE);
        assertController(firstController, firstServiceStart);
    }

    @Test
    public void changeNeverToNull() throws Exception {
        final ServiceController<?> firstController = getNeverModeFirstController();
        assertChangeModeToNullFails(firstController);
    }

    /**
     * Installs and returns the controller of a service named {@code secondServiceName} with initial mode
     * {@link Mode#ON_DEMAND ON_DEMAND}. The controller is returned in the {@code State#UP UP} state, as there is 
     * an active service named {@code firstServiceName} with a dependency on the returned service.
     * 
     * @return           the ServiceController of the specified service.
     */
    private final ServiceController<?> getUpOnDemandSecondController() throws Exception{
        final Future<ServiceController<?>> firstServiceStart = testListener.expectServiceStart(firstServiceName);
        final Future<ServiceController<?>> secondServiceStart = testListener.expectServiceStart(secondServiceName);

        serviceContainer.addService(secondServiceName, Service.NULL)
        .addListener(testListener)
        .setInitialMode(Mode.ON_DEMAND)
        .install();

        serviceContainer.addService(firstServiceName, Service.NULL)
            .addListener(testListener)
            .addDependency(secondServiceName)
            .install();

        assertController(firstServiceName, firstServiceStart);
        return assertController(secondServiceName, secondServiceStart);
    }

    /**
     * Installs and returns the controller of a service named {@code firstServiceName} with initial mode
     * {@link Mode#ON_DEMAND ON_DEMAND}. The controller is returned in the {@code State#DOWN DOWN} state, as there is 
     * no dependent services.
     * 
     * @return           the ServiceController of the specified service.
     */
    private final ServiceController<?> getDownOnDemandFirstController() throws Exception {
        final Future<ServiceController<?>> firstServiceInstall = testListener.expectListenerAdded(firstServiceName);
        final ServiceController<?> firstController = serviceContainer.addService(firstServiceName, Service.NULL)
            .addListener(testListener)
            .setInitialMode(Mode.ON_DEMAND)
            .install();
        assertController(firstServiceName, firstController);
        assertController(firstController, firstServiceInstall);

        assertSame(State.DOWN, firstController.getState());
        return firstController;
    }

    /**
     * Installs and returns the controller of a service named {@code secondServiceName} with initial mode
     * {@link Mode#ON_DEMAND ON_DEMAND}. The controller is returned in the {@link State#START_FAILED START_FAILED}
     * state, as the start attempt, triggered by a dependent named {@code firstServiceName}, failed to occur.
     * 
     * @return           the ServiceController of the specified service.
     */
    private final ServiceController<?> getFailedToStartOnDemandSecondController() throws Exception {
        final Future<ServiceController<?>> firstServiceInstall = testListener.expectListenerAdded(firstServiceName);
        final Future<ServiceController<?>> secondServiceInstall = testListener.expectListenerAdded(secondServiceName);
        final Future<ServiceController<?>> firstServiceDependencyFailure = testListener.expectDependencyFailure(firstServiceName);
        final Future<StartException> secondServiceFailure = testListener.expectServiceFailure(secondServiceName);

        serviceContainer.addService(secondServiceName, new FailToStartService(true))
            .addListener(testListener)
            .setInitialMode(Mode.ON_DEMAND)
            .install();
        serviceContainer.addService(firstServiceName, new FailToStartService(true))
            .addListener(testListener)
            .addDependency(secondServiceName)
            .install();

        final ServiceController<?> firstController = assertController(firstServiceName, firstServiceInstall);
        final ServiceController<?> secondController = assertController(secondServiceName, secondServiceInstall);
        assertFailure(secondController, secondServiceFailure);
        assertController(firstController, firstServiceDependencyFailure);

        assertSame(State.START_FAILED, secondController.getState());
        return secondController;
    }

    @Test
    public void changeUpOnDemandToRemove() throws Exception {
        final ServiceController<?> secondController = getUpOnDemandSecondController();
        final Future<ServiceController<?>> secondServiceStop = testListener.expectServiceStop(secondServiceName);
        final Future<ServiceController<?>> firstServiceStop = testListener.expectServiceStop(firstServiceName);
        final Future<ServiceController<?>> secondServiceRemoval = testListener.expectServiceRemoval(secondServiceName);
        final Future<ServiceController<?>> firstServiceDependencyMissing = testListener.expectDependencyUninstall(firstServiceName);
        secondController.setMode(Mode.REMOVE);
        assertController(secondController, secondServiceStop);
        final ServiceController<?> firstController = assertController(firstServiceName, firstServiceStop);
        assertController(secondController, secondServiceRemoval);
        assertController(firstController, firstServiceDependencyMissing);
    }

    @Test
    public void changeDownOnDemandToRemove() throws Exception {
        final ServiceController<?> firstController = getDownOnDemandFirstController();
        final Future<ServiceController<?>> firstServiceRemoval = testListener.expectServiceRemoval(firstServiceName);
        firstController.setMode(Mode.REMOVE);
        assertController(firstController, firstServiceRemoval);
    }

    @Test
    public void changeFailedToStartOnDemandToRemove() throws Exception {
        final ServiceController<?> secondController = getFailedToStartOnDemandSecondController();
        final Future<ServiceController<?>> firstServiceDependencyFailureCleared = testListener.expectDependencyFailureCleared(firstServiceName);
        final Future<ServiceController<?>> firstServiceDependencyMissing = testListener.expectDependencyUninstall(firstServiceName);
        final Future<ServiceController<?>> secondServiceStop = testListener.expectServiceStoppedOnly(secondServiceName);
        final Future<ServiceController<?>> secondServiceRemoval = testListener.expectServiceRemoval(secondServiceName);
        secondController.setMode(Mode.REMOVE);
        assertController(secondController, secondServiceStop);
        assertController(secondController, secondServiceRemoval);
        ServiceController<?> firstController = assertController(firstServiceName, firstServiceDependencyFailureCleared);
        assertController(firstController, firstServiceDependencyMissing);
    }

    @Test
    public void changeUpOnDemandToNever() throws Exception {
        final ServiceController<?> secondController = getUpOnDemandSecondController();
        final Future<ServiceController<?>> secondServiceStop = testListener.expectServiceStop(secondServiceName);
        final Future<ServiceController<?>> firstServiceStop = testListener.expectServiceStop(firstServiceName);
        secondController.setMode(Mode.NEVER);
        assertController(secondController, secondServiceStop);
        assertController(firstServiceName, firstServiceStop);
    }

    @Test
    public void changeDownOnDemandToNever() throws Exception {
        final ServiceController<?> firstController = getDownOnDemandFirstController();
        firstController.setMode(Mode.NEVER);
        assertSame(State.DOWN, firstController.getState());
    }

    @Test
    public void changeFailedToStartOnDemandToNever() throws Exception {
        final ServiceController<?> secondController = getFailedToStartOnDemandSecondController();
        final Future<ServiceController<?>> firstServiceDependencyFailureCleared = testListener.expectDependencyFailureCleared(firstServiceName);
        final Future<ServiceController<?>> secondServiceStop = testListener.expectServiceStoppedOnly(secondServiceName);
        secondController.setMode(Mode.NEVER);
        assertController(secondController, secondServiceStop);
        assertController(firstServiceName, firstServiceDependencyFailureCleared);
    }

    @Test
    public void changeUpOnDemandToOnDemand() throws Exception {
        final ServiceController<?> secondController = getUpOnDemandSecondController();
        secondController.setMode(Mode.ON_DEMAND);
        assertSame(State.UP, serviceContainer.getService(firstServiceName).getState());
        assertSame(State.UP, secondController.getState());
    }

    @Test
    public void changeDownOnDemandToOnDemand() throws Exception {
        final ServiceController<?> firstController = getDownOnDemandFirstController();
        firstController.setMode(Mode.ON_DEMAND);
        assertSame(State.DOWN, firstController.getState());
    }

    @Test
    public void changeFailedToStartOnDemandToOnDemand() throws Exception {
        final ServiceController<?> secondController = getFailedToStartOnDemandSecondController();
        secondController.setMode(Mode.ON_DEMAND);
        assertSame(State.DOWN, serviceContainer.getService(firstServiceName).getState());
        assertSame(State.START_FAILED, secondController.getState());
    }

    @Test
    public void changeUpOnDemandToPassive() throws Exception {
        final ServiceController<?> secondController = getUpOnDemandSecondController();
        secondController.setMode(Mode.PASSIVE);
        assertSame(State.UP, serviceContainer.getService(firstServiceName).getState());
        assertSame(State.UP, secondController.getState());
    }

    @Test
    public void changeDownOnDemandToPassive() throws Exception {
        final ServiceController<?> firstController = getDownOnDemandFirstController();
        final Future<ServiceController<?>> firstServiceStart = testListener.expectServiceStart(firstServiceName);
        firstController.setMode(Mode.PASSIVE);
        assertController(firstController, firstServiceStart);
    }

    @Test
    public void changeFailedToStartOnDemandToPassive() throws Exception {
        final ServiceController<?> secondController = getFailedToStartOnDemandSecondController();
        secondController.setMode(Mode.PASSIVE);
        assertSame(State.DOWN, serviceContainer.getService(firstServiceName).getState());
        assertSame(State.START_FAILED, secondController.getState());
    }

    @Test
    public void changeUpOnDemandToActive() throws Exception {
        final ServiceController<?> secondController = getUpOnDemandSecondController();
        secondController.setMode(Mode.ACTIVE);
        assertSame(State.UP, secondController.getState());
        assertSame(State.UP, serviceContainer.getService(firstServiceName).getState());
    }

    @Test
    public void changeDownOnDemandToActive() throws Exception {
        final ServiceController<?> firstController = getDownOnDemandFirstController();
        final Future<ServiceController<?>> firstServiceStart = testListener.expectServiceStart(firstServiceName);
        firstController.setMode(Mode.ACTIVE);
        assertController(firstController, firstServiceStart);
    }

    @Test
    public void changeFailedToStartOnDemandToActive() throws Exception {
        final ServiceController<?> secondController = getFailedToStartOnDemandSecondController();
        secondController.setMode(Mode.ACTIVE);
        assertSame(State.DOWN, serviceContainer.getService(firstServiceName).getState());
        assertSame(State.START_FAILED, secondController.getState());
    }

    @Test
    public void changeUpOnDemandToNull() throws Exception {
        final ServiceController<?> secondController = getUpOnDemandSecondController();
        assertChangeModeToNullFails(secondController);
    }

    @Test
    public void changeDownOnDemandToNull() throws Exception {
        final ServiceController<?> firstController = getDownOnDemandFirstController();
        assertChangeModeToNullFails(firstController);
    }

    @Test
    public void changeFailedToStartOnDemandToNull() throws Exception {
        final ServiceController<?> secondController = getFailedToStartOnDemandSecondController();
        assertChangeModeToNullFails(secondController);
    }

    /**
     * Installs and returns the controller of a service named {@code firstServiceName} with initial mode
     * {@link Mode#PASSIVE PASSIVE}, in the {@code State#UP UP} state.
     * 
     * @return           the ServiceController of the specified service.
     */
    private final ServiceController<?> getUpPassiveFirstController() throws Exception{
        final Future<ServiceController<?>> firstServiceStart = testListener.expectServiceStart(firstServiceName);
        final ServiceController<?> firstController = serviceContainer.addService(firstServiceName, Service.NULL)
            .addListener(testListener)
            .setInitialMode(Mode.PASSIVE)
            .install();
        assertController(firstServiceName, firstController);
        assertController(firstController, firstServiceStart);
        return firstController;
    }

    /**
     * Installs and returns the controller of a service named {@code firstServiceName} with initial mode
     * {@link Mode#PASSIVE PASSIVE}. The returned controller is in the {@code State#DOWN DOWN} state, as it has
     * a missing dependency on a service named {@code secondServiceName}.
     * 
     * @return           the ServiceController of the specified service.
     */
    private final ServiceController<?> getDownPassiveFirstController() throws Exception {
        final Future<ServiceController<?>> firstServiceInstall = testListener.expectListenerAdded(firstServiceName);
        final ServiceController<?> firstController = serviceContainer.addService(firstServiceName, Service.NULL)
            .addListener(testListener)
            .addDependency(secondServiceName)
            .setInitialMode(Mode.PASSIVE)
            .install();
        assertController(firstServiceName, firstController);
        assertController(firstController, firstServiceInstall);

        assertSame(State.DOWN, firstController.getState());
        return firstController;
    }

    /**
     * Installs and returns the controller of a service named {@code secondServiceName} with initial mode
     * {@link Mode#PASSIVE PASSIVE}. The returned controller is in the {@link State#START_FAILED START_FAILED} state,
     * and it has a dependent named {@code firstServiceName}, waiting for the failure to be cleared so it can start.
     * 
     * @return           the ServiceController of the specified service.
     */
    private final ServiceController<?> getFailedToStartPassiveSecondController() throws Exception {
        final Future<ServiceController<?>> firstServiceInstall = testListener.expectListenerAdded(firstServiceName);
        final Future<ServiceController<?>> secondServiceInstall = testListener.expectListenerAdded(secondServiceName);
        final Future<ServiceController<?>> firstServiceDependencyFailure = testListener.expectDependencyFailure(firstServiceName);
        final Future<StartException> secondServiceFailure = testListener.expectServiceFailure(secondServiceName);

        serviceContainer.addService(secondServiceName, new FailToStartService(true))
            .addListener(testListener)
            .setInitialMode(Mode.PASSIVE)
            .install();

        serviceContainer.addService(firstServiceName, new FailToStartService(true))
            .addListener(testListener)
            .addDependency(secondServiceName)
            .install();

        final ServiceController<?> firstController = assertController(firstServiceName, firstServiceInstall);
        final ServiceController<?> secondController = assertController(secondServiceName, secondServiceInstall);
        assertFailure(secondController, secondServiceFailure);
        assertController(firstController, firstServiceDependencyFailure);

        assertSame(State.START_FAILED, secondController.getState());
        return secondController;
    }

    @Test
    public void changeUpPassiveToRemove() throws Exception {
        final ServiceController<?> firstController = getUpPassiveFirstController();
        final Future<ServiceController<?>> firstServiceStop = testListener.expectServiceStop(firstServiceName);
        final Future<ServiceController<?>> firstServiceRemoval = testListener.expectServiceRemoval(firstServiceName);
        firstController.setMode(Mode.REMOVE);
        assertController(firstController, firstServiceStop);
        assertController(firstController, firstServiceRemoval);
    }

    @Test
    public void changeDownPassiveToRemove() throws Exception {
        final ServiceController<?> firstController = getDownPassiveFirstController();
        final Future<ServiceController<?>> firstServiceRemoval = testListener.expectServiceRemoval(firstServiceName);
        firstController.setMode(Mode.REMOVE);
        assertController(firstController, firstServiceRemoval);
    }

    @Test
    public void changeFailedToStartPassiveToRemove() throws Exception {
        final ServiceController<?> secondController = getFailedToStartPassiveSecondController();
        final Future<ServiceController<?>> firstServiceDependencyFailureCleared = testListener.expectDependencyFailureCleared(firstServiceName);
        final Future<ServiceController<?>> firstServiceDependencyUninstall = testListener.expectDependencyUninstall(firstServiceName);
        final Future<ServiceController<?>> secondServiceRemoval = testListener.expectServiceRemoval(secondServiceName);
        secondController.setMode(Mode.REMOVE);
        assertController(secondController, secondServiceRemoval);
        final ServiceController<?> firstController = assertController(firstServiceName, firstServiceDependencyFailureCleared);
        assertController(firstController, firstServiceDependencyUninstall);
    }

    @Test
    public void changeUpPassiveToNever() throws Exception {
        final ServiceController<?> firstController = getUpPassiveFirstController();
        final Future<ServiceController<?>> firstServiceStop = testListener.expectServiceStop(firstServiceName);
        firstController.setMode(Mode.NEVER);
        assertController(firstController, firstServiceStop);
    }

    @Test
    public void changeDownPassiveToNever() throws Exception {
        final ServiceController<?> firstController = getDownPassiveFirstController();
        firstController.setMode(Mode.NEVER);
        assertSame(State.DOWN, firstController.getState());
    }

    @Test
    public void changeFailedToStartPassiveToNever() throws Exception {
        final ServiceController<?> secondController = getFailedToStartPassiveSecondController();
        final Future<ServiceController<?>> secondServiceStop = testListener.expectServiceStoppedOnly(secondServiceName);
        final Future<ServiceController<?>> firstServiceDependencyFailureCleared = testListener.expectDependencyFailureCleared(firstServiceName);
        secondController.setMode(Mode.NEVER);
        assertController(firstServiceName, firstServiceDependencyFailureCleared);
        assertSame(State.DOWN, secondController.getState());
        assertController(secondController, secondServiceStop);
    }

    @Test
    public void changeUpPassiveToOnDemand() throws Exception {
        final ServiceController<?> firstController = getUpPassiveFirstController();
        final Future<ServiceController<?>> firstServiceStop = testListener.expectServiceStop(firstServiceName);
        firstController.setMode(Mode.ON_DEMAND);
        assertController(firstController, firstServiceStop);
    }

    @Test
    public void changeDownPassiveToOnDemand() throws Exception {
        final ServiceController<?> firstController = getDownPassiveFirstController();
        firstController.setMode(Mode.ON_DEMAND);
        assertSame(State.DOWN, firstController.getState());
    }

    @Test
    public void changeFailedToStartPassiveToOnDemand() throws Exception {
        final ServiceController<?> secondController = getFailedToStartPassiveSecondController();
        Future<ServiceController<?>> secondServiceStart = testListener.expectServiceStart(secondServiceName);
        secondController.setMode(Mode.ON_DEMAND);
        if (secondController.getState() == State.DOWN) {
            assertController(secondController, secondServiceStart);
        }
        else {
            assertSame(State.START_FAILED, secondController.getState());
        }
    }

    @Test
    public void changeUpPassiveToPassive() throws Exception {
        final ServiceController<?> firstController = getUpPassiveFirstController();
        firstController.setMode(Mode.PASSIVE);
        assertSame(State.UP, firstController.getState());
    }

    @Test
    public void changeDownPassiveToPassive() throws Exception {
        final ServiceController<?> firstController = getDownPassiveFirstController();
        firstController.setMode(Mode.PASSIVE);
        assertSame(State.DOWN, firstController.getState());
    }

    @Test
    public void changeFailedToStartPassiveToPassive() throws Exception {
        final ServiceController<?> secondController = getFailedToStartPassiveSecondController();
        secondController.setMode(Mode.PASSIVE);
        assertSame(State.START_FAILED, secondController.getState());
    }

    @Test
    public void changeUpPassiveToActive() throws Exception {
        final ServiceController<?> firstController = getUpPassiveFirstController();
        firstController.setMode(Mode.ACTIVE);
        assertSame(State.UP, firstController.getState());
    }

    @Test
    public void changeDownPassiveToActive() throws Exception {
        final ServiceController<?> firstController = getDownPassiveFirstController();
        firstController.setMode(Mode.ACTIVE);
        assertSame(State.DOWN, firstController.getState());
    }

    @Test
    public void changeFailedToStartPassiveToActive() throws Exception {
        final ServiceController<?> secondController = getFailedToStartPassiveSecondController();
        secondController.setMode(Mode.ACTIVE);
        assertSame(State.START_FAILED, secondController.getState());
    }

    @Test
    public void changeUpPassiveToNull() throws Exception {
        final ServiceController<?> firstController = getUpPassiveFirstController();
        assertChangeModeToNullFails(firstController);
    }

    @Test
    public void changeDownPassiveToNull() throws Exception {
        final ServiceController<?> firstController = getDownPassiveFirstController();
        assertChangeModeToNullFails(firstController);
    }

    @Test
    public void changeFailedToStartPassiveToNull() throws Exception {
        final ServiceController<?> secondController = getFailedToStartPassiveSecondController();
        assertChangeModeToNullFails(secondController);
    }

    /**
     * Installs and returns the controller of a service named {@code firstServiceName} with initial mode
     * {@link Mode#ACTIVE ACTIVE}, in the {@code State#UP UP} state, as it has
     * 
     * 
     * @return           the ServiceController of the specified service.
     */
    private final ServiceController<?> getUpActiveFirstController() throws Exception{
        final Future<ServiceController<?>> firstServiceStart = testListener.expectServiceStart(firstServiceName);
        final ServiceController<?> firstController = serviceContainer.addService(firstServiceName, Service.NULL)
            .addListener(testListener)
            .setInitialMode(Mode.ACTIVE)
            .install();
        assertController(firstServiceName, firstController);
        assertController(firstController, firstServiceStart);
        return firstController;
    }

    /**
     * Installs and returns the controller of a service named {@code firstServiceName} with initial mode
     * {@link Mode#ACTIVE ACTIVE}. The returned controller is in the {@link State#DOWN DOWN} state, as it has
     * a dependency on a service named {@code secondServiceName} in the {@link State#START_FAILED START_FAILED} state.
     * 
     * @return           the ServiceController of the specified service.
     */
    private final ServiceController<?> getDownActiveFirstController() throws Exception {
        final Future<ServiceController<?>> firstServiceInstall = testListener.expectListenerAdded(firstServiceName);
        final Future<ServiceController<?>> secondServiceInstall = testListener.expectListenerAdded(secondServiceName);
        final Future<StartException> secondServiceFailure = testListener.expectServiceFailure(secondServiceName);
        final Future<ServiceController<?>> firstServiceDependencyFailure= testListener.expectDependencyFailure(firstServiceName);

        serviceContainer.addService(secondServiceName, new FailToStartService(true))
            .addListener(testListener)
            .install();
        serviceContainer.addService(firstServiceName, Service.NULL)
            .addListener(testListener)
            .addDependency(secondServiceName)
            .setInitialMode(Mode.ACTIVE)
            .install();

        final ServiceController<?> firstController = assertController(firstServiceName, firstServiceInstall);
        final ServiceController<?> secondController = assertController(secondServiceName, secondServiceInstall);
        assertFailure(secondController, secondServiceFailure);
        assertController(firstController, firstServiceDependencyFailure);

        assertSame(State.DOWN, firstController.getState());
        return firstController;
    }

    /**
     * Installs and returns the controller of a service named {@code secondServiceName} with initial mode
     * {@link Mode#ACTIVE ACTIVE}. The returned controller is in the {@code State#START_FAILED START_FAILED} state, and
     * has a dependent named {@code firstServiceName} waiting for the failure to be cleared so it can start.
     * 
     * @return           the ServiceController of the specified service.
     */
    private final ServiceController<?> getFailedToStartActiveSecondController() throws Exception {
        final Future<ServiceController<?>> firstServiceInstall = testListener.expectListenerAdded(firstServiceName);
        final Future<ServiceController<?>> secondServiceInstall = testListener.expectListenerAdded(secondServiceName);
        final Future<ServiceController<?>> firstServiceDependencyFailure = testListener.expectDependencyFailure(firstServiceName);
        final Future<StartException> secondServiceFailure = testListener.expectServiceFailure(secondServiceName);

        serviceContainer.addService(secondServiceName, new FailToStartService(true))
            .addListener(testListener)
            .setInitialMode(Mode.ACTIVE)
            .install();
        serviceContainer.addService(firstServiceName, new FailToStartService(true))
            .addListener(testListener)
            .addDependency(secondServiceName)
            .install();

        final ServiceController<?> firstController = assertController(firstServiceName, firstServiceInstall);
        final ServiceController<?> secondController = assertController(secondServiceName, secondServiceInstall);
        assertFailure(secondController, secondServiceFailure);
        assertController(firstController, firstServiceDependencyFailure);

        assertSame(State.START_FAILED, secondController.getState());
        return secondController;
    }

    @Test
    public void changeUpActiveToRemove() throws Exception {
        final ServiceController<?> firstController = getUpActiveFirstController();
        final Future<ServiceController<?>> firstServiceStop = testListener.expectServiceStop(firstServiceName);
        final Future<ServiceController<?>> firstServiceRemoval = testListener.expectServiceRemoval(firstServiceName);
        firstController.setMode(Mode.REMOVE);
        assertController(firstController, firstServiceStop);
        assertController(firstController, firstServiceRemoval);
    }

    @Test
    public void changeDownActiveToRemove() throws Exception {
        final ServiceController<?> firstController = getDownActiveFirstController();
        final Future<ServiceController<?>> firstServiceRemoval = testListener.expectServiceRemoval(firstServiceName);
        firstController.setMode(Mode.REMOVE);
        assertController(firstController, firstServiceRemoval);
    }

    @Test
    public void changeFailedToStartActiveToRemove() throws Exception {
        final ServiceController<?> secondController = getFailedToStartActiveSecondController();
        final Future<ServiceController<?>> firstServiceDependencyFailureCleared = testListener.expectDependencyFailureCleared(firstServiceName);
        final Future<ServiceController<?>> firstServiceDependencyUninstall = testListener.expectDependencyUninstall(firstServiceName);
        final Future<ServiceController<?>> secondServiceRemoval = testListener.expectServiceRemoval(secondServiceName);
        secondController.setMode(Mode.REMOVE);
        assertController(secondController, secondServiceRemoval);
        final ServiceController<?> firstController = assertController(firstServiceName, firstServiceDependencyFailureCleared);
        assertController(firstController, firstServiceDependencyUninstall);
    }

    @Test
    public void changeUpActiveToNever() throws Exception {
        final ServiceController<?> firstController = getUpActiveFirstController();
        assertFalse(firstController.compareAndSetMode(Mode.ON_DEMAND, Mode.NEVER));
        final Future<ServiceController<?>> firstServiceStop = testListener.expectServiceStop(firstServiceName);
        assertTrue(firstController.compareAndSetMode(Mode.ACTIVE, Mode.NEVER));
        assertController(firstController, firstServiceStop);
    }

    @Test
    public void changeDownActiveToNever() throws Exception {
        final ServiceController<?> firstController = getDownActiveFirstController();
        firstController.setMode(Mode.NEVER);
        assertSame(State.DOWN, firstController.getState());
    }

    @Test
    public void changeFailedToStartActiveToNever() throws Exception {
        final ServiceController<?> secondController = getFailedToStartActiveSecondController();
        final Future<ServiceController<?>> secondServiceStop = testListener.expectServiceStoppedOnly(secondServiceName);
        final Future<ServiceController<?>> firstServiceDependencyFailureCleared = testListener.expectDependencyFailureCleared(firstServiceName);
        secondController.setMode(Mode.NEVER);
        assertController(firstServiceName, firstServiceDependencyFailureCleared);
        assertSame(State.DOWN, secondController.getState());
        assertController(secondController, secondServiceStop);
    }

    @Test
    public void changeUpActiveToOnDemand() throws Exception {
        final ServiceController<?> firstController = getUpActiveFirstController();
        final Future<ServiceController<?>> firstServiceStop = testListener.expectServiceStop(firstServiceName);
        firstController.setMode(Mode.ON_DEMAND);
        assertController(firstController, firstServiceStop);
    }

    @Test
    public void changeDownActiveToOnDemand() throws Exception {
        final ServiceController<?> firstController = getDownActiveFirstController();
        firstController.setMode(Mode.ON_DEMAND);
        assertSame(State.DOWN, firstController.getState());
    }

    @Test
    public void changeFailedToStartActiveToOnDemand() throws Exception {
        final ServiceController<?> secondController = getFailedToStartActiveSecondController();
        secondController.setMode(Mode.ON_DEMAND);
        assertSame(State.START_FAILED, secondController.getState());
    }

    @Test
    public void changeUpActiveToPassive() throws Exception {
        final ServiceController<?> firstController = getUpActiveFirstController();
        assertFalse(firstController.compareAndSetMode(Mode.PASSIVE, Mode.PASSIVE));
        assertTrue(firstController.compareAndSetMode(Mode.ACTIVE, Mode.PASSIVE));
        firstController.setMode(Mode.PASSIVE);
        assertSame(State.UP, firstController.getState());
    }

    @Test
    public void changeDownActiveToPassive() throws Exception {
        final ServiceController<?> firstController = getDownActiveFirstController();
        firstController.setMode(Mode.PASSIVE);
        assertSame(State.DOWN, firstController.getState());
    }

    @Test
    public void changeFailedToStartActiveToPassive() throws Exception {
        final ServiceController<?> secondController = getFailedToStartActiveSecondController();
        secondController.setMode(Mode.PASSIVE);
        assertSame(State.START_FAILED, secondController.getState());
    }

    @Test
    public void changeUpActiveToActive() throws Exception {
        final ServiceController<?> firstController = getUpActiveFirstController();
        firstController.setMode(Mode.ACTIVE);
        assertSame(State.UP, firstController.getState());
    }

    @Test
    public void changeDownActiveToActive() throws Exception {
        final ServiceController<?> firstController = getDownActiveFirstController();
        firstController.setMode(Mode.ACTIVE);
        assertSame(State.DOWN, firstController.getState());
    }

    @Test
    public void changeFailedToStartActiveToActive() throws Exception {
        final ServiceController<?> secondController = getFailedToStartActiveSecondController();
        secondController.setMode(Mode.ACTIVE);
        assertSame(State.START_FAILED, secondController.getState());
    }

    @Test
    public void changeUpActiveToNull() throws Exception {
        final ServiceController<?> firstController = getUpActiveFirstController();
        assertChangeModeToNullFails(firstController);
    }

    @Test
    public void changeDownActiveToNull() throws Exception {
        final ServiceController<?> firstController = getDownActiveFirstController();
        assertChangeModeToNullFails(firstController);
    }

    @Test
    public void changeFailedToStartActiveToNull() throws Exception {
        final ServiceController<?> secondController = getFailedToStartActiveSecondController();
        assertChangeModeToNullFails(secondController);
    }

    @Test
    public void changeModeAfterShutdown1() throws Exception {
        final ServiceController<?> controller = getUpActiveFirstController();
        shutdownContainer();
        try {
            controller.setMode(Mode.ACTIVE);
            fail ("IllegalArgument expected");
        } catch (IllegalArgumentException e) {}    }

    @Test
    public void changeModeAfterShutdown2() throws Exception {
        ServiceController<?> controller = getDownPassiveFirstController();
        shutdownContainer();
        try {
            controller.setMode(Mode.NEVER);
            fail ("IllegalArgument expected");
        } catch (IllegalArgumentException e) {}
    }

    @Test
    public void changeModeAfterShutdown3() throws Exception {
        ServiceController<?> controller = getUpOnDemandSecondController();
        shutdownContainer();
        try {
            controller.setMode(Mode.ACTIVE);
            fail ("IllegalArgument expected");
        } catch (IllegalArgumentException e) {}
    }
}