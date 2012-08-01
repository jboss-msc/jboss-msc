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

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
final class SimpleServiceStartSubtask implements Subtask {
    private final SimpleService service;

    SimpleServiceStartSubtask(final SimpleService service) {
        this.service = service;
    }

    public void execute(final SubtaskContext context) {
        try {
            service.start(context.getTransaction(), new SimpleStartContext(context));
        } catch (Throwable t) {
            context.executeFailed(new SubtaskFailure(context.getController(), t));
            return;
        }
        context.executeComplete();
    }

    public void rollback(final SubtaskContext context) {
        try {
            service.stop(context.getTransaction());
        } finally {
            context.rollbackComplete();
        }
    }

    public void prepare(final SubtaskContext context) {
        if (service instanceof TransactionalSimpleService) {
            ((TransactionalSimpleService) service).prepareStart(context.getTransaction(), new SimpleStartContext(context));
        }
        context.prepareComplete();
    }

    public void abort(final SubtaskContext context) {
        try {
            service.stop(context.getTransaction());
        } finally {
            context.abortComplete();
        }
    }

    public void commit(final SubtaskContext context) {
        if (service instanceof TransactionalSimpleService) {
            ((TransactionalSimpleService) service).commitStart();
        }
        context.commitComplete();
    }
}
