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

package org.jboss.msc.service;

import org.jboss.msc.inject.Injector;
import org.jboss.msc.value.Value;

/**
 * An injection of a source value into a target injector.
 *
 * @param <T> the value type
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 *
 * @deprecated use {@link org.jboss.msc.service.ServiceBuilder#requires(org.jboss.msc.service.ServiceName)}
 * and {@link org.jboss.msc.service.ServiceBuilder#provides(org.jboss.msc.service.ServiceName...)} methods instead.
 * This class will be removed in a future release.
 */
@Deprecated
public final class ValueInjection<T> {
    private final Value<? extends T> source;
    private final Injector<? super T> target;

    /**
     * Construct a new instance.
     *
     * @param source the source value
     * @param target the target injector
     */
    public ValueInjection(final Value<? extends T> source, final Injector<? super T> target) {
        this.source = source;
        this.target = target;
    }

    /**
     * Get the source value.
     *
     * @return the source value
     */
    public Value<? extends T> getSource() {
        return source;
    }

    /**
     * Get the target injector.
     *
     * @return the target injector
     */
    public Injector<? super T> getTarget() {
        return target;
    }
}
