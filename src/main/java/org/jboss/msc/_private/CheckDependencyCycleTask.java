/*
 * JBoss, Home of Professional Open Source
 *
 * Copyright 2013 Red Hat, Inc. and/or its affiliates.
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

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

import org.jboss.msc.service.CircularDependencyException;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.txn.AttachmentKey;
import org.jboss.msc.txn.Transaction;
import org.jboss.msc.txn.Validatable;
import org.jboss.msc.txn.ValidateContext;

/**
 * Task that checks for dependency cycles.
 * 
 * @author <a href="mailto:frainone@redhat.com">Flavia Rainone</a>
 *
 */
final class CheckDependencyCycleTask implements Validatable {

    static final AttachmentKey<CheckDependencyCycleTask> key = AttachmentKey.create();

    /**
     * Schedule a check for dependency cycles involving {@code service}. The check is performed during transaction
     * validation.
     * 
     * @param transaction the active transaction
     * @param service     the service to be verified
     */
    static void checkDependencyCycle(ServiceController<?> service, Transaction transaction) {
        final CheckDependencyCycleTask task;
        if (transaction.hasAttachment(key)) {
            task = transaction.getAttachment(key);
        } else {
            task = new CheckDependencyCycleTask();
            transaction.newTask().setValidatable(task).release();
        }
        task.checkService(service);
    }

    private final List<ServiceController<?>> services;

    private CheckDependencyCycleTask() {
        services = new CopyOnWriteArrayList<ServiceController<?>>();
    }

    private void checkService(ServiceController<?> service) {
        services.add(service);
    }

    @Override
    public void validate(ValidateContext context) {
        try {
            final Set<ServiceController<?>> checkedServices = new HashSet<ServiceController<?>>();
            final LinkedHashMap<ServiceName, ServiceController<?>> pathTrace = new LinkedHashMap<ServiceName, ServiceController<?>>();
            for (ServiceController<?> service: services) {
                if (checkedServices.contains(service)) {
                    continue;
                }
                final ServiceName serviceName = service.getPrimaryRegistration().getServiceName();
                pathTrace.put(serviceName, service);
                verifyCycle(service, pathTrace, checkedServices);
                checkedServices.add(service);
                pathTrace.remove(serviceName);
            }
        } catch (CircularDependencyException e) {
            context.addProblem(e);
        } finally {
            context.complete();
        }
    }

    private void verifyCycle(ServiceController<?> service, LinkedHashMap<ServiceName, ServiceController<?>> pathTrace, Set<ServiceController<?>> checkedServices) throws CircularDependencyException {
        for (AbstractDependency<?> dependency: service.getDependencies()) {
            final ServiceController<?> dependencyController = dependency.getDependencyRegistration().getController();
            if (dependencyController != null && !checkedServices.contains(dependencyController)) {
                if (pathTrace.containsValue(dependencyController)) {
                    final ServiceName[] cycle = pathTrace.keySet().toArray(new ServiceName[pathTrace.size()]);
                    throw new CircularDependencyException("Dependency cycle found: " + Arrays.toString(cycle), cycle);
                } // TODO add a problem and log
                final ServiceName dependencyName = dependency.getDependencyRegistration().getServiceName();
                pathTrace.put(dependencyName, dependencyController);
                verifyCycle(dependencyController, pathTrace, checkedServices);
                checkedServices.add(dependencyController);
                pathTrace.remove(dependencyName);
            }
        }
    }
}
