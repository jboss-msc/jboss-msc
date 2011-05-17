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
import org.jboss.msc.service.ServiceController.Mode;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.util.TestServiceListener;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Creates a scenario where a service remove request is cleared before the service is removed.
 * The remove request clear is sucessful because it is performed before the service enters {@code REMOVING} substate.
 * 
 * @author <a href="mailto:flavia.rainone@jboss.com">Flavia Rainone</a>
 */
@RunWith(BMUnitRunner.class)
@BMScript(dir="src/test/resources")
public class ClearRemoveRequestOnDownServiceTestCase extends AbstractRaceConditionTest {
    private static final TestServiceListener testListener = new TestServiceListener();
    private static final ServiceName serviceName = ServiceName.of("service");

    @Test
    public void test() throws Exception {
        Future<ServiceController<?>> serviceStart = testListener.expectServiceStart(serviceName);
        final ServiceController<?> serviceController = serviceContainer.addService(serviceName, Service.NULL)
            .addListener(testListener).install();
        assertController(serviceController, serviceStart);

        final Future<ServiceController<?>> serviceRemovalRequest = testListener.expectServiceRemovalRequest(serviceName);
        final Future<ServiceController<?>> serviceStop = testListener.expectServiceStop(serviceName);
        serviceController.setMode(Mode.REMOVE);
        assertController(serviceController, serviceRemovalRequest);
        

        final Future<ServiceController<?>> serviceRemovalRequestCleared = testListener.expectServiceRemovalRequestCleared(serviceName);
        serviceStart = testListener.expectServiceStart(serviceName);
        serviceController.setMode(Mode.ACTIVE);
        assertController(serviceController, serviceStop);
        assertController(serviceController, serviceRemovalRequestCleared);
        assertController(serviceController, serviceStart);
    }
}
