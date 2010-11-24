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
    public void primitiveFieldInjection() throws Exception {
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
        assertEquals(27506, anyService.count);
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
        assertTrue(AnyService.disableAll);
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
        assertEquals(75, field.get(anyService));
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
