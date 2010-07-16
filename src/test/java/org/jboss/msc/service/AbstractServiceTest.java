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

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.jboss.msc.service.util.LatchedFinishListener;

import java.util.List;

import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;

/**
 * Test base used for service test cases.
 *
 * @author John E. Bailey
 */
public class AbstractServiceTest {

    public static abstract class ServiceTestInstance {
        public abstract List<BatchBuilder> initializeBatches(ServiceContainer serviceContainer, LatchedFinishListener finishListener) throws Exception;

        public void performAssertions(ServiceContainer serviceContainer) throws Exception {
        }

        public void handle(BatchBuilder batch, Throwable t) {
        }
    }

    protected void performTest(ServiceTestInstance instance) throws Exception {
        final ServiceContainer serviceContainer = ServiceContainer.Factory.create();
        final LatchedFinishListener finishListener = new LatchedFinishListener();

        final List<BatchBuilder> batches = instance.initializeBatches(serviceContainer, finishListener);
        for (BatchBuilder batch : batches) {
            try {
                batch.install();
            } catch (Throwable t) {
                instance.handle(batch, t);
            }
        }
        finishListener.await();
        instance.performAssertions(serviceContainer);
        serviceContainer.shutdown();
    }

    protected void awaitState(ServiceController<?> controller, final ServiceController.State state, long millis) {
        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicBoolean ran = new AtomicBoolean(false);
        controller.addListener(new ServiceListener<Object>() {
            public void listenerAdded(final ServiceController<?> serviceController) {
                check(serviceController);
            }

            public void serviceStarting(final ServiceController<?> serviceController) {
                check(serviceController);
            }

            public void serviceStarted(final ServiceController<?> serviceController) {
                check(serviceController);
            }

            public void serviceFailed(final ServiceController<?> serviceController, final StartException reason) {
                check(serviceController);
            }

            public void serviceStopping(final ServiceController<?> serviceController) {
                check(serviceController);
            }

            public void serviceStopped(final ServiceController<?> serviceController) {
                check(serviceController);
            }

            public void serviceRemoved(final ServiceController<?> serviceController) {
                check(serviceController);
            }

            private void check(final ServiceController<?> serviceController) {
                if (serviceController.getState() == state) {
                    if (! ran.getAndSet(true)) {
                        latch.countDown();
                        serviceController.removeListener(this);
                    }
                }
            }
        });
        boolean ok = false;
        try {
            ok = latch.await(millis, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            fail("Interrupted waiting for state " + state + " of controller " + controller);
        }
        assertTrue("Timed out waiting for state " + state + " of controller " + controller, ok);
    }
}
