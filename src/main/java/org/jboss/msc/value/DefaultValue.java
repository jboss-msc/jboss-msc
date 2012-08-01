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
 * A defaulted value.  If the delegate value returns {@code null}, a default value will be returned in its place.
 *
 * @param <T> the value type
 */
public final class DefaultValue<T> implements ReadableValue<T> {
    private final ReadableValue<T> value;
    private final ReadableValue<? extends T> defaultValue;

    /**
     * Construct a new instance.
     *
     * @param value the delegate value
     * @param defaultValue the value to use if the delegate value returns {@code null}
     */
    public DefaultValue(final ReadableValue<T> value, final ReadableValue<? extends T> defaultValue) {
        this.value = value;
        this.defaultValue = defaultValue;
    }

    /** {@inheritDoc} */
    public T getValue() throws IllegalStateException {
        final T result = value.getValue();
        return result != null ? result : defaultValue.getValue();
    }
}
