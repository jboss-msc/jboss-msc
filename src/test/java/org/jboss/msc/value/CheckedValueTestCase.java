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
import static org.junit.Assert.assertSame;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;

/**
 * Test for {@link CheckedValue}.
 * 
 * @author <a href="mailto:flavia.rainone@jboss.com">Flavia Rainone</a>
 */
public class CheckedValueTestCase {

    @Test
    public void sameTypeValue() {
        // ok: "text" is of type String
        final Value<String> value = new CheckedValue<String>(String.class, new ImmediateValue<String>("text"));
        assertEquals("text", value.getValue());
    }

    @Test
    @SuppressWarnings("rawtypes")
    public void assignableTypeValue() { 
        // ok: set is of type Collection
        final Set<String> set = new HashSet<String>();
        final Value<Collection> value = new CheckedValue<Collection>(Collection.class, new ImmediateValue<Set<String>>(set));
        assertSame(set, value.getValue());
    }

    @Test
    public void wrongTypeValue1() {
        // not ok: set is not of type String
        final Set<String> set = new HashSet<String>();
        final Value<String> value = new CheckedValue<String>(String.class, new ImmediateValue<Set<String>>(set));
        try {
            value.getValue();
            Assert.fail("ClassCastException expected");
        } catch (ClassCastException e) {};
    }

    @Test
    public void wrongTypeValue2() {
        // not ok: 41l is not of Integer type
        final Value<? extends Number> value = new CheckedValue<Integer>(Integer.class, new ImmediateValue<Long>(41l));
        try {
            value.getValue();
            Assert.fail("ClassCastException expected");
        } catch (ClassCastException e) {};
    }

    @Test
    public void nullValue() {
        Value<? extends Number> value = new CheckedValue<Integer>(null, new ImmediateValue<Integer>(41));
        try {
            value.getValue();
            Assert.fail("NullPointerException expected");
        } catch (NullPointerException e) {};

        value = new CheckedValue<Integer>(Integer.class, null);
        try {
            value.getValue();
            Assert.fail("NullPointerException expected");
        } catch (NullPointerException e) {};

        value = new CheckedValue<Integer>(null, null);
        try {
            value.getValue();
            Assert.fail("NullPointerException expected");
        } catch (NullPointerException e) {};
    }
}