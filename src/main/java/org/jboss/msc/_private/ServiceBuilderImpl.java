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

import org.jboss.msc.service.DependencyFlag;
import org.jboss.msc.service.Dependency;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceMode;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceRegistry;
import org.jboss.msc.txn.ServiceContext;
import org.jboss.msc.txn.TaskBuilder;
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
    private final Service<T> service;
    // dependencies
    private final Map<ServiceName, AbstractDependency<?>> dependencies= new LinkedHashMap<ServiceName, AbstractDependency<?>>();
    // TODO decide after discussion on IRC if we have a special API for replacement (in which case we will need this parameter
    // or if we will automatically remove previously existent service always, triggering always replacement notifications
    // to dependencies; or if we will automatically remove previously existent service only if there is a replacement
    // dependent
    // indicates if this service is a replacement dependency of another service
    private final boolean replacement;
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
    ServiceBuilderImpl(final ServiceRegistry registry, final ServiceName name, final Service<T> service, final Transaction transaction) {
        this(registry, name, service, false, transaction);
    }

    /**
     * Creates the service builder.
     * @param registry       the service registry
     * @param name           the service name
     * @param service        the service itself
     * @param replaceService {@code true} if this service is a replacement dependency of another
     * @param transaction    active transaction
     */
    ServiceBuilderImpl(final ServiceRegistry registry, final ServiceName name, final Service<T> service, final boolean replaceService, final Transaction transaction) {
        this.transaction = transaction;
        this.registry = (ServiceRegistryImpl)registry;
        this.name = name;
        this.service = service;
        this.replacement = replaceService;
        this.mode = ServiceMode.ACTIVE;
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
    public Dependency<?> addDependency(final ServiceName name) {
        return addDependencyInternal(registry, name, (DependencyFlag[])null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Dependency<?> addDependency(final ServiceName name, final DependencyFlag... flags) {
        return addDependencyInternal(registry, name, flags);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Dependency<?> addDependency(final ServiceRegistry registry, final ServiceName name) {
        return addDependencyInternal(registry, name, (DependencyFlag[])null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Dependency<?> addDependency(final ServiceRegistry registry, final ServiceName name, final DependencyFlag... flags) {
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
        final AbstractDependency<D> dependency = DependencyFactory.create((ServiceRegistryImpl) registry, name, flags != null ? flags : noFlags, this, transaction);
        addDependency(dependency, name, flags != null ? flags : noFlags);
        return dependency;
    }

    private void addDependency(AbstractDependency<?> dependency, ServiceName name, DependencyFlag... flags) {
        for (DependencyFlag flag: flags) {
            if (flag == DependencyFlag.PARENT) {
                synchronized (this) {
                    if (parentDependency != null) {
                        throw new IllegalStateException("Service cannot have more than one parent dependency");
                    }
                    parentDependency = (ParentDependency<?>) dependency;
                    dependencies.remove(name);
                    return;
                }
            }
        }
        if (parentDependency != null && name.equals(parentDependency.getDependencyRegistration().getServiceName())) {
            parentDependency = null;
        }
        // TODO review this
        if (dependencies.containsKey(name)) {
            throw new IllegalStateException("ServiceBuilderImpl already contains a dependency to service " + name);
        }
        dependencies.put(name, dependency);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized void install() {
        // idempotent
        if (installed) {
            return;
        }
        if (parentDependency != null) {
            parentDependency.install(transaction);
            return;
        }
        performInstallation(null, transaction, transaction);
        if (replacement) {
            startReplacement(null, transaction);
        }
    }

    private synchronized void checkAlreadyInstalled() {
        if (installed) {
            throw new IllegalStateException("ServiceBuilder installation already requested.");
        }
    }

    /**
     * Concludes service installation by creating and installing the service controller into the registry.
     * 
     * @param parentDependency parent dependency, if any
     * @param transaction      active transaction
     * @param context          the execute context
     * 
     * @return the installed service controller
     */
    void performInstallation(ParentDependency<?> parentDependency, Transaction transaction, ServiceContext context) {
        // create primary registration
        final Registration registration = registry.getOrCreateRegistration(context, transaction, name);

        // create alias registrations
        final ServiceName[] aliasArray = aliases.toArray(new ServiceName[aliases.size()]);
        final Registration[] aliasRegistrations = new Registration[aliasArray.length];
        int i = 0; 
        for (ServiceName alias: aliases) {
            aliasRegistrations[i++] = registry.getOrCreateRegistration(context, transaction, alias);
        }

        // create dependencies
        i = 0;
        final AbstractDependency<?>[] dependenciesArray;
        if (parentDependency == null) {
            dependenciesArray = new AbstractDependency<?>[dependencies.size()];
        } else {
            dependenciesArray = new AbstractDependency<?>[dependencies.size() + 1];
            dependenciesArray[dependencies.size()] = parentDependency;
        }
        dependencies.values().toArray(dependenciesArray);
        // create and install service controller
        final ServiceController<T> serviceController =  new ServiceController<T>(registration, aliasRegistrations, service, mode, dependenciesArray, transaction, context);
        serviceController.install(transaction, context);
        // replace
        if (replacement) {
            concludeReplacement(serviceController, transaction);
        }
        CheckDependencyCycleTask.checkDependencyCycle(serviceController, transaction);
    }

    private void startReplacement(TaskBuilder<ServiceController<T>> serviceInstallTaskBuilder, Transaction transaction) {
        // TODO remove this
        startReplacement(registry.getOrCreateRegistration(transaction,transaction, name), serviceInstallTaskBuilder, transaction);
        for (ServiceName alias: aliases) {
            startReplacement(registry.getOrCreateRegistration(transaction, transaction, alias), serviceInstallTaskBuilder, transaction);
        }
    }

    private void startReplacement(Registration registration, TaskBuilder<ServiceController<T>> serviceInstallTaskBuilder, Transaction transaction) {
        for (AbstractDependency<?> dependency: registration.getIncomingDependencies()) {
            dependency.dependencyReplacementStarted(transaction);
        }
        ServiceController<?> serviceController = registration.getController();
        if (serviceController != null) {
            serviceInstallTaskBuilder.addDependency(serviceController.remove(transaction));
        }
    }

    private void concludeReplacement(ServiceController<?> serviceController, Transaction transaction) {
        concludeReplacement(serviceController.getPrimaryRegistration(), transaction);
        for (Registration registration: serviceController.getAliasRegistrations()) {
            concludeReplacement(registration,  transaction);
        }
    }

    private void concludeReplacement(Registration registration, Transaction transaction) {
        for (AbstractDependency<?> dependency: registration.getIncomingDependencies()) {
            dependency.dependencyReplacementConcluded(transaction);
        }
    }
}
