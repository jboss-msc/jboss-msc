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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.jboss.msc.service.util.FailToStartService;
import org.jboss.msc.service.util.TestTask;
import org.junit.Test;

/**
 * Test for {@link TimingServiceListener}.
 * 
 * @author <a href="mailto:flavia.rainone@jboss.com">Flavia Rainone</a>
 */
public class TimingServiceListenerTestCase extends AbstractServiceTest {

    @Test
    public void listenSingleServiceWithFinishedTask() throws Exception {
        final TestTask finishedTask = new TestTask();
        final TimingServiceListener timingServiceListener = new TimingServiceListener(finishedTask);
        serviceContainer.addListener(timingServiceListener);

        // install service1
        serviceContainer.addService(ServiceName.of("service1"), Service.NULL).install();
        // timing service listener is not finished yet
        assertFalse(timingServiceListener.finished());
        // finish the batch listened by timing service listener
        timingServiceListener.finishBatch();
        // now finished task should have been executed
        assertTrue(finishedTask.get());
        // timing service listener is finished
        assertTrue(timingServiceListener.finished());
        // the total number of services listened is 1
        assertEquals(1, timingServiceListener.getTotalCount());
        // the remaining number of services to listen is 0
        assertEquals(0, timingServiceListener.getRemainingCount());
        // the time measured by timing service listener should be positive
        assertTrue(timingServiceListener.getElapsedTime() >= 0);
    }

    @Test
    public void listenSingleService() throws Exception {
        final TimingServiceListener timingServiceListener = new TimingServiceListener();
        serviceContainer.addListener(timingServiceListener);

        // install service 1
        serviceContainer.addService(ServiceName.of("service1"), Service.NULL).install();
        // timing service listener is not finished
        assertFalse(timingServiceListener.finished());
        // finished time service listener batch
        timingServiceListener.finishBatch();
        // should have listened to only one service
        assertEquals(1, timingServiceListener.getTotalCount());
        long time = -1;
        // get the elapsed time
        while (time == -1) {
            time = timingServiceListener.getElapsedTime();
        }
        // time should be bigger than zero
        assertTrue("Time is " + time, time >= 0);
        // timing service listener should have finished
        assertTrue(timingServiceListener.finished());
    }

    @Test
    public void listenFailToStartServiceWithFinishedTask() throws Exception {
        final TestTask finishedTask = new TestTask();
        final TimingServiceListener timingServiceListener = new TimingServiceListener(finishedTask);
        serviceContainer.addListener(timingServiceListener);

        // install service 1
        serviceContainer.addService(ServiceName.of("service1"), new FailToStartService(true)).install();
        // timing service listener is not finished yet
        assertFalse(timingServiceListener.finished());
        // until we tell it to finish the batch
        timingServiceListener.finishBatch();
        // finished task should have executed now
        assertTrue(finishedTask.get());
        // timing service listener is finished
        assertTrue(timingServiceListener.finished());
        // should have listened to only one service
        assertEquals(1, timingServiceListener.getTotalCount());
        // no services left to listen to
        assertEquals(0, timingServiceListener.getRemainingCount());
        // the measured time should be positive
        assertTrue(timingServiceListener.getElapsedTime() >= 0);
    }

    @Test
    public void listenFailToStartService() throws Exception {
        final TimingServiceListener timingServiceListener = new TimingServiceListener();
        serviceContainer.addListener(timingServiceListener);

        // install service 1, a fail to start service set to fail on the first attempt to start
        serviceContainer.addService(ServiceName.of("service1"), new FailToStartService(true)).install();
        // timing service listener is not finished yet...
        assertFalse(timingServiceListener.finished());
        // tell timing service listener to finish the batch
        timingServiceListener.finishBatch();
        // the total count of listened to services is one
        assertEquals(1, timingServiceListener.getTotalCount());
        long time = -1;
        // wtai for the elapsed time result
        while (time == -1) {
            time = timingServiceListener.getElapsedTime();
        }
        // elapsed time should be positive
        assertTrue("Time is " + time, time >= 0);
        // now timing service listener should have finished listening the batch of services
        assertTrue(timingServiceListener.finished());
    }

