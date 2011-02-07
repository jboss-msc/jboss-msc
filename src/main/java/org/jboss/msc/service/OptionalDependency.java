/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.msc.service;

/**
 * An OptionalDependency.<br>This class establishes a transitive dependency relationship between the
 * dependent and the real dependency. The intermediation performed by this class adds the required optional
 * behavior to the dependency relation, by:
 * <ul>
 * <li> notifies the dependent that it is in the UP state when the real dependency is unresolved or uninstalled</li>
 * <li> once the real dependency is installed, if there is a demand previously added by the dependent, this dependency
 *      does not start forwarding the notifications to the dependent, meaning that the dependent won't even be aware
 *      that the dependency is down</li>
 * <li> waits for the dependency to be installed and the dependent to be inactive, so it can finally start forwarding
 *      notifications in both directions (from dependency to dependent and vice-versa)</li>
 * </ul>
 * 
 * @author <a href="mailto:flavia.rainone@jboss.com">Flavia Rainone</a>
 */
class OptionalDependency implements Dependency, Dependent {
    
    /**
     * One of the states of a dependency from the dependent point of view (i.e., based on notifications made by the
     * dependency).
     *
     */
    private static enum DependencyState {
        /**
         * The dependency is missing; means the last notification received by this {@code OptionalDependency} (as a
         * dependent of the real dependency) is {@link #dependencyUninstalled}.
         */
        MISSING,
        /**
         * The dependency is installed, but is not up. This is the initial state of the dependency. Also, if any
         * notification has been made by the dependency, this will be the dependency state if the last notification
         * received is {@link #dependencyInstalled}, {@link #dependencyDown}, or {@link #dependencyRetrying}.
         */
        INSTALLED,
        /**
         * The dependency failed; means the last notification received by this {@code OptionalDependency} (as a
         * dependent of the real dependency) is {@link #dependencyFailed}.
         */
        FAILED,
        /**
         * The dependency is up; means the last notification received by this {@code OptionalDependency} (as a
         * dependent of the real dependency) is {@link #dependencyUp}.
         */
        UP}

    /**
     * The real dependency.
     */
    private final Dependency optionalDependency;

    /**
     * The {@link #optionalDependency} state, based on notifications that {@code optionalDependency} made to this 
     * dependent.
     */
    private DependencyState dependencyState;

    /**
     * Indicates whether a transitive dependency is missing.
     */
    private boolean notifyTransitiveDependencyMissing;

    /**
     * The dependent on this optional dependency
     */
    private Dependent dependent;

    /**
     * Indicates if this dependency has been demanded by the dependent 
     */
    private boolean demandedByDependent;

    /**
     * Indicates if notification should take place
     */
    boolean forwardNotifications;

    /**
     * Keeps track of whether optionalDependency has been notified of a dependent started.
     * This field is useful for avoiding dependentStopped notifications that don't have a
     * corresponding previous dependentStarted notification.
     */
    private boolean dependentStartedNotified = false;

    /**
     * The container to which this dependency belongs.
     */
    private final ServiceContainerImpl container;

    OptionalDependency(ServiceContainerImpl container, Dependency optionalDependency) {
        this.optionalDependency = optionalDependency;
        dependencyState = DependencyState.INSTALLED;
        this.container = container;
    }

    @Override
    public void addDependent(Dependent dependent) {
        assert !lockHeld();
        assert !lockHeldByDependent(dependent);
        final boolean notifyDependent;
        final DependencyState currentDependencyState;
        optionalDependency.addDependent(this);
        synchronized (this) {
            if (this.dependent != null) {
                throw new IllegalStateException("Optional dependent is already set");
            }
            this.dependent = dependent;
            notifyDependent = forwardNotifications = dependencyState.compareTo(DependencyState.INSTALLED) >= 0;
            currentDependencyState = dependencyState;
        }
        if (notifyDependent) {
            switch (currentDependencyState) {
                case FAILED:
                    container.checkFailedDependencies(true);
                    break;
                case UP:
                    dependent.immediateDependencyUp();
            }
            if (notifyTransitiveDependencyMissing) {
                dependent.dependencyUninstalled();
            }
        }
        else {
            dependent.immediateDependencyUp();
        }
    }

    @Override
    public void removeDependent(Dependent dependent) {
        assert !lockHeld();
        assert !lockHeldByDependent(dependent);
        synchronized (this) {
            dependent = null;
            forwardNotifications = false;
        }
        optionalDependency.removeDependent(this);
    }

    @Override
    public void addDemand() {
        assert ! lockHeld();
        final boolean notifyOptionalDependency;
        synchronized (this) {
            demandedByDependent = true;
            notifyOptionalDependency = forwardNotifications;
        }
        if (notifyOptionalDependency) {
            optionalDependency.addDemand();
        }
    }

    @Override
    public void removeDemand() {
        assert ! lockHeld();
        final boolean startNotifying;
        final boolean notifyOptionalDependency;
        final DependencyState currentDependencyState;
        final boolean transitiveDependencyMissing;
        synchronized (this) {
            demandedByDependent = false;
            currentDependencyState = dependencyState;
            transitiveDependencyMissing = this.notifyTransitiveDependencyMissing;
            if (forwardNotifications) {
                notifyOptionalDependency = true;
                startNotifying = false;
            } else {
                notifyOptionalDependency = false;
                startNotifying = forwardNotifications = (dependencyState.compareTo(DependencyState.INSTALLED) >= 0);
            }
        }
        if (startNotifying) {
            switch (currentDependencyState) {
                case INSTALLED:
                    dependent.immediateDependencyDown();
                    break;
                case FAILED:
                    dependent.dependencyFailed();
                    break;
            }
            // the status of missing and failed dependencies is changed now
            // that this optional dep is connected with the dependent
            container.checkFailedDependencies(true);
            container.checkMissingDependencies();
            if (transitiveDependencyMissing) {
                dependent.dependencyUninstalled();
            }
        } else if (notifyOptionalDependency) {
            optionalDependency.removeDemand();
        }
    }

