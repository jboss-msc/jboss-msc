/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

package org.jboss.msc.service;

import org.jboss.msc.inject.Injector;
import org.jboss.msc.value.Value;

import java.util.Collection;

/**
 * A builder for an individual service in a {@code ServiceTarget}.  Create an instance via the
 * {@link ServiceTarget#addService(ServiceName, Service)}, 
 * {@link ServiceTarget#addServiceValue(ServiceName, Value)} or
 * methods.
 *
 * @param <T> the service type
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public interface ServiceBuilder<T> {

    /**
     * Add aliases for this service.
     *
     * @param aliases the service names to use as aliases
     * @return the builder
     */
    ServiceBuilder<T> addAliases(ServiceName... aliases);

    /**
     * Set the service definition location to be the caller's location.
     *
     * @return this builder
     */
    ServiceBuilder<T> setLocation();

    /**
     * Set the service definition location, if any.
     *
     * @param location the location
     * @return this builder
     */
    ServiceBuilder<T> setLocation(Location location);

    /**
     * Set the initial mode.
     *
     * @param mode the initial mode
     * @return this builder
     */
    ServiceBuilder<T> setInitialMode(ServiceController.Mode mode);

    /**
     * Add multiple, non-injected dependencies.
     *
     * @param dependencies the service names to depend on
     * @return this builder
     */
    ServiceBuilder<T> addDependencies(ServiceName... dependencies);

    /**
     * Add multiple, non-injected dependencies.
     *
     * @param dependencyType the dependency type; must not be {@code null}
     * @param dependencies the service names to depend on
     * @return this builder
     */
    ServiceBuilder<T> addDependencies(DependencyType dependencyType, ServiceName... dependencies);

    /**
     * Add multiple, optional, non-injected dependencies.  If the dependencies do not exist,
     * it will not stop the service from starting.
     *
     * @param dependencies the service names to depend on
     * @return this builder
     * @deprecated use {@link #addDependencies(DependencyType, ServiceName...)} instead
     */
    ServiceBuilder<T> addOptionalDependencies(ServiceName... dependencies);

    /**
     * Add multiple, non-injected dependencies.
     *
     * @param dependencies the service names to depend on
     * @return this builder
     */
    ServiceBuilder<T> addDependencies(Iterable<ServiceName> dependencies);

    /**
     * Add multiple, non-injected dependencies.
     *
     * @param dependencyType the dependency type; must not be {@code null}
     * @param dependencies the service names to depend on
     * @return this builder
     */
    ServiceBuilder<T> addDependencies(DependencyType dependencyType, Iterable<ServiceName> dependencies);

    /**
     * Add multiple, optional, non-injected dependencies.  If the dependencies do not exist,
     * it will not stop the service from starting.
     *
     * @param dependencies the service names to depend on
     * @return this builder
     * @deprecated use {@link #addDependencies(DependencyType, Iterable<ServiceName>)} instead
     */
    ServiceBuilder<T> addOptionalDependencies(Iterable<ServiceName> dependencies);

    /**
     * Add a dependency.  Calling this method multiple times for the same service name will only add it as a
     * dependency one time; however this may be useful to specify multiple injections for one dependency.
     *
     * @param dependency the name of the dependency
     * @return an injection builder for optionally injecting the dependency
     */
    ServiceBuilder<T> addDependency(ServiceName dependency);

    /**
     * Add a dependency.  Calling this method multiple times for the same service name will only add it as a
     * dependency one time; however this may be useful to specify multiple injections for one dependency.
     *
     * @param dependencyType the dependency type; must not be {@code null}
     * @param dependency the name of the dependency
     * @return an injection builder for optionally injecting the dependency
     */
    ServiceBuilder<T> addDependency(DependencyType dependencyType, ServiceName dependency);

    /**
     * Add an optional dependency.  Calling this method multiple times for the same service name will only add it as a
     * dependency one time; however this may be useful to specify multiple injections for one dependency.
     * If the dependencies do not exist, it will not stop the service from starting.
     *
     * @param dependency the name of the dependency
     * @return an injection builder for optionally injecting the dependency
     * @deprecated use {@link #addDependency(DependencyType, ServiceName)} instead
     */
    ServiceBuilder<T> addOptionalDependency(ServiceName dependency);

    /**
     * Add a service dependency.  Calling this method multiple times for the same service name will only add it as a
     * dependency one time; however this may be useful to specify multiple injections for one dependency.
     *
     * @param dependency the name of the dependency
     * @param target the injector into which the dependency should be stored
     * @return this builder
     */
    ServiceBuilder<T> addDependency(ServiceName dependency, Injector<Object> target);

    /**
     * Add a service dependency.  Calling this method multiple times for the same service name will only add it as a
     * dependency one time; however this may be useful to specify multiple injections for one dependency.
     *
     * @param dependencyType the dependency type; must not be {@code null}
     * @param dependency the name of the dependency
     * @param target the injector into which the dependency should be stored
     * @return this builder
     */
    ServiceBuilder<T> addDependency(DependencyType dependencyType, ServiceName dependency, Injector<Object> target);

    /**
     * Add an optional service dependency.  This will  Calling this method multiple times for the same service name will only add it as a
     * dependency one time; however this may be useful to specify multiple injections for one dependency.
     * If the dependencies do not exist, it will not stop the service from starting.
     *
     * @param dependency the name of the dependency
     * @param target the injector into which the dependency should be stored
     * @return this builder
     * @deprecated use {@link #addDependency(DependencyType, ServiceName, Injector<Object>)} instead
     */
    ServiceBuilder<T> addOptionalDependency(ServiceName dependency, Injector<Object> target);

    /**
     * Add a service dependency.  The type of the dependency is checked before it is passed into the (type-safe) injector
     * instance.  Calling this method multiple times for the same service name will only add it as a
     * dependency one time; however this may be useful to specify multiple injections for one dependency.
     *
     * @param dependency the name of the dependency
     * @param type the class of the value of the dependency
     * @param target the injector into which the dependency should be stored
     * @param <I> the type of the value of the dependency
     * @return this builder
     */
    <I> ServiceBuilder<T> addDependency(ServiceName dependency, Class<I> type, Injector<I> target);

    /**
     * Add a service dependency.  The type of the dependency is checked before it is passed into the (type-safe) injector
     * instance.  Calling this method multiple times for the same service name will only add it as a
     * dependency one time; however this may be useful to specify multiple injections for one dependency.
     *
     * @param dependencyType the dependency type; must not be {@code null}
     * @param dependency the name of the dependency
     * @param type the class of the value of the dependency
     * @param target the injector into which the dependency should be stored
     * @param <I> the type of the value of the dependency
     * @return this builder
     */
    <I> ServiceBuilder<T> addDependency(DependencyType dependencyType, ServiceName dependency, Class<I> type, Injector<I> target);

    /**
     * Add an optional service dependency.  The type of the dependency is checked before it is passed into the (type-safe) injector
     * instance.  Calling this method multiple times for the same service name will only add it as a
     * dependency one time; however this may be useful to specify multiple injections for one dependency.
     * If the dependencies do not exist, it will not stop the service from starting.
     *
     * @param dependency the name of the dependency
     * @param type the class of the value of the dependency
     * @param target the injector into which the dependency should be stored
     * @param <I> the type of the value of the dependency
     * @return this builder
     * @deprecated use {@link #addDependency(DependencyType, ServiceName, Class, Injector)} instead
     */
    <I> ServiceBuilder<T> addOptionalDependency(ServiceName dependency, Class<I> type, Injector<I> target);

    /**
     * Add an injection.  The given value will be injected into the given injector before service start, and uninjected
     * after service stop.
     *
     * @param target the injection target
     * @param value the injection value
     * @param <I> the injection type
     * @return this builder
     */
    <I> ServiceBuilder<T> addInjection(Injector<? super I> target, I value);

    /**
     * Add an injection value.  The given value will be injected into the given injector before service start, and uninjected
     * after service stop.
     *
     * @param target the injection target
     * @param value the injection value
     * @param <I> the injection type
     * @return this builder
     */
    <I> ServiceBuilder<T> addInjectionValue(Injector<? super I> target, Value<I> value);

    /**
     * Add an injection of this service into another target.  The given injector will be given this service upon
     * start, and uninjected when this service stops.
     *
     * @param target the injector target
     * @return this builder
     */
    ServiceBuilder<T> addInjection(Injector<? super T> target);

    /**
     * Add a service listener that will be added to this service.
     *
     * @param listener the listener to add to the service
     * @return this builder
     */
    ServiceBuilder<T> addListener(ServiceListener<? super T> listener);

    /**
     * Add service listeners that will be added to this service.
     *
     * @param listeners a list of listeners to add to the service
     * @return this builder
     */
    ServiceBuilder<T> addListener(ServiceListener<? super T>... listeners);

    /**
     * Add service listeners that will be added to this service.
     *
     * @param listeners a collection of listeners to add to the service
     * @return this builder
     */
    ServiceBuilder<T> addListener(Collection<? extends ServiceListener<? super T>> listeners);

    /**
     * Install the defined service into the container.
     *
     * @return the installed service controller
     * @throws ServiceRegistryException if installation fails
     * @throws IllegalStateException    if installation has been performed previously
     */
    ServiceController<T> install() throws ServiceRegistryException, IllegalStateException;

    /**
     * The dependency type.
     */
    enum DependencyType {

        /**
         * A required dependency.
         */
        REQUIRED,
        /**
         * An optional dependency.
         */
        OPTIONAL,
        ;
    }
}
