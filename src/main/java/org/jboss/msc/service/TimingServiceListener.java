/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat Middleware LLC, and individual contributors
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

import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;

/**
 * A service listener which times service start.  The timing starts from the moment the listener
 * is created and ends when the last service is batched and the services are all started.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class TimingServiceListener extends AbstractServiceListener<Object> implements ServiceListener<Object> {
    private volatile int finished = 0;
    private volatile int count = 1;
    private volatile int totalServices = 0;
    private final long start = System.currentTimeMillis();
    private volatile long end;
    private final FinishListener finishedTask;

    private static final AtomicIntegerFieldUpdater<TimingServiceListener> finishedUpdater = AtomicIntegerFieldUpdater.newUpdater(TimingServiceListener.class, "finished");

    private static final AtomicIntegerFieldUpdater<TimingServiceListener> countUpdater = AtomicIntegerFieldUpdater.newUpdater(TimingServiceListener.class, "count");

    private static final AtomicIntegerFieldUpdater<TimingServiceListener> totalServicesUpdater = AtomicIntegerFieldUpdater.newUpdater(TimingServiceListener.class, "totalServices");
    /**
     * Construct a new instance.
     */
    public TimingServiceListener() {
        finishedTask = null;
    }

    /**
     * Construct a new instance which calls the given task when the timing is done.
     *
     * @param finishedTask the finish task
     */
    public TimingServiceListener(final FinishListener finishedTask) {
        this.finishedTask = finishedTask;
    }

    /** {@inheritDoc} */
    public void listenerAdded(final ServiceController<? extends Object> serviceController) {
        countUpdater.incrementAndGet(this);
        totalServicesUpdater.incrementAndGet(this);
    }

    /** {@inheritDoc} */
    public void serviceStarted(final ServiceController<? extends Object> serviceController) {
        if (countUpdater.decrementAndGet(this) == 0) {
            done();
        }
        serviceController.removeListener(this);
    }

    /** {@inheritDoc} */
    public void serviceFailed(final ServiceController<? extends Object> serviceController, final StartException reason) {
        if (countUpdater.decrementAndGet(this) == 0) {
            done();
        }
        serviceController.removeListener(this);
    }

    private void done() {
        end = System.currentTimeMillis();
        if (finishedTask != null) {
            finishedTask.done(this);
        }
    }

    /**
     * Call when all services in this timing group have been added.
     */
    public void finishBatch() {
        if (finishedUpdater.getAndSet(this, 1) == 0) {
            if (countUpdater.decrementAndGet(this) == 0) {
                done();
            }
        }
    }

    /**
     * Determine whether all services have finished.
     *
     * @return {@code true} if all services have finished
     */
    public boolean finished() {
        return finished != 0;
    }

    /**
     * Get the number of remaining services to start.
     *
     * @return the remaining count
     */
    public int getRemainingCount() {
        return count;
    }

    /**
     * Get the total number of services being tracked.
     *
     * @return the total count
     */
    public int getTotalCount() {
        return totalServices;
    }

    /**
     * Get the elapsed time in milliseconds.
     *
     * @return the elapsed time, or -1 if not finished yet
     */
    public long getElapsedTime() {
        final long end = this.end;
        if (end == 0) {
            return -1;
        }
        return end - start;
    }

    /**
     * A listener for when timing has finished.
     */
    public interface FinishListener {

        /**
         * Called when timing has completed.
         *
         * @param timingServiceListener the listener
         */
        void done(TimingServiceListener timingServiceListener);
    }
}
