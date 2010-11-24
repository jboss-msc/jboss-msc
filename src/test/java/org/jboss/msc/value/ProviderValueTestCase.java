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

import javax.inject.Provider;

import org.junit.Test;

/**
 * Test for {@link ProviderValue}.
 * 
 * @author <a href="mailto:flavia.rainone@jboss.com">Flavia Rainone</a>
 */
public class ProviderValueTestCase {

    @Test
    public void stringProviderValue() {
        final Provider<String> provider = new TestProvider<String>("stringProviderTestCase");
        final Value<String> value = new ProviderValue<String>(provider);
        assertEquals("stringProviderTestCase", value.getValue());
    }

    @Test
    public void objectProviderValue() {
        final Object object = new Object();
        final Provider<Object> provider = new TestProvider<Object>(object);
        final Value<Object> value = new ProviderValue<Object>(provider);
        assertEquals(object, value.getValue());
    }

    @Test
    public void countProviderValue() {
        final CountProvider countProvider = new CountProvider();
        final Value<Integer> value = new ProviderValue<Integer>(countProvider);
        assertEquals(0, (int) value.getValue());
        assertEquals(1, (int) value.getValue());
        assertEquals(2, (int) value.getValue());
        assertEquals(3, (int) value.getValue());
        assertEquals(4, (int) value.getValue());
        assertEquals(5, (int) value.getValue());
        assertEquals(6, (int) value.getValue());
        assertEquals(7, (int) value.getValue());
        assertEquals(8, (int) value.getValue());
        assertEquals(9, (int) value.getValue());
        assertEquals(10, (int) value.getValue());
    }

    @Test
    public void nullProviderValue () {
        final Value<Object> value = new ProviderValue<Object>(null);
        try {
            value.getValue();
            fail("NullPointerException expected");
        } catch (NullPointerException e) {}
    }
    
    private static final class TestProvider<T> implements Provider<T> {

        private T value;
        
        public TestProvider(T value) {
            this.value = value;
        }

        @Override
        public T get() {
            return value;
        }
    }

    private static final class CountProvider implements Provider<Integer> {

        private int value = 0;

        @Override
        public Integer get() {
            return value ++;
        }
    }
}