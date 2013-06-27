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

import org.jboss.msc.service.DependencyFlag;
import org.jboss.msc.txn.Problem.Severity;
import org.jboss.msc.txn.ReportableContext;
import org.jboss.msc.txn.ServiceContext;
import org.jboss.msc.txn.TaskController;
import org.jboss.msc.txn.Transaction;

/**
 * Dependency implementation. 
 * 
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 * @author <a href="mailto:frainone@redhat.com">Flavia Rainone</a>
 *
 * @param <T>
 */
final class  SimpleDependency<T> extends AbstractDependency<T> {

    /**
     * The dependency registration.
     */
    private final Registration dependencyRegistration;
    /**
     * The incoming dependency.
     */
    private Dependent dependent;
    /**
     * Indicates if the dependency should be demanded to be satisfied when service is attempting to start.
     */
    private final boolean propagateDemand;

    /**
     * Creates a simple dependency to {@code dependencyRegistration}.
     * 
     * @param injections             the dependency injections
     * @param dependencyRegistration the dependency registration
     * @param demandFlag             if equal to {@link DependencyFlag#DEMANDED}, it indicates dependency should be
     *                               demanded right away; if equal to {@link DependencyFlag#UNDEMANDED}, it indicates
     *                               dependency should never be demanded; if {@code null}, it indicates dependency
     *                               should be demanded when {@link #demand(Transaction, ServiceContext) requested}.
     * @param transaction            the active transaction
     */
    SimpleDependency(final Registration dependencyRegistration, Transaction transaction, final DependencyFlag... flags) {
        super(flags);
        this.dependencyRegistration = dependencyRegistration;
        this.propagateDemand = !hasDemandedFlag() && !hasUndemandedFlag();
    }

    public T get() {
        @SuppressWarnings("unchecked")
        ServiceController<T> dependencyController = (ServiceController<T>) dependencyRegistration.getController();
        return dependencyController == null? null: dependencyController.getValue();
    }

    @Override
    void setDependent(Dependent dependent, Transaction transaction, ServiceContext context) {
        synchronized (this) {
            this.dependent = dependent;
            dependencyRegistration.addIncomingDependency(transaction, context, this);
            if (!propagateDemand) {
                if (hasDemandedFlag()) {
                    dependencyRegistration.addDemand(transaction, transaction);
                }
            }
        }
    }

    @Override
    void clearDependent(Transaction transaction, ServiceContext context) {
        dependencyRegistration.removeIncomingDependency(transaction, context, this);
    }

    @Override
    Registration getDependencyRegistration() {
        return dependencyRegistration;
    }

    /**
     * Demand this dependency to be satisfied.
     * 
     * @param transaction the active transaction
     */
    @Override
    void demand(Transaction transaction, ServiceContext context) {
        if (propagateDemand) {
            dependencyRegistration.addDemand(transaction, context);
        }
    }

    /**
     * Remove demand for this dependency to be satisfied.
     * 
     * @param transaction the active transaction
     */
    @Override
    void undemand(Transaction transaction, ServiceContext context) {
        if (propagateDemand) {
            dependencyRegistration.removeDemand(transaction, context);
        }
    }

    @Override
    TaskController<?> dependencyUp(Transaction transaction, ServiceContext context) {
        return dependent.dependencySatisfied(transaction, context);
    }

    @Override
    TaskController<?> dependencyDown(Transaction transaction, ServiceContext context) {
        return dependent.dependencyUnsatisfied(transaction, context);
    }

    @Override
    void validate(ServiceController<?> dependencyController, ReportableContext context) {
        if (dependencyController == null && !hasUnrequiredFlag()) {
            context.addProblem(Severity.ERROR, MSCLogger.SERVICE.requiredDependency(dependent.getServiceName(), dependencyRegistration.getServiceName()));
        }
    }
}