/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2009, Red Hat Middleware LLC, and individual contributors
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
import org.jboss.msc.value.Value;
import org.jboss.msc.value.Values;

public final class LifecycleService<T> implements Service {
    private final Value<T> target;
    private final Value<Method> startMethod;
    private final List<Value<?>> startParams;
    private final Value<Method> stopMethod;
    private final List<Value<?>> stopParams;

    public LifecycleService(final Value<T> target, final Method startMethod, final List<Value<?>> startParams, final Method stopMethod, final List<Value<?>> stopParams) {
        this.target = target;
        this.startMethod = startMethod == null ? Values.<Method>nullValue() : new ImmediateValue<Method>(startMethod);
        this.startParams = startParams;
        this.stopMethod = stopMethod == null ? Values.<Method>nullValue() : new ImmediateValue<Method>(stopMethod);
        this.stopParams = stopParams;
    }

    public LifecycleService(final Value<T> target, final Value<Method> startMethod, final List<Value<?>> startParams, final Value<Method> stopMethod, final List<Value<?>> stopParams) {
        this.target = target;
        this.startMethod = startMethod;
        this.startParams = startParams;
        this.stopMethod = stopMethod;
        this.stopParams = stopParams;
    }

    public void start(final StartContext context) throws StartException {
        final Method startMethod = this.startMethod.getValue();
        if (startMethod != null) {
            try {
                startMethod.invoke(target.getValue(), Values.getValues(startParams));
            } catch (Exception e) {
                throw new StartException("Cannot start bean", e);
            }
        }
    }

    public void stop(final StopContext context) {
        try {
            final Method stopMethod = this.stopMethod.getValue();
            if (stopMethod != null) {
                stopMethod.invoke(target.getValue(), Values.getValues(stopParams));
            }
        } catch (Exception e) {
            // todo log it
        }
    }
}
