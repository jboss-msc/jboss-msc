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

import java.lang.reflect.Field;

/**
 * A value which reads a field of an object.
 *
 * @param <T> the value type
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 * @deprecated use {@link org.jboss.msc.service.ServiceBuilder#requires(org.jboss.msc.service.ServiceName)}
 * method instead. This class will be removed in future releases.
 */
@Deprecated
public final class FieldValue<T> implements Value<T> {
    private final Field field;
    private final Value<?> targetValue;

    /**
     * Construct a new instance.
     *
     * @param field the field to access
     * @param targetValue the target object containing the field
     */
    public FieldValue(final Field field, final Value<?> targetValue) {
        if (field == null) {
            throw new IllegalArgumentException("field is null");
        }
        if (targetValue == null) {
            throw new IllegalArgumentException("targetValue is null");
        }
        this.field = field;
        this.targetValue = targetValue;
    }

    /** {@inheritDoc} */
    @SuppressWarnings({ "unchecked" })
    public T getValue() throws IllegalStateException {
        try {
            return (T) field.get(targetValue.getValue());
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Field is not accessible", e);
        }
    }
}
