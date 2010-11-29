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

package org.jboss.msc.inject;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.lang.reflect.Field;

import org.jboss.msc.value.Value;
import org.jboss.msc.value.Values;
import org.jboss.msc.value.util.AnyService;
import org.junit.Test;

/**
 * Test for {@link FieldInjector}.
 * 
 * @author <a href="mailto:flavia.rainone@jboss.com">Flavia Rainone</a>
 */
public class FieldInjectorTestCase {
    @Test
    public void fieldInjection() throws Exception {
        final Field field = AnyService.class.getField("description");
        final AnyService anyService = new AnyService();
        final Injector<String> injector = new FieldInjector<String>(Values.immediateValue(anyService),
                Values.immediateValue(field));
        assertNull(anyService.description);
        injector.inject("Injection of description field");
        assertEquals("Injection of description field", anyService.description);
        injector.inject("one more injection");
        assertEquals("one more injection", anyService.description);
        injector.uninject();
        assertNull(anyService.description);
        injector.inject("injection after uninjection");
        assertEquals("injection after uninjection", anyService.description);
        injector.inject(null);
        assertNull(anyService.description);
    }

    @Test
    public void byteFieldInjection() throws Exception {
        final Field field = AnyService.class.getField("byteCount");
        final AnyService anyService = new AnyService();
        final Injector<Byte> injector = new FieldInjector<Byte>(Values.immediateValue(anyService),
                Values.immediateValue(field));
        assertEquals(0, anyService.byteCount);
        injector.inject((byte) 9);
        assertEquals((byte) 9, anyService.byteCount);
        injector.inject((byte) 21);
        assertEquals(21, anyService.byteCount);
        injector.uninject();
        assertEquals(0, anyService.byteCount);
        injector.inject((byte) 56);
        assertEquals(56, anyService.byteCount);
        injector.inject((byte) 0);
        assertEquals(0, anyService.byteCount);
        try {
            injector.inject(null);
            fail("Injection Exception expected");
        } catch (InjectionException e) {}
        assertEquals(0, anyService.byteCount);
    }

    @Test
    public void shortFieldInjection() throws Exception {
        final Field field = AnyService.class.getField("shortCount");
        final AnyService anyService = new AnyService();
        final Injector<Short> injector = new FieldInjector<Short>(Values.immediateValue(anyService),
                Values.immediateValue(field));
        assertEquals(0, anyService.shortCount);
        injector.inject((short) 5);
        assertEquals((short) 5, anyService.shortCount);
        injector.inject((short) 275);
        assertEquals(275, anyService.shortCount);
        injector.uninject();
        assertEquals(0, anyService.shortCount);
        injector.inject((short) 54);
        assertEquals(54, anyService.shortCount);
        injector.inject((short) 0);
        assertEquals(0, anyService.shortCount);
        try {
            injector.inject(null);
            fail("Injection Exception expected");
        } catch (InjectionException e) {}
        assertEquals(0, anyService.shortCount);
    }

    @Test
    public void intFieldInjection() throws Exception {
        final Field field = AnyService.class.getField("count");
        final AnyService anyService = new AnyService();
        final Injector<Integer> injector = new FieldInjector<Integer>(Values.immediateValue(anyService),
                Values.immediateValue(field));
        assertEquals(0, anyService.count);
        injector.inject(54113);
        assertEquals(54113, anyService.count);
        injector.inject(27506);
        assertEquals(27506, anyService.count);
        injector.uninject();
        assertEquals(0, anyService.count);
        injector.inject(54112);
        assertEquals(54112, anyService.count);
        injector.inject(0);
        assertEquals(0, anyService.count);
        try {
            injector.inject(null);
            fail("Injection Exception expected");
        } catch (InjectionException e) {}
        assertEquals(0, anyService.count);
    }

