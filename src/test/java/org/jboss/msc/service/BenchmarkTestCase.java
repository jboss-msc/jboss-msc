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

package org.jboss.msc.service;

import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.function.Consumer;

public class BenchmarkTestCase extends AbstractServiceTest {

    private static final ServiceName[] SERVICE_NAMES = new ServiceName[100];

    static {
        for (int i = 0; i < SERVICE_NAMES.length; i++) SERVICE_NAMES[i] = ServiceName.of("" + i);
    }

    @Test
    public void omg() throws Exception {
        long startTime = System.currentTimeMillis();
        ServiceBuilder<?> sb = null;
        ServiceName sn = null;
        final CountDownLatch startLatch = new CountDownLatch(SERVICE_NAMES.length);
        LifecycleListener startListener = new LifecycleListener() {
            @Override
            public void handleEvent(ServiceController<?> controller, LifecycleEvent event) {
                if (event == LifecycleEvent.UP) startLatch.countDown();;
            }
        };
        final CountDownLatch stopLatch = new CountDownLatch(SERVICE_NAMES.length);
        LifecycleListener stopListener = new LifecycleListener() {
            @Override
            public void handleEvent(ServiceController<?> controller, LifecycleEvent event) {
                if (event == LifecycleEvent.REMOVED) stopLatch.countDown();;
            }
        };
        for (int i = 0; i < SERVICE_NAMES.length; i++) {
            sn = SERVICE_NAMES[i];
            sb = serviceContainer.addService(sn);
            Consumer<Integer> c = sb.provides(sn);
            for (int j = i + 1; j < SERVICE_NAMES.length; j++) sb.requires(SERVICE_NAMES[j]);
            sb.setInstance(org.jboss.msc.Service.newInstance(c, i));
            sb.addListener(startListener);
            sb.addListener(stopListener);
            sb.install();
        }
        startLatch.await();
        System.out.println("Services started in: " + (System.currentTimeMillis() - startTime));
        startTime = System.currentTimeMillis();
        serviceContainer.shutdown();
        stopLatch.await();
        System.out.println("Services stopped in: " + (System.currentTimeMillis() - startTime));
    }

}
