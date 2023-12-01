/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2021, Red Hat, Inc., and individual contributors
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

package org.jboss.msc;

import static junit.framework.Assert.fail;

import org.jboss.msc.service.DelegatingServiceContainer;
import org.jboss.msc.service.ServiceContainer;
import org.jboss.msc.service.ServiceRegistry;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

/**
 * Runs all tests in {@link AbstractServiceRegistryTest} against {@link DelegatingServiceContainer}.
 * Plus, tests unsupported operations in {@code DelegatingServiceContainer}.
 * 
 * @author <a href="mailto:flavia.rainone@jboss.com">Flavia Rainone</a>
 */
public class DelegatingServiceContainerTestCase extends AbstractServiceRegistryTest {

    @Override
    protected ServiceRegistry getServiceRegistry(ServiceContainer container) {
        return new DelegatingServiceContainer(container, container);
    }

    @Test
    public void getNameThrowsUnsupportedOperation() throws Exception {
        ServiceContainer delegatingContainer = new DelegatingServiceContainer(serviceContainer, serviceContainer);
        try {
            delegatingContainer.getName();
            fail ("UnsupportedOperationException expected");
        } catch (UnsupportedOperationException e) {}
    }

    @Test
    public void shutdownThrowsUnsupportedOperation() throws Exception {
        ServiceContainer delegatingContainer = new DelegatingServiceContainer(serviceContainer, serviceContainer);
        try {
            delegatingContainer.shutdown();
            fail ("UnsupportedOperationException expected");
        } catch (UnsupportedOperationException e) {}
    }

    @Test
    public void isShutdownCompleteThrowsUnsupportedOperation() throws Exception {
        ServiceContainer delegatingContainer = new DelegatingServiceContainer(serviceContainer, serviceContainer);
        try {
            delegatingContainer.isShutdownComplete();
            fail ("UnsupportedOperationException expected");
        } catch (UnsupportedOperationException e) {}
    }

    @Test
    public void dumpServicesThrowsUnsupportedOperation() throws Exception {
        ServiceContainer delegatingContainer = new DelegatingServiceContainer(serviceContainer, serviceContainer);
        try {
            delegatingContainer.dumpServices();
            fail ("UnsupportedOperationException expected");
        } catch (UnsupportedOperationException e) {}

        try {
            delegatingContainer.dumpServices(System.err);
            fail ("UnsupportedOperationException expected");
        } catch (UnsupportedOperationException e) {}
    }

    @Test
    public void addTerminateListenerThrowsUnsupportedOperation() throws Exception {
        ServiceContainer delegatingContainer = new DelegatingServiceContainer(serviceContainer, serviceContainer);
        try {
            delegatingContainer.addTerminateListener(null);
            fail ("UnsupportedOperationException expected");
        } catch (UnsupportedOperationException e) {}
    }

    @Test
    public void awaitTerminationThrowsUnsupportedOperation() throws Exception {
        ServiceContainer delegatingContainer = new DelegatingServiceContainer(serviceContainer, serviceContainer);
        try {
            delegatingContainer.awaitTermination();
            fail ("UnsupportedOperationException expected");
        } catch (UnsupportedOperationException e) {}
        try {
            delegatingContainer.awaitTermination(300, TimeUnit.MICROSECONDS);
            fail ("UnsupportedOperationException expected");
        } catch (UnsupportedOperationException e) {}
    }

}
