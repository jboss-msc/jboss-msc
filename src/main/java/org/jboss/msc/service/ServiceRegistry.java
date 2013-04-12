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

package org.jboss.msc.service;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.jboss.msc.txn.Transaction;

/**
 * A service registry.  Registries can return services by name, or get a collection of service names.
 *
 * @author <a href="mailto:frainone@redhat.com">Flavia Rainone</a>
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class ServiceRegistry extends TransactionalObject {

    private final ConcurrentMap<ServiceName, Registration> registry = new ConcurrentHashMap<ServiceName, Registration>();

    /**
     * Gets a service, throwing an exception if it is not found.
     *
     * @param serviceName the service name
     * @return the service corresponding to {@code serviceName}
     * @throws ServiceNotFoundException if the service is not present in the registry
     */
    public Service<?> getRequiredService(ServiceName serviceName) throws ServiceNotFoundException {
        final ServiceController<?> controller = registry.containsKey(serviceName)? registry.get(serviceName).getController(): null;
        if (controller == null) {
            throw new ServiceNotFoundException("Service " + serviceName + " not found");
        }
        return controller.getValue().getValue();
    }

    /**
     * Gets a service, returning {@code null} if it is not found.
     *
     * @param serviceName the service name
     * @return the service corresponding to {@code serviceName}, or {@code null} if it is not found
     */
    public Service<?> getService(ServiceName serviceName) {
        final Registration registration = registry.get(serviceName);
        if (registration == null) {
            return null;
        }
        return registration.getController() == null? null: registration.getController().getValue().getValue();
    }

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

    public void clear(Transaction transaction) {
        final HashSet<ServiceController<?>> done = new HashSet<ServiceController<?>>();
        for (Registration registration : registry.values()) {
            ServiceController<?> serviceInstance = registration.getController();
            if (serviceInstance != null && done.add(serviceInstance)) {
                serviceInstance.remove(transaction);
            }
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    synchronized void revert(Object snapshot) {
        registry.clear();
        registry.putAll((Map<ServiceName, Registration>)snapshot);
    }

    @Override
    synchronized Object takeSnapshot() {
        final Map<ServiceName, Registration> snapshot = new HashMap<ServiceName, Registration>(registry.size());
        snapshot.putAll(registry);
        return snapshot;
    }
}
