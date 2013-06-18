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
     * The incoming dependency service controller.
     */
    private ServiceController<?> dependentController;
    /**
     * Indicates if the dependency should be demanded to be satisfied when service is attempting to start.
     */
    private final boolean propagateDemand;
    /**
     * Indicates if this dependency is satisfied.
     */
    private boolean dependencySatisfied;

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
    SimpleDependency(final Registration dependencyRegistration, final DependencyFlag demandFlag, Transaction transaction) {
        this.dependencyRegistration = dependencyRegistration;
        this.propagateDemand = demandFlag == null;
        if (demandFlag != null) {
            switch (demandFlag) {
                case DEMANDED:
                    dependencyRegistration.addDemand(transaction, transaction);
                    break;
                case UNDEMANDED:
                    break;
                default: 
                    throw new IllegalArgumentException("Unexpected demand flag: " + demandFlag);
            }
        }
    }

    public T get() {
        @SuppressWarnings("unchecked")
        ServiceController<T> dependencyController = (ServiceController<T>) dependencyRegistration.getController();
        return dependencyController == null? null: dependencyController.getValue();
    }

    @Override
    void setDependent(ServiceController<?> dependentController, Transaction transaction, ServiceContext context) {
        lockWrite(transaction, context);
        synchronized (this) {
            this.dependentController = dependentController;
            final ServiceController<?> dependencyController = dependencyRegistration.getController();
            // check if dependency is satisfied
            if (dependencyController != null && dependencyController.getState() == State.UP) {
                dependencySatisfied = true;
                dependentController.dependencySatisfied(transaction, context);
            }
            dependencyRegistration.addIncomingDependency(transaction, context, this);
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
    TaskController<?> dependencyAvailable(boolean dependencyUp, Transaction transaction, ServiceContext context) {
        lockWrite(transaction, context);
        final boolean satisfied;
        synchronized (this) {
            if (dependencyUp) {
                dependencySatisfied = true;
            }
            satisfied = dependencySatisfied;
        }
        if (satisfied) {
            return dependentController.dependencySatisfied(transaction, context);
        }
        return null;
    }

    @Override
    TaskController<?> dependencyUnavailable(Transaction transaction, ServiceContext context) {
        lockWrite(transaction, context);
        final boolean unsatisfied;
        synchronized (this) {
            if (dependencySatisfied) {
                dependencySatisfied = false;
                unsatisfied = true;
            } else {
                unsatisfied = false;
            }
        }
        if (unsatisfied) {
            return dependentController.dependencyUnsatisfied(transaction, context);
        }
        return null;
    }

    @Override
    void dependencyReplacementStarted(Transaction transaction) {
        // do nothing
    }

    @Override
    void dependencyReplacementConcluded(Transaction transaction) {
        // do nothing
    }

    @Override
    synchronized Boolean takeSnapshot() {
        return dependencySatisfied;
    }

    @Override
    void validate(ReportableContext context) {
        if (!dependencySatisfied) {
            context.addProblem(Severity.ERROR, getRequirementProblem());
        }
    }

    @Override
    synchronized void revert(Object snapshot) {
        dependencySatisfied = (Boolean) snapshot;
    }

    private String getRequirementProblem() {
        final String dependencyState;
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
        return MSCLogger.SERVICE.requiredDependency(dependentController.getPrimaryRegistration().getServiceName(),
                dependencyRegistration.getServiceName(), dependencyState);
    }

}
