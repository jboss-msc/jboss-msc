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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;

import java.util.concurrent.Future;

import org.jboss.msc.service.ServiceController.Mode;
import org.jboss.msc.service.ServiceController.State;
import org.jboss.msc.service.util.FailToStartService;
import org.jboss.msc.util.TestServiceListener;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Test behavior when the mode of one or more services is changed from inside
 * {@link ServiceListener#listenerAdded} calls.
 * 
 * @author <a href="mailto:flavia.rainone@jboss.com">Flavia Rainone</a>
 */
@SuppressWarnings("unchecked")
public class ChangeModeOnListenerAddedTestCase extends AbstractServiceTest {

    private static final ServiceName firstServiceName = ServiceName.of("firstService");
    private static final ServiceName secondServiceName = ServiceName.of("secondService");
    private TestServiceListener testListener;
    private SetModeListener setModeListener;

    @Before
    public void setUpListeners() {
        testListener = new TestServiceListener();
        setModeListener = new SetModeListener();
    }

    @After
    public void checkCleanNotifications() {
        setModeListener.assertNoUnexpectedCalls();
    }

    /**
     * Install {@code serviceBuilder}, asserts that {@link setModeListener} failed to set the mode to {@code null} on
     * {@link SetModeListener#listenerAdded(ServiceController) listenerAdded} execution, receiving an
     * {@code IllegalArgumentException}.
     * </p>
     * This method asserts listener added has been called on {@link testListener}.
     * 
     * @param serviceBuilder the serviceBuilder that will be installed
     * @param serviceName the serviceName
     */
    private final ServiceController<?> assertChangeModeToNullFails(ServiceBuilder<?> serviceBuilder, ServiceName serviceName)
            throws Exception {
        final Future<ServiceController<?>> listenerAdded = testListener.expectListenerAdded(serviceName);
        ServiceController<?> serviceController = serviceBuilder.install();
        setModeListener.assertFailure(IllegalArgumentException.class);
        assertController(serviceController, listenerAdded);
        return serviceController;
    }

    /**
     * Installs and returns the controller of a service named
     * {@code firstServiceName}, with initial mode {@link Mode#NEVER NEVER}
     * 
     * @return the ServiceController of the specified service.
     */
    private final ServiceBuilder<?> getNeverModeFirstBuilder() throws Exception {
        return serviceContainer.addService(firstServiceName, Service.NULL).addListener(testListener, setModeListener)
                .setInitialMode(Mode.NEVER);
    }

    @Test
    public void changeNeverToRemove() throws Exception {
        final Future<ServiceController<?>> firstServiceRemoval = testListener.expectServiceRemoval(firstServiceName);
        setModeListener.setMode(Mode.REMOVE);
        ServiceController<?> firstController = getNeverModeFirstBuilder().install();
        assertController(firstController, firstServiceRemoval);
    }

    @Test
    public void changeNeverToNever() throws Exception {
        final Future<ServiceController<?>> firstServiceListenerAdded = testListener.expectListenerAdded(firstServiceName);
        setModeListener.setMode(Mode.NEVER);
        ServiceController<?> firstController = getNeverModeFirstBuilder().install();
        assertController(firstController, firstServiceListenerAdded);
        assertSame(State.DOWN, firstController.getState());
    }

    @Test
    public void changeNeverToOnDemand() throws Exception {
        final Future<ServiceController<?>> firstServiceListenerAdded = testListener.expectListenerAdded(firstServiceName);
        setModeListener.setMode(Mode.ON_DEMAND);
        ServiceController<?> firstController = getNeverModeFirstBuilder().install();
        assertController(firstController, firstServiceListenerAdded);
        assertSame(State.DOWN, firstController.getState());
    }

    @Test
    public void changeNeverToPassive() throws Exception {
        final Future<ServiceController<?>> firstServiceStart = testListener.expectServiceStart(firstServiceName);
        setModeListener.setMode(Mode.PASSIVE);
        ServiceController<?> firstController = getNeverModeFirstBuilder().install();
        assertController(firstController, firstServiceStart);
    }

    @Test
    public void changeDemandedNeverToPassive() throws Exception {
        final Future<ServiceController<?>> secondServiceListenerAdded = testListener.expectListenerAdded(secondServiceName);
        final ServiceController<?> secondController = serviceContainer.addService(secondServiceName, Service.NULL)
                .addDependency(firstServiceName).addListener(testListener).install();
        assertController(secondController, secondServiceListenerAdded);

        final Future<ServiceController<?>> firstServiceStart = testListener.expectServiceStart(firstServiceName);
        final Future<ServiceController<?>> secondServiceStart = testListener.expectServiceStart(secondServiceName);
        setModeListener.setMode(Mode.PASSIVE);
        ServiceController<?> firstController = getNeverModeFirstBuilder().install();
        assertController(firstController, firstServiceStart);
        assertController(secondController, secondServiceStart);
    }

    @Test
    public void changeDemandedNeverWithDependenciesToPassive() throws Exception {
        // ** prepare scenario:
        // dependent service on active mode depends on first service, on never
        // mode,
        // that depends on second service, an on_demand service
        final Future<ServiceController<?>> secondServiceListenerAdded = testListener.expectListenerAdded(secondServiceName);
        final Future<ServiceController<?>> secondServiceStart = testListener.expectServiceStart(secondServiceName);
        final ServiceController<?> secondController = serviceContainer.addService(secondServiceName, Service.NULL)
                .addListener(testListener).install();
        assertController(secondController, secondServiceListenerAdded);
        assertController(secondController, secondServiceStart);

        final ServiceName dependentServiceName = ServiceName.of("dependent");
        final Future<ServiceController<?>> dependentServiceListenerAdded = testListener
                .expectListenerAdded(dependentServiceName);
        final ServiceController<?> dependentController = serviceContainer.addService(dependentServiceName, Service.NULL)
                .addDependency(firstServiceName).addListener(testListener).install();
        assertController(dependentServiceName, dependentController);
        assertController(dependentController, dependentServiceListenerAdded);

        final Future<ServiceController<?>> firstServiceStart = testListener.expectServiceStart(firstServiceName);
        final Future<ServiceController<?>> dependentServiceStart = testListener.expectServiceStart(dependentServiceName);
        
        setModeListener.setMode(Mode.PASSIVE);
        ServiceController<?> firstController = getNeverModeFirstBuilder().addDependency(secondServiceName).install();
        // notice that it is the demand from dependent that will trigger second
        // service startup
        assertController(firstController, firstServiceStart);
        assertController(dependentController, dependentServiceStart);
    }

    @Test
    public void changeNeverToActive() throws Exception {
        final Future<ServiceController<?>> firstServiceStart = testListener.expectServiceStart(firstServiceName);
        setModeListener.setMode(Mode.ACTIVE);
        ServiceController<?> firstController = getNeverModeFirstBuilder().install();
        assertController(firstController, firstServiceStart);
    }

    @Test
    public void changeNeverToNull() throws Exception {
        ServiceController<?> firstController = assertChangeModeToNullFails(getNeverModeFirstBuilder(), firstServiceName);
        assertSame(State.DOWN, firstController.getState());
    }

    /**
     * Returns the builder of a service named {@code secondServiceName} with initial mode {@link Mode#ON_DEMAND
     * ON_DEMAND}. If immediately installed, the controller should enter the {@code State#UP UP} state,
     * as there is an active service named {@code firstServiceName} with a dependency on the returned service.
     * 
     * @return the ServiceController of the specified service.
     */
    private final ServiceBuilder<?> getUpOnDemandSecondBuilder() throws Exception {
        final Future<ServiceController<?>> firstServiceDepMissing = testListener.expectImmediateDependencyUninstall(firstServiceName);
        serviceContainer.addService(firstServiceName, Service.NULL).addListener(testListener).addDependency(secondServiceName)
                .install();
        assertController(firstServiceName, firstServiceDepMissing);

        return serviceContainer.addService(secondServiceName, Service.NULL).addListener(testListener, setModeListener)
                .setInitialMode(Mode.ON_DEMAND);
    }

    /**
     * Returns the builder of a service named {@code firstServiceName} with initial mode {@link Mode#ON_DEMAND
     * ON_DEMAND}. If installed immediately, the controller will enter the {@code State#DOWN DOWN}
     * state, as there is no dependent services.
     * 
     * @return the ServiceController of the specified service.
     */
    private final ServiceBuilder<?> getDownOnDemandFirstBuilder() throws Exception {
        return serviceContainer.addService(firstServiceName, Service.NULL).addListener(testListener, setModeListener)
                .setInitialMode(Mode.ON_DEMAND);
    }

    /**
     * Returns the builder of a service named {@code secondServiceName} with initial mode {@link Mode#ON_DEMAND
     * ON_DEMAND}. If installed immediately, the controller will enter the {@link State#START_FAILED
     * START_FAILED} state, as the start attempt, triggered by a dependent named {@code firstServiceName},
     * fails to occur.
     * 
     * @return the ServiceController of the specified service.
     */
    private final ServiceBuilder<?> getFailedToStartOnDemandSecondBuilder() throws Exception {
        final Future<ServiceController<?>> firstServiceInstall = testListener.expectListenerAdded(firstServiceName);
        serviceContainer.addService(firstServiceName, new FailToStartService(true)).addListener(testListener)
                .addDependency(secondServiceName).install();
        assertController(firstServiceName, firstServiceInstall);

        return serviceContainer.addService(secondServiceName, new FailToStartService(true))
                .addListener(testListener, setModeListener).setInitialMode(Mode.ON_DEMAND);
    }

    @Test
    public void changeUpOnDemandToRemove() throws Exception {
        final ServiceBuilder<?> secondBuilder = getUpOnDemandSecondBuilder();
        final Future<ServiceController<?>> secondServiceListenerAdded = testListener.expectListenerAdded(secondServiceName);
        final Future<ServiceController<?>> secondServiceRemoval = testListener.expectServiceRemoval(secondServiceName);
        final Future<ServiceController<?>> firstServiceDependencyInstall = testListener
                .expectImmediateDependencyInstall(firstServiceName);
        final Future<ServiceController<?>> firstServiceDependencyMissing = testListener
                .expectImmediateDependencyUninstall(firstServiceName);
        setModeListener.setMode(Mode.REMOVE);
        final ServiceController<?> secondController = secondBuilder.install();
        assertController(secondController, secondServiceListenerAdded);
        assertController(secondController, secondServiceRemoval);
        final ServiceController<?> firstController = assertController(firstServiceName, firstServiceDependencyInstall);
        assertController(firstController, firstServiceDependencyMissing);
    }

    @Test
    public void changeDownOnDemandToRemove() throws Exception {
        final Future<ServiceController<?>> firstServiceRemoval = testListener.expectServiceRemoval(firstServiceName);
        setModeListener.setMode(Mode.REMOVE);
        final ServiceController<?> firstController = getDownOnDemandFirstBuilder().install();
        assertController(firstController, firstServiceRemoval);
    }

    @Test
    public void changeFailedToStartOnDemandToRemove() throws Exception {
        final ServiceBuilder<?> secondBuilder = getFailedToStartOnDemandSecondBuilder();
        final Future<ServiceController<?>> firstServiceDependencyInstall = testListener
                .expectImmediateDependencyInstall(firstServiceName);
        final Future<ServiceController<?>> firstServiceDependencyMissing = testListener
                .expectImmediateDependencyUninstall(firstServiceName);
        final Future<ServiceController<?>> secondServiceRemoval = testListener.expectServiceRemoval(secondServiceName);
        setModeListener.setMode(Mode.REMOVE);
        final ServiceController<?> secondController = secondBuilder.install();
        assertController(secondController, secondServiceRemoval);
        final ServiceController<?> firstController = assertController(firstServiceName, firstServiceDependencyInstall);
        assertController(firstController, firstServiceDependencyMissing);
    }

    @Test
    public void changeUpOnDemandToNever() throws Exception {
        final ServiceBuilder<?> secondBuilder = getUpOnDemandSecondBuilder();
        final Future<ServiceController<?>> secondServiceListenerAdded = testListener.expectListenerAdded(secondServiceName);
        final Future<ServiceController<?>> firstServiceDependencyInstall = testListener
                .expectImmediateDependencyInstall(firstServiceName);
        setModeListener.setMode(Mode.NEVER);
        final ServiceController<?> secondController = secondBuilder.install();
        assertController(secondController, secondServiceListenerAdded);
        assertSame(State.DOWN, secondController.getState());
        assertController(firstServiceName, firstServiceDependencyInstall);
    }

    @Test
    public void changeDownOnDemandToNever() throws Exception {
        final Future<ServiceController<?>> firstServiceListenerAdded = testListener.expectListenerAdded(firstServiceName);
        setModeListener.setMode(Mode.NEVER);
        final ServiceController<?> firstController = getDownOnDemandFirstBuilder().install();
        assertController(firstServiceName, firstController);
        assertController(firstController, firstServiceListenerAdded);
        assertSame(State.DOWN, firstController.getState());
    }

    @Test
    public void changeFailedToStartOnDemandToNever() throws Exception {
        final ServiceBuilder<?> secondBuilder = getFailedToStartOnDemandSecondBuilder();
        final Future<ServiceController<?>> firstServiceDependencyInstall = testListener
                .expectImmediateDependencyInstall(firstServiceName);
        final Future<ServiceController<?>> secondServiceListenerAdded = testListener.expectListenerAdded(secondServiceName);
        setModeListener.setMode(Mode.NEVER);
        final ServiceController<?> secondController = secondBuilder.install();
        assertController(secondController, secondServiceListenerAdded);
        assertController(firstServiceName, firstServiceDependencyInstall);
    }

    @Test
    public void changeUpOnDemandToOnDemand() throws Exception {
        final ServiceBuilder<?> secondBuilder = getUpOnDemandSecondBuilder();
        final Future<ServiceController<?>> secondServiceListenerAdded = testListener.expectListenerAdded(secondServiceName);
        final Future<ServiceController<?>> secondServiceStart = testListener.expectServiceStart(secondServiceName);
        final Future<ServiceController<?>> firstServiceDependencyInstall = testListener
                .expectImmediateDependencyInstall(firstServiceName);
        final Future<ServiceController<?>> firstServiceStart = testListener.expectServiceStart(firstServiceName);
        setModeListener.setMode(Mode.ON_DEMAND);
        final ServiceController<?> secondController = secondBuilder.install();
        assertController(secondController, secondServiceListenerAdded);
        assertController(secondController, secondServiceStart);
        final ServiceController<?> firstController = assertController(firstServiceName, firstServiceDependencyInstall);
        assertController(firstController, firstServiceStart);
    }

    @Test
    public void changeDownOnDemandToOnDemand() throws Exception {
        final Future<ServiceController<?>> firstServiceListenerAdded = testListener.expectListenerAdded(firstServiceName);
        setModeListener.setMode(Mode.ON_DEMAND);
        final ServiceController<?> firstController = getDownOnDemandFirstBuilder().install();
        assertController(firstServiceName, firstController);
        assertController(firstController, firstServiceListenerAdded);
        assertSame(State.DOWN, firstController.getState());
    }

    @Test
    public void changeFailedToStartOnDemandToOnDemand() throws Exception {
        final ServiceBuilder<?> secondBuilder = getFailedToStartOnDemandSecondBuilder();
        final Future<ServiceController<?>> firstServiceDependencyInstall = testListener
                .expectImmediateDependencyInstall(firstServiceName);
        final Future<ServiceController<?>> firstServiceDependencyFailed = testListener
                .expectDependencyFailure(firstServiceName);
        final Future<StartException> secondServiceFailure = testListener.expectServiceFailure(secondServiceName);
        setModeListener.setMode(Mode.ON_DEMAND);
        final ServiceController<?> secondController = secondBuilder.install();
        assertFailure(secondController, secondServiceFailure);
        final ServiceController<?> firstController = assertController(firstServiceName, firstServiceDependencyInstall);
        assertController(firstController, firstServiceDependencyFailed);
    }

    @Test
    public void changeUpOnDemandToPassive() throws Exception {
        final ServiceBuilder<?> secondBuilder = getUpOnDemandSecondBuilder();
        final Future<ServiceController<?>> secondServiceListenerAdded = testListener.expectListenerAdded(secondServiceName);
        final Future<ServiceController<?>> secondServiceStart = testListener.expectServiceStart(secondServiceName);
        final Future<ServiceController<?>> firstServiceDependencyInstall = testListener
                .expectImmediateDependencyInstall(firstServiceName);
        final Future<ServiceController<?>> firstServiceStart = testListener.expectServiceStart(firstServiceName);
        setModeListener.setMode(Mode.PASSIVE);
        final ServiceController<?> secondController = secondBuilder.install();
        assertController(secondController, secondServiceListenerAdded);
        assertController(secondController, secondServiceStart);
        final ServiceController<?> firstController = assertController(firstServiceName, firstServiceDependencyInstall);
        assertController(firstController, firstServiceStart);
    }

    @Test
    public void changeDownOnDemandToPassive() throws Exception {
        final Future<ServiceController<?>> firstServiceListenerAdded = testListener.expectListenerAdded(firstServiceName);
        final Future<ServiceController<?>> firstServiceStart = testListener.expectServiceStart(firstServiceName);
        setModeListener.setMode(Mode.PASSIVE);
        final ServiceController<?> firstController = getDownOnDemandFirstBuilder().install();
        assertController(firstServiceName, firstController);
        assertController(firstController, firstServiceListenerAdded);
        assertController(firstController, firstServiceStart);
    }

    @Test
    public void changeFailedToStartOnDemandToPassive() throws Exception {
        final ServiceBuilder<?> secondBuilder = getFailedToStartOnDemandSecondBuilder();
        final Future<ServiceController<?>> firstServiceDependencyInstall = testListener
                .expectImmediateDependencyInstall(firstServiceName);
        final Future<ServiceController<?>> firstServiceDependencyFailed = testListener
                .expectDependencyFailure(firstServiceName);
        final Future<StartException> secondServiceFailure = testListener.expectServiceFailure(secondServiceName);
        setModeListener.setMode(Mode.PASSIVE);
        final ServiceController<?> secondController = secondBuilder.install();
        assertFailure(secondController, secondServiceFailure);
        final ServiceController<?> firstController = assertController(firstServiceName, firstServiceDependencyInstall);
        assertController(firstController, firstServiceDependencyFailed);
    }

    @Test
    public void changeUpOnDemandToActive() throws Exception {
        final ServiceBuilder<?> secondBuilder = getUpOnDemandSecondBuilder();
        final Future<ServiceController<?>> secondServiceListenerAdded = testListener.expectListenerAdded(secondServiceName);
        final Future<ServiceController<?>> secondServiceStart = testListener.expectServiceStart(secondServiceName);
        final Future<ServiceController<?>> firstServiceDependencyInstall = testListener
                .expectImmediateDependencyInstall(firstServiceName);
        final Future<ServiceController<?>> firstServiceStart = testListener.expectServiceStart(firstServiceName);
        setModeListener.setMode(Mode.ACTIVE);
        final ServiceController<?> secondController = secondBuilder.install();
        assertController(secondController, secondServiceListenerAdded);
        assertController(secondController, secondServiceStart);
        final ServiceController<?> firstController = assertController(firstServiceName, firstServiceDependencyInstall);
        assertController (firstController, firstServiceStart);
    }

    @Test
    public void changeDownOnDemandToActive() throws Exception {
        final Future<ServiceController<?>> firstServiceListenerAdded = testListener.expectListenerAdded(firstServiceName);
        final Future<ServiceController<?>> firstServiceStart = testListener.expectServiceStart(firstServiceName);
        setModeListener.setMode(Mode.PASSIVE);
        final ServiceController<?> firstController = getDownOnDemandFirstBuilder().install();
        assertController(firstServiceName, firstController);
        assertController(firstController, firstServiceListenerAdded);
        assertController(firstController, firstServiceStart);
    }

    @Test
    public void changeFailedToStartOnDemandToActive() throws Exception {
        final ServiceBuilder<?> secondBuilder = getFailedToStartOnDemandSecondBuilder();
        final Future<ServiceController<?>> firstServiceDependencyInstall = testListener
                .expectImmediateDependencyInstall(firstServiceName);
        final Future<ServiceController<?>> firstServiceDependencyFailed = testListener
                .expectDependencyFailure(firstServiceName);
        final Future<StartException> secondServiceFailure = testListener.expectServiceFailure(secondServiceName);
        setModeListener.setMode(Mode.PASSIVE);
        final ServiceController<?> secondController = secondBuilder.install();
        assertFailure(secondController, secondServiceFailure);
        final ServiceController<?> firstController = assertController(firstServiceName, firstServiceDependencyInstall);
        assertController(firstController, firstServiceDependencyFailed);
    }

    @Test
    public void changeUpOnDemandToNull() throws Exception {
        final ServiceBuilder<?> secondBuilder = getUpOnDemandSecondBuilder();
        final Future<ServiceController<?>> secondServiceStart = testListener.expectServiceStart(secondServiceName);
        final Future<ServiceController<?>> firstServiceDependencyInstall = testListener
                .expectImmediateDependencyInstall(firstServiceName);
        final Future<ServiceController<?>> firstServiceStart = testListener.expectServiceStart(firstServiceName);
        final ServiceController<?> secondController = assertChangeModeToNullFails(secondBuilder, secondServiceName);
        assertController(secondController, secondServiceStart);
        final ServiceController<?> firstController = assertController(firstServiceName, firstServiceDependencyInstall);
        assertController(firstController, firstServiceStart);
    }

    @Test
    public void changeDownOnDemandToNull() throws Exception {
        final ServiceController<?> firstController = assertChangeModeToNullFails(getDownOnDemandFirstBuilder(),
                firstServiceName);
        assertController(firstServiceName, firstController);
    }

    @Test
    public void changeFailedToStartOnDemandToNull() throws Exception {
        final ServiceBuilder<?> secondBuilder = getFailedToStartOnDemandSecondBuilder();
        final Future<ServiceController<?>> firstServiceDependencyInstall = testListener
                .expectImmediateDependencyInstall(firstServiceName);
        final Future<ServiceController<?>> firstServiceDependencyFailed = testListener
                .expectDependencyFailure(firstServiceName);
        final Future<StartException> secondServiceFailure = testListener.expectServiceFailure(secondServiceName);
        final ServiceController<?> secondController = assertChangeModeToNullFails(secondBuilder, secondServiceName);
        assertFailure(secondController, secondServiceFailure);
        final ServiceController<?> firstController = assertController(firstServiceName, firstServiceDependencyInstall);
        assertController(firstController, firstServiceDependencyFailed);
    }

    /**
     * Returns the builder of a service named {@code firstServiceName} with initial mode {@link Mode#PASSIVE PASSIVE}.
     * If installed immediately, the controller will enter the {@code State#UP UP} state.
     * 
     * @return the ServiceController of the specified service.
     */
    private final ServiceBuilder<?> getUpPassiveFirstBuilder() throws Exception {
        return serviceContainer.addService(firstServiceName, Service.NULL).addListener(testListener, setModeListener)
                .setInitialMode(Mode.PASSIVE);
    }

    /**
     * Returns the builder of a service named {@code firstServiceName} with initial mode {@link Mode#PASSIVE PASSIVE}.
     * If installed immediately, the controller will enter the {@code State#DOWN DOWN} state, as it
     * has a missing dependency on a service named {@code secondServiceName}.
     * 
     * @return the ServiceController of the specified service.
     */
    private final ServiceBuilder<?> getDownPassiveFirstBuilder() throws Exception {
        return serviceContainer.addService(firstServiceName, Service.NULL).addListener(testListener, setModeListener)
                .addDependency(secondServiceName).setInitialMode(Mode.PASSIVE);
    }

    /**
     * Returns the builder of a service named {@code secondServiceName} with initial mode {@link Mode#PASSIVE PASSIVE}.
     * If installed immeditely, the controller will enter the {@link State#START_FAILED START_FAILED}
     * state, and it has a dependent named {@code firstServiceName}, waiting for the failure to be cleared so it can
     * start.
     * 
     * @return the ServiceController of the specified service.
     */
    private final ServiceBuilder<?> getFailedToStartPassiveSecondBuilder() throws Exception {
        final Future<ServiceController<?>> firstServiceInstall = testListener.expectListenerAdded(firstServiceName);
        serviceContainer.addService(firstServiceName, new FailToStartService(true)).addListener(testListener)
                .addDependency(secondServiceName).install();
        assertController(firstServiceName, firstServiceInstall);

        return serviceContainer.addService(secondServiceName, new FailToStartService(true))
                .addListener(testListener, setModeListener).setInitialMode(Mode.PASSIVE);
    }

    @Test
    public void changeUpPassiveToRemove() throws Exception {
        final Future<ServiceController<?>> firstServiceRemoval = testListener.expectServiceRemoval(firstServiceName);
        final Future<ServiceController<?>> firstServiceListenerAdded = testListener.expectListenerAdded(firstServiceName);
        setModeListener.setMode(Mode.REMOVE);
        final ServiceController<?> firstController = getUpPassiveFirstBuilder().install();
        assertController(firstController, firstServiceListenerAdded);
        assertController(firstController, firstServiceRemoval);
    }

    @Test
    public void changeDownPassiveToRemove() throws Exception {
        final Future<ServiceController<?>> firstServiceRemoval = testListener.expectServiceRemoval(firstServiceName);
        setModeListener.setMode(Mode.REMOVE);
        final ServiceController<?> firstController = getDownPassiveFirstBuilder().install();
        assertController(firstController, firstServiceRemoval);
    }

    @Test
    public void changeFailedToStartPassiveToRemove() throws Exception {
        final ServiceBuilder<?> secondBuilder = getFailedToStartPassiveSecondBuilder();

        final Future<ServiceController<?>> secondServiceRemoval = testListener.expectServiceRemoval(secondServiceName);
        final Future<ServiceController<?>> firstServiceDependencyInstall = testListener
                .expectImmediateDependencyInstall(firstServiceName);
        final Future<ServiceController<?>> firstServiceDependencyUninstall = testListener
                .expectImmediateDependencyUninstall(firstServiceName);
        setModeListener.setMode(Mode.REMOVE);
        final ServiceController<?> secondController = secondBuilder.install();

        assertController(secondController, secondServiceRemoval);
        final ServiceController<?> firstController = assertController(firstServiceName, firstServiceDependencyInstall);
        assertController(firstController, firstServiceDependencyUninstall);
    }

    @Test
    public void changeUpPassiveToNever() throws Exception {
        final Future<ServiceController<?>> firstServiceListenerAdded = testListener.expectListenerAdded(firstServiceName);
        setModeListener.setMode(Mode.NEVER);
        final ServiceController<?> firstController = getUpPassiveFirstBuilder().install();
        assertController(firstController, firstServiceListenerAdded);
        assertSame(State.DOWN, firstController.getState());
    }

    @Test
    public void changeDownPassiveToNever() throws Exception {
        final Future<ServiceController<?>> firstServiceListenerAdded = testListener.expectListenerAdded(firstServiceName);
        setModeListener.setMode(Mode.NEVER);
        final ServiceController<?> firstController = getDownPassiveFirstBuilder().install();
        assertController(firstController, firstServiceListenerAdded);
        assertSame(State.DOWN, firstController.getState());
    }

    @Test
    public void changeFailedToStartPassiveToNever() throws Exception {
        final ServiceBuilder<?> secondBuilder = getFailedToStartPassiveSecondBuilder();

        final Future<ServiceController<?>> secondServiceListenerAdded = testListener.expectListenerAdded(secondServiceName);
        final Future<ServiceController<?>> firstServiceDependencyInstall = testListener
                .expectImmediateDependencyInstall(firstServiceName);
        setModeListener.setMode(Mode.NEVER);
        final ServiceController<?> secondController = secondBuilder.install();

        assertController(secondController, secondServiceListenerAdded);
        assertController(firstServiceName, firstServiceDependencyInstall);
    }

    @Test
    public void changeUpPassiveToOnDemand() throws Exception {
        final Future<ServiceController<?>> firstServiceListenerAdded = testListener.expectListenerAdded(firstServiceName);
        setModeListener.setMode(Mode.ON_DEMAND);
        final ServiceController<?> firstController = getUpPassiveFirstBuilder().install();
        assertController(firstController, firstServiceListenerAdded);
        assertSame(State.DOWN, firstController.getState());
    }

    @Test
    public void changeDownPassiveToOnDemand() throws Exception {
        final Future<ServiceController<?>> firstServiceListenerAdded = testListener.expectListenerAdded(firstServiceName);
        setModeListener.setMode(Mode.ON_DEMAND);
        final ServiceController<?> firstController = getDownPassiveFirstBuilder().install();
        assertController(firstController, firstServiceListenerAdded);
        assertSame(State.DOWN, firstController.getState());
    }

    @Test
    public void changeFailedToStartPassiveToOnDemand() throws Exception {
        final ServiceBuilder<?> secondBuilder = getFailedToStartPassiveSecondBuilder();

        final Future<ServiceController<?>> secondServiceListenerAdded = testListener.expectListenerAdded(secondServiceName);
        final Future<StartException> secondServiceFailure = testListener.expectServiceFailure(secondServiceName);
        final Future<ServiceController<?>> firstServiceDependencyInstall = testListener
                .expectImmediateDependencyInstall(firstServiceName);
        final Future<ServiceController<?>> firstServiceDependencyFailed = testListener
                .expectDependencyFailure(firstServiceName);
        setModeListener.setMode(Mode.ON_DEMAND);
        final ServiceController<?> secondController = secondBuilder.install();

        assertController(secondController, secondServiceListenerAdded);
        assertFailure(secondController, secondServiceFailure);
        final ServiceController<?> firstController = assertController(firstServiceName, firstServiceDependencyInstall);
        assertController(firstServiceName, firstServiceDependencyInstall);
        assertController(firstController, firstServiceDependencyFailed);
    }

    @Test
    public void changeUpPassiveToPassive() throws Exception {
        final Future<ServiceController<?>> firstServiceListenerAdded = testListener.expectListenerAdded(firstServiceName);
        final Future<ServiceController<?>> firstServiceStart = testListener.expectServiceStart(firstServiceName);
        setModeListener.setMode(Mode.PASSIVE);
        final ServiceController<?> firstController = getUpPassiveFirstBuilder().install();
        assertController(firstController, firstServiceListenerAdded);
        assertController(firstController, firstServiceStart);
    }

    @Test
    public void changeDownPassiveToPassive() throws Exception {
        final Future<ServiceController<?>> firstServiceListenerAdded = testListener.expectListenerAdded(firstServiceName);
        setModeListener.setMode(Mode.PASSIVE);
        final ServiceController<?> firstController = getDownPassiveFirstBuilder().install();
        assertController(firstController, firstServiceListenerAdded);
        assertSame(State.DOWN, firstController.getState());
    }

    @Test
    public void changeFailedToStartPassiveToPassive() throws Exception {
        final ServiceBuilder<?> secondBuilder = getFailedToStartPassiveSecondBuilder();

        final Future<ServiceController<?>> secondServiceListenerAdded = testListener.expectListenerAdded(secondServiceName);
        final Future<StartException> secondServiceFailure = testListener.expectServiceFailure(secondServiceName);
        final Future<ServiceController<?>> firstServiceDependencyInstall = testListener
                .expectImmediateDependencyInstall(firstServiceName);
        final Future<ServiceController<?>> firstServiceDependencyFailed = testListener
                .expectDependencyFailure(firstServiceName);
        setModeListener.setMode(Mode.PASSIVE);
        final ServiceController<?> secondController = secondBuilder.install();

        assertController(secondController, secondServiceListenerAdded);
        assertFailure(secondController, secondServiceFailure);
        final ServiceController<?> firstController = assertController(firstServiceName, firstServiceDependencyInstall);
        assertController(firstServiceName, firstServiceDependencyInstall);
        assertController(firstController, firstServiceDependencyFailed);
    }

    @Test
    public void changeUpPassiveToActive() throws Exception {
        final Future<ServiceController<?>> firstServiceListenerAdded = testListener.expectListenerAdded(firstServiceName);
        final Future<ServiceController<?>> firstServiceStart = testListener.expectServiceStart(firstServiceName);
        setModeListener.setMode(Mode.ACTIVE);
        final ServiceController<?> firstController = getUpPassiveFirstBuilder().install();
        assertController(firstController, firstServiceListenerAdded);
        assertController(firstController, firstServiceStart);
    }

    @Test
    public void changeDownPassiveToActive() throws Exception {
        final Future<ServiceController<?>> firstServiceListenerAdded = testListener.expectListenerAdded(firstServiceName);
        setModeListener.setMode(Mode.ACTIVE);
        final ServiceController<?> firstController = getDownPassiveFirstBuilder().install();
        assertController(firstController, firstServiceListenerAdded);
        assertSame(State.DOWN, firstController.getState());
    }

    @Test
    public void changeFailedToStartPassiveToActive() throws Exception {
        final ServiceBuilder<?> secondBuilder = getFailedToStartPassiveSecondBuilder();

        final Future<ServiceController<?>> secondServiceListenerAdded = testListener.expectListenerAdded(secondServiceName);
        final Future<StartException> secondServiceFailure = testListener.expectServiceFailure(secondServiceName);
        final Future<ServiceController<?>> firstServiceDependencyInstall = testListener
                .expectImmediateDependencyInstall(firstServiceName);
        final Future<ServiceController<?>> firstServiceDependencyFailed = testListener
                .expectDependencyFailure(firstServiceName);
        setModeListener.setMode(Mode.ACTIVE);
        final ServiceController<?> secondController = secondBuilder.install();

        assertController(secondController, secondServiceListenerAdded);
        assertFailure(secondController, secondServiceFailure);
        final ServiceController<?> firstController = assertController(firstServiceName, firstServiceDependencyInstall);
        assertController(firstServiceName, firstServiceDependencyInstall);
        assertController(firstController, firstServiceDependencyFailed);
    }

    @Test
    public void changeUpPassiveToNull() throws Exception {
        final Future<ServiceController<?>> firstServiceStart = testListener.expectServiceStart(firstServiceName);
        final ServiceController<?> firstController = assertChangeModeToNullFails(getUpPassiveFirstBuilder(), firstServiceName);
        assertController(firstController, firstServiceStart);
    }

    @Test
    public void changeDownPassiveToNull() throws Exception {
        final ServiceController<?> firstController = assertChangeModeToNullFails(getDownPassiveFirstBuilder(), firstServiceName);
        assertSame(State.DOWN, firstController.getState());
    }

    @Test
    public void changeFailedToStartPassiveToNull() throws Exception {
        final ServiceBuilder<?> secondBuilder = getFailedToStartPassiveSecondBuilder();

        final Future<StartException> secondServiceFailure = testListener.expectServiceFailure(secondServiceName);
        final Future<ServiceController<?>> firstServiceDependencyInstall = testListener
                .expectImmediateDependencyInstall(firstServiceName);
        final Future<ServiceController<?>> firstServiceDependencyFailed = testListener
                .expectDependencyFailure(firstServiceName);
        final ServiceController<?> secondController = assertChangeModeToNullFails(secondBuilder, secondServiceName);

        assertFailure(secondController, secondServiceFailure);
        final ServiceController<?> firstController = assertController(firstServiceName, firstServiceDependencyInstall);
        assertController(firstServiceName, firstServiceDependencyInstall);
        assertController(firstController, firstServiceDependencyFailed);
    }

    /**
     * Returns the builder of a service named {@code firstServiceName} with initial mode {@link Mode#ACTIVE ACTIVE}. If
     * installed immediately, the controller will enter the {@code State#UP UP} state. 
     * 
     * @return the ServiceController of the specified service.
     */
    private final ServiceBuilder<?> getUpActiveFirstBuilder() throws Exception {
        return serviceContainer.addService(firstServiceName, Service.NULL).addListener(testListener, setModeListener)
                .setInitialMode(Mode.ACTIVE);
    }

    /**
     * Returns the builder of a service named {@code firstServiceName} with initial mode {@link Mode#ACTIVE ACTIVE}.
     * If installed immediately, the controller will enter the {@link State#DOWN DOWN} state, as it has a dependency on
     * a service named {@code secondServiceName} in the {@link State#START_FAILED START_FAILED} state.
     * 
     * @return the ServiceController of the specified service.
     */
    private final ServiceBuilder<?> getDownActiveFirstBuilder() throws Exception {
        final Future<ServiceController<?>> secondServiceInstall = testListener.expectListenerAdded(secondServiceName);
        final Future<StartException> secondServiceFailure = testListener.expectServiceFailure(secondServiceName);
        serviceContainer.addService(secondServiceName, new FailToStartService(true)).addListener(testListener).install();
        final ServiceController<?> secondController = assertController(secondServiceName, secondServiceInstall);
        assertFailure(secondController, secondServiceFailure);
        
        return serviceContainer.addService(firstServiceName, Service.NULL).addListener(testListener, setModeListener)
            .addDependency(secondServiceName).setInitialMode(Mode.ACTIVE);
    }

    /**
     * Returns the builder of a service named {@code secondServiceName} with initial mode {@link Mode#ACTIVE ACTIVE}.
     * If installed immediately, the controller will enter the {@code State#START_FAILED START_FAILED}
     * state, and has a dependent named {@code firstServiceName} waiting for the failure to be cleared so it can start.
     * 
     * @return the ServiceController of the specified service.
     */
    private final ServiceBuilder<?> getFailedToStartActiveSecondBuilder() throws Exception {
        final Future<ServiceController<?>> firstServiceInstall = testListener.expectListenerAdded(firstServiceName);
        serviceContainer.addService(firstServiceName, new FailToStartService(true)).addListener(testListener)
        .addDependency(secondServiceName).install();
        assertController(firstServiceName, firstServiceInstall);

        return serviceContainer.addService(secondServiceName, new FailToStartService(true))
            .addListener(testListener, setModeListener).setInitialMode(Mode.ACTIVE);
    }

    @Test
    public void changeUpActiveToRemove() throws Exception {
        final Future<ServiceController<?>> firstServiceListenerAdded = testListener.expectListenerAdded(firstServiceName);
        final Future<ServiceController<?>> firstServiceRemoval = testListener.expectServiceRemoval(firstServiceName);
        setModeListener.setMode(Mode.REMOVE);
        final ServiceController<?> firstController = getUpActiveFirstBuilder().install();
        assertController(firstController, firstServiceListenerAdded);
        assertController(firstController, firstServiceRemoval);
    }

    @Test
    public void changeDownActiveToRemove() throws Exception {
        final ServiceBuilder<?> firstBuilder = getDownActiveFirstBuilder();
        final Future<ServiceController<?>> firstServiceRemoval = testListener.expectServiceRemoval(firstServiceName);
        setModeListener.setMode(Mode.REMOVE);
        final ServiceController<?> firstController = firstBuilder.install();
        assertController(firstController, firstServiceRemoval);
    }

    @Test
    public void changeFailedToStartActiveToRemove() throws Exception {
        final ServiceBuilder<?> secondBuilder = getFailedToStartActiveSecondBuilder();

        final Future<ServiceController<?>> secondServiceListenerAdded = testListener.expectListenerAdded(secondServiceName);
        final Future<ServiceController<?>> secondServiceRemoval = testListener.expectServiceRemoval(secondServiceName);
        final Future<ServiceController<?>> firstServiceDependencyInstall = testListener
            .expectImmediateDependencyInstall(firstServiceName);
        final Future<ServiceController<?>> firstServiceDependencyUninstall = testListener
            .expectImmediateDependencyUninstall(firstServiceName);
        setModeListener.setMode(Mode.REMOVE);
        final ServiceController<?> secondController = secondBuilder.install();
        assertController(secondController, secondServiceListenerAdded);
        assertController(secondController, secondServiceRemoval);
        final ServiceController<?> firstController = assertController(firstServiceName, firstServiceDependencyInstall);
        assertController(firstController, firstServiceDependencyUninstall);
    }

    @Test
    public void changeUpActiveToNever() throws Exception {
        final Future<ServiceController<?>> firstServiceListenerAdded = testListener.expectListenerAdded(firstServiceName);
        setModeListener.setMode(Mode.NEVER);
        final ServiceController<?> firstController = getUpActiveFirstBuilder().install();
        assertController(firstController, firstServiceListenerAdded);
        assertSame(State.DOWN, firstController.getState());
    }

    @Test
    public void changeDownActiveToNever() throws Exception {
        final Future<ServiceController<?>> firstServiceListenerAdded = testListener.expectListenerAdded(firstServiceName);
        final Future<ServiceController<?>> firstServiceDependencyFailure = testListener.expectDependencyFailure(firstServiceName);
        setModeListener.setMode(Mode.NEVER);
        final ServiceController<?> firstController = getDownActiveFirstBuilder().install();
        assertController(firstController, firstServiceListenerAdded);
        assertController(firstController, firstServiceDependencyFailure);
        assertSame(State.DOWN, firstController.getState());
    }

    @Test
    public void changeFailedToStartActiveToNever() throws Exception {
        final ServiceBuilder<?> secondBuilder = getFailedToStartActiveSecondBuilder();

        final Future<ServiceController<?>> secondServiceListenerAdded = testListener.expectListenerAdded(secondServiceName);
        final Future<ServiceController<?>> firstServiceDependencyInstall = testListener
            .expectImmediateDependencyInstall(firstServiceName);
        setModeListener.setMode(Mode.NEVER);
        final ServiceController<?> secondController = secondBuilder.install();
        assertController(secondController, secondServiceListenerAdded);
        assertController(firstServiceName, firstServiceDependencyInstall);
    }

    @Test
    public void changeUpActiveToOnDemand() throws Exception {
        final Future<ServiceController<?>> firstServiceListenerAdded = testListener.expectListenerAdded(firstServiceName);
        setModeListener.setMode(Mode.ON_DEMAND);
        final ServiceController<?> firstController = getUpActiveFirstBuilder().install();
        assertController(firstController, firstServiceListenerAdded);
        assertSame(State.DOWN, firstController.getState());
    }

    @Test
    public void changeDownActiveToOnDemand() throws Exception {
        final Future<ServiceController<?>> firstServiceListenerAdded = testListener.expectListenerAdded(firstServiceName);
        final Future<ServiceController<?>> firstServiceDependencyFailure = testListener.expectDependencyFailure(firstServiceName);
        setModeListener.setMode(Mode.ON_DEMAND);
        final ServiceController<?> firstController = getDownActiveFirstBuilder().install();
        assertController(firstController, firstServiceListenerAdded);
        assertController(firstController, firstServiceDependencyFailure);
        assertSame(State.DOWN, firstController.getState());
    }

    @Test
    public void changeFailedToStartActiveToOnDemand() throws Exception {
        final ServiceBuilder<?> secondBuilder = getFailedToStartActiveSecondBuilder();

        final Future<ServiceController<?>> secondServiceListenerAdded = testListener.expectListenerAdded(secondServiceName);
        final Future<StartException> secondServiceFailure = testListener.expectServiceFailure(secondServiceName);
        final Future<ServiceController<?>> firstServiceDependencyInstall = testListener.expectImmediateDependencyInstall(firstServiceName);
        final Future<ServiceController<?>> firstServiceDependencyFailure = testListener.expectDependencyFailure(firstServiceName);
        setModeListener.setMode(Mode.ON_DEMAND);
        final ServiceController<?> secondController = secondBuilder.install();
        assertController(secondController, secondServiceListenerAdded);
        assertFailure(secondController, secondServiceFailure);
        assertController(firstServiceName, firstServiceDependencyInstall);
        assertController(firstServiceName, firstServiceDependencyFailure);
    }

    @Test
    public void changeUpActiveToPassive() throws Exception {
        final Future<ServiceController<?>> firstServiceListenerAdded = testListener.expectListenerAdded(firstServiceName);
        final Future<ServiceController<?>> firstServiceStart = testListener.expectServiceStart(firstServiceName);
        setModeListener.setMode(Mode.PASSIVE);
        final ServiceController<?> firstController = getUpActiveFirstBuilder().install();
        assertController(firstController, firstServiceListenerAdded);
        assertController(firstController, firstServiceStart);
    }

    @Test
    public void changeDownActiveToPassive() throws Exception {
        final Future<ServiceController<?>> firstServiceListenerAdded = testListener.expectListenerAdded(firstServiceName);
        final Future<ServiceController<?>> firstServiceDependencyFailure = testListener.expectDependencyFailure(firstServiceName);
        setModeListener.setMode(Mode.PASSIVE);
        final ServiceController<?> firstController = getDownActiveFirstBuilder().install();
        assertController(firstController, firstServiceListenerAdded);
        assertController(firstController, firstServiceDependencyFailure);
        assertSame(State.DOWN, firstController.getState());
    }

    @Test
    public void changeFailedToStartActiveToPassive() throws Exception {
        final ServiceBuilder<?> secondBuilder = getFailedToStartActiveSecondBuilder();

        final Future<ServiceController<?>> secondServiceListenerAdded = testListener.expectListenerAdded(secondServiceName);
        final Future<StartException> secondServiceFailure = testListener.expectServiceFailure(secondServiceName);
        final Future<ServiceController<?>> firstServiceDependencyInstall = testListener.expectImmediateDependencyInstall(firstServiceName);
        final Future<ServiceController<?>> firstServiceDependencyFailure = testListener.expectDependencyFailure(firstServiceName);
        setModeListener.setMode(Mode.PASSIVE);
        final ServiceController<?> secondController = secondBuilder.install();
        assertController(secondController, secondServiceListenerAdded);
        assertFailure(secondController, secondServiceFailure);
        assertController(firstServiceName, firstServiceDependencyInstall);
        assertController(firstServiceName, firstServiceDependencyFailure);
    }

    @Test
    public void changeUpActiveToActive() throws Exception {
        final Future<ServiceController<?>> firstServiceListenerAdded = testListener.expectListenerAdded(firstServiceName);
        final Future<ServiceController<?>> firstServiceStart = testListener.expectServiceStart(firstServiceName);
        setModeListener.setMode(Mode.ACTIVE);
        final ServiceController<?> firstController = getUpActiveFirstBuilder().install();
        assertController(firstController, firstServiceListenerAdded);
        assertController(firstController, firstServiceStart);
    }

    @Test
    public void changeDownActiveToActive() throws Exception {
        final Future<ServiceController<?>> firstServiceListenerAdded = testListener.expectListenerAdded(firstServiceName);
        final Future<ServiceController<?>> firstServiceDependencyFailure = testListener.expectDependencyFailure(firstServiceName);
        setModeListener.setMode(Mode.ACTIVE);
        final ServiceController<?> firstController = getDownActiveFirstBuilder().install();
        assertController(firstController, firstServiceListenerAdded);
        assertController(firstController, firstServiceDependencyFailure);
        assertSame(State.DOWN, firstController.getState());
    }

    @Test
    public void changeFailedToStartActiveToActive() throws Exception {
        final ServiceBuilder<?> secondBuilder = getFailedToStartActiveSecondBuilder();

        final Future<ServiceController<?>> secondServiceListenerAdded = testListener.expectListenerAdded(secondServiceName);
        final Future<StartException> secondServiceFailure = testListener.expectServiceFailure(secondServiceName);
        final Future<ServiceController<?>> firstServiceDependencyInstall = testListener.expectImmediateDependencyInstall(firstServiceName);
        final Future<ServiceController<?>> firstServiceDependencyFailure = testListener.expectDependencyFailure(firstServiceName);
        setModeListener.setMode(Mode.ACTIVE);
        final ServiceController<?> secondController = secondBuilder.install();
        assertController(secondController, secondServiceListenerAdded);
        assertFailure(secondController, secondServiceFailure);
        assertController(firstServiceName, firstServiceDependencyInstall);
        assertController(firstServiceName, firstServiceDependencyFailure);
    }

    @Test
    public void changeUpActiveToNull() throws Exception {
        final Future<ServiceController<?>> firstServiceStart = testListener.expectServiceStart(firstServiceName);
        final ServiceController<?> firstController = assertChangeModeToNullFails(getUpActiveFirstBuilder(), firstServiceName);
        assertController(firstController, firstServiceStart);
    }

    @Test
    public void changeDownActiveToNull() throws Exception {
        final Future<ServiceController<?>> firstServiceDependencyFailure = testListener.expectDependencyFailure(firstServiceName);
        final ServiceController<?> firstController = assertChangeModeToNullFails(getDownActiveFirstBuilder(), firstServiceName);
        assertController(firstController, firstServiceDependencyFailure);
        assertSame(State.DOWN, firstController.getState());
    }

    @Test
    public void changeFailedToStartActiveToNull() throws Exception {
        final ServiceBuilder<?> secondBuilder = getFailedToStartActiveSecondBuilder();

        final Future<StartException> secondServiceFailure = testListener.expectServiceFailure(secondServiceName);
        final Future<ServiceController<?>> firstServiceDependencyInstall = testListener.expectImmediateDependencyInstall(firstServiceName);
        final Future<ServiceController<?>> firstServiceDependencyFailure = testListener.expectDependencyFailure(firstServiceName);
        final ServiceController<?> secondController = assertChangeModeToNullFails(secondBuilder, secondServiceName);
        assertFailure(secondController, secondServiceFailure);
        assertController(firstServiceName, firstServiceDependencyInstall);
        assertController(firstServiceName, firstServiceDependencyFailure);
    }

    static class SetModeListener implements ServiceListener<Object> {
        private final StringBuffer unexpectedCalls;
        private Mode mode;
        private RuntimeException exception;

        public SetModeListener() {
            unexpectedCalls = new StringBuffer();
        }

        public void setMode(Mode mode) {
            this.mode = mode;
        }

        public void assertNoUnexpectedCalls() {
            assertEquals("Unexpected calls were made to this listener:\n" + unexpectedCalls.toString(), 0,
                    unexpectedCalls.length());
        }

        public void assertFailure(Class<? extends RuntimeException> expected) {
            assertNotNull(expected.toString() + " expected", exception);
            assertSame(expected, exception.getClass());
        }

        @Override
        public void listenerAdded(ServiceController<?> controller) {
            try {
                controller.removeListener(this);
                controller.setMode(mode);
            } catch (RuntimeException e) {
                exception = e;
                throw e;
            }
        }

        @Override
        public void serviceStartRequested(final ServiceController<?> controller) {
            unexpectedCalls.append("serviceStartRequested\n");
        }

        @Override
        public void serviceStartRequestCleared(final ServiceController<?> controller) {
            unexpectedCalls.append("serviceStartRequestCleared\n");
        }

        @Override
        public void serviceStarting(ServiceController<?> controller) {
            unexpectedCalls.append("serviceStarting\n");
        }

        @Override
        public void failedServiceStarting(final ServiceController<?> serviceController) {
            unexpectedCalls.append("failedServiceStarting\n");
        }

        @Override
        public void serviceStarted(ServiceController<?> controller) {
            unexpectedCalls.append("serviceStarted\n");
        }

        @Override
        public void serviceFailed(ServiceController<?> controller, StartException reason) {
            unexpectedCalls.append("serviceFailed\n");
        }

        @Override
        public void serviceStopRequested(final ServiceController<?> controller) {
            unexpectedCalls.append("serviceStopRequested\n");
        }

        @Override
        public void serviceStopRequestCleared(final ServiceController<?> controller) {
            unexpectedCalls.append("serviceStopRequestCleared\n");
        }

        @Override
        public void serviceStopping(ServiceController<?> controller) {
            unexpectedCalls.append("serviceStopping\n");
        }

        @Override
        public void serviceStopped(ServiceController<?> controller) {
            unexpectedCalls.append("serviceStopped\n");
        }

        @Override
        public void failedServiceStopped(final ServiceController<? extends Object> controller) {
            unexpectedCalls.append("failedServiceStopped\n");
        }

        @Override
        public void serviceRemoveRequested(final ServiceController<?> controller) {
            unexpectedCalls.append("serviceRemoving\n");
        }

        @Override
        public void serviceRemoved(ServiceController<?> controller) {
            unexpectedCalls.append("serviceRemoved\n");
        }

        @Override
        public void dependencyFailed(ServiceController<?> controller) {
            unexpectedCalls.append("dependencyFailed\n");
        }

        @Override
        public void dependencyFailureCleared(ServiceController<?> controller) {
            unexpectedCalls.append("dependencyFailureCleared\n");
        }

        @Override
        public void immediateDependencyUninstalled(ServiceController<?> controller) {
            unexpectedCalls.append("immediateDependencyUninstalled\n");
        }

        @Override
        public void immediateDependencyInstalled(ServiceController<?> controller) {
            unexpectedCalls.append("immediateDependencyInstalled\n");
        }

        @Override
        public void transitiveDependencyUninstalled(ServiceController<?> controller) {
            unexpectedCalls.append("transitiveDependencyUninstalled\n");
        }

        @Override
        public void transitiveDependencyInstalled(ServiceController<?> controller) {
            unexpectedCalls.append("transitiveDependencyInstalled\n");
        }
    }
}