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
import org.jboss.msc.service.ServiceContainer;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public interface ServiceRegistry {

    /**
     * Get a new batch builder, which is used to resolve and install described services.
     *
     * @return the new batch builder
     */
    BatchBuilder batchBuilder();

    /**
     * A batch builder for installing service definitions in a single action.
     */
    interface BatchBuilder {

        /**
         * Install all the defined services into the container.
         *
         * @throws ServiceRegistryException
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

    /**
     * The factory used to create instances.
     */
    class Factory {

        private Factory() {
        }

        /**
         * Create a new service registry instance.
         *
         * @param container the associated container
         * @return the new registry
         */
        public static ServiceRegistry create(ServiceContainer container) {
            return new ServiceRegistryImpl(container);
        }
    }
}
