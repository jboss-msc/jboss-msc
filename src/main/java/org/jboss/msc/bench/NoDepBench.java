package org.jboss.msc.bench;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.jboss.msc.registry.ServiceDefinition;
import org.jboss.msc.registry.ServiceRegistrationBatchBuilder;
import org.jboss.msc.registry.ServiceRegistry;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceContainer;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.TimingServiceListener;

public class NoDepBench {

    public static void main(String[] args) throws Exception {
        final int totalServiceDefinitions = Integer.parseInt(args[0]);

        final ServiceContainer container = ServiceContainer.Factory.create();
        container.setExecutor(new ThreadPoolExecutor(8, 8, 1000, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>()));
        ServiceRegistrationBatchBuilder batch = ServiceRegistry.Factory.create(container).batchBuilder();

        final CountDownLatch latch = new CountDownLatch(1);
        final TimingServiceListener listener = new TimingServiceListener(new TimingServiceListener.FinishListener() {
            public void done(final TimingServiceListener timingServiceListener) {
                latch.countDown();
            }
        });
        for (int i = 0; i < totalServiceDefinitions; i++) {
            batch.add(ServiceDefinition.build(ServiceName.of("test" + i), Service.NULL_VALUE).addListener(listener).create());
        }
        batch.install();
        listener.finishBatch();
        latch.await();
        System.out.println(totalServiceDefinitions + " : "  + listener.getElapsedTime() / 1000.0);
    }
}
