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

import static org.jboss.msc._private.ServiceController.STATE_UP;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import org.jboss.msc.service.ServiceName;
import org.jboss.msc.txn.ReportableContext;
import org.jboss.msc.txn.ServiceContext;
import org.jboss.msc.txn.TaskController;
import org.jboss.msc.txn.Transaction;


/**
 * A service registration.
 * 
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 * @author <a href="mailto:frainone@redhat.com">Flavia Rainone</a>
 */
final class Registration extends TransactionalObject {

    /** The registration name */
    private final ServiceName serviceName;
    /**
     * The service controller.
     */
    private volatile ServiceController<?> controller;
    /**
     * Incoming dependencies, i.e., dependent services.
     */
    private final Set<DependencyImpl<?>> incomingDependencies = new CopyOnWriteArraySet<DependencyImpl<?>>();
    /**
     * The number of dependent instances which place a demand-to-start on this registration.  If this value is > 0,
     * propagate a demand to the instance, if any.
     */
    private int upDemandedByCount;

    Registration(ServiceName serviceName) {
        this.serviceName = serviceName;
    }

    ServiceName getServiceName() {
        return serviceName;
    }

    ServiceController<?> getController() {
        return controller;
    }

    void setController(final ServiceContext context, final Transaction transaction, final ServiceController<?> serviceController) {
        lockWrite(transaction, context);
        final boolean upDemanded;
        synchronized (this) {
            this.controller = serviceController;
            upDemanded = upDemandedByCount > 0;
        }
        if (upDemanded) {
            serviceController.upDemanded(transaction, transaction);
        }
    }

    void clearController(final Transaction transaction, final ServiceContext context) {
        lockWrite(transaction, context);
        synchronized (this) {
            this.controller = null;
        }
    }

    void addIncomingDependency(final Transaction transaction, final ServiceContext context, final DependencyImpl<?> dependency) {
        lockWrite(transaction, context);
        final boolean dependencyUp;
        synchronized (this) {
            incomingDependencies.add(dependency);
            dependencyUp = controller != null && controller.getState() == STATE_UP;
        }
        if (dependencyUp) {
            dependency.dependencyUp(transaction, context);
        }
    }

    void removeIncomingDependency(final Transaction transaction, final ServiceContext context, final DependencyImpl<?> dependency) {
        lockWrite(transaction, context);
        assert incomingDependencies.contains(dependency);
        incomingDependencies.remove(dependency);
    }

    void serviceUp(final Transaction transaction, final ServiceContext context) {
        for (DependencyImpl<?> incomingDependency: incomingDependencies) {
            incomingDependency.dependencyUp(transaction, context);
        }
    }

    void serviceDown(final Transaction transaction, final ServiceContext context, final List<TaskController<?>> tasks) {
        for (DependencyImpl<?> incomingDependency: incomingDependencies) {
            final TaskController<?> task = incomingDependency.dependencyDown(transaction, context);
            if (task != null) {
                tasks.add(task);
            }
        }
    }

    void addDemand(Transaction transaction, ServiceContext context) {
        assert ! Thread.holdsLock(this);
        lockWrite(transaction, context);
        final ServiceController<?> controller;
        synchronized (this) {
            controller = this.controller;
            if (++ upDemandedByCount > 1) {
                return;
            }
        }
        if (controller != null) {
            controller.upDemanded(transaction, context);
        }
    }

    void removeDemand(Transaction transaction, ServiceContext context) {
        assert ! Thread.holdsLock(this);
        lockWrite(transaction, context);
        synchronized (this) {
            controller = this.controller;
            if (upDemandedByCount-- > 0) {
                return;
            }
        }
        if (controller != null) {
            controller.upUndemanded(transaction, context);
        }
    }

    @Override
    protected synchronized Object takeSnapshot() {
        return new Snapshot();
    }

    @Override
    protected synchronized void validate(ReportableContext context) {
        for (DependencyImpl<?> incomingDependency: incomingDependencies) {
            incomingDependency.validate(controller, context);
        }
    }

    @Override
    protected synchronized void revert(Object snapshot) {
        ((Snapshot)snapshot).apply();
    }

    private final class Snapshot {

        private final ServiceController<?> controller;
        private final Collection<DependencyImpl<?>> incomingDependencies;
        private final int upDemandedByCount;

        // take snapshot
        public Snapshot() {
            controller = Registration.this.controller;
            incomingDependencies = new ArrayList<DependencyImpl<?>>(Registration.this.incomingDependencies.size());
            incomingDependencies.addAll(Registration.this.incomingDependencies);
            upDemandedByCount = Registration.this.upDemandedByCount;
        }

        // revert ServiceController state to what it was when snapshot was taken; invoked on rollback
        public void apply() {
            Registration.this.controller = controller;
            Registration.this.upDemandedByCount = upDemandedByCount;
            Registration.this.incomingDependencies.clear();
            Registration.this.incomingDependencies.addAll(incomingDependencies);
        }
    }
}
