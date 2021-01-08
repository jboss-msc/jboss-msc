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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;

/**
 * Tests for {@link ListItemValue}, {@link ListValue}, {@link MapEntryValue}, {@link MapItemValue}, {@link MapValue}, and
 * {@link MapEntry}.
 * 
 * @author <a href="mailto:flavia.rainone@jboss.com">Flavia Rainone</a>
 */
public class CollectionRelatedValueTestCase {

    @Test
    public void listItemValue() {
        final List<String> list = new ArrayList<String>();
        list.add("one");
        list.add("two");
        list.add("three");
        final Value<List<? extends String>> listValue = new ImmediateValue<List<? extends String>>(list);
        final Value<String> indexMinus1 = new ListItemValue<String>(listValue, new ImmediateValue<Integer>(-1));
        try {
            indexMinus1.getValue();
            fail("Index -1 list value should have thrown ArrayIndexOutOfBoundsException");
        } catch (IndexOutOfBoundsException e) {}

        final Value<String> indexZero = new ListItemValue<String>(listValue, new ImmediateValue<Integer>(0));
        assertEquals("one", indexZero.getValue());

        final Value<String> indexOne = new ListItemValue<String>(listValue, new ImmediateValue<Integer>(1));
        assertEquals("two", indexOne.getValue());

        final Value<String> indexTwo = new ListItemValue<String>(listValue, new ImmediateValue<Integer>(2));
        assertEquals("three", indexTwo.getValue());

        final Value<String> indexThree = new ListItemValue<String>(listValue, new ImmediateValue<Integer>(3));
        try {
            indexThree.getValue();
            fail("Index 3 list value should have thrown ArrayIndexOutOfBoundsException");
        } catch (IndexOutOfBoundsException e) {}

        final Value<String> indexNull = new ListItemValue<String>(listValue, Values.<Integer>nullValue());
        try {
            indexNull.getValue();
            fail("Index null list value should have thrown NullPointerException");
        } catch (NullPointerException e) {}
    }

    @Test
    public void nullListItemValue() {
        Value<?> listItemValue = new ListItemValue<Object>(Values.<List<?>>nullValue(), new ImmediateValue<Integer>(0));
        try {
            listItemValue.getValue();
            fail("ListItemValue with null list expected to have thrown NullPointerException");
        } catch (NullPointerException e) {}

        listItemValue = new ListItemValue<Object>(Values.<List<?>>nullValue(), new ImmediateValue<Integer>(null));
        try {
            listItemValue.getValue();
            fail("ListItemValue with null list/index expected to have thrown NullPointerException");
        } catch (NullPointerException e) {}
    }

    @Test
    public void illegalListItemValue() {
        try {
            new ListItemValue<Object>(null, new ImmediateValue<Integer>(0));
            fail("ListItemValue with null list value expected to have thrown IllegalArgumentException");
        } catch (IllegalArgumentException e) {}

        try {
            new ListItemValue<Object>(new ImmediateValue<List<?>>(new ArrayList<Object>()), (Value<Number>)null);
            fail("ListItemValue with null index expected to have thrown IllegalArgumentException");
        } catch (IllegalArgumentException e) {}

        try {
            new ListItemValue<Object>(null, null);
            fail("ListItemValue with null list value/index value expected to have thrown IllegalArgumentException");
        } catch (IllegalArgumentException e) {}
    }

    @Test
    public void listValue() {
        final List<Value<String>> list = new ArrayList<Value<String>>(); 
        final Value<List<String>> listValue = new ListValue<String>(list);
        assertTrue(listValue.getValue().isEmpty());

        list.add(new ImmediateValue<String>("item one"));
        List<String> resultingList = listValue.getValue();
        assertEquals(1, resultingList.size());
        assertEquals("item one", resultingList.get(0));

        list.add(new ImmediateValue<String>("item two"));
        resultingList = listValue.getValue();
        assertEquals(2, resultingList.size());
        assertEquals("item one", resultingList.get(0));
        assertEquals("item two", resultingList.get(1));

        list.add(new ImmediateValue<String>("item three"));
        resultingList = listValue.getValue();
        assertEquals(3, resultingList.size());
        assertEquals("item one", resultingList.get(0));
        assertEquals("item two", resultingList.get(1));
        assertEquals("item three", resultingList.get(2));

        list.remove(1);
        resultingList = listValue.getValue();
        assertEquals(2, resultingList.size());
        assertEquals("item one", resultingList.get(0));
        assertEquals("item three", resultingList.get(1));

        assertEquals(listValue.getValue(), listValue.getValue());
    }

