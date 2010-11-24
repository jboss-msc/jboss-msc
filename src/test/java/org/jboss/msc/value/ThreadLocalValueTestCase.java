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
import static org.junit.Assert.fail;

import org.junit.Test;

/**
 * Test for {@link ThreadLocalValue}.
 * 
 * @author <a href="mailto:flavia.rainone@jboss.com">Flavia Rainone</a>
 */
public class ThreadLocalValueTestCase {

    @Test
    public void threadLocalValue() throws Exception {
        final ThreadLocalValue<String> value = new ThreadLocalValue<String>();
        assertNull(value.getAndSetValue(new ImmediateValue<String>("value")));
        assertEquals("value", value.getValue());
        assertEquals("value", value.getValue());

        final ThreadLocalRunnable<String> runnable = new ThreadLocalRunnable<String>(value, "anotherValue");
        final Thread thread = new Thread(runnable);
        thread.start();
        thread.join(30000);
        assertEquals("anotherValue", runnable.getValue());
        assertEquals("value", value.getValue());
    }

    @Test
    public void nullThreadLocalValue() throws Exception {
        final ThreadLocalValue<String> value = new ThreadLocalValue<String>();
        assertNull(value.getAndSetValue(new ImmediateValue<String>("value")));
        assertEquals("value", value.getValue());
        assertEquals("value", value.getValue());

        final ThreadLocalRunnable<String> runnable = new ThreadLocalRunnable<String>(value, null);
        final Thread thread = new Thread(runnable);
        thread.start();
        thread.join(30000);
        assertNull(runnable.getValue());
        assertEquals("value", value.getValue());
    }

    @Test
    public void illegalStateThreadLocalValue() throws Exception {
        final ThreadLocalValue<String> value = new ThreadLocalValue<String>();

        final ThreadLocalRunnable<String> runnable = new ThreadLocalRunnable<String>(value, "initialValue");
        final Thread thread = new Thread(runnable);
        thread.start();
        thread.join(30000);
        assertEquals("initialValue", runnable.getValue());
        try {
            value.getValue();
            fail("IllegalStateException expected");
        } catch (IllegalStateException e) {}
    }

    @Test
    public void illegalThreadLocalValue() {
        final ThreadLocalValue<String> value = new ThreadLocalValue<String>();
        try {
            value.getValue();
            fail("IllegalStateException expected");
        } catch (IllegalStateException e) {}

        value.setValue(null);
        try {
            value.getValue();
            fail("IllegalStateException expected");
        } catch (IllegalStateException e) {}

        assertNull(value.getAndSetValue(new ImmediateValue<String>("notNull")));
        assertEquals("notNull", value.getValue());
    }

    private static class ThreadLocalRunnable<T> implements Runnable {
        private T value;
        private ThreadLocalValue<T> threadLocalValue;
        private T newValue;

        public ThreadLocalRunnable(ThreadLocalValue<T> threadLocalValue, T newValue) {
            this.threadLocalValue = threadLocalValue;
            this.newValue = newValue;
        }

        @Override
        public void run() {
            threadLocalValue.setValue(new ImmediateValue<T>(newValue));
            this.value = threadLocalValue.getValue();
        }

        public T getValue() {
            return this.value;
        }
    }
}