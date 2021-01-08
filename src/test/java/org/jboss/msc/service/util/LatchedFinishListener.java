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

package org.jboss.msc.service.util;

import org.jboss.msc.service.AbstractServiceListener;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceListener;
import org.jboss.msc.service.TimingServiceListener;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * @author John E. Bailey
 */
public class LatchedFinishListener extends AbstractServiceListener<Object> implements ServiceListener<Object> {
    private final CountDownLatch latch = new CountDownLatch(1);
    private final TimingServiceListener timingServiceListener = new TimingServiceListener(new Runnable() {
        @Override
        public void run() {
            latch.countDown();
        }
    });

    public void transition(final ServiceController<? extends Object> controller, final ServiceController.Transition transition) {
        timingServiceListener.transition(controller, transition);
    }

    @Override
    public void listenerAdded(ServiceController<? extends Object> serviceController) {
        timingServiceListener.listenerAdded(serviceController);
    }

    public void serviceRemoveRequested(final ServiceController<? extends Object> controller) {
        timingServiceListener.serviceRemoveRequested(controller);
    }

    public void serviceRemoveRequestCleared(final ServiceController<? extends Object> controller) {
        timingServiceListener.serviceRemoveRequestCleared(controller);
    }

    public void dependencyFailed(final ServiceController<? extends Object> controller) {
        timingServiceListener.dependencyFailed(controller);
    }

    public void dependencyFailureCleared(final ServiceController<? extends Object> controller) {
        timingServiceListener.dependencyFailureCleared(controller);
    }

    public void immediateDependencyUnavailable(final ServiceController<? extends Object> controller) {
        timingServiceListener.immediateDependencyUnavailable(controller);
    }

    public void immediateDependencyAvailable(final ServiceController<? extends Object> controller) {
        timingServiceListener.immediateDependencyAvailable(controller);
    }

    public void transitiveDependencyUnavailable(final ServiceController<? extends Object> controller) {
        timingServiceListener.transitiveDependencyUnavailable(controller);
    }

    public void transitiveDependencyAvailable(final ServiceController<? extends Object> controller) {
        timingServiceListener.transitiveDependencyAvailable(controller);
    }

    public void await() throws Exception {
        timingServiceListener.finishBatch();
        latch.await(30L, TimeUnit.SECONDS);
    }

    public long getElapsedTime() {
        return timingServiceListener.getElapsedTime();
    }
}
