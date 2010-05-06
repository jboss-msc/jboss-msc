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

package org.jboss.msc.pojo;

import org.jboss.msc.service.ObjectService;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import java.util.List;
import java.lang.reflect.Method;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.ImmediateValue;
import org.jboss.msc.value.Value;
import org.jboss.msc.value.Values;

public final class FactoryService<T> implements ObjectService<T> {

    private final Value<Method> factoryMethod;
    private final Value<?> factory;
    private final List<Value<?>> params;
    private volatile T instance;

    public FactoryService(final Method factoryMethod, final Value<?> factory, final List<Value<?>> params) {
        this.factoryMethod = new ImmediateValue<Method>(factoryMethod);
        this.factory = factory;
        this.params = params;
    }

    @SuppressWarnings({ "unchecked" })
    public void start(final StartContext context) throws StartException {
        try {
            instance = (T) factoryMethod.getValue().invoke(factory.getValue(), Values.getValues(params));
            if (instance == null) {
                throw new IllegalArgumentException("Factory method returned null");
            }
        } catch (Exception e) {
            throw new StartException("Failed to construct object", e);
        }
    }

    @SuppressWarnings({ "unchecked" })
    public Class<T> getType() {
        return (Class<T>) factoryMethod.getValue().getReturnType();
    }

    public void stop(final StopContext context) {
        instance = null;
    }

    public T getValue() throws IllegalStateException {
        final T value = instance;
        if (value == null) {
            throw new IllegalStateException("Not started");
        }
        return value;
    }
}
