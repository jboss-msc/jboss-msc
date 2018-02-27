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

package org.jboss.msc.inject;

import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;
import org.jboss.msc.value.Value;

/**
 * An injector which updates the value of an {@link AtomicReferenceFieldUpdater}.
 *
 * @param <C> the class which holds the target field
 * @param <T> the type of the value to inject
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 * @deprecated use {@link org.jboss.msc.service.ServiceBuilder#provides(org.jboss.msc.service.ServiceName...)}
 * method instead. This class will be removed in future releases.
 */
@Deprecated
public final class AtomicReferenceFieldUpdaterInjector<C, T> implements Injector<T> {
    private final AtomicReferenceFieldUpdater<C, ? super T> updater;
    private final Value<C> target;

    /**
     * Construct a new instance.
     *
     * @param updater the updater to inject to
     * @param target the target object upon which to inject
     */
    public AtomicReferenceFieldUpdaterInjector(final AtomicReferenceFieldUpdater<C, ? super T> updater, final Value<C> target) {
        this.updater = updater;
        this.target = target;
    }

    /** {@inheritDoc} */
    public void inject(final T value) {
        updater.set(target.getValue(), value);
    }

    /** {@inheritDoc} */
    public void uninject() {
        updater.set(target.getValue(), null);
    }
}
