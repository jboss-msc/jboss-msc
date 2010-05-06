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

public final class Pair<A, B> implements Serializable {

    private static final long serialVersionUID = 3913554072275205665L;

    private final A a;
    private final B b;

    public Pair(final A a, final B b) {
        this.a = a;
        this.b = b;
    }

    public static <A, B> Pair<A, B> of(A a, B b) {
        return new Pair<A,B>(a, b);
    }

    public A getA() {
        return a;
    }

    public B getB() {
        return b;
    }

    public boolean equals(final Object obj) {
        return obj instanceof Pair && equals((Pair<?, ?>) obj);
    }

    public boolean equals(final Pair<?, ?> obj) {
        return obj != null && (a == null ? obj.a == null : a.equals(obj.a)) && (b == null ? obj.b == null : b.equals(obj.b));
    }

    public int hashCode() {
        return (a == null ? 0 : a.hashCode()) + 7 * (b == null ? 0 : b.hashCode());
    }

    public static <K, V> Map<K, V> addTo(Map<K, V> map, Pair<? extends K, ? extends V>... pairs) {
        for (Pair<? extends K, ? extends V> pair : pairs) {
            map.put(pair.getA(), pair.getB());
        }
        return map;
    }
}
