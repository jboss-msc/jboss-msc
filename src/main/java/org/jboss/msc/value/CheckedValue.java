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

package org.jboss.msc.value;

/**
 * A value whose type is checked before it is returned.
 *
 * @param <T> the value type
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 * @deprecated Use {@link org.jboss.msc.service.ServiceBuilder#requires(org.jboss.msc.service.ServiceName)}
 * method instead. This class will be removed in a future release.
 */
@Deprecated
public final class CheckedValue<T> implements Value<T> {
    private final Class<T> valueClass;
    private final Value<?> value;

    /**
     * Construct a new instance.
     *
     * @param valueClass the value class
     * @param value the delegate value
     */
    public CheckedValue(final Class<T> valueClass, final Value<?> value) {
        this.valueClass = valueClass;
        this.value = value;
    }

    /** {@inheritDoc} */
    public T getValue() throws IllegalStateException {
        return valueClass.cast(value.getValue());
    }
}
