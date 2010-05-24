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

package org.jboss.msc.registry;

import java.lang.reflect.Method;
import java.util.List;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.inject.MethodInjector;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.value.Value;
import org.jboss.msc.value.Values;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
final class MethodInjectionDestination extends InjectionDestination {

    private final Value<Method> methodValue;
    private final List<? extends Value<?>> parameterValues;
    private final Value<?> targetValue;

    MethodInjectionDestination(final Value<Method> methodValue, final List<? extends Value<?>> parameterValues) {
        this.methodValue = methodValue;
        this.parameterValues = parameterValues;
        targetValue = Values.injectedValue();
    }

    public MethodInjectionDestination(final Value<Method> methodValue, final Value<?> targetValue, final List<? extends Value<?>> parameterValues) {
        this.methodValue = methodValue;
        this.parameterValues = parameterValues;
        this.targetValue = targetValue;
    }

    protected <T> Injector<?> getInjector(final Value<T> injectionValue, final ServiceBuilder<T> serviceBuilder, final ServiceRegistryImpl registry) {
        return new MethodInjector<Object>(methodValue, targetValue, injectedValue, parameterValues);
    }
}
