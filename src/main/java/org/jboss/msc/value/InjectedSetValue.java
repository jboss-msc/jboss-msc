/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import org.jboss.msc.inject.InjectionException;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.inject.RetainingInjector;

/**
 * A {@link Set} value that can have entries injected into it. The underlying set is an instance of {@link LinkedHashSet}, so
 * iteration order will be consistent.
 * <p/>
 * The {@link #injector()} method is used to get an injector that can inject into the set.
 *
 * @param <T> the value type
 *
 * @author Stuart Douglas
 */
public final class InjectedSetValue<T> implements Value<Set<T>> {

    private final Set<T> value = new LinkedHashSet<T>();
    private volatile Set<T> cachedValue;

    /**
     * Construct a new instance.
     */
    public InjectedSetValue() {
    }

    /** {@inheritDoc} */
    public Set<T> getValue() throws IllegalStateException {
        if (cachedValue == null) {
            synchronized (this) {
                if (cachedValue == null) {
                    cachedValue = Collections.unmodifiableSet(new LinkedHashSet<T>(value));
                }
            }
        }
        return cachedValue;
    }

    /** {@inheritDoc} */
    public Set<T> getOptionalValue() {
        return getValue();
    }

    private synchronized void addItem(T item) {
        value.add(item);
        cachedValue = null;
    }

    private synchronized void removeItem(T item) {
        value.remove(item);
        cachedValue = null;
    }

    /**
     * Gets an injector for this set.
     * 
     * @return An {@link Injector} that can inject into the value set.
     */
    public Injector<T> injector() {
        return new RetainingInjector<T>() {

            @Override
            public void inject(T value) throws InjectionException {
                super.inject(value);
                addItem(value);
            }

            @Override
            public void uninject() {
                try {
                    final Value<T> storedValue = getStoredValue();
                    if (storedValue != null) removeItem(storedValue.getValue());
                } finally {
                    super.uninject();
                }
            }
        };
    }
}
