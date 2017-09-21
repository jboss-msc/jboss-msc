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
 * A single service registration.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
final class ServiceRegistrationImpl extends Lockable implements Dependency {

    /**
     * The service container which contains this registration.
     */
    private final ServiceContainerImpl container;
    /**
     * The name of this registration.
     */
    private final ServiceName name;
    /**
     * The set of dependents on this registration.
     */
    private final IdentityHashSet<Dependent> dependents = new IdentityHashSet<Dependent>(0);

    // Mutable properties

    /**
     * The current instance.
     */
    private ServiceControllerImpl<?> instance;
    /**
     * The number of dependent instances which place a demand-to-start on this registration.  If this value is >0,
     * propagate a demand to the instance, if any.
     */
    private int demandedByCount;
    /**
     * The number of started dependent instances.
     */
    private int dependentsStartedCount;

    ServiceRegistrationImpl(final ServiceContainerImpl container, final ServiceName name) {
        this.container = container;
        this.name = name;
    }

    @Override
    public Lockable getLock() {
        return this;
    }

    IdentityHashSet<Dependent> getDependents() {
        return dependents;
    }

    @Override
    public void addDependent(final Dependent dependent) {
        assert isWriteLocked();
        if (dependents.contains(dependent)) {
            throw new IllegalStateException("Dependent already exists on this registration");
        }
        dependents.add(dependent);
        if (instance == null) {
            dependent.dependencyUnavailable(name);
            return;
        }
        synchronized (instance) {
            if (!instance.isInstallationCommitted()) {
                dependent.dependencyUnavailable(name);
                return;
            }
            instance.newDependent(name, dependent);
        }
    }

    @Override
    public void removeDependent(final Dependent dependent) {
        assert isWriteLocked();
        dependents.remove(dependent);
    }

    void setInstance(final ServiceControllerImpl<?> newInstance) throws DuplicateServiceException {
        assert newInstance != null;
        assert isWriteLocked();
        if (instance != null) {
            throw new DuplicateServiceException(String.format("Service %s is already registered", name.getCanonicalName()));
        }
        instance = newInstance;
        if (demandedByCount > 0) instance.addDemands(demandedByCount);
        if (dependentsStartedCount > 0) instance.dependentsStarted(dependentsStartedCount);
    }

    void clearInstance(final ServiceControllerImpl<?> oldInstance) {
        assert oldInstance != null;
        assert isWriteLocked();
        if (instance == oldInstance) instance = null;
    }

    ServiceContainerImpl getContainer() {
        return container;
    }

    @Override
    public Object getValue() throws IllegalStateException {
        synchronized (this) {
            if (instance != null) return instance.getValue();
        }
        throw new IllegalStateException("Service is not installed");
    }

    @Override
    public ServiceName getName() {
        return name;
    }

    @Override
    public ServiceControllerImpl<?> getDependencyController() {
        return getInstance();
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

    ServiceControllerImpl<?> getInstance() {
        synchronized (this) {
            return instance;
        }
    }

}
