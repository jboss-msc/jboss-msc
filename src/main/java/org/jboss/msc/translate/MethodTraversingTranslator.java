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

package org.jboss.msc.translate;

import java.lang.reflect.Method;
import java.util.List;
import org.jboss.msc.value.ThreadLocalValue;
import org.jboss.msc.value.Value;
import org.jboss.msc.value.Values;

/**
 * A translator which translates by calling a method and returning its return value.  The input value may be passed in to
 * the constructor using {@link Values#targetValue()}.
 *
 * @param <I> the input type
 * @param <O> the output type
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class MethodTraversingTranslator<I, O> implements Translator<I, O> {
    private final Value<Method> method;
    private final Value<?> target;
    private final List<? extends Value<?>> parameters;

    /**
     * Construct a new instance.
     *
     * @param method the method to invoke (possibly {@link Values#targetValue()})
     * @param target the target (possibly {@link Values#targetValue()})
     * @param parameters the parameters (one or more of which may be {@link Values#targetValue()})
     */
    public MethodTraversingTranslator(final Value<Method> method, final Value<?> target, final List<? extends Value<?>> parameters) {
        this.method = method;
        this.target = target;
        this.parameters = parameters;
    }

    /** {@inheritDoc} */
    @SuppressWarnings({ "unchecked" })
    public O translate(final I input) {
        final ThreadLocalValue<Object> targetValue = Values.targetValue();
        final Object oldThis = targetValue.getAndSetValue(input);
        try {
            return (O) method.getValue().invoke(target.getValue(), Values.getValues(parameters));
        } catch (Exception e) {
            throw new TranslationException(e);
        } finally {
            targetValue.setValue(oldThis);
        }
    }
}
