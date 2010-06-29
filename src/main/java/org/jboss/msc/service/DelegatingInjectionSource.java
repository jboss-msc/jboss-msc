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

import org.jboss.msc.value.Value;

/**
 * Base injection source that delegates to another injection source, and is intended to be extended to support source chaining.
 *
 * @author John E. Bailey
 */
public abstract class DelegatingInjectionSource extends InjectionSource {

    private final InjectionSource delegate;

    public DelegatingInjectionSource(InjectionSource delegate) {
        this.delegate = delegate;
    }

    @Override
    protected <T> Value<?> getValue(Value<T> serviceValue, ServiceBuilder<T> serviceBuilder, ServiceContainerImpl container) {
        return getValue(delegate.getValue(serviceValue, serviceBuilder, container), serviceValue, serviceBuilder, container);
    }

    /**
     * Contract method providing children with the value of the delegate InjectionSource.
     *
     * @param delegateValue The value from the delegate injection source
     * @param serviceValue The service value
     * @param serviceBuilder The service builder
     * @param container The service container
     * @return The value of the injection source
     */
    protected abstract <T> Value<?> getValue(Value<?> delegateValue, Value<T> serviceValue, ServiceBuilder<T> serviceBuilder, ServiceContainerImpl container);
}
