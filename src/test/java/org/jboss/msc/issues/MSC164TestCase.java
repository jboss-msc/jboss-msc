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

package org.jboss.msc.issues;

import org.jboss.msc.service.*;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;

import static org.junit.Assert.assertTrue;

/**
 * [MSC-164] Ensuring dependents counter invariant.
 *
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
public class MSC164TestCase extends AbstractServiceTest {

    @Test
    public void testCompleteCalledAfterFailed() throws Throwable {
        final ServiceName dependent = ServiceName.of("dependent");
        final ServiceName dependency = ServiceName.of("dependency");
        final ServiceController<?> dependencyController = serviceContainer.addService(dependency, Service.NULL).install();
        // the first attempt to start service will fail
        final ServiceController<?> dependentController = serviceContainer.addService(dependent, new FailService())
                .addDependency(dependency)
                .install();
        // await failure
        final StabilityMonitor sm = new StabilityMonitor();
        sm.addController(dependencyController);
        sm.addController(dependentController);
        sm.awaitStability();
        assertTrue(dependencyController.getSubstate() == ServiceController.Substate.UP);
        assertTrue(dependentController.getSubstate() == ServiceController.Substate.START_FAILED);
        // let's recover
        dependentController.retry();
        // assert started
        sm.awaitStability();
        assertTrue(dependencyController.getSubstate() == ServiceController.Substate.UP);
        assertTrue(dependentController.getSubstate() == ServiceController.Substate.UP);
        // shutdown all services
        dependencyController.setMode(ServiceController.Mode.REMOVE);
        dependentController.setMode(ServiceController.Mode.REMOVE);
        sm.awaitStability();
        assertTrue(dependentController.getSubstate() == ServiceController.Substate.TERMINATED);
    }

    /* Service that fails on first start attempt, but succeeds on retry */
    private static final class FailService implements Service<Void> {

        private volatile boolean fail = true;

        @Override
        public void start(final StartContext context) throws StartException {
            if (fail) {
                fail = false;
                context.failed(new StartException("Expected exception"));
                return;
            } else {
                context.complete();
            }
        }

        @Override
        public Void getValue() throws IllegalStateException, IllegalArgumentException { return null; }

        @Override
        public void stop(final StopContext context) {}
    }

}
