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

import java.lang.reflect.Method;

/**
 * A value which looks up a public get method by name and parameters from a class.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class LookupGetMethodValue implements Value<Method> {
    private final Value<Class<?>> target;
    private final String propertyName;

    /**
     * Construct a new instance.
     *
     * @param target the class in which to look for the method
     * @param propertyName the name of the property (e.g. "executor" will yield a method "setExecutor")
     */
    public LookupGetMethodValue(final Value<Class<?>> target, final String propertyName) {
        if (target == null) {
            throw new IllegalArgumentException("target is null");
        }
        if (propertyName == null) {
            throw new IllegalArgumentException("propertyName is null");
        }
        this.target = target;
        this.propertyName = propertyName;
    }

    /** {@inheritDoc} */
    public Method getValue() throws IllegalStateException {
        final Class<?> targetClass = target.getValue();
        final String propertyName = this.propertyName;
        final String getterName = "get" + Character.toUpperCase(propertyName.charAt(0)) + propertyName.substring(1);
        try {
            return targetClass.getMethod(getterName);
        } catch (NoSuchMethodException e) {
            final String iserName = "is" + Character.toUpperCase(propertyName.charAt(0)) + propertyName.substring(1);
            try {
                return targetClass.getMethod(iserName);
            } catch (NoSuchMethodException e1) {
                throw new IllegalStateException("No such get method for property '" + propertyName + "' found on " + targetClass);
            }
        }
    }
}