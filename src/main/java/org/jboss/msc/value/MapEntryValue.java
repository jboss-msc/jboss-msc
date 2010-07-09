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
 * A value which yields a map entry.
 *
 * @param <K> the key type
 * @param <V> the value type
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class MapEntryValue<K, V> implements Value<MapEntry<K, V>> {
    private final MapEntry<Value<K>, Value<V>> entry;

    /**
     * Construct a new instance.
     *
     * @param entry the key and value to use
     */
    public MapEntryValue(final MapEntry<Value<K>, Value<V>> entry) {
        this.entry = entry;
    }

    /** {@inheritDoc} */
    public MapEntry<K, V> getValue() throws IllegalStateException {
        final MapEntry<Value<K>, Value<V>> entry = this.entry;
        return MapEntry.entry(entry.getKey().getValue(), entry.getValue().getValue());
    }

    /**
     * Construct a new instance.
     *
     * @param key the key
     * @param value the value
     * @param <K> the key type
     * @param <V> the value type
     * @return the new entry value
     */
    public static <K, V> Value<MapEntry<K, V>> of(Value<K> key, Value<V> value) {
        return new MapEntryValue<K, V>(MapEntry.entry(key, value));
    }
}
