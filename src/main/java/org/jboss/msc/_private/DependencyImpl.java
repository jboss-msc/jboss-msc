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

import static org.jboss.msc._private.Bits.allAreSet;
import static org.jboss.msc._private.MSCLogger.SERVICE;

import org.jboss.msc.service.Dependency;
import org.jboss.msc.service.DependencyFlag;
import org.jboss.msc.txn.ReportableContext;
import org.jboss.msc.txn.ServiceContext;
import org.jboss.msc.txn.TaskController;
import org.jboss.msc.txn.Transaction;
import org.jboss.msc.txn.Problem.Severity;


/**
 * Dependency implementation.
 * 
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 * @author <a href="mailto:frainone@redhat.com">Flavia Rainone</a>
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 *
 * @param <T>
 */
class DependencyImpl<T> implements Dependency<T> {

    private static final byte REQUIRED_FLAG   = (byte)(1 << DependencyFlag.REQUIRED.ordinal());
    private static final byte UNREQUIRED_FLAG = (byte)(1 << DependencyFlag.UNREQUIRED.ordinal());
    private static final byte DEMANDED_FLAG   = (byte)(1 << DependencyFlag.DEMANDED.ordinal());
    private static final byte UNDEMANDED_FLAG = (byte)(1 << DependencyFlag.UNDEMANDED.ordinal());
    private static final byte PARENT_FLAG     = (byte)(1 << DependencyFlag.PARENT.ordinal());
    
    /**
     * Dependency flags.
     */
    private final byte flags;
    /**
     * The dependency registration.
     */
    private final Registration dependencyRegistration;
    /**
     * Indicates if the dependency should be demanded to be satisfied when service is attempting to start.
     */
    private final boolean propagateDemand;
    /**
     * The incoming dependency.
     */
    private Dependent dependent;

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
    protected DependencyImpl(final Registration dependencyRegistration, final Transaction transaction, final DependencyFlag... flags) {
        byte translatedFlags = 0;
        for (final DependencyFlag flag : flags) {
            if (flag != null) {
                translatedFlags |= (1 << flag.ordinal());
            }
        }
        if (allAreSet(translatedFlags, UNDEMANDED_FLAG | DEMANDED_FLAG)) {
            throw SERVICE.mutuallyExclusiveFlags(DependencyFlag.DEMANDED.toString(), DependencyFlag.UNDEMANDED.toString());
        }
        if (allAreSet(translatedFlags, REQUIRED_FLAG | UNREQUIRED_FLAG)) {
            throw SERVICE.mutuallyExclusiveFlags(DependencyFlag.REQUIRED.toString(), DependencyFlag.UNREQUIRED.toString());
        }
        if (allAreSet(translatedFlags, PARENT_FLAG | UNREQUIRED_FLAG)) {
            throw SERVICE.mutuallyExclusiveFlags(DependencyFlag.PARENT.toString(), DependencyFlag.UNREQUIRED.toString());
        }
        this.flags = translatedFlags;
        this.dependencyRegistration = dependencyRegistration;
        this.propagateDemand = !hasDemandedFlag() && !hasUndemandedFlag();
    }

    final boolean hasRequiredFlag() {
        return allAreSet(flags, REQUIRED_FLAG);
    }

    final boolean hasUnrequiredFlag() {
        return allAreSet(flags, UNREQUIRED_FLAG);
    }

    final boolean hasDemandedFlag() {
        return allAreSet(flags, DEMANDED_FLAG);
    }

    final boolean hasUndemandedFlag() {
        return allAreSet(flags, UNDEMANDED_FLAG);
    }

    final boolean hasParentFlag() {
        return allAreSet(flags, PARENT_FLAG);
    }

    public T get() {
        @SuppressWarnings("unchecked")
        ServiceController<T> dependencyController = (ServiceController<T>) dependencyRegistration.getController();
        return dependencyController == null? null: dependencyController.getValue();
    }

    /**
     * Sets the dependency dependent, invoked during {@link dependentController} installation or {@link ParentDependency}
     * activation (when parent dependency is satisfied and installed).
     * 
     * @param dependent    dependent associated with this dependency
     * @param transaction  the active transaction
     * @param context      the service context
     */
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

    /**
     * Clears the dependency dependent, invoked during {@link dependentController} removal.
     * 
     * @param transaction   the active transaction
     * @param context       the service context
     */
    void clearDependent(Transaction transaction, ServiceContext context) {
        dependencyRegistration.removeIncomingDependency(transaction, context, this);
    }

    /**
     * Returns the dependency registration.
     * 
     * @return the dependency registration
     */
    Registration getDependencyRegistration() {
        return dependencyRegistration;
    }

    /**
     * Demands this dependency to be satisfied.
     * 
     * @param transaction the active transaction
     * @param context     the service context
     */
    void demand(Transaction transaction, ServiceContext context) {
        if (propagateDemand) {
            dependencyRegistration.addDemand(transaction, context);
        }
    }

    /**
     * Removes demand for this dependency to be satisfied.
     * 
     * @param transaction the active transaction
     * @param context     the service context
     */
    void undemand(Transaction transaction, ServiceContext context) {
        if (propagateDemand) {
            dependencyRegistration.removeDemand(transaction, context);
        }
    }

    /**
     * Notifies that dependency is now {@code UP}.
     * 
     * @param transaction   the active transaction
     * @param context       the service context
     */
    TaskController<?> dependencyUp(Transaction transaction, ServiceContext context) {
        return dependent.dependencySatisfied(transaction, context);
    }

    /**
     * Notifies that dependency is now {@code DOWN}).
     *  
     * @param transaction    the active transaction
     * @param serviceContext the service context
     */
    TaskController<?> dependencyDown(Transaction transaction, ServiceContext context) {
        return dependent.dependencyUnsatisfied(transaction, context);
    }

    /**
     * Validates dependency state before active transaction commits.
     * 
     * @param controllerDependency the dependency controller, if available
     * @param context              context where all validation problems found will be added
     */
    void validate(ServiceController<?> dependencyController, ReportableContext context) {
        if (dependencyController == null && !hasUnrequiredFlag()) {
            context.addProblem(Severity.ERROR, MSCLogger.SERVICE.requiredDependency(dependent.getServiceName(), dependencyRegistration.getServiceName()));
        }
    }

}