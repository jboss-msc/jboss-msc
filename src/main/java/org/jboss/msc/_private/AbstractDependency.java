/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013 Red Hat, Inc., and individual contributors
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

import org.jboss.msc.service.Dependency;
import org.jboss.msc.txn.ServiceContext;
import org.jboss.msc.txn.TaskController;
import org.jboss.msc.txn.Transaction;


/**
 * A dependency. This class represents the dependency relationship from both the dependent
 * and dependency point of view.
 * 
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 * @author <a href="mailto:frainone@redhat.com">Flavia Rainone</a>
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 *
 * @param <T>
 */
abstract class AbstractDependency<T> extends TransactionalObject implements Dependency<T> {

    /**
     * Sets the dependency dependent, invoked during {@link dependentController} installation.
     * 
     * @param dependent    dependent associated with this dependency
     * @param transaction  the active transaction
     * @param context      the service context
     */
    abstract void setDependent(ServiceController<?> dependent, Transaction transaction, ServiceContext context);

    /**
     * Clears the dependency dependent, invoked during {@link dependentController} removal.
     * 
     * @param transaction   the active transaction
     * @param context       the service context
     */
    abstract void clearDependent(Transaction transaction, ServiceContext context);

    /**
     * Returns the dependency registration.
     * 
     * @return the dependency registration
     */
    abstract Registration getDependencyRegistration();

    /**
     * Demands this dependency to be satisfied.
     * 
     * @param transaction the active transaction
     * @param context     the service context
     */
    abstract void demand(Transaction transaction, ServiceContext context);

    /**
     * Removes demand for this dependency to be satisfied.
     * 
     * @param transaction the active transaction
     * @param context     the service context
     */
    abstract void undemand(Transaction transaction, ServiceContext context);

    /**
     * Notifies that dependency is now available, either {@code UP}, or {@code DOWN}. A dependency in any other state is
     * considered unavailable.
     * 
     * @param dependencyUp  {@code true} if dependency is now {@link ServiceController.State#UP}; {@code false} if it is
     *                      now {@link ServiceController.State#DOWN}.
     * @param transaction   the active transaction
     * @param context       the service context
     */
    abstract TaskController<?> dependencyAvailable(boolean dependencyUp, Transaction transaction, ServiceContext context);

    /**
     * Notifies that dependency is now unavailable (it is not {@code UP}, nor {@code DOWN}). Every unavailable
     * dependency is considered unsatisfied.
     *  
     * @param transaction    the active transaction
     * @param serviceContext the service context
     */
    abstract TaskController<?> dependencyUnavailable(Transaction transaction, ServiceContext context);
}