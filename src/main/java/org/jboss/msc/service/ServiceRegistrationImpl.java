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

import java.util.ArrayList;

import static java.lang.Thread.holdsLock;

/**
 * A single service registration.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
final class ServiceRegistrationImpl implements Dependency {


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

    ServiceRegistrationImpl(final ServiceContainerImpl container, final ServiceName name) {
        this.container = container;
        this.name = name;
    }

    /**
     * Returns the dependents set.
     *
     * @return the dependents set
     */
    IdentityHashSet<Dependent> getDependents() {
        return dependents;
    }

    /**
     * Add a dependent to this controller.
     *
     * @param dependent the dependent to add
     */
    @Override
    public void addDependent(final Dependent dependent) {
        assert !holdsLock(this);
        assert !holdsLock(dependent);
        final ServiceControllerImpl<?> instance;
        final ArrayList<Runnable> tasks = new ArrayList<Runnable>();
        synchronized (this) {
            synchronized (dependents) {
                if (dependents.contains(dependent)) {
                    throw new IllegalStateException("Dependent already exists on this registration");
                }
            }
            instance = this.instance;
            if (instance == null) {
                dependent.immediateDependencyUnavailable(name);
                synchronized (dependents) {
                    dependents.add(dependent);
                }
                return;
            }
            synchronized (instance) {
                synchronized (dependents) {
                    dependents.add(dependent);
                }
                // if instance is not fully installed yet, we need to be on a synchronized(instance) block to avoid
                // creation and execution of ServiceAvailableTask before immediateDependencyUnavailable is invoked on
                // new dependent
                if (!instance.isInstallationCommitted()) {
                    dependent.immediateDependencyUnavailable(name);
                    return;
                }
                instance.newDependent(name, dependent);
                instance.addAsyncTasks(tasks.size() + 1);
            }
        }
        instance.doExecute(tasks);
        tasks.clear();
        synchronized(this) {
            synchronized (instance) {
                instance.removeAsyncTask();
                instance.transition(tasks);
                instance.addAsyncTasks(tasks.size());
            }
        }
        instance.doExecute(tasks);
    }

    /**
     * Remove a dependent from this controller.
     *
     * @param dependent the dependent to remove
     */
    @Override
    public void removeDependent(final Dependent dependent) {
        assert ! holdsLock(this);
        assert ! holdsLock(dependent);
        synchronized (dependents) {
            dependents.remove(dependent);
        }
    }

    /**
     * Set the instance.
     *
     * @param instance the new instance
     * @throws DuplicateServiceException if there is already an instance
     */
    void setInstance(final ServiceControllerImpl<?> instance) throws DuplicateServiceException {
        assert instance != null;
        assert ! holdsLock(this);
        assert ! holdsLock(instance);
        synchronized (this) {
            if (this.instance != null) {
                throw new DuplicateServiceException(String.format("Service %s is already registered", name.getCanonicalName()));
            }
            this.instance = instance;
            if (demandedByCount > 0) instance.addDemands(demandedByCount);
        }
    }

    void clearInstance(final ServiceControllerImpl<?> oldInstance) {
        assert ! holdsLock(this);
        synchronized (this) {
            final ServiceControllerImpl<?> instance = this.instance;
            if (instance != oldInstance) {
                return;
            }
            this.instance = null;
        }
    }

    ServiceContainerImpl getContainer() {
        return container;
    }

    @Override
    public void dependentStopped() {
        assert ! holdsLock(this);
        final ServiceControllerImpl<?> instance;
        synchronized (this) {
            instance = this.instance;
        }
        if (instance != null) {
            instance.dependentStopped();
        }
    }

    @Override
    public Object getValue() throws IllegalStateException {
        synchronized (this) {
            final ServiceControllerImpl<?> instance = this.instance;
            if (instance == null) {
                throw new IllegalStateException("Service is not installed");
            } else {
                return instance.getValue();
            }
        }
    }

    @Override
    public ServiceName getName() {
        return name;
    }

    @Override
    public void dependentStarted() {
        assert ! holdsLock(this);
        synchronized (this) {
            if (instance != null) {
                instance.dependentStarted();
            }
        }
    }

    @Override
    public void addDemand() {
        assert ! holdsLock(this);
        final ServiceControllerImpl<?> instance;
        synchronized (this) {
            demandedByCount++;
            instance = this.instance;
        }
        if (instance != null) {
            instance.addDemand();
        }
    }

    @Override
    public void removeDemand() {
        assert ! holdsLock(this);
        final ServiceControllerImpl<?> instance;
        synchronized (this) {
            demandedByCount--;
            instance = this.instance;
        }
        if (instance != null) {
            instance.removeDemand();
        }
    }

    ServiceControllerImpl<?> getInstance() {
        synchronized (this) {
            return instance;
        }
    }
}
