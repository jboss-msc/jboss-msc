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

package org.jboss.msc.inject;

/**
 * An injector which casts the value to a specific type.
 *
 * @param <T> the type to which the argument should be cast
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 * @deprecated Use {@link org.jboss.msc.service.ServiceBuilder#provides(org.jboss.msc.service.ServiceName...)}
 * method instead. This class will be removed in a future release.
 */
@Deprecated
public final class CastingInjector<T> implements Injector<Object> {
    private final Injector<T> target;
    private final Class<T> type;

    /**
     * Construct a new instance.
     *
     * @param target the injection target
     * @param type the type to cast to
     */
    public CastingInjector(final Injector<T> target, final Class<T> type) {
        this.target = target;
        this.type = type;
    }

    /** {@inheritDoc} */
    public void inject(final Object value) throws InjectionException {
        final T castValue;
        try {
            castValue = type.cast(value);
        } catch (ClassCastException e) {
            throw new InjectionException("Injecting the wrong type (expected " + type + ", got " + value.getClass() + ")", e);
        }
        target.inject(castValue);
    }

    /** {@inheritDoc} */
    public void uninject() {
        target.uninject();
    }
}
