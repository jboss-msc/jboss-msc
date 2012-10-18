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

import org.jboss.msc.txn.CommitContext;
import org.jboss.msc.txn.Committable;
import org.jboss.msc.txn.Executable;
import org.jboss.msc.txn.ExecuteContext;
import org.jboss.msc.txn.Revertible;
import org.jboss.msc.txn.RollbackContext;
import org.jboss.msc.txn.Validatable;
import org.jboss.msc.txn.ValidateContext;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
final class ServiceInstallTask<T> implements Executable<Controller<T>>, Revertible, Validatable, Committable {
    private final ServiceTxn txn;
    private final Registration primaryReg;
    private final Registration[] aliasReg;
    private final Service<T> service;

    ServiceInstallTask(final ServiceTxn txn, final Registration primaryReg, final Registration[] aliasReg, final Service<T> service) {
        this.txn = txn;
        this.primaryReg = primaryReg;
        this.aliasReg = aliasReg;
        this.service = service;
    }

    public void execute(final ExecuteContext<Controller<T>> context) {

    }

    public void validate(final ValidateContext validateContext) {
    }

    public void rollback(final RollbackContext context) {
    }

    public void commit(final CommitContext context) {
    }
}
