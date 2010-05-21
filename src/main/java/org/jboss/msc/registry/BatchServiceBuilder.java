/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat Middleware LLC, and individual contributors
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

package org.jboss.msc.registry;

import java.util.Collection;
import org.jboss.msc.service.Location;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceListener;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.value.Value;
import org.jboss.msc.service.Service;

/**
 * A builder for an individual service in a batch.  Create an instance via the
 * {@link BatchBuilder#addService(ServiceName, Service)}
 * or
 * {@link BatchBuilder#addServiceValue(ServiceName, Value)}
 * methods.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public interface BatchServiceBuilder<T> {
    /**
     * Set the service definition location to be the caller's location.
     *
     * @return this builder
     */
    BatchServiceBuilder<T> setLocation();

    /**
     * Set the service definition location, if any.
     *
     * @param location the location
     * @return this builder
     */
    BatchServiceBuilder<T> setLocation(Location location);

    /**
     * Set the initial mode.
     *
     * @param mode the initial mode
     * @return this builder
     */
    BatchServiceBuilder<T> setInitialMode(ServiceController.Mode mode);

    BatchServiceBuilder<T> addDependencies(ServiceName... dependencies);

    BatchServiceBuilder<T> addDependencies(Iterable<ServiceName> dependencies);

    /**
     * Add a dependency.  Calling this method multiple times for the same service name will only add it as a
     * dependency one time; however this may be useful to specify multiple injections for one dependency.
     *
     * @param dependency the name of the dependency
     * @return an injection builder for optionally injecting the dependency
     */
    BatchInjectionBuilder addDependency(ServiceName dependency);

    /**
     * Add an injection value.
     *
     * @param value the value to inject
     * @return an injection builder for specifying the injection target
     */
    BatchInjectionBuilder addInjectionValue(Value<?> value);

    /**
     * Add an injection.
     *
     * @param value the value to inject
     * @return an injection builder for specifying the injection target
     */
    BatchInjectionBuilder addInjection(Object value);

    /**
     * Add a service listener that will be added to this service.
     *
     * @param listener the listener to add to the service
     * @return this builder
     */
    BatchServiceBuilder<T> addListener(ServiceListener<? super T> listener);

    /**
     * Add service listeners that will be added to this service.
     *
     * @param listeners a list of listeners to add to the service
     * @return this builder
     */
    BatchServiceBuilder<T> addListener(ServiceListener<? super T>... listeners);

    /**
     * Add service listeners that will be added to this service.
     *
     * @param listeners a collection of listeners to add to the service
     * @return this builder
     */
    BatchServiceBuilder<T> addListener(Collection<? extends ServiceListener<? super T>> listeners);
}
