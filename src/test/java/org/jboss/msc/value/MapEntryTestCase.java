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
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

/**
 * Test for {@link MapEntry}.
 * 
 * @author <a href="mailto:flavia.rainone@jboss.com">Flavia Rainone</a>
 */
@SuppressWarnings("unchecked")
public class MapEntryTestCase {

    private Object key1, key2, value1, value2;
    private MapEntry<Object, Object> mapEntry1, mapEntry2, mapEntry3, mapEntry4;

    @Before
    public void setUp() {
        key1 = new Object();
        value1 = new Object();
        mapEntry1 = new MapEntry<Object, Object>(key1, value1);
        key2 = new Object();
        value2 = new Object();
        mapEntry2 = MapEntry.entry(key2, value2);
        mapEntry3 = MapEntry.entry(key1, value2);
        mapEntry4 = MapEntry.entry(key2, value1);
    }

    @Test
    public void mapEntry1() {
        assertSame(key1, mapEntry1.getKey());
        assertSame(value1, mapEntry1.getValue());
        int hashCode1 = mapEntry1.hashCode();
        assertEquals(hashCode1, mapEntry1.hashCode());
        assertEquals(hashCode1, mapEntry1.hashCode());
        assertEquals(hashCode1, mapEntry1.hashCode());
    }

    @Test
    public void mapEntry2() {
        assertSame(key2, mapEntry2.getKey());
        assertSame(value2, mapEntry2.getValue());
        int hashCode2 = mapEntry2.hashCode();
        assertEquals(hashCode2, mapEntry2.hashCode());
        assertEquals(hashCode2, mapEntry2.hashCode());
        assertEquals(hashCode2, mapEntry2.hashCode());
    }

    @Test
    public void mapEntry3() {
        assertSame(key1, mapEntry3.getKey());
        assertSame(value2, mapEntry3.getValue());
        int hashCode3 = mapEntry3.hashCode();
        assertEquals(hashCode3, mapEntry3.hashCode());
        assertEquals(hashCode3, mapEntry3.hashCode());
        assertEquals(hashCode3, mapEntry3.hashCode());
    }

    @Test
    public void mapEntry4() {
        assertSame(key2, mapEntry4.getKey());
        assertSame(value1, mapEntry4.getValue());
        int hashCode4 = mapEntry4.hashCode();
        assertEquals(hashCode4, mapEntry4.hashCode());
        assertEquals(hashCode4, mapEntry4.hashCode());
        assertEquals(hashCode4, mapEntry4.hashCode());
    }

    @Test
    public void addToMap() {
        final Map<Object, Object> map = new HashMap<Object, Object>();
        MapEntry.addTo(map, mapEntry1, mapEntry2);
        assertEquals(2, map.size());
        assertSame(value1, map.get(key1));
        assertSame(value2, map.get(key2));

        MapEntry.<Object, Object>addTo(map, mapEntry3);
        assertEquals(2, map.size());
        assertSame(value2, map.get(key1));
        assertSame(value2, map.get(key2));

        MapEntry.<Object, Object>addTo(map, mapEntry4);
        assertEquals(2, map.size());
        assertSame(value2, map.get(key1));
        assertSame(value1, map.get(key2));
    }

    @Test
    public void addNothingToMap() {
        final Map<Object, Object> map = new HashMap<Object, Object>();
        MapEntry.addTo(map);
        assertTrue(map.isEmpty());
    }

    @Test
    public void addToNullMap() {
        MapEntry.addTo(null);
        try {
            MapEntry.addTo(null, mapEntry4);
            fail("NullPointerException expected");
        } catch (NullPointerException e) {}
    }

    @Test
    public void equals() {
        assertFalse(mapEntry1.equals(mapEntry2));
        assertFalse(mapEntry1.hashCode() == mapEntry2.hashCode());
        assertFalse(mapEntry1.equals(mapEntry3));
        assertFalse(mapEntry1.hashCode() == mapEntry3.hashCode());
        assertFalse(mapEntry1.equals(mapEntry4));
        assertFalse(mapEntry1.hashCode() == mapEntry4.hashCode());
        assertFalse(mapEntry2.equals(mapEntry3));
        assertFalse(mapEntry2.hashCode() == mapEntry3.hashCode());
        assertFalse(mapEntry2.equals(mapEntry4));
        assertFalse(mapEntry2.hashCode() == mapEntry4.hashCode());
        assertFalse(mapEntry3.equals(mapEntry4));
        assertFalse(mapEntry3.hashCode() == mapEntry4.hashCode());
    }

    @Test
    public void equalsNonMapEntries() {
        assertFalse(mapEntry1.equals(false));
        assertFalse(mapEntry1.equals(null));
        assertFalse(mapEntry1.equals("non map entry"));
    }

    @Test
    public void setValue() {
        try {
            mapEntry1.setValue(value2);
            fail("UnsupportedOperationException expected");
        } catch (UnsupportedOperationException e) {}
        try {
            mapEntry3.setValue(value1);
            fail("UnsupportedOperationException expected");
        } catch (UnsupportedOperationException e) {}
    }
}