    @Test
    public void nullListValue() {
        final Value<List<String>> listValue = new ListValue<String>(null);
        try {
            listValue.getValue();
            fail("getValue should have thrown NullPointerException");
        } catch (NullPointerException e) {}
    }

    @Test
    public void mapEntryValue() {
        final MapEntry<Value<Integer>, Value<String>> mapEntry = new MapEntry<Value<Integer>, Value<String>>(
                new ImmediateValue<Integer>(1), new ImmediateValue<String>("first"));
        final Value<MapEntry<Integer, String>> value = new MapEntryValue<Integer, String>(mapEntry);
        final MapEntry<Integer, String> resultingEntry = value.getValue();
        assertNotNull(resultingEntry);
        assertEquals(1, (int) resultingEntry.getKey());
        assertEquals("first", resultingEntry.getValue());
        assertEquals(resultingEntry, value.getValue());
    }

    @Test
    public void incrementMapEntryValue() {
        // key is a plain object
        final Object entryKey = new Object();
        // value should return a different number every time, from 0 to 2; next, it returns null;
        // on subsequent calls, a runtime exception is returned;
        final Value<Short> entryValue = new Value<Short>() {
            private short count = -1;
            public Short getValue() {
                if (++ count > 2) {
                    if (count == 3) {
                        return null;
                    }
                    throw new RuntimeException("count cannot be larger than 3");
                }
                return count;
            }
        };
        // create map entry value
        final Value<MapEntry<Object, Short>> value = MapEntryValue.of(new ImmediateValue<Object>(entryKey), entryValue);
        final MapEntry<Object, Short> resultingEntry0 = value.getValue();
        assertNotNull(resultingEntry0);
        assertSame(entryKey, resultingEntry0.getKey());
        assertEquals(0, (short) resultingEntry0.getValue());

        final MapEntry<Object, Short> resultingEntry1 = value.getValue();
        Assert.assertFalse(resultingEntry0.equals(resultingEntry1));
        assertNotNull(resultingEntry1);
        assertSame(entryKey, resultingEntry1.getKey());
        assertEquals(1, (short) resultingEntry1.getValue());

        final MapEntry<Object, Short> resultingEntry2 = value.getValue();
        Assert.assertFalse(resultingEntry0.equals(resultingEntry2));
        Assert.assertFalse(resultingEntry1.equals(resultingEntry2));
        assertNotNull(resultingEntry2);
        assertSame(entryKey, resultingEntry2.getKey());
        assertEquals(2, (short) resultingEntry2.getValue());

        final MapEntry<Object, Short> resultingEntry3 = value.getValue();
        Assert.assertFalse(resultingEntry0.equals(resultingEntry3));
        Assert.assertFalse(resultingEntry1.equals(resultingEntry3));
        Assert.assertFalse(resultingEntry2.equals(resultingEntry3));
        assertNotNull(resultingEntry3);
        assertSame(entryKey, resultingEntry3.getKey());
        assertNull(resultingEntry3.getValue());

        try {
            value.getValue();
            fail("RuntimeException expected");
        } catch (RuntimeException e) {
            assertEquals("Unexpected exception", "count cannot be larger than 3", e.getMessage());
        }
    }

    @Test
    public void illegalMapEntryValue() {
        final MapEntry<Value<Object>, Value<Object>> mapEntry = new MapEntry<Value<Object>, Value<Object>>(null, null);
        final Value<MapEntry<Object, Object>> value = new MapEntryValue<Object, Object>(mapEntry);
        try {
            value.getValue();
            fail("NullPointerException is expected");
        } catch (NullPointerException e) {}
    }

