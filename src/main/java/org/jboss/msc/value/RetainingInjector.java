/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2022, Red Hat, Inc., and individual contributors
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

import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

import org.jboss.msc.inject.InjectionException;
import org.jboss.msc.inject.Injector;

/**
 * An injector which retains its value.
 *
 * @param <T> the injected value type
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
abstract class RetainingInjector<T> implements Injector<T> {

    /**
     * The stored value.
     */
    @SuppressWarnings("unused")
    private volatile Value<T> value;

    @SuppressWarnings("unchecked")
    private static final AtomicReferenceFieldUpdater<RetainingInjector, Value> valueUpdater = AtomicReferenceFieldUpdater.newUpdater(RetainingInjector.class, Value.class, "value");

    /** {@inheritDoc} */
    public void inject(final T value) throws InjectionException {
        if (! valueUpdater.compareAndSet(this, null, () -> value)) {
            throw new InjectionException("Value already set for this injector");
        }
    }

    /** {@inheritDoc} */
    public void uninject() {
        if (valueUpdater.getAndSet(this, null) == null) {
            // todo log double-uninject warning
        }
    }

    /**
     * Get the value object stored in this injector.
     *
     * @return the value object
     */
    protected Value<T> getStoredValue() {
        return value;
    }

    /**
     * Set the value object stored in this injector (must not be {@code null}).
     *
     * @param value the value object (must not be {@code null})
     * @return the old value object, or {@code null} if none was stored
     */
    @SuppressWarnings("unchecked")
    protected Value<T> setStoredValue(final Value<T> value) {
        if (value == null) {
            throw new IllegalArgumentException("value is null");
        }
        return (Value<T>) valueUpdater.getAndSet(this, value);
    }
}
