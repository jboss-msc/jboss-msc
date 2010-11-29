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

package org.jboss.msc.racecondition;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import java.util.concurrent.Future;

import org.jboss.msc.service.AbstractServiceTest;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.util.TestServiceListener;
import org.junit.Test;

/**
 * Tests the service transition from {@code STOP_REQUESTED} state to {@code UP} state.
 * 
 * @author <a href="mailto:flavia.rainone@jboss.com">Flavia Rainone</a>
 *
 */
public class StopRequestedToUpTransitionTestCase extends AbstractServiceTest {
  
    /*
     * In order to test this specific scenario, we use Byteman to insert a monitor in the specific moment
     *  where the transition from STOP_REQUESTED to the next state is going to occur.
     *  This monitor will force the thread to wait until upperCount is incremented to 1.
     */
    @Test
    public void test() throws Exception {
       ServiceName serviceName = ServiceName.of("service");
        TestServiceListener serviceListener = new TestServiceListener();

        // install service as usual
        Future<ServiceController<?>> serviceStart = serviceListener.expectServiceStart(serviceName);
        serviceContainer.addService(serviceName, Service.NULL).addListener(serviceListener).install();
        ServiceController<?> serviceController = assertController(serviceName, serviceStart);

        Future<ServiceController<?>> serviceStopping = serviceListener.expectNoServiceStopping(serviceName);
        Future<ServiceController<?>> serviceStop = serviceListener.expectNoServiceStop(serviceName);
        serviceStart = serviceListener.expectNoServiceStart(serviceName);
        // set the mode to NEVER, so that serviceController enters STOP_REQUESTED state
        serviceController.setMode(ServiceController.Mode.NEVER);
        // set the mode to ACTIVE, so that serviceController transitions to UP state
        serviceController.setMode(ServiceController.Mode.ACTIVE);
        // no notifications are expected
        assertNull(serviceStop.get());
        assertNull(serviceStopping.get());
        assertNull(serviceStart.get());
        // service should still be in the up state
        assertSame(ServiceController.State.UP, serviceController.getState());
    }
}
