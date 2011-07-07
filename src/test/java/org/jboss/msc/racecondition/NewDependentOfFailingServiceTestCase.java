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

import static org.junit.Assert.assertEquals;

import java.util.concurrent.Future;

import org.jboss.byteman.contrib.bmunit.BMScript;
import org.jboss.byteman.contrib.bmunit.BMUnitRunner;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.util.TestServiceListener;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Creates a scenario where a new dependent is added to a service at the exact moment this service is failing to start.
 * The {@link org.jboss.msc.service.ServiceRegistrationImpl#addDependent(org.jboss.msc.service.Dependent)} method is
 * called at the point where none of the dependents have been notified that dependency has failed, but failCount is
 * already incremented to 1.<p>
 * This test ensures the new dependent will receive exactly a single {@link
 * org.jboss.msc.service.Dependent#dependencyFailed()} notification, making sure its failCount value is kept consistent.
 * 
 * @author <a href="mailto:flavia.rainone@jboss.com">Flavia Rainone</a>
 */
@RunWith(BMUnitRunner.class)
@BMScript(dir="src/test/resources")
public class NewDependentOfFailingServiceTestCase extends AbstractRaceConditionTest {

    private static final TestServiceListener testListener = new TestServiceListener();
    private static final ServiceName serviceAName = ServiceName.of("A");
    private static final ServiceName serviceBName = ServiceName.of("B");
    public int serviceAFailCount = 0;
    public int serviceADepFailedInvocations = 0;
    public ServiceController<?> serviceAController = null;

    @Test
    public void test() throws Exception {
        final Future<ServiceController<?>> serviceAInstall = testListener.expectListenerAdded(serviceAName);
        final Future<ServiceController<?>> serviceADepFailed = testListener.expectDependencyFailure(serviceAName);
        final Future<ServiceController<?>> serviceBInstall = testListener.expectListenerAdded(serviceBName);
        final Future<StartException> serviceBStartFailed = testListener.expectServiceFailure(serviceBName);
        serviceContainer.addService(serviceBName, new FailToStartService()).addListener(testListener).install();
        serviceContainer.addService(serviceAName, Service.NULL).addDependency(serviceBName).addListener(testListener).install();
        serviceAController = assertController(serviceAName, serviceAInstall);
        final ServiceController<?> serviceBController = assertController(serviceBName, serviceBInstall);
        assertFailure(serviceBController, serviceBStartFailed);
        assertController(serviceAController, serviceADepFailed);
        assertEquals(1, serviceAFailCount);
        assertEquals(1, serviceADepFailedInvocations);
    }

    public static class FailToStartService implements Service<Void> {

        @Override
        public void start(StartContext context) throws StartException {
            throw new NullPointerException();
        }

        @Override
        public void stop(StopContext context) {}

        @Override
        public Void getValue() throws IllegalStateException {
            return null;
        }
    }
}
