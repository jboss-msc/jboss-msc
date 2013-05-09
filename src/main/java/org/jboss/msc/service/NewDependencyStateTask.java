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
package org.jboss.msc.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.jboss.msc.txn.Executable;
import org.jboss.msc.txn.ExecuteContext;
import org.jboss.msc.txn.ServiceContext;
import org.jboss.msc.txn.TaskController;
import org.jboss.msc.txn.Transaction;

/**
 * Notifies dependents that dependency state has changed.
 * 
 * @author <a href="mailto:frainone@redhat.com">Flavia Rainone</a>
 */
class NewDependencyStateTask implements Executable<Void> {

    private final Transaction transaction;
    private final boolean dependencyUp;
    private final ServiceController<?> serviceController;

    public NewDependencyStateTask(Transaction transaction, ServiceController<?> serviceController, boolean dependencyUp) {
        this.transaction = transaction;
        this.serviceController = serviceController;
        this.dependencyUp = dependencyUp;
    }

    @Override
    public void execute(ExecuteContext<Void> context) {
        try {
            updateDependencyStatus(serviceController.getPrimaryRegistration(), context);
            for (Registration registration: serviceController.getAliasRegistrations()) {
                updateDependencyStatus(registration, context);
            }
        } finally {
            context.complete();
        }
    }

    protected void updateDependencyStatus (Registration serviceRegistration, ServiceContext context) {
        for (Dependency<?> incomingDependency : serviceRegistration.getIncomingDependencies()) {
            incomingDependency.newDependencyState(transaction, context, dependencyUp);
        }
    }

    public static Collection<TaskController<?>> run(Transaction transaction, ServiceContext context, ServiceController<?> controller, boolean dependencyUp) {
        final List<TaskController<?>> tasks = new ArrayList<TaskController<?>>();
        updateDependencyStatus(tasks, transaction, controller.getPrimaryRegistration(), context, dependencyUp);
        for (Registration registration: controller.getAliasRegistrations()) {
            updateDependencyStatus(tasks, transaction, registration, context, dependencyUp);
        }
        return tasks;
    }

    private static void updateDependencyStatus (List<TaskController<?>> tasks, Transaction transaction, Registration serviceRegistration, ServiceContext context, boolean dependencyUp) {
        for (Dependency<?> incomingDependency : serviceRegistration.getIncomingDependencies()) {
            TaskController<?> task = incomingDependency.newDependencyState(transaction, context, dependencyUp);
            tasks.add(task);
        }
    }
}
