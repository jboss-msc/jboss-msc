/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2021, Red Hat, Inc., and individual contributors
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

import java.util.ConcurrentModificationException;
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
 * Implementations of this interface are thread safe because they rely on thread confinement.
 * The builder instance can be used only by thread that created it.
 *
 * @param <T> service value type if service provides single value
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
public interface ServiceBuilder<T> {

    /**
     * Specifies value name required by service. There can be multiple values service may depend on.
     *
     * @param name required dependency name
     * @param <V> required dependency value type
     * @return readonly dependency reference
     * @throws ConcurrentModificationException if builder is shared between threads.
     * Only thread that created the builder can manipulate it.
     * @throws IllegalArgumentException if value <code>name</code> was before used as parameter either in
     * {@link ServiceTarget#addService(ServiceName)} method when creating this builder instance or
     * in {@link #provides(ServiceName...)} method call. Value can be either required or provided but not both.
     * @throws IllegalStateException if this method have been called after {@link #install()} method.
     * @throws NullPointerException if <code>name</code> parameter is null.
     */
    <V> Supplier<V> requires(ServiceName name);

    /**
     * Specifies value provided by service. There can be multiple names for the same value.
     * At least one <code>name</code> parameter must be provided to this method. If there are more <code>names</code>
     * in the vararg array then the first one is called provided value name and other are called provided value aliases.
     *
     * @param names provided value name (and its aliases)
     * @param <V> provided value type
     * @return writable dependency reference
     * @throws ConcurrentModificationException if builder is shared between threads.
     * Only thread that created the builder can manipulate it.
     * @throws IllegalArgumentException if value <code>name</code> was before used as parameter in
     * in {@link #requires(ServiceName)} method call. Value can be either required or provided but not both.
     * @throws IllegalStateException if this method have been called after {@link #install()} method.
     * @throws NullPointerException if <code>names</code> parameter is <code>null</code> or any value of the vararg
     * array is <code>null</code>.
     */
    <V> Consumer<V> provides(ServiceName... names);

    /**
     * Sets initial service mode.
     *
     * @param mode initial service mode
     * @return this builder
     * @throws ConcurrentModificationException if builder is shared between threads.
     * Only thread that created the builder can manipulate it.
     * @throws IllegalArgumentException if <code>mode</code> is {@link ServiceController.Mode#REMOVE}.
     * @throws IllegalStateException if this method have been either called twice or it was called after
     * {@link #install()} method.
     * @throws NullPointerException if <code>mode</code> parameter is <code>null</code>.
     */
    ServiceBuilder<T> setInitialMode(ServiceController.Mode mode);

    /**
     * Sets service instance. If {@link #install()} method call is issued
     * without this method being called then <code>NULL</code> service will be
     * installed into the container.
     * <p>
     * Once this method have been called then all subsequent
     * calls of {@link #requires(ServiceName)}, and {@link #provides(ServiceName...)}
     * methods will fail because their return values should be provided to service instance.
     *
     * @param service the service instance
     * @return this configurator
     * @throws ConcurrentModificationException if builder is shared between threads.
     * Only thread that created the builder can manipulate it.
     * @throws IllegalStateException if this method have been either called twice or it was called after
     * {@link #install()} method.
     */
    ServiceBuilder<T> setInstance(org.jboss.msc.Service service);

    /**
     * Adds a service listener to be added to the service.
     *
     * @param listener the listener to add to the service
     * @return this builder
     * @throws ConcurrentModificationException if builder is shared between threads.
     * Only thread that created the builder can manipulate it.
     * @throws IllegalStateException if this method have been called after {@link #install()} method.
     * @throws NullPointerException if <code>listener</code> parameter is <code>null</code>.
     */
    ServiceBuilder<T> addListener(LifecycleListener listener);

    /**
     * Installs configured service into the container.
     *
     * @return installed service controller
     * @throws ConcurrentModificationException if builder is shared between threads.
     * Only thread that created the builder can manipulate it.
     * @throws IllegalStateException if this method have been called twice.
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
     * @deprecated Use {@link #provides(ServiceName...)} instead.
     * This method will be removed in a future release.
     * @throws ConcurrentModificationException if builder is shared between threads.
     * Only thread that created the builder can manipulate it.
     * @throws IllegalArgumentException if value <code>name</code> was before used as parameter in
     * in {@link #requires(ServiceName)} method call. Value can be either required or provided but not both.
     * @throws IllegalStateException if this method have been called after {@link #install()}  method.
     * @throws NullPointerException if <code>aliases</code> parameter is <code>null</code> or any value of the vararg
     * array is <code>null</code>.
     */
    @Deprecated
    ServiceBuilder<T> addAliases(ServiceName... aliases);

    /**
     * Add a dependency.  Calling this method multiple times for the same service name will only add it as a
     * dependency one time; however this may be useful to specify multiple injections for one dependency.
     *
     * @param dependency the name of the dependency
     * @return an injection builder for optionally injecting the dependency
     * @deprecated Use {@link #requires(ServiceName)} instead.
     * This method will be removed in a future release.
     * @throws ConcurrentModificationException if builder is shared between threads.
     * Only thread that created the builder can manipulate it.
     * @throws IllegalStateException if this method have been called after {@link #install()}  method.
     * @throws NullPointerException if <code>dependency</code> parameter is <code>null</code>.
     */
    @Deprecated
    ServiceBuilder<T> addDependency(ServiceName dependency);

    /**
     * Add a service dependency.  Calling this method multiple times for the same service name will only add it as a
     * dependency one time; however this may be useful to specify multiple injections for one dependency.
     *
     * @param dependency the name of the dependency
     * @param target the injector into which the dependency should be stored
     * @return this builder
     * @deprecated Use {@link #requires(ServiceName)} instead.
     * This method will be removed in a future release.
     * @throws ConcurrentModificationException if builder is shared between threads.
     * Only thread that created the builder can manipulate it.
     * @throws IllegalStateException if this method have been called after {@link #install()}  method.
     * @throws NullPointerException if <code>dependency</code> or <code>target</code> parameter is <code>null</code>.
     */
    @Deprecated
    ServiceBuilder<T> addDependency(ServiceName dependency, Injector<Object> target);

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
     * @deprecated Use {@link #requires(ServiceName)} instead.
     * This method will be removed in a future release.
     * @throws ConcurrentModificationException if builder is shared between threads.
     * Only thread that created the builder can manipulate it.
     * @throws IllegalStateException if this method have been called after {@link #install()}  method.
     * @throws NullPointerException if <code>dependency</code> or <code>type</code> or <code>target</code>
     * parameter is <code>null</code>.
     */
    @Deprecated
    <I> ServiceBuilder<T> addDependency(ServiceName dependency, Class<I> type, Injector<I> target);

    /**
     * Adds a stability monitor to be added to the service.
     *
     * @param monitor the monitor to add to the service
     * @return this builder
     * @deprecated Stability monitors are unreliable - do not use them.
     * This method will be removed in a future release.
     * @throws ConcurrentModificationException if builder is shared between threads.
     * Only thread that created the builder can manipulate it.
     * @throws IllegalStateException if this method have been called after {@link #install()}  method.
     * @throws NullPointerException if <code>monitor</code> parameter is <code>null</code>.
     */
    @Deprecated
    ServiceBuilder<T> addMonitor(StabilityMonitor monitor);

}
