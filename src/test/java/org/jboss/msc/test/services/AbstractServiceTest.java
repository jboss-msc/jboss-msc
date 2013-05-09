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

package org.jboss.msc.test.services;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.logging.Logger;

import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceContainer;
import org.jboss.msc.service.ServiceContainerFactory;
import org.jboss.msc.service.ServiceMode;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceRegistry;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.test.utils.AbstractTransactionTest;
import org.jboss.msc.test.utils.CompletionListener;
import org.jboss.msc.txn.Transaction;
import org.junit.After;
import org.junit.Before;

/**
 * Test base used for service test cases.
 *
 * @author <a href="mailto:flavia.rainone@jboss.com">Flavia Rainone</a>
 */
public class AbstractServiceTest extends AbstractTransactionTest {

    protected volatile ServiceContainer serviceContainer;
    protected volatile ServiceRegistry serviceRegistry;
    private boolean shutdownOnTearDown;

    @Override
    @Before
    public void setUp() {
        super.setUp();
        Logger.getLogger("").fine("Setting up test " + getClass());
        serviceContainer = ServiceContainerFactory.getInstance().newServiceContainer();
        serviceRegistry = serviceContainer.newRegistry();
        shutdownOnTearDown = true;
        //transaction = newTransaction();
    }

    @Override
    @After
    public void tearDown() {
        Logger.getLogger("").fine("Tearing down test " + getClass());
        final Transaction transaction = newTransaction();
        if (shutdownOnTearDown) {
            // TODO how to shutdown serviceContainer? serviceContainer.shutdown
            final CompletionListener listener = new CompletionListener();
            transaction.commit(listener);
            try {
                listener.awaitCompletion();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        serviceContainer = null;
        super.tearDown();
    }

    /**
     * Shutdowns the container.
     */
    public void shutdownContainer() {
        final Transaction transaction = newTransaction();
        // TODO how to shutdown container? serviceContainer.stop(transaction);
        final CompletionListener listener = new CompletionListener();
        transaction.commit(listener);
        try {
            listener.awaitCompletion();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        shutdownOnTearDown = false;
    }

    protected TestService installService(ServiceName serviceName, boolean failToStart, ServiceMode serviceMode, ServiceName... dependencies) throws InterruptedException {
        final Transaction transaction = newTransaction();
        final TestService service = new TestService(failToStart);
        final ServiceBuilder<Void> serviceBuilder = transaction.newServiceTarget().addService(serviceRegistry, serviceName, service);
        serviceBuilder.setMode(serviceMode);
        for (ServiceName dependency: dependencies) {
            serviceBuilder.addDependency(dependency);
        }
        serviceBuilder.install();
        final CompletionListener completionListener = new CompletionListener();
        transaction.commit(completionListener);
        completionListener.awaitCompletion();
        assertSame(service, serviceRegistry.getRequiredService(serviceName));
        return service;
    }

    protected TestService installService(ServiceName serviceName, ServiceMode serviceMode, ServiceName... dependencies) throws InterruptedException {
        return installService(serviceName, false, serviceMode, dependencies);
    }

    protected TestService installService(ServiceName serviceName, boolean failToStart, ServiceName... dependencies) throws InterruptedException {
        final Transaction transaction = newTransaction();
        final TestService service = new TestService(failToStart);
        final ServiceBuilder<Void> serviceBuilder = transaction.newServiceTarget().addService(serviceRegistry, serviceName, service);
        for (ServiceName dependency: dependencies) {
            serviceBuilder.addDependency(dependency);
        }
        serviceBuilder.install();
        final CompletionListener completionListener = new CompletionListener();
        transaction.commit(completionListener);
        completionListener.awaitCompletion();
        assertSame(service, serviceRegistry.getRequiredService(serviceName));
        return service;
    }

    protected TestService installService(ServiceName serviceName, ServiceName... dependencies) throws InterruptedException {
        return installService(serviceName, false, dependencies);
    }

    protected void remove(ServiceName serviceName) throws InterruptedException {
        assertNotNull(serviceRegistry.getService(serviceName));
        final Transaction transaction = newTransaction();
        final CompletionListener listener = new CompletionListener();
        final ServiceTarget serviceTarget = transaction.newServiceTarget();
        assertNotNull(serviceTarget);
        serviceTarget.removeService(serviceRegistry, serviceName);
        transaction.commit(listener);
        listener.awaitCompletion();
        assertTrue(transaction.getProblemReport().getProblems().isEmpty());
        assertNull(serviceRegistry.getService(serviceName));
    }
}
