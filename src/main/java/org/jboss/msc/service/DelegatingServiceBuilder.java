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
import org.jboss.msc.inject.Injector;
import org.jboss.msc.value.Value;

/**
 * A service builder which delegates to another service builder.
 *
 * @param <T> the service type
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
public final class DelegatingServiceBuilder<T> implements ServiceBuilder<T> {
    private final ServiceBuilder<T> delegate;

    /**
     * Construct a new instance.
     *
     * @param delegate the builder to delegate to
     */
    public DelegatingServiceBuilder(final ServiceBuilder<T> delegate) {
        this.delegate = delegate;
    }

    /** {@inheritDoc} */
    public ServiceBuilder<T> addAliases(final ServiceName... aliases) {
        delegate.addAliases(aliases);
        return this;
    }

    /** {@inheritDoc} */
    public ServiceBuilder<T> setInitialMode(final ServiceController.Mode mode) {
        delegate.setInitialMode(mode);
        return this;
    }

    /** {@inheritDoc} */
    public ServiceBuilder<T> addDependencies(final ServiceName... dependencies) {
        delegate.addDependencies(dependencies);
        return this;
    }

    /** {@inheritDoc} */
    public ServiceBuilder<T> addDependencies(final DependencyType dependencyType, final ServiceName... dependencies) {
        delegate.addDependencies(dependencyType, dependencies);
        return this;
    }

    /** {@inheritDoc} */
    public ServiceBuilder<T> addDependencies(final Iterable<ServiceName> dependencies) {
        delegate.addDependencies(dependencies);
        return this;
    }

    /** {@inheritDoc} */
    public ServiceBuilder<T> addDependencies(final DependencyType dependencyType, final Iterable<ServiceName> dependencies) {
        delegate.addDependencies(dependencyType, dependencies);
        return this;
    }

    /** {@inheritDoc} */
    public ServiceBuilder<T> addDependency(final ServiceName dependency) {
        delegate.addDependency(dependency);
        return this;
    }

    /** {@inheritDoc} */
    public ServiceBuilder<T> addDependency(final DependencyType dependencyType, final ServiceName dependency) {
        delegate.addDependency(dependencyType, dependency);
        return this;
    }

    /** {@inheritDoc} */
    public ServiceBuilder<T> addDependency(final ServiceName dependency, final Injector<Object> target) {
        delegate.addDependency(dependency, target);
        return this;
    }

    /** {@inheritDoc} */
    public ServiceBuilder<T> addDependency(final DependencyType dependencyType, final ServiceName dependency, final Injector<Object> target) {
        delegate.addDependency(dependencyType, dependency, target);
        return this;
    }

    /** {@inheritDoc} */
    public <I> ServiceBuilder<T> addDependency(final ServiceName dependency, final Class<I> type, final Injector<I> target) {
        delegate.addDependency(dependency, type, target);
        return this;
    }

    /** {@inheritDoc} */
    public <I> ServiceBuilder<T> addDependency(final DependencyType dependencyType, final ServiceName dependency, final Class<I> type, final Injector<I> target) {
        delegate.addDependency(dependencyType, dependency, type, target);
        return this;
    }

    /** {@inheritDoc} */
    public <I> ServiceBuilder<T> addInjection(final Injector<? super I> target, final I value) {
        delegate.addInjection(target, value);
        return this;
    }

    /** {@inheritDoc} */
    public <I> ServiceBuilder<T> addInjectionValue(final Injector<? super I> target, final Value<I> value) {
        delegate.addInjectionValue(target, value);
        return this;
    }

    /** {@inheritDoc} */
    public ServiceBuilder<T> addInjection(final Injector<? super T> target) {
        delegate.addInjection(target);
        return this;
    }

    @Override
    public ServiceBuilder<T> addMonitor(final StabilityMonitor monitor) {
        delegate.addMonitor(monitor);
        return this;
    }

    @Override
    public ServiceBuilder<T> addMonitors(final StabilityMonitor... monitors) {
        delegate.addMonitors(monitors);
        return this;
    }

    /** {@inheritDoc} */
    public ServiceBuilder<T> addListener(final ServiceListener<? super T> listener) {
        delegate.addListener(listener);
        return this;
    }

    /** {@inheritDoc} */
    public ServiceBuilder<T> addListener(final ServiceListener<? super T>... listeners) {
        delegate.addListener(listeners);
        return this;
    }

    /** {@inheritDoc} */
    public ServiceBuilder<T> addListener(final Collection<? extends ServiceListener<? super T>> listeners) {
        delegate.addListener(listeners);
        return this;
    }

    /** {@inheritDoc} */
    public ServiceController<T> install() throws ServiceRegistryException {
        return delegate.install();
    }
}
