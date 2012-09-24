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

import org.jboss.msc.txn.Committable;
import org.jboss.msc.txn.Executable;
import org.jboss.msc.txn.ExecutionContext;
import org.jboss.msc.txn.Revertible;
import org.jboss.msc.txn.Validatable;
import org.jboss.msc.txn.ValidationContext;
import org.jboss.msc.txn.WorkContext;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
final class ServiceInstallTask<T> implements Executable<Controller<T>>, Validatable, Committable, Revertible {
    private final ServiceTxn txn;
    private final Registration primaryReg;
    private final Registration[] aliasReg;
    private final Executable<T> startSubtask;

    ServiceInstallTask(final Executable<T> startSubtask, final Registration[] aliasReg, final Registration primaryReg, final ServiceTxn txn) {
        this.startSubtask = startSubtask;
        this.aliasReg = aliasReg;
        this.primaryReg = primaryReg;
        this.txn = txn;
    }

    public void commit(final WorkContext context) {
    }

    public void execute(final ExecutionContext<Controller<T>> context) {
    }

    public void rollback(final WorkContext context) {
    }

    public void validate(final ValidationContext validateContext) {
    }
}
