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
package org.jboss.msc.service;

import org.jboss.msc.service.ServiceController.State;
import org.jboss.msc.txn.Transaction;
import org.jboss.msc.value.WritableValue;

/**
 * Dependency implementation. 
 * 
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 * @author <a href="mailto:frainone@redhat.com">Flavia Rainone</a>
 *
 * @param <T>
 */
final class  SimpleDependency<T> implements Dependency<T> {
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
     * List of injections.
     */
    private final WritableValue<? super T>[] injections;

    @SuppressWarnings("unchecked")
    static final WritableValue<Object>[] NO_INJECTIONS = new WritableValue[0];

    SimpleDependency(final WritableValue<? super T>[] injections, final Registration dependencyRegistration, final boolean dependencyUp, final boolean propagateDemand) {
        this.injections = injections;
        this.dependencyRegistration = dependencyRegistration;
        this.dependencyUp = dependencyUp;
        this.propagateDemand = propagateDemand;
    }

    public synchronized void setDependent(Transaction transaction, ServiceController<?> dependentController) {
        this.dependentController = dependentController;
        if (!isDependencySatisfied()) {
            dependentController.dependencyUnsatisfied(transaction);
        }
        dependencyRegistration.addIncomingDependency(transaction,  this);
    }

    private final boolean isDependencySatisfied() {
        final ServiceController<?> dependencyController = dependencyRegistration.getController();
        return (dependencyUp && dependencyController != null && dependencyController.getState() == State.UP) ||
                (!dependencyUp && (dependencyController == null || dependencyController.getState() != State.UP));
    }

    public Registration getDependencyRegistration() {
        return dependencyRegistration;
    }

    public void performInjections() {
        assert dependencyRegistration.getController() != null;
        assert dependencyRegistration.getController().getValue() != null;
        for (WritableValue <? super T> injection: injections) {
            // TODO injection.setValue();
        }
    }

    /**
     * Demand this dependency to be satisfied.
     * 
     * @param transaction the active transaction
     */
    public void demand(Transaction transaction) {
        if (propagateDemand) {
            dependencyRegistration.addDemand(transaction, dependencyUp);
        }
    }

    /**
     * Remove demand for this dependency to be satisfied.
     * 
     * @param transaction the active transaction
     */
    public void undemand(Transaction transaction) {
        if (propagateDemand) {
            dependencyRegistration.removeDemand(transaction, dependencyUp);
        }
    }

    /**
     * Notifies that dependency state is changed.
     *  
     * @param transaction   the active transaction
     * @param dependencyUp  {@code true} if dependency is now {@link ServiceController.State#UP}; {@code false} if it is
     *                      now {@link ServiceController.State#DOWN}.
     */
    public synchronized void newDependencyState(Transaction transaction, boolean dependencyUp) {
        if (this.dependencyUp == dependencyUp) {
            dependentController.dependencySatisfied(transaction);
        } else {
            dependentController.dependencyUnsatisfied(transaction);
        }
    }

    @Override
    public void dependencyReplacementStarted(Transaction transaction) {
        // do nothing
    }

    @Override
    public void dependencyReplacementConcluded(Transaction transaction) {
        // do nothing
    }
}
