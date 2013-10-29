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

package org.jboss.msc.inject;

import java.util.concurrent.ConcurrentMap;

import org.jboss.msc.value.Value;

/**
 * An injector which applies a value to a concurrent map entry.
 *
 * @param <K> the key type
 * @param <T> the value type
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class ConcurrentMapInjector<K, T> extends RetainingInjector<T> implements Injector<T> {
    private final ConcurrentMap<K, T> map;
    private final K key;

    /**
     * Construct a new instance.
     *
     * @param map the map to update
     * @param key the key for this injector
     */
    public ConcurrentMapInjector(final ConcurrentMap<K, T> map, final K key) {
        this.map = map;
        this.key = key;
    }

    /** {@inheritDoc} */
    public void inject(final T value) throws InjectionException {
        if (map.putIfAbsent(key, value) != null) {
            throw new InjectionException("Value already injected into map key " + key);
        }
        super.inject(value);
    }

    /** {@inheritDoc} */
    public void uninject() {
        try {
            final Value<T> storedValue = getStoredValue();
            if (storedValue != null) map.remove(key, storedValue.getValue());
        } finally {
            super.uninject();
        }
    }
}