    @Test
    public void longFieldInjection() throws Exception {
        final Field field = AnyService.class.getField("longCount");
        final AnyService anyService = new AnyService();
        final Injector<Long> injector = new FieldInjector<Long>(Values.immediateValue(anyService),
                Values.immediateValue(field));
        assertEquals(0, anyService.longCount);
        injector.inject(5411301345l);
        assertEquals(5411301345l, anyService.longCount);
        injector.inject(27506l);
        assertEquals(27506l, anyService.longCount);
        injector.uninject();
        assertEquals(0, anyService.longCount);
        injector.inject(1000354112l);
        assertEquals(1000354112l, anyService.longCount);
        injector.inject(0l);
        assertEquals(0l, anyService.longCount);
        try {
            injector.inject(null);
            fail("Injection Exception expected");
        } catch (InjectionException e) {}
        assertEquals(0, anyService.longCount);
    }

    @Test
    public void floatFieldInjection() throws Exception {
        final Field field = AnyService.class.getField("floatStatus");
        final AnyService anyService = new AnyService();
        final Injector<Float> injector = new FieldInjector<Float>(Values.immediateValue(anyService),
                Values.immediateValue(field));
        assertTrue(anyService.floatStatus < 0.0001 && anyService.floatStatus > -0.0001);
        injector.inject(54113.0f);
        float diff = 54113.0f -anyService.floatStatus;
        assertTrue(diff < 0.0001 && diff > -0.0001);
        injector.inject(275.06f);
        diff = 275.06f - anyService.floatStatus;
        assertTrue(diff < 0.0001 && diff > -0.0001);
        injector.uninject();
        assertTrue(anyService.floatStatus < 0.0001 && anyService.floatStatus > -0.0001);
        injector.inject(54.112f);
        diff = 54.112f - anyService.floatStatus;
        assertTrue(diff < 0.0001 && diff > -0.0001);
        injector.inject(0f);
        assertTrue(anyService.floatStatus < 0.0001 && anyService.floatStatus > -0.0001);
        try {
            injector.inject(null);
            fail("Injection Exception expected");
        } catch (InjectionException e) {}
        assertTrue(anyService.floatStatus < 0.0001 && anyService.floatStatus > -0.0001);
    }

    @Test
    public void doubleFieldInjection() throws Exception {
        final Field field = AnyService.class.getField("doubleStatus");
        final AnyService anyService = new AnyService();
        final Injector<Double> injector = new FieldInjector<Double>(Values.immediateValue(anyService),
                Values.immediateValue(field));
        assertTrue(anyService.doubleStatus < 0.0001 && anyService.doubleStatus > -0.0001);
        injector.inject(54113.01345);
        double diff = 54113.01345 - anyService.doubleStatus;
        assertTrue(diff < 0.0001 && diff > -0.0001);
        injector.inject(27506.0);
        diff = 27506 - anyService.doubleStatus;
        assertTrue(diff < 0.0001 && diff > -0.0001);
        injector.uninject();
        assertTrue(anyService.doubleStatus < 0.0001 && anyService.doubleStatus > -0.0001);
        injector.inject(10000000354.112);
        diff = 10000000354.112 - anyService.doubleStatus;
        assertTrue(diff < 0.0001 && diff > -0.0001);
        injector.inject(0.0);
        assertTrue(anyService.doubleStatus < 0.0001 && anyService.doubleStatus > -0.0001);
        try {
            injector.inject(null);
            fail("Injection Exception expected");
        } catch (InjectionException e) {}
        assertTrue(anyService.doubleStatus < 0.0001 && anyService.doubleStatus > -0.0001);
    }

    @Test
    public void charFieldInjection() throws Exception {
        final Field field = AnyService.class.getField("charStatus");
        final AnyService anyService = new AnyService();
        final Injector<Character> injector = new FieldInjector<Character>(Values.immediateValue(anyService),
                Values.immediateValue(field));
        assertEquals('\u0000', anyService.charStatus);
        injector.inject('m');
        assertEquals('m', anyService.charStatus);
        injector.inject('s');
        assertEquals('s', anyService.charStatus);
        injector.uninject();
        assertEquals('\u0000', anyService.charStatus);
        injector.inject('c');
        assertEquals('c', anyService.charStatus);
        injector.inject('\u0000');
        assertEquals('\u0000', anyService.charStatus);
        try {
            injector.inject(null);
            fail("Injection Exception expected");
        } catch (InjectionException e) {}
        assertEquals('\u0000', anyService.charStatus);
    }

