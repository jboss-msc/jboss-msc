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

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * A map value.
 *
 * @param <K> the key type
 * @param <V> the value type
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class MapValue<K, V> implements Value<Map<K, V>> {
    private final List<MapEntry<? extends Value<? extends K>, ? extends Value<? extends V>>> values;
    private final Value<? extends Map<K, V>> mapValue;

    /**
     * Construct a new instance.
     *
     * @param mapValue the map value to add values to
     * @param values the values to add
     */
    public MapValue(final Value<? extends Map<K, V>> mapValue, final List<MapEntry<? extends Value<? extends K>, ? extends Value<? extends V>>> values) {
        this.values = values;
        this.mapValue = mapValue;
    }

    /**
     * Construct a new instance.
     *
     * @param mapValue the map value to add values to
     * @param values the values to add
     */
    public MapValue(final Value<? extends Map<K, V>> mapValue, MapEntry<? extends Value<? extends K>, ? extends Value<? extends V>>... values) {
        this(mapValue, Arrays.asList(values));
    }

    /** {@inheritDoc} */
    public Map<K, V> getValue() throws IllegalStateException {
        final List<MapEntry<? extends Value<? extends K>, ? extends Value<? extends V>>> values = this.values;
        final Map<K, V> map = mapValue.getValue();
        for (MapEntry<? extends Value<? extends K>, ? extends Value<? extends V>> pair : values) {
            map.put(pair.getKey().getValue(), pair.getValue().getValue());
        }
        return map;
    }
}
