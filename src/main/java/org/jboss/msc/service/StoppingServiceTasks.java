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

import org.jboss.msc.service.ServiceController.TransactionalState;
import org.jboss.msc.txn.Executable;
import org.jboss.msc.txn.ExecuteContext;
import org.jboss.msc.txn.TaskBuilder;
import org.jboss.msc.txn.TaskController;
import org.jboss.msc.txn.Transaction;

/**
 * Tasks executed when a service is stopping.
 * 
 * @author <a href="mailto:frainone@redhat.com">Flavia Rainone</a>
 *
 */
final class StoppingServiceTasks {

    /**
     * Create stopping service tasks. When all created tasks finish execution, {@code service} will enter {@code DOWN} state.
     * 
     * @param transaction     the active transaction
     * @param service         stopping service
     * @param taskDependency  task on which all created tasks should depend. Can be {@code null}.
     * @return                the final task to be executed. Can be used for creating tasks that depend on the
     *                        conclusion of stopping transition.
     */
    public static <T> TaskController<Void> createTasks(Transaction transaction, ServiceController<T> service, TaskController<?> taskDependency) {
        final Service<T> serviceValue = service.getValue().getValue();

        // stop service
        final TaskBuilder<Void> stopTaskBuilder = transaction.newTask(new SimpleServiceStopTask(serviceValue));
        if (taskDependency != null) {
            stopTaskBuilder.addDependency(taskDependency);
        }
        final TaskController<Void> stop = stopTaskBuilder.release();

        // revert injections
        final TaskController<Void> revertInjections = transaction.newTask(new RevertInjectionsTask()).addDependency(stop).release();

        // set DOWN state
        final TaskBuilder<Void> setDownStateBuilder = transaction.newTask(new SetTransactionalStateTask(service, TransactionalState.DOWN)).addDependency(revertInjections);

        // notify dependencies that service is stopped 
        if (service.getDependencies().length != 0) {
            final TaskController<Void> notifyDependentStop = transaction.newTask(new NotifyDependentStopTask(transaction, service)).addDependency(stop).release();
            setDownStateBuilder.addDependency(notifyDependentStop);
        }
        return setDownStateBuilder.release();
    }

    /**
     * Create stopping service tasks. When all created tasks finish execution, {@code service} will enter {@code DOWN} state.
     * 
     * @param transaction     the active transaction
     * @param service         stopping service
     * @return                the final task to be executed. Can be used for creating tasks that depend on the
     *                        conclusion of stopping transition.
     */
    public static <T> TaskController<Void> createTasks(Transaction transaction, ServiceController<T> serviceController) {
        return createTasks(transaction, serviceController, null);
    }

    private static class NotifyDependentStopTask implements Executable<Void> {

        private final Transaction transaction;
        private final ServiceController<?> serviceController;

        public NotifyDependentStopTask(Transaction transaction, ServiceController<?> serviceController) {
            this.transaction = transaction;
            this.serviceController = serviceController;
        }

        @Override
        public void execute(ExecuteContext<Void> context) {
            for (Dependency<?> dependency: serviceController.getDependencies()) {
                ServiceController<?> dependencyController = dependency.getDependencyRegistration().getController();
                dependencyController.dependentStopped(transaction);
            }
        }

    }

    private static class RevertInjectionsTask implements Executable<Void> {

        @Override
        public void execute(ExecuteContext<Void> context) {
            // TODO
        }
    }
}