    @Test
    public void listenServicesWithFinishedTask() throws Exception {
        final TestTask finishedTask = new TestTask();
        final TimingServiceListener timingServiceListener = new TimingServiceListener(finishedTask);
        serviceContainer.addListener(timingServiceListener);
        // install service1, service2, and service3
        serviceContainer.addService(ServiceName.of("service1"), Service.NULL).install();
        serviceContainer.addService(ServiceName.of("service2"), Service.NULL).install();
        serviceContainer.addService(ServiceName.of("service3"), Service.NULL).install();
        // timing servie listener should not have finished
        assertFalse(timingServiceListener.finished());
        // tell timing service listener to finish the batch
        timingServiceListener.finishBatch();
        // finished task should complete
        assertTrue(finishedTask.get());
        // timing service listener should have finished now
        assertTrue(timingServiceListener.finished());
        // the number of listened services is 3
        assertEquals(3, timingServiceListener.getTotalCount());
        // no service left to listen
        assertEquals(0, timingServiceListener.getRemainingCount());
        // a positive elapsed time is expected
        assertTrue(timingServiceListener.getElapsedTime() >= 0);
    }

    @Test
    public void listenServices() throws Exception {
        TimingServiceListener timingServiceListener = new TimingServiceListener();
        serviceContainer.addListener(timingServiceListener);
        // install service1, service2, and service3
        serviceContainer.addService(ServiceName.of("service1"), Service.NULL).install();
        serviceContainer.addService(ServiceName.of("service2"), Service.NULL).install();
        serviceContainer.addService(ServiceName.of("service3"), Service.NULL).install();
        // timing service listener should not be finished
        assertFalse(timingServiceListener.finished());
        // tell timing servie listener to finish the batch
        timingServiceListener.finishBatch();
        // wait for elapsed time to be calculated
        long time = -1;
        while (time == -1) {
            time = timingServiceListener.getElapsedTime();
        }
        assertTrue("Time is " + time, time >= 0);
        // the number of listened services is 3
        assertEquals(3, timingServiceListener.getTotalCount());
        // timing service listener is expected to be finished now
        assertTrue(timingServiceListener.finished());
    }

    @Test
    public void listenMixedServicesWithFinishedTask() throws Exception {
        TestTask finishedTask = new TestTask();
        TimingServiceListener timingServiceListener = new TimingServiceListener(finishedTask);
        serviceContainer.addListener(timingServiceListener);
        // install service1, service2, and service3
        // service1 and 3 should start normally, but service2 will fail
        serviceContainer.addService(ServiceName.of("service1"), Service.NULL).install();
        serviceContainer.addService(ServiceName.of("service2"), new FailToStartService(true)).install();
        serviceContainer.addService(ServiceName.of("service3"), Service.NULL).install();
        // timing service listener is not finished
        assertFalse(timingServiceListener.finished());
        // finish the batch
        timingServiceListener.finishBatch();
        // not timing service listener should be finished
        assertTrue(timingServiceListener.finished());
        // finished task is executed
        assertTrue(finishedTask.get());
        // 3 services were listened to
        assertEquals(3, timingServiceListener.getTotalCount());
        // no service remaining
        assertEquals(0, timingServiceListener.getRemainingCount());
        // a positive elapsed time is expected
        assertTrue(timingServiceListener.getElapsedTime() >= 0);
    }

    @Test
    public void listenMixedServices() throws Exception {
        final TimingServiceListener timingServiceListener = new TimingServiceListener(null);
        serviceContainer.addListener(timingServiceListener);
        // install service1, service2, and service3. 
        // service2 will start normally, but service1 and service3 should fail to start
        serviceContainer.addService(ServiceName.of("service1"), new FailToStartService(true)).install();
        serviceContainer.addService(ServiceName.of("service2"), Service.NULL).install();
        serviceContainer.addService(ServiceName.of("service3"), new FailToStartService(true)).install();
        // timing service listener is not finished
        assertFalse(timingServiceListener.finished());
        // finish the batch
        timingServiceListener.finishBatch();
        // 3 services were listened
        assertEquals(3, timingServiceListener.getTotalCount());
        long time = -1;
        while (time == -1) {
            time = timingServiceListener.getElapsedTime();
        }
        // a positive elapsed time is expected
        assertTrue("Time is " + time, time >= 0);
        // timing service listener is finished now
        assertTrue(timingServiceListener.finished());
    }
}
