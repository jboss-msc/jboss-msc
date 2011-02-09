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

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import org.jboss.msc.inject.InjectionException;
import org.jboss.msc.inject.Injector;

/**
 * A {@link Set} value that can have entries injected into it. The underlying set is an instance of {@link LinkedHashSet}, so
 * iteration order will be consistent.
 * <p/>
 * The {@link #injector()} method is used to get an injector that can inject into the set.
 * 
 * @author Stuart Douglas
 */
public final class InjectedSetValue<T> implements Value<Set<T>> {

    private final Set<T> value = new LinkedHashSet<T>();

    /**
     * Construct a new instance.
     */
    public InjectedSetValue() {
    }

    /** {@inheritDoc} */
    public Set<T> getValue() throws IllegalStateException {
        return Collections.unmodifiableSet(value);
    }

    /** {@inheritDoc} */
    public Set<T> getOptionalValue() {
        return Collections.unmodifiableSet(value);
    }

    private synchronized void addItem(T item) {
        value.add(item);
    }

    private synchronized void removeItem(T item) {
        value.remove(item);
    }

    /**
     * Gets an injector for this set.
     * 
     * @return An {@link Injector} that can inject into the value set.
     */
    public Injector<T> injector() {
        return new Injector<T>() {

            private volatile T value;

            @Override
            public void inject(T value) throws InjectionException {
                this.value = value;
                addItem(value);
            }

            @Override
            public void uninject() {
                removeItem(value);
                value = null;
            }
        };
    }
}