    @Override
    public void dependentStarted() {
        assert ! lockHeld();
        final boolean notifyOptionalDependency;
        synchronized (this) {
            dependentStartedNotified = notifyOptionalDependency = forwardNotifications;
        }
        if (notifyOptionalDependency) {
            optionalDependency.dependentStarted();
        }
    }

    @Override
    public void dependentStopped() {
        assert ! lockHeld();
        final boolean notifyOptionalDependency;
        synchronized (this) {
            // on some multi-thread scenarios, it can happen that forwardNotification become true as the result of a
            // removeDemand call that is performed before dependentStopped. In this case, dependentStartedNotified
            // will prevent us from notify the dependency of a dependentStopped without a corresponding
            // previous dependentStarted notification
            notifyOptionalDependency = forwardNotifications && dependentStartedNotified;
            dependentStartedNotified = false;
        }
        if (notifyOptionalDependency) {
            optionalDependency.dependentStopped();
        }
    }

    @Override
    public Object getValue() throws IllegalStateException {
        assert ! lockHeld();
        final boolean retrieveValue;
        synchronized (this) {
            retrieveValue = forwardNotifications;
        }
        return retrieveValue? optionalDependency.getValue(): null;
    }

    @Override
    public ServiceName getName() {
        return optionalDependency.getName();
    }

    @Override
    public void immediateDependencyInstalled() {
        assert ! lockHeld();
        final boolean notifyOptionalDependent;
        synchronized (this) {
            dependencyState = DependencyState.INSTALLED;
            forwardNotifications = notifyOptionalDependent = !demandedByDependent && dependent != null;
        }
        if (notifyOptionalDependent) {
            // need to update the dependent by telling it that the dependency is down
            dependent.immediateDependencyDown();
        }
    }

    @Override
    public void immediateDependencyUninstalled() {
        assert ! lockHeld();
        final boolean notificationsForwarded;
        final boolean demandNotified;
        synchronized (this) {
            notificationsForwarded = forwardNotifications;
            forwardNotifications = false;
            dependencyState = DependencyState.MISSING;
            demandNotified = demandedByDependent;
        }
        if (notificationsForwarded) {
            // now that the optional dependency is uninstalled, we enter automatically the up state
            dependent.immediateDependencyUp();
            if (demandNotified) {
                optionalDependency.removeDemand();
            }
        }
    }

    @Override
    public void immediateDependencyUp() {
        assert ! lockHeld();
        final boolean notifyOptionalDependent;
        synchronized (this) {
            dependencyState = DependencyState.UP;
            notifyOptionalDependent = forwardNotifications;
        }
        if (notifyOptionalDependent) {
            dependent.immediateDependencyUp();
        }
    }

    @Override
    public void immediateDependencyDown() {
        assert ! lockHeld();
        final boolean notifyOptionalDependent;
        synchronized (this) {
            dependencyState = DependencyState.INSTALLED;
            notifyOptionalDependent = forwardNotifications;
        }
        if (notifyOptionalDependent) {
            dependent.immediateDependencyDown();
        }
    }

    @Override
    public void dependencyFailed() {
        assert ! lockHeld();
        final boolean notifyOptionalDependent;
        synchronized (this) {
            dependencyState = DependencyState.FAILED;
            notifyOptionalDependent = forwardNotifications;
        }
        if (notifyOptionalDependent) {
            dependent.dependencyFailed();
        }
    }

    @Override
    public void dependencyFailureCleared() {
        assert ! lockHeld();
        final boolean notifyOptionalDependent;
        synchronized (this) {
            dependencyState = DependencyState.INSTALLED;
            notifyOptionalDependent = forwardNotifications;
        }
        if (notifyOptionalDependent) {
            dependent.dependencyFailureCleared();
        }
    }

    @Override
    public void dependencyInstalled() {
        assert ! lockHeld();
        final boolean notifyOptionalDependent;
        synchronized (this) {
            notifyOptionalDependent = forwardNotifications;
            notifyTransitiveDependencyMissing = false;
        }
        if (notifyOptionalDependent) {
            dependent.dependencyInstalled();
        }
    }

    @Override
    public void dependencyUninstalled() {
        assert ! lockHeld();
        final boolean notifyOptionalDependent;
        synchronized (this) {
            notifyOptionalDependent = forwardNotifications;
            notifyTransitiveDependencyMissing = !notifyOptionalDependent;
        }
        if (notifyOptionalDependent) {
            dependent.dependencyUninstalled();
        }
    }

    @Override
    public <T> T accept(Visitor<T> visitor) {
        assert ! lockHeld();
        final boolean acceptRealDependency;
        synchronized (this) {
            acceptRealDependency = forwardNotifications;
        }
        if (acceptRealDependency) {
            return optionalDependency.accept(visitor);
        }
        return visitor.visit(null);
    }

    /**
     * Determine whether the lock is currently held.
     *
     * @return {@code true} if the lock is held
     */
    boolean lockHeld() {
        return Thread.holdsLock(this);
    }

    /**
     * Determine whether the dependent lock is currently held.
     *
     * @return {@code true} if the lock is held
     */
    boolean lockHeldByDependent(Dependent dependent) {
        return Thread.holdsLock(dependent);
    }
}
