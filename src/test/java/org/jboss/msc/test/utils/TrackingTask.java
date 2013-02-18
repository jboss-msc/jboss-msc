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

import java.util.concurrent.atomic.AtomicBoolean;

import org.jboss.msc.txn.CommitContext;
import org.jboss.msc.txn.Committable;
import org.jboss.msc.txn.Executable;
import org.jboss.msc.txn.ExecuteContext;
import org.jboss.msc.txn.Revertible;
import org.jboss.msc.txn.RollbackContext;
import org.jboss.msc.txn.Validatable;
import org.jboss.msc.txn.ValidateContext;

/**
 * A simple transaction task that tracks task calls.
 * It provides utility methods:
 * 
 * <UL>
 *   <LI>{@link #isCommitted()} - returns <code>true</code> if transaction have been committed, <code>false</code> otherwise</LI>
 *   <LI>{@link #isValidated()} - returns <code>true</code> if transaction have been validated, <code>false</code> otherwise</LI>
 *   <LI>{@link #isReverted()} - returns <code>true</code> if transaction have been rolled back, <code>false</code> otherwise</LI>
 *   <LI>{@link #isExecuted()} - returns <code>true</code> if transaction have been executed, <code>false</code> otherwise</LI>
 * </UL>
 *
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
public class TrackingTask implements Executable<Object>, Revertible, Validatable, Committable {

    private final AtomicBoolean executed = new AtomicBoolean();
    private final AtomicBoolean validated = new AtomicBoolean();
    private final AtomicBoolean reverted = new AtomicBoolean();
    private final AtomicBoolean committed = new AtomicBoolean();

    @Override
    public void commit(final CommitContext context) {
        committed.set(true);
        context.complete();
    }

    @Override
    public void validate(final ValidateContext context) {
        validated.set(true);
        context.complete();
    }

    @Override
    public void rollback(final RollbackContext context) {
        reverted.set(true);
        context.complete();
    }

    @Override
    public void execute(final ExecuteContext<Object> context) {
        executed.set(true);
        context.complete();
    }
    
    public final boolean isCommitted() {
        return committed.get();
    }
    
    public final boolean isValidated() {
        return validated.get();
    }
    
    public final boolean isReverted() {
        return reverted.get();
    }
    
    public final boolean isExecuted() {
        return executed.get();
    }

}
