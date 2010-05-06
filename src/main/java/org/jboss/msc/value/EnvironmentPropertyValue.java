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

package org.jboss.msc.value;

import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.PrivilegedAction;

public final class EnvironmentPropertyValue implements Value<String>, PrivilegedAction<String> {
    private final String propertyName;
    private final AccessControlContext accessControlContext;
    private final Value<?> defaultValue;

    public EnvironmentPropertyValue(final String propertyName) {
        this(propertyName, Values.nullValue());
    }

    public EnvironmentPropertyValue(final String propertyName, final AccessControlContext accessControlContext) {
        this(propertyName, accessControlContext, Values.nullValue());
    }

    public EnvironmentPropertyValue(final String propertyName, final Value<?> defaultValue) {
        this(propertyName, AccessController.getContext(), defaultValue);
    }

    public EnvironmentPropertyValue(final String propertyName, final AccessControlContext accessControlContext, final Value<?> defaultValue) {
        if (propertyName == null) {
            throw new IllegalArgumentException("propertyName is null");
        }
        if (defaultValue == null) {
            throw new IllegalArgumentException("defaultValue is null");
        }
        if (accessControlContext == null) {
            throw new IllegalArgumentException("accessControlContext is null");
        }
        this.propertyName = propertyName;
        this.accessControlContext = accessControlContext;
        this.defaultValue = defaultValue;
    }

    public String getValue() throws IllegalStateException {
        final SecurityManager sm = System.getSecurityManager();
        final String result;
        if (sm == null) {
            result = run();
        } else {
            result = AccessController.doPrivileged(this, accessControlContext);
        }
        if (result != null) return result;
        final Object value = defaultValue.getValue();
        return value != null ? value.toString() : null;
    }

    public String run() {
        return System.getenv(propertyName);
    }
}