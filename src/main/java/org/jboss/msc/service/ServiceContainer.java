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
import org.jboss.msc.value.Value;

/**
 *
 */
public interface ServiceContainer extends Service {
    void start(StartContext context) throws StartException;

    void stop(StopContext context);

    /**
     * Set the container executor.  If {@code null} is specified, a default single-thread executor is used.
     *
     * @param executor the executor to use
     */
    void setExecutor(Executor executor);


    <S extends Service> ServiceBuilder<S> buildService(Value<S> service) throws IllegalArgumentException;

    /**
     * Build a service whose public type is different from the service type.
     *
     * @param service the service itself
     * @param value the public service value
     * @param <T> the public service value
     * @return a service builder
     * @throws IllegalArgumentException if another service is already registered with the given identifier
     */
    <T> ServiceBuilder<T> buildService(Value<? extends Service> service, Value<T> value) throws IllegalArgumentException;

    /**
     * Get a point-in-time snapshot of all failed services.
     *
     * @return the list of services that are in a failed state
     */
    List<ServiceController<?>> getFailedServices();
}
