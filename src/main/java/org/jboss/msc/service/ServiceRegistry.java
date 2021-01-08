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

package org.jboss.msc.service;

import java.util.List;

/**
 * A service registry.  Registries can return services by name, or get a collection of service names.
 *
 * @author <a href="mailto:flavia.rainone@jboss.com">Flavia Rainone</a>
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public interface ServiceRegistry {

    /**
     * Get a service, throwing an exception if it is not found.
     *
     * @param serviceName the service name
     * @return the service controller for the corresponding service
     * @throws ServiceNotFoundException if the service is not present in the registry
     */
    ServiceController<?> getRequiredService(ServiceName serviceName) throws ServiceNotFoundException;

    /**
     * Get a service, returning {@code null} if it is not found.
     *
     * @param serviceName the service name
     * @return the service controller for the corresponding service, or {@code null} if it is not found
     */
    ServiceController<?> getService(ServiceName serviceName);

    /**
     * Get a list of service names installed in this registry.
     *
     * @return the list
     */
    List<ServiceName> getServiceNames();
}
