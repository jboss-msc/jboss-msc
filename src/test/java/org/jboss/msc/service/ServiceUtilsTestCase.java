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

import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;

import org.jboss.msc.service.ServiceController.State;
import org.jboss.msc.service.util.TestTask;
import org.jboss.msc.util.TestServiceListener;
import org.junit.Test;

/**
 * Test for {@link ServiceUtils}.
 * 
 * @author <a href="mailto:flavia.rainone@jboss.com">Flavia Rainone</a>
 */
public class ServiceUtilsTestCase extends AbstractServiceTest {

    @Test
    public void undeployNothing() throws Exception {
        final TestTask completeTask = new TestTask();
        // undeploy nothing with a complete task
        ServiceUtils.undeployAll(completeTask);
        // the complete task should have been executed
        assertTrue(completeTask.get());
    }

    @Test
    public void undeployService() throws Exception {
        final ServiceName serviceName = ServiceName.of("service");
        final TestServiceListener testListener = new TestServiceListener();

        Future<ServiceController<?>> serviceStart = testListener.expectServiceStart(serviceName);
        // install service
        serviceContainer.addService(serviceName, Service.NULL).addListener(testListener).install();
        ServiceController<?> serviceController = assertController(serviceName, serviceStart);

        // use ServiceUtils to undeploy the service
        final TestTask completeTask = new TestTask();
        Future<ServiceController<?>> serviceRemoval = testListener.expectServiceRemoval(serviceName);
        ServiceUtils.undeployAll(completeTask, serviceController);
        assertController(serviceController, serviceRemoval);
        // complete task should have been executed
        assertTrue(completeTask.get());
    }

    @Test
    public void undeployServices() throws Exception {
        final ServiceName serviceName1 = ServiceName.of("service", "1");
        final ServiceName serviceName2 = ServiceName.of("service", "2");
        final ServiceName serviceName3 = ServiceName.of("service", "3");
        final TestServiceListener testListener = new TestServiceListener();

        // install service1
        Future<ServiceController<?>> service1Start = testListener.expectServiceStart(serviceName1);
        serviceContainer.addService(serviceName1, Service.NULL).addListener(testListener).install();
        ServiceController<?> serviceController1 = assertController(serviceName1, service1Start);

        // install service2
        Future<ServiceController<?>> service2Start = testListener.expectServiceStart(serviceName2);
        serviceContainer.addService(serviceName2, Service.NULL).addListener(testListener).install();
        ServiceController<?> serviceController2 = assertController(serviceName2, service2Start);

        // install service3
        Future<ServiceController<?>> service3Start = testListener.expectServiceStart(serviceName3);
        serviceContainer.addService(serviceName3, Service.NULL).addListener(testListener).install();
        ServiceController<?> serviceController3 = assertController(serviceName3, service3Start);

        // undeploy service1 and service2 by calling ServiceUtils.undeployAll
        final TestTask completeTask = new TestTask();
        Future<ServiceController<?>> service1Removal = testListener.expectServiceRemoval(serviceName1);
        Future<ServiceController<?>> service2Removal = testListener.expectServiceRemoval(serviceName2);
        ServiceUtils.undeployAll(completeTask, serviceController1, serviceController2);
        assertController(serviceController1, service1Removal);
        assertController(serviceController2, service2Removal);
        // complete task should have been executed
        assertTrue(completeTask.get());
        // service 3 should continue installed
        assertSame(State.UP, serviceController3.getState());
    }

    @Test
    public void undeployNull() throws Exception {
        final ServiceName serviceName = ServiceName.of("service");
        final TestServiceListener testListener = new TestServiceListener();

        // install service
        Future<ServiceController<?>> serviceStart = testListener.expectServiceStart(serviceName);
        serviceContainer.addService(serviceName, Service.NULL).addListener(testListener).install();
        ServiceController<?> serviceController = assertController(serviceName, serviceStart);

        // try to undeploy a null service... NPE is expected
        final TestTask completeTask = new TestTask();
        ServiceUtils.undeployAll(completeTask, (ServiceController<?>) null);

        // now remove the service with a null complete task
        Future<ServiceController<?>> serviceRemoval = testListener.expectServiceRemoval(serviceName);
        ServiceUtils.undeployAll(null, serviceController);// should ignore the null task
        assertController(serviceController, serviceRemoval);

        // reinstall service
        serviceStart = testListener.expectServiceStart(serviceName);
        serviceContainer.addService(serviceName, Service.NULL).addListener(testListener).install();
        serviceController = assertController(serviceName, serviceStart);

        // try to remove null service with null complete task... no NPE is expected
        ServiceUtils.undeployAll(null, (ServiceController<?>) null);

        // try to remove a null list of services with non-null complete task... no NPE is expected
        ServiceUtils.undeployAll(completeTask, (List<ServiceController<?>>) null);

        // remove service again
        serviceRemoval = testListener.expectServiceRemoval(serviceName);
        List<ServiceController<?>> serviceControllers = new ArrayList<ServiceController<?>>();
        serviceControllers.add(serviceController);
        ServiceUtils.undeployAll(null, serviceControllers);
        assertController(serviceController, serviceRemoval);

        // reinstall service
        serviceStart = testListener.expectServiceStart(serviceName);
        serviceContainer.addService(serviceName, Service.NULL).addListener(testListener).install();
        serviceController = assertController(serviceName, serviceStart);

        // remove null list of services with a null complete task... NPE is expected
        ServiceUtils.undeployAll(null, (List<ServiceController<?>>) null);
    }
}