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

import java.io.PrintStream;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * A delegating container for things which depend on a service container rather than a specific
 * target or registry instance.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
public class DelegatingServiceContainer implements ServiceContainer {

    private final ServiceTarget serviceTargetDelegate;
    private final ServiceRegistry serviceRegistryDelegate;

    /**
     * Construct a new instance.
     *
     * @param serviceTargetDelegate the delegate to forward service target requests to
     * @param serviceRegistryDelegate the delegate to forward registry requests to
     */
    public DelegatingServiceContainer(final ServiceTarget serviceTargetDelegate, final ServiceRegistry serviceRegistryDelegate) {
        this.serviceTargetDelegate = serviceTargetDelegate;
        this.serviceRegistryDelegate = serviceRegistryDelegate;
    }

    /**
     * Get the ServiceTarget delegate.
     * @return ServiceTarget delegate
     */
    protected ServiceTarget getServiceTargetDelegate() {
        return serviceTargetDelegate;
    }

    /**
     * Get the ServiceRegistry delegate.
     * @return ServiceRegistry delegate
     */
    protected ServiceRegistry getServiceRegistryDelegate() {
        return serviceRegistryDelegate;
    }

    /** {@inheritDoc} */
    @Override
    public ServiceBuilder<?> addService() {
        return getServiceTargetDelegate().addService();
    }

    /** {@inheritDoc} */
    @Override
    public ServiceContainer addListener(final LifecycleListener listener) {
        getServiceTargetDelegate().addListener(listener);
        return this;
    }

    /** {@inheritDoc} */
    @Override
    public ServiceContainer removeListener(final LifecycleListener listener) {
        getServiceTargetDelegate().removeListener(listener);
        return this;
    }

    /** {@inheritDoc} */
    @Override
    public ServiceTarget subTarget() {
        return getServiceTargetDelegate().subTarget();
    }

    /** {@inheritDoc} */
    @Override
    public ServiceController<?> getRequiredService(final ServiceName serviceName) throws ServiceNotFoundException {
        return getServiceRegistryDelegate().getRequiredService(serviceName);
    }

    /** {@inheritDoc} */
    @Override
    public ServiceController<?> getService(final ServiceName serviceName) {
        return getServiceRegistryDelegate().getService(serviceName);
    }

    /** {@inheritDoc} */
    @Override
    public List<ServiceName> getServiceNames() {
        return getServiceRegistryDelegate().getServiceNames();
    }

    ///////////////////////////
    // UNIMPLEMENTED METHODS //
    ///////////////////////////

    /** {@inheritDoc} */
    @Override
    public String getName() {
        throw new UnsupportedOperationException();
    }

    /** {@inheritDoc} */
    @Override
    public void shutdown() {
        throw new UnsupportedOperationException();
    }

    /** {@inheritDoc} */
    @Override
    public boolean isShutdown() {
        throw new UnsupportedOperationException();
    }

    /** {@inheritDoc} */
    @Override
    public boolean isShutdownComplete() {
        throw new UnsupportedOperationException();
    }

    /** {@inheritDoc} */
    @Override
    public void dumpServices() {
        throw new UnsupportedOperationException();
    }

    /** {@inheritDoc} */
    @Override
    public void dumpServices(final PrintStream stream) {
        throw new UnsupportedOperationException();
    }

    /** {@inheritDoc} */
    @Override
    public void addTerminateListener(TerminateListener listener) {
        throw new UnsupportedOperationException();
    }

    /** {@inheritDoc} */
    @Override
    public void awaitTermination() {
        throw new UnsupportedOperationException();
    }

    /** {@inheritDoc} */
    @Override
    public void awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        throw new UnsupportedOperationException();
    }

    /** {@inheritDoc} */
    @Override
    public void awaitStability() throws InterruptedException {
        throw new UnsupportedOperationException();
    }

    /** {@inheritDoc} */
    @Override
    public boolean awaitStability(final long timeout, final TimeUnit unit) throws InterruptedException {
        throw new UnsupportedOperationException();
    }

    /** {@inheritDoc} */
    @Override
    public void awaitStability(final Set<? super ServiceController<?>> failed, final Set<? super ServiceController<?>> problem) throws InterruptedException {
        throw new UnsupportedOperationException();
    }

    /** {@inheritDoc} */
    @Override
    public boolean awaitStability(final long timeout, final TimeUnit unit, final Set<? super ServiceController<?>> failed, final Set<? super ServiceController<?>> problem) throws InterruptedException {
        throw new UnsupportedOperationException();
    }

    ////////////////////////
    // DEPRECATED METHODS //
    ////////////////////////

    /** {@inheritDoc} */
    @Deprecated
    @Override
    public ServiceBuilder<?> addService(final ServiceName name) {
        return getServiceTargetDelegate().addService(name);
    }

    /** {@inheritDoc} */
    @Deprecated
    @Override
    public <T> ServiceBuilder<T> addService(final ServiceName name, final Service<T> service) {
        return getServiceTargetDelegate().addService(name, service);
    }

    /** {@inheritDoc} */
    @Deprecated
    @Override
    public ServiceTarget addMonitor(StabilityMonitor monitor) {
        getServiceTargetDelegate().addMonitor(monitor);
        return this;
    }

    /** {@inheritDoc} */
    @Deprecated
    @Override
    public ServiceTarget removeMonitor(StabilityMonitor monitor) {
        getServiceTargetDelegate().removeMonitor(monitor);
        return this;
    }

    /** {@inheritDoc} */
    @Deprecated
    @Override
    public ServiceContainer addDependency(final ServiceName dependency) {
        getServiceTargetDelegate().addDependency(dependency);
        return this;
    }
}
