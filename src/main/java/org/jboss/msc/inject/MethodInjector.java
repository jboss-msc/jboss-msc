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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
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
    private final Value<?> thisValue;
    private final List<? extends Value<?>> parameterList;

    public MethodInjector(final Value<Method> methodValue, final Value<?> thisValue, final List<? extends Value<?>> parameterList) {
        this.methodValue = methodValue;
        this.thisValue = thisValue;
        this.parameterList = parameterList;
    }

    public void inject(final T value) throws InjectionException {
        final ThreadLocalValue<Object> tlsTargetValue = Values.injectedValue();
        final ThreadLocalValue<Object> tlsThisValue = Values.thisValue();
        final Value<?> thisValue = this.thisValue;
        final Value<?> oldTarget = tlsTargetValue.getAndSetValue((Value<?>) new ImmediateValue<T>(value));
        try {
            final Value<?> oldThis = tlsThisValue.getAndSetValue(thisValue);
            try {
                methodValue.getValue().invoke(thisValue.getValue(), Values.getValues(parameterList));
            } catch (Exception e) {
                throw new InjectionException("Injection failed", e);
            } finally {
                tlsThisValue.setValue(oldThis);
            }
        } finally {
            tlsTargetValue.setValue(oldTarget);
        }
    }

    public void uninject() {
        final ThreadLocalValue<Object> tlsTargetValue = Values.injectedValue();
        final ThreadLocalValue<Object> tlsThisValue = Values.thisValue();
        final Value<?> oldTarget = tlsTargetValue.getAndSetValue(Values.nullValue());
        try {
            final Value<?> oldThis = tlsThisValue.getAndSetValue(thisValue);
            try {
                methodValue.getValue().invoke(thisValue.getValue(), Values.getValues(parameterList));
            } catch (InvocationTargetException e) {
                // todo log it
            } catch (IllegalAccessException e) {
                // todo log it
            } finally {
                tlsThisValue.setValue(oldThis);
            }
        } finally {
            tlsTargetValue.setValue(oldTarget);
        }
    }
}
