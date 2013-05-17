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

import java.util.HashSet;
import java.util.Set;

import org.jboss.msc.service.ServiceController.TransactionalState;
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
     * @param transaction        the active transaction
     * @param serviceController  starting service
     * @return                   the final task to be executed. Can be used for creating tasks that depend on the
     *                           conclusion of starting transition.
     */
    public static <T> TaskController<Void> createTasks(Transaction transaction, ServiceContext context, ServiceController<T> serviceController) {
        final Service<T> serviceValue = serviceController.getValue().get();

        // start service task builder
        final TaskBuilder<T> startBuilder = context.newTask(new SimpleServiceStartTask<T>(serviceValue, transaction)).setTraits(serviceValue);

        if (hasDependencies(serviceController)) {
            // notify dependent is starting to dependencies
            final TaskController<Void> notifyDependentStart = context.newTask(new NotifyDependentStartTask(transaction, serviceController)).release();
            startBuilder.addDependency(notifyDependentStart);

            // perform injections
            final TaskController<Void> performInjections = context.newTask(new PerformInjectionsTask(serviceController.getDependencies())).release();
            startBuilder.addDependency(performInjections);
        }

        // start service
        final TaskController<T> start = startBuilder.release();

        return context.newTask(new PostStartServiceTask<T>(serviceController, start, transaction)).addDependency(start).release();
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
                    dependencyController.dependentStarted(transaction);
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
            T result = serviceStartTask.getResult();
            // service failed
            if (result == null && transaction.getAttachment(FAILED_SERVICES).contains(service.getValue().get())) {
                service.setTransition(TransactionalState.FAILED, context);
                // notify dependency failed
            } else {
                performOutInjections();
                notifyDependencyUp(context);
                // TODO store the actual value of result somewhere
                service.setTransition(TransactionalState.UP, context);
            }
            context.complete();
        }

        private void performOutInjections() {
            // TODO
        }

        private void notifyDependencyUp(ServiceContext context) {
            notifyDependencyUp(service.getPrimaryRegistration(), context);
            for (Registration registration: service.getAliasRegistrations()) {
                notifyDependencyUp(registration, context);
            }
        }

        protected void notifyDependencyUp (Registration serviceRegistration, ServiceContext context) {
            for (Dependency<?> incomingDependency : serviceRegistration.getIncomingDependencies()) {
                incomingDependency.newDependencyState(transaction, context, true);
            }
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
