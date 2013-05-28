/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
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

import org.jboss.msc.txn.Executable;
import org.jboss.msc.txn.ExecuteContext;
import org.jboss.msc.txn.Problem;
import org.jboss.msc.txn.Problem.Severity;
import org.jboss.msc.txn.TaskBuilder;
import org.jboss.msc.txn.Transaction;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 * @author <a href="mailto:frainone@redhat.com">Flavia Rainone</a>
 */
final class SimpleServiceStartTask<T> implements Executable<T> {

    private final Service<T> service;
    private final Transaction transaction;
    

    public SimpleServiceStartTask(final Service<T> service, final Transaction transaction) {
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
            public ServiceTarget newServiceTarget() throws IllegalStateException {
                return context.newServiceTarget();
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
