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

package org.jboss.msc.service;

import org.jboss.msc.txn.TaskController;

/**
 * A service builder.
 * Implementations of this interface are not thread safe.
 *
 * @author <a href="mailto:frainone@redhat.com">Flavia Rainone</a>
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
public interface ServiceBuilder<T> {

    /**
     * Sets the service mode.
     *
     * @param mode the service mode
     * @return a reference to this object
     * @throws IllegalStateException if {@link #install()} has been called.
     */
    ServiceBuilder<T> setMode(ServiceMode mode) throws IllegalStateException;

    /**
     * Add aliases for the service.
     *
     * @param aliases the dependency names to use as aliases
     * @return a reference to this object
     * @throws IllegalStateException if {@link #install()} has been called.
     */
    ServiceBuilder<T> addAliases(ServiceName... aliases) throws IllegalStateException;

    /**
     * Adds a dependency to the service being built with default flags.
     *
     * @param name the dependency name
     * @return a reference to this object
     * @throws IllegalStateException if {@link #install()} has been called.
     */
    ServiceBuilder<T> addDependency(ServiceName name) throws IllegalStateException;

    /**
     * Adds a dependency to the service being built with specified flags.
     *
     * @param name the dependency name
     * @param flags the flags for the service dependency
     * @return a reference to this object
     * @throws IllegalStateException if {@link #install()} has been called.
     */
    ServiceBuilder<T> addDependency(ServiceName name, DependencyFlag... flags) throws IllegalStateException;

    /**
     * Adds an injected dependency to the service being built with default flags.
     * The dependency will be injected before service starts and uninjected after service stops.
     *
     * @param name the dependency name
     * @param injector the injector for the dependency value
     * @return a reference to this object
     * @throws IllegalStateException if {@link #install()} has been called.
     */
    ServiceBuilder<T> addDependency(ServiceName name, Injector<?> injector) throws IllegalStateException;

    /**
     * Adds an injected dependency to the service being built with specified flags.
     * The dependency will be injected before service starts and uninjected after service stops.
     *
     * @param name the dependency name
     * @param injector the injector for the dependency value
     * @param flags the flags for the service dependency
     * @return a reference to this object
     * @throws IllegalStateException if {@link #install()} has been called.
     */
    ServiceBuilder<T> addDependency(ServiceName name, Injector<?> injector, DependencyFlag... flags) throws IllegalStateException;

    /**
     * Adds a dependency to the service being built with default flags.
     *
     * @param container the service container containing dependency
     * @param name the dependency name
     * @return a reference to this object
     * @throws IllegalStateException if {@link #install()} has been called.
     */
    ServiceBuilder<T> addDependency(ServiceContainer container, ServiceName name) throws IllegalStateException;
    
    /**
     * Adds a dependency to the service being built with specified flags.
     *
     * @param container the service container containing dependency
     * @param name the dependency name
     * @param flags the flags for the service dependency
     * @return a reference to this object
     * @throws IllegalStateException if {@link #install()} has been called.
     */
    ServiceBuilder<T> addDependency(ServiceContainer container, ServiceName name, DependencyFlag... flags) throws IllegalStateException;

    /**
     * Adds an injected dependency to the service being built with default flags.
     * The dependency will be injected before service starts and uninjected after service stops.
     *
     * @param container the service container containing dependency
     * @param name the dependency name
     * @param injector the injector for the dependency value
     * @return a reference to this object
     * @throws IllegalStateException if {@link #install()} has been called.
     */
    ServiceBuilder<T> addDependency(ServiceContainer container, ServiceName name, Injector<?> injector) throws IllegalStateException;

    /**
     * Adds an injected dependency to the service being built with specified flags.
     * The dependency will be injected before service starts and uninjected after service stops.
     *
     * @param container the service container containing dependency
     * @param name the dependency name
     * @param injector the injector for the dependency value
     * @param flags the flags for the service dependency
     * @return a reference to this object
     * @throws IllegalStateException if {@link #install()} has been called.
     */
    ServiceBuilder<T> addDependency(ServiceContainer container, ServiceName name, Injector<?> injector, DependencyFlag... flags) throws IllegalStateException;

    /**
     * Adds a dependency on a task.  If the task fails, the service install will also fail.  The task must be
     * part of the same transaction as the service.
     *
     * @param task the task
     * @return a reference to this object
     * @throws IllegalStateException if {@link #install()} has been called.
     */
    ServiceBuilder<T> addDependency(TaskController<?> task) throws IllegalStateException;

    /**
     * Initiates installation of this configured service to the container.
     *
     * @throws IllegalStateException if {@link #install()} has been already called.
     */
    void install() throws IllegalStateException;

}
