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
abstract class AbstractDependency<T> implements Dependency<T> {

    private static final byte REQUIRED_FLAG   = (byte)(1 << DependencyFlag.REQUIRED.ordinal());
    private static final byte UNREQUIRED_FLAG = (byte)(1 << DependencyFlag.UNREQUIRED.ordinal());
    private static final byte DEMANDED_FLAG   = (byte)(1 << DependencyFlag.DEMANDED.ordinal());
    private static final byte UNDEMANDED_FLAG = (byte)(1 << DependencyFlag.UNDEMANDED.ordinal());
    private static final byte PARENT_FLAG     = (byte)(1 << DependencyFlag.PARENT.ordinal());
    private final byte flags;

    protected AbstractDependency(final DependencyFlag... flags) {
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
    }

    protected final boolean hasRequiredFlag() {
        return allAreSet(flags, REQUIRED_FLAG);
    }

    protected final boolean hasUnrequiredFlag() {
        return allAreSet(flags, UNREQUIRED_FLAG);
    }

    protected final boolean hasDemandedFlag() {
        return allAreSet(flags, DEMANDED_FLAG);
    }

    protected final boolean hasUndemandedFlag() {
        return allAreSet(flags, UNDEMANDED_FLAG);
    }

    protected final boolean hasParentFlag() {
        return allAreSet(flags, PARENT_FLAG);
    }

    /**
     * Sets the dependency dependent, invoked during {@link dependentController} installation or {@link ParentDependency}
     * activation (when parent dependency is satisfied and installed).
     * 
     * @param dependent    dependent associated with this dependency
     * @param transaction  the active transaction
     * @param context      the service context
     */
    abstract void setDependent(Dependent dependent, Transaction transaction, ServiceContext context);

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
     * Notifies that dependency is now {@code UP}.
     * 
     * @param transaction   the active transaction
     * @param context       the service context
     */
    abstract TaskController<?> dependencyUp(Transaction transaction, ServiceContext context);

    /**
     * Notifies that dependency is now {@code DOWN}).
     *  
     * @param transaction    the active transaction
     * @param serviceContext the service context
     */
    abstract TaskController<?> dependencyDown(Transaction transaction, ServiceContext context);

    /**
     * Validates dependency state before active transaction commits.
     * 
     * @param controllerDependency the dependency controller, if available
     * @param context              context where all validation problems found will be added
     */
    abstract void validate(ServiceController<?> controllerDependency, ReportableContext context);
}