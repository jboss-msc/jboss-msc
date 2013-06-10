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
import org.jboss.msc.service.Injector;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceMode;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceRegistry;
import org.jboss.msc.txn.ServiceContext;
import org.jboss.msc.txn.TaskBuilder;
import org.jboss.msc.txn.TaskController;
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
    private final Map<ServiceName, DependencySpec<?>> specs = new LinkedHashMap<ServiceName, DependencySpec<?>>();
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
    // service install task
    private TaskController<ServiceController<T>> installTask;
    // parent dependency spec, if any (only if this service is installed as a child
    private DependencySpec<?> parentDependencySpec;
    // task dependencies
    private final Set<TaskController<?>> taskDependencies = new HashSet<TaskController<?>>(0);

    /**
     * Creates service builder.
     * @param registry     the service registry
     * @param name         service name
     * @param service      the service itself
     * @param transaction  active transaction
     */
    public ServiceBuilderImpl(final ServiceRegistry registry, final ServiceName name, final Service<T> service, final Transaction transaction) {
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
    public ServiceBuilderImpl(final ServiceRegistry registry, final ServiceName name, final Service<T> service, final boolean replaceService, final Transaction transaction) {
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
    public ServiceBuilder<T> addDependency(final ServiceName name) {
        return addDependencyInternal(registry, name, null, (DependencyFlag[])null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ServiceBuilder<T> addDependency(final ServiceName name, final DependencyFlag... flags) {
        return addDependencyInternal(registry, name, null, flags);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ServiceBuilder<T> addDependency(final ServiceName name, final Injector<?> injector) {
        return addDependencyInternal(registry, name, injector, (DependencyFlag[])null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ServiceBuilder<T> addDependency(final ServiceName name, final Injector<?> injector, final DependencyFlag... flags) {
        return addDependencyInternal(registry, name, injector, flags);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ServiceBuilder<T> addDependency(final ServiceRegistry registry, final ServiceName name) {
        return addDependencyInternal(registry, name, null, (DependencyFlag[])null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ServiceBuilder<T> addDependency(final ServiceRegistry registry, final ServiceName name, final DependencyFlag... flags) {
        return addDependencyInternal(registry, name, null, flags);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ServiceBuilder<T> addDependency(final ServiceRegistry registry, final ServiceName name, final Injector<?> injector) {
        return addDependencyInternal(registry, name, injector, (DependencyFlag[])null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ServiceBuilder<T> addDependency(final ServiceRegistry registry, final ServiceName name, final Injector<?> injector, final DependencyFlag... flags) {
        return addDependencyInternal(registry, name, injector, flags);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ServiceBuilder<T> addDependency(TaskController<?> task) {
        checkAlreadyInstalled();
        taskDependencies.add(task);
        return this;
    }

    private <D> ServiceBuilder<T> addDependencyInternal(final ServiceRegistry registry, final ServiceName name, final Injector<D> injector, final DependencyFlag... flags) {
        checkAlreadyInstalled();
        if (registry == null) {
            MSCLogger.SERVICE.methodParameterIsNull("registry");
        }
        if (name == null) {
            MSCLogger.SERVICE.methodParameterIsNull("name");
        }
        final DependencySpec<D> spec = new DependencySpec<D>((ServiceRegistryImpl)registry, name, flags != null ? flags : noFlags);
        spec.addInjection(injector);
        addDependencySpec(spec, name, flags != null ? flags : noFlags);
        return this;
    }

    private void addDependencySpec(DependencySpec<?> dependencySpec, ServiceName name, DependencyFlag... flags) {
        for (DependencyFlag flag: flags) {
            if (flag == DependencyFlag.PARENT) {
                synchronized (this) {
                    if (parentDependencySpec != null) {
                        throw new IllegalStateException("Service cannot have more than one parent dependency");
                    }
                    parentDependencySpec = dependencySpec;
                    specs.remove(name);
                    return;
                }
            }
        }
        if (parentDependencySpec != null && name == parentDependencySpec.getName()) {
            parentDependencySpec = null;
        }
        specs.put(name, dependencySpec);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized void install() {
        // idempotent
        if (installTask != null) {
            return;
        }
        final TaskBuilder<ServiceController<T>> taskBuilder = transaction.newTask(new ServiceInstallTask<T>(transaction, this));
        taskBuilder.addDependencies(taskDependencies);
        if (replacement) {
            startReplacement(taskBuilder, transaction);
        }
        installTask = taskBuilder.release();
    }

    private synchronized void checkAlreadyInstalled() {
        if (installTask != null) {
            throw new IllegalStateException("ServiceBuilder installation already requested.");
        }
    }

    /**
     * Performs installation of this service builder into registry (invoked by {@link ServiceInstallTask}).
     * 
     * @param transaction active transaction
     * @param context     the service context
     * @return            the installed service controller. May be {@code null} if the service is being created with a
     *                    parent dependency.
     */
    ServiceController<T> performInstallation(Transaction transaction, ServiceContext context) {
        if (parentDependencySpec != null) {
            parentDependencySpec.createDependency(this, transaction, context);
            return null;
        }
        return installController(null, transaction, context);
    }

    /**
     * Concludes service installation by creating and installing the service controller into the registry.
     * 
     * @param parentDependency parent dependency, if any
     * @param transaction      active transaction
     * @param context          the service context
     * 
     * @return the installed service controller
     */
    ServiceController<T> installController(Dependency<?> parentDependency, Transaction transaction, ServiceContext context) {
        final Registration registration = registry.getOrCreateRegistration(context, transaction, name);
        final ServiceName[] aliasArray = aliases.toArray(new ServiceName[aliases.size()]);
        final Registration[] aliasRegistrations = new Registration[aliasArray.length];
        int i = 0; 
        for (ServiceName alias: aliases) {
            aliasRegistrations[i++] = registry.getOrCreateRegistration(context, transaction, alias);
        }
        i = 0;
        final Dependency<?>[] dependencies;
        if (parentDependency == null) {
            dependencies = new Dependency<?>[specs.size()];
        } else {
            dependencies = new Dependency<?>[specs.size() + 1];
            dependencies[i++] = parentDependency;
        }
        for (DependencySpec<?> spec : specs.values()) {
            dependencies[i++] = spec.createDependency(this, transaction, context);
        }
        final ServiceController<T> serviceController =  new ServiceController<T>(registration, aliasRegistrations, service, mode, dependencies, transaction, context);
        serviceController.install(transaction, context);
        if (replacement) {
            concludeReplacement(serviceController, transaction);
        }
        return serviceController;
    }

    private void startReplacement(TaskBuilder<ServiceController<T>> serviceInstallTaskBuilder, Transaction transaction) {
        startReplacement(registry.getOrCreateRegistration(transaction,transaction, name), serviceInstallTaskBuilder, transaction);
        for (ServiceName alias: aliases) {
            startReplacement(registry.getOrCreateRegistration(transaction, transaction, alias), serviceInstallTaskBuilder, transaction);
        }
    }

    private void startReplacement(Registration registration, TaskBuilder<ServiceController<T>> serviceInstallTaskBuilder, Transaction transaction) {
        for (Dependency<?> dependency: registration.getIncomingDependencies()) {
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
        for (Dependency<?> dependency: registration.getIncomingDependencies()) {
            dependency.dependencyReplacementConcluded(transaction);
        }
    }
}
