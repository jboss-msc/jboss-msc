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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceContainer;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceRegistry;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.txn.AttachmentKey;
import org.jboss.msc.txn.Executable;
import org.jboss.msc.txn.ExecuteContext;
import org.jboss.msc.txn.Factory;
import org.jboss.msc.txn.Problem;
import org.jboss.msc.txn.Problem.Severity;
import org.jboss.msc.txn.ServiceContext;
import org.jboss.msc.txn.TaskBuilder;
import org.jboss.msc.txn.TaskController;
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
     * @param taskDependencies   the tasks that must be first concluded before service can start
     * @param transaction        the active transaction
     * @param context            the service context
     * @return                   the final task to be executed. Can be used for creating tasks that depend on the
     *                           conclusion of starting transition.
     */
    static <T> TaskController<Void> create(ServiceController<T> serviceController,
            Collection<TaskController<?>> taskDependencies, Transaction transaction, ServiceContext context) {

        final Service<T> serviceValue = serviceController.getService();

        // start service task builder
        final TaskBuilder<T> startBuilder = context.newTask(new StartServiceTask<T>(serviceValue, transaction)).setTraits(serviceValue);

        if (hasDependencies(serviceController)) {
            // notify dependent is starting to dependencies
            final TaskController<Void> notifyDependentStart = context.newTask(new NotifyDependentStartTask(transaction, serviceController)).release();
            startBuilder.addDependency(notifyDependentStart);
        } else {
            startBuilder.addDependencies(taskDependencies);
        }

        // start service
        final TaskController<T> start = startBuilder.release();

        return context.newTask(new SetServiceUpTask<T>(serviceController, start, transaction)).addDependency(start).release();
    }

    /**
     * Creates starting service tasks. When all created tasks finish execution, {@code service} will enter {@code UP}
     * state.
     * 
     * @param serviceController  starting service
     * @param taskDependency     the task that must be first concluded before service can start
     * @param transaction        the active transaction
     * @param context            the service context
     * @return                   the final task to be executed. Can be used for creating tasks that depend on the
     *                           conclusion of starting transition.
     */
    static <T> TaskController<Void> create(ServiceController<T> serviceController,
            TaskController<?> taskDependency, Transaction transaction, ServiceContext context) {

        assert taskDependency != null;
        final List<TaskController<?>> taskDependencies = new ArrayList<TaskController<?>>(1);
        taskDependencies.add(taskDependency);
        return create(serviceController, taskDependencies, transaction, context);
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
            for (AbstractDependency<?> dependency: serviceController.getDependencies()) {
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
                
                public <T> ServiceBuilder<T> addService(ServiceRegistry registry, ServiceName name) {
                    return context.addService(registry, name);
                }

                @Override
                public void enableService(ServiceRegistry registry, ServiceName name) throws IllegalStateException {
                    context.enableService(registry, name);
                }

                @Override
                public void disableService(ServiceRegistry registry, ServiceName name) throws IllegalStateException {
                    context.disableService(registry, name);
                }

                @Override
                public void removeService(ServiceRegistry registry, ServiceName name) throws IllegalStateException {
                    context.removeService(registry, name);
                }

                @Override
                public void enableRegistry(ServiceRegistry registry) {
                    context.enableRegistry(registry);
                }

                @Override
                public void disableRegistry(ServiceRegistry registry) {
                    context.disableRegistry(registry);
                }

                @Override
                public void removeRegistry(ServiceRegistry registry) {
                    context.removeRegistry(registry);
                }

                @Override
                public void shutdownContainer(ServiceContainer container) {
                    context.shutdownContainer(container);
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
