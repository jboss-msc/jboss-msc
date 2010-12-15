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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import org.jboss.msc.value.ImmediateValue;
import org.jboss.msc.value.ThreadLocalValue;
import org.jboss.msc.value.Value;
import org.jboss.msc.value.Values;

/**
 * An injector which invokes a method.  The value being injected can be specified by {@link Values#injectedValue()}.  The
 * value being invoked upon can be specified by {@link Values#thisValue()}.
 *
 * @param <T> the injection type
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class MethodInjector<T> implements Injector<T> {
    private final Value<Method> methodValue;
    private final Value<?> injectedValue;
    private final Value<?> targetValue;
    private final List<? extends Value<?>> parameterList;

    /**
     * Construct a new instance.
     *
     * @param methodValue the value of the method to invoke
     * @param targetValue the value of the invocation target (the object being called upon) - use {@link Values#nullValue()} for static methods
     * @param injectedValue the value to use for {@link Values#injectedValue()} on uninjection (usually {@link Values#nullValue()})
     * @param parameterList the list of parameter values (any {@code null} parameters should use {@link Values#nullValue()})
     */
    public MethodInjector(final Value<Method> methodValue, final Value<?> targetValue, final Value<?> injectedValue, final List<? extends Value<?>> parameterList) {
        if (methodValue == null) {
            throw new IllegalArgumentException("methodValue is null");
        }
        if (targetValue == null) {
            throw new IllegalArgumentException("targetValue is null");
        }
        if (injectedValue == null) {
            throw new IllegalArgumentException("injectedValue is null");
        }
        if (parameterList == null) {
            throw new IllegalArgumentException("parameterList is null");
        }
        final List<? extends Value<?>> list = new ArrayList<Value<?>>(parameterList);
        final int size = list.size();
        for (int i = 0; i < size; i++) {
            final Value<?> value = list.get(i);
            if (value == null) {
                throw new IllegalArgumentException("parameter value at index " + i + " is null");
            }
        }
        this.methodValue = methodValue;
        this.targetValue = targetValue;
        this.injectedValue = injectedValue;
        this.parameterList = list;
    }

    /** {@inheritDoc} */
    public void inject(final T value) throws InjectionException {
        final ThreadLocalValue<Object> injectedValue = Values.injectedValue();
        final ThreadLocalValue<Object> tlsThisValue = Values.thisValue();
        final Value<?> thisValue = targetValue;
        final Value<?> oldInjectedValue = injectedValue.getAndSetValue((Value<?>) new ImmediateValue<T>(value));
        try {
            final Value<?> oldThis = tlsThisValue.getAndSetValue(thisValue);
            try {
                methodValue.getValue().invoke(thisValue.getValue(), Values.getValues(parameterList));
            } catch (InvocationTargetException e) {
                try {
                    throw e.getCause();
                } catch (InjectionException e2) {
                    throw e2;
                } catch (Throwable throwable) {
                    throw new InjectionException("Injection failed", e);
                }
            } catch (Exception e) {
                throw new InjectionException("Injection failed", e);
            } finally {
                tlsThisValue.setValue(oldThis);
            }
        } finally {
            injectedValue.setValue(oldInjectedValue);
        }
    }

    /** {@inheritDoc} */
    public void uninject() {
        final ThreadLocalValue<Object> injectedValue = Values.injectedValue();
        final ThreadLocalValue<Object> thisValue = Values.thisValue();
        final Value<?> oldTarget = injectedValue.getAndSetValue(this.injectedValue);
        try {
            final Value<?> oldThis = thisValue.getAndSetValue(targetValue);
            try {
                methodValue.getValue().invoke(targetValue.getValue(), Values.getValues(parameterList));
            } catch (Throwable t) {
                InjectorLogger.INSTANCE.uninjectFailed(t, methodValue);
            } finally {
                thisValue.setValue(oldThis);
            }
        } finally {
            injectedValue.setValue(oldTarget);
        }
    }
}
