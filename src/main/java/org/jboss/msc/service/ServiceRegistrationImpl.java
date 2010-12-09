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
    private ServiceInstanceImpl<?> instance;
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
     * Returns the dependents lock.<p>
     * This lock must be used to synchronize calls to {@link getDependents}, and any operations on the 
     * returned set. The lock should be released only after the dependents set reference is released
     * by the caller.
     *  
     * @return the dependents set lock
     * @see #getDependents()
     */
    final Object getDependentsLock() {
        return dependents;
    }

    /**
     * Returns the dependents set.<p>
     * This method must be called only by threads that hold the {@link #getDependentsLock() dependents lock},
     * and any operations on the returned set, including iteration, should synchronize on the same lock.
     * 
     * @return the dependents set
     * @see #getDependentsLock()
     */
    final IdentityHashSet<Dependent> getDependents() {
        assert lockHeldBy(dependents);
        return dependents;
    }

    /**
     * Add a dependent to this controller.
     *
     * @param dependent the dependent to add
     */
    @Override
    public void addDependent(final Dependent dependent) {
        assert !lockHeld();
        assert !lockHeldByDependent(dependent);
        final ServiceInstanceImpl<?> instance;
        synchronized (dependents) {
            if (! dependents.add(dependent)) {
                throw new IllegalStateException("Dependent already exists on this registration");
            }
        }
        final Runnable[] tasks;
        synchronized (this) {
            instance = this.instance;
            if (instance == null) {
                dependent.immediateDependencyUninstalled();
                return;
            }
            synchronized (instance) {
                instance.addAsyncTask();
            }
            instance.newDependent(dependent);
            synchronized (instance) {
                instance.removeAsyncTask();
                tasks = instance.transition();
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
        assert !lockHeld();
        assert !lockHeldByDependent(dependent);
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
    void setInstance(final ServiceInstanceImpl<?> instance) throws DuplicateServiceException {
        assert instance != null;
        assert !lockHeld();
        assert !instance.lockHeld();
        synchronized (this) {
            if (this.instance != null) {
                throw new DuplicateServiceException(String.format("Service %s is already registered", name.getCanonicalName()));
            }
            this.instance = instance;
            if (demandedByCount > 0) instance.addDemands(demandedByCount);
        }
        synchronized (dependents) {
            for (Dependent dependent: dependents) {
                dependent.immediateDependencyInstalled();
            }
        }
    }

    void clearInstance(final ServiceInstanceImpl<?> oldInstance) {
        assert !lockHeld();
        synchronized (this) {
            final ServiceInstanceImpl<?> instance = this.instance;
            if (instance != oldInstance) {
                return;
            }
            this.instance = null;
        }
        synchronized (dependents) {
            for (Dependent dependent: dependents) {
                dependent.immediateDependencyUninstalled();
            }
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

    boolean lockHeldBy(Object object) {
        return Thread.holdsLock(object);
    }

    ServiceContainerImpl getContainer() {
        return container;
    }

    @Override
    public void dependentStopped() {
        synchronized (this) {
            assert instance != null;
            instance.dependentStopped();
        }
    }

    @Override
    public Object getValue() throws IllegalStateException {
        synchronized (this) {
            final ServiceInstanceImpl<?> instance = this.instance;
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
        synchronized (this) {
            assert instance != null;
            instance.dependentStarted();
        }
    }

    @Override
    public void addDemand() {
        synchronized (this) {
            demandedByCount++;
            final ServiceInstanceImpl<?> instance = this.instance;
            if (instance != null) {
                instance.addDemand();
            }
        }
    }

    @Override
    public void removeDemand() {
        synchronized (this) {
            demandedByCount--;
            final ServiceInstanceImpl<?> instance = this.instance;
            if (instance != null) {
                instance.removeDemand();
            }
        }
    }

    ServiceInstanceImpl<?> getInstance() {
        synchronized (this) {
            return instance;
        }
    }
}
