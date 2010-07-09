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

/**
 * A thread-local value.  Used to pass values in special situations.
 *
 * @param <T> the value type
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class ThreadLocalValue<T> implements Value<T> {
    private final ThreadLocal<Value<? extends T>> threadLocal = new ThreadLocal<Value<? extends T>>();

    /**
     * Construct a new instance.
     */
    public ThreadLocalValue() {
    }

    /** {@inheritDoc} */
    public T getValue() {
        final Value<? extends T> value = threadLocal.get();
        if (value == null) {
            throw new IllegalStateException("No value set");
        }
        return value.getValue();
    }

    /**
     * Set this value, replacing any current value.
     *
     * @param newValue the new value to set
     */
    public void setValue(Value<? extends T> newValue) {
        threadLocal.set(newValue);
    }

    /**
     * Get and set the value.  Returns the old value so it can be restored later on (typically in a {@code finally} block).
     *
     * @param newValue the new value
     * @return the old value
     */
    public Value<? extends T> getAndSetValue(Value<? extends T> newValue) {
        try {
            return threadLocal.get();
        } finally {
            threadLocal.set(newValue);
        }
    }
}
