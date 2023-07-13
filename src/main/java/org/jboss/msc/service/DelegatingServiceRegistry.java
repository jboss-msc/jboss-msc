/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2022, Red Hat, Inc., and individual contributors
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
 * A service registry which delegates to another service registry.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
public class DelegatingServiceRegistry implements ServiceRegistry {

    private final ServiceRegistry delegate;

    /**
     * Construct a new instance.
     *
     * @param delegate the registry to delegate to
     */
    public DelegatingServiceRegistry(final ServiceRegistry delegate) {
        this.delegate = delegate;
    }

    /**
     * Get the ServiceRegistry delegate.
     * @return ServiceRegistry delegate
     */
    protected ServiceRegistry getDelegate() {
        return delegate;
    }

    /** {@inheritDoc} */
    @Override
    public ServiceController<?> getRequiredService(final ServiceName serviceName) throws ServiceNotFoundException {
        return getDelegate().getRequiredService(serviceName);
    }

    /** {@inheritDoc} */
    @Override
    public ServiceController<?> getService(final ServiceName serviceName) {
        return getDelegate().getService(serviceName);
    }

    /** {@inheritDoc} */
    @Override
    public List<ServiceName> getServiceNames() {
        return getDelegate().getServiceNames();
    }

}
