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

package org.jboss.msc._private;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceNotFoundException;
import org.jboss.msc.service.ServiceRegistry;
import org.jboss.msc.txn.ServiceContext;
import org.jboss.msc.txn.Transaction;

/**
 * A service registry.  Registries can return services by name, or get a collection of service names.
 *
 * @author <a href="mailto:frainone@redhat.com">Flavia Rainone</a>
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
final class ServiceRegistryImpl extends TransactionalObject implements ServiceRegistry {
    // map of service registrations
    private final ConcurrentMap<ServiceName, Registration> registry = new ConcurrentHashMap<ServiceName, Registration>();
    // indicates whether this registry is removed
    private boolean removed;

    /**
     * Gets a service, throwing an exception if it is not found.
     *
     * @param serviceName the service name
     * @return the service corresponding to {@code serviceName}
     * @throws ServiceNotFoundException if the service is not present in the registry
     */
    public Service<?> getRequiredService(ServiceName serviceName) throws ServiceNotFoundException {
        return getRequiredServiceController(serviceName).getService();
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
        return registration.getController() == null? null: registration.getController().getService();
    }

    Registration getOrCreateRegistration(ServiceContext context, Transaction transaction, ServiceName name) {
        Registration registration = registry.get(name);
        if (registration == null) {
            checkRemoved();
            lockWrite(transaction, context);
            registration = new Registration(name);
            Registration appearing = registry.putIfAbsent(name, registration);
            if (appearing != null) {
                registration = appearing;
            }
        }
        return registration;
    }

    Registration getRegistration(ServiceName name) {
        return registry.get(name);
    }

    public ServiceController<?> getRequiredServiceController(ServiceName serviceName) throws ServiceNotFoundException {
        final ServiceController<?> controller = registry.containsKey(serviceName)? registry.get(serviceName).getController(): null;
        if (controller == null) {
            throw new ServiceNotFoundException("Service " + serviceName + " not found");
        }
        return controller;
    }

    void remove(Transaction transaction) {
        synchronized(this) {
            if (removed) {
                return;
            }
            removed = true;
        }
        final HashSet<ServiceController<?>> done = new HashSet<ServiceController<?>>();
        for (Registration registration : registry.values()) {
            ServiceController<?> serviceInstance = registration.getController();
            if (serviceInstance != null && done.add(serviceInstance)) {
                serviceInstance.remove(transaction);
            }
        }
    }

    synchronized boolean isRemoved() {
        return removed;
    }

    synchronized void disable(Transaction transaction) {
        checkRemoved();
        for (Registration registration: registry.values()) {
            final ServiceController<?> controller = registration.getController();
            if (controller != null) {
                controller.disable(transaction);
            }
        }
    }

    synchronized void enable(Transaction transaction) {
        checkRemoved();
        for (Registration registration: registry.values()) {
            final ServiceController<?> controller = registration.getController();
            if (controller != null) {
                controller.enable(transaction);
            }
        }
    }

    // TODO ongoing work: code under review
    public void install(ServiceController<?> serviceController) {
        checkRemoved();
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

    private synchronized void checkRemoved() {
        if (removed) {
            throw new IllegalStateException("ServiceRegistry is removed");
        }
    }
}
