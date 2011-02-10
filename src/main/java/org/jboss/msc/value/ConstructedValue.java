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

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

/**
 * A value which is created on demand from a constructor.  Each call to {@link #getValue()} will create a new instance,
 * so if the same instance should be returned, this should be used in conjunction with {@link CachedValue}.
 *
 * @param <T> the value type
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class ConstructedValue<T> implements Value<T> {
    private final Constructor<T> constructor;
    private final List<? extends Value<?>> parameters;

    /**
     * Construct a new instance.
     *
     * @param constructorValue the constructor to use
     * @param parameters the parameters to pass to the constructor
     *
     * @deprecated Will be removed before 1.0.0.GA
     */
    @Deprecated
    public ConstructedValue(final Value<Constructor<T>> constructorValue, final List<? extends Value<?>> parameters) {
        this(constructorValue.getValue(), parameters);
    }

    /**
     * Construct a new instance.
     *
     * @param constructor the constructor to use
     * @param parameters the parameters to pass to the constructor
     */
    public ConstructedValue(final Constructor<T> constructor, final List<? extends Value<?>> parameters) {
        if (constructor == null) {
            throw new IllegalArgumentException("constructor is null");
        }
        if (parameters == null) {
            throw new IllegalArgumentException("parameters is null");
        }
        this.constructor = constructor;
        this.parameters = parameters;
    }

    /** {@inheritDoc} */
    public T getValue() throws IllegalStateException {
        try {
            return constructor.newInstance(Values.getValues(parameters));
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Field is not accessible", e);
        } catch (InvocationTargetException e) {
            throw new IllegalStateException("Failed to invoke constructor", e);
        } catch (InstantiationException e) {
            throw new IllegalStateException("Failed to construct instance", e);
        }
    }
}