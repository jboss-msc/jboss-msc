/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.msc.inject;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import org.jboss.msc.value.Value;

/**
 * An injector which calls an add/remove method pair on inject/uninject.  Note that this class still adheres to
 * the contract of {@code Injector} - namely, the same injector cannot be used to inject multiple instances into
 * a collection.
 *
 * @param <T> the value type
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class AddMethodInjector<T> extends RetainingInjector<T> implements Injector<T> {

    private final Value<?> target;
    private final Method addMethod;
    private final Method removeMethod;

    /**
     * Construct a new instance.
     *
     * @param target the target upon which the add/remove methods should be invoked
     * @param addMethod the add method
     * @param removeMethod the remove method
     */
    public AddMethodInjector(final Value<?> target, final Method addMethod, final Method removeMethod) {
        this.target = target;
        this.addMethod = addMethod;
        this.removeMethod = removeMethod;
    }

    /** {@inheritDoc} */
    public void inject(final T value) {
        super.inject(value);
        try {
            addMethod.invoke(target.getValue(), value);
        } catch (Exception e) {
            throw new InjectionException("Failed to inject value into method", e);
        }
    }

    /** {@inheritDoc} */
    public void uninject() {
        try {
            final Value<T> storedValue = getStoredValue();
            if (storedValue != null) removeMethod.invoke(target.getValue(), storedValue.getValue());
        } catch (InvocationTargetException e) {
            InjectorLogger.INSTANCE.uninjectFailed(e.getCause(), removeMethod);
        } catch (IllegalAccessException e) {
            InjectorLogger.INSTANCE.uninjectFailed(e, removeMethod);
        } finally {
            super.uninject();
        }
    }
}
