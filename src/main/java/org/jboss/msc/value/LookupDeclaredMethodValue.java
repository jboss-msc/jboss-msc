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

import static org.jboss.msc.value.ErrorMessage.noSuchMethod;

import java.lang.reflect.Method;
import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.List;

/**
 * A value which looks up a possibly non-public method by name and parameters from a class.  This may be considerably slower than
 * {@link LookupMethodValue} so that class should be used whenever possible.
 *
 * @deprecated Will be removed before 1.0.0.GA
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
@Deprecated
public final class LookupDeclaredMethodValue implements Value<Method> {
    private final Value<Class<?>> target;
    private final String methodName;
    private final List<? extends Value<Class<?>>> parameterTypes;
    private final AccessControlContext context;
    private final boolean makeAccessible;
    private final int paramCount;

    /**
     * Construct a new instance.
     *
     * @param target the class in which to look for the method
     * @param methodName the name of the method
     * @param parameterTypes the method parameter types
     * @param context the access control context to use
     * @param makeAccessible {@code true} to make the method accessible under the provided access control context
     */
    public LookupDeclaredMethodValue(final Value<Class<?>> target, final String methodName, final List<? extends Value<Class<?>>> parameterTypes, final AccessControlContext context, final boolean makeAccessible) {
        if (target == null) {
            throw new IllegalArgumentException("target is null");
        }
        if (methodName == null) {
            throw new IllegalArgumentException("methodName is null");
        }
        if (parameterTypes == null) {
            throw new IllegalArgumentException("parameterTypes is null");
        }
        if (context == null) {
            throw new IllegalArgumentException("context is null");
        }
        this.target = target;
        this.methodName = methodName;
        this.parameterTypes = parameterTypes;
        this.paramCount = parameterTypes.size();
        this.context = context;
        this.makeAccessible = makeAccessible;
    }

    public LookupDeclaredMethodValue(final Value<Class<?>> target, final String methodName, final int paramCount,  final AccessControlContext context, final boolean makeAccessible) {
        if (target == null) {
            throw new IllegalArgumentException("target is null");
        }
        if (methodName == null) {
            throw new IllegalArgumentException("methodName is null");
        }
        if (context == null) {
            throw new IllegalArgumentException("context is null");
        }
        this.target = target;
        this.methodName = methodName;
        parameterTypes = null;
        this.paramCount = paramCount;
        this.context = context;
        this.makeAccessible = makeAccessible;
    }

    /** {@inheritDoc} */
    public Method getValue() throws IllegalStateException {
        final Class<?>[] types = parameterTypes == null? null: new Class[parameterTypes.size()];
        if (types != null) {
            int i = 0;
            for (Value<Class<?>> type : parameterTypes) {
                types[i++] = type.getValue();
            }
        }
        final Class<?> targetClass = target.getValue();
        final SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            return AccessController.doPrivileged(new PrivilegedAction<Method>() {
                public Method run() {
                    return getMethod(targetClass, types);
                }
            }, context);
        }
        return getMethod(targetClass, types);
    }

    private Method getMethod(Class<?> targetClass, Class<?>[] types) {
        Method method = null;
        if (types != null) {
            try {
                method = targetClass.getDeclaredMethod(methodName, types);
            } catch (NoSuchMethodException e) {
                throw new IllegalStateException(noSuchMethod(targetClass, methodName, parameterTypes));
            }
        } else {
            final int paramCount = this.paramCount;
            for (Method declaredMethod : targetClass.getDeclaredMethods()) {
                if (declaredMethod.getName().equals(methodName) && declaredMethod.getParameterTypes().length == paramCount) {
                    method = declaredMethod;
                    break;
                }
            }
            if (method == null) {
                throw new IllegalStateException("No such method '" + methodName + "' found on " + targetClass);
            }
        }
        if (makeAccessible) method.setAccessible(true);
        return method;
    }
}