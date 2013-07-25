/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013 Red Hat, Inc., and individual contributors
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

package org.jboss.msc.test.utils;

import org.jboss.msc.service.ManagementContext;
import org.jboss.msc.service.ServiceContainer;
import org.jboss.msc.txn.Executable;
import org.jboss.msc.txn.ExecuteContext;
import org.jboss.msc.txn.Transaction;
import org.jboss.msc.txn.TransactionController;

/**
 * A task that shuts down the container.
 *
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
final class ShutdownContainerTask implements Executable<Void> {
    
    private final ServiceContainer container;
    private final Transaction transaction;

    ShutdownContainerTask(final ServiceContainer container, final Transaction transaction) {
        this.container = container;
        this.transaction = transaction;
    }

    @Override
    public void execute(final ExecuteContext<Void> context) {
        final ManagementContext managementContext = TransactionController.getInstance().getManagementContext();
        managementContext.shutdownContainer(container, transaction);
        context.complete();
    }
    
}
