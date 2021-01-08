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

import org.junit.Test;

/**
 * Test for {@link DefaultValue}.
 * 
 * @author <a href="mailto:flavia.rainone@jboss.com">Flavia Rainone</a>
 */
public class DefaultValueTestCase {

    @Test
    public void integerValue() {
        final DefaultValue<Integer> value = new DefaultValue<Integer>(new ImmediateValue<Integer>(3), new ImmediateValue<Integer>(4));
        assertEquals(3, (int) value.getValue());
    }

    @Test
    public void nullIntegerValue() {
        final DefaultValue<Integer> value = new DefaultValue<Integer>(new ImmediateValue<Integer>(null), new ImmediateValue<Integer>(4));
        assertEquals(4, (int) value.getValue());
    }

    @Test
    public void nullIntegerValueWithNullDefault() {
        final DefaultValue<Integer> value = new DefaultValue<Integer>(new ImmediateValue<Integer>(null), new ImmediateValue<Integer>(null));
        assertEquals(null, value.getValue());
    }

    @Test
    public void longValue() {
        final Value<Long> value = new DefaultValue<Long>(new ImmediateValue<Long> (5l), new ImmediateValue<Long>(4l));
        assertEquals(5l, (long) value.getValue());
    }

    @Test
    public void longValueWithNull() {
        final Value<Long> value = new DefaultValue<Long>(new ImmediateValue<Long> (null), new ImmediateValue<Long>(4l));
        assertEquals(4l, (long) value.getValue());
    }

    @Test
    public void shortValue() {
        final Value<Short> value = new DefaultValue<Short>(new ImmediateValue<Short>((short) 10), new ImmediateValue<Short>((short) 0));
        assertEquals(10, (short)value.getValue());
    }

    @Test
    public void nullShortValue() {
        final Value<Short> value = new DefaultValue<Short>(new ImmediateValue<Short>(null), new ImmediateValue<Short>((short) 0));
        assertEquals(0, (short)value.getValue());
    }

    @Test
    public void byteValue() {
        final Value<Byte> value = new DefaultValue<Byte>( new ImmediateValue<Byte>((byte) 0), new ImmediateValue<Byte>((byte) 2));
        assertEquals((byte) 0, (byte)value.getValue());
    }

    @Test
    public void nullByteValue() {
        final Value<Byte> value = new DefaultValue<Byte>( new ImmediateValue<Byte>(null), new ImmediateValue<Byte>((byte) 2));
        assertEquals((byte) 2, (byte)value.getValue());
    }

    @Test
    public void floatValue() {
        final Value<Float> value = new DefaultValue<Float>(new ImmediateValue<Float>(1.0f), new ImmediateValue<Float>(1.4f));
        assertEquals(1.0f, value.getValue(), 0.001);
    }

    @Test
    public void nullFloatValue() {
        final Value<Float> value = new DefaultValue<Float>(new ImmediateValue<Float>(null), new ImmediateValue<Float>(1.4f));
        assertEquals(1.4f, value.getValue(), 0.001);
    }

    @Test
    public void doubleValue() {
        final Value<Double> value = new DefaultValue<Double>(new ImmediateValue<Double>(0.3), new ImmediateValue<Double>(1.4));
        assertEquals(0.3, value.getValue(), 0.001);
    }

    @Test
    public void nullDoubleValue() {
        final Value<Double> value = new DefaultValue<Double>(new ImmediateValue<Double>(null), new ImmediateValue<Double>(1.4));
        assertEquals(1.4, value.getValue(), 0.001);
    }

    @Test
    public void booleanValue() {
        final Value<Boolean> value = new DefaultValue<Boolean>(new ImmediateValue<Boolean>(false), new ImmediateValue<Boolean>(true));
        assertFalse(value.getValue());
    }

    @Test
    public void nullBooleanValue() {
        final Value<Boolean> value = new DefaultValue<Boolean>(new ImmediateValue<Boolean>(null), new ImmediateValue<Boolean>(true));
        assertTrue(value.getValue());
    }

    @Test
    public void stringValue() {
        final Value<String> value = new DefaultValue<String>(new ImmediateValue<String>("someText"), new ImmediateValue<String>("default"));
        assertEquals("someText", value.getValue());
    }

    @Test
    public void nullStringValue() {
        final Value<String> value = new DefaultValue<String>(new ImmediateValue<String>(null), new ImmediateValue<String>("default"));
        assertEquals("default", value.getValue());
    }

