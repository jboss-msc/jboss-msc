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

package org.jboss.msc.test.utils;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;

import java.util.List;

import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceContainer;
import org.jboss.msc.service.ServiceContainerFactory;
import org.jboss.msc.service.ServiceMode;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceRegistry;
import org.jboss.msc.txn.Problem;
import org.jboss.msc.txn.Problem.Severity;
import org.jboss.msc.txn.Transaction;
import org.junit.After;
import org.junit.Before;

/**
 * Test base used for service test cases.
 *
 * @author <a href="mailto:flavia.rainone@jboss.com">Flavia Rainone</a>
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
public class AbstractServiceTest extends AbstractTransactionTest {

    protected volatile ServiceContainer serviceContainer;
    protected volatile ServiceRegistry serviceRegistry;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        serviceContainer = ServiceContainerFactory.getInstance().newServiceContainer();
        serviceRegistry = serviceContainer.newRegistry();
    }

    @Override
    @After
    public void tearDown() throws Exception {
        shutdownContainer();
        super.tearDown();
    }

    protected final void removeRegistry() throws Exception {
        removeRegistry(serviceRegistry);
    }

    protected final void removeRegistry(final ServiceRegistry serviceRegistry) throws Exception {
        final Transaction txn = newTransaction();
        txn.newTask(new RemoveRegistryTask(serviceRegistry)).release();
        commit(txn);
        assertNoCriticalProblems(txn);
    }

    protected final void enableRegistry() throws Exception {
        enableRegistry(serviceRegistry);
    }

    protected final void enableRegistry(final ServiceRegistry serviceRegistry) throws Exception {
        final Transaction txn = newTransaction();
        txn.newTask(new EnableRegistryTask(serviceRegistry)).release();
        commit(txn);
        assertNoCriticalProblems(txn);
    }

    protected final void disableRegistry() throws Exception {
        disableRegistry(serviceRegistry);
    }

    protected final void disableRegistry(final ServiceRegistry serviceRegistry) throws Exception {
        final Transaction txn = newTransaction();
        txn.newTask(new DisableRegistryTask(serviceRegistry)).release();
        commit(txn);
        assertNoCriticalProblems(txn);
    }

    protected final void shutdownContainer() throws Exception {
        shutdownContainer(serviceContainer);
    }

    protected final void shutdownContainer(final ServiceContainer serviceContainer) throws Exception {
        final Transaction txn = newTransaction();
        txn.newTask(new ShutdownContainerTask(serviceContainer)).release();
        commit(txn);
        assertNoCriticalProblems(txn);
    }

    protected final TestService addService(final ServiceRegistry serviceRegistry, final ServiceName serviceName, final boolean failToStart, final ServiceMode serviceMode, final ServiceName... dependencies) throws InterruptedException {
        final Transaction txn = newTransaction();
        final TestService service = new TestService(failToStart);
        final ServiceBuilder<Void> serviceBuilder = txn.addService(serviceRegistry, serviceName, service);
        if (serviceMode != null) serviceBuilder.setMode(serviceMode);
        for (ServiceName dependency: dependencies) {
            serviceBuilder.addDependency(dependency);
        }
        serviceBuilder.install();
        commit(txn);
        assertSame(service, serviceRegistry.getRequiredService(serviceName));
        return service;
    }

    protected final TestService addService(final ServiceRegistry serviceRegistry, final ServiceName serviceName, final ServiceMode serviceMode, final ServiceName... dependencies) throws InterruptedException {
        return addService(serviceRegistry, serviceName, false, serviceMode, dependencies);
    }

    protected final TestService addService(final ServiceRegistry serviceRegistry, final ServiceName serviceName, final ServiceName... dependencies) throws InterruptedException {
        return addService(serviceRegistry, serviceName, false, null, dependencies);
    }

    protected final TestService addService(final ServiceRegistry serviceRegistry, final ServiceName serviceName, final boolean failToStart, final ServiceName... dependencies) throws InterruptedException {
        return addService(serviceRegistry, serviceName, failToStart, null, dependencies);
    }

    protected final TestService addService(final ServiceName serviceName, final boolean failToStart, final ServiceMode serviceMode, final ServiceName... dependencies) throws InterruptedException {
        return addService(serviceRegistry, serviceName, failToStart, serviceMode, dependencies);
    }

    protected final TestService addService(final ServiceName serviceName, final ServiceMode serviceMode, final ServiceName... dependencies) throws InterruptedException {
        return addService(serviceRegistry, serviceName, false, serviceMode, dependencies);
    }

    protected final TestService addService(final ServiceName serviceName, final ServiceName... dependencies) throws InterruptedException {
        return addService(serviceRegistry, serviceName, false, null, dependencies);
    }

    protected final TestService addService(final ServiceName serviceName, final boolean failToStart, final ServiceName... dependencies) throws InterruptedException {
        return addService(serviceRegistry, serviceName, failToStart, null, dependencies);
    }

    protected final void removeService(final ServiceRegistry serviceRegistry, final ServiceName serviceName) throws InterruptedException {
        assertNotNull(serviceRegistry.getService(serviceName));
        final Transaction txn = newTransaction();
        txn.newTask(new RemoveServiceTask(serviceRegistry, serviceName)).release();
        commit(txn);
        assertNoCriticalProblems(txn);
        assertNull(serviceRegistry.getService(serviceName));
    }

    protected final void removeService(final ServiceName serviceName) throws InterruptedException {
        removeService(serviceRegistry, serviceName);
    }
    
    protected final void enableService(final ServiceRegistry serviceRegistry, final ServiceName serviceName) throws InterruptedException {
        assertNotNull(serviceRegistry.getService(serviceName));
        final Transaction txn = newTransaction();
        txn.newTask(new EnableServiceTask(serviceRegistry, serviceName)).release();
        commit(txn);
        assertNoCriticalProblems(txn);
        assertNull(serviceRegistry.getService(serviceName));
    }

    protected final void enableService(final ServiceName serviceName) throws InterruptedException {
        enableService(serviceRegistry, serviceName);
    }
    
    protected final void disableService(final ServiceRegistry serviceRegistry, final ServiceName serviceName) throws InterruptedException {
        assertNotNull(serviceRegistry.getService(serviceName));
        final Transaction txn = newTransaction();
        txn.newTask(new DisableServiceTask(serviceRegistry, serviceName)).release();
        commit(txn);
        assertNoCriticalProblems(txn);
        assertNull(serviceRegistry.getService(serviceName));
    }

    protected final void disableService(final ServiceName serviceName) throws InterruptedException {
        enableService(serviceRegistry, serviceName);
    }
    
    protected final TestService getService(final ServiceName serviceName) throws InterruptedException {
        return getService(serviceRegistry, serviceName);
    }
    
    protected final TestService getService(final ServiceRegistry serviceRegistry, final ServiceName serviceName) throws InterruptedException {
        return (TestService) serviceRegistry.getService(serviceName); 
    }
    
    private void assertNoCriticalProblems(final Transaction txn) {
        List<Problem> problems = txn.getProblemReport().getProblems();
        for (final Problem problem : problems) {
            if (problem.getSeverity() == Severity.CRITICAL) {
                fail("Critical problem detected");
            }
        }
    }

}
