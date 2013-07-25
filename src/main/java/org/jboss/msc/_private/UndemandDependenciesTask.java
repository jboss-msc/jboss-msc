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

import java.util.Collection;
import java.util.Collections;

import org.jboss.msc.txn.Executable;
import org.jboss.msc.txn.ExecuteContext;
import org.jboss.msc.txn.TaskBuilder;
import org.jboss.msc.txn.TaskController;
import org.jboss.msc.txn.TaskFactory;
import org.jboss.msc.txn.Transaction;

/**
 * Task for undemanding dependencies.
 * 
 * @author <a href="mailto:frainone@redhat.com">Flavia Rainone</a>
 */
class UndemandDependenciesTask implements Executable<Void> {

    private Transaction transaction;
    private ServiceController<?> service;

    /**
     * Creates and releases the undemand dependencies task.
     * 
     * @param service      the service whose dependencies will be undemanded by the task
     * @param transaction  the active transaction
     * @param taskFactory  the task factory
     * @return the task controller
     */
    @SuppressWarnings("unchecked")
    static TaskController<Void> create(ServiceController<?> service, Transaction transaction, TaskFactory taskFactory) {
        return create(service, Collections.EMPTY_LIST, transaction, taskFactory);
    }

    /**
     * Creates and releases the undemand dependencies task.
     * <p>
     * Invoke this method when the undemand dependencies task must be executed only after other tasks finish execution.
     * 
     * @param service          the service whose dependencies will be undemanded by the task
     * @param taskDependencies the dependencies of the undemand dependencies task
     * @param transaction      the active transaction
     * @param taskFactory      the task factory
     * @return the task controller
     */
    static TaskController<Void> create(ServiceController<?> service, Collection<TaskController<?>> taskDependencies, Transaction transaction, TaskFactory taskFactory) {
        if (service.getDependencies().length == 0) {
            return null;
        }
        TaskBuilder<Void> taskBuilder = taskFactory.newTask(new UndemandDependenciesTask(transaction, service));
        if (!taskDependencies.isEmpty()) {
            taskBuilder.addDependencies(taskDependencies);
        }
        return taskBuilder.release();
    }

    private UndemandDependenciesTask(Transaction transaction, ServiceController<?> service) {
        this.transaction = transaction;
        this.service = service;
    }

    @Override
    public void execute(ExecuteContext<Void> context) {
        try {
            for (DependencyImpl<?> dependency: service.getDependencies()) {
                dependency.undemand(transaction, context);
            }
        } finally {
            context.complete();
        }
    }
}
