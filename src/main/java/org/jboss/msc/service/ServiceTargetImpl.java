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

import static java.util.Collections.synchronizedSet;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.jboss.msc.value.Value;

/**
 * Abstract base class used for ServiceTargets.
 *
 * @author John Bailey
 * @author <a href="mailto:flavia.rainone@jboss.com">Flavia Rainone</a>
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
class ServiceTargetImpl implements ServiceTarget {

    private final ServiceTargetImpl parent;
    private final Set<LifecycleListener> lifecycleListeners = synchronizedSet(new IdentityHashSet<>());
    private final Set<ServiceName> dependencies = synchronizedSet(new HashSet<>());
    private final Set<StabilityMonitor> monitors = synchronizedSet(new IdentityHashSet<>());

    ServiceTargetImpl(final ServiceTargetImpl parent) {
        if (parent == null) {
            throw new IllegalStateException("parent is null");
        }
        this.parent = parent;
    }

    ServiceTargetImpl() {
        this.parent = null;
    }

    @Override
    public <T> ServiceBuilder<T> addServiceValue(final ServiceName name, final Value<? extends Service<T>> value) throws IllegalArgumentException {
        if (name == null) {
            throw new IllegalArgumentException("name is null");
        }
        if (value == null) {
            throw new IllegalArgumentException("value is null");
        }
        final Service<T> service = value.getValue();
        return createServiceBuilder(name, (Service<T>)(service != null ? service : Service.NULL), null);
    }

    protected <T> ServiceBuilder<T> createServiceBuilder(final ServiceName name, final Service<T> service, final ServiceControllerImpl<?> parent) throws IllegalArgumentException {
        return new ServiceBuilderImpl<>(name, this, service, parent);
    }

    protected ServiceBuilder<?> createServiceBuilder(final ServiceName name, final ServiceControllerImpl<?> parent) throws IllegalArgumentException {
        return new ServiceBuilderImpl<>(name, this, parent);
    }

    @Override
    public <T> ServiceBuilder<T> addService(final ServiceName name, final Service<T> service) throws IllegalArgumentException {
        if (name == null) {
            throw new IllegalArgumentException("name is null");
        }
        if (service == null) {
            throw new IllegalArgumentException("service is null");
        }
        return createServiceBuilder(name, service, null);
    }

    @Override
    public ServiceBuilder<?> addService(final ServiceName name) {
        if (name == null) {
            throw new IllegalArgumentException("name is null");
        }
        return createServiceBuilder(name, null);
    }

    public ServiceTarget addListener(final LifecycleListener listener) {
        if (listener != null) {
            lifecycleListeners.add(listener);
        }
        return this;
    }

    @Override
    public ServiceTarget addMonitor(final StabilityMonitor monitor) {
        if (monitor != null) {
            monitors.add(monitor);
        }
        return this;
    }

    @Override
    public ServiceTarget removeMonitor(final StabilityMonitor monitor) {
        if (monitor != null) {
            monitors.remove(monitor);
        }
        return this;
    }

    @Override
    public ServiceTarget removeListener(final LifecycleListener listener) {
        if (listener != null) lifecycleListeners.remove(listener);
        return this;
    }

    @Override
    public ServiceTarget addDependency(ServiceName dependency) {
        if (dependency == null) {
            return this;
        }
        dependencies.add(dependency);
        return this;
    }

    @Override
    public ServiceTarget addDependency(ServiceName... dependencies) {
        if (dependencies == null) {
            return this;
        }
        final Set<ServiceName> myDependencies = this.dependencies;
        for(ServiceName dependency : dependencies) {
            myDependencies.add(dependency);
        }
        return this;
    }

    @Override
    public ServiceTarget addDependency(Collection<ServiceName> dependencies) {
        if (dependencies == null) {
            return this;
        }
        final Set<ServiceName> myDependencies = this.dependencies;
        for(ServiceName dependency : dependencies) {
            myDependencies.add(dependency);
        }
        return this;
    }

    @Override
    public ServiceTarget removeDependency(final ServiceName dependency) {
        if (dependency == null) {
            return this;
        }
        dependencies.remove(dependency);
        return this;
    }

    /**
     * Apply listeners and dependencies to {@code serviceBuilder}.
     * 
     * @param serviceBuilder serviceBuilder which listeners and dependencies will be added to.
     */
    void apply(ServiceBuilderImpl<?> serviceBuilder) {
        synchronized (monitors) {
            serviceBuilder.addMonitorsNoCheck(monitors);
        }
        synchronized (lifecycleListeners) {
            serviceBuilder.addLifecycleListenersNoCheck(lifecycleListeners);
        }
        synchronized (dependencies) {
            serviceBuilder.addDependenciesNoCheck(dependencies);
        }
    }

    /**
     * Install {@code serviceBuilder} in this target.
     *
     * @param serviceBuilder a serviceBuilder created by this ServiceTarget
     *
     * @return the installed service controller
     *
     * @throws ServiceRegistryException if a service registry issue occurred during installation
     */
    <T> ServiceController<T> install(ServiceBuilderImpl<T> serviceBuilder) throws ServiceRegistryException {
        apply(serviceBuilder);
        return parent.install(serviceBuilder);
    }

    ServiceRegistrationImpl getOrCreateRegistration(final ServiceName name) {
        return parent.getOrCreateRegistration(name);
    }

    @Override
    public ServiceTarget subTarget() {
        return new ServiceTargetImpl(this);
    }
}
