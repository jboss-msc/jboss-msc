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

import java.util.Collection;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * A service controller which delegates to another service controller.
 *
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
public class DelegatingServiceController<S> implements ServiceController<S> {

    private final ServiceController<S> delegate;

    /**
     * Construct a new instance.
     *
     * @param delegate the controller to delegate to
     */
    public DelegatingServiceController(final ServiceController<S> delegate) {
        this.delegate = delegate;
    }

    /**
     * Get the ServiceController delegate.
     * @return ServiceController delegate
     */
    protected ServiceController<S> getDelegate() {
        return delegate;
    }

    /** {@inheritDoc} */
    @Override
    public ServiceController<?> getParent() {
        return getDelegate().getParent();
    }

    /** {@inheritDoc} */
    @Override
    public ServiceContainer getServiceContainer() {
        return getDelegate().getServiceContainer();
    }

    /** {@inheritDoc} */
    @Override
    public Mode getMode() {
        return getDelegate().getMode();
    }

    /** {@inheritDoc} */
    @Override
    public boolean compareAndSetMode(final Mode expected, final Mode newMode) {
        return getDelegate().compareAndSetMode(expected, newMode);
    }

    /** {@inheritDoc} */
    @Override
    public void setMode(final Mode mode) {
        getDelegate().setMode(mode);
    }

    /** {@inheritDoc} */
    @Override
    public State getState() {
        return getDelegate().getState();
    }

    /** {@inheritDoc} */
    @Override
    @Deprecated
    public Substate getSubstate() {
        return getDelegate().getSubstate();
    }

    /** {@inheritDoc} */
    @Override
    @Deprecated
    public S getValue() throws IllegalStateException {
        return getDelegate().getValue();
    }

    /** {@inheritDoc} */
    @Override
    @Deprecated
    public S awaitValue() throws IllegalStateException, InterruptedException {
        return getDelegate().awaitValue();
    }

    /** {@inheritDoc} */
    @Override
    @Deprecated
    public S awaitValue(final long time, final TimeUnit unit) throws IllegalStateException, InterruptedException, TimeoutException {
        return getDelegate().awaitValue(time, unit);
    }

    /** {@inheritDoc} */
    @Override
    public Service<S> getService() throws IllegalStateException {
        return getDelegate().getService();
    }

    /** {@inheritDoc} */
    @Override
    public ServiceName getName() {
        return getDelegate().getName();
    }

    /** {@inheritDoc} */
    @Override
    public ServiceName[] getAliases() {
        return getDelegate().getAliases();
    }

    /** {@inheritDoc} */
    @Override
    public void addListener(final LifecycleListener listener) {
        getDelegate().addListener(listener);
    }

    /** {@inheritDoc} */
    @Override
    @Deprecated
    public void addListener(final ServiceListener<? super S> listener) {
        getDelegate().addListener(listener);
    }

    /** {@inheritDoc} */
    @Override
    public void removeListener(final LifecycleListener listener) {
        getDelegate().removeListener(listener);
    }

    /** {@inheritDoc} */
    @Override
    @Deprecated
    public void removeListener(final ServiceListener<? super S> listener) {
        getDelegate().removeListener(listener);
    }

    /** {@inheritDoc} */
    @Override
    public StartException getStartException() {
        return getDelegate().getStartException();
    }

    /** {@inheritDoc} */
    @Override
    public void retry() {
        getDelegate().retry();
    }

    /** {@inheritDoc} */
    @Override
    public Collection<ServiceName> getUnavailableDependencies() {
        return getDelegate().getUnavailableDependencies();
    }

}
