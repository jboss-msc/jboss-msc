package org.jboss.msc.bench;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import org.jboss.msc.registry.ServiceDefinition;
import org.jboss.msc.registry.ServiceRegistrationBatchBuilder;
import org.jboss.msc.registry.ServiceRegistry;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceContainer;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.TimingServiceListener;

public class TenForwardBench {

    public static void main(String[] args) throws Exception {
        final int totalServiceDefinitions = Integer.parseInt(args[0]);

        final ServiceContainer container = ServiceContainer.Factory.create();
        ServiceRegistrationBatchBuilder batch = ServiceRegistry.Factory.create(container).batchBuilder();

        final CountDownLatch latch = new CountDownLatch(1);
        final TimingServiceListener listener = new TimingServiceListener(new TimingServiceListener.FinishListener() {
            public void done(final TimingServiceListener timingServiceListener) {
                latch.countDown();
            }
        });
        for (int i = 0; i < totalServiceDefinitions; i++) {
            List<ServiceName> deps = new ArrayList<ServiceName>();
            int numDeps = Math.min(10, totalServiceDefinitions - i - 1);

            for (int j = 1; j < numDeps + 1; j++) {
                deps.add(ServiceName.of(("test" + (i + j)).intern()));
            }
            batch.add(ServiceDefinition.build(ServiceName.of(("test" + i).intern()), Service.NULL_VALUE).addListener(listener).addDependencies(deps.toArray(new ServiceName[deps.size()])).create());
        }
        
        batch.install();
        listener.finishBatch();
        latch.await();
        System.out.println(totalServiceDefinitions + " : "  + listener.getElapsedTime() / 1000.0);
        container.shutdown();
    }
}
