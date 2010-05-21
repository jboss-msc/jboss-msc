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

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.translate.Translator;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.value.Value;
import org.jboss.msc.value.Values;

/**
 * A builder for a specific injection specification.  Create an instance via any of the following methods:
 * <ul>
 * <li>{@link BatchServiceBuilder#addInjection(Object)}</li>
 * <li>{@link BatchServiceBuilder#addInjectionValue(org.jboss.msc.value.Value)}</li>
 * <li>{@link BatchServiceBuilder#addDependency(ServiceName)}</li>
 * <li>{@link BatchServiceBuilder#addDependency(ServiceName, String)}</li>
 * <li>{@link BatchServiceBuilder#addDependency(ServiceName, Value)}</li>
 * </ul>
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public interface BatchInjectionBuilder {

    /**
     * Specify that the target of injection be a property on the destination object.
     *
     * @param property the destination property name
     * @return this builder
     */
    BatchInjectionBuilder toProperty(String property);

    /**
     * Specify that the target of injection be a method on the destination object.  The object being injected can be
     * referenced in the parameter list with the {@link Values#injectedValue()} value; the
     * destination object can be referenced with the {@link Values#thisValue()} value.
     *
     * @param name the method name
     * @param parameterTypes the parameter type values
     * @param parameterValues the parameter values
     * @return this builder
     */
    BatchInjectionBuilder toMethod(String name, List<? extends Value<Class<?>>> parameterTypes, List<? extends Value<?>> parameterValues);

    BatchInjectionBuilder toMethod(Value<Method> methodValue, List<? extends Value<?>> parameterValues);

    BatchInjectionBuilder toMethod(String name);

    /**
     * Specify that the target of injection be an injector.
     *
     * @param injector the target
     * @return this builder
     */
    BatchInjectionBuilder to(Injector<?> injector);

    BatchInjectionBuilder toField(String fieldName);

    BatchInjectionBuilder toField(Value<Field> fieldValue);

    BatchInjectionBuilder fromProperty(String property);

    BatchInjectionBuilder fromMethod(String name, List<? extends Value<Class<?>>> parameterTypes, List<? extends Value<?>> parameterValues);

    BatchInjectionBuilder fromMethod(String name);

    BatchInjectionBuilder fromMethod(Value<Method> methodValue, List<? extends Value<?>> parameterValues);

    BatchInjectionBuilder fromField(String fieldName);

    BatchInjectionBuilder fromField(Value<Field> fieldValue);

    BatchInjectionBuilder from(Value<?> value);

    BatchInjectionBuilder viaProperty(String property);

    BatchInjectionBuilder viaMethod(String name, List<? extends Value<Class<?>>> parameterTypes, List<? extends Value<?>> parameterValues);

    BatchInjectionBuilder viaMethod(Value<Method> methodValue, List<? extends Value<?>> parameterValues);

    BatchInjectionBuilder viaMethod(String name);

    BatchInjectionBuilder viaField(String fieldName);

    BatchInjectionBuilder viaField(Field field);

    /**
     * Add a translator which will translate the injection target.
     *
     * @param translator the translator
     * @return this builder
     */
    BatchInjectionBuilder via(Translator<?, ?> translator);
}
