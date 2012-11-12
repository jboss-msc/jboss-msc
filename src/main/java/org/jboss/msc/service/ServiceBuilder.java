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

import java.util.LinkedHashMap;
import java.util.Map;
import org.jboss.msc.txn.TaskBuilder;
import org.jboss.msc.txn.TaskController;
import org.jboss.msc.value.WritableValue;
import org.jboss.msc.txn.Transaction;

/**
 * A service builder.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class ServiceBuilder<T> {
    private final ServiceContainer container;
    private final ServiceName name;
    private final Service<T> service;
    private final Map<ServiceName, DependencySpec<?>> specs = new LinkedHashMap<ServiceName, DependencySpec<?>>();
    private ServiceMode mode;

    public ServiceBuilder(final ServiceContainer container, final ServiceName name, final Service<T> service) {
        this.container = container;
        this.name = name;
        this.service = service;
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
        if (mode == null) {
            throw new IllegalArgumentException("mode is null");
        }
        this.mode = mode;
    }

    /**
     * Add a dependency to the service being built.
     *
     * @param container the service container
     * @param name the service name
     */
    public void addDependency(ServiceContainer container, ServiceName name) {
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

    /**
     * Add a dependency to the service being built.
     *
     * @param name the service name
     * @param flags the flags for the service
     */
    public void addDependency(ServiceContainer container, ServiceName name, DependencyFlag... flags) {
        specs.put(name, new DependencySpec(container, name, flags));
    }

    /**
     * Add a dependency to the service being built.
     *
     * @param name the service name
     * @param flags the flags for the service
     */
    public void addDependency(ServiceName name, DependencyFlag... flags) {
        specs.put(name, new DependencySpec(container, name, flags));
    }

    /**
     * Add an injected dependency to the service being built.  The dependency will be injected just before
     * starting this service and uninjected just before stopping it.
     *
     * @param name the service name
     * @param injector the injector for the dependency value
     */
    public void addDependency(ServiceContainer container, ServiceName name, WritableValue<?> injector) {
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
    public <T> void addDependency(ServiceContainer container, ServiceName name, WritableValue<T> injector, DependencyFlag... flags) {
        DependencySpec<T> spec = new DependencySpec<T>(container, name, flags);
        spec.addInjection(injector);
        specs.put(name, spec);
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
        DependencySpec<T> spec = new DependencySpec<T>(container, name, flags);
        spec.addInjection(injector);
        specs.put(name, spec);
    }

    /**
     * Add a dependency on a task.  If the task fails, the service install will also fail.  The task must be
     * part of the same transaction as the service.
     *
     * @param task the task
     */
    public void addDependency(Object task) {
    }

    /**
     * Initiate installation of this service as configured.  If the service was already installed, this method has no
     * effect.
     */
    public void install(Transaction transaction) {
        // todo install into registry (throws {@link org.jboss.msc.service.DuplicateServiceException})
        //  - wire up dependency graph
        final ServiceInstallTask<T> installTask = new ServiceInstallTask<T>(null, null, service);
        final TaskBuilder<Void> taskBuilder = transaction.newTask();
        taskBuilder.setTraits(installTask);
        final TaskController<Void> installController = taskBuilder.release();
        // todo put this controller on the transaction
        // todo examine the service
        //  - check mode & dependency state, determine task
        /* maybe */ {
            SimpleServiceStartTask<T> startTask = new SimpleServiceStartTask<T>(service);
            final TaskBuilder<T> startBuilder = transaction.newTask(startTask);
            startBuilder.addDependency(installController);
            final TaskController<T> startController = startBuilder.release();
            // todo put this controller on the transaction
        }
    }

    /**
     * Initiate rollback of this service installation.  If the service was already installed, it will be removed.
     */
    public void remove() {
    }
}
