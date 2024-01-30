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

import static org.junit.Assert.fail;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.junit.Test;

import java.util.function.Consumer;

/**
 * Test for {@link ServiceBuilder} implementations.
 * 
 * @author <a href="mailto:flavia.rainone@jboss.com">Flavia Rainone</a>
 */
public abstract class AbstractServiceBuilderTest extends AbstractServiceTest {

    private final TestLifecycleListener testListener = new TestLifecycleListener();
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
        // add service
        ServiceBuilder<?> sb = serviceContainer.addService();
        Consumer<String> providedValue = sb.provides(serviceName);
        sb.setInstance(org.jboss.msc.Service.newInstance(providedValue, serviceName.toString()));
        sb.addListener(testListener);
        ServiceController<?> serviceController = sb.install();
        assertNotNull(serviceController);
        serviceContainer.awaitStability();
        assertEquals(testListener.upValues().size(), 1);
        assertTrue(testListener.upValues().contains(serviceName));
        try {
            // edition of an already installed builder should fail
            sb.addListener(testListener);
            fail("IllegalStateException expected");
        } catch (IllegalStateException e) {}
        // try to install again, IllegalStateException expected
        try {
            sb.install();
            fail ("IllegalStateException expected");
        } catch (IllegalStateException e) {}
    }

}
