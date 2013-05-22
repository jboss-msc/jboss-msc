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

import java.io.PrintStream;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.jboss.msc.value.Value;

/**
 * A delegating container for things which depend on a service container rather than a specific
 * target or registry instance.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
public final class DelegatingServiceContainer implements ServiceContainer {
    private final ServiceTarget delegateTarget;
    private final ServiceRegistry delegateRegistry;

    /**
     * Construct a new instance.
     *
     * @param delegateTarget the delegate to forward service target requests to
     * @param delegateRegistry the delegate to forward registry requests to
     */
    public DelegatingServiceContainer(final ServiceTarget delegateTarget, final ServiceRegistry delegateRegistry) {

        this.delegateTarget = delegateTarget;
        this.delegateRegistry = delegateRegistry;
    }

    /** {@inheritDoc} */
    public <T> ServiceBuilder<T> addServiceValue(final ServiceName name, final Value<? extends Service<T>> value) throws IllegalArgumentException {
        return delegateTarget.addServiceValue(name, value);
    }

    /** {@inheritDoc} */
    public <T> ServiceBuilder<T> addService(final ServiceName name, final Service<T> service) throws IllegalArgumentException {
        return delegateTarget.addService(name, service);
    }

    /** {@inheritDoc} */
    public ServiceContainer addListener(final ServiceListener<Object> listener) {
        delegateTarget.addListener(listener);
        return this;
    }

    /** {@inheritDoc} */
    public ServiceContainer addListener(final ServiceListener<Object>... listeners) {
        delegateTarget.addListener(listeners);
        return this;
    }

    /** {@inheritDoc} */
    public ServiceContainer addListener(final Collection<ServiceListener<Object>> listeners) {
        delegateTarget.addListener(listeners);
        return this;
    }

    /** {@inheritDoc} */
    public ServiceContainer addListener(final ServiceListener.Inheritance inheritance, final ServiceListener<Object> listener) {
        delegateTarget.addListener(inheritance, listener);
        return this;
    }

    /** {@inheritDoc} */
    public ServiceContainer addListener(final ServiceListener.Inheritance inheritance, final ServiceListener<Object>... listeners) {
        delegateTarget.addListener(inheritance, listeners);
        return this;
    }

    /** {@inheritDoc} */
    public ServiceContainer addListener(final ServiceListener.Inheritance inheritance, final Collection<ServiceListener<Object>> listeners) {
        delegateTarget.addListener(inheritance, listeners);
        return this;
    }

    /** {@inheritDoc} */
    public ServiceContainer removeListener(final ServiceListener<Object> listener) {
        delegateTarget.removeListener(listener);
        return this;
    }

    /** {@inheritDoc} */
    public Set<ServiceListener<Object>> getListeners() {
        return delegateTarget.getListeners();
    }

    /** {@inheritDoc} */
    public ServiceTarget addMonitor(StabilityMonitor monitor) {
        delegateTarget.addMonitor(monitor);
        return this;
    }

    /** {@inheritDoc} */
    public ServiceTarget addMonitors(StabilityMonitor... monitors) {
        delegateTarget.addMonitors(monitors);
        return this;
    }

    /** {@inheritDoc} */
    public ServiceTarget removeMonitor(StabilityMonitor monitor) {
        delegateTarget.removeMonitor(monitor);
        return this;
    }

    /** {@inheritDoc} */
    public Set<StabilityMonitor> getMonitors() {
        return delegateTarget.getMonitors();
    }

    /** {@inheritDoc} */
    public ServiceContainer addDependency(final ServiceName dependency) {
        delegateTarget.addDependency(dependency);
        return this;
    }

    /** {@inheritDoc} */
    public ServiceContainer addDependency(final ServiceName... dependencies) {
        delegateTarget.addDependency(dependencies);
        return this;
    }

    /** {@inheritDoc} */
    public ServiceContainer addDependency(final Collection<ServiceName> dependencies) {
        delegateTarget.addDependency(dependencies);
        return this;
    }

    /** {@inheritDoc} */
    public ServiceContainer removeDependency(final ServiceName dependency) {
        delegateTarget.removeDependency(dependency);
        return this;
    }

    /** {@inheritDoc} */
    public Set<ServiceName> getDependencies() {
        return delegateTarget.getDependencies();
    }

    /** {@inheritDoc} */
    public ServiceTarget subTarget() {
        return delegateTarget.subTarget();
    }

    /** {@inheritDoc} */
    public BatchServiceTarget batchTarget() {
        return delegateTarget.batchTarget();
    }

    /** {@inheritDoc} */
    public ServiceController<?> getRequiredService(final ServiceName serviceName) throws ServiceNotFoundException {
        return delegateRegistry.getRequiredService(serviceName);
    }

    /** {@inheritDoc} */
    public ServiceController<?> getService(final ServiceName serviceName) {
        return delegateRegistry.getService(serviceName);
    }

    /** {@inheritDoc} */
    public List<ServiceName> getServiceNames() {
        return delegateRegistry.getServiceNames();
    }

    /** {@inheritDoc} */
    public String getName() {
        throw new UnsupportedOperationException();
    }

    /** {@inheritDoc} */
    public void shutdown() {
        throw new UnsupportedOperationException();
    }

    /** {@inheritDoc} */
    public boolean isShutdown() {
        throw new UnsupportedOperationException();
    }

    /** {@inheritDoc} */
    public boolean isShutdownComplete() {
        throw new UnsupportedOperationException();
    }

    /** {@inheritDoc} */
    public void dumpServices() {
        throw new UnsupportedOperationException();
    }

    /** {@inheritDoc} */
    public void dumpServices(final PrintStream stream) {
        throw new UnsupportedOperationException();
    }

    /** {@inheritDoc} */
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
}
