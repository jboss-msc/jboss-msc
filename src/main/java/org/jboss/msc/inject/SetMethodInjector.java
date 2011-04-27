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

package org.jboss.msc.inject;

import java.lang.reflect.Method;

import org.jboss.msc.value.Value;

/**
 * An injector which calls a setter method.
 *
 * @param <T> the value type
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class SetMethodInjector<T> implements Injector<T> {

    private static final Object[] NULL_PARAM = new Object[] {null};

    private final Value<?> target;
    private final Method method;

    /**
     * Construct a new instance.
     *
     * @param target the object upon which the method is to be called
     * @param method the method to invoke
     */
    public SetMethodInjector(final Value<?> target, final Method method) {
        this.target = target;
        this.method = method;
    }

    /**
     * Construct a new instance.
     *
     * @param target the object upon which the method is to be called
     * @param method the method to invoke
     * @return the new instance
     */
    public static <T> Injector<T> create(final Value<?> target, final Method method) {
        return new SetMethodInjector<T>(target, method);
    }

    /** {@inheritDoc} */
    public void inject(final T value) {
        try {
            method.invoke(target.getValue(), value);
        } catch (Exception e) {
            throw new InjectionException("Failed to inject value into method", e);
        }
    }

    /** {@inheritDoc} */
    public void uninject() {
        try {
            method.invoke(target.getValue(), NULL_PARAM);
        } catch (Exception e) {
            InjectorLogger.INSTANCE.uninjectFailed(e, method);
        }
    }
}
