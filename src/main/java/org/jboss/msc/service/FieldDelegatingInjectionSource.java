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

import org.jboss.msc.value.ClassOfValue;
import org.jboss.msc.value.FieldValue;
import org.jboss.msc.value.LookupFieldValue;
import org.jboss.msc.value.LookupPropertyValue;
import org.jboss.msc.value.Value;

import java.lang.reflect.Field;

/**
 * InjectionSource that gets the value of a field from the value of a delegate InjectionSource. 
 *
 * @author John E. Bailey
 */
public class FieldDelegatingInjectionSource extends DelegatingInjectionSource {
    private final Value<Field> fieldValue;
    private final String fieldName;

    public FieldDelegatingInjectionSource(final InjectionSource delegate, final Value<Field> fieldValue) {
        super(delegate);
        this.fieldValue = fieldValue;
        this.fieldName = null;
    }

    public FieldDelegatingInjectionSource(final InjectionSource delegate, final String fieldName) {
        super(delegate);
        this.fieldName = fieldName;
        this.fieldValue = null;
    }

    @Override
    protected <T> Value<?> getValue(final Value<?> delegateValue, final Value<T> serviceValue, final ServiceBuilder<T> serviceBuilder, final ServiceContainerImpl container) {
        return new FieldValue(getFieldValue(delegateValue), delegateValue);
    }

    private Value<Field> getFieldValue(final Value<?> delegateValue) {
        if(fieldValue != null)
            return fieldValue;
        if(fieldName == null)
            throw new IllegalStateException("Either field value or field name is required");
        return new LookupFieldValue(new ClassOfValue<Object>(delegateValue), fieldName);
    }
}
