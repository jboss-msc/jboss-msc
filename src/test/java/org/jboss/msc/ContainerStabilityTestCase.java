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

import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.jboss.msc.service.LifecycleEvent;
import org.jboss.msc.service.LifecycleListener;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;

import org.junit.Test;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
public final class ContainerStabilityTestCase extends AbstractServiceTest {

    @Test
    public void testSimpleInstallation() {
        ServiceName sn = ServiceName.of("Test1");
        ServiceBuilder<?> sb = serviceContainer.addService();
        Consumer<String> providedValue = sb.provides(sn);
        sb.setInstance(Service.newInstance(providedValue, sn.toString()));
        ServiceController<?> controller = sb.install();

        final Set<Object> problem = new HashSet<Object>();
        final Set<Object> failed = new HashSet<Object>();
        try {
            serviceContainer.awaitStability(failed, problem);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        assertSame(controller, serviceContainer.getRequiredService(sn));
        assertTrue(problem.isEmpty());
        assertTrue(failed.isEmpty());
    }

    @Test
    public void testSimpleInstallation2() {
        ServiceName sn1 = ServiceName.of("Test1");
        ServiceBuilder<?> sb1 = serviceContainer.addService();
        final Consumer<String> parentProvidedValue = sb1.provides(sn1);
        sb1.setInstance(new Service() {
            @Override
            public void start(StartContext context) {
                ServiceName childSN = ServiceName.of("Test1.child");
                ServiceBuilder<?> childSB = context.getChildTarget().addService();
                Consumer<String> childProvidedValue = childSB.provides(childSN);
                childSB.setInstance(Service.newInstance(childProvidedValue, childSN.toString()));
                childSB.addListener(new LifecycleListener() {
                    @Override
                    public void handleEvent(ServiceController<?> controller, LifecycleEvent event) {
                        // blah
                    }
                });
                childSB.install();
                parentProvidedValue.accept(sn1.toString());
            }

            @Override
            public void stop(StopContext context) {
                parentProvidedValue.accept(null);
            }
        });
        ServiceName sn2 = ServiceName.of("Test2");
        sb1.requires(sn2);
        final ServiceController<?> controller1 = sb1.install();
        ServiceBuilder<?> sb2 = serviceContainer.addService();
        Consumer<String> providedValue = sb2.provides(sn2);
        sb2.setInstance(Service.newInstance(providedValue, sn2.toString()));
        sb2.setInitialMode(ServiceController.Mode.ON_DEMAND);
        final ServiceController<?> controller2 = sb2.install();
        final Set<Object> problem = new HashSet<Object>();
        final Set<Object> failed = new HashSet<Object>();
        try {
            serviceContainer.awaitStability(failed, problem);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        assertSame(controller1, serviceContainer.getRequiredService(sn1));
        assertSame(controller2, serviceContainer.getRequiredService(sn2));
        assertTrue(problem.isEmpty());
        assertTrue(failed.isEmpty());
    }

    @Test
    public void testSimpleInstallationWithFailure1() {
        ServiceName sn1 = ServiceName.of("Test1");
        ServiceBuilder<?> sb1 = serviceContainer.addService();
        final Consumer<String> providedValue = sb1.provides(sn1);
        sb1.setInstance(new Service() {
            @Override
            public void start(StartContext context) throws StartException {
                ServiceName childSN = ServiceName.of("Test1.child");
                ServiceBuilder<?> childSB = context.getChildTarget().addService();
                Consumer<String> childProvidedValue = childSB.provides(childSN);
                childSB.setInstance(new Service() {
                    @Override
                    public void start(StartContext context) throws StartException {
                        context.failed(new StartException("Failed on purpose!"));
                    }

                    @Override
                    public void stop(StopContext context) {
                        // blah
                    }
                });
                childSB.addListener(new LifecycleListener() {
                    @Override
                    public void handleEvent(ServiceController<?> controller, LifecycleEvent event) {
                    }
                });
                childSB.install();
                providedValue.accept(sn1.toString());
            }

            @Override
            public void stop(StopContext context) {
                providedValue.accept(null);
            }
        });
        ServiceName sn2 = ServiceName.of("Test2");
        sb1.requires(sn2);
        final ServiceController<?> controller1 = sb1.install();

        ServiceBuilder<?> sb2 = serviceContainer.addService();
        Consumer<String> value2 = sb2.provides(sn2);
        sb2.setInstance(Service.newInstance(value2, sn2.toString()));
        sb2.setInitialMode(ServiceController.Mode.ON_DEMAND);
        final ServiceController<?> controller2 = sb2.install();

        final Set<Object> problem = new HashSet<Object>();
        final Set<Object> failed = new HashSet<Object>();
        try {
            serviceContainer.awaitStability(failed, problem);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        assertSame(controller1, serviceContainer.getRequiredService(sn1));
        assertSame(controller2, serviceContainer.getRequiredService(sn2));
        assertTrue(problem.isEmpty());
        assertTrue(failed.size() == 1);
    }
}
