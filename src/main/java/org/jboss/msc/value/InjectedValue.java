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

import org.jboss.msc.inject.Injector;
import org.jboss.msc.inject.RetainingInjector;

/**
 * A value which is injected from another source.  The value may only be read if the injector has populated it.
 *
 * @param <T> the value type
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 * @deprecated Use {@link org.jboss.msc.service.ServiceBuilder#requires(org.jboss.msc.service.ServiceName)}
 * method instead. This class will be removed in a future release.
 */
@Deprecated
public final class InjectedValue<T> extends RetainingInjector<T> implements Injector<T>, Value<T> {

    /**
     * Construct a new instance.
     */
    public InjectedValue() {
    }

    /** {@inheritDoc} */
    public T getValue() throws IllegalStateException {
        final Value<T> value = getStoredValue();
        if (value == null) {
            throw new IllegalStateException();
        }
        return value.getValue();
    }

    /**
     * Set the value to be injected to a {@code Value} instance.
     *
     * @param value the value to set, cannot be {@code null} (though it may be {@link org.jboss.msc.value.Values#nullValue()})
     */
    public void setValue(final Value<T> value) {
        setStoredValue(value);
    }

    /**
     * Get the value if it was injected, or return {@code null} if it was not.
     *
     * @return the value or {@code null} if it was not injected
     */
    public T getOptionalValue() {
        final Value<T> value = getStoredValue();
        return value == null ? null : value.getValue();
    }
}
