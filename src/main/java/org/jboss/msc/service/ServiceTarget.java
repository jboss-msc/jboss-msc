/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013 Red Hat, Inc., and individual contributors
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
public interface ServiceTarget {

    /**
     * Gets a builder which can be used to add a service to this target.
     *
     * @param registry the target service registry where new service will be installed
     * @param name the service name
     * @param service the service
     * @return the builder for the service
     */
    <T> ServiceBuilder<T> addService(ServiceRegistry registry, ServiceName name, Service<T> service);

    /**
     * Adds a dependency that will be added to all ServiceBuilders installed in this target.
     *
     * @param dependency the dependency to add to the target
     * @return this target
     */
    ServiceTarget addDependency(ServiceName dependency);

    /**
     * Adds a dependency that will be added to all ServiceBuilders installed in this target.
     *
     * @param dependency the dependency to add to the target
     * @param flags the flags for the dependency
     * @return this target
     */
    ServiceTarget addDependency(ServiceName dependency, DependencyFlag... flags);

    /**
     * Adds a dependency that will be added to all ServiceBuilders installed in this target.
     *
     * @param registry the service registry containing dependency
     * @param dependency the dependency to add to the target
     * @return this target
     */
    ServiceTarget addDependency(ServiceRegistry registry, ServiceName dependency);

    /**
     * Adds a dependency that will be added to all ServiceBuilders installed in this target.
     *
     * @param registry the service registry containing dependency
     * @param dependency the dependency to add to the target
     * @param flags the flags for the dependency
     * @return this target
     */
    ServiceTarget addDependency(ServiceRegistry registry, ServiceName dependency, DependencyFlag... flags);

    /**
     * Adds a dependency on a task.  If the task fails, the service install will also fail.  The task must be
     * part of the same transaction as the service.
     *
     * @param dependency the dependency task
     * @return this target
     */
    ServiceTarget addDependency(TaskController<?> dependency);

    /**
     * Removes a dependency from this target. Subsequently defined services will not have this dependency.
     *
     * @param dependency the dependency
     * @return this target
     */
    ServiceTarget removeDependency(ServiceName dependency);

    /**
     * Removes a dependency from this target. Subsequently defined services will not have this dependency.
     *
     * @param registry the service registry containing dependency
     * @param dependency the dependency
     * @return this target
     */
    ServiceTarget removeDependency(ServiceRegistry registry, ServiceName dependency);

    /**
     * Removes a dependency from this target. Subsequently defined services will not have this dependency.
     *
     * @param dependency the dependency
     * @return this target
     */
    ServiceTarget removeDependency(TaskController<?> dependency);

    /**
     * Creates a sub-target using this as the parent target.
     *
     * @return the new child service target
     */
    ServiceTarget subTarget();

}
