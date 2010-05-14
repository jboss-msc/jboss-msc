package org.jboss.msc.bench;

import org.jboss.msc.registry.ServiceDefinition;
import org.jboss.msc.registry.ServiceRegistrationBatchBuilder;
import org.jboss.msc.registry.ServiceRegistry;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceContainer;
import org.jboss.msc.service.ServiceName;

public class NoDepBench {

    public static void main(String[] args) throws Exception {
        final int totalServiceDefinitions = Integer.parseInt(args[0]);

        ServiceRegistrationBatchBuilder batch = ServiceRegistry.Factory.create(ServiceContainer.Factory.create()).batchBuilder();

        long start = System.nanoTime();
        
        for (int i = 0; i < totalServiceDefinitions; i++) {
            batch.add(ServiceDefinition.build(ServiceName.of("test" + i), Service.NULL_VALUE).create());
        }
        batch.install();
        
        long end = System.nanoTime();
        System.out.println(totalServiceDefinitions + " : "  + (end - start) / 1000000000.0);
    }
}
