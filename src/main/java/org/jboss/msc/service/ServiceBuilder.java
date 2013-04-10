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

import java.util.concurrent.Future;

/**
 * A service builder.
 * The implementations of this interface are not thread safe.
 *
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
public interface ServiceBuilder<T extends Service> {

    /**
     * Sets the service mode.
     *
     * @param mode the service mode
     * @return a reference to this object
     * @throws IllegalStateException if {@link #build()} have been called.
     */
    ServiceBuilder<T> setMode(ServiceMode mode) throws IllegalStateException;

    /**
     * Adds alias for the service.
     *
     * @param alias the service name to use as alias
     * @return a reference to this object
     * @throws IllegalStateException if {@link #build()} have been called.
     */
    ServiceBuilder<T> addAlias(ServiceName alias) throws IllegalStateException;

    /**
     * Add aliases for the service.
     *
     * @param aliases the service names to use as aliases
     * @return a reference to this object
     * @throws IllegalStateException if {@link #build()} have been called.
     */
    ServiceBuilder<T> addAliases(ServiceName... aliases) throws IllegalStateException;

    /**
     * Adds a dependency to the service being built with default flags.
     *
     * @param serviceName the service name
     * @return a reference to this object
     * @throws IllegalStateException if {@link #build()} have been called.
     */
    ServiceBuilder<T> addDependency(ServiceName serviceName) throws IllegalStateException;

    /**
     * Adds a dependency to the service being built with specified flags.
     *
     * @param serviceName the service name
     * @param flags the flags for the service dependency
     * @return a reference to this object
     * @throws IllegalStateException if {@link #build()} have been called.
     */
    ServiceBuilder<T> addDependency(ServiceName serviceName, DependencyFlag... flags) throws IllegalStateException;

    /**
     * Adds an injected dependency to the service being built with default flags.
     * The dependency will be injected before service starts and uninjected after service stops.
     *
     * @param serviceName the service name
     * @param injector the injector for the dependency value
     * @return a reference to this object
     * @throws IllegalStateException if {@link #build()} have been called.
     */
    ServiceBuilder<T> addDependency(ServiceName serviceName, Injector<?> injector) throws IllegalStateException;

    /**
     * Add an injected dependency to the service being built with specified flags.
     * The dependency will be injected before service starts and uninjected after service stops.
     *
     * @param serviceName the service name
     * @param injector the injector for the dependency value
     * @param flags the flags for the service dependency
     * @return a reference to this object
     * @throws IllegalStateException if {@link #build()} have been called.
     */
    ServiceBuilder<T> addDependency(ServiceName serviceName, Injector<?> injector, DependencyFlag... flags) throws IllegalStateException;

    // TODO: is this build() throws A,B,C exceptions correct? Shouldn't we use either transaction.addProblem() or Future's ExecuteException to propagate A,B,C exceptions?
    /**
     * Initiate installation of this configured service to the container.
     *
     * @return a reference to the service future
     * @throws CircularDependencyException if dependencies cycle have been detected
     * @throws DuplicateServiceException if service with specified name is already installed in the container.
     * @throws IllegalStateException if {@link #build()} have been already called.
     */
    Future<T> build() throws CircularDependencyException, DuplicateServiceException, IllegalStateException;
}
