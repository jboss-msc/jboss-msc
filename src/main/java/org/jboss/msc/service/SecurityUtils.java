/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2022, Red Hat, Inc., and individual contributors
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

import static java.lang.System.getSecurityManager;
import static java.security.AccessController.doPrivileged;

import java.lang.reflect.Field;
import java.security.PrivilegedAction;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
final class SecurityUtils {

    private SecurityUtils() {
        // forbidden instantiation
    }

    static int getSystemProperty(final String propertyName, final int defaultValue) {
        if (getSecurityManager() != null) {
            return doPrivileged(new GetSystemPropertyAction(propertyName, defaultValue));
        } else {
            return Integer.getInteger(propertyName, defaultValue);
        }
    }

    static ClassLoader getCL(final Class<?> clazz) {
        if (getSecurityManager() != null) {
            return doPrivileged(new GetCLAction(clazz));
        } else {
            return clazz.getClassLoader();
        }
    }

    static ClassLoader setTCCL(final ClassLoader newTCCL) {
        final SetTCCLAction setTCCLAction = new SetTCCLAction(newTCCL);
        if (getSecurityManager() != null) {
            return doPrivileged(setTCCLAction);
        } else {
            return setTCCLAction.run();
        }
    }

    static Field getClassField(final Class clazz, final String fieldName) {
        final GetFieldAction getFieldAction = new GetFieldAction(clazz, fieldName);
        if (getSecurityManager() != null) {
            return doPrivileged(getFieldAction);
        } else {
            return getFieldAction.run();
        }
    }

    private static final class SetTCCLAction implements PrivilegedAction<ClassLoader> {
        private final ClassLoader classLoader;

        SetTCCLAction(final ClassLoader classLoader) {
            this.classLoader = classLoader;
        }

        public ClassLoader run() {
            try {
                return Thread.currentThread().getContextClassLoader();
            } finally {
                Thread.currentThread().setContextClassLoader(classLoader);
            }
        }
    }

    private static final class GetFieldAction implements PrivilegedAction<Field> {
        private final Class clazz;
        private final String fieldName;

        GetFieldAction(final Class clazz, final String fieldName) {
            this.clazz = clazz;
            this.fieldName = fieldName;
        }

        public Field run() {
            final Field field;
            try {
                field = clazz.getDeclaredField(fieldName);
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
            field.setAccessible(true);
            return field;
        }
    }

    private static final class GetSystemPropertyAction implements PrivilegedAction<Integer> {
        private final String propertyName;
        private final int defaultValue;

        GetSystemPropertyAction(final String propertyName, final int defaultValue) {
            this.propertyName = propertyName;
            this.defaultValue = defaultValue;
        }

        public Integer run() {
            return Integer.getInteger(propertyName, defaultValue);
        }
    }

    private static final class GetCLAction implements PrivilegedAction<ClassLoader> {
        private final Class clazz;

        GetCLAction(final Class clazz) {
            this.clazz = clazz;
        }

        public ClassLoader run() {
            return clazz.getClassLoader();
        }
    }

}
