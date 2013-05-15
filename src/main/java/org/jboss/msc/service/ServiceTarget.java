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

import java.util.Collection;
import java.util.Set;

import org.jboss.msc.value.Value;

/**
 * The target of ServiceBuilder installations.
 * ServicesBuilders to be installed on a target should be retrieved by calling one of the {@code addService} methods
 * ({@link #addService(ServiceName, Service)}, {@link #addServiceValue(ServiceName, Value)}.
 * Notice that installation will only take place after {@link ServiceBuilder#install()} is invoked. ServiceBuilders that
 * are not installed are ignored.
 * 
 * @author <a href="mailto:flavia.rainone@jboss.com">Flavia Rainone</a>
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
public interface ServiceTarget {

    /**
     * Get a builder which can be used to add a service to this target.
     *
     * @param name the service name
     * @param value the service value
     * @return the builder for the service
     */
    <T> ServiceBuilder<T> addServiceValue(ServiceName name, Value<? extends Service<T>> value);

    /**
     * Get a builder which can be used to add a service to this target.
     *
     * @param name the service name
     * @param service the service
     * @return the builder for the service
     */
    <T> ServiceBuilder<T> addService(ServiceName name, Service<T> service);
    
    /**
     * Add a stability monitor that will be added to all the ServiceBuilders installed in this target.
     *
     * @param monitor the monitor to add to the target
     * @return this target
     */
    ServiceTarget addMonitor(StabilityMonitor monitor);

    /**
     * Add a stability monitors that will be added to all the ServiceBuilders installed in this target.
     *
     * @param monitors the monitors to add to the target
     * @return this target
     */
    ServiceTarget addMonitors(StabilityMonitor... monitors);

    /**
     * Add a service listener that will be added to all the ServiceBuilders installed in this target.
     *
     * @param listener the listener to add to the target
     * @return this target
     */
    ServiceTarget addListener(ServiceListener<Object> listener);

    /**
     * Add a list of service listener that will be added to all ServiceBuilders installed in this target.
     *
     * @param listeners a list of listeners to add to the target
     * @return this target
     */
    ServiceTarget addListener(ServiceListener<Object>... listeners);

    /**
     * Add a collection of service listener that will be added to all ServiceBuilders installed in this target.
     *
     * @param listeners a collection of listeners to add to the target
     * @return this target
     */
    ServiceTarget addListener(Collection<ServiceListener<Object>> listeners);

    /**
     * Add a service listener that will be added to all the ServiceBuilders installed in this target.
     *
     * @param inheritance the inheritance type for this listener
     * @param listener the listener to add to the target
     * @return this target
     */
    ServiceTarget addListener(ServiceListener.Inheritance inheritance, ServiceListener<Object> listener);

    /**
     * Add a list of service listener that will be added to all ServiceBuilders installed in this target.
     *
     * @param inheritance the inheritance type for this listener
     * @param listeners a list of listeners to add to the target
     * @return this target
     */
    ServiceTarget addListener(ServiceListener.Inheritance inheritance, ServiceListener<Object>... listeners);

    /**
     * Add a collection of service listener that will be added to all ServiceBuilders installed in this target.
     *
     * @param inheritance the inheritance type for this listener
     * @param listeners a collection of listeners to add to the target
     * @return this target
     */
    ServiceTarget addListener(ServiceListener.Inheritance inheritance, Collection<ServiceListener<Object>> listeners);
    
    /**
     * Remove a monitor from this target, if it exists.
     *
     * @param monitor the monitor to remove
     * @return this target
     */
    ServiceTarget removeMonitor(StabilityMonitor monitor);

    /**
     * Remove a listener from this target, if it exists.
     *
     * @param listener the listener to remove
     * @return this target
     */
    ServiceTarget removeListener(ServiceListener<Object> listener);
    
    /**
     * Returns a set of the monitors added to this target.
     * 
     * @return the monitors added to this target
     */
    Set<StabilityMonitor> getMonitors();

    /**
     * Returns a set of the listeners added to this target.
     * 
     * @return the listeners added to this target
     */
    Set<ServiceListener<Object>> getListeners();

    /**
     * Add a dependency that will be added to the all ServiceBuilders installed in this target.
     *
     * @param dependency the dependency to add to the target
     * @return this target
     */
    ServiceTarget addDependency(ServiceName dependency);

    /**
     * Add a list of dependencies that will be added to the all ServiceBuilders installed in this target.
     *
     * @param dependencies a list of dependencies to add to the target
     * @return this target
     */
    ServiceTarget addDependency(ServiceName... dependencies);

    /**
     * Add a collection of dependencies that will be added to the all ServiceBuilders installed in this target
     *
     * @param dependencies a collection of dependencies to add to this target
     * @return this target
     */
    ServiceTarget addDependency(Collection<ServiceName> dependencies);

    /**
     * Remove a dependency from this target.  Subsequently defined services will not have this dependency.
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
     * Create a sub-target using this as the parent target.
     *
     * @return the new service target
     */
    ServiceTarget subTarget();

    /**
     * Create a new batch service target, which is used to install described services in this target.
     *
     * @return the new batch service target
     */
    BatchServiceTarget batchTarget();
}
