/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

import static java.lang.Integer.signum;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;
import static org.junit.Assert.assertArrayEquals;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.junit.Test;

/**
 * Test for {@link ServiceName}.
 * 
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 * @author <a href="mailto:flavia.rainone@jboss.com">Flavia Rainone</a>
 */
public final class ServiceNameTestCase {
    @Test
    public void testIdentityCollation() {
        final ServiceName name1 = ServiceName.of("one", "two", "three");
        assertEquals("compareTo identity equality", 0, name1.compareTo(name1));
        assertEquals("compareTo identity equality plus non-identity component", 0, name1.append("xxx").compareTo(name1.append("xxx")));
    }

    @Test
    public void testEvenLengthCollation() {
        assertEquals("compareTo 1-level equality", 0, signum(ServiceName.of("aaaa").compareTo(ServiceName.of("aaaa"))));
        assertEquals("compareTo 2-level equality", 0, signum(ServiceName.of("aaaa", "bbbb").compareTo(ServiceName.of("aaaa", "bbbb"))));
        assertEquals("compareTo 1-level less-than", -1, signum(ServiceName.of("@@@@").compareTo(ServiceName.of("aaaa"))));
        assertEquals("compareTo 1-level greater-than", 1, signum(ServiceName.of("aaaa").compareTo(ServiceName.of("@@@@"))));
        assertEquals("compareTo 2-level less-than (1)", -1, signum(ServiceName.of("xxxx", "@@@@").compareTo(ServiceName.of("xxxx", "aaaa"))));
        assertEquals("compareTo 2-level less-than (2)", -1, signum(ServiceName.of("aaaa", "zzzz").compareTo(ServiceName.of("xxxx", "aaaa"))));
        assertEquals("compareTo 2-level greater-than (1)", 1, signum(ServiceName.of("xxxx", "aaaa").compareTo(ServiceName.of("xxxx", "@@@@"))));
        assertEquals("compareTo 2-level greater-than (2)", 1, signum(ServiceName.of("xxxx", "aaaa").compareTo(ServiceName.of("aaaa", "zzzz"))));
    }

    @Test
    public void testUnevenLengthCollation() {
        assertEquals("compareTo uneven less-than (1)", -1, signum(ServiceName.of("aaaa").compareTo(ServiceName.of("aaaa", "bbbb"))));
        assertEquals("compareTo uneven less-than (2)", -1, signum(ServiceName.of("aaaa").compareTo(ServiceName.of("aaab", "bbbb"))));
        assertEquals("compareTo uneven less-than (3)", -1, signum(ServiceName.of("aaaa", "cccc", "xxxx").compareTo(ServiceName.of("aaab", "bbbb"))));
        assertEquals("compareTo uneven greater-than (1)", 1, signum(ServiceName.of("aaaa", "bbbb").compareTo(ServiceName.of("aaaa"))));
        assertEquals("compareTo uneven greater-than (2)", 1, signum(ServiceName.of("aaab", "bbbb").compareTo(ServiceName.of("aaaa"))));
        assertEquals("compareTo uneven greater-than (3)", 1, signum(ServiceName.of("aaab", "bbbb").compareTo(ServiceName.of("aaaa", "cccc", "xxxx"))));
    }

    @Test
    public void testNullCollation() {
        final ServiceName name = ServiceName.of("one", "two", "three");
        try {
            name.compareTo(null);
            fail("ExpectedException: o is null");
        } catch (IllegalArgumentException expected) {
            
        }
    }

