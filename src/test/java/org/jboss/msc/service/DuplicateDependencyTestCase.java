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

import java.util.concurrent.Future;

import org.jboss.msc.service.ServiceController.Mode;
import org.jboss.msc.service.ServiceController.State;
import org.jboss.msc.service.util.FailToStartService;
import org.jboss.msc.util.TestServiceListener;
import org.junit.Before;
import org.junit.Test;

/**
 * Test for scenarios where a service has a duplicate dependency on another service (by depending on
 * aliases).
 * 
 * @author <a href="mailto:flavia.rainone@jboss.com">Flavia Rainone</a>
 */
public class DuplicateDependencyTestCase extends AbstractServiceTest {

    private static final ServiceName firstServiceName = ServiceName.of("first", "service");
    private static final ServiceName secondServiceName = ServiceName.of("second", "service");
    private static final ServiceName thirdServiceName = ServiceName.of("third", "service");
    private static final ServiceName secondServiceAlias1 = ServiceName.of("second", "alias", "1");
    private static final ServiceName secondServiceAlias2 = ServiceName.of("second", "alias", "2");
    private static final ServiceName secondServiceAlias3 = ServiceName.of("second", "alias", "3");
    private static final ServiceName secondServiceAlias4 = ServiceName.of("second", "alias", "4");
    private static final ServiceName secondServiceAlias5 = ServiceName.of("second", "alias", "5");
    private static final ServiceName firstServiceAlias = ServiceName.of("first", "alias");
    private TestServiceListener testListener;

    @Before
    public void setTestListener() {
        testListener = new TestServiceListener();
    }

    @Test
    public void duplicateDependency() throws Exception {
        final Future<ServiceController<?>> secondServiceStart = testListener.expectServiceStart(secondServiceName);
        final ServiceController<?> secondController = serviceContainer.addService(secondServiceName, Service.NULL)
            .addAliases(secondServiceAlias1, secondServiceAlias2)
            .addListener(testListener)
            .install();
        assertController(secondServiceName, secondController);
        assertController(secondController, secondServiceStart);

        final Future<ServiceController<?>> firstServiceStart = testListener.expectServiceStart(firstServiceName);
        final ServiceController<?> firstController = serviceContainer.addService(firstServiceName, Service.NULL)
            .addDependencies(secondServiceName, secondServiceAlias2)
            .addListener(testListener)
            .install();
        assertController(firstServiceName, firstController);
        assertController(firstController, firstServiceStart);
    }

    @Test
    public void duplicateDependencyWithMissingDependency() throws Exception {
        
        final Future<ServiceController<?>> secondServiceStart = testListener.expectServiceStart(secondServiceName);
        final ServiceController<?> secondController = serviceContainer.addService(secondServiceName, Service.NULL)
            .addAliases(secondServiceAlias1, secondServiceAlias2)
            .addListener(testListener)
            .install();
        assertController(secondServiceName, secondController);
        assertController(secondController, secondServiceStart);

        final Future<ServiceController<?>> firstServiceListenerAdded = testListener.expectListenerAdded(firstServiceName);
        final Future<ServiceController<?>> firstServiceDependencyMissing = testListener.expectImmediateDependencyUninstall(firstServiceName);
        final ServiceController<?> firstController = serviceContainer.addService(firstServiceName, Service.NULL)
            .addAliases(firstServiceAlias)
            .addDependencies(secondServiceName, secondServiceAlias2, thirdServiceName)
            .addListener(testListener)
            .install();
        assertController(firstServiceAlias, firstController);
        assertController(firstController, firstServiceListenerAdded);
        assertController(firstController, firstServiceDependencyMissing);
        assertEquals(State.DOWN, firstController.getState());
    }

    @Test
    public void duplicateDependencyFailsToStart() throws Exception {
        Future<StartException> secondServiceFailure = testListener.expectServiceFailure(secondServiceName);
        ServiceController<?> secondController = serviceContainer.addService(secondServiceName, new FailToStartService(true))
            .addAliases(secondServiceAlias1, secondServiceAlias2, secondServiceAlias3, secondServiceAlias4, secondServiceAlias5)
            .addListener(testListener)
            .install();
        assertController(secondServiceName, secondController);
        assertController(secondServiceAlias3, secondController);
        assertFailure(secondServiceAlias4, secondServiceFailure);

        Future<ServiceController<?>> firstServiceListenerAdded = testListener.expectListenerAdded(firstServiceName);
        Future<ServiceController<?>> firstServiceDependencyFailure = testListener.expectDependencyFailure(firstServiceName);
        Future<ServiceController<?>> thirdServiceListenerAdded = testListener.expectListenerAdded(thirdServiceName);
        Future<ServiceController<?>> thirdServiceDependencyFailure = testListener.expectDependencyFailure(thirdServiceName);
        serviceContainer.addService(firstServiceName, Service.NULL)
            .addDependencies(secondServiceName, secondServiceAlias2, secondServiceAlias5, thirdServiceName)
            .addListener(testListener)
            .install();
        serviceContainer.addService(thirdServiceName, Service.NULL)
            .addDependencies(secondServiceAlias1, secondServiceAlias2, secondServiceAlias3, secondServiceAlias4, secondServiceAlias5)
            .addListener(testListener)
            .install();
        ServiceController<?> firstController = assertController(firstServiceName, firstServiceListenerAdded);
        assertController(firstController, firstServiceDependencyFailure);
        assertEquals(State.DOWN, firstController.getState());
        ServiceController<?> thirdController = assertController(thirdServiceName, thirdServiceListenerAdded);
        assertController(thirdController, thirdServiceDependencyFailure);
        assertEquals(State.DOWN, thirdController.getState());

        Future<ServiceController<?>> firstServiceDependencyFailureCleared = testListener.expectDependencyFailureCleared(firstServiceName);
        Future<ServiceController<?>> thirdServiceDependencyFailureCleared = testListener.expectDependencyFailureCleared(thirdServiceName);
        secondController.setMode(Mode.NEVER);
        assertController(firstController, firstServiceDependencyFailureCleared);
        assertController(thirdController, thirdServiceDependencyFailureCleared);

        Future<ServiceController<?>> firstServiceStart = testListener.expectServiceStart(firstServiceName);
        Future<ServiceController<?>> secondServiceStart = testListener.expectServiceStart(secondServiceName);
        Future<ServiceController<?>> thirdServiceStart = testListener.expectServiceStart(thirdServiceName);
        secondController.setMode(Mode.ON_DEMAND);
        assertController(secondController, secondServiceStart);
        assertController(firstController, firstServiceStart);
        assertController(thirdController, thirdServiceStart);
    }
}
