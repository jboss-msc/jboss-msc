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
 * A value which looks up a public field by name from a class.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class LookupFieldValue implements Value<Field> {
    private final Value<Class<?>> target;
    private final String fieldName;

    /**
     * Construct a new instance.
     *
     * @param target the class in which to look for the field
     * @param fieldName the name of the field to look up
     */
    public LookupFieldValue(final Value<Class<?>> target, final String fieldName) {
        if (target == null) {
            throw new IllegalArgumentException("target is null");
        }
        if (fieldName == null) {
            throw new IllegalArgumentException("fieldName is null");
        }
        this.target = target;
        this.fieldName = fieldName;
    }

    /** {@inheritDoc} */
    public Field getValue() throws IllegalStateException {
        final Class<?> targetClass = target.getValue();
        try {
            return targetClass.getField(fieldName);
        } catch (NoSuchFieldException e) {
            throw new IllegalStateException("No such field '" + fieldName + "' found on " + targetClass);
        }
    }
}
