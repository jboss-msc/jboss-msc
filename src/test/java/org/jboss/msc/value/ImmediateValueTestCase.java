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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Test for {@link ImmediateValue}.
 * 
 * @author <a href="mailto:flavia.rainone@jboss.com">Flavia Rainone</a>
 */
public class ImmediateValueTestCase {

    @Test
    public void integerValue() {
        final ImmediateValue<Integer> immediateValue = new ImmediateValue<Integer>(34);
        assertEquals(34, (int)immediateValue.getValue());
    }

    @Test
    public void longValue() {
        final ImmediateValue<Long> immediateValue = new ImmediateValue<Long>(5l);
        assertEquals(5l, (long)immediateValue.getValue());
    }

    @Test
    public void shortValue() {
        final ImmediateValue<Short> immediateValue = new ImmediateValue<Short>((short) 1000);
        assertEquals(1000, (short)immediateValue.getValue());
    }

    @Test
    public void byteValue() {
        final ImmediateValue<Byte> immediateValue = new ImmediateValue<Byte>((byte) 2);
        assertEquals((byte) 2, (byte)immediateValue.getValue());
    }

    @Test
    public void floatValue() {
        final ImmediateValue<Float> immediateValue = new ImmediateValue<Float>(1.4f);
        assertEquals(1.4f, immediateValue.getValue(), 0.001);
    }

    @Test
    public void doubleValue() {
        final ImmediateValue<Double> immediateValue = new ImmediateValue<Double>(1.7);
        assertEquals(1.7, immediateValue.getValue(), 0.001);
    }

    @Test
    public void booleanValue() {
        final ImmediateValue<Boolean> immediateValue = new ImmediateValue<Boolean>(true);
        assertTrue(immediateValue.getValue());
    }

    @Test
    public void stringValue() {
        final ImmediateValue<String> immediateValue = new ImmediateValue<String>("someText");
        assertEquals("someText", immediateValue.getValue());
    }

    @Test
    public void nullValue() {
        final ImmediateValue<Object> immediateValue = new ImmediateValue<Object>(null);
        assertNull(immediateValue.getValue());
    }

    @Test
    public void plainObjectValue() {
        final Object someObject = new Object();
        final ImmediateValue<Object> immediateValue = new ImmediateValue<Object>(someObject);
        assertSame(someObject, immediateValue.getValue());
    }
}