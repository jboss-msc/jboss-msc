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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.jboss.msc._private.ServiceController.TransactionalState;
import org.jboss.msc._private.ServiceModeBehavior.Demand;
import org.jboss.msc.service.Service;
import org.jboss.msc.txn.Executable;
import org.jboss.msc.txn.ExecuteContext;
import org.jboss.msc.txn.ServiceContext;
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
     * @param service          stopping service
     * @param taskDependencies the tasks that must be first concluded before service can stop
     * @param transaction      the active transaction
     * @param context          the service context
     * @return                 the final task to be executed. Can be used for creating tasks that depend on the
     *                         conclusion of stopping transition.
     */
    public static <T> TaskController<Void> createTasks(ServiceController<T> service, Collection<TaskController<?>> taskDependencies,
            Transaction transaction, ServiceContext context) {

        final Service<T> serviceValue = service.getService();

        // stop service
        final TaskBuilder<Void> stopTaskBuilder = context.newTask(new SimpleServiceStopTask(serviceValue));

        // undemand dependencies if needed
        if (service.getMode().shouldDemandDependencies() == Demand.SERVICE_UP && service.getDependencies().length > 0) {
            TaskController<Void> undemandDependenciesTask = UndemandDependenciesTask.create(service, taskDependencies, transaction, transaction);
            stopTaskBuilder.addDependency(undemandDependenciesTask);
        } else if (!taskDependencies.isEmpty()) {
            stopTaskBuilder.addDependencies(taskDependencies);
        }

        final TaskController<Void> stop = stopTaskBuilder.release();

        // revert injections
        final TaskController<Void> revertInjections = context.newTask(new RevertInjectionsTask()).addDependency(stop).release();

        // set DOWN state
        final TaskBuilder<Void> setDownStateBuilder = context.newTask(new SetTransactionalStateTask(service, TransactionalState.DOWN, transaction)).addDependency(revertInjections);

        // notify dependencies that service is stopped 
        if (service.getDependencies().length != 0) {
            final TaskController<Void> notifyDependentStop = context.newTask(new NotifyDependentStopTask(transaction, service)).addDependency(stop).release();
            setDownStateBuilder.addDependency(notifyDependentStop);
        }
        return setDownStateBuilder.release();
    }

    /**
     * Create stopping service tasks. When all created tasks finish execution, {@code service} will enter {@code DOWN} state.
     * 
     * @param service          stopping service
     * @param taskDependency   the task that must be first concluded before service can stop
     * @param transaction      the active transaction
     * @param context          the service context
     * @return                 the final task to be executed. Can be used for creating tasks that depend on the
     *                         conclusion of stopping transition.
     */
    public static <T> TaskController<Void> createTasks(ServiceController<T> service, TaskController<?> taskDependency,
            Transaction transaction, ServiceContext context) {

        final List<TaskController<?>> taskDependencies = new ArrayList<TaskController<?>>(1);
        taskDependencies.add(taskDependency);
        return createTasks(service, taskDependencies, transaction, context);
    }

    /**
     * Create stopping service tasks. When all created tasks finish execution, {@code service} will enter {@code DOWN} state.
     * @param transaction     the active transaction
     * @param service         stopping service
     * 
     * @return                the final task to be executed. Can be used for creating tasks that depend on the
     *                        conclusion of stopping transition.
     */
    @SuppressWarnings("unchecked")
    public static <T> TaskController<Void> createTasks(ServiceController<T> serviceController, Transaction transaction, ServiceContext context) {
        return createTasks(serviceController, Collections.EMPTY_LIST, transaction, context);
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
            try {
                for (Dependency<?> dependency: serviceController.getDependencies()) {
                    ServiceController<?> dependencyController = dependency.getDependencyRegistration().getController();
                    if (dependencyController != null) {
                        dependencyController.dependentStopped(transaction, context);
                    }
                }
            } finally {
                context.complete();
            }
        }

    }

    private static class RevertInjectionsTask implements Executable<Void> {

        @Override
        public void execute(ExecuteContext<Void> context) {
            // TODO
            context.complete();
        }
    }
}
