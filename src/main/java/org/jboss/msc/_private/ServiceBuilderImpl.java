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

package org.jboss.msc._private;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.jboss.msc.service.Dependency;
import org.jboss.msc.service.DependencyFlag;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceMode;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceRegistry;
import org.jboss.msc.txn.ServiceContext;
import org.jboss.msc.txn.TaskFactory;
import org.jboss.msc.txn.Transaction;

/**
 * A service builder.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 * @author <a href="mailto:frainone@redhat.com">Flavia Rainone</a>
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
final class ServiceBuilderImpl<T> implements ServiceBuilder<T> {

    private static final DependencyFlag[] noFlags = new DependencyFlag[0];

    // the service registry
    private final ServiceRegistryImpl registry;
    // service name
    private final ServiceName name;
    // service aliases
    private final Set<ServiceName> aliases = new HashSet<ServiceName>(0);
    // service itself
    private Service<T> service;
    // dependencies
    private final Map<ServiceName, DependencyImpl<?>> dependencies= new LinkedHashMap<ServiceName, DependencyImpl<?>>();
    // active transaction
    private final Transaction transaction;
    // service mode
    private ServiceMode mode;
    // is service builder installed?
    private boolean installed;
    // parent dependency, if any (only if this service is installed as a child
    private ParentDependency<?> parentDependency;

    /**
     * Creates service builder.
     * @param registry     the service registry
     * @param name         service name
     * @param service      the service itself
     * @param transaction  active transaction
     */
    ServiceBuilderImpl(final ServiceRegistry registry, final ServiceName name, final Transaction transaction) {
        this.transaction = transaction;
        this.registry = (ServiceRegistryImpl)registry;
        this.name = name;
        this.mode = ServiceMode.ACTIVE;
    }

    ServiceName getServiceName() {
        return name;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ServiceBuilder<T> setMode(final ServiceMode mode) {
        checkAlreadyInstalled();
        if (mode == null) {
            MSCLogger.SERVICE.methodParameterIsNull("mode");
        }
        this.mode = mode;
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ServiceBuilder<T> setService(final Service<T> service) {
        checkAlreadyInstalled();
        if (service == null) {
            MSCLogger.SERVICE.methodParameterIsNull("service");
        }
        this.service = service;
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ServiceBuilderImpl<T> addAliases(final ServiceName... aliases) {
        checkAlreadyInstalled();
        if (aliases != null) for (final ServiceName alias : aliases) {
            if (alias != null && !alias.equals(name)) {
                this.aliases.add(alias);
            }
        }
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <D> Dependency<D> addDependency(final ServiceName name) {
        return addDependencyInternal(registry, name, (DependencyFlag[])null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <D> Dependency<D> addDependency(final ServiceName name, final DependencyFlag... flags) {
        return addDependencyInternal(registry, name, flags);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <D> Dependency<D> addDependency(final ServiceRegistry registry, final ServiceName name) {
        return addDependencyInternal(registry, name, (DependencyFlag[])null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <D> Dependency<D> addDependency(final ServiceRegistry registry, final ServiceName name, final DependencyFlag... flags) {
        return addDependencyInternal(registry, name, flags);
    }

    private <D> Dependency<D> addDependencyInternal(final ServiceRegistry registry, final ServiceName name, final DependencyFlag... flags) {
        checkAlreadyInstalled();
        if (registry == null) {
            MSCLogger.SERVICE.methodParameterIsNull("registry");
        }
        if (name == null) {
            MSCLogger.SERVICE.methodParameterIsNull("name");
        }
        final DependencyImpl<D> dependency = DependencyFactory.create((ServiceRegistryImpl) registry, name, flags != null ? flags : noFlags, this, transaction);
        addDependency(dependency, name, flags != null ? flags : noFlags);
        return dependency;
    }

    private void addDependency(DependencyImpl<?> dependency, ServiceName name, DependencyFlag... flags) {
        for (DependencyFlag flag: flags) {
            if (flag == DependencyFlag.PARENT) {
                if (parentDependency != null) {
                    throw new IllegalStateException("Service cannot have more than one parent dependency");
                }
                parentDependency = (ParentDependency<?>) dependency;
                dependencies.remove(name);
                return;
            }
        }
        if (parentDependency != null && name.equals(parentDependency.getDependencyRegistration().getServiceName())) {
            parentDependency = null;
        }
        dependencies.put(name, dependency);
    }

    @Override
    public ServiceContext getServiceContext() {
        return new ParentServiceContext(name, registry);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void install() {
        // idempotent
        if (installed) {
            return;
        }
        if (parentDependency != null) {
            parentDependency.install(transaction);
            return;
        }
        performInstallation(null, transaction, transaction);
    }

    private void checkAlreadyInstalled() {
        if (installed) {
            throw new IllegalStateException("ServiceBuilder installation already requested.");
        }
    }

    /**
     * Concludes service installation by creating and installing the service controller into the registry.
     * 
     * @param parentDependency parent dependency, if any
     * @param transaction      active transaction
     * @param context          the execute context (this parameter will be needed by the parent dependency)
     * 
     * @return the installed service controller
     */
    ServiceController<?> performInstallation(ParentDependency<?> parentDependency, Transaction transaction, TaskFactory taskFactory) {
        // create primary registration
        final Registration registration = registry.getOrCreateRegistration(transaction, taskFactory, name);

        // create alias registrations
        final ServiceName[] aliasArray = aliases.toArray(new ServiceName[aliases.size()]);
        final Registration[] aliasRegistrations = new Registration[aliasArray.length];
        int i = 0; 
        for (ServiceName alias: aliases) {
            aliasRegistrations[i++] = registry.getOrCreateRegistration(transaction, taskFactory, alias);
        }

        // create dependencies
        i = 0;
        final DependencyImpl<?>[] dependenciesArray;
        if (parentDependency == null) {
            dependenciesArray = new DependencyImpl<?>[dependencies.size()];
            dependencies.values().toArray(dependenciesArray);
        } else {
            dependenciesArray = new DependencyImpl<?>[dependencies.size() + 1];
            dependencies.values().toArray(dependenciesArray);
            dependenciesArray[dependencies.size()] = parentDependency;
        }
        // create and install service controller
        final ServiceController<T> serviceController =  new ServiceController<T>(registration, aliasRegistrations, service, mode, dependenciesArray, transaction, taskFactory);
        serviceController.install(registry, transaction, taskFactory);
        CheckDependencyCycleTask.checkDependencyCycle(serviceController, transaction, taskFactory);
        return serviceController;
    }
}
