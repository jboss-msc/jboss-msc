/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2021, Red Hat, Inc., and individual contributors
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

import java.util.List;

/**
 * A value which is acquired from a list by numerical index.
 *
 * @param <T> the value type
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 * @deprecated Use {@link org.jboss.msc.service.ServiceBuilder#requires(org.jboss.msc.service.ServiceName)}
 * method instead. This class will be removed in a future release.
 */
@Deprecated
public final class ListItemValue<T> implements Value<T> {
    private final Value<? extends List<? extends T>> listValue;
    private final Value<? extends Number> indexValue;

    /**
     * Construct a new instance.
     *
     * @param listValue the list in which to look
     * @param indexValue the index at which to look
     */
    public ListItemValue(final Value<List<? extends T>> listValue, final Value<? extends Number> indexValue) {
        if (listValue == null) {
            throw new IllegalArgumentException("listValue is null");
        }
        if (indexValue == null) {
            throw new IllegalArgumentException("indexValue is null");
        }
        this.listValue = listValue;
        this.indexValue = indexValue;
    }

    /** {@inheritDoc} */
    public T getValue() throws IllegalStateException {
        return listValue.getValue().get(indexValue.getValue().intValue());
    }
}
