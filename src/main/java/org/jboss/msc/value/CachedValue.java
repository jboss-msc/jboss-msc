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
 * A value which is calculated once only.  After the initial calculation, the result is cached and returned.
 *
 * @param <T> the value type
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 * @deprecated use {@link org.jboss.msc.service.ServiceBuilder#requires(org.jboss.msc.service.ServiceName)}
 * method instead. This class will be removed in future releases.
 */
@Deprecated
public final class CachedValue<T> implements Value<T> {
    private volatile Value<? extends T> value;

    /**
     * Construct a new instance.
     *
     * @param value the value from which this value is calculated
     */
    public CachedValue(final Value<? extends T> value) {
        this.value = value;
    }

    /** {@inheritDoc} */
    public T getValue() throws IllegalStateException {
        Value<? extends T> value;
        if ((value = this.value) instanceof ImmediateValue) {
            return value.getValue();
        }
        synchronized (this) {
            if ((value = this.value) instanceof ImmediateValue) {
                return value.getValue();
            }
            final T result = value.getValue();
            this.value = new ImmediateValue<T>(result);
            return result;
        }
    }
}
