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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.translate.Translator;
import org.jboss.msc.value.ClassOfValue;
import org.jboss.msc.value.ImmediateValue;
import org.jboss.msc.value.LookupGetMethodValue;
import org.jboss.msc.value.LookupMethodValue;
import org.jboss.msc.value.MethodValue;
import org.jboss.msc.value.Value;
import org.jboss.msc.value.Values;

import static org.jboss.msc.registry.BatchBuilderImpl.alreadyInstalled;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
final class BatchInjectionBuilderImpl implements BatchInjectionBuilder {

    private final List<Translator<?, ?>> translators = new ArrayList<Translator<?,?>>();
    private final BatchServiceBuilderImpl<?> batchServiceBuilder;
    private final BatchBuilderImpl batchBuilder;

    private Value<?> target;
    private Value<?> injectionValue;
    private InjectionSource injectionSource;
    private InjectionDestination injectionDestination;

    BatchInjectionBuilderImpl(final BatchServiceBuilderImpl<?> batchServiceBuilder, final InjectionSource injectionSource, final BatchBuilderImpl batchBuilder) {
        this.batchServiceBuilder = batchServiceBuilder;
        this.injectionSource = injectionSource;
        this.batchBuilder = batchBuilder;
    }

    private static IllegalStateException alreadySpecified() {
        return new IllegalStateException("Injection destination already specified");
    }

    public BatchInjectionBuilderImpl toProperty(final String property) {
        if (batchBuilder.isDone()) {
            throw alreadyInstalled();
        }
        if (injectionDestination != null) {
            throw alreadySpecified();
        }
        injectionDestination = new PropertyInjectionDestination(property);
        return this;
    }

    public BatchInjectionBuilderImpl toMethod(final String name, final List<? extends Value<Class<?>>> parameterTypes, final List<? extends Value<?>> parameterValues) {
        if (batchBuilder.isDone()) {
            throw alreadyInstalled();
        }
        if (injectionDestination != null) {
            throw alreadySpecified();
        }
        injectionDestination = new MethodInjectionDestination(new LookupMethodValue(new ClassOfValue<Object>(target), name, parameterTypes), parameterValues);
        return this;
    }

    public BatchInjectionBuilderImpl toMethodValue(final Value<Method> methodValue, final List<? extends Value<?>> parameterValues) {
        if (batchBuilder.isDone()) {
            throw alreadyInstalled();
        }
        if (injectionDestination != null) {
            throw alreadySpecified();
        }
        injectionDestination = new MethodInjectionDestination(methodValue, parameterValues);
        return this;
    }

    public BatchInjectionBuilderImpl toMethod(final String name) {
        if (batchBuilder.isDone()) {
            throw alreadyInstalled();
        }
        if (injectionDestination != null) {
            throw alreadySpecified();
        }
        injectionDestination = new MethodInjectionDestination(new LookupMethodValue(new ClassOfValue<Object>(target), name, 1), Collections.singletonList(Values.injectedValue()));
        return this;
    }

    public BatchInjectionBuilderImpl toField(final String fieldName) {
        if (batchBuilder.isDone()) {
            throw alreadyInstalled();
        }
        if (injectionDestination != null) {
            throw alreadySpecified();
        }
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
        return this;
    }

    public BatchInjectionBuilderImpl to(final Injector<?> injector) {
        if (batchBuilder.isDone()) {
            throw alreadyInstalled();
        }
        if (injectionDestination != null) {
            throw alreadySpecified();
        }
        injectionDestination = new InjectorInjectionDestination(injector);
        return this;
    }

    public BatchInjectionBuilderImpl fromProperty(final String property) {
        if (batchBuilder.isDone()) {
            throw alreadyInstalled();
        }
        target = new MethodValue<Object>(new LookupGetMethodValue(new ClassOfValue<Object>(target), property), target, Values.emptyList());
        return this;
    }

    public BatchInjectionBuilderImpl fromMethod(final String name, final List<? extends Value<Class<?>>> parameterTypes, final List<? extends Value<?>> parameterValues) {
        if (batchBuilder.isDone()) {
            throw alreadyInstalled();
        }
        target = new MethodValue<Object>(new LookupMethodValue(new ClassOfValue<Object>(target), name, parameterTypes), target, parameterValues);
        return this;
    }

    public BatchInjectionBuilderImpl fromMethod(final String name) {
        if (batchBuilder.isDone()) {
            throw alreadyInstalled();
        }
        return this;
    }

    public BatchInjectionBuilderImpl fromMethod(final Method method, final List<? extends Value<?>> parameterValues) {
        return fromMethodValue(new ImmediateValue<Method>(method), parameterValues);
    }

    public BatchInjectionBuilderImpl fromMethodValue(final Value<Method> methodValue, final List<? extends Value<?>> parameterValues) {
        if (batchBuilder.isDone()) {
            throw alreadyInstalled();
        }
        return this;
    }

    public BatchInjectionBuilderImpl fromField(final String fieldName) {
        if (batchBuilder.isDone()) {
            throw alreadyInstalled();
        }
        return this;
    }

    public BatchInjectionBuilderImpl fromField(final Field field) {
        return fromFieldValue(new ImmediateValue<Field>(field));
    }

    public BatchInjectionBuilderImpl fromFieldValue(final Value<Field> fieldValue) {
        if (batchBuilder.isDone()) {
            throw alreadyInstalled();
        }
        return this;
    }

    public BatchInjectionBuilderImpl fromValue(final Value<?> value) {
        if (batchBuilder.isDone()) {
            throw alreadyInstalled();
        }
        return this;
    }

    public BatchInjectionBuilderImpl from(final Object object) {
        return fromValue(new ImmediateValue<Object>(object));
    }

    public BatchInjectionBuilderImpl viaProperty(final String property) {
        if (batchBuilder.isDone()) {
            throw alreadyInstalled();
        }
        return this;
    }

    public BatchInjectionBuilderImpl viaMethod(final String name, final List<? extends Value<Class<?>>> parameterTypes, final List<? extends Value<?>> parameterValues) {
        if (batchBuilder.isDone()) {
            throw alreadyInstalled();
        }
        return this;
    }

    public BatchInjectionBuilderImpl viaMethod(final Method method, final List<? extends Value<?>> parameterValues) {
        return viaMethodValue(new ImmediateValue<Method>(method), parameterValues);
    }

    public BatchInjectionBuilderImpl viaMethodValue(final Value<Method> methodValue, final List<? extends Value<?>> parameterValues) {
        if (batchBuilder.isDone()) {
            throw alreadyInstalled();
        }
        return this;
    }

    public BatchInjectionBuilderImpl viaMethod(final String name) {
        if (batchBuilder.isDone()) {
            throw alreadyInstalled();
        }
        return this;
    }

    public BatchInjectionBuilderImpl viaField(final String fieldName) {
        if (batchBuilder.isDone()) {
            throw alreadyInstalled();
        }
        return this;
    }

    public BatchInjectionBuilderImpl viaField(final Field field) {
        return viaFieldValue(new ImmediateValue<Field>(field));
    }

    public BatchInjectionBuilderImpl viaFieldValue(final Value<Field> fieldValue) {
        if (batchBuilder.isDone()) {
            throw alreadyInstalled();
        }
        return this;
    }

    public BatchInjectionBuilderImpl via(final Translator<?, ?> translator) {
        if (batchBuilder.isDone()) {
            throw alreadyInstalled();
        }
        translators.add(translator);
        return this;
    }

    InjectionSource getSource() {
        return injectionSource;
    }

    InjectionDestination getDestination() {
        return injectionDestination;
    }
}
