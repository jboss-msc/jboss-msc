/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat Middleware LLC, and individual contributors
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

import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.jboss.msc.value.Value;

final class ServiceContainerImpl extends Dependable<ServiceContainer> implements ServiceContainer {
    final Object lock = new Object();

    private static final class ExecutorHolder {
        private static final Executor VALUE;

        static {
            final ThreadPoolExecutor executor = new ThreadPoolExecutor(0, 1, 30L, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());
            executor.allowCoreThreadTimeOut(true);
            executor.setCorePoolSize(1);
            VALUE = executor;
        }

        private ExecutorHolder() {
        }
    }

    private volatile Executor executor;

    ServiceContainerImpl() {
    }

    public <T> ServiceBuilder<T> buildService(final Value<? extends Service> service, final Value<T> value) throws IllegalArgumentException {
        return new ServiceBuilderImpl<T>(this, service, value);
    }

    public <S extends Service> ServiceBuilder<S> buildService(final Value<S> service) throws IllegalArgumentException {
        return null;
    }

    public void setExecutor(final Executor executor) {
        this.executor = executor;
    }

    Executor getExecutor() {
        final Executor executor = this.executor;
        return executor != null ? executor : ExecutorHolder.VALUE;
    }

    public List<ServiceController<?>> getFailedServices() {
        return null;
    }

    void addDemand() {
    }

    void removeDemand() {
    }

    void dependentStarted() {
    }

    void dependentStopped() {
    }
}