    @Test
    public void mapItemValue() {
        final Map<String, String> addresses = new HashMap<String, String>();
        final String johnAddress = "412 Main St";
        final String janeAddress = "11000 East International Blvd Suite 10";
        addresses.put("John Doe", "412 Main St");
        addresses.put("Jane Doe", janeAddress);

        final Value<Map<String, String>> addressesValue = new ImmediateValue<Map<String, String>>(addresses);
        final Value<String> johnAddressValue = new MapItemValue<String>(new ImmediateValue<String>("John Doe"), addressesValue);
        assertEquals(johnAddress, johnAddressValue.getValue());

        final Value<String> janeAddressValue = new MapItemValue<String>(new ImmediateValue<String>("Jane Doe"), addressesValue);
        assertEquals(janeAddress, janeAddressValue.getValue());

        final Value<String> inexistentAddressValue = new MapItemValue<String>(new ImmediateValue<String>("James Doe"), addressesValue);
        assertNull(inexistentAddressValue.getValue());
    }

    @Test
    public void nullMapItemValue() {
        final Value<?> mapItemValue = new MapItemValue<String>(new ImmediateValue<String>("key"), Values.<Map<String, String>>nullValue());
        try {
            mapItemValue.getValue();
            fail("NullPointerException expected");
        } catch (NullPointerException e) {}
    }

    @Test
    public void illegalMapItemValue() {
        try {
            new MapItemValue<String>(new ImmediateValue<String>("key"), null);
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException e) {}

        try {
            new MapItemValue<String>(null, new ImmediateValue<Map<String, String>>(new HashMap<String, String>()));
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException e) {}

        try {
            new MapItemValue<String>(null, null);
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException e) {}
    }

    @Test
    public void mapValue() {
        final Map<Integer, String> map = new HashMap<Integer, String>();
        map.put(1000, "one thousand");
        map.put(1001, "one thousand and one");
        map.put(1010, "one thousand and ten");
        map.put(1100, "one thousand and one hundred");
        map.put(1101, "one thousand and one hundred and one");
        final Value<Map<Integer, String>> mapValue = new MapValue<Integer, String>(new ImmediateValue<Map<Integer, String>>(map));
        final Map<Integer, String> resultingMap = mapValue.getValue();
        assertNotNull(resultingMap);
        assertEquals(5, resultingMap.size());
        assertEquals("one thousand", resultingMap.get(1000));
        assertEquals("one thousand and one", resultingMap.get(1001));
        assertEquals("one thousand and ten", resultingMap.get(1010));
        assertEquals("one thousand and one hundred", resultingMap.get(1100));
        assertEquals("one thousand and one hundred and one", resultingMap.get(1101));
    }

    @Test
    public void mapValueWithEntryList() {
        final Value<Map<Integer, String>> map = new ImmediateValue<Map<Integer, String>>(new HashMap<Integer, String>());
        final List<MapEntry<? extends Value<? extends Integer>, ? extends Value<? extends String>>> entryList =
            new ArrayList<MapEntry<? extends Value<? extends Integer>, ? extends Value<? extends String>>>();
        entryList.add(new MapEntry<Value<Integer>, Value<String>>(
                new ImmediateValue<Integer>(1000), new ImmediateValue<String>("one thousand")));
        entryList.add(new MapEntry<Value<Integer>, Value<String>>(
                new ImmediateValue<Integer>(1001), new ImmediateValue<String>("one thousand and one")));
        entryList.add(new MapEntry<Value<Integer>, Value<String>>(
                new ImmediateValue<Integer>(1010), new ImmediateValue<String>("one thousand and ten")));
        entryList.add(new MapEntry<Value<Integer>, Value<String>>(
                new ImmediateValue<Integer>(1100), new ImmediateValue<String>("one thousand and one hundred")));
        entryList.add(new MapEntry<Value<Integer>, Value<String>>(
                new ImmediateValue<Integer>(1101), new ImmediateValue<String>("one thousand and one hundred and one")));
        final Value<Map<Integer, String>> mapValue = new MapValue<Integer, String>(map, entryList);
        final Map<Integer, String> resultingMap = mapValue.getValue();
        assertNotNull(resultingMap);
        assertEquals(5, resultingMap.size());
        assertEquals("one thousand", resultingMap.get(1000));
        assertEquals("one thousand and one", resultingMap.get(1001));
        assertEquals("one thousand and ten", resultingMap.get(1010));
        assertEquals("one thousand and one hundred", resultingMap.get(1100));
        assertEquals("one thousand and one hundred and one", resultingMap.get(1101));
    }

