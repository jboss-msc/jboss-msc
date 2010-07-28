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

package org.jboss.msc.service;

import org.jboss.msc.value.MethodValue;
import org.jboss.msc.value.Value;

import java.lang.reflect.Method;
import java.util.List;

/**
 * @author John E. Bailey
 */
public class MethodInjectionSource extends InjectionSource {
    private final Value<Method> methodValue;
    private final Value<?> targetValue;
    private final List<? extends Value<?>> parameteres;

    public MethodInjectionSource(Value<Method> methodValue, Value<?> targetValue, List<? extends Value<?>> parameteres) {
        this.methodValue = methodValue;
        this.targetValue = targetValue;
        this.parameteres = parameteres;
    }

    @Override
    protected <T> Value<?> getValue(Value<T> serviceValue, ServiceContainerImpl container) {
        return new MethodValue<T>(methodValue, targetValue, parameteres);
    }
}
