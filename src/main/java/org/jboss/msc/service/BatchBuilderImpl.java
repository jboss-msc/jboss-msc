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

import java.util.HashMap;
import java.util.Map;

/**
 * An ordered set of service batchEntries that should be processed as one.
 * 
 * @author Jason T. Greene
 * @author John E. Bailey
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 * @author <a href="mailto:flavia.rainone@jboss.com">Flavia Rainone</a>
 */
final class BatchBuilderImpl extends AbstractServiceTarget implements BatchBuilder {

    private final Map<ServiceName, ServiceBuilderImpl<?>> batchServices = new HashMap<ServiceName, ServiceBuilderImpl<?>>();
    private final AbstractServiceTarget parent;
    private boolean done;

    BatchBuilderImpl(final AbstractServiceTarget parent) {
        this.parent = parent;
    }

    @Override
    public void install() throws ServiceRegistryException {
        validateTargetState();
        // Reconcile batch level listeners/dependencies
        apply(batchServices.values());

        done = true;
        parent.install(this);
    }

    @Override
    void install(ServiceBuilderImpl<?> serviceBuilder) {
        validateTargetState();
        batchServices.put(serviceBuilder.getName(), serviceBuilder);
    }

    @Override
    void install(BatchBuilderImpl serviceBuilder) {
        validateTargetState();
        for (Map.Entry<ServiceName, ServiceBuilderImpl<?>> entry: serviceBuilder.batchServices.entrySet()) {
            batchServices.put(entry.getKey(), entry.getValue());
        }
    }

    @Override
    boolean hasService(ServiceName serviceName) {
        return batchServices.containsKey(serviceName);
    }

    @Override
    void validateTargetState() {
        if(done) {
            throw alreadyInstalled();
        }
    }

    Map<ServiceName, ServiceBuilderImpl<?>> getBatchServices() {
        return batchServices;
    }

    static IllegalStateException alreadyInstalled() {
        return new IllegalStateException("Batch already installed");
    }
}
