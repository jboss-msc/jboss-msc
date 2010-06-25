/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat Middleware LLC, and individual contributors
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

package org.jboss.msc.bench;

import org.jboss.msc.service.BatchBuilder;
import org.jboss.msc.service.ServiceContainer;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.TimingServiceListener;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class NoDepsSleepyStartBench {

    public static void main(String[] args) throws Exception {
        final int totalServiceDefinitions = Integer.parseInt(args[0]);
        final int threadPoolSize = Integer.parseInt(args[1]);

        final ServiceContainer container = ServiceContainer.Factory.create();

        final ThreadPoolExecutor executor = new ThreadPoolExecutor(threadPoolSize, threadPoolSize, 1000, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>());
        container.setExecutor(executor);

        BatchBuilder batch = container.batchBuilder();

        final CountDownLatch latch = new CountDownLatch(1);
        final TimingServiceListener listener = new TimingServiceListener(new Runnable() {
            public void run() {
                latch.countDown();
            }
        });

        for (int i = 0; i < totalServiceDefinitions; i++) {
            final SleepService service = new SleepService();
            batch.addService(ServiceName.of(("test" + i).intern()), service);
        }
        
        batch.addListener(listener);
        batch.install();
        listener.finishBatch();
        latch.await();
        System.out.println(totalServiceDefinitions + " : " + listener.getElapsedTime() / 1000.0);
        container.shutdown();
        executor.shutdown();
    }
}