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

import org.jboss.msc.txn.ReportableContext;
import org.jboss.msc.txn.TaskController;
import org.jboss.msc.txn.TaskFactory;
import org.jboss.msc.txn.Transaction;

/**
 * Dependency decorator. Builds extra functionality on top of {@link SimpleDependency}.
 * 
 * @author <a href="mailto:frainone@redhat.com">Flavia Rainone</a>
 *
 */
class DependencyDecorator<T> extends DependencyImpl<T> {
    protected final DependencyImpl<T> dependency;

    DependencyDecorator(DependencyImpl<T> dependency) {
        super(null, null);
        this.dependency = dependency;
    }

    @Override
    public void setDependent(Dependent dependent, Transaction transaction, TaskFactory taskFactory) {
        dependency.setDependent(dependent, transaction, taskFactory);
    }

    @Override
    public void clearDependent(Transaction transaction, TaskFactory taskFactory) {
        dependency.clearDependent(transaction, taskFactory);
    }

    @Override
    public final Registration getDependencyRegistration() {
        return dependency.getDependencyRegistration();
    }

    @Override
    public void demand(Transaction transaction, TaskFactory taskFactory) {
        dependency.demand(transaction, taskFactory);
    }

    @Override
    public void undemand(Transaction transaction, TaskFactory taskFactory) {
        dependency.undemand(transaction, taskFactory);
    }

    @Override
    public TaskController<?> dependencyUp(Transaction transaction, TaskFactory taskFactory) {
        return dependency.dependencyUp(transaction, taskFactory);
    }

    @Override
    public TaskController<?> dependencyDown(Transaction transaction, TaskFactory taskFactory) {
        return dependency.dependencyDown(transaction, taskFactory);
    }

    @Override
    void validate(ServiceController<?> controllerDependency, ReportableContext context) {
        dependency.validate(controllerDependency, context);
    }

    @Override
    public T get() {
        return dependency.get();
    }
}