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

package org.jboss.msc.service.management;

import java.util.List;

/**
 * The service container management bean interface.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public interface ServiceContainerMXBean {

    /**
     * Get the status of one service.
     *
     * @param name the service name
     * @return the status
     */
    ServiceStatus getServiceStatus(String name);

    /**
     * Get a list of service names in this container.
     *
     * @return the list of names
     */
    List<String> queryServiceNames();

    /**
     * Get a list of service statuses in this container.
     *
     * @return the list of statuses
     */
    List<ServiceStatus> queryServiceStatuses();

    /**
     * Change the mode of a service.
     *
     * @param name the service name
     * @param mode the new mode
     */
    void setServiceMode(String name, String mode);

    /**
     * Dump the container state to the console.
     */
    void dumpServices();

    /**
     * Dump the container state to a big string.  The string has no particular standard format and may
     * change over time; this method is simply a convenience.
     *
     * @return the container state, as a string
     */
    String dumpServicesToString();

    /**
     * Dump all details of a service.
     *
     * @param serviceName the name of the service to examine
     * @return the details, as a string
     */
    String dumpServiceDetails(String serviceName);
}
