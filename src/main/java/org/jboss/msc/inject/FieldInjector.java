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

package org.jboss.msc.inject;

import java.lang.reflect.Field;
import org.jboss.msc.value.Value;

/**
 * An injector which updates the value of a field.
 *
 * @param <T> the value type
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 * @deprecated Use {@link org.jboss.msc.service.ServiceBuilder#provides(org.jboss.msc.service.ServiceName...)}
 * method instead. This class will be removed in a future release.
 */
@Deprecated
public final class FieldInjector<T> implements Injector<T> {

    private final Value<?> target;
    private final Field field;

    /**
     * Construct a new instance.
     *
     * @param target the object whose field is to be updated
     * @param field the field to update
     */
    public FieldInjector(final Value<?> target, final Field field) {
        this.target = target;
        this.field = field;
    }

    /** {@inheritDoc} */
    public void inject(final T value) {
        try {
            field.set(target.getValue(), value);
        } catch (Exception e) {
            throw new InjectionException("Failed to inject value into field", e);
        }
    }

    /** {@inheritDoc} */
    public void uninject() {
        final Field field = this.field;
        try {
            Class<?> fieldType = field.getType();
            Object targetValue = target.getValue();
            if (fieldType.isPrimitive()) {
                uninjectPrimitive(field, fieldType, targetValue);
            } else {
                field.set(targetValue, null);
            }
        } catch (Throwable throwable) {
            InjectorLogger.INSTANCE.uninjectFailed(throwable, field);
        }
    }

    private void uninjectPrimitive(Field field, Class<?> fieldType, Object targetValue)
        throws IllegalArgumentException, IllegalAccessException {
        
        switch(fieldType.getName().toString().charAt(0)) {
            case 'b':
                if (fieldType == byte.class) {
                    field.setByte(targetValue, (byte) 0);
                } else { // fieldType is boolean.class
                    field.setBoolean(targetValue, false);
                }
                break;
            case 'c': // char
                field.setChar(targetValue, '\u0000');
                break;
            case 'd':// double
                field.setDouble(targetValue, 0.0);
                break;
            case 'f':// float
                field.setFloat(targetValue, 0.0f);
                break;
            case 'i':// int
                field.setInt(targetValue, 0);
                break;
            case 'l':// long
                field.setLong(targetValue, 0l);
                break;
            case 's':// short
                field.setShort(targetValue, (short) 0);
                break;
            default:
                throw new IllegalStateException("Unexpected field primitive type " + fieldType.getName());
        }
    }
}