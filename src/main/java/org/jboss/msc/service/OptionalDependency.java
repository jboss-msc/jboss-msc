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

import static java.lang.Thread.holdsLock;

/**
 * An OptionalDependency.<br>This class establishes a transitive dependency relationship between the
 * dependent and the real dependency. The intermediation performed by this class adds the required optional
 * behavior to the dependency relation, by:
 * <ul>
 * <li> notifies the dependent that it is in the UP state when the real dependency is unresolved or unavailable</li>
 * <li> once the real dependency is available, if there is a demand previously added by the dependent, this dependency
 *      does not start forwarding the notifications to the dependent, meaning that the dependent won't even be aware
 *      that the dependency is down</li>
 * <li> waits for the dependency to be available and the dependent to be inactive, so it can finally start forwarding
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
         * The dependency is unavailable; means the last notification received by this {@code OptionalDependency} (as a
         * dependent of the real dependency) is {@link #dependencyUnavailable}.
         */
        UNAVAILABLE,
        /**
         * The dependency is available, but is not up. This is the initial state of the dependency. Also, if any
         * notification has been made by the dependency, this will be the dependency state if the last notification
         * received is {@link #dependencyInstalled}, {@link #dependencyDown}, or {@link #dependencyRetrying}.
         */
        AVAILABLE,
        /**
         * The dependency is up; means the last notification received by this {@code OptionalDependency} (as a
         * dependent of the real dependency) is {@link #dependencyUp}.
         */
        UP}

    /**
     * The actual dependency.
     */
    private final Dependency optionalDependency;

    /**
     * The {@link #optionalDependency} state, based on notifications that {@code optionalDependency} made to this 
     * dependent.
     */
    private DependencyState dependencyState;

    /**
     * Indicates if optional dependency has a transitive dependency unavailable.
     */
    private boolean transitiveDependencyUnavailable = false;

    /**
     * Indicates if optionalDependency notified a dependency failure.
     */
    private boolean dependencyFailed = false;

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

    OptionalDependency(Dependency optionalDependency) {
        this.optionalDependency = optionalDependency;
        dependencyState = DependencyState.AVAILABLE;
    }

    @Override
    public void addDependent(Dependent dependent) {
        assert !holdsLock(this);
        assert !holdsLock(dependent);
        final boolean notifyDependent;
        final DependencyState depState;
        final boolean transDepUnavailable;
        final boolean depFailed;
        optionalDependency.addDependent(this);
        synchronized (this) {
            if (this.dependent != null) {
                throw new IllegalStateException("Optional dependent is already set");
            }
            this.dependent = dependent;
            notifyDependent = forwardNotifications = dependencyState.compareTo(DependencyState.AVAILABLE) >= 0;
            depState = dependencyState;
            transDepUnavailable = transitiveDependencyUnavailable;
            depFailed = dependencyFailed;
        }
        if (notifyDependent) {
            if (depState == DependencyState.UP) {
                dependent.immediateDependencyUp();
            }
            if (transDepUnavailable) {
                dependent.transitiveDependencyUnavailable();
            }
            if (depFailed) {
                dependent.dependencyFailed();
            }
        } else {
            dependent.immediateDependencyUp();
        }
    }

    @Override
    public void removeDependent(Dependent dependent) {
        assert !holdsLock(this);
        assert !holdsLock(dependent);
        synchronized (this) {
            this.dependent = null;
            forwardNotifications = false;
        }
        optionalDependency.removeDependent(this);
    }

    @Override
    public void addDemand() {
        assert !holdsLock(this);
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
        assert !holdsLock(this);
        final boolean startNotifying;
        final boolean notifyOptionalDependency;
        final DependencyState depState;
        final boolean transDepUnavailable;
        final boolean depFailed;
        final Dependent dependent;
        synchronized (this) {
            dependent = this.dependent;
            demandedByDependent = false;
            depState = dependencyState;
            transDepUnavailable = transitiveDependencyUnavailable;
            depFailed = dependencyFailed;
            if (forwardNotifications) {
                notifyOptionalDependency = true;
                startNotifying = false;
            } else {
                notifyOptionalDependency = false;
                startNotifying = forwardNotifications = dependencyState.compareTo(DependencyState.AVAILABLE) >= 0 && dependent != null;
            }
        }
        if (startNotifying) {
            if (depState == DependencyState.AVAILABLE) {
                dependent.immediateDependencyDown();
            }
            // the status of unavailable and failed dependencies is changed now
            // given this optional dep is connected with the dependent
            if (transDepUnavailable) {
                dependent.transitiveDependencyUnavailable();
            }
            if (depFailed) {
                dependent.dependencyFailed();
            }
        } else if (notifyOptionalDependency) {
            optionalDependency.removeDemand();
        }
    }

    @Override
    public void dependentStarted() {
        assert !holdsLock(this);
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
        assert !holdsLock(this);
        final boolean notifyOptionalDependency;
        synchronized (this) {
            // on some multi-threaded scenarios, it can happen that forwardNotification become true as the result of a
            // removeDemand call that is performed before dependentStopped. In this case, dependentStartedNotified
            // will prevent us from notifying the dependency of a dependentStopped without a corresponding
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
        assert !holdsLock(this);
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
    public void immediateDependencyAvailable(ServiceName dependencyName) {
        assert !holdsLock(this);
        final boolean notifyOptionalDependent;
        final boolean depFailed;
        final boolean startNotifying;
        final Dependent dependent;
        synchronized (this) {
            dependent = this.dependent;
            depFailed = dependencyFailed;
            startNotifying = !forwardNotifications;
            dependencyState = DependencyState.AVAILABLE;
            forwardNotifications = notifyOptionalDependent = !demandedByDependent && dependent != null;
        }
        if (notifyOptionalDependent) {
            // need to update the dependent by telling it the dependency is down
            dependent.immediateDependencyDown();
            if (startNotifying && depFailed) {
                dependent.dependencyFailed();
            }
        }
    }

    @Override
    public void immediateDependencyUnavailable(ServiceName dependencyName) {
        assert !holdsLock(this);
        final boolean notificationsForwarded;
        final boolean demandNotified;
        final DependencyState depState;
        final boolean transitiveDepUnavailable;
        final boolean depFailed;
        final Dependent dependent;
        synchronized (this) {
            dependent = this.dependent;
            depState = dependencyState;
            transitiveDepUnavailable = transitiveDependencyUnavailable;
            depFailed = dependencyFailed;
            notificationsForwarded = forwardNotifications && dependent != null;
            forwardNotifications = false;
            dependencyState = DependencyState.UNAVAILABLE;
            demandNotified = demandedByDependent;
        }
        if (notificationsForwarded) {
            // now that the optional dependency is unavailable, we enter automatically the up state
            if (depState == DependencyState.AVAILABLE) {
                dependent.immediateDependencyUp();
            }
            if (depFailed) {
                dependent.dependencyFailureCleared();
            }
            if (transitiveDepUnavailable) {
                dependent.transitiveDependencyAvailable();
            }
            if (demandNotified) {
                optionalDependency.removeDemand();
            }
        }
    }

    @Override
    public void immediateDependencyUp() {
        assert !holdsLock(this);
        final boolean notifyOptionalDependent;
        final Dependent dependent;
        synchronized (this) {
            dependent = this.dependent;
            dependencyState = DependencyState.UP;
            notifyOptionalDependent = forwardNotifications;
        }
        if (notifyOptionalDependent) {
            dependent.immediateDependencyUp();
        }
    }

    @Override
    public void immediateDependencyDown() {
        assert !holdsLock(this);
        final boolean notifyOptionalDependent;
        final Dependent dependent;
        synchronized (this) {
            dependent = this.dependent;
            dependencyState = DependencyState.AVAILABLE;
            notifyOptionalDependent = forwardNotifications;
        }
        if (notifyOptionalDependent) {
            dependent.immediateDependencyDown();
        }
    }

    @Override
    public void dependencyFailed() {
        assert !holdsLock(this);
        final boolean notifyOptionalDependent;
        final Dependent dependent;
        synchronized (this) {
            dependent = this.dependent;
            notifyOptionalDependent = forwardNotifications;
            dependencyFailed = true;
        }
        if (notifyOptionalDependent) {
            dependent.dependencyFailed();
        }
    }

    @Override
    public void dependencyFailureCleared() {
        assert !holdsLock(this);
        final boolean notifyOptionalDependent;
        final Dependent dependent;
        synchronized (this) {
            dependent = this.dependent;
            notifyOptionalDependent = forwardNotifications;
            dependencyFailed = false;
        }
        if (notifyOptionalDependent) {
            dependent.dependencyFailureCleared();
        }
    }

    @Override
    public void transitiveDependencyAvailable() {
        assert !holdsLock(this);
        final boolean notifyOptionalDependent;
        final Dependent dependent;
        synchronized (this) {
            notifyOptionalDependent = forwardNotifications;
            transitiveDependencyUnavailable = false;
            dependent = this.dependent;
        }
        if (notifyOptionalDependent) {
            dependent.transitiveDependencyAvailable();
        }
    }

    @Override
    public ServiceControllerImpl<?> getController() {
        final Dependent dependent;
        synchronized (this) {
            dependent = this.dependent;
        }
        return dependent != null ? dependent.getController() : null; // [MSC-145] optional dependencies may return null
    }

    @Override
    public void transitiveDependencyUnavailable() {
        assert !holdsLock(this);
        final boolean notifyOptionalDependent;
        final Dependent dependent;
        synchronized (this) {
            dependent = this.dependent;
            notifyOptionalDependent = forwardNotifications;
            transitiveDependencyUnavailable = true;
        }
        if (notifyOptionalDependent) {
            dependent.transitiveDependencyUnavailable();
        }
    }
}
