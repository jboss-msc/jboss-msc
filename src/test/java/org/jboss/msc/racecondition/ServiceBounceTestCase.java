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

package org.jboss.msc.racecondition;

import static org.jboss.modules.management.ObjectProperties.property;

import java.lang.management.ManagementFactory;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;

import org.jboss.modules.management.ObjectProperties;
import org.jboss.msc.service.AbstractService;
import org.jboss.msc.service.AbstractServiceListener;
import org.jboss.msc.service.AbstractServiceTest;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

/**
 * Test to verify that there is no race condition when services are both stopping and starting at the same time.
 *
 * @author Stuart Douglas
 */
@Ignore("This tests needs org.jboss.msc.directionalExecutor set to pass")
public class ServiceBounceTestCase extends AbstractServiceTest {

    public static final String MODULE = "module";

    @Test
    public void testServiceBounceFromListener() throws Exception {
        for (int i = 0; i < getIterationCount(); ++i) {
            ServiceName s3 = ServiceName.JBOSS.append("s3");
            ServiceName s2 = ServiceName.JBOSS.append("s2");
            ServiceName s1 = ServiceName.JBOSS.append("s1");
            ServiceController<Void> c3 = serviceContainer.addService(s3, new RootService(s3, s2.append(MODULE)))
                    .install();
            ServiceController<Void> c2 = serviceContainer.addService(s2, new RootService(s2, s1.append(MODULE)))
                    .install();
            ServiceController<Void> c1 = serviceContainer.addService(s1, new RootService(s1))
                    .install();
            serviceContainer.awaitStability();
            final CountDownLatch latch = new CountDownLatch(1);
            c1.addListener(new AbstractServiceListener<Void>() {
                @Override
                public void transition(ServiceController<? extends Void> controller, ServiceController.Transition transition) {
                    if (transition.getAfter() == ServiceController.Substate.REMOVED) {
                        latch.countDown();
                    }
                }
            });

            c1.setMode(ServiceController.Mode.REMOVE);
            latch.await();
            c1 = serviceContainer.addService(s1, new RootService(s1))
                    .install();
            if (!serviceContainer.awaitStability(2, TimeUnit.SECONDS)) {
                dumpDetails();
                Assert.fail();
            }
            c1.setMode(ServiceController.Mode.REMOVE);
            c2.setMode(ServiceController.Mode.REMOVE);
            c3.setMode(ServiceController.Mode.REMOVE);
            if (!serviceContainer.awaitStability(2, TimeUnit.SECONDS)) {
                dumpDetails();
                Assert.fail();
            }
        }
    }

    protected int getIterationCount() {
        return 10000;
    }

    private void dumpDetails() throws Exception {
        MBeanServerConnection mbs = ManagementFactory.getPlatformMBeanServer();
        ObjectName on = new ObjectName("jboss.msc", ObjectProperties.properties(property("type", "container"), property("name", serviceContainer.getName())));
        String[] names = (String[]) mbs.invoke(on, "queryServiceNames", new Object[]{}, new String[]{});
        StringBuilder sb = new StringBuilder("Services for ");
        sb.append(serviceContainer.getName());
        sb.append("\n");
        for (String name : names) {
            sb.append(mbs.invoke(on, "dumpServiceDetails", new Object[]{name}, new String[]{String.class.getName()}));
            sb.append("\n");
        }
        sb.append(names.length);
        sb.append(" services displayed");
        System.out.println(sb);
    }

    private class RootService extends AbstractService<Void> {
        final ServiceName baseName;
        private final ServiceName[] serviceNames;

        private RootService(ServiceName baseName, ServiceName... serviceNames) {
            this.baseName = baseName;
            this.serviceNames = serviceNames;
        }

        @Override
        public void start(final StartContext context) throws StartException {
            ServiceName module = baseName.append(MODULE);
            context.getChildTarget().addService(baseName.append("firstModuleUse"), new FirstModuleUseService())
                    .addDependency(module)
                    .install();
            context.getChildTarget().addService(module, Service.NULL)
                    .addDependencies(serviceNames)
                    .install();
        }
    }

    private class FirstModuleUseService extends AbstractService<Void> {

        boolean first = true;

        @Override
        public void start(final StartContext context) throws StartException {
            if (first) {
                first = false;
            } else {
                first = true;
                context.getController().getParent().addListener(new AbstractServiceListener() {
                    @Override
                    public void transition(ServiceController controller, ServiceController.Transition transition) {
                        if (transition.getAfter() == ServiceController.Substate.DOWN) {
                            controller.setMode(ServiceController.Mode.ACTIVE);
                            controller.removeListener(this);
                        }
                    }
                });
                context.getController().getParent().setMode(ServiceController.Mode.NEVER);
            }
        }
    }
}
