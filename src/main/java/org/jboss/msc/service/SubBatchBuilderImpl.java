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

import org.jboss.msc.value.Value;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * A sub-batch set of service batchEntries that should be processed as one.  Mainly used to apply batch level
 * listeners/dependencies to a subset of a batch.
 *
 * @author John Bailey
 */
final class SubBatchBuilderImpl extends AbstractBatchBuilder<SubBatchBuilder> implements SubBatchBuilder {

    private final BatchBuilderImpl parentBatch;
    // This duplicate collection sucks.  But we need some way to know what is in the sub batch.
    private final Set<BatchServiceBuilder<?>> subBatchServiceBuilders = new HashSet<BatchServiceBuilder<?>>();
    
    SubBatchBuilderImpl(final BatchBuilderImpl parentBatch) {
        this.parentBatch = parentBatch;
    }

    @Override
    public <T> BatchServiceBuilder<T> addServiceValue(final ServiceName name, final Value<? extends Service<T>> value) throws DuplicateServiceException {
        if (isDone()) {
            throw alreadyInstalled();
        }
        final BatchServiceBuilder<T> batchServiceBuilder = parentBatch.addServiceValue(name, value);
        subBatchServiceBuilders.add(batchServiceBuilder);
        return batchServiceBuilder;  
    }

    @Override
    public <T> BatchServiceBuilder<T> addService(final ServiceName name, final Service<T> service) throws DuplicateServiceException {
        if (isDone()) {
            throw alreadyInstalled();
        }
        final BatchServiceBuilder<T> batchServiceBuilder = parentBatch.addService(name, service);
        subBatchServiceBuilders.add(batchServiceBuilder);
        return batchServiceBuilder;
    }

    @Override
    public <T> BatchServiceBuilder<T> addServiceValueIfNotExist(ServiceName name, Value<? extends Service<T>> value) throws DuplicateServiceException {
        if (isDone()) {
            throw alreadyInstalled();
        }
        final BatchServiceBuilder<T> batchServiceBuilder = parentBatch.addServiceValueIfNotExist(name, value);
        subBatchServiceBuilders.add(batchServiceBuilder);
        return batchServiceBuilder;  
    }

    void reconcile() {
        final Set<ServiceListener<Object>> listeners = this.getListeners();
        final Set<ServiceName> dependencies = this.getDependencies();
        final Set<BatchServiceBuilder<?>> subBatchServiceBuilders = this.subBatchServiceBuilders;
        for(BatchServiceBuilder<?> batchServiceBuilder : subBatchServiceBuilders) {
            batchServiceBuilder.addListener(listeners);
            batchServiceBuilder.addDependencies(dependencies);
        }
    }

    @Override
    boolean isDone() {
        return parentBatch.isDone();
    }

    @Override
    SubBatchBuilder covariantReturn() {
        return this;
    }
}
