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

import java.util.Collection;
import org.jboss.msc.value.Value;
import org.jboss.msc.value.Values;

/**
 * A batch builder for installing service definitions in a single action.  Create an instance via the
 * {@link ServiceContainer#batchBuilder()} method.
 */
public interface BatchBuilder {

    /**
     * Install all the defined services into the container.
     *
     * @throws ServiceRegistryException
     */
    void install() throws ServiceRegistryException;

    /**
     * Get a builder which can be used to add a service to this batch.  The provided value should return the same
     * result every time (see {@link Values#cached(Value)} for more information).
     *
     * @param name the service name
     * @param value the service value
     * @return the builder for the service
     */
    <T> BatchServiceBuilder<T> addServiceValue(ServiceName name, Value<? extends Service<T>> value) throws DuplicateServiceException;

    /**
     * Get a builder which can be used to add a service to this batch.
     *
     * @param name the service name
     * @param service the service
     * @return the builder for the service
     */
    <T> BatchServiceBuilder<T> addService(ServiceName name, Service<T> service) throws DuplicateServiceException;

    /**
     * Add a service listener that will be added to the all the ServiceDefinitions in the batch.
     *
     * @param listener the listener to add to the batch
     * @return this batch
     */
    BatchBuilder addListener(ServiceListener<Object> listener);

    /**
     * Add a list of service listener that will be added to the all the ServiceDefinitions in the batch.
     *
     * @param listeners a list of listeners to add to the batch
     * @return this batch
     */
    BatchBuilder addListener(ServiceListener<Object>... listeners);

    /**
     * Add a collection of service listener that will be added to the all the ServiceDefinitions in the batch.
     *
     * @param listeners a collection of listeners to add to the batch
     * @return this batch
     */
    BatchBuilder addListener(Collection<ServiceListener<Object>> listeners);
    
}
