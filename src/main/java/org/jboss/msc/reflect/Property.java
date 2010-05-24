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

package org.jboss.msc.reflect;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;

/**
 * A JavaBean-style property.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class Property {
    private final Class<?> declaringClass;
    private final String name;
    private final Method getter;
    private final Map<Class<?>, Method> setters;

    private Property(Class<?> declaringClass, String name, Method[] methods) {
        this.declaringClass = declaringClass;
        this.name = name;
        final Map<Class<?>, Method> setters = new IdentityHashMap<Class<?>, Method>(0);
        Method getter = null;
        final String capProperty = Character.toUpperCase(name.charAt(0)) + name.substring(1);
        String getName = "get" + capProperty;
        String isName = "is" + capProperty;
        String setName = "set" + capProperty;
        for (Method method : methods) {
            final String methodName = method.getName();
            if (methodName.equals(getName) || methodName.equals(isName)) {
                final Class<?>[] paramTypes = method.getParameterTypes();
                if (paramTypes.length == 0) {
                    if (getter != null) {
                        throw new IllegalArgumentException("Ambiguous getter methods");
                    }
                    getter = method;
                }
            } else if (methodName.equals(setName)) {
                final Class<?>[] paramTypes = method.getParameterTypes();
                if (paramTypes.length == 1) {
                    setters.put(paramTypes[0], method);
                }
            }
        }
        this.getter = getter;
        if (setters.size() == 1) {
            final Map.Entry<Class<?>, Method> entry = setters.entrySet().iterator().next();
            this.setters = Collections.<Class<?>, Method>singletonMap(entry.getKey(), entry.getValue());
        } else {
            this.setters = setters;
        }
    }

    private Property(Class<?> declaringClass, String name) {
        this(declaringClass, name, declaringClass.getMethods());
    }

    private Property(final Class<?> declaringClass, String name, AccessControlContext context) {
        this(declaringClass, name, AccessController.doPrivileged(new PrivilegedAction<Method[]>() {
            public Method[] run() {
                return declaringClass.getDeclaredMethods();
            }
        }, context));
    }

    public static Property getProperty(Class<?> declaringClass, String name) {
        return new Property(declaringClass, name);
    }

    public static Property getNonPublicProperty(Class<?> declaringClass, String name, AccessControlContext context) {
        return new Property(declaringClass, name, context);
    }

    public Object get(Object target) throws InvocationTargetException, IllegalAccessException {
        return getter.invoke(target);
    }

    public void set(Object target, Object value) throws InvocationTargetException, IllegalAccessException {
        set(target, value, value.getClass());
    }

    public void set(Object target, Object value, Class<?> matchClass) throws InvocationTargetException, IllegalAccessException {
        if (setters.size() == 1) {
            // special optimized case; only one candidate setter so just try it
            setters.values().iterator().next().invoke(target, value);
            return;
        }
        int i = 0;
        int v;
        do {
            v = trySet(target, value, matchClass, i++);
        } while (v > 0);
        if (v == 0) {
            throw new IllegalArgumentException("No matching setter found for " + matchClass);
        }
    }

    private int trySet(Object target, Object value, Class<?> matchClass, int depth) throws InvocationTargetException, IllegalAccessException {
        if (depth == 0) {
            Method method = setters.get(matchClass);
            if (method != null) {
                method.invoke(target, value);
                // done!
                return -1;
            } else {
                return 0;
            }
        } else {
            int r = 0;
            depth --;
            final Class<?> superClass = matchClass.getSuperclass();
            if (superClass != null) {
                int v = trySet(target, value, superClass, depth);
                if (v == -1) {
                    return -1;
                }
                r = r + v + 1;
            }
            for (Class<?> interf : matchClass.getInterfaces()) {
                int v = trySet(target, value, interf, depth);
                if (v == -1) {
                    return -1;
                }
                r = r + v + 1;
            }
            return r;
        }
    }

    public Class<?> getDeclaringClass() {
        return declaringClass;
    }

    public String getName() {
        return name;
    }
}
