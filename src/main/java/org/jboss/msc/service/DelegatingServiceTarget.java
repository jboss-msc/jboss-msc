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

import java.util.Collection;
import java.util.Set;

import org.jboss.msc.value.Value;

/**
 * An "insulated" view of a service target which prevents access to other public methods on the delegate target object.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
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

    /** {@inheritDoc} */
    public <T> ServiceBuilder<T> addServiceValue(final ServiceName name, final Value<? extends Service<T>> value) throws IllegalArgumentException {
        return delegate.addServiceValue(name, value);
    }

    /** {@inheritDoc} */
    public <T> ServiceBuilder<T> addService(final ServiceName name, final Service<T> service) throws IllegalArgumentException {
        return delegate.addService(name, service);
    }

    /** {@inheritDoc} */
    public ServiceTarget addListener(final ServiceListener<Object> listener) {
        delegate.addListener(listener);
        return this;
    }

    /** {@inheritDoc} */
    public ServiceTarget addListener(final ServiceListener<Object>... listeners) {
        delegate.addListener(listeners);
        return this;
    }

    /** {@inheritDoc} */
    public ServiceTarget addListener(final Collection<ServiceListener<Object>> listeners) {
        delegate.addListener(listeners);
        return this;
    }

    /** {@inheritDoc} */
    public ServiceTarget removeListener(final ServiceListener<Object> listener) {
        delegate.removeListener(listener);
        return this;
    }

    /** {@inheritDoc} */
    public Set<ServiceListener<Object>> getListeners() {
        return delegate.getListeners();
    }

    /** {@inheritDoc} */
    public ServiceTarget addDependency(final ServiceName dependency) {
        delegate.addDependency(dependency);
        return this;
    }

    /** {@inheritDoc} */
    public ServiceTarget addDependency(final ServiceName... dependencies) {
        delegate.addDependency(dependencies);
        return this;
    }

    /** {@inheritDoc} */
    public ServiceTarget addDependency(final Collection<ServiceName> dependencies) {
        delegate.addDependency(dependencies);
        return this;
    }

    /** {@inheritDoc} */
    public ServiceTarget removeDependency(final ServiceName dependency) {
        delegate.removeDependency(dependency);
        return this;
    }

    /** {@inheritDoc} */
    public Set<ServiceName> getDependencies() {
        return delegate.getDependencies();
    }

    /** {@inheritDoc} */
    public ServiceTarget subTarget() {
        return delegate.subTarget();
    }

    /** {@inheritDoc} */
    public BatchBuilder batchBuilder() {
        return delegate.batchBuilder();
    }
}
