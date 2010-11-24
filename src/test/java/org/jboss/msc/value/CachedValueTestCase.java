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

import org.junit.Test;

/**
 * Test for {@link CachedValue}.
 * 
 * @author <a href="mailto:flavia.rainone@jboss.com">Flavia Rainone</a>
 */
public class CachedValueTestCase {

    @Test
    public void immediateStringValue() {
        final Value<String> value = new CachedValue<String>(new ImmediateValue<String>("value"));
        // multiple calls should always return the same result
        assertEquals("value", value.getValue());
        assertEquals("value", value.getValue());
        assertEquals("value", value.getValue());
    }

    @Test
    public void immediateNumberValue() {
        final Value<Number> value = new CachedValue<Number>(new ImmediateValue<Integer>(5));
        assertEquals(5, value.getValue());
        assertEquals(5, value.getValue());
        assertEquals(5, value.getValue());
    }

    @Test
    public void incrementValue1() {
        final Value<Number> value = new CachedValue<Number>(new IncrementValue(-1));
        // the value should never be incremented
        assertEquals(-1, value.getValue());
        assertEquals(-1, value.getValue());
        assertEquals(-1, value.getValue());
    }

    @Test
    public void incrementValue2() {
        final Value<Number> value = new CachedValue<Number>(new IncrementValue(1000));
        // the value should never be incremented
        assertEquals(1000, value.getValue());
        assertEquals(1000, value.getValue());
        assertEquals(1000, value.getValue());
    }

    @Test
    public void nullCachedValue() {
        final Value<Number> value = new CachedValue<Number>(null);
        try {
            value.getValue();
            fail("NullPointerException expected");
        } catch (NullPointerException e) {}
    }

    /**
     * Value whose integer value is incremented at every call to {@link #getValue()}.
     * 
     * @author <a href="mailto:flavia.rainone@jboss.com">Flavia Rainone</a>
     *
     */
    private static final class IncrementValue implements Value<Integer> {

        private volatile int count = 0;

        public IncrementValue(int initalCount) {
            count = initalCount;
        }

        @Override
        public synchronized Integer getValue() throws IllegalStateException {
            return count ++;
        }
    }
}