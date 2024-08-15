/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2024, Red Hat, Inc., and individual contributors
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
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
final class LeakDetectorServiceContainer implements ServiceContainer {

    private final ServiceContainer delegate;

    LeakDetectorServiceContainer(final ServiceContainer delegate) {
        this.delegate = delegate;
    }

    private ServiceContainer getDelegate() {
        return delegate;
    }

    @Override
    public void shutdown() {
        getDelegate().shutdown();
    }

    @Override
    public boolean isShutdown() {
        return getDelegate().isShutdown();
    }

    @Override
    public boolean isShutdownComplete() {
        return getDelegate().isShutdownComplete();
    }

    @Override
    public void addTerminateListener(final TerminateListener listener) {
        getDelegate().addTerminateListener(listener);
    }

    @Override
    public void awaitTermination() throws InterruptedException {
        getDelegate().awaitTermination();
    }

    @Override
    public void awaitTermination(final long timeout, final TimeUnit unit) throws InterruptedException {
        getDelegate().awaitTermination(timeout, unit);
    }

    @Override
    public void awaitStability() throws InterruptedException {
        getDelegate().awaitStability();
    }

    @Override
    public boolean awaitStability(final long timeout, final TimeUnit unit) throws InterruptedException {
        return getDelegate().awaitStability(timeout, unit);
    }

    @Override
    public void awaitStability(final Set<? super ServiceController<?>> failed, final Set<? super ServiceController<?>> problem) throws InterruptedException {
        getDelegate().awaitStability(failed, problem);
    }

    @Override
    public boolean awaitStability(final long timeout, TimeUnit unit, final Set<? super ServiceController<?>> failed, final Set<? super ServiceController<?>> problem) throws InterruptedException {
        return getDelegate().awaitStability(timeout, unit, failed, problem);
    }

    @Override
    public void dumpServices() {
        getDelegate().dumpServices();
    }

    @Override
    public void dumpServices(final PrintStream stream) {
        getDelegate().dumpServices(stream);
    }

    @Override
    public String getName() {
        return getDelegate().getName();
    }

    @Override
    public ServiceController<?> getRequiredService(final ServiceName serviceName) throws ServiceNotFoundException {
        return getDelegate().getRequiredService(serviceName);
    }

    @Override
    public ServiceController<?> getService(final ServiceName serviceName) {
        return getDelegate().getService(serviceName);
    }

    @Override
    public List<ServiceName> getServiceNames() {
        return getDelegate().getServiceNames();
    }

    @Override
    public ServiceTarget addListener(final LifecycleListener listener) {
        return getDelegate().addListener(listener);
    }

    @Override
    public ServiceTarget removeListener(final LifecycleListener listener) {
        return getDelegate().removeListener(listener);
    }

    @Override
    public ServiceBuilder<?> addService() {
        return getDelegate().addService();
    }

    @Override
    public ServiceTarget subTarget() {
        return getDelegate().subTarget();
    }

    @Override
    public ServiceBuilder<?> addService(final ServiceName name) {
        return getDelegate().addService(name);
    }

    @Override
    public <T> ServiceBuilder<T> addService(final ServiceName name, final Service<T> service) {
        return getDelegate().addService(name, service);
    }

    @Override
    public ServiceTarget addMonitor(final StabilityMonitor monitor) {
        return getDelegate().addMonitor(monitor);
    }

    @Override
    public ServiceTarget removeMonitor(final StabilityMonitor monitor) {
        return getDelegate().removeMonitor(monitor);
    }

    @Override
    public ServiceTarget addDependency(final ServiceName dependency) {
        return getDelegate().addDependency(dependency);
    }

}
