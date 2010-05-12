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

import java.lang.reflect.Constructor;
import java.util.List;

/**
 * A value which looks up a public constructor by name from a class.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class LookupConstructorValue implements Value<Constructor> {
    private final Value<Class<?>> target;
    private final List<? extends Value<Class<?>>> parameterTypes;

    /**
     * Construct a new instance.
     *
     * @param target the class in which to look for the constructor
     * @param parameterTypes the parameter types of the constructor
     */
    public LookupConstructorValue(final Value<Class<?>> target, final List<? extends Value<Class<?>>> parameterTypes) {
        this.target = target;
        this.parameterTypes = parameterTypes;
    }

    /** {@inheritDoc} */
    public Constructor getValue() throws IllegalStateException {
        Class[] types = new Class[parameterTypes.size()];
        int i = 0;
        for (Value<Class<?>> type : parameterTypes) {
            types[i++] = type.getValue();
        }
        final Class<?> targetClass = target.getValue();
        try {
            return targetClass.getConstructor(types);
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException("No such constructor found on " + targetClass);
        }
    }
}