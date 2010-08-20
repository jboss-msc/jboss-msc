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

import org.jboss.msc.util.TestServiceListener;
import org.junit.Test;

import java.util.concurrent.Future;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * Test case used to verify service alias functionality.
 *
 * @author John Bailey
 */
public class ServiceAliasTestCase extends AbstractServiceTest {

    @Test
    public void testAliases() throws Exception {
        final BatchBuilder builder = serviceContainer.batchBuilder();
        final TestServiceListener listener = new TestServiceListener();
        builder.addListener(listener);

        builder.addService(ServiceName.of("service1"), Service.NULL)
            .addAliases(ServiceName.of("alias1"))
            .addAliases(ServiceName.of("alias2"));

        final Future<ServiceController<?>> future = listener.expectServiceStart(ServiceName.of("service1"));

        builder.install();

        ServiceController<?> controller = future.get();

        assertEquals(serviceContainer.getService(ServiceName.of("service1")), serviceContainer.getService(ServiceName.of("alias1")));
        assertEquals(serviceContainer.getService(ServiceName.of("service1")), serviceContainer.getService(ServiceName.of("alias2")));

        final Future<ServiceController<?>> removeFuture = listener.expectServiceRemoval(ServiceName.of("service1"));

        controller.setMode(ServiceController.Mode.REMOVE);

        assertNotNull(removeFuture.get());

        assertNull(serviceContainer.getService(ServiceName.of("service1")));
        assertNull(serviceContainer.getService(ServiceName.of("alias1")));
        assertNull(serviceContainer.getService(ServiceName.of("alias2")));
    }
}
