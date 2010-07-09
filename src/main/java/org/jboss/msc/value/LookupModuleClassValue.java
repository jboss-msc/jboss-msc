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

package org.jboss.msc.value;

import org.jboss.modules.ModuleClassLoader;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.modules.ModuleLoadException;

/**
 * A value which looks up a class by name from a module.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class LookupModuleClassValue implements Value<Class<?>> {
    private final String className;
    private final ModuleIdentifier moduleIdentifier;
    private volatile Class<?> result;

    /**
     * Construct a new instance.
     *
     * @param className the name of the class
     * @param moduleIdentifier the module identifier
     */
    public LookupModuleClassValue(final String className, final ModuleIdentifier moduleIdentifier) {
        if (className == null) {
            throw new IllegalArgumentException("className is null");
        }
        if (moduleIdentifier == null) {
            throw new IllegalArgumentException("moduleIdentifier is null");
        }
        this.className = className;
        this.moduleIdentifier = moduleIdentifier;
    }

    /** {@inheritDoc} */
    public Class<?> getValue() throws IllegalStateException {
        Class<?> result = this.result;
        if (result != null) {
            return result;
        }
        synchronized (this) {
            result = this.result;
            if (result != null) {
                return result;
            }
            final ClassLoader classLoader;
            try {
                classLoader = ModuleClassLoader.forModule(moduleIdentifier);
            } catch (ModuleLoadException e) {
                throw new IllegalStateException("No module available with name '" + moduleIdentifier + "'");
            }
            try {
                this.result = (result = Class.forName(className, false, classLoader));
            } catch (ClassNotFoundException e) {
                throw new IllegalStateException("No class available with name '" + className + "'");
            }
            return result;
        }
    }
}