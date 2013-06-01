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

import org.jboss.msc._private.ServiceController.State;
import org.jboss.msc.service.Injector;
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
final class  SimpleDependency<T> extends TransactionalObject implements Dependency<T> {
    /**
     * The dependency registration.
     */
    private final Registration dependencyRegistration;
    /**
     * The incoming dependency service controller.
     */
    private ServiceController<?> dependentController;
    /**
     * Indicates if this dependency is required to be UP. If false, indicates that this dependency is required to be
     * down in order for the dependency to be satisfied.
     */
    private final boolean dependencyUp;
    /**
     * Indicates if the dependency should be demanded to be satisfied when service is attempting to start.
     */
    private final boolean propagateDemand;
    /**
     * Indicates if the dependency is required to be satisfied. If it is, the transaction will be invalidated if the
     * dependency is not satisfied.
     */
    private final boolean required;
    /**
     * List of injections.
     */
    private final Injector<? super T>[] injections;
    /**
     * Indicates if this dependency is satisfied.
     */
    private boolean dependencySatisfied;

    /**
     * Creates a simple dependency to {@code dependencyRegistration}.
     * 
     * @param injections             the dependency injections
     * @param dependencyRegistration the dependency registration
     * @param dependencyUp           {@code true} if the dependency is expected to be {@code UP}, {@code} false if the
     *                               dependency is expected to be {@code DOWN}.
     * @param propagateDemand        {@code true} if dependency should be demanded
     * @param required               {@code true} if dependency is required, i.e., every transaction that finishes
     *                               with the dependency not satisfied should be invalidated
     */
    SimpleDependency(final Injector<? super T>[] injections, final Registration dependencyRegistration, final boolean dependencyUp,
            final boolean propagateDemand, final boolean required) {
        this.injections = injections;
        this.dependencyRegistration = dependencyRegistration;
        this.dependencyUp = dependencyUp;
        this.propagateDemand = propagateDemand;
        this.required = required;
    }

    @Override
    public void setDependent(ServiceController<?> dependentController, Transaction transaction, ServiceContext context) {
        lockWrite(transaction, context);
        synchronized (this) {
            this.dependentController = dependentController;
            final ServiceController<?> dependencyController = dependencyRegistration.getController();
            // check if dependency is satisfied
            if ((dependencyUp && dependencyController != null && dependencyController.getState() == State.UP) ||
                    (!dependencyUp && (dependencyController == null || dependencyController.getState() != State.UP))) {
                dependencySatisfied = true;
                dependentController.dependencySatisfied(transaction, context);
            }
            dependencyRegistration.addIncomingDependency(transaction, context, this);
        }
    }

    @Override
    public synchronized void clearDependent(Transaction transaction, ServiceContext context) {
        dependencyRegistration.removeIncomingDependency(transaction, context, this);
    }

    @Override
    public Registration getDependencyRegistration() {
        return dependencyRegistration;
    }

    @Override
    public void performInjections() {
        assert dependencyRegistration.getController() != null;
        assert dependencyRegistration.getController().getValue() != null;
        for (Injector <? super T> injection: injections) {
            // TODO injection.setValue();
        }
    }

    /**
     * Demand this dependency to be satisfied.
     * 
     * @param transaction the active transaction
     */
    @Override
    public void demand(Transaction transaction, ServiceContext context) {
        if (propagateDemand) {
            dependencyRegistration.addDemand(transaction, context, dependencyUp);
        }
    }

    /**
     * Remove demand for this dependency to be satisfied.
     * 
     * @param transaction the active transaction
     */
    @Override
    public void undemand(Transaction transaction, ServiceContext context) {
        if (propagateDemand) {
            dependencyRegistration.removeDemand(transaction, context, dependencyUp);
        }
    }

    @Override
    public TaskController<?> dependencyAvailable(boolean dependencyUp, Transaction transaction, ServiceContext context) {
        lockWrite(transaction, context);
        synchronized (this) {
            if (this.dependencyUp == dependencyUp) {
                dependencySatisfied = true;
                return dependentController.dependencySatisfied(transaction, context);
            }
        }
        return null;
    }

    @Override
    public TaskController<?> dependencyUnavailable(Transaction transaction, ServiceContext context) {
        lockWrite(transaction, context);
        synchronized (this) {
            if (dependencySatisfied) {
                dependencySatisfied = false;
                return dependentController.dependencyUnsatisfied(transaction, context);
            }
        }
        return null;
    }

    @Override
    public void dependencyReplacementStarted(Transaction transaction) {
        // do nothing
    }

    @Override
    public void dependencyReplacementConcluded(Transaction transaction) {
        // do nothing
    }

    @Override
    synchronized Boolean takeSnapshot() {
        return dependencySatisfied;
    }

    @Override
    void validate(ReportableContext context) {
        if (required && !dependencySatisfied) {
            context.addProblem(Severity.ERROR, getRequirementProblem());
        }
    }

    @Override
    synchronized void revert(Object snapshot) {
        dependencySatisfied = (Boolean) snapshot;
    }

    private String getRequirementProblem() {
        assert required;
        final String dependencyState;
        final String anti = dependencyUp? "": "anti ";
        final ServiceController<?> dependencyController = dependencyRegistration.getController();
        if (dependencyController == null) {
            dependencyState = "is missing";
        } else {
            switch (dependencyController.getState()) {
                case DOWN:
                    dependencyState = "is not started";
                    break;
                case UP:
                    dependencyState = "is started";
                    break;
                case FAILED:
                    dependencyState = "has failed";
                    break;
                default:
                    throw new RuntimeException("Unexpected dependency state: " + dependencyController.getState());
            }
        }
        return MSCLogger.SERVICE.requiredDependency(dependentController.getPrimaryRegistration().getServiceName(), anti,
                dependencyRegistration.getServiceName(), dependencyState);
    }

}
