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

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.jboss.msc.txn.TaskController;
import org.jboss.msc.txn.TaskListener;
import org.jboss.msc.txn.Transaction;

/**
 * The service transaction context.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
final class ServiceTxn {
    private final ServiceContainer container;
    private final Transaction transaction;

    private final ConcurrentMap<ServiceName, Pending> map = new ConcurrentHashMap<ServiceName, Pending>();

    public ServiceTxn(final ServiceContainer container, final Transaction transaction) {
        this.container = container;
        this.transaction = transaction;
    }

    public ServiceContainer getContainer() {
        return container;
    }

    public void remove(final ServiceName serviceName) {
        Pending existing = map.get(serviceName);
        Pending created;
        do {
            if (existing == null) {
                Registration registration = container.getRegistration(serviceName);
                if (registration == null) {
                    // nothing to do
                    return;
                }
                Controller<?> controller = registration.getController();
                if (controller == null) {
                    // nothing to do
                    return;
                }
                created = new Pending(State.PENDING_REMOVE, transaction.newSubtask(new ServiceRemoveTask(serviceName)));
            } else {
                State state = existing.getState();
                switch (state) {
                    case PENDING_REMOVE: return; // nothing to do
                    case PENDING_ADD_CANCEL: return; // nothing to do
                    case PENDING_REMOVE_ADD: {
                        created = new Pending(State.PENDING_REMOVE, existing.getSubtask());
                        break;
                    }
                    case PENDING_REMOVE_CANCEL: {
                        created = new Pending(State.PENDING_REMOVE_CANCEL_REMOVE, existing.getSubtask());
                        break;
                    }
                    default: {
                        throw new IllegalStateException();
                    }
                }
            }
            existing = map.putIfAbsent(serviceName, created);
        } while (existing != null);
        if (created.getState() == State.PENDING_REMOVE_CANCEL) {
            created.getSubtask().rollback(new TaskListener<Object>() {
                public void handleEvent(final TaskController<?> controller) {
                    Pending pending;
                    for (;;) {
                        pending = map.get(serviceName);
                        if (pending.getState() == State.PENDING_REMOVE_CANCEL) {
                            if (map.remove(serviceName, pending)) {
                                // we're good
                                return;
                            }
                            // retry
                        } else {
                            assert pending.getState() == State.PENDING_REMOVE_CANCEL_REMOVE;
                            TaskController<Void> subtask = transaction.newSubtask(new ServiceRemoveTask(serviceName));
                            if (map.replace(serviceName, pending, new Pending(State.PENDING_REMOVE, subtask))) {
                                subtask.release(null);
                                return;
                            }
                            subtask.rollback(null);
                            // retry
                        }
                    }
                }
            });
        } else if (created.getState() == State.PENDING_REMOVE) {
            created.getSubtask().release(null);
        }
    }

    public Transaction getTransaction() {
        return transaction;
    }

    TaskController removePendingInstall(final ServiceName name) {
        return null;
    }

    static final class Pending {
        private final TaskController<?> subtask;
        private final State state;

        Pending(final State state, final TaskController<?> subtask) {
            if (state == null) {
                throw new IllegalArgumentException("state is null");
            }
            if (subtask == null) {
                throw new IllegalArgumentException("subtask is null");
            }
            this.state = state;
            this.subtask = subtask;
        }

        public TaskController<?> getSubtask() {
            return subtask;
        }

        public State getState() {
            return state;
        }
    }

    enum State {
        PENDING_ADD,
        PENDING_ADD_CANCEL,
        PENDING_REMOVE,
        PENDING_REMOVE_CANCEL,
        PENDING_REMOVE_CANCEL_REMOVE,
        PENDING_REMOVE_ADD,
        PENDING_MODE_CHANGE,
    }
}
