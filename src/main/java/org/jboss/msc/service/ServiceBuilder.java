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
     * @return a reference to the dependency added
     * @throws IllegalStateException if {@link #install()} has been called.
     */
    Dependency<?> addDependency(ServiceName name) throws IllegalStateException;

    /**
     * Adds a dependency to the service being built with specified flags.
     *
     * @param name the dependency name
     * @param flags the flags for the service dependency
     * @return a reference to the dependency added
     * @throws IllegalStateException if {@link #install()} has been called.
     */
    Dependency<?> addDependency(ServiceName name, DependencyFlag... flags) throws IllegalStateException;

    /**
     * Adds a dependency to the service being built with default flags.
     *
     * @param registry the service registry containing dependency
     * @param name the dependency name
     * @return a reference to the dependency added
     * @throws IllegalStateException if {@link #install()} has been called.
     */
    Dependency<?> addDependency(ServiceRegistry registry, ServiceName name) throws IllegalStateException;
    
    /**
     * Adds a dependency to the service being built with specified flags.
     *
     * @param registry the service registry containing dependency
     * @param name the dependency name
     * @param flags the flags for the service dependency
     * @return a reference to the dependency added
     * @throws IllegalStateException if {@link #install()} has been called.
     */
    Dependency<?> addDependency(ServiceRegistry registry, ServiceName name, DependencyFlag... flags) throws IllegalStateException;

    /**
     * Initiates installation of this configured service to the container.
     */
    void install() throws IllegalStateException;

}
