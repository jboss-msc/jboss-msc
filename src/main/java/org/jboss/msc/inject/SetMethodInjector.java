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

package org.jboss.msc.inject;

import java.lang.reflect.Method;
import org.jboss.msc.value.ImmediateValue;
import org.jboss.msc.value.Value;

/**
 * An injector which calls a setter method.
 *
 * @param <T> the value type
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class SetMethodInjector<T> implements Injector<T> {

    private final static Object[] NULL_PARAM = new Object[] {null};

    private final Value<?> target;
    private final Value<Method> methodValue;

    /**
     * Construct a new instance.
     *
     * @param target the object upon which the method is to be called
     * @param methodValue the method to invoke
     */
    public SetMethodInjector(final Value<?> target, final Value<Method> methodValue) {
        this.target = target;
        this.methodValue = methodValue;
    }

    /**
     * Construct a new instance.
     *
     * @param target the object upon which the method is to be called
     * @param method the method to invoke
     */
    public SetMethodInjector(final Value<?> target, final Method method) {
        this(target, new ImmediateValue<Method>(method));
    }

    /**
     * Construct a new instance.
     *
     * @param target the object upon which the method is to be called
     * @param methodValue the method to invoke
     */
    public static <T> Injector<T> create(final Value<?> target, Value<Method> methodValue) {
        return new SetMethodInjector<T>(target, methodValue);
    }

    /**
     * Construct a new instance.
     *
     * @param target the object upon which the method is to be called
     * @param method the method to invoke
     */
    public static <T> Injector<T> create(final Value<?> target, final Method method) {
        return new SetMethodInjector<T>(target, method);
    }

    /**
     * Construct a new instance.
     *
     * @param target the object upon which the method is to be called
     * @param clazz the class upon which the method may be found
     * @param methodName the setter method name
     * @param paramType the parameter type
     * @param <C> the type of the class upon which the method may be found
     */
    public <C> SetMethodInjector(final Value<? extends C> target, final Class<C> clazz, final String methodName, final Class<T> paramType) {
        this(target, lookupMethod(clazz, methodName, paramType));
    }

    /**
     * Construct a new instance.
     *
     * @param target the object upon which the method is to be called
     * @param clazz the class upon which the method may be found
     * @param methodName the setter method name
     * @param paramType the parameter type
     * @param <C> the type of the class upon which the method may be found
     */
    public <C> SetMethodInjector(final C target, final Class<C> clazz, final String methodName, final Class<T> paramType) {
        this(new ImmediateValue<C>(target), clazz, methodName, paramType);
    }

    private static <C, T> Method lookupMethod(final Class<C> clazz, final String methodName, final Class<T> paramType) {
        final Method method;
        try {
            method = clazz.getMethod(methodName, paramType);
        } catch (NoSuchMethodException e) {
            throw new IllegalArgumentException("No such method found", e);
        }
        return method;
    }

    /** {@inheritDoc} */
    public void inject(final T value) {
        try {
            methodValue.getValue().invoke(target.getValue(), value);
        } catch (Exception e) {
            throw new InjectionException("Failed to inject value into method", e);
        }
    }

    /** {@inheritDoc} */
    public void uninject() {
        try {
            methodValue.getValue().invoke(target.getValue(), NULL_PARAM);
        } catch (Exception e) {
            InjectorLogger.INSTANCE.uninjectFailed(e, methodValue);
        }
    }
}
