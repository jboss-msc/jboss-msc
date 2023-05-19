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

/**
 * A service target which delegates to another service target.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
public class DelegatingServiceTarget implements ServiceTarget {

    private final ServiceTarget delegate;

    /**
     * Construct a new instance.
     *
     * @param delegate the delegate service target
     */
    public DelegatingServiceTarget(final ServiceTarget delegate) {
        this.delegate = delegate;
    }

    /**
     * Get the ServiceTarget delegate.
     * @return ServiceTarget delegate
     */
    protected ServiceTarget getDelegate() {
        return delegate;
    }

    /** {@inheritDoc} */
    public ServiceTarget addListener(final LifecycleListener listener) {
        getDelegate().addListener(listener);
        return this;
    }

    /** {@inheritDoc} */
    public ServiceTarget removeListener(final LifecycleListener listener) {
        getDelegate().removeListener(listener);
        return this;
    }

    /** {@inheritDoc} */
    public ServiceBuilder<?> addService() {
        return getDelegate().addService();
    }

    /** {@inheritDoc} */
    public ServiceTarget subTarget() {
        return getDelegate().subTarget();
    }

    ////////////////////////
    // DEPRECATED METHODS //
    ////////////////////////

    /** {@inheritDoc} */
    @Deprecated
    @Override
    public ServiceBuilder<?> addService(ServiceName name) {
        return getDelegate().addService(name);
    }

    /** {@inheritDoc} */
    @Deprecated
    @Override
    public <T> ServiceBuilder<T> addService(final ServiceName name, final Service<T> service) {
        return getDelegate().addService(name, service);
    }

    /** {@inheritDoc} */
    @Deprecated
    @Override
    public ServiceTarget addDependency(final ServiceName dependency) {
        getDelegate().addDependency(dependency);
        return this;
    }

    /** {@inheritDoc} */
    @Deprecated
    @Override
    public ServiceTarget addMonitor(final StabilityMonitor monitor) {
        getDelegate().addMonitor(monitor);
        return this;
    }

    /** {@inheritDoc} */
    @Deprecated
    @Override
    public ServiceTarget removeMonitor(final StabilityMonitor monitor) {
        getDelegate().removeMonitor(monitor);
        return this;
    }
}
