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

package org.jboss.msc.inject;

import java.lang.reflect.Method;
import org.jboss.logging.Logger;
import org.jboss.msc.value.Value;

public final class SetMethodInjector<T> implements Injector<T> {
    private static final Logger log = Logger.getI18nLogger("org.jboss.msc.inject.method", null, "MSC");

    private final Value<?> target;
    private final Value<Method> methodValue;

    public SetMethodInjector(final Value<?> target, final Value<Method> methodValue) {
        this.target = target;
        this.methodValue = methodValue;
    }

    public void inject(final T value) {
        try {
            methodValue.getValue().invoke(target.getValue(), value);
        } catch (Exception e) {
            throw new InjectionException("Failed to inject value into method", e);
        }
    }

    public void uninject() {
        try {
            methodValue.getValue().invoke(target.getValue());
        } catch (Exception e) {
            log.warnf(e, "Unexpected failure to uninject method");
        }
    }
}
