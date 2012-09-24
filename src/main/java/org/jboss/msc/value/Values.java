/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
    private static final ReadableValue NULL = new ImmediateValue<Object>(null);
    private static final Object[] NONE = new Object[0];

    private Values() {}

    private static <T> T[] getValues(Iterator<? extends ReadableValue<? extends T>> i, Class<T> clazz, int idx) {
        if (i.hasNext()) {
            final ReadableValue<? extends T> v = i.next();
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

    private static Object[] getValues(Iterator<? extends ReadableValue<?>> i, int idx) {
        if (i.hasNext()) {
            final ReadableValue<?> v = i.next();
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
    public static Object[] getValues(Iterable<? extends ReadableValue<?>> i) {
        return getValues(i.iterator(), 0);
    }

    /**
     * Get a typed object array from the result of an iterable series of values.
     *
     * @param i the iterable series
     * @param clazz the resultant array type
     * @return the values array
     */
    public static <T> T[] getValues(Iterable<? extends ReadableValue<? extends T>> i, Class<T> clazz) {
        return getValues(i.iterator(), clazz, 0);
    }

    /**
     * Get a typed object array from the result of an iterable series of values.
     *
     * @param i the iterable series
     * @param array the array to populate
     * @return the values array
     */
    public static <T> T[] getValues(Iterable<? extends ReadableValue<? extends T>> i, T[] array) {
        int idx = 0;
        for (final ReadableValue<? extends T> value : i) {
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
    public static <T> ReadableValue<T> nullValue() {
        return NULL;
    }

    /**
     * Get a cached value for some opaque value.  If the value is already cached, it is returned as-is.
     *
     * @param value the value to wrap
     * @param <T> the value type
     * @return a cached value
     */
    public static <T> ReadableValue<T> cached(ReadableValue<T> value) {
        if (value instanceof CachedReadableValue || value instanceof ImmediateValue) {
            return value;
        } else {
            return new CachedReadableValue<T>(value);
        }
    }

    /**
     * The empty value list.
     */
    public static final List<ReadableValue<?>> EMPTY_LIST = emptyList();

    /**
     * The empty value type list.
     */
    public static final List<? extends ReadableValue<Class<?>>> EMPTY_TYPE_LIST = new ArrayList<ReadableValue<Class<?>>>();

    /**
     * The empty value list.
     *
     * @param <T> the value type
     * @return the empty value list
     */
    public static <T> List<ReadableValue<? extends T>> emptyList() {
        return Collections.emptyList();
    }

    /**
     * Get an immediate value.
     *
     * @param value the value to return
     * @param <T> the value type
     * @return the immediate value
     */
    public static <T> ReadableValue<T> immediateValue(T value) {
        return new ImmediateValue<T>(value);
    }

    private static final ReadableValue EMPTY_LIST_VALUE = new ImmediateValue<List>(Collections.emptyList());

    /**
     * A value which yields the empty list.
     *
     * @param <T> the list member type
     * @return the empty list value
     */
    @SuppressWarnings({ "unchecked" })
    public static <T> ReadableValue<List<T>> emptyListValue() {
        return EMPTY_LIST_VALUE;
    }

    public static <T> List<ReadableValue<? extends T>> immediateValues(List<T> values) {
        final List<ReadableValue<? extends T>> newList = new ArrayList<ReadableValue<? extends T>>(values.size());
        for (T value : values) {
            newList.add(new ImmediateValue<T>(value));
        }
        return newList;
    }

    public static <T> List<ReadableValue<? extends T>> immediateValues(T... values) {
        final List<ReadableValue<? extends T>> newList = new ArrayList<ReadableValue<? extends T>>(values.length);
        for (T value : values) {
            newList.add(new ImmediateValue<T>(value));
        }
        return newList;
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
