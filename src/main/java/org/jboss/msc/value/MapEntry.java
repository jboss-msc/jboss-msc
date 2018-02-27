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

import java.io.Serializable;
import java.util.Map;

/**
 * An immutable key-value object for constructing map instances programmatically.
 *
 * @param <K> the key type
 * @param <V> the value type
 * @deprecated use {@link org.jboss.msc.service.ServiceBuilder#requires(org.jboss.msc.service.ServiceName)}
 * method instead. This class will be removed in future releases.
 */
@Deprecated
public final class MapEntry<K, V> implements Map.Entry<K, V>, Serializable {

    private static final long serialVersionUID = 3913554072275205665L;

    private final K key;
    private final V value;

    /**
     * Construct a new instance.
     *
     * @param key the map key
     * @param value the map value
     */
    public MapEntry(final K key, final V value) {
        this.key = key;
        this.value = value;
    }

    /**
     * Construct a new instance.
     *
     * @param key the map key
     * @param value the map value
     * @param <K> the key type
     * @param <V> the value type
     * @return the entry
     */
    public static <K, V> MapEntry<K, V> entry(K key, V value) {
        return new MapEntry<K, V>(key, value);
    }

    /**
     * Get the map key.
     *
     * @return the map key
     */
    public K getKey() {
        return key;
    }

    /**
     * Get the map value.
     *
     * @return the map value
     */
    public V getValue() {
        return value;
    }

    /**
     * Unsupported operation.
     *
     * @param value ignored
     * @return nothing
     * @throws UnsupportedOperationException always
     */
    public V setValue(final V value) {
        throw new UnsupportedOperationException();
    }

    /**
     * Compare this entry with another.
     *
     * @param obj the other entry
     * @return {@code true} if this object equals the other {@code MapEntry} instance, {@code false} otherwise
     */
    public boolean equals(final Object obj) {
        return obj instanceof MapEntry && equals((MapEntry<?, ?>) obj);
    }

    /**
     * Compare this entry with another.
     *
     * @param obj the other entry
     * @return {@code true} if this object equals the other {@code MapEntry} instance, {@code false} otherwise
     */
    public boolean equals(final MapEntry<?, ?> obj) {
        return obj != null && (key == null ? obj.key == null : key.equals(obj.key)) && (value == null ? obj.value == null : value.equals(obj.value));
    }

    /**
     * Get the hash code of this object.
     *
     * @return the hash code
     */
    public int hashCode() {
        return (key == null ? 0 : key.hashCode()) + 7 * (value == null ? 0 : value.hashCode());
    }

    /**
     * Add entries to a map.
     *
     * @param map the map to add to
     * @param entries the entries to add
     * @param <K> the key type
     * @param <V> the value type
     * @return the map
     */
    public static <K, V> Map<K, V> addTo(Map<K, V> map, MapEntry<? extends K, ? extends V>... entries) {
        for (MapEntry<? extends K, ? extends V> pair : entries) {
            map.put(pair.getKey(), pair.getValue());
        }
        return map;
    }
}