    @Test
    public void testCanonicalization() {
        assertEquals("OSGi regression case 1", ServiceName.JBOSS.append("osgi", "framework"), ServiceName.parse("jboss.osgi.framework"));
        assertEquals("OSGi regression case 1 (hash code)", ServiceName.JBOSS.append("osgi", "framework").hashCode(), ServiceName.parse("jboss.osgi.framework").hashCode());
        assertEquals("OSGi regression case 2", ServiceName.JBOSS.append("osgi").append("framework"), ServiceName.parse("jboss.osgi.framework"));
        assertEquals("OSGi regression case 2 (hash code)", ServiceName.JBOSS.append("osgi").append("framework").hashCode(), ServiceName.parse("jboss.osgi.framework").hashCode());
        assertEquals("simple canonical (string side)", "a.b.c", ServiceName.parse("a.b.c").getCanonicalName());
        assertEquals("simple canonical", ServiceName.of("a", "b", "c"), ServiceName.parse("a.b.c"));
        assertEquals("complex canonical (string side)", "a.\"\\r\\n\".b", ServiceName.parse("\"a\".\"\\r\\n\".b").getCanonicalName());
        assertEquals("complex canonical (string side)", ServiceName.of("\t\b\f", "\'", "abab", "end"), ServiceName.parse("\"\\t\\b\\f\".\"\\\'\".abab.end"));
        assertEquals("complex canonical (string side)", "\"\\t\\b\\f\".\'.abab.end", ServiceName.parse("\"\\t\\b\\f\".\"\\\'\".abab.end").getCanonicalName());
        assertEquals("complex canonical", ServiceName.of("a", "\r\n", "b"), ServiceName.parse("\"a\".\"\\r\\n\".b"));
        assertEquals("regression 1a", ServiceName.of("jboss", "managedbean-example.jar", "Bean"), ServiceName.parse("jboss.\"managedbean-example.jar\".Bean"));
        assertEquals("regression 1b", "jboss.\"managedbean-example.jar\".Bean", ServiceName.of("jboss", "managedbean-example.jar", "Bean").getCanonicalName());
        try {
            ServiceName.parse(" foo");
            fail("Expected exception: whitespace in simple name");
        } catch (IllegalArgumentException expected) {
        }
        try {
            ServiceName.parse("foo ");
            fail("Expected exception: whitespace in simple name");
        } catch (IllegalArgumentException expected) {
        }
        try {
            ServiceName.parse("");
            fail("Expected exception: empty name");
        } catch (IllegalArgumentException expected) {
        }
        try {
            ServiceName.parse("\"\"");
            fail("Expected exception: empty name");
        } catch (IllegalArgumentException expected) {
        }
        try {
            ServiceName.parse("\tfoo");
            fail("Expected exception: tab character in simple name");
        } catch (IllegalArgumentException expected) {
        }
        try {
            ServiceName.parse("foo\t");
            fail("Expected exception: tab character in simple name");
        } catch (IllegalArgumentException expected) {
        }
        try {
            ServiceName.parse("\"\\u\"");
            fail("Expected exception: incomplete unicode escape");
        } catch (IllegalArgumentException expected) {
        }
        try {
            ServiceName.parse("\"\\u0\"");
            fail("Expected exception: incomplete unicode escape");
        } catch (IllegalArgumentException expected) {
        }
        try {
            ServiceName.parse("\"\\u00\"");
            fail("Expected exception: incomplete unicode escape");
        } catch (IllegalArgumentException expected) {
        }
        try {
            ServiceName.parse("\"\\u000\"");
            fail("Expected exception: incomplete unicode escape");
        } catch (IllegalArgumentException expected) {
        }
        ServiceName.parse("\"\\u0000\"");
        try {
            ServiceName.parse("\"");
            fail("Expected exception: unexpected end of string");
        } catch (IllegalArgumentException expected) {
        }
        try {
            ServiceName.parse("\"foo");
            fail("Expected exception: unexpected end of string");
        } catch (IllegalArgumentException expected) {
        }
    }

    @Test
    public void testInvalidServiceName() {
        // this untrusted service name can be created with of, because of performance issues
        // check that the canonicalName can be retrieved
        assertEquals("complex canonical with ISO", "\"\\u0003 \\t\\b\\f\\0\".\"\\\"\\\"\\\\\'abab\\\\\'\".end", ServiceName.of("\u0003 \t\b\f\0", "\"\"\\'abab\\'", "end").getCanonicalName());
        try {
            // parse doesnt accept untrusted service names
            ServiceName.parse("\u0003 \t\b\f\0.\"\"\\'abab\\'.end");
            fail ("Expected exception: invalid name character");
        } catch (IllegalArgumentException expected) {
        }

        try {
            // parse doesn't accept untrusted service names
            ServiceName.parse((char) (0x0000 + 1) + " ");
            fail ("Expected exception: invalid name character");
        } catch (IllegalArgumentException expected) {
        }
    }

    @Test
    public void testIsValidNameSegment() {
        assertTrue(ServiceName.isValidNameSegment("a"));
        assertFalse(ServiceName.isValidNameSegment("\u0001"));
        assertFalse(ServiceName.isValidNameSegment(null));
        assertFalse(ServiceName.isValidNameSegment(""));
        assertFalse(ServiceName.isValidNameSegment(" "));
    }

    @Test
    public void testIllegalArgumentException() {
        try {
            ServiceName.of();
            fail ("Expected exception: must provide at least one name segment");
        } catch (IllegalArgumentException expected) {
        }

        try {
            ServiceName.of("A", "B", "", "C");
            fail ("Expected exception: empty name segment is not allowed");
        } catch (IllegalArgumentException expected) {
        }

        try {
            ServiceName.of("A", null, "B", "C");
            fail ("Expected exception: name segment is null");
        } catch (IllegalArgumentException expected) {
        }
    }

