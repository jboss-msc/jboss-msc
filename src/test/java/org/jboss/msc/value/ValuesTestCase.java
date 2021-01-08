/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2021, Red Hat, Inc., and individual contributors
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;

import org.jboss.msc.value.util.AnyService;
import org.junit.Test;

/**
 * Test for {@link Values}.
 * 
 * @author <a href="mailto:flavia.rainone@jboss.com">Flavia Rainone</a>
 */
public class ValuesTestCase {

    @Test
    public void getEmptyValues() {
        final ArrayList<Value<Object>> values = new ArrayList<Value<Object>>();
        final Object[] valueArray = Values.getValues(values);
        assertEquals(0, valueArray.length);
    }

    @Test
    public void getUnitaryValues() {
        final Object object = new Object();
        final ArrayList<Value<Object>> values = new ArrayList<Value<Object>>();
        values.add(new ImmediateValue<Object>(object));
        final Object[] valueArray = Values.getValues(values);
        assertEquals(1, valueArray.length);
        assertSame(object, valueArray[0]);
    }

    @Test
    public void getValues() {
        final Collection<String> collection = new HashSet<String>();
        collection.add("anything");
        final Object object = new Object();
        final ArrayList<Value<Object>> values = new ArrayList<Value<Object>>();
        values.add(new ImmediateValue<Object>(collection));
        values.add(new ImmediateValue<Object>(object));
        values.add(new ImmediateValue<Object>(false));

        final Object[] valueArray = Values.getValues(values);
        assertEquals(3, valueArray.length);
        assertSame(collection, valueArray[0]);
        assertSame(object, valueArray[1]);
        assertSame(Boolean.FALSE, valueArray[2]);
    }

    @Test
    public void getValuesFromNullIterable() {
        try {
            Values.getValues((Iterable<Value<?>>) null);
            fail ("NullPointerException e");
        } catch (NullPointerException e) {}
    }
 
    @Test
    public void getEmptyTypedValues() {
        final ArrayList<Value<String>> values = new ArrayList<Value<String>>();
        final String[] valueArray = Values.getValues(values, String.class);
        assertEquals(0, valueArray.length);
    }

    @Test
    public void getUnitaryTypedValues() {
        final Collection<String> collection = new HashSet<String>();
        collection.add("anything");
        final ArrayList<Value<Collection<String>>> values = new ArrayList<Value<Collection<String>>>();
        values.add(new ImmediateValue<Collection<String>>(collection));

        final Object[] valueArray = Values.getValues(values, Object.class);
        assertEquals(1, valueArray.length);
        assertSame(collection, valueArray[0]);
    }

    @Test
    public void getTypedValues() {
        final ArrayList<Value<Number>> values = new ArrayList<Value<Number>>();
        values.add(new ImmediateValue<Number>(1l));
        values.add(new ImmediateValue<Number>(0));
        values.add(new ImmediateValue<Number>(1.5));

        final Number[] valueArray = Values.getValues(values, Number.class);
        assertEquals(3, valueArray.length);
        assertEquals(1l, valueArray[0]);
        assertEquals(0, valueArray[1]);
        assertEquals(1.5, valueArray[2]);
    }

    @Test
    public void getTypedValuesFromNullIterable() {
        try {
            Values.getValues((Iterable<Value<?>>) null, Object.class);
            fail ("NullPointerException e");
        } catch (NullPointerException e) {}
    }
    
    @Test
    public void populateEmptyValues() {
        final ArrayList<Value<String>> values = new ArrayList<Value<String>>();
        final String[] valueArray = new String[0];
        final String[] resultingArray = Values.getValues(values, valueArray);
        assertSame(valueArray, resultingArray);
    }

    @Test
    public void populateUnitaryValues() {
        final Collection<String> collection = new HashSet<String>();
        collection.add("anything");
        final ArrayList<Value<Collection<String>>> values = new ArrayList<Value<Collection<String>>>();
        values.add(new ImmediateValue<Collection<String>>(collection));

        Object[] valueArray = new Object[1];
        Object[] resultingArray = Values.getValues(values, valueArray);
        assertSame(valueArray, resultingArray);
        assertSame(collection, valueArray[0]);
    }

    @Test
    public void populateValues() {
        final ArrayList<Value<Collection<?>>> values = new ArrayList<Value<Collection<?>>>();
        final Collection<?> col1 = new ArrayList<String>();
        values.add(new ImmediateValue<Collection<?>>(col1));
        final Collection<?> col2 = new HashSet<Object>();
        values.add(new ImmediateValue<Collection<?>>(col2));
        final Collection<?> col3 = new ArrayDeque<Integer>();
        values.add(new ImmediateValue<Collection<?>>(col3));
        final Collection<?> col4 = new LinkedList<Object>();
        values.add(new ImmediateValue<Collection<?>>(col4));

        final Collection<?>[] valueArray = new Collection<?>[4];
        final Collection<?>[] resultingArray = Values.getValues(values, valueArray);
        assertSame(valueArray, resultingArray);
        assertEquals(col1, valueArray[0]);
        assertEquals(col2, valueArray[1]);
        assertEquals(col3, valueArray[2]);
        assertEquals(col4, valueArray[3]);
    }

    @Test
    public void populateValuesFromNullIterable() {
        try {
            Values.getValues((Iterable<Value<?>>) null, new Object[10]);
            fail ("NullPointerException e");
        } catch (NullPointerException e) {}
    }

