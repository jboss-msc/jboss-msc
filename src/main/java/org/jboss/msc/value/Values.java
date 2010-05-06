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

import java.lang.reflect.Array;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

public final class Values {

    private static final ThreadLocalValue<Object> THIS = new ThreadLocalValue<Object>();

    private static final ThreadLocalValue<Object> TARGET = new ThreadLocalValue<Object>();

    private static final Value NULL = new ImmediateValue<Object>(null);

    private Values() {}

    private static final Object[] NONE = new Object[0];

    private static <T> T[] getValues(Iterator<? extends Value<? extends T>> i, Class<T> clazz, int idx) {
        if (i.hasNext()) {
            final Value<? extends T> v = i.next();
            final T[] params = getValues(i, clazz, idx + 1);
            params[idx] = v.getValue();
            return params;
        } else {
            return arrayOf(clazz, idx);
        }
    }

    @SuppressWarnings({ "unchecked" })
    private static <T> T[] arrayOf(Class<T> clazz, int len) {
        return (T[]) Array.newInstance(clazz, len);
    }

    private static Object[] getValues(Iterator<? extends Value<?>> i, int idx) {
        if (i.hasNext()) {
            final Value<?> v = i.next();
            final Object[] params = getValues(i, idx + 1);
            params[idx] = v.getValue();
            return params;
        } else {
            return idx == 0 ? NONE : new Object[idx];
        }
    }

    public static Object[] getValues(Iterable<? extends Value<?>> i) {
        return getValues(i.iterator(), 0);
    }

    public static <T> T[] getValues(Iterable<? extends Value<? extends T>> i, Class<T> clazz) {
        return getValues(i.iterator(), clazz, 0);
    }

    public static <T> T[] getValues(Iterable<? extends Value<? extends T>> i, T[] array) {
        int idx = 0;
        for (final Value<? extends T> value : i) {
            array[idx++] = value.getValue();
        }
        return array;
    }

    @SuppressWarnings({ "unchecked" })
    public static <T> Value<T> nullValue() {
        return NULL;
    }

    public static <T> CachedValue<T> cached(Value<T> value) {
        if (value instanceof CachedValue) {
            return (CachedValue<T>) value;
        } else {
            return new CachedValue<T>(value);
        }
    }

    public static final List<Value<?>> EMPTY_LIST = emptyList();

    public static <T> List<Value<? extends T>> emptyList() {
        return Collections.emptyList();
    }

    public static <T> Value<T> immediateValue(T value) {
        return new ImmediateValue<T>(value);
    }

    public static final Value EMPTY_LIST_VALUE = new ImmediateValue<List>(Collections.emptyList());

    @SuppressWarnings({ "unchecked" })
    public static <T> Value<List<T>> emptyListValue() {
        return EMPTY_LIST_VALUE;
    }

    @SuppressWarnings({ "unchecked" })
    public static <T> Value<T> asSuperclass(Value<? extends T> value) {
        return (Value<T>) value;
    }

    public static ThreadLocalValue<Object> thisValue() {
        return THIS;
    }

    public static ThreadLocalValue<Object> targetValue() {
        return TARGET;
    }
}
