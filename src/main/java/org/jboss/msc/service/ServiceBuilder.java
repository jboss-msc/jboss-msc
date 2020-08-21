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
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Builder to configure service before installing it into the container.
 * <p>
 * Service may require multiple dependencies (named values) to be satisfied before starting.
 * Every dependency requirement must be specified via {@link #requires(ServiceName)} method.
 * <p>
 * Single service can provide multiple values which can be requested by dependent services.
 * Every named value service provides must be specified via {@link #provides(ServiceName...)} method.
 * <p>
 * Once all required and provided dependencies are defined, references to all {@link Consumer}s
 * and {@link Supplier}s should be passed to service instance so they can be accessed by service
 * at runtime.
 * <p>
 * Implementations of this interface are not thread safe.
 *
 * @param <T> service value type if service provides single value
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
public interface ServiceBuilder<T> {

    /**
     * Specifies dependency value required by service. There can be multiple dependencies service may depend on.
     *
     * @param name required dependency name
     * @param <V> required dependency value type
     * @return readonly dependency reference
     * @throws IllegalStateException if this method
     * have been called after {@link #setInstance(org.jboss.msc.Service)} method.
     * @throws UnsupportedOperationException if this service builder
     * wasn't created via {@link ServiceTarget#addService(ServiceName)} method.
     */
    <V> Supplier<V> requires(ServiceName name);

    /**
     * Specifies injector provided by service. There can be multiple injectors service may provide.
     *
     * @param names provided dependency names
     * @param <V> provided dependency value type
     * @return writable dependency reference
     * @throws IllegalStateException if this method
     * have been called after {@link #setInstance(org.jboss.msc.Service)} method.
     * @throws UnsupportedOperationException if this service builder
     * wasn't created via {@link ServiceTarget#addService(ServiceName)} method.
     */
    <V> Consumer<V> provides(ServiceName... names);

    /**
     * Sets initial service mode.
     *
     * @param mode initial service mode
     * @return this builder
     */
    ServiceBuilder<T> setInitialMode(ServiceController.Mode mode);

    /**
     * Sets service instance. If {@link #install()} method call is issued
     * without this method being called then <code>NULL</code> service will be
     * installed into the container.
     * <p>
     * When this method have been called then all subsequent
     * calls of {@link #requires(ServiceName)}, and {@link #provides(ServiceName...)}
     * methods will fail.
     *
     * @param service the service instance
     * @return this configurator
     */
    ServiceBuilder<T> setInstance(org.jboss.msc.Service service);

    /**
     * Adds a service listener to be added to the service.
     *
     * @param listener the listener to add to the service
     * @return this builder
     */
    ServiceBuilder<T> addListener(LifecycleListener listener);

    /**
     * Installs configured service into the container.
     *
     * @return installed service controller
     * @throws ServiceRegistryException if installation fails for any reason
     */
    ServiceController<T> install();

    ////////////////////////
    // DEPRECATED METHODS //
    ////////////////////////

    /**
     * Adds aliases for this service.
     *
     * @param aliases the service names to use as aliases
     * @return the builder
     * @throws UnsupportedOperationException if this service builder
     * wasn't created via {@link ServiceTarget#addService(ServiceName)} method.
     * @deprecated Use {@link #provides(ServiceName...)} instead.
     * This method will be removed in a future release.
     */
    @Deprecated
    ServiceBuilder<T> addAliases(ServiceName... aliases);

    /**
     * Add multiple, non-injected dependencies.
     *
     * @param dependencies the service names to depend on
     * @return this builder
     * @throws UnsupportedOperationException if this service builder
     * was created via {@link ServiceTarget#addService(ServiceName)} method.
     * @deprecated Use {@link #requires(ServiceName)} instead.
     * This method will be removed in a future release.
     */
    @Deprecated
    ServiceBuilder<T> addDependencies(ServiceName... dependencies);

    /**
     * Add multiple, non-injected dependencies.
     *
     * @param dependencyType the dependency type; must not be {@code null}
     * @param dependencies the service names to depend on
     * @return this builder
     * @throws UnsupportedOperationException if this service builder
     * was created via {@link ServiceTarget#addService(ServiceName)} method.
     * @deprecated Optional dependencies are <em>unsafe</em> and should not be used.
     * This method will be removed in a future release.
     */
    @Deprecated
    ServiceBuilder<T> addDependencies(DependencyType dependencyType, ServiceName... dependencies);

    /**
     * Add multiple, non-injected dependencies.
     *
     * @param dependencies the service names to depend on
     * @return this builder
     * @throws UnsupportedOperationException if this service builder
     * was created via {@link ServiceTarget#addService(ServiceName)} method.
     * @deprecated Use {@link #requires(ServiceName)} instead.
     * This method will be removed in a future release.
     */
    @Deprecated
    ServiceBuilder<T> addDependencies(Iterable<ServiceName> dependencies);

    /**
     * Add multiple, non-injected dependencies.
     *
     * @param dependencyType the dependency type; must not be {@code null}
     * @param dependencies the service names to depend on
     * @return this builder
     * @throws UnsupportedOperationException if this service builder
     * was created via {@link ServiceTarget#addService(ServiceName)} method.
     * @deprecated Optional dependencies are <em>unsafe</em> and should not be used.
     * This method will be removed in a future release.
     */
    @Deprecated
    ServiceBuilder<T> addDependencies(DependencyType dependencyType, Iterable<ServiceName> dependencies);

    /**
     * Add a dependency.  Calling this method multiple times for the same service name will only add it as a
     * dependency one time; however this may be useful to specify multiple injections for one dependency.
     *
     * @param dependency the name of the dependency
     * @return an injection builder for optionally injecting the dependency
     * @throws UnsupportedOperationException if this service builder
     * was created via {@link ServiceTarget#addService(ServiceName)} method.
     * @deprecated Use {@link #requires(ServiceName)} instead.
     * This method will be removed in a future release.
     */
    @Deprecated
    ServiceBuilder<T> addDependency(ServiceName dependency);

    /**
     * Add a dependency.  Calling this method multiple times for the same service name will only add it as a
     * dependency one time; however this may be useful to specify multiple injections for one dependency.
     *
     * @param dependencyType the dependency type; must not be {@code null}
     * @param dependency the name of the dependency
     * @return an injection builder for optionally injecting the dependency
     * @throws UnsupportedOperationException if this service builder
     * was created via {@link ServiceTarget#addService(ServiceName)} method.
     * @deprecated Optional dependencies are <em>unsafe</em> and should not be used.
     * This method will be removed in a future release.
     */
    @Deprecated
    ServiceBuilder<T> addDependency(DependencyType dependencyType, ServiceName dependency);

    /**
     * Add a service dependency.  Calling this method multiple times for the same service name will only add it as a
     * dependency one time; however this may be useful to specify multiple injections for one dependency.
     *
     * @param dependency the name of the dependency
     * @param target the injector into which the dependency should be stored
     * @return this builder
     * @throws UnsupportedOperationException if this service builder
     * was created via {@link ServiceTarget#addService(ServiceName)} method.
     * @deprecated Use {@link #requires(ServiceName)} instead.
     * This method will be removed in a future release.
     */
    @Deprecated
    ServiceBuilder<T> addDependency(ServiceName dependency, Injector<Object> target);

    /**
     * Add a service dependency.  Calling this method multiple times for the same service name will only add it as a
     * dependency one time; however this may be useful to specify multiple injections for one dependency.
     *
     * @param dependencyType the dependency type; must not be {@code null}
     * @param dependency the name of the dependency
     * @param target the injector into which the dependency should be stored
     * @return this builder
     * @throws UnsupportedOperationException if this service builder
     * was created via {@link ServiceTarget#addService(ServiceName)} method.
     * @deprecated Optional dependencies are <em>unsafe</em> and should not be used.
     * This method will be removed in a future release.
     */
    @Deprecated
    ServiceBuilder<T> addDependency(DependencyType dependencyType, ServiceName dependency, Injector<Object> target);

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
     * @throws UnsupportedOperationException if this service builder
     * was created via {@link ServiceTarget#addService(ServiceName)} method.
     * @deprecated Use {@link #requires(ServiceName)} instead.
     * This method will be removed in a future release.
     */
    @Deprecated
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
     * @throws UnsupportedOperationException if this service builder
     * was created via {@link ServiceTarget#addService(ServiceName)} method.
     * @deprecated Optional dependencies are <em>unsafe</em> and should not be used.
     * This method will be removed in a future release.
     */
    @Deprecated
    <I> ServiceBuilder<T> addDependency(DependencyType dependencyType, ServiceName dependency, Class<I> type, Injector<I> target);

    /**
     * Add an injection.  The given value will be injected into the given injector before service start, and uninjected
     * after service stop.
     *
     * @param target the injection target
     * @param value the injection value
     * @param <I> the injection type
     * @return this builder
     * @throws UnsupportedOperationException if this service builder
     * was created via {@link ServiceTarget#addService(ServiceName)} method.
     * @deprecated Use {@link #requires(ServiceName)} instead.
     * This method will be removed in a future release.
     */
    @Deprecated
    <I> ServiceBuilder<T> addInjection(Injector<? super I> target, I value);

    /**
     * Add an injection value.  The given value will be injected into the given injector before service start, and uninjected
     * after service stop.
     *
     * @param target the injection target
     * @param value the injection value
     * @param <I> the injection type
     * @return this builder
     * @throws UnsupportedOperationException if this service builder
     * was created via {@link ServiceTarget#addService(ServiceName)} method.
     * @deprecated Use {@link #requires(ServiceName)} instead.
     * This method will be removed in a future release.
     */
    @Deprecated
    <I> ServiceBuilder<T> addInjectionValue(Injector<? super I> target, Value<I> value);

    /**
     * Add an injection of this service into another target.  The given injector will be given this service after
     * start, and uninjected when this service stops.
     * <p> Differently from other injection types, failures to perform an outward injection will not result in a failure
     * to start the service.
     *
     * @param target the injector target
     * @return this builder
     * @throws UnsupportedOperationException if this service builder
     * was created via {@link ServiceTarget#addService(ServiceName)} method.
     * @deprecated Use {@link #provides(ServiceName...)} instead.
     * This method will be removed in a future release.
     */
    @Deprecated
    ServiceBuilder<T> addInjection(Injector<? super T> target);

    /**
     * Adds a stability monitor to be added to the service.
     *
     * @param monitor the monitor to add to the service
     * @return this builder
     * @deprecated Stability monitors are unreliable - do not use them.
     * This method will be removed in a future release.
     */
    @Deprecated
    ServiceBuilder<T> addMonitor(StabilityMonitor monitor);

    /**
     * Add service stability monitors that will be added to this service.
     * 
     * @param monitors a list of stability monitors to add to the service
     * @return this builder
     * @throws UnsupportedOperationException if this service builder
     * was created via {@link ServiceTarget#addService(ServiceName)} method.
     * @deprecated Stability monitors are unreliable - do not use them.
     * This method will be removed in a future release.
     */
    @Deprecated
    ServiceBuilder<T> addMonitors(final StabilityMonitor... monitors);

    /**
     * Add a service listener that will be added to this service.
     *
     * @param listener the listener to add to the service
     * @return this builder
     * @throws UnsupportedOperationException if this service builder
     * was created via {@link ServiceTarget#addService(ServiceName)} method.
     * @deprecated Use {@link #addListener(LifecycleListener)} instead.
     * This method will be removed in a future release.
     */
    @Deprecated
    ServiceBuilder<T> addListener(ServiceListener<? super T> listener);

    /**
     * Add service listeners that will be added to this service.
     *
     * @param listeners a list of listeners to add to the service
     * @return this builder
     * @throws UnsupportedOperationException if this service builder
     * was created via {@link ServiceTarget#addService(ServiceName)} method.
     * @deprecated Use {@link #addListener(LifecycleListener)} instead.
     * This method will be removed in a future release.
     */
    @Deprecated
    ServiceBuilder<T> addListener(ServiceListener<? super T>... listeners);

    /**
     * Add service listeners that will be added to this service.
     *
     * @param listeners a collection of listeners to add to the service
     * @return this builder
     * @throws UnsupportedOperationException if this service builder
     * was created via {@link ServiceTarget#addService(ServiceName)} method.
     * @deprecated Use {@link #addListener(LifecycleListener)} instead.
     * This method will be removed in a future release.
     */
    @Deprecated
    ServiceBuilder<T> addListener(Collection<? extends ServiceListener<? super T>> listeners);

    /**
     * The dependency type.
     *
     * @deprecated Optional dependencies are <em>unsafe</em> and should not be used.
     * This enum will be removed in a future release.
     */
    @Deprecated
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
