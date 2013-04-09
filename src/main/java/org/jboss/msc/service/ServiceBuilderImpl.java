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

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.jboss.msc.txn.TaskBuilder;
import org.jboss.msc.txn.TaskController;
import org.jboss.msc.txn.Transaction;
import org.jboss.msc.value.WritableValue;

/**
 * A service builder.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 * @author <a href="mailto:frainone@redhat.com">Flavia Rainone</a>
 */
final class ServiceBuilderImpl<T> {
    private final ServiceContainerImpl container;
    private final ServiceName name;
    private final Set<ServiceName> aliases = new HashSet<ServiceName>(0);
    private final Service<T> service;
    private final Map<ServiceName, DependencySpec<?>> specs = new LinkedHashMap<ServiceName, DependencySpec<?>>();
    private final boolean replacement;
    private ServiceMode mode;
    private TaskController<ServiceController<T>> installTask;
    private DependencySpec<?> parentDependencySpec;
    private final Set<TaskController<?>> taskDependencies = new HashSet<TaskController<?>>(0);

    public ServiceBuilderImpl(final ServiceContainerImpl container, final ServiceName name, final Service<T> service) {
        this(container, name, service, false);
    }

    public ServiceBuilderImpl(final ServiceContainerImpl container, final ServiceName name, final Service<T> service, final boolean replaceService) {
        this.container = container;
        this.name = name;
        this.service = service;
        this.replacement = true;
    }

    /**
     * Get the service mode.
     *
     * @return the service mode
     */
    public ServiceMode getMode() {
        return mode;
    }

    /**
     * Set the service mode.
     *
     * @param mode the service mode
     */
    public void setMode(final ServiceMode mode) {
        checkAlreadyInstalled();
        if (mode == null) {
            throw new IllegalArgumentException("mode is null");
        }
        this.mode = mode;
    }

    /**
     * Add aliases for this service.
     *
     * @param aliases the service names to use as aliases
     * @return the builder
     */
    public ServiceBuilderImpl<T> addAliases(ServiceName... aliases) {
        checkAlreadyInstalled();
        if (aliases != null) for(ServiceName alias : aliases) {
            if(alias != null && !alias.equals(name)) {
                this.aliases.add(alias);
            }
        }
        return this;
    }

    /**
     * Add a dependency to the service being built.
     *
     * @param container the service container
     * @param name the service name
     */
    public void addDependency(ServiceContainerImpl container, ServiceName name) {
        addDependency(container, name, DependencyFlag.NONE);
    }

    /**
     * Add a dependency to the service being built.
     *
     * @param name the service name
     */
    public void addDependency(ServiceName name) {
        addDependency(container, name, DependencyFlag.NONE);
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
        if (name == parentDependencySpec.getName()) {
            parentDependencySpec = null;
        }
        specs.put(name, dependencySpec);
    }

    /**
     * Add a dependency to the service being built.
     *
     * @param name the service name
     * @param flags the flags for the service
     */
    public void addDependency(ServiceContainerImpl container, ServiceName name, DependencyFlag... flags) {
        checkAlreadyInstalled();
        addDependencySpec(new DependencySpec(container, name, flags), name, flags);
    }

    /**
     * Add a dependency to the service being built.
     *
     * @param name the service name
     * @param flags the flags for the service
     */
    public void addDependency(ServiceName name, DependencyFlag... flags) {
        checkAlreadyInstalled();
        addDependencySpec(new DependencySpec(container, name, flags), name, flags);
    }

    /**
     * Add an injected dependency to the service being built.  The dependency will be injected just before
     * starting this service and uninjected just before stopping it.
     *
     * @param name the service name
     * @param injector the injector for the dependency value
     */
    public void addDependency(ServiceContainerImpl container, ServiceName name, WritableValue<?> injector) {
        checkAlreadyInstalled();
        addDependency(container, name, injector, DependencyFlag.NONE);
    };

    /**
     * Add an injected dependency to the service being built.  The dependency will be injected just before
     * starting this service and uninjected just before stopping it.
     *
     * @param name the service name
     * @param injector the injector for the dependency value
     */
    public void addDependency(ServiceName name, WritableValue<?> injector) {
        checkAlreadyInstalled();
        addDependency(name, injector, DependencyFlag.NONE);
    };

    /**
     * Add an injected dependency to the service being built.  The dependency will be injected just before starting this
     * service and uninjected just before stopping it.
     *
     * @param name the service name
     * @param injector the injector for the dependency value
     * @param flags the flags for the service
     */
    public <T> void addDependency(ServiceContainerImpl container, ServiceName name, WritableValue<T> injector, DependencyFlag... flags) {
        checkAlreadyInstalled();
        DependencySpec<T> spec = new DependencySpec<T>(container, name, flags);
        spec.addInjection(injector);
        addDependencySpec(spec, name, flags);
    }

