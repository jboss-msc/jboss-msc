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

import org.jboss.msc.service.ServiceName;
import org.jboss.msc.txn.ServiceContext;
import org.jboss.msc.txn.TaskController;
import org.jboss.msc.txn.Transaction;

/**
 * Parent dependency. The dependent is created whenever dependency is satisfied, and is removed whenever
 * dependency is no longer satisfied.
 * 
 * @author <a href="mailto:frainone@redhat.com">Flavia Rainone</a>
 *
 */
final class ParentDependency<T> extends DependencyDecorator<T> implements Dependent {
    // child service (non-null if dependency is satisfied only)
    private ServiceController<?> childService;
    // child service builder, used to created child service whenever needed
    private final ServiceBuilderImpl<?> childServiceBuilder;

    ParentDependency(SimpleDependency<T> dependency, ServiceBuilderImpl<?> childServiceBuilder, Transaction transaction) {
        super(dependency);
        this.childServiceBuilder = childServiceBuilder;
    }

    public void install(Transaction transaction) {
        // only at this moment the child service may be invoked
        // for that reason, only at this moment we invoke setDependent at dependency
        dependency.setDependent(this, transaction, transaction);
    }

    @Override
    public void setDependent(Dependent dependent, Transaction transaction, ServiceContext context) {
        // do nothing
    }

    @Override
    public void clearDependent(Transaction transaction, ServiceContext context) {
        // do nothing
    }

    @Override
    public TaskController<?> dependencySatisfied(Transaction transaction, ServiceContext context) {
        childService = childServiceBuilder.performInstallation(this, transaction, context);
        childService.dependencySatisfied(transaction, context);
        return null;
    }

    @Override
    public TaskController<?> dependencyUnsatisfied(Transaction transaction, ServiceContext context) {
        return childService.remove(transaction, context);
    }

    @Override
    public ServiceName getServiceName() {
        return childServiceBuilder.getServiceName();
    }
}
