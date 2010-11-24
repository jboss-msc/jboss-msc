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

package org.jboss.msc.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Set;

import org.junit.Test;

/**
 * Test for {@link IdentityHashSet}.
 * 
 * @author <a href="mailto:flavia.rainone@jboss.com">Flavia Rainone</a>
 */
public class IdentityHashSetTestCase {

    @Test
    public void testEmptyHashSet() {
        IdentityHashSet<String> set = new IdentityHashSet<String>(0);
        assertTrue(set.isEmpty());
        set = new IdentityHashSet<String>(1000);
        assertTrue(set.isEmpty());
        set = new IdentityHashSet<String>(3, 0.5f);
        assertTrue(set.isEmpty());
        assertFalse(set.contains("entry"));
        set.clear();
        assertTrue(set.isEmpty());
        //set.printDebugStats();
    }

    @Test
    public void populateHashSet() {
        final IdentityHashSet<String> set = new IdentityHashSet<String>(0);
        assertTrue(set.isEmpty());
        set.add("new entry");
        assertEquals(1, set.size());
        final Collection<String> entries = new ArrayList<String>();
        entries.add("entry1");
        entries.add("entry2");
        entries.add("entry3");
        entries.add("entry4");
        entries.add("entry5");
        entries.add("entry6");
        entries.add("new entry");
        set.addAll(entries);
        assertEquals(7, set.size());
        assertTrue(set.contains("entry1"));
        assertTrue(set.contains("entry2"));
        assertTrue(set.contains("entry3"));
        assertTrue(set.contains("entry4"));
        assertTrue(set.contains("entry5"));
        assertTrue(set.contains("entry6"));
        assertFalse(set.contains("entry7"));
        assertTrue(set.contains("new entry"));
        try {
            set.add(null);
            fail("NullPointerException expected");
        } catch (NullPointerException e) {}
        //set.printDebugStats();
    }

    @Test
    public void removal() {
        final Set<Integer> set = new IdentityHashSet<Integer>(0);
        assertTrue(set.isEmpty());
        set.add(2);
        set.add(4);
        set.add(8);
        set.add(16);
        set.add(32);
        set.add(64);
        assertEquals(6, set.size());
        assertTrue(set.remove(32));
        assertEquals(5, set.size());
        assertFalse(set.contains(32));
        assertFalse(set.remove(32));
        assertFalse(set.remove(null));
        assertTrue(set.remove(2));
        assertEquals(4, set.size());
        assertFalse(set.contains(2));
        assertFalse(set.remove(2));
    }

    @Test
    public void toArray() {
        final IdentityHashSet<Integer> set = new IdentityHashSet<Integer>(0);
        assertTrue(set.isEmpty());
        set.add(2);
        set.add(4);
        set.add(8);
        final Object[] toArray = set.toArray();
        for (int i = 0; i < 3; i++) {
            assertTrue(toArray[i].equals(2) || toArray[i].equals(4) || toArray[i].equals(8));
        }
    }

    @Test
    public void toTypedArray() {
        final IdentityHashSet<Integer> set = new IdentityHashSet<Integer>(0);
        assertTrue(set.isEmpty());
        set.add(16);
        set.add(32);
        final Integer[] array = set.toArray(new Integer[]{2, 4, 8, null, null}, 3, 2);
        assertEquals(2, (int) array[0]);
        assertEquals(4, (int) array[1]);
        assertEquals(8, (int) array[2]);
        assertTrue((array[3].equals(16) && array[4].equals(32)) || (array[3].equals(32) && array[4].equals(16)));
    }

    @Test
    public void illegalToTypedArray() {
        final IdentityHashSet<Integer> set = new IdentityHashSet<Integer>(0);
        assertTrue(set.isEmpty());
        set.add(16);
        set.add(32);
        try {
            set.toArray(new Integer[]{2, 4, 8, null, null}, 3, 3);
            fail("AssertionError expected");
        } catch (AssertionError e) {}
    }

