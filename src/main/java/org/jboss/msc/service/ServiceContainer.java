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
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;
import org.jboss.msc.txn.AttachmentKey;
import org.jboss.msc.txn.Subtask;
import org.jboss.msc.txn.SubtaskController;
import org.jboss.msc.txn.Transaction;
import org.jboss.msc.value.ReadableValue;

/**
 * A transactional service container.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class ServiceContainer {
    private static final AtomicReferenceFieldUpdater<ServiceContainer, Transaction> transactionUpdater = AtomicReferenceFieldUpdater.newUpdater(ServiceContainer.class, Transaction.class, "transaction");

    private final ConcurrentMap<ServiceName, Registration> registry = new ConcurrentHashMap<ServiceName, Registration>();
    private final AttachmentKey<ServiceTxn> key = AttachmentKey.create();

    private volatile Transaction transaction;

    private Registration getOrCreateRegistration(ServiceName name) {
        Registration registration = registry.get(name);
        if (registration == null) {
            registration = new Registration();
            Registration appearing = registry.putIfAbsent(name, registration);
            if (appearing != null) {
                registration = appearing;
            }
        }
        return registration;
    }

    private ServiceTxn getTxn(Transaction transaction) {
        Transaction old;
        do {
            old = this.transaction;
            if (old == transaction) {
                break;
            }
            if (old != null) {
                throw new IllegalStateException("Another transaction is active");
            }
        } while (! transactionUpdater.compareAndSet(this, old, transaction));
        ServiceTxn txn = transaction.getAttachment(key);
        if (txn == null) {
            txn = new ServiceTxn(this, transaction);
            ServiceTxn appearing = transaction.putAttachmentIfAbsent(key, txn);
            if (appearing != null) {
                txn = appearing;
            }
            assert txn.getTransaction() == transaction;
        }
        return txn;
    }

    /**
     * Initiate a service removal in the given transaction.
     *
     * @param transaction the transaction
     * @param name the service to remove
     */
    public void removeService(Transaction transaction, ServiceName name) {
        final ServiceTxn txn = getTxn(transaction);
        synchronized (txn) {
            SubtaskController controller = txn.removePendingInstall(name);
            if (controller != null) {
                controller.rollback(null);
            } else {

            }
        }
    }

    /**
     * Start building a new simple service.
     *
     * @param transaction the transaction
     * @param name the service to add
     * @param service the service
     *
     * @return the service builder
     */
    public ServiceBuilder<Void> installService(Transaction transaction, ServiceName name, SimpleService service) {
        return null;
    }

    /**
     * Start building a new simple service.
     *
     * @param transaction the transaction
     * @param name the service to add
     * @param value the injectable service value
     * @param service the service
     *
     * @return the service builder
     */
    public <T> ServiceBuilder<T> installService(Transaction transaction, ServiceName name, ReadableValue<T> value, SimpleService service) {
        return null;
    }

    /**
     * Start building a new complex service.
     *
     * @param transaction the transaction
     * @param name the service to add
     * @param startTask the service start subtask
     * @param stopTask the service stop subtask
     *
     * @return the service builder
     */
    public ServiceBuilder<Void> installService(Transaction transaction, ServiceName name, Subtask startTask, Subtask stopTask) {
        return null;
    }

    /**
     * Start building a new complex service.
     *
     * @param transaction the transaction
     * @param name the service to add
     * @param value the injectable service value
     * @param startTask the service start subtask
     * @param stopTask the service stop subtask
     *
     * @return the service builder
     */
    public <T> ServiceBuilder<T> installService(Transaction transaction, ServiceName name, ReadableValue<T> value, Subtask startTask, Subtask stopTask) {
        return new ServiceBuilder<T>(name, transaction, value, startTask, stopTask);
    }

    Registration getRegistration(final ServiceName serviceName) {
        return registry.get(serviceName);
    }
}
