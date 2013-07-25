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

import static org.jboss.msc._private.ServiceController.STATE_DOWN;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.jboss.msc.service.Service;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.txn.Executable;
import org.jboss.msc.txn.ExecuteContext;
import org.jboss.msc.txn.Problem;
import org.jboss.msc.txn.Problem.Severity;
import org.jboss.msc.txn.TaskBuilder;
import org.jboss.msc.txn.TaskController;
import org.jboss.msc.txn.TaskFactory;
import org.jboss.msc.txn.Transaction;

/**
 * Tasks executed when a service is stopping.
 * 
 * @author <a href="mailto:frainone@redhat.com">Flavia Rainone</a>
 *
 */
final class StoppingServiceTasks {

    /**
     * Creates stopping service tasks. When all created tasks finish execution, {@code service} will enter {@code DOWN} state.
     * 
     * @param service          stopping service
     * @param taskDependencies the tasks that must be first concluded before service can stop
     * @param transaction      the active transaction
     * @param taskFactory      the task factory
     * @return                 the final task to be executed. Can be used for creating tasks that depend on the
     *                         conclusion of stopping transition.
     */
    static <T> TaskController<Void> create(ServiceController<T> service, Collection<TaskController<?>> taskDependencies,
            Transaction transaction, TaskFactory taskFactory) {

        final Service<T> serviceValue = service.getService();

        // stop service
        final TaskBuilder<Void> stopTaskBuilder = taskFactory.newTask(new StopServiceTask(serviceValue));

        // undemand dependencies if needed
        if (service.getDependencies().length > 0) {
            TaskController<Void> undemandDependenciesTask = UndemandDependenciesTask.create(service, taskDependencies, transaction, transaction);
            stopTaskBuilder.addDependency(undemandDependenciesTask);
        } else if (!taskDependencies.isEmpty()) {
            stopTaskBuilder.addDependencies(taskDependencies);
        }

        final TaskController<Void> stop = stopTaskBuilder.release();

        // post stop task
        return taskFactory.newTask(new SetServiceDownTask(service, transaction)).addDependency(stop).release();
    }

    /**
     * Creates stopping service tasks. When all created tasks finish execution, {@code service} will enter {@code DOWN} state.
     * 
     * @param service          stopping service
     * @param taskDependency   the task that must be first concluded before service can stop
     * @param transaction      the active transaction
     * @param taskFactory      the task factory
     * @return                 the final task to be executed. Can be used for creating tasks that depend on the
     *                         conclusion of stopping transition.
     */
    static <T> TaskController<Void> create(ServiceController<T> service, TaskController<?> taskDependency,
            Transaction transaction, TaskFactory taskFactory) {

        final List<TaskController<?>> taskDependencies = new ArrayList<TaskController<?>>(1);
        taskDependencies.add(taskDependency);
        return create(service, taskDependencies, transaction, taskFactory);
    }

    /**
     * Creates stopping service tasks. When all created tasks finish execution, {@code service} will enter {@code DOWN} state.
     * @param service         stopping service
     * @param transaction     the active transaction
     * @param taskFactory     the task factory
     * @return                the final task to be executed. Can be used for creating tasks that depend on the
     *                        conclusion of stopping transition.
     */
    @SuppressWarnings("unchecked")
    static <T> TaskController<Void> create(ServiceController<T> service, Transaction transaction, TaskFactory taskFactory) {
        return create(service, Collections.EMPTY_LIST, transaction, taskFactory);
    }

    /**
     * Creates stopping service tasks. When all created tasks finish execution, {@code service} will enter {@code DOWN} state.
     * @param service         failed service that is stopping
     * @param transaction     the active transaction
     * @param taskFactory      the task factory
     * @return                the final task to be executed. Can be used for creating tasks that depend on the
     *                        conclusion of stopping transition.
     */
    // TODO discuss: what if we just set the service down after it fails?
    static <T> TaskController<Void> createForFailedService(ServiceController<T> service, Transaction transaction, TaskFactory taskFactory) {

        // post stop task
        final TaskBuilder<Void> setServiceDownBuilder = taskFactory.newTask(new SetServiceDownTask(service, transaction));

        // undemand dependencies if needed
        if (service.getDependencies().length > 0) {
            TaskController<Void> undemandDependenciesTask = UndemandDependenciesTask.create(service, transaction, transaction);
            setServiceDownBuilder.addDependency(undemandDependenciesTask);
        }

        return setServiceDownBuilder.release();
    }

    /**
     * Task that stops service.
     * 
     * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
     */
    static class StopServiceTask implements Executable<Void> {

        private final Service<?> service;

        StopServiceTask(final Service<?> service) {
            this.service = service;
        }

        public void execute(final ExecuteContext<Void> context) {
            service.stop(new StopContext(){

                @Override
                public void complete(Void result) {
                    context.complete(result);
                }

                @Override
                public void complete() {
                    context.complete();
                }

                @Override
                public void addProblem(Problem reason) {
                    context.addProblem(reason);
                }

                @Override
                public void addProblem(Severity severity, String message) {
                    context.addProblem(severity, message);
                }

                @Override
                public void addProblem(Severity severity, String message, Throwable cause) {
                    context.addProblem(severity, message, cause);
                }

                @Override
                public void addProblem(String message, Throwable cause) {
                    context.addProblem(message, cause);
                }

                @Override
                public void addProblem(String message) {
                    context.addProblem(message);
                }

                @Override
                public void addProblem(Throwable cause) {
                    context.addProblem(cause);
                }

                @Override
                public boolean isCancelRequested() {
                    return context.isCancelRequested();
                }

                @Override
                public void cancelled() {
                    context.cancelled();
                }

                @Override
                public <T> TaskBuilder<T> newTask(Executable<T> task) throws IllegalStateException {
                    return context.newTask(task);
                }

                @Override
                public TaskBuilder<Void> newTask() throws IllegalStateException {
                    return context.newTask();
                }
            });
        }
    }

    /**
     * Sets service at DOWN state, uninjects service value and notifies dependencies, if any.
     *
     * @author <a href="mailto:frainone@redhat.com">Flavia Rainone</a>
     *
     */
    private static class SetServiceDownTask implements Executable<Void> {

        private final Transaction transaction;
        private final ServiceController<?> serviceController;

        private SetServiceDownTask(ServiceController<?> serviceController, Transaction transaction) {
            this.transaction = transaction;
            this.serviceController = serviceController;
        }

        @Override
        public void execute(ExecuteContext<Void> context) {
            try {
                // set down state
                serviceController.setTransition(STATE_DOWN, transaction, context);

                // clear service value, thus performing an automatic uninjection
                serviceController.setValue(null);

                // notify dependent is stopped
                for (DependencyImpl<?> dependency: serviceController.getDependencies()) {
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
}
