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

package org.jboss.msc.service;

/**
 *
 */
public interface ServiceBuilder<S> {

    /**
     * Add a dependency.
     * @param dependency
     */
    void addDependency(ServiceController<?> dependency);

    ServiceBuilder<S> addListener(ServiceListener<? super S> listener);

    ServiceBuilder<S> setInitialMode(ServiceController.Mode mode);

    ServiceBuilder<S> setLocation(Location location);

    /**
     * Set the location to be the caller's location.
     *
     * @return this builder
     */
    ServiceBuilder<S> setLocation();

    /**
     * Get the built service controller.  Once this method is called, no further changes may be made to the builder.
     * Calling this method multiple times will return the same controller.
     *
     * @return the service controller
     */
    ServiceController<S> create();
}
