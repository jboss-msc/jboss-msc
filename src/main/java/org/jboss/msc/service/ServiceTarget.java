/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2022, Red Hat, Inc., and individual contributors
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

/**
 * The target of ServiceBuilder installations.
 *
 * ServiceBuilders to be installed on a target have to be retrieved via {@link #addService(ServiceName)} method.
 * Service installation will only take place after {@link ServiceBuilder#install()} is issued.
 * ServiceBuilders that are not installed will be ignored.
 * <p>
 * Implementations of this interface are thread safe.
 * 
 * @author <a href="mailto:flavia.rainone@jboss.com">Flavia Rainone</a>
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
public interface ServiceTarget {

    /**
     * Add a service lifecycle listener that will be added to this service.
     *
     * @param listener the lifecycle listener to add to the service
     * @return this target
     */
    ServiceTarget addListener(LifecycleListener listener);

    /**
     * Remove a lifecycle listener from this target, if it exists.
     *
     * @param listener the lifecycle listener to remove
     * @return this target
     */
    ServiceTarget removeListener(LifecycleListener listener);

    /**
     * Get a builder which can be used to add a service to this target.
     *
     * @param name the service name
     * @return new service configurator
     */
    ServiceBuilder<?> addService(ServiceName name);

    /**
     * Create a sub-target using this as the parent target.
     *
     * @return the new service target
     */
    ServiceTarget subTarget();

    ////////////////////////
    // DEPRECATED METHODS //
    ////////////////////////

    /**
     * Get a builder which can be used to add a service to this target.
     *
     * @param name the service name
     * @param service the service
     * @return the builder for the service
     * @deprecated Use {@link #addService(ServiceName)} instead.
     * This method will be removed in a future release.
     */
    @Deprecated
    <T> ServiceBuilder<T> addService(ServiceName name, Service<T> service);

    /**
     * Add a stability monitor that will be added to all the ServiceBuilders installed in this target.
     *
     * @param monitor the monitor to add to the target
     * @return this target
     * @deprecated Stability monitors are unreliable - do not use them.
     * This method will be removed in a future release.
     */
    @Deprecated
    ServiceTarget addMonitor(StabilityMonitor monitor);

    /**
     * Remove a monitor from this target, if it exists.
     *
     * @param monitor the monitor to remove
     * @return this target
     * @deprecated Stability monitors are unreliable - do not use them.
     * This method will be removed in a future release.
     */
    @Deprecated
    ServiceTarget removeMonitor(StabilityMonitor monitor);

    /**
     * Add a dependency that will be added to the all ServiceBuilders installed in this target.
     *
     * @param dependency the dependency to add to the target
     * @return this target
     * @deprecated This method will be removed in a future release.
     */
    @Deprecated
    ServiceTarget addDependency(ServiceName dependency);
}
