/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017, Red Hat, Inc., and individual contributors
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

import static org.junit.Assert.assertTrue;

import org.jboss.msc.inject.Injector;
import org.jboss.msc.inject.SetMethodInjector;
import org.jboss.msc.service.*;
import org.jboss.msc.value.ImmediateValue;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;

/**
 * [MSC-163] Ensure 'out injections' work for both synchronous and asynchronous services.
 *
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
public class MSC163TestCase extends AbstractServiceTest {

    private static final ServiceName fooService = ServiceName.of("foo");
    private static final ServiceName barService = ServiceName.of("bar");

    @Test
    public void outInjectionsSynchronousCompleteCalled() throws Exception {
        final StabilityMonitor sm = new StabilityMonitor();
        DependentService dependentService = new DependentService();
        sm.addController(serviceContainer.addService(barService, dependentService).addDependency(fooService).install());
        Injector i = new SetMethodInjector(new ImmediateValue(dependentService), DependentService.class.getDeclaredMethod("setBoolean", Boolean.class));
        sm.addController(serviceContainer.addService(fooService, new SyncAsyncService(false, true)).addInjection(i).install());
        sm.awaitStability();
        assertTrue(dependentService.getValue() != null);
    }

    @Test
    public void outInjectionsAsynchronousCompleteCalled() throws Exception {
        final StabilityMonitor sm = new StabilityMonitor();
        DependentService dependentService = new DependentService();
        sm.addController(serviceContainer.addService(barService, dependentService).addDependency(fooService).install());
        Injector i = new SetMethodInjector(new ImmediateValue(dependentService), DependentService.class.getDeclaredMethod("setBoolean", Boolean.class));
        sm.addController(serviceContainer.addService(fooService, new SyncAsyncService(true, true)).addInjection(i).install());
        sm.awaitStability();
        assertTrue(dependentService.getValue() != null);
    }

    @Test
    public void outInjectionsNoLifecycleMethodCalled() throws Exception {
        final StabilityMonitor sm = new StabilityMonitor();
        DependentService dependentService = new DependentService();
        sm.addController(serviceContainer.addService(barService, dependentService).addDependency(fooService).install());
        Injector i = new SetMethodInjector(new ImmediateValue(dependentService), DependentService.class.getDeclaredMethod("setBoolean", Boolean.class));
        sm.addController(serviceContainer.addService(fooService, new SyncAsyncService(false, false)).addInjection(i).install());
        sm.awaitStability();
        assertTrue(dependentService.getValue() != null);
    }

    public static class DependentService implements Service<Boolean> {

        private volatile Boolean injectedValue;

        public void setBoolean(final Boolean b) {
            this.injectedValue = b;
        }

        @Override
        public Boolean getValue() throws IllegalStateException, IllegalArgumentException {
            return injectedValue;
        }

        @Override
        public void start(final StartContext context) throws StartException {
            context.complete();
        }

        @Override
        public void stop(final StopContext context) {
            // does nothing
        }
    }

    private static class SyncAsyncService implements Service<Boolean> {
        private final boolean callAsynchronous;
        private final boolean callComplete;

        private SyncAsyncService(final boolean callAsynchronous, final boolean callComplete) {
            this.callAsynchronous = callAsynchronous;
            this.callComplete = callComplete;
        }

        @Override
        public Boolean getValue() throws IllegalStateException, IllegalArgumentException {
            return Boolean.TRUE;
        }

        @Override
        public void start(final StartContext context) throws StartException {
            if (callAsynchronous) context.asynchronous();
            if (callComplete) context.complete();
        }

        @Override
        public void stop(final StopContext context) {
            // does nothing
        }
    }

}
