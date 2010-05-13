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

import java.lang.reflect.Field;
import org.jboss.logging.Logger;
import org.jboss.msc.value.Value;

/**
 * An injector which updates the value of a field.
 *
 * @param <T> the value type
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class FieldInjector<T> implements Injector<T> {
    private static final Logger log = Logger.getI18nLogger("org.jboss.msc.inject.field", null, "MSC");

    private final Value<?> target;
    private final Value<Field> fieldValue;

    /**
     * Construct a new instance.
     *
     * @param target the object whose field is to be updated
     * @param fieldValue the field to update
     */
    public FieldInjector(final Value<?> target, final Value<Field> fieldValue) {
        this.target = target;
        this.fieldValue = fieldValue;
    }

    /** {@inheritDoc} */
    public void inject(final T value) {
        try {
            fieldValue.getValue().set(target.getValue(), value);
        } catch (Exception e) {
            throw new InjectionException("Failed to inject value into field", e);
        }
    }

    /** {@inheritDoc} */
    public void uninject() {
        final Field field = fieldValue.getValue();
        if (field == null) {
            log.trace("Field disappeared before uninject");
            return;
        }
        try {
            field.set(target.getValue(), null);
        } catch (Throwable throwable) {
            log.warnf(throwable, "Unexpected failure to uninject into field");
        }
    }
}