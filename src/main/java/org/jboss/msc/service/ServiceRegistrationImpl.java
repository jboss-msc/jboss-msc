/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2022, Red Hat, Inc., and individual contributors
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

import java.util.Set;

/**
 * A single service registration.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
final class ServiceRegistrationImpl extends Lockable implements Dependency {

    /**
     * The name of this registration.
     */
    private final ServiceName name;
    /**
     * The set of dependents on this registration.
     */
    private final Set<Dependent> dependents = new IdentityHashSet<>();
    /**
     * The dependency value provided by this registration.
     */
    private final ReadableValueImpl value = new ReadableValueImpl(this);

    // Mutable properties

    /**
     * The current instance.
     */
    private volatile ServiceControllerImpl<?> instance;
    /**
     * The injector providing value.
     */
    private volatile WritableValueImpl injector;
    /**
     * The number of dependent instances which place a demand-to-start on this registration.  If this value is >0,
     * propagate a demand to the instance, if any.
     */
    private int demandedByCount;
    /**
     * The number of started dependent instances.
     */
    private int dependentsStartedCount;
    /**
     * The number of installations being currently installed into this registration.
     */
    private int pendingInstallation;
    /**
     * Indicates whether this registration was removed.
     */
    private boolean removed;

    ServiceRegistrationImpl(final ServiceName name) {
        this.name = name;
    }

    @Override
    public Lockable getLock() {
        return this;
    }

    Set<Dependent> getDependents() {
        return dependents;
    }

    boolean addPendingInstallation() {
        assert isWriteLocked();
        if (removed) {
            // failed
            return false;
        } else {
            // success
            pendingInstallation++;
            return true;
        }
    }

    @Override
    public void addDependent(final Dependent dependent) {
        assert isWriteLocked();
        pendingInstallation--;
        if (dependents.contains(dependent)) {
            throw new IllegalStateException("Dependent already exists on this registration");
        }
        dependents.add(dependent);
        if (instance == null) {
            dependent.dependencyUnavailable();
            return;
        }
        synchronized (instance) {
            if (!instance.isInstallationCommitted()) {
                dependent.dependencyUnavailable();
                return;
            }
            instance.newDependent(dependent);
        }
    }

    @Override
    public boolean removeDependent(final Dependent dependent) {
        assert isWriteLocked();
        dependents.remove(dependent);
        removed = instance == null && dependents.size() == 0 && pendingInstallation == 0;
        return removed;
    }

    void set(final ServiceControllerImpl<?> newInstance, final WritableValueImpl newInjector) throws DuplicateServiceException {
        assert newInstance != null;
        assert isWriteLocked();
        pendingInstallation--;
        if (instance != null) {
            throw new DuplicateServiceException(String.format("Service %s is already registered", name.getCanonicalName()));
        }
        instance = newInstance;
        injector = newInjector;
        if (demandedByCount > 0) instance.addDemands(demandedByCount);
        if (dependentsStartedCount > 0) instance.dependentsStarted(dependentsStartedCount);
    }

    boolean clear(final ServiceControllerImpl<?> oldInstance) {
        assert oldInstance != null;
        assert isWriteLocked();
        if (instance == oldInstance) {
            instance = null;
            injector = null;
            removed = dependents.size() == 0 && pendingInstallation == 0;
        }
        return removed;
    }

    ReadableValueImpl getReadableValue() {
        return value;
    }

    @Override
    public Object getValue() throws IllegalStateException {
        final WritableValueImpl injector = this.injector;
        if (injector != null) return injector.getValue();
        final ServiceControllerImpl instance = this.instance;
        if (instance != null) return instance.getValue();
        throw new IllegalStateException("Service is not installed");
    }

    @Override
    public ServiceName getName() {
        return name;
    }

    @Override
    public ServiceControllerImpl<?> getDependencyController() {
        synchronized (this) {
            return instance;
        }
    }

    @Override
    public void dependentStarted() {
        assert isWriteLocked();
        dependentsStartedCount++;
        if (instance != null) instance.dependentStarted();
    }

    @Override
    public void dependentStopped() {
        assert isWriteLocked();
        dependentsStartedCount--;
        if (instance != null) instance.dependentStopped();
    }

    @Override
    public void addDemand() {
        assert isWriteLocked();
        demandedByCount++;
        if (instance != null) instance.addDemand();
    }

    @Override
    public void removeDemand() {
        assert isWriteLocked();
        demandedByCount--;
        if (instance != null) instance.removeDemand();
    }

}
