/*
 * JBoss, Home of Professional Open Source
 *
 * Copyright 2013 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.msc.service;

import org.jboss.msc.txn.Transaction;

/**
 * A management context for enabling and disabling services and registries. It also allows managers to shutdown the
 * container.
 * All operations are performed under a transaction, which must be provided.
 * 
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 * @author <a href="mailto:frainone@redhat.com">Flavia Rainone</a>
 *
 */
public interface ManagementContext {
    
    /**
     * Disables a service, causing this service to stop if it is {@code UP}.
     *
     * @param registry    the service registry
     * @param name        the service name
     * @param transaction the transaction
     */
    void disableService(ServiceRegistry registry, ServiceName name, Transaction transaction);

    /**
     * Enables the service, which may start as a result, according to its {@link org.jboss.msc.service.ServiceMode mode} rules.
     * <p> Services are enabled by default.
     *
     * @param registry    the service registry
     * @param name        the service name
     * @param transaction the transaction
     */
    void enableService(ServiceRegistry registry, ServiceName name, Transaction transaction);

    /**
     * Disables {@code registry} and all its services, causing {@code UP} services to stop.
     *
     * @param registry    the service registry
     * @param transaction the transaction
     */
    void disableRegistry(ServiceRegistry registry, Transaction transaction);

    /**
     * Enables {@code registry}. As a result, its services may start, depending on their
     * {@link org.jboss.msc.service.ServiceMode mode} rules.
     * <p> Registries are enabled by default.
     *
     * @param registry    the service registry
     * @param transaction the transaction
     */
    void enableRegistry(ServiceRegistry registry, Transaction transaction);

    /**
     * Shuts down the container, removing all registries and their services.
     *
     * @param container   the service container
     * @param transaction the transaction
     */
    void shutdownContainer(ServiceContainer container, Transaction transaction);

}
