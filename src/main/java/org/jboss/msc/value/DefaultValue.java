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

package org.jboss.msc.value;

/**
 * A defaulted value.  If the delegate value returns {@code null}, a default value will be returned in its place.
 *
 * @param <T> the value type
 * @deprecated Use {@link org.jboss.msc.service.ServiceBuilder#requires(org.jboss.msc.service.ServiceName)}
 * method instead. This class will be removed in a future release.
 */
@Deprecated
public final class DefaultValue<T> implements Value<T> {
    private final Value<T> value;
    private final Value<? extends T> defaultValue;

    /**
     * Construct a new instance.
     *
     * @param value the delegate value
     * @param defaultValue the value to use if the delegate value returns {@code null}
     */
    public DefaultValue(final Value<T> value, final Value<? extends T> defaultValue) {
        this.value = value;
        this.defaultValue = defaultValue;
    }

    /** {@inheritDoc} */
    public T getValue() throws IllegalStateException {
        final T result = value.getValue();
        return result != null ? result : defaultValue.getValue();
    }
}
