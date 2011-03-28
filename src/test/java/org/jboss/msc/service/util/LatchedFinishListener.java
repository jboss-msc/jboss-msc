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

package org.jboss.msc.service.util;

import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceListener;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.TimingServiceListener;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * @author John E. Bailey
 */
public class LatchedFinishListener implements ServiceListener<Object> {
    private final CountDownLatch latch = new CountDownLatch(1);
    private final TimingServiceListener timingServiceListener = new TimingServiceListener(new Runnable() {
        @Override
        public void run() {
            latch.countDown();
        }
    });

    @Override
    public void serviceStartRequested(final ServiceController<? extends Object> controller) {
        timingServiceListener.serviceStartRequested(controller);
    }

    @Override
    public void serviceStartRequestCleared(final ServiceController<? extends Object> controller) {
        timingServiceListener.serviceStartRequestCleared(controller);
    }

    @Override
    public void serviceStopRequested(final ServiceController<? extends Object> controller) {
        timingServiceListener.serviceStopRequested(controller);
    }

    @Override
    public void serviceStopRequestCleared(final ServiceController<? extends Object> controller) {
        timingServiceListener.serviceStopRequestCleared(controller);
    }

    @Override
    public void listenerAdded(ServiceController<? extends Object> serviceController) {
        timingServiceListener.listenerAdded(serviceController);
    }

    @Override
    public void serviceStarting(ServiceController<? extends Object> serviceController) {
        timingServiceListener.serviceStarting(serviceController);
    }

    @Override
    public void failedServiceStarting(final ServiceController<? extends Object> serviceController) {
        timingServiceListener.failedServiceStarting(serviceController);
    }

    @Override
    public void serviceStarted(ServiceController<? extends Object> serviceController) {
        timingServiceListener.serviceStarted(serviceController);
    }

    @Override
    public void serviceFailed(ServiceController<? extends Object> serviceController, StartException reason) {
        timingServiceListener.serviceFailed(serviceController, reason);
    }

    @Override
    public void serviceStopping(ServiceController<? extends Object> serviceController) {
        timingServiceListener.serviceStopping(serviceController);
    }

    @Override
    public void serviceStopped(ServiceController<? extends Object> serviceController) {
        timingServiceListener.serviceStopped(serviceController);
    }

    @Override
    public void failedServiceStopped(final ServiceController<? extends Object> controller) {
        timingServiceListener.failedServiceStopped(controller);
    }

    @Override
    public void serviceRemoveRequested(final ServiceController<? extends Object> controller) {
        timingServiceListener.serviceRemoveRequested(controller);
    }

    @Override
    public void serviceRemoved(ServiceController<? extends Object> serviceController) {
        timingServiceListener.serviceRemoved(serviceController);
    }

    @Override
    public void dependencyFailed(ServiceController<? extends Object> serviceController) {
        timingServiceListener.dependencyFailed(serviceController);
    }

    @Override
    public void dependencyFailureCleared(ServiceController<? extends Object> serviceController) {
        timingServiceListener.dependencyFailureCleared(serviceController);
    }
    
    @Override
    public void dependencyInstalled(ServiceController<? extends Object> controller) {
        timingServiceListener.dependencyInstalled(controller);
    }

    @Override
    public void dependencyUninstalled(ServiceController<? extends Object> controller) {
        timingServiceListener.dependencyUninstalled(controller);
    }

    public void await() throws Exception {
        timingServiceListener.finishBatch();
        latch.await(30L, TimeUnit.SECONDS);
    }

    public long getElapsedTime() {
        return timingServiceListener.getElapsedTime();
    }
}
