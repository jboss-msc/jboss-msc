/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

/**
 * A {@link ServiceTarget} that provides {@link #removeServices() removal} of all services installed so far. 
 * 
 * @author <a href="mailto:flavia.rainone@jboss.com">Flavia Rainone</a>
 */
public interface BatchServiceTarget extends ServiceTarget {
    /**
     * Removes all services installed into this target so far.
     */
    void removeServices();

    /** {@inheritDoc} */
    @Override
    BatchServiceTarget addMonitor(StabilityMonitor monitor);

    /** {@inheritDoc} */
    @Override
    BatchServiceTarget addMonitors(StabilityMonitor... monitors);

    /** {@inheritDoc} */
    @Override
    BatchServiceTarget removeMonitor(StabilityMonitor monitor);

    /** {@inheritDoc} */
    @Override
    BatchServiceTarget addListener(ServiceListener<Object> listener);

    /** {@inheritDoc} */
    @Override
    BatchServiceTarget addListener(ServiceListener<Object>... listeners);

    /** {@inheritDoc} */
    @Override
    BatchServiceTarget addListener(Collection<ServiceListener<Object>> listeners);

    /** {@inheritDoc} */
    @Override
    BatchServiceTarget removeListener(ServiceListener<Object> listener);

    /** {@inheritDoc} */
    @Override
    BatchServiceTarget addDependency(ServiceName dependency);

    /** {@inheritDoc} */
    @Override
    BatchServiceTarget addDependency(ServiceName... dependencies);

    /** {@inheritDoc} */
    @Override
    BatchServiceTarget addDependency(Collection<ServiceName> dependencies);

    /** {@inheritDoc} */
    @Override
    BatchServiceTarget removeDependency(ServiceName dependency);
}
