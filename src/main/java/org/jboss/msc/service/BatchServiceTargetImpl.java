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

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.jboss.msc.service.ServiceController.Mode;
import org.jboss.msc.value.Value;

/**
 * {@link BatchServiceTarget} implementation.
 * 
 * @author <a href="mailto:flavia.rainone@jboss.com">Flavia Rainone</a>
 */
class BatchServiceTargetImpl implements BatchServiceTarget {

    private final ServiceTarget serviceTarget;
    private final ServiceRegistry serviceRegistry;
    private final Collection<ServiceName> addedServiceNames;

    BatchServiceTargetImpl(ServiceTarget serviceTarget, ServiceRegistry serviceRegistry) {
        this.serviceTarget = serviceTarget.subTarget();
        this.serviceRegistry = serviceRegistry;
        this.addedServiceNames = new HashSet<ServiceName>();
    }

    @Override
    public void removeServices() {
        synchronized(addedServiceNames) {
            for (ServiceName name: addedServiceNames) {
                ServiceController<?> serviceController = serviceRegistry.getService(name);
                if (serviceController != null) {
                    serviceController.setMode(Mode.REMOVE);
                }
            }
            addedServiceNames.clear();
        }
    }

    @Override
    public <T> ServiceBuilder<T> addServiceValue(ServiceName name, Value<? extends Service<T>> value) {
        ServiceBuilder<T> serviceBuilder = serviceTarget.addServiceValue(name, value);
        synchronized (addedServiceNames) {
            addedServiceNames.add(name);
        }
        return serviceBuilder;
    }

    @Override
    public <T> ServiceBuilder<T> addService(ServiceName name, Service<T> service) {
        ServiceBuilder<T> serviceBuilder = serviceTarget.addService(name, service);
        synchronized (addedServiceNames) {
            addedServiceNames.add(name);
        }
        return serviceBuilder;
    }

    @Override
    public BatchServiceTarget addListener(ServiceListener<Object> listener) {
        serviceTarget.addListener(listener);
        return this;
    }

    @Override
    public BatchServiceTarget addListener(ServiceListener<Object>... listeners) {
        serviceTarget.addListener(listeners);
        return this;
    }

    @Override
    public BatchServiceTarget addListener(Collection<ServiceListener<Object>> listeners) {
        serviceTarget.addListener(listeners);
        return this;
    }

    @Override
    public BatchServiceTarget removeListener(ServiceListener<Object> listener) {
        serviceTarget.removeListener(listener);
        return this;
    }

    @Override
    public Set<ServiceListener<Object>> getListeners() {
        return serviceTarget.getListeners();
    }

    @Override
    public BatchServiceTarget addDependency(ServiceName dependency) {
        serviceTarget.addDependency(dependency);
        return this;
    }

    @Override
    public BatchServiceTarget addDependency(ServiceName... dependencies) {
        serviceTarget.addDependency(dependencies);
        return this;
    }

    @Override
    public BatchServiceTarget addDependency(Collection<ServiceName> dependencies) {
        serviceTarget.addDependency(dependencies);
        return this;
    }

    @Override
    public BatchServiceTarget removeDependency(ServiceName dependency) {
        serviceTarget.removeDependency(dependency);
        return this;
    }

    @Override
    public Set<ServiceName> getDependencies() {
        return serviceTarget.getDependencies();
    }

    @Override
    public ServiceTarget subTarget() {
        return serviceTarget.subTarget();
    }

    @Override
    public BatchServiceTarget batchTarget() {
        return new BatchServiceTargetImpl(serviceTarget, serviceRegistry);
    }
}
