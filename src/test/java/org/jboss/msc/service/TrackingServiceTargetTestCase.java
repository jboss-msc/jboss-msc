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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

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
        final List<ServiceName> serviceList = trackingServiceTarget.getList();
        assertNotNull(serviceList);
        assertEquals(1, serviceList.size());
        assertEquals(ServiceName.of("service", "name"), serviceList.get(0));
    }

    @Test
    public void addServiceValue() throws Exception {
        super.addServiceValue();
        final List<ServiceName> serviceList = trackingServiceTarget.getList();
        assertNotNull(serviceList);
        assertEquals(1, serviceList.size());
        assertEquals(ServiceName.of("service", "name"), serviceList.get(0));
    }

    @Test
    public void addServiceWithDependencyToBatchBuilder() throws Exception {
        super.addServiceWithDependencyToBatchBuilder();
        final List<ServiceName> serviceList = trackingServiceTarget.getList();
        assertNotNull(serviceList);
        assertEquals(2, serviceList.size());
        assertTrue((ServiceName.of("service", "name").equals(serviceList.get(0)) && ServiceName.of("service", "another", "name").equals(serviceList.get(1)))
                || (ServiceName.of("service", "name").equals(serviceList.get(1)) && ServiceName.of("service", "another", "name").equals(serviceList.get(0))));
    }

    @Test
    public void addServiceWithDependenciesToBatchBuilder() throws Exception {
        super.addServiceWithDependenciesToBatchBuilder();
        final List<ServiceName> serviceList = trackingServiceTarget.getList();
        assertNotNull(serviceList);
        assertEquals(2, serviceList.size());
        assertTrue((ServiceName.of("service", "name").equals(serviceList.get(0)) && ServiceName.of("service", "extra", "name").equals(serviceList.get(1)))
                || (ServiceName.of("service", "name").equals(serviceList.get(1)) && ServiceName.of("service", "extra", "name").equals(serviceList.get(0))));
    }

    @Test
    public void addServiceWithDependencyCollection() throws Exception {
        super.addServiceWithDependencyCollection();
        final List<ServiceName> serviceList = trackingServiceTarget.getList();
        assertNotNull(serviceList);
        assertEquals(3, serviceList.size());
        assertTrue(serviceList.contains(ServiceName.of("service", "name")));
        assertTrue(serviceList.contains(ServiceName.of("service", "extra", "name")));
        assertTrue(serviceList.contains(ServiceName.of("service", "another", "name")));
    }
}
