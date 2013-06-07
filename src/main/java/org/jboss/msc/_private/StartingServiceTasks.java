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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jboss.msc._private.ServiceController.TransactionalState;
import org.jboss.msc.service.Service;
import org.jboss.msc.txn.AttachmentKey;
import org.jboss.msc.txn.Executable;
import org.jboss.msc.txn.ExecuteContext;
import org.jboss.msc.txn.Factory;
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

    public static final AttachmentKey<Set<Service<?>>> FAILED_SERVICES = AttachmentKey.<Set<Service<?>>>create(new Factory<Set<Service<?>>>() {
        @Override
        public Set<Service<?>> create() {
            return new HashSet<Service<?>>();
        }
    });

    /**
     * Create starting service tasks. When all created tasks finish execution, {@code service} will enter {@code UP}
     * state.
     * 
     * @param serviceController  starting service
     * @param taskDependencies   the tasks that must be first concluded before service can start
     * @param transaction        the active transaction
     * @return                   the final task to be executed. Can be used for creating tasks that depend on the
     *                           conclusion of starting transition.
     */
    public static <T> TaskController<Void> createTasks(ServiceController<T> serviceController,
            Collection<TaskController<?>> taskDependencies, Transaction transaction, ServiceContext context) {

        final Service<T> serviceValue = serviceController.getValue().get();

        // start service task builder
        final TaskBuilder<T> startBuilder = context.newTask(new SimpleServiceStartTask<T>(serviceValue, transaction)).setTraits(serviceValue);

        if (hasDependencies(serviceController)) {
            // notify dependent is starting to dependencies
            final TaskController<Void> notifyDependentStart = context.newTask(new NotifyDependentStartTask(transaction, serviceController)).release();
            startBuilder.addDependency(notifyDependentStart);

            // perform injections
            final TaskController<Void> performInjections = context.newTask(new PerformInjectionsTask(serviceController.getDependencies())).
                    addDependencies(taskDependencies).release();
            startBuilder.addDependency(performInjections);
        } else {
            startBuilder.addDependencies(taskDependencies);
        }

        // start service
        final TaskController<T> start = startBuilder.release();

        return context.newTask(new PostStartServiceTask<T>(serviceController, start, transaction)).addDependency(start).release();
    }

    /**
     * Create starting service tasks. When all created tasks finish execution, {@code service} will enter {@code UP}
     * state.
     * 
     * @param serviceController  starting service
     * @param taskDependency     the task that must be first concluded before service can start
     * @param transaction        the active transaction
     * @return                   the final task to be executed. Can be used for creating tasks that depend on the
     *                           conclusion of starting transition.
     */
    public static <T> TaskController<Void> createTasks(ServiceController<T> serviceController,
            TaskController<?> taskDependency, Transaction transaction, ServiceContext context) {

        assert taskDependency != null;
        final List<TaskController<?>> taskDependencies = new ArrayList<TaskController<?>>(1);
        taskDependencies.add(taskDependency);
        return createTasks(serviceController, taskDependencies, transaction, context);
    }

    private static boolean hasDependencies(ServiceController<?> service) {
        return service.getDependencies().length > 0;
    }

    private static class NotifyDependentStartTask implements Executable<Void> {

        private final Transaction transaction;
        private final ServiceController<?> serviceController;

        public NotifyDependentStartTask(Transaction transaction, ServiceController<?> serviceController) {
            this.transaction = transaction;
            this.serviceController = serviceController;
        }

        @Override
        public void execute(ExecuteContext<Void> context) {
            for (Dependency<?> dependency: serviceController.getDependencies()) {
                ServiceController<?> dependencyController = dependency.getDependencyRegistration().getController();
                if (dependencyController != null) {
                    dependencyController.dependentStarted(transaction, context);
                }
            }
            context.complete();
        }
    }

    private static class PostStartServiceTask<T> implements Executable<Void> {

        private final ServiceController<T> service;
        private final TaskController<T> serviceStartTask;
        private final Transaction transaction;

        public PostStartServiceTask (ServiceController<T> service, TaskController<T> serviceStartTask, Transaction transaction) {
            this.service = service;
            this.serviceStartTask = serviceStartTask;
            this.transaction = transaction;
        }

        @Override
        public void execute(ExecuteContext<Void> context) {
            try {
                T result = serviceStartTask.getResult();
                // service failed
                if (result == null && transaction.getAttachment(FAILED_SERVICES).contains(service.getValue().get())) {
                    service.setTransition(TransactionalState.FAILED, transaction, context);
                } else {
                    performOutInjections();
                    // TODO store the actual value of result somewhere
                    service.setTransition(TransactionalState.UP, transaction, context);
                }
            } finally {
                context.complete();
            }
        }

        private void performOutInjections() {
            // TODO
        }

    }

    private static class PerformInjectionsTask implements Executable<Void> {

        private final Dependency<?>[] dependencies;

        public PerformInjectionsTask(Dependency<?>[] dependencies) {
            this.dependencies = dependencies;
        }

        @Override
        public void execute(ExecuteContext<Void> context) {
            for (Dependency<?> dependency: dependencies) {
                dependency.performInjections();
            }
            context.complete();
        }

    }
}
