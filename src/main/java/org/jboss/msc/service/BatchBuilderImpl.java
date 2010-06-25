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

package org.jboss.msc.service;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.jboss.msc.value.ImmediateValue;
import org.jboss.msc.value.Value;

/**
 * An ordered set of service batchEntries that should be processed as one.
 * 
 * @author Jason T. Greene
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
final class BatchBuilderImpl extends AbstractBatchBuilder<BatchBuilder> implements BatchBuilder {

    private final Map<ServiceName, BatchServiceBuilderImpl<?>> batchServices = new HashMap<ServiceName, BatchServiceBuilderImpl<?>>();
    private final Set<SubBatchBuilderImpl> subBatchBuilders = new HashSet<SubBatchBuilderImpl>();
    private final ServiceContainerImpl container;
    private boolean done;

    BatchBuilderImpl(final ServiceContainerImpl container) {
        this.container = container;
    }

    public void install() throws ServiceRegistryException {
        if (isDone()) {
            throw alreadyInstalled();
        }
        // Reconcile batch level listeners/dependencies
        final Set<ServiceListener<Object>> batchListeners = this.getListeners();
        final Set<ServiceName> batchDependencies = this.getDependencies();
        for(BatchServiceBuilder<?> serviceBuilder : batchServices.values()) {
            serviceBuilder.addListener(batchListeners);
            serviceBuilder.addDependencies(batchDependencies);
        }

        // Reconcile sub-batch level listeners/dependencies
        final Set<SubBatchBuilderImpl> subBatchBuilders = this.subBatchBuilders;
        for(SubBatchBuilderImpl subBatchBuilder : subBatchBuilders) {
            subBatchBuilder.reconcile();
        }

        done = true;
        container.install(this);
    }

    public <T> BatchServiceBuilderImpl<T> addServiceValue(final ServiceName name, final Value<? extends Service<T>> value) throws DuplicateServiceException {
        if (done) {
            throw alreadyInstalled();
        }
        final Map<ServiceName, BatchServiceBuilderImpl<?>> batchServices = this.batchServices;
        final BatchServiceBuilderImpl<?> old = batchServices.get(name);
        if (old != null) {
            throw new DuplicateServiceException("Service named " + name + " is already defined in this batch");
        }
        final BatchServiceBuilderImpl<T> builder = new BatchServiceBuilderImpl<T>(this, value, name);
        batchServices.put(name, builder);
        return builder;
    }

    public <T> BatchServiceBuilderImpl<T> addService(final ServiceName name, final Service<T> service) throws DuplicateServiceException {
        if (done) {
            throw alreadyInstalled();
        }
        return addServiceValue(name, new ImmediateValue<Service<T>>(service));
    }

    Map<ServiceName, BatchServiceBuilderImpl<?>> getBatchServices() {
        return batchServices;
    }

    @Override
    BatchBuilder covariantReturn() {
        return this;
    }

    @Override
    public SubBatchBuilder subBatchBuilder() {
        final Set<SubBatchBuilderImpl> subBatches = this.subBatchBuilders;
        final SubBatchBuilderImpl subBatchBuilder = new SubBatchBuilderImpl(this);
        subBatches.add(subBatchBuilder);
        return subBatchBuilder;
    }

    @Override
    boolean isDone() {
        return done;
    }
}
