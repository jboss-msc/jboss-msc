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

import java.lang.reflect.Method;

/**
 * A value which looks up a public set method by name and parameters from a class.
 *
 * @deprecated Will be removed before 1.0.0.GA
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
@Deprecated
public final class LookupSetMethodValue implements Value<Method> {
    private final Value<Class<?>> target;
    private final String propertyName;
    private final Value<Class<?>> propertyType;

    /**
     * Construct a new instance.
     *
     * @param target the class in which to look for the method
     * @param propertyName the name of the property (e.g. "executor" will yield a method "setExecutor")
     * @param propertyType the type of the property
     */
    public LookupSetMethodValue(final Value<Class<?>> target, final String propertyName, final Value<Class<?>> propertyType) {
        if (target == null) {
            throw new IllegalArgumentException("target is null");
        }
        if (propertyName == null) {
            throw new IllegalArgumentException("propertyName is null");
        }
        if (propertyType == null) {
            throw new IllegalArgumentException("propertyType is null");
        }
        this.target = target;
        this.propertyName = propertyName;
        this.propertyType = propertyType;
    }

    /**
     * Construct a new instance which searches for the first name-only match.
     *
     * @param target the class in which to look for the method
     * @param propertyName the name of the property (e.g. "executor" will yield a method "setExecutor")
     */
    public LookupSetMethodValue(final Value<Class<?>> target, final String propertyName) {
        if (target == null) {
            throw new IllegalArgumentException("target is null");
        }
        if (propertyName == null) {
            throw new IllegalArgumentException("propertyName is null");
        }
        this.target = target;
        this.propertyName = propertyName;
        propertyType = null;
    }

    /** {@inheritDoc} */
    public Method getValue() throws IllegalStateException {
        final Class<?> targetClass = target.getValue();
        final String propertyName = this.propertyName;
        final String setterName = "set" + Character.toUpperCase(propertyName.charAt(0)) + propertyName.substring(1);
        try {
            final Value<Class<?>> propertyType = this.propertyType;
            if (propertyType != null) {
                Class<?>[] types = new Class[] { propertyType.getValue() };
                return targetClass.getMethod(setterName, types);
            } else {
                for (Method method : targetClass.getMethods()) {
                    if (method.getName().equals(setterName) && method.getParameterTypes().length == 1) {
                        return method;
                    }
                }
            }
        } catch (NoSuchMethodException e) {
            // fall thru
        }
        throw new IllegalStateException("No such set method for property '" + propertyName + "' found on " + targetClass);
    }
}