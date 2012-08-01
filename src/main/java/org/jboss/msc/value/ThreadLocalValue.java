/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jboss.msc.value;

/**
 * A thread-local value.  Used to pass values in special situations.
 *
 * @param <T> the value type
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class ThreadLocalValue<T> implements ReadableValue<T> {
    private final ThreadLocal<ReadableValue<? extends T>> threadLocal = new ThreadLocal<ReadableValue<? extends T>>();

    /**
     * Construct a new instance.
     */
    public ThreadLocalValue() {
    }

    /** {@inheritDoc} */
    public T getValue() {
        final ReadableValue<? extends T> value = threadLocal.get();
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
    public void setValue(ReadableValue<? extends T> newValue) {
        threadLocal.set(newValue);
    }

    /**
     * Get and set the value.  Returns the old value so it can be restored later on (typically in a {@code finally} block).
     *
     * @param newValue the new value
     * @return the old value
     */
    public ReadableValue<? extends T> getAndSetValue(ReadableValue<? extends T> newValue) {
        try {
            return threadLocal.get();
        } finally {
            threadLocal.set(newValue);
        }
    }
}
