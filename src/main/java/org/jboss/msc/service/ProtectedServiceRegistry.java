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

import java.util.List;

/**
 * An "insulated" view of a service registry which prevents access to other public methods on the delegate registry object.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class ProtectedServiceRegistry implements ServiceRegistry {
    private final ServiceRegistry delegate;

    private ProtectedServiceRegistry(final ServiceRegistry delegate) {
        this.delegate = delegate;
    }

    /**
     * Construct a new instance
     *
     * @param delegate the delegate service registry
     * @return a protected service registry
     */
    public static ProtectedServiceRegistry create(ServiceRegistry delegate) {
        return delegate instanceof ProtectedServiceRegistry ? (ProtectedServiceRegistry) delegate : new ProtectedServiceRegistry(delegate);
    }

    /** {@inheritDoc} */
    public ServiceController<?> getRequiredService(final ServiceName serviceName) throws ServiceNotFoundException {
        return delegate.getRequiredService(serviceName);
    }

    /** {@inheritDoc} */
    public ServiceController<?> getService(final ServiceName serviceName) {
        return delegate.getService(serviceName);
    }

    /** {@inheritDoc} */
    public List<ServiceName> getServiceNames() {
        return delegate.getServiceNames();
    }
}