    @Test
    public void mapValueVaragsConstructor() {
        final Value<Map<Integer, String>> map = new ImmediateValue<Map<Integer, String>>(new HashMap<Integer, String>());
        @SuppressWarnings("unchecked")
        final Value<Map<Integer, String>> mapValue = new MapValue<Integer, String>(map, 
                new MapEntry<Value<Integer>, Value<String>>(
                        new ImmediateValue<Integer>(1000), new ImmediateValue<String>("one thousand")),
                        new MapEntry<Value<Integer>, Value<String>>(
                                new ImmediateValue<Integer>(1001), new ImmediateValue<String>("one thousand and one")),
                                new MapEntry<Value<Integer>, Value<String>>(
                                        new ImmediateValue<Integer>(1010), new ImmediateValue<String>("one thousand and ten")),
                                        new MapEntry<Value<Integer>, Value<String>>(
                                                new ImmediateValue<Integer>(1100), new ImmediateValue<String>("one thousand and one hundred")),
                                                new MapEntry<Value<Integer>, Value<String>>(
                                                        new ImmediateValue<Integer>(1101), new ImmediateValue<String>("one thousand and one hundred and one")));
        final Map<Integer, String> resultingMap = mapValue.getValue();
        assertNotNull(resultingMap);
        assertEquals(5, resultingMap.size());
        assertEquals("one thousand", resultingMap.get(1000));
        assertEquals("one thousand and one", resultingMap.get(1001));
        assertEquals("one thousand and ten", resultingMap.get(1010));
        assertEquals("one thousand and one hundred", resultingMap.get(1100));
        assertEquals("one thousand and one hundred and one", resultingMap.get(1101));
    }

    @Test
    public void overrideMapValueWithNewEntries() {
        final Map<Integer, String> map = new HashMap<Integer, String>();
        map.put(1000, "one thousand");
        map.put(1001, "one thousand and one");
        map.put(1010, "one thousand and ten");
        map.put(1100, "one thousand and one hundred");
        map.put(1101, "one thousand and one hundred and one");
        // creat a map value that will overwrite the entry (1000, "one thousand")
        @SuppressWarnings("unchecked")
        Value<Map<Integer, String>> mapValue = new MapValue<Integer, String>(
                new ImmediateValue<Map<Integer, String>>(map), new MapEntry<Value<Integer>, Value<String>>(
                        new ImmediateValue<Integer>(1000), new ImmediateValue<String>("a thousand")));
        Map<Integer, String> resultingMap = mapValue.getValue();
        assertNotNull(resultingMap);
        assertEquals(5, resultingMap.size());
        assertEquals("a thousand", resultingMap.get(1000));
        assertEquals("a thousand", map.get(1000));// value also changes in original map
        assertEquals("one thousand and one", resultingMap.get(1001));
        assertEquals("one thousand and ten", resultingMap.get(1010));
        assertEquals("one thousand and one hundred", resultingMap.get(1100));
        assertEquals("one thousand and one hundred and one", resultingMap.get(1101));
    }

