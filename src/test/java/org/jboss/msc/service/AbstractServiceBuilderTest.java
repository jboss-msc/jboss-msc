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

package org.jboss.msc.service;

import static org.junit.Assert.fail;

import java.util.concurrent.Future;

import org.jboss.msc.util.TestServiceListener;
import org.junit.Test;

/**
 * Test for {@link ServiceBuilder} implementations.
 * 
 * @author <a href="mailto:flavia.rainone@jboss.com">Flavia Rainone</a>
 */
public abstract class AbstractServiceBuilderTest extends AbstractServiceTest {

    private final TestServiceListener testListener = new TestServiceListener();
    private final ServiceName serviceName = ServiceName.of("service");

    /**
     * Returns the ServiceBuilder that should be tested.
     * 
     * @param serviceBuilder a serviceBuilder 
     * @return  a servicer builder that delegates to {@code serviceBuilder}, providing ServiceBuilders that
     *          will be installed into the same target as {@code serviceBuilder}
     */
    protected abstract <T> ServiceBuilder<T> getServiceBuilder(ServiceBuilder<T> serviceBuilder);

    @Test
    public void editInstalledBuilder() throws Exception {
        Future<ServiceController<?>> serviceStart = testListener.expectServiceStart(serviceName);
        // add service
        ServiceBuilder<?> serviceBuilder = getServiceBuilder(serviceContainer.addService(serviceName, Service.NULL))
            .addListener(testListener);
        // install it
        ServiceController<?> serviceController = assertController(serviceName, serviceBuilder.install());
        assertController(serviceController, serviceStart);
        try {
            // edition of a already installed builder should fail
            serviceBuilder.addListener(testListener);
            fail("IllegalStateException expected");
        } catch (IllegalStateException e) {}
        // try to install again, IllegalStateException expected
        try {
            serviceBuilder.install();
            fail ("IllegalStateException expected");
        } catch (IllegalStateException e) {}
    }

}
