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
final class ServiceRegistrationImpl {


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
    private final IdentityHashSet<ServiceInstanceImpl<?>> dependents = new IdentityHashSet<ServiceInstanceImpl<?>>(0);
    /**
     * The set of optional dependents on this registration.
     */
    private final IdentityHashSet<ServiceInstanceImpl<?>> optionalDependents = new IdentityHashSet<ServiceInstanceImpl<?>>(0);

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
     * Add a dependent to this controller.
     *
     * @param dependent the dependent to add
     */
    void addDependent(final ServiceInstanceImpl<?> dependent) {
        assert !lockHeld();
        assert !dependent.lockHeld();
        final ServiceInstanceImpl<?> instance;
        final ServiceInstanceImpl.Substate state;
        synchronized (this) {
            if (! dependents.add(dependent)) {
                throw new IllegalStateException("Dependent already exists on this registration");
            }
            instance = this.instance;
            if (instance == null) {
                return;
            }
            synchronized (instance) {
                state = instance.getSubstate();
                instance.addDependent(dependent);
                if (state != ServiceInstanceImpl.Substate.UP) return;
                instance.addAsyncTask();
            }
        }
        if (state == ServiceInstanceImpl.Substate.UP) {
            dependent.dependencyUp();
        }
        final Runnable[] tasks;
        synchronized (this) {
            instance.removeAsyncTask();
            tasks = instance.transition();
        }
        instance.doExecute(tasks);
    }

    /**
     * Remove a dependent from this controller.
     *
     * @param dependent the dependent to remove
     */
    void removeDependent(final ServiceInstanceImpl<?> dependent) {
        assert !lockHeld();
        assert !dependent.lockHeld();
        synchronized (this) {
            if (dependents.remove(dependent) && instance == null) {
                container.remove(name, this);
            }
        }
    }

    void setInstance(final ServiceInstanceImpl<?> instance) throws DuplicateServiceException {
        assert instance != null;
        assert !lockHeld();
        assert !instance.lockHeld();
        synchronized (this) {
            if (this.instance != null) {
                throw new DuplicateServiceException("Service already registered");
            }
            this.instance = instance;
            synchronized (instance) {
                instance.addDependents(dependents);
            }
            instance.addDemands(demandedByCount);
        }
    }

    void clearInstance() {
        assert !lockHeld();
        synchronized (this) {
            final ServiceInstanceImpl<?> instance = this.instance;
            if (instance == null) {
                throw new IllegalStateException("Service already cleared");
            }
            if (instance.getSubstate() != ServiceInstanceImpl.Substate.REMOVED) {
                // todo: someday, support runtime unlinking
                //
                // The problem is that while we could track whether our dependents are active or not,
                // we have no sane way to deal with the situation where they are active.  Throwing an
                // exception doesn't quite fit because then the best you can do is a "try unlink" kind of
                // thing.  To make it work, the reference would need its own full state machine which controls
                // all its dependencies.  Such complexity would probably hurt performance if it weren't very
                // carefully considered.  So, maybe later.
                throw new IllegalStateException("Cannot remove active service link");
            }
            // Once runtime unlinking is supported, this code will be necessary to remove our set
//            assert !instance.lockHeld();
//            synchronized (instance) {
//                instance.removeAllDependents(dependents);
//            }
            this.instance = null;
            if (dependents.isEmpty()) {
                container.remove(name, this);
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

    ServiceContainerImpl getContainer() {
        return container;
    }

    void dependentStopped() {
        synchronized (this) {
            assert instance != null;
            instance.dependentStopped();
        }
    }

    ServiceName getName() {
        return name;
    }

    void dependentStarted() {
        synchronized (this) {
            assert instance != null;
            instance.dependentStarted();
        }
    }

    void addDemand() {
        synchronized (this) {
            demandedByCount++;
            final ServiceInstanceImpl<?> instance = this.instance;
            if (instance != null) {
                instance.addDemand();
            }
        }
    }

    void removeDemand() {
        synchronized (this) {
            demandedByCount--;
            final ServiceInstanceImpl<?> instance = this.instance;
            if (instance != null) {
                instance.addDemand();
            }
        }
    }

    ServiceController<?> getInstance() {
        synchronized (this) {
            return instance;
        }
    }
}
