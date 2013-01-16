/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012, Red Hat, Inc., and individual contributors
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
import static org.jboss.msc.service.ServiceController.Mode.ACTIVE;
import static org.jboss.msc.service.ServiceController.Mode.LAZY;
import static org.jboss.msc.service.ServiceController.Mode.NEVER;
import static org.jboss.msc.service.ServiceController.Mode.ON_DEMAND;
import static org.jboss.msc.service.ServiceController.Mode.PASSIVE;
import static org.jboss.msc.service.ServiceController.Mode.REMOVE;
import static org.jboss.msc.service.ServiceController.State.UP;

import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A stability monitor for satisfying certain AS use cases.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
public final class StabilityMonitor {

    private final Object stabilityLock = new Object();
    private final Object controllersLock = new Object();
    private final Set<ServiceController<?>> problems = new IdentityHashSet<ServiceController<?>>();
    private final Set<ServiceController<?>> failed = new IdentityHashSet<ServiceController<?>>();
    private final AtomicBoolean cleanupInProgress = new AtomicBoolean();
    private Set<ServiceControllerImpl<?>> controllers = new IdentityHashSet<ServiceControllerImpl<?>>();
    private int unstableServices;

    public void addController(final ServiceController<?> controller) {
        if (controller == null) return;
        final ServiceControllerImpl<?> serviceController = (ServiceControllerImpl<?>) controller;
        synchronized (controllersLock) {
            // It is safe to call controller.addMonitor() under controllersLock because
            // controller.addMonitor() may callback only stabilityLock protected methods.
            serviceController.addMonitor(this);
            controllers.add(serviceController);
        }
    }

    public void removeController(final ServiceController<?> controller) {
        if (controller == null) return;
        final ServiceControllerImpl<?> serviceController = (ServiceControllerImpl<?>) controller;
        synchronized (controllersLock) {
            // It is safe to call controller.addMonitor() under controllersLock because
            // controller.addMonitor() may callback only stabilityLock protected methods.
            serviceController.removeMonitor(this);
            controllers.remove(serviceController);
        }
    }

    public void clear() {
        if (cleanupInProgress.compareAndSet(false, true)) {
            synchronized (controllersLock) {
                final Set<ServiceControllerImpl<?>> controllers;
                synchronized (stabilityLock) {
                    controllers = this.controllers;
                    this.controllers = new IdentityHashSet<ServiceControllerImpl<?>>();
                    failed.clear();
                    problems.clear();
                    unstableServices = 0;
                }
                // We cannot call removeMonitorNoCallback under stabilityLock
                // because of deadlock possibility. In order for removing controllers
                // to don't break stability invariants we're setting cleanupInProgress flag
                // until all the controllers are removed.
                for (final ServiceControllerImpl<?> controller : controllers) {
                    controller.removeMonitorNoCallback(this);
                }
            }
            cleanupInProgress.set(false);
        }
    }

    public void awaitStability() throws InterruptedException {
        awaitStability(null, null, null);
    }

    public void awaitStability(final StabilityStatistics statistics) throws InterruptedException {
        awaitStability(null, null, statistics);
    }

    public boolean awaitStability(final long timeout, final TimeUnit unit) throws InterruptedException {
        return awaitStability(timeout, unit, null, null, null);
    }

    public boolean awaitStability(final long timeout, final TimeUnit unit, final StabilityStatistics statistics) throws InterruptedException {
        return awaitStability(timeout, unit, null, null, statistics);
    }

    public void awaitStability(final Set<? super ServiceController<?>> failed, final Set<? super ServiceController<?>> problems) throws InterruptedException {
        this.awaitStability(failed, problems, null);
    }

    public boolean awaitStability(final long timeout, final TimeUnit unit, final Set<? super ServiceController<?>> failed, final Set<? super ServiceController<?>> problems) throws InterruptedException {
        return awaitStability(timeout, unit, failed, problems, null);
    }

