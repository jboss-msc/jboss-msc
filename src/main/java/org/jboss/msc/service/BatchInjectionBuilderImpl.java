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

import org.jboss.msc.inject.Injector;
import org.jboss.msc.reflect.Property;
import org.jboss.msc.translate.Translator;
import org.jboss.msc.value.ClassOfValue;
import org.jboss.msc.value.ImmediateValue;
import org.jboss.msc.value.LookupFieldValue;
import org.jboss.msc.value.LookupMethodValue;
import org.jboss.msc.value.LookupPropertyValue;
import org.jboss.msc.value.Value;
import org.jboss.msc.value.Values;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;

import static org.jboss.msc.service.BatchBuilderImpl.alreadyInstalled;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
final class BatchInjectionBuilderImpl implements BatchInjectionBuilder {

    private final BatchServiceBuilderImpl<?> batchServiceBuilder;
    private final BatchBuilderImpl batchBuilder;

    private Value<?> target;
    private InjectionSource injectionSource;
    private InjectionDestination injectionDestination;

    BatchInjectionBuilderImpl(final BatchServiceBuilderImpl<?> batchServiceBuilder, final InjectionSource injectionSource, final BatchBuilderImpl batchBuilder) {
        this.batchServiceBuilder = batchServiceBuilder;
        this.injectionSource = injectionSource;
        this.batchBuilder = batchBuilder;
        target = batchServiceBuilder.getServiceValue();
    }

    private static IllegalStateException alreadySpecified() {
        return new IllegalStateException("Injection destination already specified");
    }

    public BatchInjectionBuilderImpl toProperty(final String propertyName) {
        if (batchBuilder.isDone()) {
            throw alreadyInstalled();
        }
        if (injectionDestination != null) {
            throw alreadySpecified();
        }
        injectionDestination = new PropertyInjectionDestination(new LookupPropertyValue(new ClassOfValue<Object>(target), propertyName));
        batchServiceBuilder.getInjections().add(this);
        return this;
    }

    public BatchInjectionBuilder toProperty(final Property property) {
        if (batchBuilder.isDone()) {
            throw alreadyInstalled();
        }
        if (injectionDestination != null) {
            throw alreadySpecified();
        }
        injectionDestination = new PropertyInjectionDestination(new ImmediateValue<Property>(property));
        batchServiceBuilder.getInjections().add(this);
        return this;
    }

    public BatchInjectionBuilder toPropertyValue(final Value<Property> propertyValue) {
        if (batchBuilder.isDone()) {
            throw alreadyInstalled();
        }
        if (injectionDestination != null) {
            throw alreadySpecified();
        }
        if (batchBuilder == null) {
            throw new IllegalArgumentException("batchBuilder is null");
        }
        if (injectionDestination == null) {
            throw new IllegalArgumentException("injectionDestination is null");
        }
        injectionDestination = new PropertyInjectionDestination(propertyValue);
        batchServiceBuilder.getInjections().add(this);
        return this;
    }

    public BatchInjectionBuilderImpl toMethod(final String name, final List<? extends Value<Class<?>>> parameterTypes, final List<? extends Value<?>> parameterValues) {
        if (batchBuilder.isDone()) {
            throw alreadyInstalled();
        }
        if (injectionDestination != null) {
            throw alreadySpecified();
        }
        injectionDestination = new MethodInjectionDestination(new LookupMethodValue(new ClassOfValue<Object>(target), name, parameterTypes), target, parameterValues);
        batchServiceBuilder.getInjections().add(this);
        return this;
    }

    public BatchInjectionBuilderImpl toMethodValue(final Value<Method> methodValue, final List<? extends Value<?>> parameterValues) {
        if (batchBuilder.isDone()) {
            throw alreadyInstalled();
        }
        if (injectionDestination != null) {
            throw alreadySpecified();
        }
        injectionDestination = new MethodInjectionDestination(methodValue, target, parameterValues);
        batchServiceBuilder.getInjections().add(this);
        return this;
    }

    public BatchInjectionBuilderImpl toMethod(final Method method, final List<? extends Value<?>> parameterValues) {
        if (batchBuilder.isDone()) {
            throw alreadyInstalled();
        }
        if (injectionDestination != null) {
            throw alreadySpecified();
        }
        injectionDestination = new MethodInjectionDestination(Values.immediateValue(method), target, parameterValues);
        batchServiceBuilder.getInjections().add(this);
        return this;
    }

