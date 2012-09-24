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

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
final class ServiceInstallTask<T> implements TxnTask<Controller<T>> {
    private final ServiceTxn txn;
    private final Registration primaryReg;
    private final Registration[] aliasReg;
    private final TxnTask<T> startSubtask;

    ServiceInstallTask(final ServiceTxn txn) {
        this.txn = txn;
    }

    public void execute(final TxnTaskContext<Controller<T>> context) {
        // add a new Controller to the transaction
        new Controller<T>(dependencies, aliasReg, primaryReg, stopSubtask, startSubtask);
    }

    public void rollback(final TxnTaskContext<Controller<T>> context) {
    }

    public void commit(final TxnTaskContext<Controller<T>> context) {
    }
}
