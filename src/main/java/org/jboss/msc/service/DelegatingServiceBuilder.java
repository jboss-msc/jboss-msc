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

import org.jboss.msc.inject.Injector;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * A service builder which delegates to another service builder.
 *
 * @param <T> the service type
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
public class DelegatingServiceBuilder<T> implements ServiceBuilder<T> {

    private final ServiceBuilder<T> delegate;

    /**
     * Construct a new instance.
     *
     * @param delegate the builder to delegate to
     */
    public DelegatingServiceBuilder(final ServiceBuilder<T> delegate) {
        this.delegate = delegate;
    }

    /**
     * Get the ServiceBuilder delegate.
     * @return ServiceBuilder delegate
     */
    protected ServiceBuilder<T> getDelegate() {
        return delegate;
    }

    /** {@inheritDoc} */
    public <V> Supplier<V> requires(final ServiceName name) {
        return getDelegate().requires(name);
    }

    /** {@inheritDoc} */
    public <V> Consumer<V> provides(final ServiceName... names) {
        return getDelegate().provides(names);
    }

    /** {@inheritDoc} */
    public ServiceBuilder<T> setInitialMode(final ServiceController.Mode mode) {
        getDelegate().setInitialMode(mode);
        return this;
    }

    /** {@inheritDoc} */
    public ServiceBuilder<T> setInstance(final org.jboss.msc.Service service) {
        getDelegate().setInstance(service);
        return this;
    }

    /** {@inheritDoc} */
    public ServiceBuilder<T> addListener(final LifecycleListener listener) {
        getDelegate().addListener(listener);
        return this;
    }

    /** {@inheritDoc} */
    public ServiceController<T> install() {
        return getDelegate().install();
    }

    ////////////////////////
    // DEPRECATED METHODS //
    ////////////////////////

    /** {@inheritDoc} */
    @Deprecated
    @Override
    public ServiceBuilder<T> addAliases(final ServiceName... aliases) {
        getDelegate().addAliases(aliases);
        return this;
    }

    /** {@inheritDoc} */
    @Deprecated
    @Override
    public ServiceBuilder<T> addDependencies(final ServiceName... dependencies) {
        getDelegate().addDependencies(dependencies);
        return this;
    }

    /** {@inheritDoc} */
    @Deprecated
    @Override
    public ServiceBuilder<T> addDependencies(final Iterable<ServiceName> dependencies) {
        getDelegate().addDependencies(dependencies);
        return this;
    }

    /** {@inheritDoc} */
    @Deprecated
    @Override
    public ServiceBuilder<T> addDependency(final ServiceName dependency) {
        getDelegate().addDependency(dependency);
        return this;
    }

    /** {@inheritDoc} */
    @Deprecated
    @Override
    public ServiceBuilder<T> addDependency(final ServiceName dependency, final Injector<Object> target) {
        getDelegate().addDependency(dependency, target);
        return this;
    }

    /** {@inheritDoc} */
    @Deprecated
    @Override
    public <I> ServiceBuilder<T> addDependency(final ServiceName dependency, final Class<I> type, final Injector<I> target) {
        getDelegate().addDependency(dependency, type, target);
        return this;
    }

    /** {@inheritDoc} */
    @Deprecated
    @Override
    public ServiceBuilder<T> addMonitor(final StabilityMonitor monitor) {
        getDelegate().addMonitor(monitor);
        return this;
    }

}
