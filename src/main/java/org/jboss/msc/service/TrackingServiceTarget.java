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
import java.util.Collections;
import java.util.HashSet;

import java.util.Set;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.value.Value;

/**
 * A service target which tracks what services are added to it.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public class TrackingServiceTarget extends DelegatingServiceTarget {
    private final Set<ServiceName> set;

    private TrackingServiceTarget(final ServiceTarget delegate, final Set<ServiceName> set) {
        super(delegate);
        this.set = set;
    }

    /**
     * Construct a new instance.
     *
     * @param delegate the delegate instance
     */
    public TrackingServiceTarget(final ServiceTarget delegate) {
        this(delegate, Collections.synchronizedSet(new HashSet<ServiceName>()));
    }

    /** {@inheritDoc} */
    public <T> ServiceBuilder<T> addServiceValue(final ServiceName name, final Value<? extends Service<T>> value) throws IllegalArgumentException {
        return new Builder<T>(name, super.addServiceValue(name, value));
    }

    /** {@inheritDoc} */
    public <T> ServiceBuilder<T> addService(final ServiceName name, final Service<T> service) throws IllegalArgumentException {
        return new Builder<T>(name, super.addService(name, service));
    }

    /** {@inheritDoc} */
    public ServiceTarget subTarget() {
        return new TrackingServiceTarget(super.subTarget(), set);
    }

    /** {@inheritDoc} */
    public BatchBuilder batchBuilder() {
        return new TrackingBatchBuilder(super.batchBuilder(), set);
    }

    /**
     * Get the set of service names.
     *
     * @return the set of service names
     */
    public Set<ServiceName> getSet() {
        return set;
    }

    private class Builder<T> implements ServiceBuilder<T> {

        private final ServiceName name;
        private final ServiceBuilder<T> builder;

        Builder(final ServiceName name, final ServiceBuilder<T> builder) {
            this.name = name;
            this.builder = builder;
        }

        public ServiceBuilder<T> addAliases(final ServiceName... aliases) {
            return builder.addAliases(aliases);
        }

        public ServiceBuilder<T> setLocation() {
            return builder.setLocation();
        }

        public ServiceBuilder<T> setLocation(final Location location) {
            return builder.setLocation(location);
        }

        public ServiceBuilder<T> setInitialMode(final ServiceController.Mode mode) {
            return builder.setInitialMode(mode);
        }

        public ServiceBuilder<T> addDependencies(final ServiceName... dependencies) {
            return builder.addDependencies(dependencies);
        }

        public ServiceBuilder<T> addDependencies(final DependencyType dependencyType, final ServiceName... dependencies) {
            return builder.addDependencies(dependencyType, dependencies);
        }

        @SuppressWarnings({ "deprecation" })
        public ServiceBuilder<T> addOptionalDependencies(final ServiceName... dependencies) {
            return builder.addOptionalDependencies(dependencies);
        }

        public ServiceBuilder<T> addDependencies(final Iterable<ServiceName> dependencies) {
            return builder.addDependencies(dependencies);
        }

        public ServiceBuilder<T> addDependencies(final DependencyType dependencyType, final Iterable<ServiceName> dependencies) {
            return builder.addDependencies(dependencyType, dependencies);
        }

        @SuppressWarnings({ "deprecation" })
        public ServiceBuilder<T> addOptionalDependencies(final Iterable<ServiceName> dependencies) {
            return builder.addOptionalDependencies(dependencies);
        }

        public ServiceBuilder<T> addDependency(final ServiceName dependency) {
            return builder.addDependency(dependency);
        }

        public ServiceBuilder<T> addDependency(final DependencyType dependencyType, final ServiceName dependency) {
            return builder.addDependency(dependencyType, dependency);
        }

        @SuppressWarnings({ "deprecation" })
        public ServiceBuilder<T> addOptionalDependency(final ServiceName dependency) {
            return builder.addOptionalDependency(dependency);
        }

        public ServiceBuilder<T> addDependency(final ServiceName dependency, final Injector<Object> target) {
            return builder.addDependency(dependency, target);
        }

        public ServiceBuilder<T> addDependency(final DependencyType dependencyType, final ServiceName dependency, final Injector<Object> target) {
            return builder.addDependency(dependencyType, dependency, target);
        }

        @SuppressWarnings({ "deprecation" })
        public ServiceBuilder<T> addOptionalDependency(final ServiceName dependency, final Injector<Object> target) {
            return builder.addOptionalDependency(dependency, target);
        }

        public <I> ServiceBuilder<T> addDependency(final ServiceName dependency, final Class<I> type, final Injector<I> target) {
            return builder.addDependency(dependency, type, target);
        }

        public <I> ServiceBuilder<T> addDependency(final DependencyType dependencyType, final ServiceName dependency, final Class<I> type, final Injector<I> target) {
            return builder.addDependency(dependencyType, dependency, type, target);
        }

        @SuppressWarnings({ "deprecation" })
        public <I> ServiceBuilder<T> addOptionalDependency(final ServiceName dependency, final Class<I> type, final Injector<I> target) {
            return builder.addOptionalDependency(dependency, type, target);
        }

        public <I> ServiceBuilder<T> addInjection(final Injector<? super I> target, final I value) {
            return builder.addInjection(target, value);
        }

        public <I> ServiceBuilder<T> addInjectionValue(final Injector<? super I> target, final Value<I> value) {
            return builder.addInjectionValue(target, value);
        }

        public ServiceBuilder<T> addListener(final ServiceListener<? super T> listener) {
            return builder.addListener(listener);
        }

        public ServiceBuilder<T> addListener(final ServiceListener<? super T>... listeners) {
            return builder.addListener(listeners);
        }

        public ServiceBuilder<T> addListener(final Collection<? extends ServiceListener<? super T>> listeners) {
            return builder.addListener(listeners);
        }

        public void install() throws ServiceRegistryException {
            set.add(name);
            builder.install();
        }
    }
}