    /**
     * Add an injected dependency to the service being built.  The dependency will be injected just before starting this
     * service and uninjected just before stopping it.
     *
     * @param name the service name
     * @param injector the injector for the dependency value
     * @param flags the flags for the service
     */
    public <T> void addDependency(ServiceName name, WritableValue<T> injector, DependencyFlag... flags) {
        checkAlreadyInstalled();
        DependencySpec<T> spec = new DependencySpec<T>(container, name, flags);
        spec.addInjection(injector);
        addDependencySpec(spec, name, flags);
    }

    /**
     * Add a dependency on a task.  If the task fails, the service install will also fail.  The task must be
     * part of the same transaction as the service.
     *
     * @param task the task
     */
    public void addDependency(TaskController<?> task) {
        checkAlreadyInstalled();
        taskDependencies.add(task);
    }

    /**
     * Initiate installation of this service as configured.  If the service was already installed, this method has no
     * effect.
     */
    public synchronized void install(Transaction transaction) {
        checkAlreadyInstalled();
        final TaskBuilder<ServiceController<T>> taskBuilder = transaction.newTask(new ServiceInstallTask<T>(transaction, this));
        taskBuilder.addDependencies(taskDependencies);
        if (replacement) {
            startReplacement(transaction, taskBuilder);
        }
        installTask = taskBuilder.release();
    }

    /**
     * Manually rollback this service installation.  If the service was already installed, it will be removed.
     */
    public void remove(Transaction transaction) {
        // TODO review this method
        final ServiceController<?> serviceController;
        synchronized (this) {
            serviceController = installTask.getResult();
        }
        if (serviceController != null) {
            serviceController.remove(transaction);
        }
    }

    private synchronized void checkAlreadyInstalled() {
        if (installTask != null) {
            throw new IllegalStateException("ServiceBuilder installation already requested.");
        }
    }

    /**
     * Perform installation of this service builder into container.
     * 
     * @param transaction active transaction
     * @return            the installed service controller. May be {@code null} if the service is being created with a
     *                    parent dependency.
     */
    ServiceController<T> performInstallation(Transaction transaction) {
        if (parentDependencySpec != null) {
            parentDependencySpec.createDependency(transaction, this);
            return null;
        }
        return installController(transaction, null);
    }

    /**
     * Concludes service installation by creating and installing the service controller into the container.
     * 
     * @param transaction      active transaction
     * @param parentDependency parent dependency, if any
     * @return the installed service controller
     */
    ServiceController<T> installController(Transaction transaction, Dependency<?> parentDependency) {
        final Registration registration = container.getOrCreateRegistration(transaction, name);
        final ServiceName[] aliasArray = aliases.toArray(new ServiceName[aliases.size()]);
        final Registration[] aliasRegistrations = new Registration[aliasArray.length];
        int i = 0; 
        for (ServiceName alias: aliases) {
            aliasRegistrations[i++] = container.getOrCreateRegistration(transaction, alias);
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
            dependencies[i++] = spec.createDependency(transaction, this);
        }
        final ServiceController<T> serviceController =  new ServiceController<T>(transaction, dependencies, aliasRegistrations, registration);
        serviceController.getWriteValue().setValue(service);
        serviceController.setMode(transaction, mode);
        if (replacement) {
            concludeReplacement(transaction, serviceController);
        }
        return serviceController;
    }

    private void startReplacement(Transaction transaction, TaskBuilder<ServiceController<T>> serviceInstallTaskBuilder) {
        startReplacement(transaction, container.getOrCreateRegistration(transaction, name), serviceInstallTaskBuilder);
        for (ServiceName alias: aliases) {
            startReplacement(transaction, container.getOrCreateRegistration(transaction, alias), serviceInstallTaskBuilder);
        }
    }

    private void startReplacement(Transaction transaction, Registration registration, TaskBuilder<ServiceController<T>> serviceInstallTaskBuilder) {
        for (Dependency<?> dependency: registration.getIncomingDependencies()) {
            dependency.dependencyReplacementStarted(transaction);
        }
        ServiceController<?> serviceController = registration.getController();
        if (serviceController != null) {
            serviceInstallTaskBuilder.addDependency(serviceController.remove(transaction));
        }
    }

    private void concludeReplacement(Transaction transaction, ServiceController<?> serviceController) {
        concludeReplacement(transaction, serviceController.getPrimaryRegistration());
        for (Registration registration: serviceController.getAliasRegistrations()) {
            concludeReplacement(transaction,  registration);
        }
    }

    private void concludeReplacement(Transaction transaction, Registration registration) {
        for (Dependency<?> dependency: registration.getIncomingDependencies()) {
            dependency.dependencyReplacementConcluded(transaction);
        }
    }
}
