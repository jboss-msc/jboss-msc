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

import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * A stability monitor for satisfying certain AS use cases.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
public final class StabilityMonitor {

    private final Object lock = new Object();
    private final Set<ServiceController<?>> problems = new IdentityHashSet<ServiceController<?>>();
    private final Set<ServiceController<?>> failed = new IdentityHashSet<ServiceController<?>>();
    private Set<ServiceControllerImpl<?>> controllers = new IdentityHashSet<ServiceControllerImpl<?>>();
    private int unstableServices;

    public void addController(final ServiceController<?> controller) {
        if (controller == null) return;
        final ServiceControllerImpl<?> serviceController = (ServiceControllerImpl<?>) controller;
        serviceController.addMonitor(this);
        synchronized (lock) {
            controllers.add(serviceController);
        }
    }

    public void removeController(final ServiceController<?> controller) {
        if (controller == null) return;
        final ServiceControllerImpl<?> serviceController = (ServiceControllerImpl<?>) controller;
        serviceController.removeMonitor(this);
        synchronized (lock) {
            controllers.remove(serviceController);
        }
    }
    
    public void clear() {
        final Set<ServiceControllerImpl<?>> controllers;
        synchronized(lock) {
            controllers = this.controllers;
            this.controllers = new IdentityHashSet<ServiceControllerImpl<?>>();
        }
        for (final ServiceControllerImpl<?> controller : controllers) {
            controller.removeMonitor(this);
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

    public void awaitStability(final Set<? super ServiceController<?>> failed, final Set<? super ServiceController<?>> problem) throws InterruptedException {
        this.awaitStability(failed, problem, null);
    }

    public void awaitStability(final Set<? super ServiceController<?>> failed, final Set<? super ServiceController<?>> problems, final StabilityStatistics statistics) throws InterruptedException {
        synchronized (lock) {
            while (unstableServices != 0) {
                lock.wait();
            }
            provideStatistics(failed, problems, statistics);
        }
    }

    public boolean awaitStability(final long timeout, final TimeUnit unit, final Set<? super ServiceController<?>> failed, final Set<? super ServiceController<?>> problem) throws InterruptedException {
        return awaitStability(timeout, unit, failed, problem, null);
    }

    public boolean awaitStability(final long timeout, final TimeUnit unit, final Set<? super ServiceController<?>> failed, final Set<? super ServiceController<?>> problems, final StabilityStatistics statistics) throws InterruptedException {
        long now = System.nanoTime();
        long remaining = unit.toNanos(timeout);
        synchronized (lock) {
            while (unstableServices != 0) {
                if (remaining <= 0L) {
                    return false;
                }
                lock.wait(remaining / 1000000L, (int) (remaining % 1000000L));
                remaining -= (-now + (now = System.nanoTime()));
            }
            provideStatistics(failed, problems, statistics);
            return true;
        }
    }

    void addProblem(final ServiceController<?> controller) {
        assert holdsLock(controller);
        synchronized (lock) {
            problems.add(controller);
        }
    }

    void removeProblem(final ServiceController<?> controller) {
        assert holdsLock(controller);
        synchronized (lock) {
            problems.remove(controller);
        }
    }

    void addFailed(final ServiceController<?> controller) {
        assert holdsLock(controller);
        synchronized (lock) {
            failed.add(controller);
        }
    }

    void removeFailed(final ServiceController<?> controller) {
        assert holdsLock(controller);
        synchronized (lock) {
            failed.remove(controller);
        }
    }

    void incrementUnstableServices() {
        synchronized (lock) {
            unstableServices++;
        }
    }

    void decrementUnstableServices() {
        synchronized (lock) {
            if (--unstableServices == 0) {
                lock.notifyAll();
            }
            assert unstableServices >= 0;
        }
    }

    private void provideStatistics(final Set<? super ServiceController<?>> failed, final Set<? super ServiceController<?>> problems, final StabilityStatistics statistics) {
        assert holdsLock(lock);
        if (failed != null) {
            failed.addAll(this.failed);
        }
        if (problems != null) {
            problems.addAll(this.problems);
        }
        if (statistics == null) return;
        int active = 0, lazy = 0, onDemand = 0, never = 0, passive = 0, remove = 0;
        for (final ServiceController<?> controller : controllers) {
            if (controller.getMode() == ACTIVE) active++;
            else if (controller.getMode() == PASSIVE) passive++;
            else if (controller.getMode() == ON_DEMAND) onDemand++;
            else if (controller.getMode() == NEVER) never++;
            else if (controller.getMode() == LAZY) lazy++;
            else if (controller.getMode() == REMOVE) remove++;
        }
        statistics.setActiveCount(active);
        statistics.setFailCount(this.failed.size());
        statistics.setLazyCount(lazy);
        statistics.setOnDemandCount(onDemand);
        statistics.setNeverCount(never);
        statistics.setPassiveCount(passive);
        statistics.setProblemCount(this.problems.size());
        statistics.setRemoveCount(remove);
    }
}
