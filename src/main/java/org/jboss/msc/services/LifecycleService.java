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

package org.jboss.msc.services;

import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import java.lang.reflect.Method;
import java.util.List;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.ImmediateValue;
import org.jboss.msc.value.ThreadLocalValue;
import org.jboss.msc.value.Value;
import org.jboss.msc.value.Values;

/**
 * A service which calls lifecycle methods on a POJO-style object.
 *
 * @param <T> the target type
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class LifecycleService<T> implements Service<T> {
    private final Value<T> target;
    private final Value<Method> startMethod;
    private final List<Value<?>> startParams;
    private final Value<Method> stopMethod;
    private final List<Value<?>> stopParams;

    /**
     * Construct a new instance.
     *
     * @param target the target object value
     * @param startMethod the start method to call, if any
     * @param startParams the start method parameters to pass
     * @param stopMethod the stop method to call, if any
     * @param stopParams the stop method parameters to pass
     */
    public LifecycleService(final Value<T> target, final Method startMethod, final List<Value<?>> startParams, final Method stopMethod, final List<Value<?>> stopParams) {
        this.target = target;
        this.startMethod = startMethod == null ? Values.<Method>nullValue() : new ImmediateValue<Method>(startMethod);
        this.startParams = startParams;
        this.stopMethod = stopMethod == null ? Values.<Method>nullValue() : new ImmediateValue<Method>(stopMethod);
        this.stopParams = stopParams;
    }

    /**
     * Construct a new instance.
     *
     * @param target the target object value
     * @param startMethod the start method to call, if any
     * @param startParams the start method parameters to pass
     * @param stopMethod the stop method to call, if any
     * @param stopParams the stop method parameters to pass
     */
    public LifecycleService(final Value<T> target, final Value<Method> startMethod, final List<Value<?>> startParams, final Value<Method> stopMethod, final List<Value<?>> stopParams) {
        this.target = target;
        this.startMethod = startMethod;
        this.startParams = startParams;
        this.stopMethod = stopMethod;
        this.stopParams = stopParams;
    }

    /** {@inheritDoc} */
    public void start(final StartContext context) throws StartException {
        final Method startMethod = this.startMethod.getValue();
        if (startMethod != null) {
            final ThreadLocalValue<Object> thisValue = Values.thisValue();
            try {
                final Object target = this.target.getValue();
                final Value<?> old = thisValue.getAndSetValue(new ImmediateValue<Object>(target));
                try {
                    startMethod.invoke(target, Values.getValues(startParams));
                } finally {
                    thisValue.setValue(old);
                }
            } catch (Exception e) {
                throw new StartException("Cannot start bean", e);
            }
        }
    }

    /** {@inheritDoc} */
    public void stop(final StopContext context) {
        try {
            final Method stopMethod = this.stopMethod.getValue();
            if (stopMethod != null) {
                final ThreadLocalValue<Object> thisValue = Values.thisValue();
                final Object target = this.target.getValue();
                final Value<?> old = thisValue.getAndSetValue(new ImmediateValue<Object>(target));
                try {
                    stopMethod.invoke(target, Values.getValues(stopParams));
                } finally {
                    thisValue.setValue(old);
                }
            }
        } catch (Exception e) {
            // todo log it
        }
    }

    /** {@inheritDoc} */
    public T getValue() throws IllegalStateException {
        return target.getValue();
    }
}
