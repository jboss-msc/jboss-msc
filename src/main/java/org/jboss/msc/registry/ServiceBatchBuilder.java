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

package org.jboss.msc.registry;

import java.util.Collection;

/**
 * A batch builder for installing service definitions in a single action.
 */
public interface ServiceBatchBuilder {

    /**
     * Install all the defined services into the container.
     *
     * @throws org.jboss.msc.registry.ServiceRegistryException
     */
    void install() throws ServiceRegistryException;

    /**
     * Add a service definition to the batch.
     *
     * @param definition
     * @return this batch
     */
    BatchBuilderImpl add(ServiceDefinition definition);

    /**
     * Add a list of service batchEntries to the batch, in the order of the list.
     *
     * @param definitions add a list of service batchEntries to the batch, in the
     *        order of the list
     * @return this batch
     */
    BatchBuilderImpl add(ServiceDefinition<?>... definitions);

    /**
     * Add a collection of service batchEntries to the batch, in the order of the
     * collection (if ordered).
     *
     * @param definitions add a list of service batchEntries to the batch, in the
     *        order of the list
     * @return this batch
     */
    BatchBuilderImpl add(Collection<ServiceDefinition<?>> definitions);
}
