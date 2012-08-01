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
 * A readable value which is calculated once only.  After the initial calculation, the result is cached and returned.
 *
 * @param <T> the value type
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class CachedReadableValue<T> implements ReadableValue<T> {
    private volatile ReadableValue<? extends T> value;

    /**
     * Construct a new instance.
     *
     * @param value the value from which this value is calculated
     */
    public CachedReadableValue(final ReadableValue<? extends T> value) {
        this.value = value;
    }

    /** {@inheritDoc} */
    public T getValue() throws IllegalStateException {
        ReadableValue<? extends T> value;
        if ((value = this.value) instanceof ImmediateValue) {
            return value.getValue();
        }
        synchronized (this) {
            if ((value = this.value) instanceof ImmediateValue) {
                return value.getValue();
            }
            final T result = value.getValue();
            this.value = new ImmediateValue<T>(result);
            return result;
        }
    }
}
