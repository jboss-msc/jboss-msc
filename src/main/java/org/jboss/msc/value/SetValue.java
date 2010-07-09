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

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * A set value.
 *
 * @param <T> the set member type
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class SetValue<T> implements Value<Set<T>> {
    private final List<? extends Value<? extends T>> values;

    /**
     * Construct a new instance.
     *
     * @param values the values to add to the set
     */
    public SetValue(final List<? extends Value<? extends T>> values) {
        this.values = values;
    }

    /** {@inheritDoc} */
    public Set<T> getValue() throws IllegalStateException {
        final LinkedHashSet<T> set = new LinkedHashSet<T>();
        for (Value<? extends T> value : values) {
            set.add(value.getValue());
        }
        return set;
    }
}
