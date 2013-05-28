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

import org.jboss.msc.service.*;

/**
 * A service context, which can be used to add new tasks and manipulate services, containers and registries
 * until transaction is marked for prepare.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
public interface ServiceContext {

    /**
     * Add a task with an executable component.  If the task implements any of the supplementary
     * interfaces {@link Revertible}, {@link Validatable}, or {@link Committable}, the corresponding
     * builder properties will be pre-initialized.
     *
     * @param task the task
     * @param <T> the result value type (may be {@link Void})
     * @return the builder for the task
     * @throws IllegalStateException if this context is not accepting new tasks
     */
    <T> TaskBuilder<T> newTask(Executable<T> task) throws IllegalStateException;

    /**
     * Add a task without an executable component.  All task components will be uninitialized.
     *
     * @return the builder for the task
     * @throws IllegalStateException if this context is not accepting new tasks
     */
    TaskBuilder<Void> newTask() throws IllegalStateException;

    /**
     * Create new service target builder.
     *
     * @return the service target builder
     * @throws IllegalStateException if this context is not accepting new service targets
     */
    ServiceTarget newServiceTarget() throws IllegalStateException;

    /**
     * Enables registry. As a result, its services may start, depending on their
     * {@link org.jboss.msc.service.ServiceMode mode} rules.
     * <p> Services are enabled by default.
     *
     * @param registry the service registry
     */
    void enableRegistry(ServiceRegistry registry);

    /**
     * Disables registry and all its services, causing {@code UP} services to stop.
     *
     * @param registry the service registry
     */
    void disableRegistry(ServiceRegistry registry);

    /**
     * Removes registry and its services from the {@code container}, causing {@code UP} services to stop.
     *
     * @param registry the service registry
     */
    void removeRegistry(ServiceRegistry registry);

    /**
     * Shutdown the container, removing all registries and their services.
     *
     * @param container the service container
     */
    void shutdownContainer(ServiceContainer container);

}
