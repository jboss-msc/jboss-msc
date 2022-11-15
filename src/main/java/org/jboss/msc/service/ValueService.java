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

import java.util.function.Consumer;

import org.jboss.msc.value.Value;

/**
 * A service which returns the provided value, which is evaluated once per service start.
 *
 * @param <T> the service type
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 * @deprecated Use {@link org.jboss.msc.Service#newInstance(Consumer, Object) Service.newInstance(Consumer&lt;T&gt;,T} instead.  This
 * class will be removed in a future release.
 */
@Deprecated
public final class ValueService<T> implements Service<T> {
    private final Value<T> value;
    private volatile T valueInstance;

    /**
     * Construct a new instance.
     *
     * @param value the value to return
     */
    public ValueService(final Value<T> value) {
        this.value = value;
    }

    /** {@inheritDoc} */
    public void start(final StartContext context) throws StartException {
        valueInstance = value.getValue();
    }

    /** {@inheritDoc} */
    public void stop(final StopContext context) {
        valueInstance = null;
    }

    /** {@inheritDoc} */
    public T getValue() throws IllegalStateException {
        final T value = valueInstance;
        if (value == null) {
            throw ServiceLogger.SERVICE.serviceNotStarted();
        }
        return value;
    }
}
