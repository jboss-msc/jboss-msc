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

package org.jboss.msc.service;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.reflect.Property;
import org.jboss.msc.value.Value;
import org.jboss.msc.value.Values;

/**
 * A builder for a specific injection specification.  Create an instance via any of the following methods:
 * <ul>
 * <li>{@link BatchServiceBuilder#addInjection(Object)}</li>
 * <li>{@link BatchServiceBuilder#addInjectionValue(org.jboss.msc.value.Value)}</li>
 * <li>{@link BatchServiceBuilder#addDependency(ServiceName)}</li>
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
     * Specify that the target of injection be a property on the destination object.
     *
     * @param property the destination property
     * @return this builder
     */
    BatchInjectionBuilder toProperty(Property property);

    /**
     * Specify that the target of injection be a property on the destination object.
     *
     * @param propertyValue the value of the destination property
     * @return this builder
     */
    BatchInjectionBuilder toPropertyValue(Value<Property> propertyValue);

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

    /**
     * Specify that the target of injection be a method on the destination object.  The object being injected can be
     * referenced in the parameter list with the {@link Values#injectedValue()} value; the
     * destination object can be referenced with the {@link Values#thisValue()} value.
     *
     * @param method the method
     * @param parameterValues the parameter values
     * @return this builder
     */
    BatchInjectionBuilder toMethod(Method method, List<? extends Value<?>> parameterValues);

    /**
     * Specify that the target of injection be a method on the destination object.  The object being injected can be
     * referenced in the parameter list with the {@link Values#injectedValue()} value; the
     * destination object can be referenced with the {@link Values#thisValue()} value.
     *
     * @param methodValue the method value
     * @param parameterValues the parameter values
     * @return this builder
     */
    BatchInjectionBuilder toMethodValue(Value<Method> methodValue, List<? extends Value<?>> parameterValues);

    /**
     * Specify that the target of injection be a method on the destination object.  The object being injected can be
     * referenced in the parameter list with the {@link Values#injectedValue()} value; the
     * destination object can be referenced with the {@link Values#thisValue()} value.
     *
     * @param name the method name
     * @param targetValue the value upon which the method should be invoked (use {@link Values#nullValue()} for static methods)
     * @param parameterTypes the parameter type values
     * @param parameterValues the parameter values
     * @return this builder
     */
    BatchInjectionBuilder toMethod(String name, Value<?> targetValue, List<? extends Value<Class<?>>> parameterTypes, List<? extends Value<?>> parameterValues);

    /**
     * Specify that the target of injection be a method on the destination object.  The object being injected can be
     * referenced in the parameter list with the {@link Values#injectedValue()} value; the
     * destination object can be referenced with the {@link Values#thisValue()} value.
     *
     * @param method the method
     * @param parameterValues the parameter values
     * @return this builder
     */
    BatchInjectionBuilder toMethod(Method method, Value<?> targetValue,  List<? extends Value<?>> parameterValues);

    /**
     * Specify that the target of injection be a method on the destination object.  The object being injected can be
     * referenced in the parameter list with the {@link Values#injectedValue()} value; the
     * destination object can be referenced with the {@link Values#thisValue()} value.
     *
     * @param methodValue the method value
     * @param targetValue the value upon which the method should be invoked (use {@link Values#nullValue()} for static methods)
     * @param parameterValues the parameter values
     * @return this builder
     */
    BatchInjectionBuilder toMethodValue(Value<Method> methodValue, Value<?> targetValue, List<? extends Value<?>> parameterValues);

    /**
     * Specify that the target of injection be a one-argument method on the destination object.
     *
     * @param name the method name
     * @return this builder
     */
    BatchInjectionBuilder toMethod(String name);

    /**
     * Specify that the target of injection be a one-argument method on the destination object.
     *
     * @param name the method name
     * @param targetValue  the value upon which the method should be invoked (use {@link Values#nullValue()} for static methods)
     * @return this builder
     */
    BatchInjectionBuilder toMethod(String name, Value<?> targetValue);

    /**
     * Specify that the target of injection be a field on the target object.
     *
     * @param fieldName the name of the field
     * @return this builder
     */
    BatchInjectionBuilder toField(String fieldName);

    /**
     * Specify that the target of injection be a field on the target object.
     *
     * @param field the field
     * @return this builder
     */
    BatchInjectionBuilder toField(Field field);

    /**
     * Specify that the target of injection be a field on the target object.
     *
     * @param fieldValue the field value
     * @return this builder
     */
    BatchInjectionBuilder toFieldValue(Value<Field> fieldValue);

    /**
     * Specify that the target of injection be an injector.
     *
     * @param injector the target
     * @return this builder
     */
    BatchInjectionBuilder toInjector(Injector<?> injector);


    /**
     * Specify that the injected value should come from a property on the source object.
     *
     * @param propertyName the property name
     * @return this builder
     */
    BatchInjectionBuilder fromProperty(String propertyName);

    /**
     * Specify that the injected value should come from a property on the source object.
     *
     * @param property the property
     * @return this builder
     */
    BatchInjectionBuilder fromProperty(Property property);

    /**
     * Specify that the injected value should come from a property on the source object.
     *
     * @param propertyValue the property value
     * @return this builder
     */
    BatchInjectionBuilder fromPropertyValue(Value<Property> propertyValue);

    /**
     * Specify that the injected value should come from the result of a method call.  The
     * source object can be referenced with the {@link Values#thisValue()} value.
     *
     * @param name the name of the method to invoke
     * @param parameterTypes the parameter types of the method
     * @param parameterValues the parameter values
     * @return this builder
     */
    BatchInjectionBuilder fromMethod(String name, List<? extends Value<Class<?>>> parameterTypes, List<? extends Value<?>> parameterValues);

    /**
     * Specify that the injected value should come from the result of a method call.  The
     * source object can be referenced with the {@link Values#thisValue()} value.
     *
     * @param name the name of the method to invoke
     * @param target the object upon which to invoke the method (use {@link Values#nullValue()} for static methods)
     * @param parameterTypes the parameter types of the method
     * @param parameterValues the parameter values
     * @return this builder
     */
    BatchInjectionBuilder fromMethod(String name, Value<?> target, List<? extends Value<Class<?>>> parameterTypes, List<? extends Value<?>> parameterValues);

    /**
     * Specify that the injected value should come from the result of a no-args method call.
     *
     * @param name the name of the method to invoke
     * @return this builder
     */
    BatchInjectionBuilder fromMethod(String name);

    /**
     * Specify that the injected value should come from the result of a method call.  The
     * source object can be referenced with the {@link Values#thisValue()} value.
     *
     * @param method the method to invoke
     * @param parameterValues the parameter values
     * @return this builder
     */
    BatchInjectionBuilder fromMethod(Method method, List<? extends Value<?>> parameterValues);

    /**
     * Specify that the injected value should come from the result of a method call.  The
     * source object can be referenced with the {@link Values#thisValue()} value.
     *
     * @param method the method to invoke
     * @param target the object upon which to invoke the method (use {@link Values#nullValue()} for static methods)
     * @param parameterValues the parameter values
     * @return this builder
     */
    BatchInjectionBuilder fromMethod(Method method, Value<?> target, List<? extends Value<?>> parameterValues);

    /**
     * Specify that the injected value should come from the result of a method call.  The
     * source object can be referenced with the {@link Values#thisValue()} value.
     *
     * @param methodValue the method value to invoke
     * @param parameterValues the parameter values
     * @return this builder
     */
    BatchInjectionBuilder fromMethodValue(Value<Method> methodValue, List<? extends Value<?>> parameterValues);

    /**
     * Specify that the injected value should come from the result of a method call.  The
     * source object can be referenced with the {@link Values#thisValue()} value.
     *
     * @param methodValue the method value to invoke
     * @param target the object upon which to invoke the method (use {@link Values#nullValue()} for static methods)
     * @param parameterValues the parameter values
     * @return this builder
     */
    BatchInjectionBuilder fromMethodValue(Value<Method> methodValue, Value<?> target, List<? extends Value<?>> parameterValues);

    BatchInjectionBuilder fromField(String fieldName);

    BatchInjectionBuilder fromField(Field field);

    BatchInjectionBuilder fromFieldValue(Value<Field> fieldValue);

//    /**
//     * Add a translator which will translate the injection target.
//     *
//     * @param translator the translator
//     * @return this builder
//     */
//    BatchInjectionBuilder via(Translator<?, ?> translator);
}
