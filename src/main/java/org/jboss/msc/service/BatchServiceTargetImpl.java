/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

import org.jboss.msc.service.ServiceController.Mode;
import org.jboss.msc.value.ImmediateValue;
import org.jboss.msc.value.Value;

/**
 * {@link BatchServiceTarget} implementation.
 * 
 * @author <a href="mailto:flavia.rainone@jboss.com">Flavia Rainone</a>
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
class BatchServiceTargetImpl extends DelegatingServiceTarget implements BatchServiceTarget {

    private final ServiceTarget serviceTarget;
    private final ServiceRegistry serviceRegistry;
    private final Collection<ServiceController<?>> addedServiceControllers;

    BatchServiceTargetImpl(ServiceTarget serviceTarget, ServiceRegistry serviceRegistry) {
        this(new HashSet<ServiceController<?>>(), serviceTarget.subTarget(), serviceRegistry);
    }

    private BatchServiceTargetImpl(Collection<ServiceController<?>> controllers, ServiceTarget serviceTarget, ServiceRegistry serviceRegistry) {
        super(serviceTarget.subTarget());
        this.serviceTarget = serviceTarget;
        this.serviceRegistry = serviceRegistry;
        addedServiceControllers = controllers;
    }

    @Override
    public void removeServices() {
        final Collection<ServiceController<?>> controllers = addedServiceControllers;
        synchronized(controllers) {
            for (ServiceController<?> serviceController : controllers) {
                if (serviceController != null) {
                    serviceController.setMode(Mode.REMOVE);
                }
            }
            controllers.clear();
        }
    }

    @Override
    public <T> ServiceBuilder<T> addServiceValue(ServiceName name, Value<? extends Service<T>> value) {
        return new DelegatingServiceBuilder<T>(super.addServiceValue(name, value)) {
            public ServiceController<T> install() throws ServiceRegistryException {
                ServiceController<T> installed = super.install();
                final Collection<ServiceController<?>> controllers = addedServiceControllers;
                synchronized (controllers) {
                    controllers.add(installed);
                }
                return installed;
            }
        };
    }

    @Override
    public <T> ServiceBuilder<T> addService(ServiceName name, Service<T> service) {
        return addServiceValue(name, new ImmediateValue<Service<T>>(service));
    }

    @Override
    public BatchServiceTarget batchTarget() {
        return new BatchServiceTargetImpl(serviceTarget, serviceRegistry);
    }

    public BatchServiceTarget addListener(final ServiceListener<Object> listener) {
        super.addListener(listener);
        return this;
    }

    public BatchServiceTarget addListener(final ServiceListener<Object>... listeners) {
        super.addListener(listeners);
        return this;
    }

    public BatchServiceTarget addListener(final Collection<ServiceListener<Object>> listeners) {
        super.addListener(listeners);
        return this;
    }

    public BatchServiceTarget removeListener(final ServiceListener<Object> listener) {
        super.removeListener(listener);
        return this;
    }

    public BatchServiceTarget addDependency(final ServiceName dependency) {
        super.addDependency(dependency);
        return this;
    }

    public BatchServiceTarget addDependency(final ServiceName... dependencies) {
        super.addDependency(dependencies);
        return this;
    }

    public BatchServiceTarget addDependency(final Collection<ServiceName> dependencies) {
        super.addDependency(dependencies);
        return this;
    }

    public BatchServiceTarget removeDependency(final ServiceName dependency) {
        super.removeDependency(dependency);
        return this;
    }

    public ServiceTarget subTarget() {
        return new BatchServiceTargetImpl(new HashSet<ServiceController<?>>(), super.subTarget(), serviceRegistry);
    }
}
