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

import static org.jboss.msc.service.BatchBuilderImpl.alreadyInstalled;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
@Deprecated
final class ServiceBatchInjectionBuilder implements BatchInjectionBuilder {

    private final BatchServiceBuilderImpl<?> batchServiceBuilder;
    private final BatchBuilderImpl batchBuilder;
    private final ServiceName source;

    ServiceBatchInjectionBuilder(final BatchServiceBuilderImpl<?> batchServiceBuilder, final BatchBuilderImpl batchBuilder, final ServiceName source) {
        this.batchServiceBuilder = batchServiceBuilder;
        this.batchBuilder = batchBuilder;
        this.source = source;
    }

    @SuppressWarnings({ "unchecked" })
    @Deprecated
    public ServiceBatchInjectionBuilder toInjector(final Injector<?> injector) {
        if (batchBuilder.isDone()) {
            throw alreadyInstalled();
        }
        batchServiceBuilder.addDependency(source, (Injector<Object>) injector);
        return this;
    }
}