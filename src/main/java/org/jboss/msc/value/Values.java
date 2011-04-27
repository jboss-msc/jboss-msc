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

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * Value utility methods.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class Values {

    private static final ThreadLocalValue<Object> THIS = new ThreadLocalValue<Object>();

    private static final ThreadLocalValue<Object> INJECTED = new ThreadLocalValue<Object>();

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

    /**
     * Get an object array from the result of an iterable series of values.
     *
     * @param i the iterable series
     * @return the values array
     */
    public static Object[] getValues(Iterable<? extends Value<?>> i) {
        return getValues(i.iterator(), 0);
    }

    /**
     * Get a typed object array from the result of an iterable series of values.
     *
     * @param i the iterable series
     * @param clazz the resultant array type
     * @return the values array
     */
    public static <T> T[] getValues(Iterable<? extends Value<? extends T>> i, Class<T> clazz) {
        return getValues(i.iterator(), clazz, 0);
    }

    /**
     * Get a typed object array from the result of an iterable series of values.
     *
     * @param i the iterable series
     * @param array the array to populate
     * @return the values array
     */
    public static <T> T[] getValues(Iterable<? extends Value<? extends T>> i, T[] array) {
        int idx = 0;
        for (final Value<? extends T> value : i) {
            array[idx++] = value.getValue();
        }
        return array;
    }

    /**
     * Get the null value.
     *
     * @param <T> the value type
     * @return a value which always yields {@code null}
     */
    @SuppressWarnings({ "unchecked" })
    public static <T> Value<T> nullValue() {
        return NULL;
    }

    /**
     * Get a cached value for some opaque value.  If the value is already cached, it is returned as-is.
     *
     * @param value the value to wrap
     * @param <T> the value type
     * @return a cached value
     */
    public static <T> Value<T> cached(Value<T> value) {
        if (value instanceof CachedValue || value instanceof ImmediateValue) {
            return value;
        } else {
            return new CachedValue<T>(value);
        }
    }

    /**
     * The empty value list.
     */
    public static final List<Value<?>> EMPTY_LIST = emptyList();

    /**
     * The empty value type list.
     */
    public static final List<? extends Value<Class<?>>> EMPTY_TYPE_LIST = new ArrayList<Value<Class<?>>>();

    /**
     * The empty value list.
     *
     * @param <T> the value type
     * @return the empty value list
     */
    public static <T> List<Value<? extends T>> emptyList() {
        return Collections.emptyList();
    }

    /**
     * Get an immediate value.
     *
     * @param value the value to return
     * @param <T> the value type
     * @return the immediate value
     */
    public static <T> Value<T> immediateValue(T value) {
        return new ImmediateValue<T>(value);
    }

    private static final Value EMPTY_LIST_VALUE = new ImmediateValue<List>(Collections.emptyList());

    /**
     * A value which yields the empty list.
     *
     * @param <T> the list member type
     * @return the empty list value
     */
    @SuppressWarnings({ "unchecked" })
    public static <T> Value<List<T>> emptyListValue() {
        return EMPTY_LIST_VALUE;
    }

    public static <T> List<Value<? extends T>> immediateValues(List<T> values) {
        final List<Value<? extends T>> newList = new ArrayList<Value<? extends T>>(values.size());
        for (T value : values) {
            newList.add(new ImmediateValue<T>(value));
        }
        return newList;
    }

    public static <T> List<Value<? extends T>> immediateValues(T... values) {
        final List<Value<? extends T>> newList = new ArrayList<Value<? extends T>>(values.length);
        for (T value : values) {
            newList.add(new ImmediateValue<T>(value));
        }
        return newList;
    }

    /**
     * Safely re-cast a value as its superclass.
     *
     * @param value the value to re-cast
     * @param <T> the value type
     * @return the value
     */
    @SuppressWarnings({ "unchecked" })
    public static <T> Value<T> asSuperclass(Value<? extends T> value) {
        return (Value<T>) value;
    }

    /**
     * The special value representing {@code this} (the object being invoked upon).
     *
     * @return the value for {@code this}
     */
    public static ThreadLocalValue<Object> thisValue() {
        return THIS;
    }

    /**
     * The special value representing the value of an injection operation.
     *
     * @return the target value
     */
    public static ThreadLocalValue<Object> injectedValue() {
        return INJECTED;
    }

}
