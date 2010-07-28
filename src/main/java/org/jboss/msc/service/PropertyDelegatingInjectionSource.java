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

package org.jboss.msc.service;

import org.jboss.msc.reflect.Property;
import org.jboss.msc.value.ClassOfValue;
import org.jboss.msc.value.LookupPropertyValue;
import org.jboss.msc.value.PropertyValue;
import org.jboss.msc.value.Value;

/**
 * InjectionSource that delegates to another InjectionSource and uses a property of the value as the actual injection value.
 * 
 * @author John E. Bailey
 */
class PropertyDelegatingInjectionSource extends DelegatingInjectionSource {
    private final String propertyName;
    private final Value<Property> propertyValue;

    public PropertyDelegatingInjectionSource(final InjectionSource delegate, final String propertyName) {
        super(delegate);
        this.propertyName = propertyName;
        this.propertyValue = null;
    }

    public PropertyDelegatingInjectionSource(final InjectionSource delegate, final Value<Property> propertyValue) {
        super(delegate);
        this.propertyName = null;
        this.propertyValue = propertyValue;
    }

    @Override
    protected <T> Value<?> getValue(final Value<?> delegateValue, final Value<T> serviceValue, final ServiceContainerImpl container) {
        return new PropertyValue(getPropertyValue(delegateValue), delegateValue);
    }

    private Value<Property> getPropertyValue(final Value<?> delegateValue) {
        if(propertyValue != null)
            return propertyValue;
        if(propertyName == null)
            throw new IllegalStateException("Either property value or property name is required");
        return new LookupPropertyValue(new ClassOfValue(delegateValue), propertyName);
    }
}
