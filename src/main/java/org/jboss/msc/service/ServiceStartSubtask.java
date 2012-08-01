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

import org.jboss.msc.txn.Subtask;
import org.jboss.msc.txn.SubtaskContext;
import org.jboss.msc.txn.SubtaskController;
import org.jboss.msc.txn.SubtaskFailure;
import org.jboss.msc.value.ReadableValue;
import org.jboss.msc.value.WritableValue;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
final class ServiceStartSubtask implements Subtask {
    private final Controller controller;

    ServiceStartSubtask(final Controller controller) {
        this.controller = controller;
    }

    public void execute(final SubtaskContext context) {
        for (Dependency dependency : controller.getDependencies()) {
            Registration registration = dependency.getDependencyRegistration();
            Controller depCtrl = registration.getController();
            ReadableValue<?> injectValue = depCtrl.getValue();
            Object value = injectValue.getValue();
            for (WritableValue<?> injection : dependency.getInjections()) {
                doInject(injection, value);
            }
        }
        Subtask subtask = controller.getStartSubtask();
        context.getTransaction().newSubtask(subtask).addDependencies(context.getController()).release(null);
    }

    @SuppressWarnings("unchecked")
    private static <T> void doInject(WritableValue<T> target, Object value) {
        target.inject((T) value);
    }

    public void rollback(final SubtaskContext context) {

    }

    public void prepare(final SubtaskContext context) {
    }

    public void abort(final SubtaskContext context) {
    }

    public void commit(final SubtaskContext context) {
    }
}
