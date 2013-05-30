/*
 * JBoss, Home of Professional Open Source
 *
 * Copyright 2013 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.msc._private;

import org.jboss.msc.service.DependencyFlag;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceRegistry;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.txn.TaskController;
import org.jboss.msc.txn.Transaction;

/**
 * ServiceTarget implementation.
 * 
 * @author <a href="mailto:frainone@redhat.com">Flavia Rainone</a>
 *
 */
public class ServiceTargetImpl implements ServiceTarget {

    private final Transaction transaction;

    public ServiceTargetImpl(Transaction transaction) {
        this.transaction = transaction;
    }

    @Override
    public <T> ServiceBuilder<T> addService(ServiceRegistry registry, ServiceName name, Service<T> service) {
        if (registry == null) {
            throw new IllegalArgumentException("registry is null");
        }
        if (name == null) {
            throw new IllegalArgumentException("name is null");
        }
        return new ServiceBuilderImpl<T>(registry, transaction, name, service);
    }

    public void disableService(ServiceRegistry registry, ServiceName name) {
        ((ServiceRegistryImpl) registry).getRequiredServiceController(name).disable(transaction);
    }

    public void enableService(ServiceRegistry registry, ServiceName name) {
        ((ServiceRegistryImpl) registry).getRequiredServiceController(name).enable(transaction);
    }

    public void retryService(ServiceRegistry registry, ServiceName name) {
        ((ServiceRegistryImpl) registry).getRequiredServiceController(name).retry(transaction);
    }

    @Override
    public void removeService(ServiceRegistry registry, ServiceName name) {
        if (registry == null) {
            throw new IllegalArgumentException("registry is null");
        }
        if (name == null) {
            throw new IllegalArgumentException("name is null");
        }
        final Registration registration = ((ServiceRegistryImpl) registry).getRegistration(transaction, name);
        if (registration == null) {
            return;
        }
        final ServiceController<?> controller = registration.getController();
        if (controller == null) {
            return;
        }
        controller.remove(transaction);
    }

    @Override
    public ServiceTarget addDependency(ServiceName dependency) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ServiceTarget addDependency(ServiceName dependency, DependencyFlag... flags) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ServiceTarget addDependency(ServiceRegistry registry, ServiceName dependency) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ServiceTarget addDependency(ServiceRegistry registry, ServiceName dependency, DependencyFlag... flags) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ServiceTarget addDependency(TaskController<?> dependency) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ServiceTarget removeDependency(ServiceName dependency) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ServiceTarget removeDependency(ServiceRegistry registry, ServiceName dependency) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ServiceTarget removeDependency(TaskController<?> dependency) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public ServiceTarget subTarget() {
        // TODO Auto-generated method stub
        return null;
    }

}