    @Test
    public void nullMapValue() {
        @SuppressWarnings("unchecked")
        Value<Map<Integer, String>> mapValue = new MapValue<Integer, String>(Values.<Map<Integer, String>>nullValue(),
                new MapEntry<Value<Integer>, Value<String>>(
                        new ImmediateValue<Integer>(1000), new ImmediateValue<String>("a thousand")));
        try {
            mapValue.getValue();
            fail("NullPointerException expected");
        } catch (NullPointerException e) {}

        mapValue = new MapValue<Integer, String>(Values.<Map<Integer, String>>nullValue());
        assertNull(mapValue.getValue());
    }

    @Test @SuppressWarnings("unchecked")
    public void illegalArgumentMapValue() {
        final Value<Map<Integer, String>> mapValue = new MapValue<Integer, String>(null, new MapEntry<Value<Integer>, Value<String>>(
                new ImmediateValue<Integer>(1000), new ImmediateValue<String>("a thousand")));
        try {
            mapValue.getValue();
            fail ("NullPointerException expected");
        } catch (NullPointerException e) {}
    }

    @Test
    public void setValue() {
        final List<Value<Float>> list = new ArrayList<Value<Float>>();
        list.add(new ImmediateValue<Float>(2f));
        list.add(new ImmediateValue<Float>(2.2f));
        list.add(new ImmediateValue<Float>(2.22f));
        list.add(new ImmediateValue<Float>(2.222f));
        final Value<Set<Float>> value = new SetValue<Float>(list);

        final Set<Float> resultingSet = value.getValue();
        assertNotNull(resultingSet);
        assertEquals(4, resultingSet.size());
        final List<Float> expected = new ArrayList<Float>();
        expected.add(2f);
        expected.add(2.2f);
        expected.add(2.22f);
        expected.add(2.222f);
        for (float resulting: resultingSet) {
            for (Iterator<Float> expectedIterator = expected.iterator(); expectedIterator.hasNext();) {
                if (expectedIterator.next() - resulting < 0.001) {
                    expectedIterator.remove();
                }
            }
        }
        assertTrue("Unexpected float values", expected.isEmpty());
    }

    @Test
    public void emptySetValue() {
        final List<Value<Float>> list = new ArrayList<Value<Float>>();
        final Value<Set<Float>> value = new SetValue<Float>(list);
        final Set<Float> resultingSet = value.getValue();
        assertNotNull(resultingSet);
        assertTrue(resultingSet.isEmpty());
    }

    @Test 
    public void nullSetValue() {
        final Value<Set<Float>> value = new SetValue<Float>(null);
        try {
            value.getValue();
            fail("NullPointerException expected");
        } catch (NullPointerException e) {};
    }

