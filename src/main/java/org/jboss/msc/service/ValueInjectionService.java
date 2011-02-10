/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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
import org.jboss.msc.value.InjectedValue;

/**
 * A service which propagates a value from a dependency.
 *
 * @param <T> the service type
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class ValueInjectionService<T> implements Service<T> {
    private final InjectedValue<T> injector = new InjectedValue<T>();

    /** {@inheritDoc} */
    public void start(final StartContext context) throws StartException {
    }

    /** {@inheritDoc} */
    public void stop(final StopContext context) {
    }

    /** {@inheritDoc} */
    public T getValue() throws IllegalStateException, IllegalArgumentException {
        return injector.getValue();
    }

    /**
     * Get the injector, which should be used to inject the dependency.
     *
     * @return the injector
     */
    public Injector<T> getInjector() {
        return injector;
    }
}
