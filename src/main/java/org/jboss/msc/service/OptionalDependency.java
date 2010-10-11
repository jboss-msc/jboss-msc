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
 * <li> flagging itself as a dependency in the UP state when the real dependency is unresolved or uninstalled</li>
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
     * The real dependency.
     */
    private final Dependency optionalDependency;

    /**
     * Is dependency up
     */
    private boolean dependencyUp;

    /**
     * The dependent on this optional dependency
     */
    private Dependent dependent;

    /**
     * Indicates if this dependency has been demanded by the dependent 
     */
    private boolean demandedByDependent;

    /**
     * Indicates if the real optional dependency is installed
     */
    private boolean optionalDependencyInstalled;

    /**
     * Indicates if notification should take place
     */
    boolean notify;

    OptionalDependency(Dependency optionalDependency) {
        this.optionalDependency = optionalDependency;
        optionalDependencyInstalled = true;
        optionalDependency.addDependent(this);
    }

    @Override
    public void addDependent(Dependent dependent) {
        assert !lockHeld();
        assert !lockHeldByDependent(dependent);
        final boolean isDependencyUp;
        synchronized (this) {
            if (this.dependent != null) {
                throw new IllegalStateException("Optional dependent is already set");
            }
            this.dependent = dependent;
            isDependencyUp = isUp();
        }
        if (isDependencyUp) {
            dependent.dependencyInstalled();
            dependent.dependencyUp();
        }
    }

    @Override
    public void removeDependent(Dependent dependent) {
        assert !lockHeld();
        assert !lockHeldByDependent(dependent);
        final boolean notifyOptionalDependency;
        synchronized (this) {
            notifyOptionalDependency = notify;
            dependent = null;
        }
        if (notifyOptionalDependency) {
            optionalDependency.removeDependent(this);
        }
    }

    @Override
    public void addDemand() {
        assert ! lockHeld();
        final boolean notifyOptionalDependency;
        synchronized (this) {
            demandedByDependent = true;
            notifyOptionalDependency = notify;
        }
        if (notifyOptionalDependency) {
            optionalDependency.addDemand();
        }
    }

    @Override
    public void removeDemand() {
        assert ! lockHeld();
        final boolean isDependencyUp;
        final boolean startNotifying;
        final boolean notifyOptionalDependency;
        synchronized (this) {
            demandedByDependent = false;
            if (notify) {
                notifyOptionalDependency = true;
                startNotifying = false;
                isDependencyUp = false; // this boolean won't be read, so no need to calculate it
            } else {
                notifyOptionalDependency = false;
                startNotifying = notify = optionalDependencyInstalled;
                isDependencyUp = isUp();
            }
        }
        if (startNotifying && !isDependencyUp) {
            dependent.dependencyDown();
        } else if (notifyOptionalDependency) {
            optionalDependency.removeDemand();
        }
    }

    @Override
    public void dependentStarted() {
        assert ! lockHeld();
        final boolean notifyOptionalDependency;
        synchronized (this) {
            notifyOptionalDependency = notify;
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
            notifyOptionalDependency = notify;
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
            retrieveValue = notify;
        }
        return retrieveValue? optionalDependency.getValue(): null;
    }

    @Override
    public void dependencyInstalled() {
        assert ! lockHeld();
        final boolean notifyOptionalDependent;
        synchronized (this) {
            optionalDependencyInstalled = true;
            notify = notifyOptionalDependent = !demandedByDependent;
        }
        if (notifyOptionalDependent) {
            // dependencyInstalled has already been invoked on dependent.What we need to do is update
            // the dependent by telling it that the dependency is down
            dependent.dependencyDown();
        }
    }

    @Override
    public void dependencyUninstalled() {
        assert ! lockHeld();
        final boolean notifyOptionalDependent;
        synchronized (this) {
            notifyOptionalDependent = notify && !dependencyUp;
            notify = optionalDependencyInstalled = false;
        }
        if (notifyOptionalDependent) {
            // now that the optional dependency is uninstalled, we enter automatically the up state
            dependent.dependencyUp();
        }
    }

    @Override
    public void dependencyUp() {
        assert ! lockHeld();
        final boolean notifyOptionalDependent;
        synchronized (this) {
            dependencyUp = true;
            notifyOptionalDependent = notify;
        }
        if (notifyOptionalDependent) {
            dependent.dependencyUp();
        }
    }

    @Override
    public void dependencyDown() {
        assert ! lockHeld();
        final boolean notifyOptionalDependent;
        synchronized (this) {
            dependencyUp = false;
            notifyOptionalDependent = notify;
        }
        if (notifyOptionalDependent) {
            dependent.dependencyDown();
        }
    }

    @Override
    public void dependencyFailed() {
        assert ! lockHeld();
        final boolean notifyOptionalDependent;
        synchronized (this) {
            notifyOptionalDependent = notify;
        }
        if (notifyOptionalDependent) {
            dependent.dependencyFailed();
        }
    }

    @Override
    public void dependencyRetrying() {
        assert ! lockHeld();
        final boolean notifyOptionalDependent;
        synchronized (this) {
            notifyOptionalDependent = notify;
        }
        if (notifyOptionalDependent) {
            dependent.dependencyRetrying();
        }
    }

    @Override
    public void transitiveDependencyInstalled() {
        assert ! lockHeld();
        final boolean notifyOptionalDependent;
        synchronized (this) {
            notifyOptionalDependent = notify;
        }
        if (notifyOptionalDependent) {
            dependent.transitiveDependencyInstalled();
        }
    }

    @Override
    public void transitiveDependencyUninstalled() {
        assert ! lockHeld();
        final boolean notifyOptionalDependent;
        synchronized (this) {
            notifyOptionalDependent = notify;
        }
        if (notifyOptionalDependent) {
            dependent.transitiveDependencyUninstalled();
        }
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

    private boolean isUp() {
        assert lockHeld();
        return !optionalDependencyInstalled || dependencyUp;
    }
}
