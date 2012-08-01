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
 * A value which returns the {@code Class} object of another value.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class ClassOfValue<T> implements ReadableValue<Class<? extends T>> {
    private final ReadableValue<? extends T> value;

    public ClassOfValue(final ReadableValue<? extends T> value) {
        this.value = value;
    }

    /**
     * @return the {@code Class} of the value, or {@code null} if value.getValue() is {@code null}.
     */
    @SuppressWarnings({ "unchecked" })
    public Class<? extends T> getValue() throws IllegalStateException {
        final ReadableValue<? extends T> value = this.value;
        final T actualValue = value.getValue();
        return actualValue == null? null: (Class<? extends T>) actualValue.getClass();
    }
}
