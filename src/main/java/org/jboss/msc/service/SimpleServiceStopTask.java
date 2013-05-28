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

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
final class SimpleServiceStopTask implements Executable<Void> {

    private final Service<?> service;

    public SimpleServiceStopTask(final Service<?> service) {
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

            public void shutdownContainer(ServiceContainer container) {
                context.shutdownContainer(container);
            }
        });
    }
}