    @Test
    public void arrayFieldInjection() throws Exception {
        final Field field = AnyService.class.getField("allCounts");
        final AnyService anyService = new AnyService();
        final Injector<byte[]> injector = new FieldInjector<byte[]>(Values.immediateValue(anyService),
                Values.immediateValue(field));
        assertNull(anyService.allCounts);
        injector.inject(new byte[0]);
        assertNotNull(anyService.allCounts);
        assertEquals(0, anyService.allCounts.length);
        injector.inject(new byte[] { (byte) 1, (byte) 2, (byte) 3});
        assertNotNull(anyService.allCounts);
        assertEquals((byte)1, anyService.allCounts[0]);
        assertEquals((byte)2, anyService.allCounts[1]);
        assertEquals((byte)3, anyService.allCounts[2]);
        injector.uninject();
        assertNull(anyService.allCounts);
        injector.inject(new byte[] { (byte) 2, (byte) 3});
        assertNotNull(anyService.allCounts);
        assertEquals((byte)2, anyService.allCounts[0]);
        assertEquals((byte)3, anyService.allCounts[1]);
        injector.inject(null);
        assertNull(anyService.allCounts);
    }

    @Test
    public void staticFieldInjection() throws Exception {
        final Field field = AnyService.class.getField("disableAll");
        final Injector<Boolean> injector = new FieldInjector<Boolean>(Values.<AnyService>nullValue(), Values.immediateValue(field));
        assertFalse(AnyService.disableAll);
        injector.inject(true);
        assertTrue(AnyService.disableAll);
        try {
           injector.inject(null);
           fail("InjectionException expected");
        } catch (InjectionException e) {}
        assertTrue(AnyService.disableAll);
        injector.inject(true);
        assertTrue(AnyService.disableAll);
        injector.uninject();
        assertFalse(AnyService.disableAll);
        injector.inject(false);
        assertFalse(AnyService.disableAll);
    }

    @Test
    public void unaccessibleFieldTraversing() throws Exception {
        final Field field = AnyService.class.getDeclaredField("sum");
        final AnyService anyService = new AnyService();
        final Injector<Integer> injector = new FieldInjector<Integer>(Values.immediateValue(anyService),
                Values.immediateValue(field));
        try {
            injector.inject(71);
            fail("InjectionException expected");
        } catch (InjectionException e) {}
        injector.uninject();
        field.setAccessible(true);
        injector.inject(73);
        assertEquals(73, field.get(anyService));
        injector.inject(75);
        assertEquals(75, field.get(anyService));
        injector.uninject();
        assertEquals(0, field.get(anyService));
        injector.inject(77);
        assertEquals(77, field.get(anyService));
    }

    @Test
    public void nullFieldInjection() throws Exception {
        final Value<AnyService> anyServiceValue = Values.immediateValue(new AnyService());
        Injector<String> injector = new FieldInjector<String>(anyServiceValue, Values.<Field>nullValue());
        try {
            injector.inject("inject into null field");
            fail("InjectionException expected");
        } catch (InjectionException e) {}

        final Value<Field> fieldValue = Values.immediateValue(AnyService.class.getField("description"));
        injector = new FieldInjector<String>(Values.<AnyService>nullValue(), fieldValue);
        try {
            injector.inject("inject into null target");
            fail("InjectionException expected");
        } catch (InjectionException e) {}

        injector = new FieldInjector<String>(Values.<AnyService>nullValue(), Values.<Field>nullValue());
        try {
            injector.inject("inject value into null field of null target");
            fail("InjectionException expected");
        } catch (InjectionException e) {}

        injector = new FieldInjector<String>(anyServiceValue, null);
        try {
            injector.inject("inject into null field");
            fail("InjectionException expected");
        } catch (InjectionException e) {}

        injector = new FieldInjector<String>(null, fieldValue);
        try {
            injector.inject("inject into null target");
            fail("InjectionException expected");
        } catch (InjectionException e) {}

        injector = new FieldInjector<String>(null, null);
        try {
            injector.inject("inject value into null field of null target");
            fail("InjectionException expected");
        } catch (InjectionException e) {}
    }
}