    public void awaitStability(final Set<? super ServiceController<?>> failed, final Set<? super ServiceController<?>> problems, final StabilityStatistics statistics) throws InterruptedException {
        final int failedCount;
        final int problemsCount;
        synchronized (stabilityLock) {
            while (unstableServices != 0) {
                stabilityLock.wait();
            }
            // propagate failures
            if (failed != null) {
                failed.addAll(this.failed);
            }
            failedCount = this.failed.size();
            // propagate problems
            if (problems != null) {
                problems.addAll(this.problems);
            }
            problemsCount = this.problems.size();
        }
        // propagate statistics
        provideStatistics(failedCount, problemsCount, statistics);
    }

    public boolean awaitStability(final long timeout, final TimeUnit unit, final Set<? super ServiceController<?>> failed, final Set<? super ServiceController<?>> problems, final StabilityStatistics statistics) throws InterruptedException {
        long now = System.nanoTime();
        long remaining = unit.toNanos(timeout);
        final int failedCount;
        final int problemsCount;
        synchronized (stabilityLock) {
            while (unstableServices != 0) {
                if (remaining <= 0L) {
                    return false;
                }
                stabilityLock.wait(remaining / 1000000L, (int) (remaining % 1000000L));
                remaining -= (-now + (now = System.nanoTime()));
            }
            // propagate failures
            if (failed != null) {
                failed.addAll(this.failed);
            }
            failedCount = this.failed.size();
            // propagate problems
            if (problems != null) {
                problems.addAll(this.problems);
            }
            problemsCount = this.problems.size();
        }
        // propagate statistics
        provideStatistics(failedCount, problemsCount, statistics);
        return true;
    }

    void addProblem(final ServiceController<?> controller) {
        assert holdsLock(controller);
        if (cleanupInProgress.get()) return;
        synchronized (stabilityLock) {
            problems.add(controller);
        }
    }

    void removeProblem(final ServiceController<?> controller) {
        assert holdsLock(controller);
        if (cleanupInProgress.get()) return;
        synchronized (stabilityLock) {
            problems.remove(controller);
        }
    }

    void addFailed(final ServiceController<?> controller) {
        assert holdsLock(controller);
        if (cleanupInProgress.get()) return;
        synchronized (stabilityLock) {
            failed.add(controller);
        }
    }

    void removeFailed(final ServiceController<?> controller) {
        assert holdsLock(controller);
        if (cleanupInProgress.get()) return;
        synchronized (stabilityLock) {
            failed.remove(controller);
        }
    }

    void incrementUnstableServices() {
        if (cleanupInProgress.get()) return;
        synchronized (stabilityLock) {
            unstableServices++;
        }
    }

    void decrementUnstableServices() {
        if (cleanupInProgress.get()) return;
        synchronized (stabilityLock) {
            if (--unstableServices == 0) {
                stabilityLock.notifyAll();
            }
            assert unstableServices >= 0;
        }
    }

    private void provideStatistics(final int failedCount, final int problemsCount, final StabilityStatistics statistics) {
        assert !holdsLock(stabilityLock);
        assert !holdsLock(controllersLock);
        if (statistics == null) return;
        // We cannot obtain controllers snapshot and we cannot collect controllers statistics
        // under stabilityLock because of deadlock possibility. Thus we tolerate that
        // the snapshot can be little bit out of date until controllersLock is obtained
        // plus controllers mode and state can be changed during the statistics collection phase.
        final Set<ServiceControllerImpl<?>> controllers;
        synchronized (controllersLock) {
            controllers = this.controllers; 
        }
        // collect statistics
        int active = 0, lazy = 0, onDemand = 0, never = 0, passive = 0, started = 0, remove = 0;
        for (final ServiceController<?> controller : controllers) {
            if (controller.getState() == UP) started++;
            if (controller.getMode() == ACTIVE) active++;
            else if (controller.getMode() == PASSIVE) passive++;
            else if (controller.getMode() == ON_DEMAND) onDemand++;
            else if (controller.getMode() == NEVER) never++;
            else if (controller.getMode() == LAZY) lazy++;
            else if (controller.getMode() == REMOVE) remove++;
        }
        // report statistics
        statistics.setActiveCount(active);
        statistics.setFailedCount(failedCount);
        statistics.setLazyCount(lazy);
        statistics.setOnDemandCount(onDemand);
        statistics.setNeverCount(never);
        statistics.setPassiveCount(passive);
        statistics.setProblemsCount(problemsCount);
        statistics.setStartedCount(started);
        statistics.setRemovedCount(remove);
    }
}
