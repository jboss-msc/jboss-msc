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

import java.util.Set;
import org.jboss.msc.service.util.FailToStartService;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class ContainerStabilityTestCase extends AbstractServiceTest {

    @Test
    public void testSimpleInstallation() {
        final ServiceBuilder<Void> builder = serviceContainer.addService(ServiceName.of("Test1"), Service.NULL);
        final ServiceController<Void> controller = builder.install();
        final Set<Object> problem = new IdentityHashSet<Object>();
        final Set<Object> failed = new IdentityHashSet<Object>();
        try {
            serviceContainer.awaitStability(failed, problem);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        assertController(controller.getName(), controller);
        assertTrue(problem.isEmpty());
        assertTrue(failed.isEmpty());
    }

    @Test
    public void testSimpleInstallation2() {
        ServiceBuilder<?> builder = serviceContainer.addService(ServiceName.of("Test1"), new Service<Object>() {
            public void start(final StartContext context) throws StartException {
                final ServiceBuilder<Void> builder = context.getChildTarget().addService(ServiceName.of("Test1.child"), NULL);
                builder.addListener(new AbstractServiceListener<Void>() {
                    public void transition(final ServiceController<? extends Void> controller, final ServiceController.Transition transition) {
                        // blah
                    }
                });
                builder.install();
            }

            public void stop(final StopContext context) {
            }

            public Object getValue() throws IllegalStateException, IllegalArgumentException {
                return null;
            }
        });
        builder.addDependencies(ServiceName.of("Test2"));
        final ServiceController<?> controller1 = builder.install();
        builder = serviceContainer.addService(ServiceName.of("Test2"), Service.NULL);
        builder.setInitialMode(ServiceController.Mode.ON_DEMAND);
        final ServiceController<?> controller2 = builder.install();
        final Set<Object> problem = new IdentityHashSet<Object>();
        final Set<Object> failed = new IdentityHashSet<Object>();
        try {
            serviceContainer.awaitStability(failed, problem);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        assertController(controller1.getName(), controller1);
        assertController(controller2.getName(), controller2);
        assertTrue(problem.isEmpty());
        assertTrue(failed.isEmpty());
    }

    @Test
    public void testSimpleInstallation3() {
        final ServiceBuilder<Void> builder = serviceContainer.addService(ServiceName.of("Test1"), Service.NULL);
        final ServiceController<Void> controller = builder.install();
        final StabilityMonitor stabilityMonitor = new StabilityMonitor();
        stabilityMonitor.addController(controller);
        final Set<Object> problem = new IdentityHashSet<Object>();
        final Set<Object> failed = new IdentityHashSet<Object>();
        try {
            stabilityMonitor.awaitStability(failed, problem);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        assertController(controller.getName(), controller);
        assertTrue(problem.isEmpty());
        assertTrue(failed.isEmpty());
    }

    @Test
    public void testSimpleInstallation4() {
        final StabilityMonitor stabilityMonitor = new StabilityMonitor();
        ServiceBuilder<?> builder = serviceContainer.addService(ServiceName.of("Test1"), new Service<Object>() {
            public void start(final StartContext context) throws StartException {
                final ServiceBuilder<Void> builder = context.getChildTarget().addService(ServiceName.of("Test1.child"), NULL);
                builder.addListener(new AbstractServiceListener<Void>() {
                    public void transition(final ServiceController<? extends Void> controller, final ServiceController.Transition transition) {
                        // blah
                    }
                });
                builder.install();
            }

            public void stop(final StopContext context) {
            }

            public Object getValue() throws IllegalStateException, IllegalArgumentException {
                return null;
            }
        });
        builder.addDependencies(ServiceName.of("Test2"));
        final ServiceController<?> controller1 = builder.install();
        stabilityMonitor.addController(controller1);
        builder = serviceContainer.addService(ServiceName.of("Test2"), Service.NULL);
        builder.setInitialMode(ServiceController.Mode.ON_DEMAND);
        final ServiceController<?> controller2 = builder.install();
        stabilityMonitor.addController(controller2);
        final Set<Object> problem = new IdentityHashSet<Object>();
        final Set<Object> failed = new IdentityHashSet<Object>();
        try {
            stabilityMonitor.awaitStability(failed, problem);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        assertController(controller1.getName(), controller1);
        assertController(controller2.getName(), controller2);
        assertTrue(problem.isEmpty());
        assertTrue(failed.isEmpty());
    }

    @Test
    public void testSimpleInstallationWithFailure1() {
        final StabilityMonitor stabilityMonitor = new StabilityMonitor();
        ServiceBuilder<?> builder = serviceContainer.addService(ServiceName.of("Test1"), new Service<Object>() {
            public void start(final StartContext context) throws StartException {
                final ServiceBuilder<Void> builder = context.getChildTarget().addService(ServiceName.of("Test1.child"), new AbstractService<Void>() {
                    public void start(final StartContext context) throws StartException {
                        context.failed(new StartException("Failed on purpose!"));
                    }
                });
                builder.addListener(new AbstractServiceListener<Void>() {
                    public void transition(final ServiceController<? extends Void> controller, final ServiceController.Transition transition) {
                        // blah
                    }
                });
                builder.install();
            }

            public void stop(final StopContext context) {
            }

            public Object getValue() throws IllegalStateException, IllegalArgumentException {
                return null;
            }
        });
        builder.addDependency(ServiceName.of("Test2"));
        final ServiceController<?> controller1 = builder.install();
        stabilityMonitor.addController(controller1);
        builder = serviceContainer.addService(ServiceName.of("Test2"), Service.NULL);
        builder.setInitialMode(ServiceController.Mode.ON_DEMAND);
        final ServiceController<?> controller2 = builder.install();
        stabilityMonitor.addController(controller2);
        final Set<Object> problem = new IdentityHashSet<Object>();
        final Set<Object> failed = new IdentityHashSet<Object>();
        try {
            stabilityMonitor.awaitStability(failed, problem);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        assertController(controller1.getName(), controller1);
        assertController(controller2.getName(), controller2);
        assertTrue(problem.isEmpty());
        assertTrue(failed.size() == 1);
    }
}
