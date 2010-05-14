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

package org.jboss.msc.value;

import java.lang.reflect.Field;
import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.PrivilegedAction;

/**
 * A value which looks up a possibly non-public field by name from a class.  This may be considerably slower than
 * {@link LookupFieldValue} so that class should be used whenever possible.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class LookupDeclaredFieldValue implements Value<Field> {
    private final Value<Class<?>> target;
    private final String fieldName;
    private final AccessControlContext context;
    private final boolean makeAccessible;

    /**
     * Construct a new instance.
     *
     * @param target the class in which to look for the field
     * @param fieldName the name of the field to look up
     * @param context the access control context to use
     * @param makeAccessible {@code true} to make the field accessible under the provided access control context
     */
    public LookupDeclaredFieldValue(final Value<Class<?>> target, final String fieldName, final AccessControlContext context, final boolean makeAccessible) {
        this.target = target;
        this.fieldName = fieldName;
        this.context = context;
        this.makeAccessible = makeAccessible;
    }

    /** {@inheritDoc} */
    public Field getValue() throws IllegalStateException {
        final Class<?> targetClass = target.getValue();
        final SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            return AccessController.doPrivileged(new PrivilegedAction<Field>() {
                public Field run() {
                    try {
                        final Field field = targetClass.getDeclaredField(fieldName);
                        if (makeAccessible) field.setAccessible(true);
                        return field;
                    } catch (NoSuchFieldException e) {
                        throw new IllegalStateException("No such field '" + fieldName + "' found on " + targetClass);
                    }
                }
            }, context);
        }
        try {
            final Field field = targetClass.getDeclaredField(fieldName);
            if (makeAccessible) field.setAccessible(true);
            return field;
        } catch (NoSuchFieldException e) {
            throw new IllegalStateException("No such field '" + fieldName + "' found on " + targetClass);
        }
    }
}