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

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.jboss.msc.txn.Transaction;

/**
 * A transactional service container.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 * @author <a href="mailto:frainone@redhat.com">Flavia Rainone</a>
 */
final class ServiceContainerImpl extends TransactionalObject {
    private final ConcurrentMap<ServiceName, Registration> registry = new ConcurrentHashMap<ServiceName, Registration>();

    Registration getOrCreateRegistration(Transaction transaction, ServiceName name) {
        Registration registration = registry.get(name);
        if (registration == null) {
            lockWrite(transaction);
            registration = new Registration(name);
            Registration appearing = registry.putIfAbsent(name, registration);
            if (appearing != null) {
                registration = appearing;
            }
        }
        return registration;
    }

    /**
     * Initiate a service removal in the given transaction.
     *
     * @param transaction the transaction
     * @param name the service to remove
     */
    public void removeService(Transaction transaction, ServiceName name) {
        if (!registry.containsKey(name)) {
            return;
        }
        final ServiceController<?> serviceController = registry.get(name).getController();
        if (serviceController != null) {
            serviceController.remove(transaction);
        }
    }

    Registration getRegistration(final ServiceName serviceName) {
        return registry.get(serviceName);
    }

    @Override
    protected Object takeSnapshot() {
        Map<ServiceName, Registration> registrySnapshot = new HashMap<ServiceName, Registration>(registry.size());
        registrySnapshot.putAll(registry);
        return registrySnapshot;
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void revert(Object snapshot) {
        registry.clear();
        registry.putAll((Map<ServiceName, Registration>)snapshot);
    }
}
