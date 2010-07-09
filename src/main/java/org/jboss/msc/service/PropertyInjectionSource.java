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

import java.util.Collections;
import org.jboss.msc.value.ClassOfValue;
import org.jboss.msc.value.LookupGetMethodValue;
import org.jboss.msc.value.MethodValue;
import org.jboss.msc.value.Value;
import org.jboss.msc.value.Values;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
final class PropertyInjectionSource extends InjectionSource {

    private final ServiceName dependency;
    private final String propertySpec;

    PropertyInjectionSource(final ServiceName dependency, final String propertySpec) {
        this.dependency = dependency;
        this.propertySpec = propertySpec;
    }

    protected <T> Value<?> getValue(final Value<T> serviceValue, final ServiceBuilder<T> serviceBuilder, final ServiceContainerImpl registry) {
        final String propertySpec = this.propertySpec;
        Value<?> prevValue = registry.getService(dependency);
        for (int current = 0, next = propertySpec.indexOf('.'); current != -1; current = next, next = propertySpec.indexOf('.', current + 1)) {
            final String property = next == -1 ? propertySpec.substring(current) : propertySpec.substring(current, next);
            prevValue = Values.cached(new MethodValue<Object>(new LookupGetMethodValue(new ClassOfValue<Object>(prevValue), property), prevValue, Collections.<Value<?>>emptyList()));
        }
        return prevValue;
    }
}