    @Test
    public void mapEntry() {
        final MapEntry<Integer, Boolean> mapEntry1 = new MapEntry<Integer, Boolean>(1, true);
        MapEntry<Integer, Boolean> mapEntry2 = new MapEntry<Integer, Boolean>(2, false);
        assertFalse(mapEntry1.equals(mapEntry2));
        assertFalse(mapEntry2.equals(mapEntry1));
        assertFalse(mapEntry2.equals((Object) mapEntry1));
        assertFalse(mapEntry1.equals(new Object()));
        assertEquals(mapEntry1, mapEntry1);
        assertEquals(mapEntry2, mapEntry2);
        assertTrue(mapEntry2.equals(mapEntry2));

        final MapEntry<Integer, Boolean> mapEntry3 = new MapEntry<Integer, Boolean>(1, false);
        assertFalse(mapEntry3.equals(mapEntry1));
        assertFalse(mapEntry3.equals(mapEntry2));
        assertEquals(mapEntry3, mapEntry3);
        int hashCode1 = mapEntry1.hashCode();
        int hashCode2 = mapEntry2.hashCode();
        int hashCode3 = mapEntry3.hashCode();
        assertEquals(hashCode1, mapEntry1.hashCode());
        assertEquals(hashCode2, mapEntry2.hashCode());
        assertEquals(hashCode3, mapEntry3.hashCode());
        assertFalse(hashCode1 == hashCode2);
        assertFalse(hashCode1 == hashCode3);
        assertFalse(hashCode2 == hashCode3);

        final MapEntry<Integer, Boolean> mapEntry4 = new MapEntry<Integer, Boolean>(1, false);
        int hashCode4 = mapEntry4.hashCode();
        assertFalse(hashCode4 == hashCode1);
        assertTrue(hashCode4 == hashCode3);
        assertEquals(mapEntry4, mapEntry3);
        assertEquals(mapEntry4, mapEntry4);

        final MapEntry<Integer, Boolean> mapEntry5 = new MapEntry<Integer, Boolean>(null, true);
        int hashCode5 = mapEntry5.hashCode();
        assertEquals(hashCode5, mapEntry5.hashCode());
        assertFalse(mapEntry1.equals(mapEntry5) || mapEntry5.equals(mapEntry1) || hashCode1 == hashCode5);
        assertFalse(mapEntry2.equals(mapEntry5) || mapEntry5.equals(mapEntry2) || hashCode2 == hashCode5);
        assertFalse(mapEntry3.equals(mapEntry5) || mapEntry5.equals(mapEntry3) || hashCode3 == hashCode5);
        assertFalse(mapEntry4.equals(mapEntry5) || mapEntry5.equals(mapEntry4) || hashCode4 == hashCode5);
        assertEquals(mapEntry5, mapEntry5);

        final MapEntry<Integer, Boolean> mapEntry6 = new MapEntry<Integer, Boolean>(7, null);
        int hashCode6 = mapEntry6.hashCode();
        assertEquals(hashCode6, mapEntry6.hashCode());
        assertFalse(mapEntry1.equals(mapEntry6) || mapEntry6.equals(mapEntry1) || hashCode1 == hashCode6);
        assertFalse(mapEntry2.equals(mapEntry6) || mapEntry6.equals(mapEntry2) || hashCode2 == hashCode6);
        assertFalse(mapEntry3.equals(mapEntry6) || mapEntry6.equals(mapEntry3) || hashCode3 == hashCode6);
        assertFalse(mapEntry4.equals(mapEntry6) || mapEntry6.equals(mapEntry4) || hashCode4 == hashCode6);
        assertFalse(mapEntry5.equals(mapEntry6) || mapEntry6.equals(mapEntry5) || hashCode5 == hashCode6);
        assertEquals(mapEntry6, mapEntry6);

        final MapEntry<Integer, Boolean> mapEntry7 = new MapEntry<Integer, Boolean>(null, null);
        int hashCode7 = mapEntry7.hashCode();
        assertEquals(hashCode7, mapEntry7.hashCode());
        assertFalse(mapEntry1.equals(mapEntry7) || mapEntry7.equals(mapEntry1) || hashCode1 == hashCode7);
        assertFalse(mapEntry2.equals(mapEntry7) || mapEntry7.equals(mapEntry2) || hashCode2 == hashCode7);
        assertFalse(mapEntry3.equals(mapEntry7) || mapEntry7.equals(mapEntry3) || hashCode3 == hashCode7);
        assertFalse(mapEntry4.equals(mapEntry7) || mapEntry7.equals(mapEntry4) || hashCode4 == hashCode7);
        assertFalse(mapEntry5.equals(mapEntry7) || mapEntry7.equals(mapEntry5) || hashCode5 == hashCode7);
        assertFalse(mapEntry6.equals(mapEntry7) || mapEntry7.equals(mapEntry6) || hashCode6 == hashCode7);
        assertEquals(mapEntry7, mapEntry7);

        assertFalse(mapEntry1.equals((MapEntry<?, ?>) null));
        assertFalse(mapEntry2.equals((MapEntry<?, ?>)null));
        assertFalse(mapEntry3.equals((MapEntry<?, ?>)null));
        assertFalse(mapEntry4.equals((MapEntry<?, ?>)null));
        assertFalse(mapEntry5.equals((MapEntry<?, ?>)null));
        assertFalse(mapEntry6.equals((MapEntry<?, ?>)null));
        assertFalse(mapEntry7.equals((MapEntry<?, ?>)null));
    }
}
