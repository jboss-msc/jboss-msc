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

package org.jboss.msc.inject;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import org.jboss.msc.value.Values;
import org.junit.Test;

/**
 * Test for {@link CastingInjector}.
 * 
 * @author <a href="mailto:flavia.rainone@jboss.com">Flavia Rainone</a>
 */
public class CastingInjectorTestCase {
    public List<String> listField;
    public static String stringField;

    @SuppressWarnings("rawtypes")
    @Test
    public void castingFieldInjection() throws Exception {
        final Field field = CastingInjectorTestCase.class.getField("listField");
        Collection<String> collection = new ArrayList<String>();
        final Injector<List> fieldInjector = new FieldInjector<List>(Values.immediateValue(this), Values.immediateValue(field).getValue());
        final Injector<Object> castingInjector = Injectors.cast(fieldInjector, List.class);
        castingInjector.inject(collection);
        assertSame(collection, listField);
        castingInjector.uninject();
        assertNull(listField);

        castingInjector.inject(null);
        assertNull(listField);

        collection = new LinkedList<String>();
        castingInjector.inject(collection);
        assertSame(collection, listField);

        try {
            castingInjector.inject(new HashMap<Object, Object>());
            fail("InjectionException expected"); 
        } catch (InjectionException e) {}
    }

    @Test
    public void castingStaticFieldInjection() throws Exception {
        final Field field = CastingInjectorTestCase.class.getField("stringField");
        final Injector<String> fieldInjector = new FieldInjector<String>(Values.<CastingInjectorTestCase>nullValue(), Values.immediateValue(field).getValue());
        final Injector<Object> castingInjector = new CastingInjector<String>(fieldInjector, String.class);
        castingInjector.inject("injection");
        assertEquals("injection", stringField);
        castingInjector.uninject();
        assertNull(stringField);

        castingInjector.inject(null);
        assertNull(listField);

        castingInjector.inject("");
        assertEquals("", stringField);

        try {
            castingInjector.inject(new StringBuffer());
            fail("InjectionException expected"); 
        } catch (InjectionException e) {}
    }

    @Test
    public void nullCastingInjector() throws Exception {
        final Field field = CastingInjectorTestCase.class.getField("stringField");
        final Injector<String> fieldInjector = new FieldInjector<String>(Values.<CastingInjectorTestCase>nullValue(), Values.immediateValue(field).getValue());
        Injector<Object> castingInjector = new CastingInjector<String>(null, String.class);
        try {
            castingInjector.inject("injection");
            fail("NullPointerException expected");
        } catch (NullPointerException e) {}

        castingInjector = new CastingInjector<String>(fieldInjector, null);
        try {
            castingInjector.inject("injection");
            fail("NullPointerException expected");
        } catch (NullPointerException e) {}

        castingInjector = new CastingInjector<String>(null, null);
        try {
            castingInjector.inject("injection");
            fail("NullPointerException expected");
        } catch (NullPointerException e) {}
    }
}