    @Test
    public void testChainAppend() {
        final ServiceName black = ServiceName.of("bl", "ack");
        final ServiceName white = ServiceName.of("wh", "ite");
        final ServiceName blackAndWhite = black.append("&").append(white);
        assertEquals("bl.ack.&.wh.ite", blackAndWhite.getCanonicalName());
        assertNull(white.commonAncestorOf(blackAndWhite));
        assertEquals(black, black.commonAncestorOf(blackAndWhite));
    }

    @Test
    public void testAppend() {
        final ServiceName  jbossMSC = ServiceName.of("JBoss", "MSC");
        final ServiceName jbossMSCNewRelease = jbossMSC.append("New", "Release");
        assertFalse(jbossMSC.equals(jbossMSCNewRelease));
        assertEquals(jbossMSC, jbossMSC.commonAncestorOf(jbossMSCNewRelease));
    }

    @Test
    public void testIllegalAppend() {
        final ServiceName jbossMSC = ServiceName.of("JBoss", "MSC");
        try {
            jbossMSC.append();
            fail ("Expected exception: must provide at least one name segment");
        } catch (IllegalArgumentException expected) {
        }

        try {
            jbossMSC.append("Illegal", "");
            fail ("Expected exception: empty name segment is not allowed");
        } catch (IllegalArgumentException expected) {
        }

        try {
            jbossMSC.append((String) null);
            fail ("Expected exception: name segment is null");
        } catch (IllegalArgumentException expected) {
        }
    }

    @Test
    public void testCommonAncestor() {
        final ServiceName parent = ServiceName.of("parent");
        final ServiceName unrelated = ServiceName.of("unrelated");
        final ServiceName ancestor = ServiceName.of(parent, "child");
        final ServiceName child1 = ServiceName.of(parent, "child", "1");
        final ServiceName child2 = ServiceName.of(parent, "child", "2");
        assertEquals(ancestor, child1.commonAncestorOf(child2));
        assertEquals(ancestor, child2.commonAncestorOf(child1));
        assertNull(child1.commonAncestorOf(unrelated));
        assertNull(unrelated.commonAncestorOf(child1));
        final ServiceName child3 = ServiceName.of(child2, "child", "3");
        assertEquals(ancestor, child1.commonAncestorOf(child3));
        assertEquals(ancestor, child2.commonAncestorOf(child1));
        assertEquals(child2, child2.commonAncestorOf(child3));
        assertEquals(child2, child3.commonAncestorOf(child2));
        assertEquals(parent, parent.commonAncestorOf(parent));
        assertNull(parent.commonAncestorOf(null));
        assertNull(child1.commonAncestorOf(null));
        assertNull(child2.commonAncestorOf(null));
        assertNull(child3.commonAncestorOf(null));
        assertEquals(ancestor, child1.getParent());
        assertEquals(ancestor, child2.getParent());
        assertEquals(child2.append("child"), child3.getParent());
        assertTrue(parent.isParentOf(child1));
        assertTrue(parent.isParentOf(child2));
        assertTrue(parent.isParentOf(child3));
        assertTrue(child2.isParentOf(child3));
        assertFalse(parent.isParentOf(null));
        assertFalse(parent.isParentOf(unrelated));
    }

    @Test
    public void testSerialize() throws Exception {
        final ServiceName serviceName = ServiceName.of("S", "E", "R", "V", "I", "C", "E");
        ByteArrayOutputStream byteOutputStream = new ByteArrayOutputStream();
        ObjectOutputStream objectOutputStream = new ObjectOutputStream (byteOutputStream);
        objectOutputStream.writeObject(serviceName);
        objectOutputStream.close();
        byte[] bytes = byteOutputStream.toByteArray();
        ObjectInputStream objectInputStream = new ObjectInputStream(new ByteArrayInputStream(bytes));
        assertEquals(serviceName, objectInputStream.readObject());
    }

    @Test
    public void testMsc76Parsing() {
        assertEquals(ServiceName.parse("jboss.server.path.\"java.home\""), ServiceName.of("jboss", "server", "path", "java.home"));
        assertEquals(ServiceName.parse("jboss.server.path.\"jboss.home.dir\""), ServiceName.of("jboss", "server", "path", "jboss.home.dir"));
        assertEquals(ServiceName.parse("\"first.section\".\"second.section\".third.fourth"), ServiceName.of("first.section", "second.section", "third", "fourth"));
        assertEquals(ServiceName.parse("\"all.in.one\""), ServiceName.of("all.in.one"));
    }

    @Test
    public void testToArray() {
        final ServiceName one = ServiceName.of("foo", "bar", "baz");
        final ServiceName two = ServiceName.of("foo", "bar");
        final ServiceName three = ServiceName.of("foo");
        assertArrayEquals(new String[] { "foo", "bar", "baz" }, one.toArray());
        assertArrayEquals(new String[] { "foo", "bar" }, two.toArray());
        assertArrayEquals(new String[] { "foo" }, three.toArray());
    }
}
