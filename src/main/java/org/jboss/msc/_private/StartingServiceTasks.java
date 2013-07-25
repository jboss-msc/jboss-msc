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

import static org.jboss.msc._private.ServiceController.STATE_FAILED;
import static org.jboss.msc._private.ServiceController.STATE_UP;

import java.util.HashSet;
import java.util.Set;

import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.txn.AttachmentKey;
import org.jboss.msc.txn.Executable;
import org.jboss.msc.txn.ExecuteContext;
import org.jboss.msc.txn.Factory;
import org.jboss.msc.txn.Problem;
import org.jboss.msc.txn.Problem.Severity;
import org.jboss.msc.txn.TaskBuilder;
import org.jboss.msc.txn.TaskController;
import org.jboss.msc.txn.TaskFactory;
import org.jboss.msc.txn.Transaction;

/**
 * Tasks executed when a service is starting.
 * 
 * @author <a href="mailto:frainone@redhat.com">Flavia Rainone</a>
 *
 */
final class StartingServiceTasks {

    // keep track of services that have failed to start at current transaction
    static final AttachmentKey<Set<Service<?>>> FAILED_SERVICES = AttachmentKey.<Set<Service<?>>>create(new Factory<Set<Service<?>>>() {
        @Override
        public Set<Service<?>> create() {
            return new HashSet<Service<?>>();
        }
    });

    /**
     * Creates starting service tasks. When all created tasks finish execution, {@code service} will enter {@code UP}
     * state.
     * 
     * @param serviceController  starting service
     * @param taskDependency     the task that must be first concluded before service can start
     * @param transaction        the active transaction
     * @param taskFactory        the task factory
     * @return                   the final task to be executed. Can be used for creating tasks that depend on the
     *                           conclusion of starting transition.
     */
    static <T> TaskController<Void> create(ServiceController<T> serviceController,
            TaskController<?> taskDependency, Transaction transaction, TaskFactory taskFactory) {

        final Service<T> serviceValue = serviceController.getService();

        // start service task builder
        final TaskBuilder<T> startBuilder = taskFactory.newTask(new StartServiceTask<T>(serviceValue, transaction)).setTraits(serviceValue);

        if (hasDependencies(serviceController)) {
            // notify dependent is starting to dependencies
            final TaskBuilder<Void> notifyDependentStartBuilder = taskFactory.newTask(new NotifyDependentStartTask(transaction, serviceController));
            if (taskDependency != null) {
                notifyDependentStartBuilder.addDependency(taskDependency);
            }
            final TaskController<Void> notifyDependentStart = notifyDependentStartBuilder.release();
            startBuilder.addDependency(notifyDependentStart);
        } else if (taskDependency != null) {
            startBuilder.addDependency(taskDependency);
        }

        // start service
        final TaskController<T> start = startBuilder.release();

        return taskFactory.newTask(new SetServiceUpTask<T>(serviceController, start, transaction)).addDependency(start).release();
    }

    /**
     * Creates starting service tasks. When all created tasks finish execution, {@code service} will enter {@code UP}
     * state.
     * 
     * @param serviceController  starting service
     * @param transaction        the active transaction
     * @param taskFactory        the task factory
     * @return                   the final task to be executed. Can be used for creating tasks that depend on the
     *                           conclusion of starting transition.
     */
    static <T> TaskController<Void> create(ServiceController<T> serviceController, Transaction transaction, TaskFactory taskFactory) {
        return create(serviceController, null, transaction, taskFactory);
    }

    private static boolean hasDependencies(ServiceController<?> service) {
        return service.getDependencies().length > 0;
    }

    /**
     * Task that notifies dependencies that a dependent service is about to start
     */
    private static class NotifyDependentStartTask implements Executable<Void> {

        private final Transaction transaction;
        private final ServiceController<?> serviceController;

        public NotifyDependentStartTask(Transaction transaction, ServiceController<?> serviceController) {
            this.transaction = transaction;
            this.serviceController = serviceController;
        }

        @Override
        public void execute(ExecuteContext<Void> context) {
            for (DependencyImpl<?> dependency: serviceController.getDependencies()) {
                ServiceController<?> dependencyController = dependency.getDependencyRegistration().getController();
                if (dependencyController != null) {
                    dependencyController.dependentStarted(transaction, context);
                }
            }
            context.complete();
        }
    }

    /**
     * Task that starts service.
     * 
     * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
     * @author <a href="mailto:frainone@redhat.com">Flavia Rainone</a>
     */
    static class StartServiceTask<T> implements Executable<T> {

        private final Service<T> service;
        private final Transaction transaction;

        StartServiceTask(final Service<T> service, final Transaction transaction) {
            this.service = service;
            this.transaction = transaction;
        }

        /**
         * Perform the task.
         *
         * @param context
         */
        public void execute(final ExecuteContext<T> context) {
            service.start(new StartContext<T>() {
                @Override
                public void complete(T result) {
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
                public <K> TaskBuilder<K> newTask(Executable<K> task) throws IllegalStateException {
                    return context.newTask(task);
                }

                @Override
                public TaskBuilder<Void> newTask() throws IllegalStateException {
                    return context.newTask();
                }

                @Override
                public void fail() {
                    transaction.getAttachment(StartingServiceTasks.FAILED_SERVICES).add(service);
                    complete();
                }

            });
        }
    }


    /**
     * Task that sets service at UP state, and performs service value injection.
     */
    private static class SetServiceUpTask<T> implements Executable<Void> {

        private final ServiceController<T> service;
        private final TaskController<T> serviceStartTask;
        private final Transaction transaction;

        private SetServiceUpTask (ServiceController<T> service, TaskController<T> serviceStartTask, Transaction transaction) {
            this.service = service;
            this.serviceStartTask = serviceStartTask;
            this.transaction = transaction;
        }

        @Override
        public void execute(ExecuteContext<Void> context) {
            try {
                T result = serviceStartTask.getResult();
                // service failed
                if (result == null && transaction.getAttachment(FAILED_SERVICES).contains(service.getService())) {
                    MSCLogger.FAIL.startFailed(service.getServiceName());
                    service.setTransition(STATE_FAILED, transaction, context);
                } else {
                    service.setValue(result);
                    service.setTransition(STATE_UP, transaction, context);
                }
            } finally {
                context.complete();
            }
        }
    }
}
