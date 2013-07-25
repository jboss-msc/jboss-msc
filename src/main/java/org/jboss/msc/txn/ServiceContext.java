/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
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

package org.jboss.msc.txn;

import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceRegistry;

/**
 * A service context, which can be used to add new tasks, services and registries inside a transaction context. Can only
 * be used with active transactions, that have not been prepared to commit nor rolled back.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 * @author <a href="mailto:frainone@redhat.com">Flavia Rainone</a>
 */
public interface ServiceContext {

    /**
     * Adds a task with an executable component to {@code transaction}.  If the task implements any of the supplementary
     * interfaces {@link Revertible}, {@link Validatable}, or {@link Committable}, the corresponding
     * builder properties will be pre-initialized.
     * 
     * @param task        the task
     * @param transaction the transaction
     * @param <T> the result value type (may be {@link Void})
     * @return the builder for the task
     * @throws IllegalStateException if this context is not accepting new tasks
     */
    <T> TaskBuilder<T> newTask(Executable<T> task, Transaction transaction) throws IllegalStateException;

    /**
     * Adds a task without an executable component to {@code transaction}.  All task components will be uninitialized.
     * 
     * @param transaction  the transaction
     * @return the builder for the task
     * @throws IllegalStateException if this context is not accepting new tasks
     */
    TaskBuilder<Void> newTask(Transaction transaction) throws IllegalStateException;

    /**
     * Gets a builder which can be used to add a service to {@code registry}.
     *
     * @param registry    the target service registry where new service will be installed
     * @param name        the service name
     * @param transaction the transaction
     * @return the builder for the service
     */
    <T> ServiceBuilder<T> addService(ServiceRegistry registry, ServiceName name, Transaction transaction);

    
    /**
     * Removes a service, causing this service to stop if it is {@code UP}.
     *
     * @param registry    the service registry
     * @param name        the service name
     * @param transaction the transaction
     */
    void removeService(ServiceRegistry registry, ServiceName name, Transaction transaction);

    /**
     * Removes registry and its services from the {@code container}, causing {@code UP} services to stop.
     *
     * @param registry    the service registry
     * @param transaction the transaction
     */
    void removeRegistry(ServiceRegistry registry, Transaction transaction);

}
