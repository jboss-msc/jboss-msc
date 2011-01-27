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

import static org.junit.Assert.*;

import java.util.Set;

import org.junit.Test;

/**
 * Test for {@link TrackingServiceTarget}.
 * 
 * @author <a href="mailto:flavia.rainone@jboss.com">Flavia Rainone</a>
 *
 */
public class TrackingServiceTargetTestCase extends AbstractDelegatingServiceTargetTest {

    private TrackingServiceTarget trackingServiceTarget; 

    @Override
    protected ServiceTarget getDelegatingServiceTarget(ServiceTarget serviceTarget) {
        trackingServiceTarget = new TrackingServiceTarget(serviceTarget);
        return trackingServiceTarget;
    }

    @Test
    public void addService() throws Exception {
        super.addService();
        final Set<ServiceName> services = trackingServiceTarget.getSet();
        assertNotNull(services);
        assertEquals(1, services.size());
        assertTrue(services.contains(ServiceName.of("service", "name")));
    }

    @Test
    public void addServiceValue() throws Exception {
        super.addServiceValue();
        final Set<ServiceName> services = trackingServiceTarget.getSet();
        assertNotNull(services);
        assertEquals(1, services.size());
        assertTrue(services.contains(ServiceName.of("service", "name")));
    }

     
    @Test
    public void addServiceWithDependency() throws Exception {
        super.addServiceWithDependency();
        final Set<ServiceName> services = trackingServiceTarget.getSet();
        assertNotNull(services);
        assertEquals(2, services.size());
        assertTrue(services.contains(ServiceName.of("service", "name")));
        assertTrue(services.contains(ServiceName.of("service", "another", "name")));
    }

    @Test
    public void addServiceWithDependencies() throws Exception {
        super.addServiceWithDependencies();
        final Set<ServiceName> services = trackingServiceTarget.getSet();
        assertNotNull(services);
        assertEquals(2, services.size());
        assertTrue(services.contains(ServiceName.of("service", "name")));
        assertTrue(services.contains(ServiceName.of("service", "extra", "name")));
    }

    @Test
    public void addServiceWithDependencyCollection() throws Exception {
        super.addServiceWithDependencyCollection();
        final Set<ServiceName> services = trackingServiceTarget.getSet();
        assertNotNull(services);
        assertEquals(3, services.size());
        assertTrue(services.contains(ServiceName.of("service", "name")));
        assertTrue(services.contains(ServiceName.of("service", "extra", "name")));
        assertTrue(services.contains(ServiceName.of("service", "another", "name")));
    }
}
