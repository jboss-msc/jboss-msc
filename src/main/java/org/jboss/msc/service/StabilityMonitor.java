/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2021, Red Hat, Inc., and individual contributors
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
import static org.jboss.msc.service.ServiceController.State.UP;

import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * A stability detection utility. It can be used to detect
 * if all the registered controllers with {@link StabilityMonitor} are in REST state.
 * The following controller substates are considered to be in REST state:
 * 
 * <ul>
 *   <li>{@link Substate#NEW}</li>
 *   <li>{@link Substate#DOWN}</li>
 *   <li>{@link Substate#PROBLEM}</li>
 *   <li>{@link Substate#START_FAILED}</li>
 *   <li>{@link Substate#UP}</li>
 *   <li>{@link Substate#REMOVED}</li>
 * </ul>
 * 
 * Sample bulk usage:
 * 
 * <pre>
 * Set&lt;ServiceController&lt;?&gt;&gt; controllers = ...
 * <b>StabilityMonitor monitor = new StabilityMonitor();</b>
 * for (ServiceController&lt;?&gt; controller : controllers) {
 *    <b>monitor.addController(controller);</b>
 * }
 * try {
 *    <b>monitor.awaitStability();</b>
 * } finally {
 *    <b>monitor.clear();</b>
 *    // since now on the monitor can be reused for another stability detection
 * }
 * // do something after all the controllers are in REST state
 * </pre>
 * 
 * Sample simple usage:
 * 
 * <pre>
 * ServiceController&lt;?&gt; controller = ...
 * <b>StabilityMonitor monitor = new StabilityMonitor();</b>
 * monitor.addController(controller);
 * controller.setMode(REMOVE);
 * try {
 *    <b>monitor.awaitStability();</b>
 * } finally {
 *    <b>monitor.removeController(controller);</b>
 * }
 * // do something after controller have been removed from container
 * </pre>
 * 
 * @see StabilityStatistics
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 * @deprecated Stability monitors are unreliable - do not use them.
 * This class will be removed in a future release.
 */
@Deprecated
public final class StabilityMonitor {

    private final Object stabilityLock = new Object();
    private final Object controllersLock = new Object();
    private final Set<ServiceController<?>> problems = new IdentityHashSet<>();
    private final Set<ServiceController<?>> failed = new IdentityHashSet<>();
    private IdentityHashSet<ServiceControllerImpl<?>> controllers = new IdentityHashSet<>();
    private boolean addInProgress;
    private boolean cleanupInProgress;
    private boolean removeInProgress;
    private int unstableServices;

    /**
     * Register controller with this monitor.
     *
     * @param controller to be registered for stability detection.
     * @throws java.lang.IllegalArgumentException if {@code controller} is null
     * @throws java.lang.IllegalStateException if {@code controller}s lock is held by current thread
     */
    public void addController(final ServiceController<?> controller) throws IllegalArgumentException, IllegalStateException {
        if (controller == null) {
            throw new IllegalArgumentException("Controller is null");
        }
        if (holdsLock(controller)) {
            throw new IllegalStateException("Controller lock is held");
        }
        final ServiceControllerImpl<?> serviceController = (ServiceControllerImpl<?>) controller;
        final boolean addMonitorToController;
        synchronized (controllersLock) {
            awaitCleanupCompletion();
            awaitAddCompletion();
            addInProgress = addMonitorToController = controllers.add(serviceController);
        }
        if (!addMonitorToController) return;
        serviceController.addMonitor(this);
        synchronized (controllersLock) {
            addInProgress = false;
            controllersLock.notifyAll();
        }
    }

    /**
     * Register controller with this monitor but don't call serviceController.addMonitor() at all.
     *
     * @param controller to be registered for stability detection.
     */
    void addControllerNoCallback(final ServiceControllerImpl<?> controller) {
        synchronized (controllersLock) {
            awaitCleanupCompletion();
            controllers.add(controller);
        }
    }

    /**
     * Unregister controller with this monitor.
     *
     * @param controller to be unregistered from stability detection.
     * @throws java.lang.IllegalArgumentException if {@code controller} is null
     * @throws java.lang.IllegalStateException if {@code controller}s lock is held by current thread
     */
    public void removeController(final ServiceController<?> controller) throws IllegalArgumentException, IllegalStateException {
        if (controller == null) {
            throw new IllegalArgumentException("Controller is null");
        }
        if (holdsLock(controller)) {
            throw new IllegalStateException("Controller lock is held");
        }
        final ServiceControllerImpl<?> serviceController = (ServiceControllerImpl<?>) controller;
        final boolean removeMonitorFromController;
        synchronized (controllersLock) {
            if (cleanupInProgress) return;
            awaitRemoveCompletion();
            removeInProgress = removeMonitorFromController = controllers.remove(serviceController);
        }
        if (!removeMonitorFromController) return;
        serviceController.removeMonitor(this);
        synchronized (controllersLock) {
            removeInProgress = false;
            controllersLock.notifyAll();
        }
    }

    /**
     * Unregister controller with this monitor but don't call serviceController.removeMonitor() at all.
     *
     * @param controller to be unregistered from stability detection.
     */
    void removeControllerNoCallback(final ServiceControllerImpl<?> controller) {
        synchronized (controllersLock) {
            if (!cleanupInProgress) {
                controllers.remove(controller);
            } else {
                // currently running cleanup process will remove this controller from controllers set
            }
        }
    }

    /**
     * Removes all the registered controllers in this monitor.
     * The monitor can be later reused for stability detection again.
     */
    public void clear() {
        final Set<ServiceControllerImpl<?>> controllers;
        synchronized (controllersLock) {
            synchronized (stabilityLock) {
                if (cleanupInProgress) return;
                cleanupInProgress = true;
                controllers = this.controllers;
                this.controllers = new IdentityHashSet<>();
                failed.clear();
                problems.clear();
                unstableServices = 0;
            }
        }
        try {
            // We cannot call removeMonitorNoCallback neither under stabilityLock nor controllersLock
            // because of deadlock possibility. In order for removing controllers
            // to don't break stability invariants we're setting cleanupInProgress flag
            // until all the controllers are removed.
            for (final ServiceControllerImpl<?> controller : controllers) {
                controller.removeMonitorNoCallback(this);
            }
        } finally {
            synchronized (controllersLock) {
                synchronized (stabilityLock) {
                    cleanupInProgress = false;
                    stabilityLock.notifyAll();
                }
                controllersLock.notifyAll();
            }
        }
    }

    /**
     * Causes the current thread to wait until the monitor is stable.
     *
     * @throws InterruptedException if the current thread is interrupted
     *         while waiting
     */
    public void awaitStability() throws InterruptedException {
        awaitStability(null, null, null);
    }

    /**
     * Causes the current thread to wait until the monitor is stable.
     *
     * @param statistics stability statistics report to fill in
     * @throws InterruptedException if the current thread is interrupted
     *         while waiting
     */
    public void awaitStability(final StabilityStatistics statistics) throws InterruptedException {
        awaitStability(null, null, statistics);
    }

    /**
     * Causes the current thread to wait until the monitor is stable.
     *
     * @param timeout the maximum time to wait
     * @param unit the time unit of the {@code timeout} argument
     * @return <tt>true</tt> if this monitor achieved stability,
     *         <tt>false</tt> if the timeout elapsed before stability
     * @throws InterruptedException if the current thread is interrupted
     *         while waiting
     */
    public boolean awaitStability(final long timeout, final TimeUnit unit) throws InterruptedException {
        return awaitStability(timeout, unit, null, null, null);
    }

    /**
     * Causes the current thread to wait until the monitor is stable.
     *
     * @param timeout the maximum time to wait
     * @param unit the time unit of the {@code timeout} argument
     * @param statistics stability statistics report to fill in
     * @return <tt>true</tt> if this monitor achieved stability,
     *         <tt>false</tt> if the timeout elapsed before stability
     * @throws InterruptedException if the current thread is interrupted
     *         while waiting
     */
    public boolean awaitStability(final long timeout, final TimeUnit unit, final StabilityStatistics statistics) throws InterruptedException {
        return awaitStability(timeout, unit, null, null, statistics);
    }

    /**
     * Causes the current thread to wait until the monitor is stable.
     *
     * @param failed a set into which failed services should be copied
     * @param problems a set into which problem services should be copied
     * @throws InterruptedException if the current thread is interrupted
     *         while waiting
     */
    public void awaitStability(final Set<? super ServiceController<?>> failed, final Set<? super ServiceController<?>> problems) throws InterruptedException {
        this.awaitStability(failed, problems, null);
    }

    /**
     * Causes the current thread to wait until the monitor is stable.
     *
     * @param timeout the maximum time to wait
     * @param unit the time unit of the {@code timeout} argument
     * @param failed a set into which failed services should be copied
     * @param problems a set into which problem services should be copied
     * @return <tt>true</tt> if this monitor achieved stability,
     *         <tt>false</tt> if the timeout elapsed before stability
     * @throws InterruptedException if the current thread is interrupted
     *         while waiting
     */
    public boolean awaitStability(final long timeout, final TimeUnit unit, final Set<? super ServiceController<?>> failed, final Set<? super ServiceController<?>> problems) throws InterruptedException {
        return awaitStability(timeout, unit, failed, problems, null);
    }

    /**
     * Causes the current thread to wait until the monitor is stable.
     *
     * @param failed a set into which failed services should be copied
     * @param problems a set into which problem services should be copied
     * @param statistics stability statistics report to fill in
     * @throws InterruptedException if the current thread is interrupted
     *         while waiting
     */
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

    /**
     * Causes the current thread to wait until the monitor is stable.
     *
     * @param timeout the maximum time to wait
     * @param unit the time unit of the {@code timeout} argument
     * @param failed a set into which failed services should be copied
     * @param problems a set into which problem services should be copied
     * @param statistics stability statistics report to fill in
     * @return <tt>true</tt> if this monitor achieved stability,
     *         <tt>false</tt> if the timeout elapsed before stability
     * @throws InterruptedException if the current thread is interrupted
     *         while waiting
     */
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
        synchronized (stabilityLock) {
            if (cleanupInProgress) return;
            problems.add(controller);
        }
    }

    void removeProblem(final ServiceController<?> controller) {
        assert holdsLock(controller);
        synchronized (stabilityLock) {
            if (cleanupInProgress) return;
            problems.remove(controller);
        }
    }

    void addFailed(final ServiceController<?> controller) {
        assert holdsLock(controller);
        synchronized (stabilityLock) {
            if (cleanupInProgress) return;
            failed.add(controller);
        }
    }

    void removeFailed(final ServiceController<?> controller) {
        assert holdsLock(controller);
        synchronized (stabilityLock) {
            if (cleanupInProgress) return;
            failed.remove(controller);
        }
    }

    void incrementUnstableServices() {
        synchronized (stabilityLock) {
            if (cleanupInProgress) return;
            unstableServices++;
        }
    }

    void decrementUnstableServices() {
        synchronized (stabilityLock) {
            if (cleanupInProgress) return;
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
        final Set<ServiceControllerImpl<?>> controllers = new IdentityHashSet<>();
        synchronized (controllersLock) {
            controllers.addAll(this.controllers); 
        }
        // collect statistics
        int active = 0, lazy = 0, onDemand = 0, never = 0, passive = 0, started = 0;
        for (final ServiceController<?> controller : controllers) {
            if (controller.getState() == UP) started++;
            if (controller.getMode() == ACTIVE) active++;
            else if (controller.getMode() == PASSIVE) passive++;
            else if (controller.getMode() == ON_DEMAND) onDemand++;
            else if (controller.getMode() == NEVER) never++;
            else if (controller.getMode() == LAZY) lazy++;
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
    }


    private void awaitAddCompletion() {
        assert holdsLock(controllersLock);
        boolean interrupted = false;
        try {
            while (addInProgress) {
                try {
                    controllersLock.wait();
                } catch (final InterruptedException e) {
                    interrupted = true;
                }
            }
        } finally {
            if (interrupted) {
                Thread.currentThread().interrupt();
            }
        }
    }


    private void awaitCleanupCompletion() {
        assert holdsLock(controllersLock);
        boolean interrupted = false;
        try {
            while (cleanupInProgress) {
                try {
                    controllersLock.wait();
                } catch (final InterruptedException e) {
                    interrupted = true;
                }
            }
        } finally {
            if (interrupted) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private void awaitRemoveCompletion() {
        assert holdsLock(controllersLock);
        boolean interrupted = false;
        try {
            while (removeInProgress) {
                try {
                    controllersLock.wait();
                } catch (final InterruptedException e) {
                    interrupted = true;
                }
            }
        } finally {
            if (interrupted) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
