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
import org.jboss.msc.txn.Revertible;
import org.jboss.msc.txn.RollbackContext;
import org.jboss.msc.txn.Transaction;

/**
 * Service installation task.
 * 
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 * @author <a href="mailto:frainone@redhat.com">Flavia Rainone</a>
 */
final class ServiceInstallTask<T> implements Executable<ServiceController<T>>, Revertible {
    private final ServiceBuilderImpl<T> serviceBuilder;
    private final Transaction transaction;

    ServiceInstallTask(final Transaction transaction, final ServiceBuilderImpl<T> serviceBuilder) {
        this.transaction = transaction;
        this.serviceBuilder = serviceBuilder;
    }

    @Override
    public void execute(final ExecuteContext<ServiceController<T>> context) {
        final ServiceController<T> serviceController = serviceBuilder.performInstallation(transaction, context);
        CheckDependencyCycleTask.checkDependencyCycle(serviceController, transaction, context);
        context.complete(serviceController);
    }

    public void rollback(final RollbackContext context) {
        serviceBuilder.remove(transaction);
    }
}
