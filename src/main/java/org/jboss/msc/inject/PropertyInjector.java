/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat Middleware LLC, and individual contributors
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

import org.jboss.msc.reflect.Property;
import org.jboss.msc.value.Value;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class PropertyInjector<T> implements Injector<T> {

    private final Value<Property> propertyValue;
    private final Value<? super T> injectionValue;

    public PropertyInjector(final Value<Property> propertyValue, final Value<? super T> injectionValue) {
        if (propertyValue == null) {
            throw new IllegalArgumentException("propertyValue is null");
        }
        if (injectionValue == null) {
            throw new IllegalArgumentException("injectionValue is null");
        }
        this.propertyValue = propertyValue;
        this.injectionValue = injectionValue;
    }

    public void inject(final T value) throws InjectionException {
        try {
            propertyValue.getValue().set(injectionValue.getValue(), value);
        } catch (Exception e) {
            throw new InjectionException("Injection failed", e);
        }
    }

    public void uninject() {
        try {
            propertyValue.getValue().set(injectionValue.getValue(), null);
        } catch (Exception e) {
            // todo log
        }
    }
}