    @Test
    public void plainObjectValue() {
        final Object someObject = new Object();
        final Object defaultObject = new Object();
        final Value<Object> value = new DefaultValue<Object>(new ImmediateValue<Object>(someObject), new ImmediateValue<Object>(defaultObject));
        assertSame(someObject, value.getValue());
    }

    @Test
    public void nullObjectValue() {
        final Object defaultObject = new Object();
        final Value<Object> value = new DefaultValue<Object>(new ImmediateValue<Object>(null), new ImmediateValue<Object>(defaultObject));
        assertSame(defaultObject, value.getValue());
    }

    @Test
    public void defaultValueChain1() {
        final Object someObject = new Object();
        final Value<Object> value = new DefaultValue<Object>(
                new DefaultValue<Object>(
                        new DefaultValue<Object>(
                                new DefaultValue<Object>(
                                        new DefaultValue<Object>(new ImmediateValue<Object>(34l), new ImmediateValue<Long>(45l)),
                                        new ImmediateValue<Object>(someObject)),
                                        new ImmediateValue<Boolean>(false)),
                                        new ImmediateValue<Double>(0.1)),
                                        new ImmediateValue<String>("Default"));
        assertEquals(34l, value.getValue());
    }

    @Test
    public void defaultValueChain2() {
        final Object someObject = new Object();
        final Value<Object> value = new DefaultValue<Object>(
                new DefaultValue<Object>(
                        new DefaultValue<Object>(
                                new DefaultValue<Object>(
                                        new DefaultValue<Object>(new ImmediateValue<Object>(null), new ImmediateValue<Long>(45l)),
                                        new ImmediateValue<Object>(someObject)),
                                        new ImmediateValue<Boolean>(false)),
                                        new ImmediateValue<Double>(0.1)),
                                        new ImmediateValue<String>("Default"));
        assertEquals(45l, value.getValue());
    }

    @Test
    public void defaultValueChain3() {
        final Object someObject = new Object();
        final Value<Object> value = new DefaultValue<Object>(
                new DefaultValue<Object>(
                        new DefaultValue<Object>(
                                new DefaultValue<Object>(
                                        new DefaultValue<Object>(new ImmediateValue<Object>(null), new ImmediateValue<Long>(null)),
                                        new ImmediateValue<Object>(someObject)),
                                        new ImmediateValue<Boolean>(false)),
                                        new ImmediateValue<Double>(0.1)),
                                        new ImmediateValue<String>("Default"));
        assertSame(someObject, value.getValue());
    }

    @Test
    public void defaultValueChain4() {
        final Value<Object> value = new DefaultValue<Object>(
                new DefaultValue<Object>(
                        new DefaultValue<Object>(
                                new DefaultValue<Object>(
                                        new DefaultValue<Object>(new ImmediateValue<Object>(null), new ImmediateValue<Long>(null)),
                                        new ImmediateValue<Object>(null)),
                                        new ImmediateValue<Boolean>(false)),
                                        new ImmediateValue<Double>(0.1)),
                                        new ImmediateValue<String>("Default"));
        assertSame(Boolean.FALSE, value.getValue());
    }

    @Test
    public void defaultValueChain5() {
        final Value<Object> value = new DefaultValue<Object>(
                new DefaultValue<Object>(
                        new DefaultValue<Object>(
                                new DefaultValue<Object>(
                                        new DefaultValue<Object>(new ImmediateValue<Object>(null), new ImmediateValue<Long>(null)),
                                        new ImmediateValue<Object>(null)),
                                        new ImmediateValue<Boolean>(null)),
                                        new ImmediateValue<Double>(0.1)),
                                        new ImmediateValue<String>("Default"));
        assertEquals(0.1, ((Double) value.getValue()), 0.001);
    }

    @Test
    public void defaultValueChain6() {
        final Value<Object> value = new DefaultValue<Object>(
                new DefaultValue<Object>(
                        new DefaultValue<Object>(
                                new DefaultValue<Object>(
                                        new DefaultValue<Object>(new ImmediateValue<Object>(null), new ImmediateValue<Long>(null)),
                                        new ImmediateValue<Object>(null)),
                                        new ImmediateValue<Boolean>(null)),
                                        new ImmediateValue<Double>(null)),
                                        new ImmediateValue<String>("Default"));
        assertEquals("Default", value.getValue());
    }

    @Test
    public void nullDefaultValue() {
        final Value<Object> value = new DefaultValue<Object>(null, new ImmediateValue<Object>(new Object()));
        try {
            value.getValue();
            fail("NullPointerException expected");
        } catch (NullPointerException e) {}
    }
}