    public BatchInjectionBuilderImpl toMethod(final Method method, final Value<?> targetValue, final List<? extends Value<?>> parameterValues) {
        if (batchBuilder.isDone()) {
            throw alreadyInstalled();
        }
        if (injectionDestination != null) {
            throw alreadySpecified();
        }
        injectionDestination = new MethodInjectionDestination(Values.immediateValue(method), targetValue, parameterValues);
        batchServiceBuilder.getInjections().add(this);
        return this;
    }

    public BatchInjectionBuilder toMethod(final String name, final Value<?> targetValue, final List<? extends Value<Class<?>>> parameterTypes, final List<? extends Value<?>> parameterValues) {
        if (batchBuilder.isDone()) {
            throw alreadyInstalled();
        }
        if (injectionDestination != null) {
            throw alreadySpecified();
        }
        injectionDestination = new MethodInjectionDestination(new LookupMethodValue(new ClassOfValue<Object>(targetValue), name, parameterTypes), targetValue, parameterValues);
        batchServiceBuilder.getInjections().add(this);
        return this;
    }

    public BatchInjectionBuilder toMethodValue(final Value<Method> methodValue, final Value<?> targetValue, final List<? extends Value<?>> parameterValues) {
        if (batchBuilder.isDone()) {
            throw alreadyInstalled();
        }
        if (injectionDestination != null) {
            throw alreadySpecified();
        }
        injectionDestination = new MethodInjectionDestination(methodValue, targetValue, parameterValues);
        batchServiceBuilder.getInjections().add(this);
        return this;
    }

    public BatchInjectionBuilderImpl toMethod(final String name) {
        if (batchBuilder.isDone()) {
            throw alreadyInstalled();
        }
        if (injectionDestination != null) {
            throw alreadySpecified();
        }
        injectionDestination = new MethodInjectionDestination(new LookupMethodValue(new ClassOfValue<Object>(target), name, 1),  target, Collections.singletonList(Values.injectedValue()));
        batchServiceBuilder.getInjections().add(this);
        return this;
    }

    public BatchInjectionBuilderImpl toMethod(final String name, final Value<?> targetValue) {
        if (batchBuilder.isDone()) {
            throw alreadyInstalled();
        }
        if (injectionDestination != null) {
            throw alreadySpecified();
        }
        injectionDestination = new MethodInjectionDestination(new LookupMethodValue(new ClassOfValue<Object>(targetValue), name, 1),  targetValue, Collections.singletonList(Values.injectedValue()));
        batchServiceBuilder.getInjections().add(this);
        return this;
    }

    public BatchInjectionBuilderImpl toField(final String fieldName) {
        if (batchBuilder.isDone()) {
            throw alreadyInstalled();
        }
        if (injectionDestination != null) {
            throw alreadySpecified();
        }
        injectionDestination = new FieldInjectionDestination(new LookupFieldValue(new ClassOfValue<Object>(target), fieldName));
        batchServiceBuilder.getInjections().add(this);
        return this;
    }

    public BatchInjectionBuilderImpl toField(final Field field) {
        return toFieldValue(new ImmediateValue<Field>(field));
    }

    public BatchInjectionBuilderImpl toFieldValue(final Value<Field> fieldValue) {
        if (batchBuilder.isDone()) {
            throw alreadyInstalled();
        }
        if (injectionDestination != null) {
            throw alreadySpecified();
        }
        injectionDestination = new FieldInjectionDestination(fieldValue);
        batchServiceBuilder.getInjections().add(this);
        return this;
    }

    public BatchInjectionBuilderImpl toInjector(final Injector<?> injector) {
        if (batchBuilder.isDone()) {
            throw alreadyInstalled();
        }
        if (injectionDestination != null) {
            throw alreadySpecified();
        }
        injectionDestination = new InjectorInjectionDestination(injector);
        batchServiceBuilder.getInjections().add(this);
        return this;
    }

    public BatchInjectionBuilderImpl fromProperty(final String propertyName) {
        if (batchBuilder.isDone()) {
            throw alreadyInstalled();
        }
        if (injectionDestination != null) {
            throw alreadySpecified();
        }
        injectionSource = new PropertyDelegatingInjectionSource(injectionSource, propertyName);
        return this;
    }

