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

import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceRegistry;
import org.jboss.msc.txn.Executable;
import org.jboss.msc.txn.ServiceContext;
import org.jboss.msc.txn.TaskBuilder;
import org.jboss.msc.txn.Transaction;

/**
 * ServiceContext implementation.
 * 
 * @author <a href="mailto:frainone@redhat.com">Flavia Rainone</a>
 *
 */
public class ServiceContextImpl implements ServiceContext {

    private static ServiceContextImpl instance = new ServiceContextImpl();

    ServiceContextImpl() {}

    public static ServiceContextImpl getInstance() {
        return instance;
    }

    @Override
    public <T> TaskBuilder<T> newTask(Executable<T> task, Transaction transaction) throws IllegalStateException {
        // TODO can a task be created from here?
        return null;
    }

    @Override
    public TaskBuilder<Void> newTask(Transaction transaction) throws IllegalStateException {
        // TODO can a task be created from here?
        return null;
    }

    @Override
    public <T> ServiceBuilder<T> addService(ServiceRegistry registry, ServiceName name, Transaction transaction) {
        if (registry == null) {
            throw new IllegalArgumentException("registry is null");
        }
        if (name == null) {
            throw new IllegalArgumentException("name is null");
        }
        return new ServiceBuilderImpl<T>(registry, name, transaction);
    }

    @Override
    public void removeService(ServiceRegistry registry, ServiceName name, Transaction transaction) {
        if (registry == null) {
            throw new IllegalArgumentException("registry is null");
        }
        if (name == null) {
            throw new IllegalArgumentException("name is null");
        }
        final Registration registration = ((ServiceRegistryImpl) registry).getRegistration(name);
        if (registration == null) {
            return;
        }
        final ServiceController<?> controller = registration.getController();
        if (controller == null) {
            return;
        }
        controller.remove(transaction, transaction); // FIXME we need an appropriate task factory to pass along as second parameter
    }

    @Override
    public void removeRegistry(ServiceRegistry registry, Transaction transaction) {
        ((ServiceRegistryImpl)registry).remove(transaction);
    }

}
