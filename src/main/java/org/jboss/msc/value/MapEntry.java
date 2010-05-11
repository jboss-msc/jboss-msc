/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat Middleware LLC, and individual contributors
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

public final class MapEntry<K, V> implements Map.Entry<K, V>, Serializable {

    private static final long serialVersionUID = 3913554072275205665L;

    private final K key;
    private final V value;

    public MapEntry(final K key, final V value) {
        this.key = key;
        this.value = value;
    }

    public static <A, B> MapEntry<A, B> entry(A a, B b) {
        return new MapEntry<A,B>(a, b);
    }

    public K getKey() {
        return key;
    }

    public V getValue() {
        return value;
    }

    public V setValue(final V value) {
        throw new UnsupportedOperationException();
    }

    public boolean equals(final Object obj) {
        return obj instanceof MapEntry && equals((MapEntry<?, ?>) obj);
    }

    public boolean equals(final MapEntry<?, ?> obj) {
        return obj != null && (key == null ? obj.key == null : key.equals(obj.key)) && (value == null ? obj.value == null : value.equals(obj.value));
    }

    public int hashCode() {
        return (key == null ? 0 : key.hashCode()) + 7 * (value == null ? 0 : value.hashCode());
    }

    public static <K, V> Map<K, V> addTo(Map<K, V> map, MapEntry<? extends K, ? extends V>... entries) {
        for (MapEntry<? extends K, ? extends V> pair : entries) {
            map.put(pair.getKey(), pair.getValue());
        }
        return map;
    }
}
