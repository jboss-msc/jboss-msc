/*
 * JBoss, Home of Professional Open Source
 *
 * Copyright 2013 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jboss.msc.test.utils;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;

import java.util.List;

import org.jboss.msc.service.DependencyFlag;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceContainer;
import org.jboss.msc.service.ServiceContainerFactory;
import org.jboss.msc.service.ServiceMode;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceRegistry;
import org.jboss.msc.txn.Problem;
import org.jboss.msc.txn.Problem.Severity;
import org.jboss.msc.txn.ServiceContext;
import org.jboss.msc.txn.Transaction;
import org.jboss.msc.txn.TransactionRolledBackException;
import org.junit.After;
import org.junit.Before;

/**
 * Test base used for service test cases.
 *
 * @author <a href="mailto:flavia.rainone@jboss.com">Flavia Rainone</a>
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
public class AbstractServiceTest extends AbstractTransactionTest {

    protected static final DependencyFlag[] requiredFlag = new DependencyFlag[] {DependencyFlag.REQUIRED};
    protected static final DependencyFlag[] unrequiredFlag = new DependencyFlag[] {DependencyFlag.UNREQUIRED};

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
        txn.newTask(new RemoveRegistryTask(serviceRegistry, txn)).release();
        commit(txn);
        assertNoCriticalProblems(txn);
    }

    protected final void enableRegistry() throws Exception {
        enableRegistry(serviceRegistry);
    }

    protected final void enableRegistry(final ServiceRegistry serviceRegistry) throws Exception {
        final Transaction txn = newTransaction();
        txn.newTask(new EnableRegistryTask(serviceRegistry, txn)).release();
        commit(txn);
        assertNoCriticalProblems(txn);
    }

    protected final void disableRegistry() throws Exception {
        disableRegistry(serviceRegistry);
    }

    protected final void disableRegistry(final ServiceRegistry serviceRegistry) throws Exception {
        final Transaction txn = newTransaction();
        txn.newTask(new DisableRegistryTask(serviceRegistry, txn)).release();
        commit(txn);
        assertNoCriticalProblems(txn);
    }

    protected final void shutdownContainer() throws Exception {
        shutdownContainer(serviceContainer);
    }

    protected final void shutdownContainer(final ServiceContainer serviceContainer) throws Exception {
        final Transaction txn = newTransaction();
        txn.newTask(new ShutdownContainerTask(serviceContainer, txn)).release();
        commit(txn);
        assertNoCriticalProblems(txn);
    }

    protected final TestService addService(final ServiceRegistry serviceRegistry, final ServiceName serviceName, final boolean failToStart, final ServiceMode serviceMode, final ServiceName... dependencies) throws InterruptedException {
        final Transaction txn = newTransaction();
        final TestService service = new TestService(failToStart);
        final ServiceContext serviceContext = transactionController.getServiceContext();
        final ServiceBuilder<Void> serviceBuilder = serviceContext.addService(serviceRegistry, serviceName, txn);
        service.setServiceContext(serviceBuilder.getServiceContext());
        if (serviceMode != null) serviceBuilder.setMode(serviceMode);
        serviceBuilder.setService(service);
        for (ServiceName dependency: dependencies) {
            serviceBuilder.addDependency(dependency);
        }
        serviceBuilder.install();
        if (attemptToCommit(txn)) {
            assertSame(service, serviceRegistry.getRequiredService(serviceName));
            return service;
        } else {
            assertNull(serviceRegistry.getService(serviceName));
            assertFalse(service.isUp());
            return null;
        }
    }

    private static boolean attemptToCommit(final Transaction txn) throws InterruptedException {
        try {
            commit(txn);
        } catch (TransactionRolledBackException e) {
            System.out.println(e);
            for (Problem problem: txn.getProblemReport().getProblems()) {
                System.out.print(problem.getSeverity() + ": " + problem.getMessage());
                if (problem.getLocation() != null) {
                    System.out.println(" at " + problem.getLocation());
                }
                if (problem.getCause() != null) {
                    problem.getCause().printStackTrace();
                } else {
                    System.out.println();
                }
            }
            return false;
        }
        return true;
    }

    protected final TestService addService(final ServiceRegistry serviceRegistry, final ServiceName serviceName, final boolean failToStart, final ServiceMode serviceMode, final DependencyFlag[] dependencyFlags, final ServiceName... dependencies) throws InterruptedException {
        final Transaction txn = newTransaction();
        final TestService service = new TestService(failToStart);
        final ServiceContext serviceContext = transactionController.getServiceContext();
        final ServiceBuilder<Void> serviceBuilder = serviceContext.addService(serviceRegistry, serviceName, txn);
        if (serviceMode != null) serviceBuilder.setMode(serviceMode);
        serviceBuilder.setService(service);
        for (ServiceName dependency: dependencies) {
            serviceBuilder.addDependency(dependency, dependencyFlags);
        }
        serviceBuilder.install();
        if (attemptToCommit(txn)) {
            assertSame(service, serviceRegistry.getRequiredService(serviceName));
            return service;
        } else {
            assertNull(serviceRegistry.getService(serviceName));
            assertFalse(service.isUp());
            return null;
        }
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

    protected final TestService addService(final ServiceName serviceName, final boolean failToStart, final ServiceMode serviceMode, final DependencyFlag[] dependencyFlags, final ServiceName... dependencies) throws InterruptedException {
        return addService(serviceRegistry, serviceName, failToStart, serviceMode, dependencyFlags, dependencies);
    }

    protected final TestService addService(final ServiceName serviceName, final ServiceMode serviceMode, final ServiceName... dependencies) throws InterruptedException {
        return addService(serviceRegistry, serviceName, false, serviceMode, dependencies);
    }

    protected final TestService addService(final ServiceName serviceName, final ServiceMode serviceMode, final DependencyFlag[] dependencyFlags, final ServiceName... dependencies) throws InterruptedException {
        return addService(serviceRegistry, serviceName, false, serviceMode, dependencyFlags, dependencies);
    }

    protected final TestService addService(final ServiceName serviceName, final ServiceName... dependencies) throws InterruptedException {
        return addService(serviceRegistry, serviceName, false, null, dependencies);
    }

    protected final TestService addService(final ServiceName serviceName, final boolean failToStart, final ServiceName... dependencies) throws InterruptedException {
        return addService(serviceRegistry, serviceName, failToStart, null, dependencies);
    }

    protected final boolean removeService(final ServiceRegistry serviceRegistry, final ServiceName serviceName) throws InterruptedException {
        assertNotNull(serviceRegistry.getService(serviceName));
        final Transaction txn = newTransaction();
        txn.newTask(new RemoveServiceTask(serviceRegistry, serviceName,txn)).release();
        if (attemptToCommit(txn)) {
            assertNoCriticalProblems(txn);
            assertNull(serviceRegistry.getService(serviceName));
            return true;
        } else {
            assertNotNull(serviceRegistry.getRequiredService(serviceName));
            return false;
        }
    }

    protected final boolean removeService(final ServiceName serviceName) throws InterruptedException {
        return removeService(serviceRegistry, serviceName);
    }

    protected final void enableService(final ServiceRegistry serviceRegistry, final ServiceName serviceName) throws InterruptedException {
        assertNotNull(serviceRegistry.getService(serviceName));
        final Transaction txn = newTransaction();
        txn.newTask(new EnableServiceTask(serviceRegistry, serviceName, txn)).release();
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
        txn.newTask(new DisableServiceTask(serviceRegistry, serviceName, txn)).release();
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
