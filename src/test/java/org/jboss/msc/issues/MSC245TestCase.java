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

import org.jboss.modules.management.ObjectProperties;
import org.jboss.msc.service.*;
import org.junit.*;

import javax.management.*;
import java.lang.management.ManagementFactory;
import java.util.concurrent.CountDownLatch;
import java.util.function.Consumer;

import static org.jboss.modules.management.ObjectProperties.property;
import static org.junit.Assert.assertTrue;

/**
 * [MSC-245] Test that verifies fix of container registry memory leak.
 *
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
public class MSC245TestCase {

    private static final int MAX = 50;
    private static final String CONTAINER_NAME = "MSC-245-ISSUE";
    private static final ServiceName[] SERVICE_NAMES = new ServiceName[MAX];
    private static final ServiceController[] controllers = new ServiceController[MAX];
    private static final MBeanServer server = ManagementFactory.getPlatformMBeanServer();
    private static volatile ServiceContainer container;

    static {
        for (int i = 0; i < MAX; i++) {
            SERVICE_NAMES[i] = ServiceName.of("" + i);
        }
    }

    @BeforeClass
    public static void init() throws Exception {
        container = ServiceContainer.Factory.create(CONTAINER_NAME);
    }

    @AfterClass
    public static void destroy() throws Exception {
        container.shutdown();
        container.awaitTermination();
        container = null;
    }

    @Test
    public void testMemoryLeakNoDependencies() throws Exception {
        memoryLeakTest(false);
    }

    @Test
    public void testMemoryLeakWithDependencies() throws Exception {
        memoryLeakTest(true);
    }

    private void memoryLeakTest(final boolean addDependencies) throws Exception {
        assertTrue(0 == getContainerRegistrySize());
        final CountDownLatch startLatch = new CountDownLatch(MAX);
        final LifecycleListener startListener = new LifecycleListener() {
            @Override
            public void handleEvent(ServiceController<?> controller, LifecycleEvent event) {
                if (event == LifecycleEvent.UP) startLatch.countDown();
            }
        };
        final CountDownLatch removeLatch = new CountDownLatch(MAX);
        final LifecycleListener removeListener = new LifecycleListener() {
            @Override
            public void handleEvent(ServiceController<?> controller, LifecycleEvent event) {
                if (event == LifecycleEvent.REMOVED) removeLatch.countDown();
            }
        };
        ServiceBuilder<?> sb;
        Consumer<Integer> c;
        for (int i = 0; i < MAX; i++) {
            sb = container.addService(SERVICE_NAMES[i]);
            c = sb.provides(SERVICE_NAMES[i]);
            if (addDependencies) {
                for (int j = i + 1; j < MAX; j++) {
                    sb.requires(SERVICE_NAMES[j]);
                }
            }
            sb.addListener(startListener);
            sb.addListener(removeListener);
            sb.setInstance(org.jboss.msc.Service.newInstance(c, i));
            controllers[i] = sb.install();
        }
        startLatch.await();
        System.out.println("All " + MAX + " services have been started");
        assertTrue(MAX == getContainerRegistrySize());

        for (int i = 0; i < MAX; i++) {
            controllers[i].setMode(ServiceController.Mode.REMOVE);
        }
        removeLatch.await();
        System.out.println("All " + MAX + " services have been removed");
        assertTrue(0 == getContainerRegistrySize());
    }

    private int getContainerRegistrySize() throws Exception {
        final ObjectName containerON = new ObjectName("jboss.msc", ObjectProperties.properties(property("type", "container"), property("name", CONTAINER_NAME)));
        return ((String[])server.invoke(containerON, "queryServiceNames", null, null)).length;
    }
}
