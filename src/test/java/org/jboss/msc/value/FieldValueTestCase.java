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
import static org.junit.Assert.fail;

import java.lang.reflect.Field;

import org.jboss.msc.value.util.AnyService;
import org.junit.Test;

/**
 * Test for {@link FieldValue}.
 * 
 * @author <a href="mailto:flavia.rainone@jboss.com">Flavia Rainone</a>
 */
public class FieldValueTestCase {

    @Test
    public void testPublicField() throws Exception {
        final AnyService anyObject = new AnyService();
        anyObject.count = 5;
        final Field field = AnyService.class.getDeclaredField("count");
        final Value<Field> fieldValue = new ImmediateValue<Field>(field);
        final Value<AnyService> anyObjectValue = new ImmediateValue<AnyService>(anyObject);
        final FieldValue<Integer> countValue = new FieldValue<Integer>(fieldValue, anyObjectValue);
        assertEquals(5, (int) countValue.getValue());

        anyObject.count = 10;
        assertEquals(10, (int) countValue.getValue());

        anyObject.count = -1000;
        assertEquals(-1000, (int) countValue.getValue());
    }

    @Test
    public void testPrivateField() throws Exception {
        final AnyService anyObject = new AnyService();
        final Field field = AnyService.class.getDeclaredField("sum");
        final Value<Field> fieldValue = new ImmediateValue<Field>(field);
        final Value<AnyService> anyObjectValue = new ImmediateValue<AnyService>(anyObject);
        final FieldValue<Integer> sumValue = new FieldValue<Integer>(fieldValue, anyObjectValue);

        try {
            sumValue.getValue();
            fail("Shouldn't be able of accessing sum value");
        } catch (IllegalStateException e) {}

        field.setAccessible(true);
        field.set(anyObject, -1);
        assertEquals(-1, (int) sumValue.getValue());

        field.set(anyObject, 7);
        assertEquals(7, (int) sumValue.getValue());
    }

    @Test
    public void nullField() throws Exception {
        final AnyService anyObject = new AnyService();
        anyObject.count = 50;
        final Field field = AnyService.class.getDeclaredField("count");
        final Value<Field> fieldValue = new ImmediateValue<Field>(field);
        final Value<AnyService> anyObjectValue = new ImmediateValue<AnyService>(anyObject);

        FieldValue<Integer> countValue = new FieldValue<Integer>(Values.<Field>nullValue(), anyObjectValue);
        try {
            countValue.getValue();
            fail("NullPointerException expected");
        } catch (NullPointerException e) {}

        countValue = new FieldValue<Integer>(fieldValue, Values.<AnyService>nullValue());
        try {
            countValue.getValue();
            fail("NullPointerException expected");
        } catch (NullPointerException e) {}

        countValue = new FieldValue<Integer>(Values.<Field>nullValue(), Values.<AnyService>nullValue());
        try {
            countValue.getValue();
            fail("NullPointerException expected");
        } catch (NullPointerException e) {}

        countValue = new FieldValue<Integer>(null, anyObjectValue);
        try {
            countValue.getValue();
            fail("NullPointerException expected");
        } catch (NullPointerException e) {}

        countValue = new FieldValue<Integer>(fieldValue, null);
        try {
            countValue.getValue();
            fail("NullPointerException expected");
        } catch (NullPointerException e) {}

        countValue = new FieldValue<Integer>(null, null);
        try {
            countValue.getValue();
            fail("NullPointerException expected");
        } catch (NullPointerException e) {}
    }
}