    public BatchInjectionBuilderImpl fromProperty(final Property property) {
        return fromPropertyValue(new ImmediateValue<Property>(property));
    }

    public BatchInjectionBuilderImpl fromPropertyValue(final Value<Property> propertyValue) {
        if (batchBuilder.isDone()) {
            throw alreadyInstalled();
        }
        if (injectionDestination != null) {
            throw alreadySpecified();
        }
        injectionSource = new PropertyDelegatingInjectionSource(injectionSource, propertyValue);
        return this;
    }

    public BatchInjectionBuilderImpl fromMethod(final String name, final List<? extends Value<Class<?>>> parameterTypes, final List<? extends Value<?>> parameterValues) {
        if (batchBuilder.isDone()) {
            throw alreadyInstalled();
        }
        injectionSource = new MethodDelegatingInjectionSource(injectionSource, name, parameterTypes, parameterValues);
        return this;
    }

    public BatchInjectionBuilderImpl fromMethod(final String name, final Value<?> target, final List<? extends Value<Class<?>>> parameterTypes, final List<? extends Value<?>> parameterValues) {
        if (batchBuilder.isDone()) {
            throw alreadyInstalled();
        }
        injectionSource = new MethodInjectionSource(new LookupMethodValue(new ClassOfValue<Object>(target), name, parameterTypes), target, parameterValues);
        return this;
    }

    public BatchInjectionBuilderImpl fromMethod(final String name) {
        if (batchBuilder.isDone()) {
            throw alreadyInstalled();
        }
        injectionSource = new MethodDelegatingInjectionSource(injectionSource, name, Values.EMPTY_TYPE_LIST, Values.<Object>emptyList());
        return this;
    }

    public BatchInjectionBuilderImpl fromMethod(final String name, final Value<?> targetValue) {
        if (batchBuilder.isDone()) {
            throw alreadyInstalled();
        }
        injectionSource = new MethodInjectionSource(new LookupMethodValue(new ClassOfValue<Object>(targetValue), name, Values.EMPTY_TYPE_LIST), targetValue, Values.<Object>emptyList());
        return this;
    }

    public BatchInjectionBuilderImpl fromMethod(final Method method, final List<? extends Value<?>> parameterValues) {
        return fromMethodValue(new ImmediateValue<Method>(method), parameterValues);
    }

    public BatchInjectionBuilderImpl fromMethod(final Method method, final Value<?> target, final List<? extends Value<?>> parameterValues) {
        return fromMethodValue(new ImmediateValue<Method>(method), target, parameterValues);
    }

    public BatchInjectionBuilderImpl fromMethodValue(final Value<Method> methodValue, final List<? extends Value<?>> parameterValues) {
        if (batchBuilder.isDone()) {
            throw alreadyInstalled();
        }
        injectionSource = new MethodDelegatingInjectionSource(injectionSource, methodValue, parameterValues);
        return this;
    }

    public BatchInjectionBuilderImpl fromMethodValue(final Value<Method> methodValue, final Value<?> target, final List<? extends Value<?>> parameterValues) {
        if (batchBuilder.isDone()) {
            throw alreadyInstalled();
        }
        injectionSource = new MethodInjectionSource(methodValue, target, parameterValues);
        return this;
    }

    public BatchInjectionBuilderImpl fromField(final String fieldName) {
        if (batchBuilder.isDone()) {
            throw alreadyInstalled();
        }
        injectionSource = new FieldDelegatingInjectionSource(injectionSource, fieldName);
        return this;
    }

    public BatchInjectionBuilderImpl fromField(final Field field) {
        return fromFieldValue(new ImmediateValue<Field>(field));
    }

    public BatchInjectionBuilderImpl fromFieldValue(final Value<Field> fieldValue) {
        if (batchBuilder.isDone()) {
            throw alreadyInstalled();
        }
        injectionSource = new FieldDelegatingInjectionSource(injectionSource, fieldValue);
        return this;
    }

    @SuppressWarnings({ "unchecked" })
    @Override
    public BatchInjectionBuilder via(Translator<?, ?> translator) {
        if (batchBuilder.isDone()) {
            throw alreadyInstalled();
        }
        injectionDestination = new TranslatedInjectionDestination(injectionDestination, translator);
        return this;
    }

    InjectionSource getSource() {
        return injectionSource;
    }

    InjectionDestination getDestination() {
        return injectionDestination;
    }
}
