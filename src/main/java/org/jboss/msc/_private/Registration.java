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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import org.jboss.msc.service.ServiceName;
import org.jboss.msc.txn.ServiceContext;
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
    private final Set<AbstractDependency<?>> incomingDependencies = new CopyOnWriteArraySet<AbstractDependency<?>>();
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

    void addIncomingDependency(final Transaction transaction, final ServiceContext context, final AbstractDependency<?> dependency) {
        lockWrite(transaction, context);
        synchronized (this) {
            incomingDependencies.add(dependency);
        }
    }

    void removeIncomingDependency(final Transaction transaction, final ServiceContext context, final AbstractDependency<?> dependency) {
        lockWrite(transaction, context);
        assert incomingDependencies.contains(dependency);
        incomingDependencies.remove(dependency);
    }

    Set<AbstractDependency<?>> getIncomingDependencies() {
        return Collections.unmodifiableSet(incomingDependencies);
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
    protected synchronized void revert(Object snapshot) {
        ((Snapshot)snapshot).apply();
    }

    private final class Snapshot {

        private final ServiceController<?> controller;
        private final Collection<AbstractDependency<?>> incomingDependencies;
        private final int upDemandedByCount;

        // take snapshot
        public Snapshot() {
            controller = Registration.this.controller;
            incomingDependencies = new ArrayList<AbstractDependency<?>>(Registration.this.incomingDependencies.size());
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