    @Test
    public void copySet() {
        final Set<String> set = new HashSet<String>();
        set.add("entry1");
        set.add("entry2");
        set.add("entry3");
        final Set<String> identitySet = new IdentityHashSet<String>(set);
        assertFalse(identitySet.isEmpty());
        assertEquals(3, identitySet.size());
        assertTrue(identitySet.contains("entry1"));
        assertTrue(identitySet.contains("entry2"));
        assertTrue(identitySet.contains("entry3"));
        assertFalse(identitySet.contains("entry4"));
        assertFalse(identitySet.contains(null));
    }

    @Test
    public void cloneSet() {
        final IdentityHashSet<String> set1 = new IdentityHashSet<String>();
        set1.add("entry1");
        set1.add("entry2");
        set1.add("entry3");
        final IdentityHashSet<String> set2 = set1.clone();
        assertFalse(set2.isEmpty());
        assertEquals(3, set2.size());
        assertTrue(set2.contains("entry1"));
        assertTrue(set2.contains("entry2"));
        assertTrue(set2.contains("entry3"));
        assertFalse(set2.contains("entry4"));
        assertFalse(set2.contains(null));
    }

    @Test
    public void readWriteSet() throws Exception {
        final IdentityHashSet<Integer> set = new IdentityHashSet<Integer>(0);
        set.add(16);
        set.add(32);
        final ByteArrayOutputStream byteArrayStream = new ByteArrayOutputStream();
        final ObjectOutputStream output = new ObjectOutputStream(byteArrayStream);
        output.writeObject(set);
        final ObjectInputStream input = new ObjectInputStream(new ByteArrayInputStream(byteArrayStream.toByteArray()));
        @SuppressWarnings("unchecked")
        final IdentityHashSet<Integer> readSet = (IdentityHashSet<Integer>) input.readObject();
        assertNotNull(readSet);
        assertEquals(2, readSet.size());
        final Iterator<Integer> iterator = readSet.iterator();
        final Integer int1 = iterator.next();
        final Integer int2 = iterator.next();
        assertTrue((int1.equals(16) && int2.equals(32)) || (int1.equals(32) && int2.equals(16)));
    }

    @Test
    public void iterateHashSet() {
        final Set<String> set = new IdentityHashSet<String>(0);
        final Collection<String> entries = new HashSet<String>();
        entries.add("entry1");
        entries.add("entry2");
        entries.add("entry3");
        entries.add("entry4");
        entries.add("entry5");
        entries.add("entry6");
        entries.add("entry7");
        set.addAll(entries);
        Iterator<String> iterator = set.iterator();
        assertTrue(iterator.hasNext());
        assertTrue(entries.remove(iterator.next()));
        iterator.remove();
        assertEquals(6, entries.size());

        iterator = set.iterator();
        for (int i = 0; i < 6; i++) {
            assertTrue(iterator.hasNext());
            assertTrue(iterator.hasNext());
            assertTrue(entries.remove(iterator.next()));
        }
        assertTrue(entries.isEmpty());
        assertFalse(iterator.hasNext());
        assertFalse(iterator.hasNext());
        try {
            iterator.next();
            fail("NoSuchElementException expected");
        } catch (NoSuchElementException e) {}

        iterator = set.iterator();
        assertTrue(iterator.hasNext());
        assertNotNull(iterator.next());
        set.add("new entry");
        try {
            iterator.next();
            fail("ConcurrentModificationException expected");
        } catch (ConcurrentModificationException e) {}

        iterator = set.iterator();
        assertTrue(iterator.hasNext());
        assertNotNull(iterator.next());
        set.add("new entry2");
        try {
            iterator.remove();
            fail("ConcurrentModificationException expected");
        } catch (ConcurrentModificationException e) {}
    }

    @Test
    public void illegalHashSet() {
        try {
            new IdentityHashSet<String>(-1);
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException e) {}

        try {
            new IdentityHashSet<String>(-1, -1.0f);
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException e) {}

        try {
            new IdentityHashSet<String>(3, 1.1f);
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException e) {}

        try {
            new IdentityHashSet<String>(3, 0.0f);
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException e) {}
    }
}
