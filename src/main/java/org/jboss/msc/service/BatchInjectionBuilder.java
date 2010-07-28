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

/**
 * A builder for a specific injection specification.  Create an instance via any of the following methods:
 * <ul>
 * <li>{@link BatchServiceBuilder#addInjection(Object)}</li>
 * <li>{@link BatchServiceBuilder#addInjectionValue(org.jboss.msc.value.Value)}</li>
 * <li>{@link BatchServiceBuilder#addDependency(ServiceName)}</li>
 * </ul>
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 * @deprecated This interface should no longer be used, and will be removed before 1.0.0.Beta3.
 */
public interface BatchInjectionBuilder {

    /**
     * Specify that the target of injection be an injector.
     *
     * @param injector the target
     * @return this builder
     * @deprecated This method does not implement proper type safety and will be removed before 1.0.0.Beta3.
     */
    BatchInjectionBuilder toInjector(Injector<?> injector);
}