    @Test
    public void populateValuesLargeArraySize() {
        final ArrayList<Value<File>> values = new ArrayList<Value<File>>();
        final File file1 = new File("path1");
        values.add(new ImmediateValue<File>(file1));
        final File file2 = new File("path2");
        values.add(new ImmediateValue<File>(file2));

        final File[] valueArray = new File[3];
        final File[] resultingArray = Values.getValues(values, valueArray);
        assertSame(valueArray, resultingArray);
        assertEquals(file1, valueArray[0]);
        assertEquals(file2, valueArray[1]);
        assertNull(valueArray[2]);
    }

    @Test
    public void populateValuesSmallArraySize() {
        final ArrayList<Value<File>> values = new ArrayList<Value<File>>();
        final File file1 = new File("path1");
        values.add(new ImmediateValue<File>(file1));
        final File file2 = new File("path2");
        values.add(new ImmediateValue<File>(file2));

        final File[] valueArray = new File[1];
        try {
            Values.getValues(values, valueArray);
            fail("IndexOutOfBoundsException expected");
        } catch (IndexOutOfBoundsException e) {}
    }

    @Test
    public void nullValue() {
        final Value<Object> value = Values.nullValue();
        assertNull(value.getValue());
        assertNull(value.getValue());
        assertNull(value.getValue());
    }

    @Test
    public void cacheValue() {
        final Value<Class<? extends AnyService>> value = new ClassOfValue<AnyService>(new ImmediateValue<AnyService>(new AnyService()));
        final Value<?> cachedValue = Values.cached(value);
        assertNotSame(value, cachedValue);
        assertTrue(cachedValue instanceof CachedValue);
        assertSame(AnyService.class, cachedValue.getValue());
    }

    @Test
    public void cacheImmediateValue() {
        final Value<?> value = new ImmediateValue<Object>(new Object());
        final Value<?> cachedValue = Values.cached(value);
        assertSame(value, cachedValue);
    }

    @Test
    public void cacheCachedValue() {
        final Value<String> cachedValue = new CachedValue<String>(new ImmediateValue<String>("value"));
        final Value<?> cachedCachedValue = Values.cached(cachedValue);
        assertSame(cachedValue, cachedCachedValue);
    }

    @Test
    public void emptyList() {
        assertTrue(Values.EMPTY_LIST.isEmpty());
        assertTrue(Values.emptyList().isEmpty());
    }

    @Test
    public void emptyTypeList() {
        assertTrue(Values.EMPTY_TYPE_LIST.isEmpty());
    }

    @Test
    public void emptyListValue() {
        final Value<List<Object>> value = Values.emptyListValue();
        assertNotNull(value);
        final List<Object> list = value.getValue();
        assertNotNull(list);
        assertTrue(list.isEmpty());
    }

    @Test
    public void immediateValue() {
        final Object object = new Object();
        final Value<Object> value = Values.immediateValue(object);
        assertSame(object, value.getValue());
        assertTrue(value instanceof ImmediateValue);
    }

    @Test
    public void immediateValues() {
        final List<String> list = new ArrayList<String>();
        list.add("value1");
        list.add("value2");
        list.add("value3");
        final List<Value<? extends String>> values = Values.immediateValues(list);
        assertNotNull(values);
        assertEquals(3, values.size());
        assertEquals("value1", values.get(0).getValue());
        assertEquals("value2", values.get(1).getValue());
        assertEquals("value3", values.get(2).getValue());
    }

    @Test
    public void emptyImmediateValues() {
        final List<String> list = new ArrayList<String>();
        final List<Value<? extends String>> values = Values.immediateValues(list);
        assertNotNull(values);
        assertTrue(values.isEmpty());
    }

    @Test
    public void nullImmediateValues() {
        try {
            Values.immediateValues((List<?>) null);
            fail("NullPointerException expected");
        } catch (NullPointerException e) {}
    }

    @Test
    public void immediateValuesWithVarargs() {
        final List<Value<? extends String>> values = Values.immediateValues("value4", "value5", "value6");
        assertNotNull(values);
        assertEquals(3, values.size());
        assertEquals("value4", values.get(0).getValue());
        assertEquals("value5", values.get(1).getValue());
        assertEquals("value6", values.get(2).getValue());
    }

    @Test
    public void emptyImmediateValuesWithVarargsAndRecast() {
        final List<Value<? extends String>> values = Values.immediateValues();
        assertNotNull(values);
        assertTrue(values.isEmpty());
    }

    @Test
    public void asSuperclass() {
        final Value<? extends String> value = new ImmediateValue<String>("string");
        final Value<String> recastValue = Values.asSuperclass(value);
        assertSame(value, recastValue);
    }

    @Test
    public void thisValue() {
        final ThreadLocalValue<Object> this1 = Values.thisValue();
        final ThreadLocalValue<Object> this2 = Values.thisValue();
        assertSame(this1, this2); // makes sure that the same ThreadLocalValue is consistently returned
    }

    @Test
    public void injectedValue() {
        final ThreadLocalValue<Object> injected1 = Values.injectedValue();
        final ThreadLocalValue<Object> injected2 = Values.injectedValue();
        assertSame(injected1, injected2); // makes sure that the same ThreadLocalValue is consistently returned
    }
}
