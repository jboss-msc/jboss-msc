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
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceRegistry;
import org.jboss.msc.txn.Executable;
import org.jboss.msc.txn.ExecuteContext;
import org.jboss.msc.txn.Transaction;
import org.jboss.msc.txn.TransactionController;

/**
 * A task that enables the service.
 *
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
final class EnableServiceTask implements Executable<Void> {
    
    private final ServiceRegistry registry;
    private final ServiceName serviceName;
    private final Transaction transaction;

    EnableServiceTask(final ServiceRegistry registry, final ServiceName serviceName, final Transaction transaction) {
        this.registry = registry;
        this.serviceName = serviceName;
        this.transaction = transaction;
    }

    @Override
    public void execute(final ExecuteContext<Void> context) {
        final ManagementContext managementContext = TransactionController.getInstance().getManagementContext();
        managementContext.enableService(registry, serviceName, transaction);
        context.complete();
    }
    
}
