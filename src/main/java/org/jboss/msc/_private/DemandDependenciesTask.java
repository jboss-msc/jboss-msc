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

import org.jboss.msc.txn.Executable;
import org.jboss.msc.txn.ExecuteContext;
import org.jboss.msc.txn.ServiceContext;
import org.jboss.msc.txn.TaskBuilder;
import org.jboss.msc.txn.TaskController;
import org.jboss.msc.txn.Transaction;

/**
 * Task for demanding dependencies.
 * 
 * @author <a href="mailto:frainone@redhat.com">Flavia Rainone</a>
 */
class DemandDependenciesTask implements Executable<Void> {

    private Transaction transaction;
    private ServiceController<?> service;

    /**
     * Creates and releases the demand dependencies task.
     * 
     * @param service      the service whose dependencies will be demanded by the task
     * @param transaction  the active transaction
     * @param context      the service context
     * @return the task controller
     */
    static TaskController<Void> create(ServiceController<?> service, Transaction transaction, ServiceContext context) {
        return create(service, null, transaction, context);
    }

    /**
     * Creates and releases the demand dependencies task.
     * <p>
     * Invoke this method when the demand dependencies task must be executed only after another task finishes execution.
     * 
     * @param service        the service whose dependencies will be demanded by the task
     * @param taskDependency the dependency of the demand dependencies task
     * @param transaction    the active transaction
     * @param context        the service context
     * @return the task controller
     */
    static TaskController<Void> create(ServiceController<?> service, TaskController<?> taskDependency, Transaction transaction, ServiceContext context) {
        if (service.getDependencies().length == 0) {
            return null;
        }
        TaskBuilder<Void> taskBuilder = context.newTask(new DemandDependenciesTask(transaction, service));
        if(taskDependency != null) {
            taskBuilder.addDependency(taskDependency);
        }
        return taskBuilder.release();
    }

    private DemandDependenciesTask(Transaction transaction, ServiceController<?> service) {
        this.transaction = transaction;
        this.service = service;
    }

    @Override
    public void execute(ExecuteContext<Void> context) {
        try {
            for (AbstractDependency<?> dependency: service.getDependencies()) {
                dependency.demand(transaction, context);
            }
        } finally {
            context.complete();
        }
    }
}
