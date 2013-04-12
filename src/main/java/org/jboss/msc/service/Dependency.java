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

import org.jboss.msc.txn.Transaction;

/**
 * A dependency. This interface represents the dependency relationship from both the dependent
 * and dependency point of view.
 * 
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 * @author <a href="mailto:frainone@redhat.com">Flavia Rainone</a>
 *
 * @param <T>
 */
interface  Dependency<T> {

    /**
     * Sets the dependency dependent, invoked during {@link dependentController} installation.
     * 
     * @param transaction          the active transaction
     * @param dependentController  dependent associated with this dependency
     */
    public void setDependent(Transaction transaction, ServiceController<?> dependentController);

    /**
     * Return the dependency registration.
     * 
     * @return the dependency registration
     */
    public Registration getDependencyRegistration();

    /**
     * Perform injections.
     */
    public void performInjections();

    /**
     * Demand this dependency to be satisfied.
     * 
     * @param transaction the active transaction
     */
    public void demand(Transaction transaction);

    /**
     * Remove demand for this dependency to be satisfied.
     * 
     * @param transaction the active transaction
     */
    public void undemand(Transaction transaction);

    /**
     * Notifies that dependency state is changed.
     *  
     * @param transaction   the active transaction
     * @param dependencyUp  {@code true} if dependency is now {@link ServiceController.State#UP}; {@code false} if it is
     *                      now {@link ServiceController.State#DOWN}.
     */
    public void newDependencyState(Transaction transaction, boolean dependencyUp);

    /**
     * Notifies that dependency current service is about to be replaced by a different service.
     */
    public void dependencyReplacementStarted(Transaction transaction);

    /**
     * Notifies that dependency replacement is concluded and now the newly installed service is available.
     */
    public void dependencyReplacementConcluded(Transaction transaction);
}