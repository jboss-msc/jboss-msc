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

import java.util.Collection;
import java.util.Set;

import org.jboss.msc.txn.TaskTarget;


/**
 * The target of ServiceBuilder installations.
 * ServicesBuilders to be installed on a target should be retrieved by calling one of the {@code addService} methods
 * ({@link #addService(ServiceName, Service)}.
 * Notice that installation will only take place after {@link ServiceBuilder#install(org.jboss.msc.txn.Transaction)} is
 * invoked. ServiceBuilders that are not installed are ignored.
 * 
 * @author <a href="mailto:frainone@redhat.com">Flavia Rainone</a>
 */
public interface ServiceTarget extends TaskTarget {

    /**
     * Gets a builder which can be used to add a service to this target.
     *
     * @param container the service container
     * @param name the service name
     * @param service the service
     * @return the builder for the service
     */
    <T> ServiceBuilder<T> addService(ServiceContainer container, ServiceName name, Service<T> service);

    /**
     * Adds a dependency that will be added to all ServiceBuilders installed in this target.
     *
     * @param dependency the dependency to add to the target
     * @return this target
     */
    ServiceTarget addDependency(ServiceName dependency);

    /**
     * Adds a list of dependencies that will be added to the all ServiceBuilders installed in this target.
     *
     * @param dependencies a list of dependencies to add to the target
     * @return this target
     */
    ServiceTarget addDependency(ServiceName... dependencies);

    /**
     * Adds a collection of dependencies that will be added to the all ServiceBuilders installed in this target
     *
     * @param dependencies a collection of dependencies to add to this target
     * @return this target
     */
    ServiceTarget addDependency(Collection<ServiceName> dependencies);

    /**
     * Adds a dependency that will be added to all ServiceBuilders installed in this target.
     *
     * @param dependency the dependency to add to the target
     * @param flags the flags for the dependency
     */
    public ServiceTarget addDependency(ServiceName dependency, DependencyFlag... flags);

    /**
     * Adds an injected dependency to the service being built.  The dependency will be injected just before
     * starting this service and uninjected just before stopping it.
     *
     * @param name the service name
     * @param injector the injector for the dependency value
     */
    public void addDependency(ServiceContainer container, ServiceName name, Injector<?> injector);

    /**
     * Adds an injected dependency to the service being built.  The dependency will be injected just before
     * starting this service and uninjected just before stopping it.
     *
     * @param name the service name
     * @param injector the injector for the dependency value
     */
    public void addDependency(ServiceName name, Injector<?> injector);

    /**
     * Adds an injected dependency to the service being built.  The dependency will be injected just before starting this
     * service and uninjected just before stopping it.
     *
     * @param name the service name
     * @param injector the injector for the dependency value
     * @param flags the flags for the service
     */
    public <T> void addDependency(ServiceContainer container, ServiceName name, Injector<T> injector, DependencyFlag... flags);

    /**
     * Adds an injected dependency to the service being built.  The dependency will be injected just before starting this
     * service and uninjected just before stopping it.
     *
     * @param name the service name
     * @param injector the injector for the dependency value
     * @param flags the flags for the service
     */
    public <T> void addDependency(ServiceName name, Injector<T> injector, DependencyFlag... flags);

    /**
     * Remove all dependencies from this target.
     *
     * @return this target
     */
    ServiceTarget clearDependencies();

    /**
     * Removes a dependency from this target.  Subsequently defined services will not have this dependency.
     *
     * @param dependency the dependency
     * @return this target
     */
    ServiceTarget removeDependency(ServiceName dependency);

    /**
     * Returns a set of all dependencies added to this target.
     * 
     * @return all dependencies of this target
     */
    Set<ServiceName> getDependencies();

    /**
     * Creates a sub-target using this as the parent target.
     *
     * @return the new service target
     */
    ServiceTarget subTarget();
}
