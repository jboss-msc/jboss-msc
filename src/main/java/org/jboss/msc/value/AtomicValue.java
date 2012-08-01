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

import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;
import org.jboss.msc.value.ReadableValue;

/**
 * A simple readable and writable value.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class AtomicValue<T> implements WritableValue<T>, ReadableValue<T> {
    private static final Object UNSET = new Object();

    private volatile Object value = UNSET;

    private static final AtomicReferenceFieldUpdater<AtomicValue, Object> valueUpdater = AtomicReferenceFieldUpdater.newUpdater(AtomicValue.class, Object.class, "value");

    public void inject(final T value) throws InjectionException {
        if (! valueUpdater.compareAndSet(this, UNSET, value)) {
            throw new InjectionException("Value already set for this injector");
        }
    }

    public void uninject() {
        value = UNSET;
    }

    @SuppressWarnings("unchecked")
    public T getValue() throws IllegalStateException, IllegalArgumentException {
        Object value = this.value;
        if (value == UNSET) {
            throw new IllegalStateException("Value is not set");
        }
        return (T) value;
    }
}
