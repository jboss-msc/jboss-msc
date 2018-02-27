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

import java.util.ArrayList;
import java.util.List;

/**
 * A list value.
 *
 * @param <T> the list entry type
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 * @deprecated use {@link org.jboss.msc.service.ServiceBuilder#requires(org.jboss.msc.service.ServiceName)}
 * method instead. This class will be removed in future releases.
 */
@Deprecated
public final class ListValue<T> implements Value<List<T>> {
    private final List<? extends Value<? extends T>> values;

    /**
     * Construct a new instance.
     *
     * @param values the list of values with which to populate the list
     */
    public ListValue(final List<? extends Value<? extends T>> values) {
        this.values = values;
    }

    /** {@inheritDoc} */
    public List<T> getValue() throws IllegalStateException {
        final List<? extends Value<? extends T>> values = this.values;
        final ArrayList<T> list = new ArrayList<T>(values.size());
        for (Value<? extends T> value : values) {
            list.add(value.getValue());
        }
        return list;
    }
}
