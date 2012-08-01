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
import org.jboss.msc.value.WritableValue;
import org.jboss.msc.txn.Subtask;
import org.jboss.msc.txn.SubtaskController;
import org.jboss.msc.txn.SubtaskListener;
import org.jboss.msc.txn.Transaction;
import org.jboss.msc.value.ReadableValue;

/**
 * A service builder.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class ServiceBuilder<T> {
    private final ServiceName name;
    private final Transaction transaction;
    private final ReadableValue<T> serviceValue;
    private final Subtask startSubtask;
    private final Subtask stopSubtask;
    private final SubtaskController installTask;
    private final Map<ServiceName, DependencySpec> specs = new LinkedHashMap<ServiceName, DependencySpec>();
    private ServiceMode mode;

    ServiceBuilder(final ServiceName name, final Transaction transaction, final ReadableValue<T> serviceValue, final Subtask startSubtask, final Subtask stopSubtask) {
        this.name = name;
        this.transaction = transaction;
        this.serviceValue = serviceValue;
        this.startSubtask = startSubtask;
        this.stopSubtask = stopSubtask;

        installTask = transaction.newSubtask(new ServiceInstallTask());
    }

    ServiceBuilder(final ServiceName name, final Transaction transaction, final SimpleService service, final ReadableValue<T> serviceValue) {
        this(name, transaction, serviceValue, new SimpleServiceStartSubtask(service), new SimpleServiceStopSubtask(service));
    }

    /**
     * Get the transaction associated with this service install.
     *
     * @return the transaction associated with this service install
     */
    public Transaction getTransaction() {
        return transaction;
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
     * @param name the service name
     */
    public void addDependency(ServiceName name) {
        addDependency(name, DependencyFlag.NONE);
    }

    /**
     * Add a dependency to the service being built.
     *
     * @param name the service name
     * @param flags the flags for the service
     */
    public void addDependency(ServiceName name, DependencyFlag... flags) {
        specs.put(name, new DependencySpec(name, flags));
    }

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
    public void addDependency(ServiceName name, WritableValue<?> injector, DependencyFlag... flags) {
        DependencySpec spec = new DependencySpec(name, flags);
        spec.getInjections().add(injector);
        specs.put(name, spec);
    }

    /**
     * Add a dependency on a subtask.  If the subtask fails, the service install will also fail.  The subtask must be
     * part of the same transaction as the service.
     *
     * @param subtask the subtask
     */
    public void addDependency(SubtaskController subtask) {
        subtask.addDependencies(subtask);
    }

    /**
     * Initiate installation of this service as configured.  If the service was already installed, this method has no
     * effect.
     *
     * @param listener the completion listener or {@code null} for none
     * @return the controller which manages the installation of the service
     */
    public SubtaskController install(final SubtaskListener listener) {
        return installTask.release(listener);
    }

    /**
     * Initiate rollback of this service installation.  If the service was already installed, it will be removed.
     */
    public void remove() {
        installTask.rollback(null);
    }
}
