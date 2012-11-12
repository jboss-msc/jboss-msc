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

import org.jboss.msc.txn.AttachmentKey;
import org.jboss.msc.txn.Transaction;
import org.jboss.msc.value.AtomicValue;
import org.jboss.msc.value.ReadableValue;
import org.jboss.msc.value.WritableValue;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class ServiceController<T> {
    private final Registration primaryRegistration;
    private final Registration[] aliasRegistrations;
    private final Dependency[] dependencies;
    private final AtomicValue<T> value = new AtomicValue<T>();
    private volatile ServiceMode mode = ServiceMode.NEVER;
    private volatile State state = null;

    private final AttachmentKey<TransactionControllerInfo> key = AttachmentKey.create();

    ServiceController(final Dependency[] dependencies, final Registration[] aliasRegistrations, final Registration primaryRegistration) {
        this.dependencies = dependencies;
        this.aliasRegistrations = aliasRegistrations;
        this.primaryRegistration = primaryRegistration;
    }

    Registration getPrimaryRegistration() {
        return primaryRegistration;
    }

    Registration[] getAliasRegistrations() {
        return aliasRegistrations;
    }

    Dependency[] getDependencies() {
        return dependencies;
    }

    public ReadableValue<T> getValue() {
        return value;
    }

    WritableValue<T> getWriteValue() {
        return value;
    }

    public void remove(Transaction transaction) {

    }

    public ServiceMode setMode(Transaction transaction, ServiceMode mode) {
        return null;
    }

    public State getState() {
        return state;
    }

    public State getState(Transaction transaction) {
        if (transaction == null) {
            throw new IllegalArgumentException("transaction is null");
        }
        // todo check for overridden state in transaction
        return state;
    }

    public TransactionalState getTransition(Transaction transaction) {
        final TransactionControllerInfo controllerInfo = transaction.getAttachment(key);
        return controllerInfo.getTransactionalState();
    }

    private class TransactionControllerInfo {
        private TransactionalState transactionalState;

        public TransactionalState getTransactionalState() {
            return transactionalState;
        }

        public State getState() {
            return transactionalState.getState();
        }

        public void remove() {
        }

        public ServiceMode getAndSetMode(ServiceMode mode) {
            return null;
        }
    }

    public enum State {
        UP,
        DOWN,
        FAILED,
        REMOVED,
        ;
    };

    public enum TransactionalState {
        UP(State.UP),
        DOWN(State.DOWN),
        FAILED(State.FAILED),
        REMOVED(State.REMOVED),
        STARTING(State.DOWN),
        STOPPING(State.UP),
        NEW(State.DOWN),
        REMOVING(State.DOWN),
        ;
        private final State state;

        public State getState() {
            return state;
        }

        private TransactionalState(final State state) {
            this.state = state;
        }
    }
}
