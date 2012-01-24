/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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

import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceController.Mode;
import org.jboss.msc.util.TestServiceListener;
import org.junit.Test;

/**
 * Test if services in a dependency chain behave consistently after one of the services receives an
 * transitiveDependencyUnavailable notification during its removal.
 * 
 * @author <a href="mailto:flavia.rainone@jboss.com">Flavia Rainone</a>
 */
public abstract class TransitiveDependencyUnavailableDuringServiceRemovalTest extends AbstractRaceConditionTest {

    @Test
    public void test() throws Exception {
        final TestServiceListener testListener = new TestServiceListener();
        serviceContainer.addListener(testListener);
        final ServiceName serviceNameA = ServiceName.of("A");
        final ServiceName serviceNameB = ServiceName.of("B");
        final ServiceName serviceNameC = ServiceName.of("C");
        final ServiceName serviceNameD = ServiceName.of("D");

        Future<ServiceController<?>> serviceAStart = testListener.expectServiceStart(serviceNameA);
        Future<ServiceController<?>> serviceBStart = testListener.expectServiceStart(serviceNameB);
        Future<ServiceController<?>> serviceCStart = testListener.expectServiceStart(serviceNameC);
        Future<ServiceController<?>> serviceDStart = testListener.expectServiceStart(serviceNameD);
        serviceContainer.addService(serviceNameA, Service.NULL).addDependency(serviceNameB).install();
        serviceContainer.addService(serviceNameB, Service.NULL).addDependency(serviceNameC).install();
        serviceContainer.addService(serviceNameC, Service.NULL).addDependency(serviceNameD).install();
        serviceContainer.addService(serviceNameD, Service.NULL).install();
        final ServiceController<?> serviceAController = assertController(serviceNameA, serviceAStart);
        final ServiceController<?> serviceBController = assertController(serviceNameB, serviceBStart);
        final ServiceController<?> serviceCController = assertController(serviceNameC, serviceCStart);
        final ServiceController<?> serviceDController = assertController(serviceNameD, serviceDStart);

        final Future<ServiceController<?>> serviceBRemoval = testListener.expectServiceRemoval(serviceNameB);
        final Future<ServiceController<?>> serviceDRemoval = testListener.expectServiceRemoval(serviceNameD);
        final Future<ServiceController<?>> serviceAStop = testListener.expectServiceStop(serviceNameA);
        final Future<ServiceController<?>> serviceCStop = testListener.expectServiceStop(serviceNameC);
        serviceDController.setMode(Mode.REMOVE);
        serviceBController.setMode(Mode.REMOVE);
        assertController(serviceBController, serviceBRemoval);
        assertController(serviceDController, serviceDRemoval);
        assertController(serviceAController, serviceAStop);
        assertController(serviceCController, serviceCStop);

        // make sure the service controller internal counts are consistent, by adding a new service C and making sure A and B start
        serviceAStart = testListener.expectServiceStart(serviceNameA);
        serviceBStart = testListener.expectServiceStart(serviceNameB);
        serviceCStart = testListener.expectServiceStart(serviceNameC);
        serviceDStart = testListener.expectServiceStart(serviceNameD);
        serviceContainer.addService(serviceNameB, Service.NULL).addDependency(serviceNameC).install();
        serviceContainer.addService(serviceNameD, Service.NULL).install();
        assertController(serviceNameA, serviceAStart);
        assertController(serviceNameB, serviceBStart);
        assertController(serviceNameC, serviceCStart);
        assertController(serviceNameD, serviceDStart);
    }
}
