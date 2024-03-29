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

package org.jboss.msc;

import org.jboss.msc.service.*;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.function.Consumer;

import static org.junit.Assert.assertNotNull;

/**
 * [MSC-160] Ensuring StartContext lifecycle method invariants.
 *
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
public class MSC160TestCase extends AbstractServiceTest {

    @Test
    public void testCompleteCalledAfterFailed() throws Throwable {
        final CountDownLatch latch = new CountDownLatch(1);
        final ServiceBuilder<?> sb = serviceContainer.addService();
        final Consumer<Void> providedValue = sb.provides(ServiceName.of("CAF"));
        final CompleteAfterFailedService cafService = new CompleteAfterFailedService(latch, providedValue);
        sb.setInstance(cafService).install();
        latch.await();
        assertNotNull("IllegalStateException expected", cafService.e);
    }

    @Test
    public void testFailedCalledAfterComplete() throws Throwable {
        final CountDownLatch latch = new CountDownLatch(1);
        final ServiceBuilder<?> sb = serviceContainer.addService();
        final Consumer<Void> providedValue = sb.provides(ServiceName.of("FAC"));
        final FailedAfterCompleteService facService = new FailedAfterCompleteService(latch, providedValue);
        sb.setInstance(facService).install();
        latch.await();
        assertNotNull("IllegalStateException expected", facService.e);
    }

    private static final class FailedAfterCompleteService implements Service {

        private final CountDownLatch cdl;
        private final Consumer<Void> providedValue;
        private volatile Exception e;

        FailedAfterCompleteService(final CountDownLatch cdl, final Consumer<Void> providedValue) {
            this.cdl = cdl;
            this.providedValue = providedValue;
        }

        @Override
        public void start(final StartContext context) {
            providedValue.accept(null);
            context.complete();
            try {
                // the following call should throw RuntimeException
                context.failed(new StartException());
            } catch (final IllegalStateException expected) {
                e = expected;
            } finally {
                cdl.countDown();
            }
        }

        @Override
        public void stop(final StopContext context) {}

    }

    private static final class CompleteAfterFailedService implements Service {

        private final CountDownLatch cdl;
        private final Consumer<Void> providedValue;
        private volatile Exception e;

        CompleteAfterFailedService(final CountDownLatch cdl, final Consumer<Void> providedValue) {
            this.cdl = cdl;
            this.providedValue = providedValue;
        }


        @Override
        public void start(final StartContext context) {
            providedValue.accept(null);
            context.failed(new StartException());
            try {
                context.complete();
            } catch (final IllegalStateException expected) {
                e = expected;
            } finally {
                cdl.countDown();
            }
        }

        @Override
        public void stop(final StopContext context) {}

    }
}
