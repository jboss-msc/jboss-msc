package org.jboss.msc.registry;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceBuilder;

/**
 * An ordered set of service batchEntries that should be processed as one.
 * 
 * @author Jason T. Greene
 */
public final class ServiceBatch {

    private final LinkedHashMap<ServiceName, BatchEntry> batchEntries = new LinkedHashMap<ServiceName, BatchEntry>();
    private final ServiceRegistry serviceRegistry;

    ServiceBatch(final ServiceRegistry serviceRegistry) {
        this.serviceRegistry = serviceRegistry;
    }

    public void install() throws ServiceRegistryException {
        serviceRegistry.install(this);
    }

    /**
     * Add a service definition to the batch.
     * 
     * @param definition
     * @return this batch
     */
    public ServiceBatch add(ServiceDefinition definition) {
        batchEntries.put(definition.getName(), new BatchEntry(definition));

        return this;
    }

    /**
     * Add a list of service batchEntries to the batch, in the order of the list.
     * 
     * @param definitions add a list of service batchEntries to the batch, in the
     *        order of the list
     * @return this batch
     */
    public ServiceBatch add(ServiceDefinition... definitions) {
        final Map<ServiceName, BatchEntry> batchEntries = this.batchEntries;

        for (ServiceDefinition definition : definitions) {
            batchEntries.put(definition.getName(), new BatchEntry(definition));
        }

        return this;
    }

    /**
     * Add a collection of service batchEntries to the batch, in the order of the
     * collection (if ordered).
     * 
     * @param definitions add a list of service batchEntries to the batch, in the
     *        order of the list
     * @return this batch
     */
    public ServiceBatch add(Collection<ServiceDefinition> definitions) {
        if (definitions == null)
            throw new IllegalArgumentException("Definitions can not be null");

        final Map<ServiceName, BatchEntry> batchEntries = this.batchEntries;

        for (ServiceDefinition definition : definitions) {
            batchEntries.put(definition.getName(), new BatchEntry(definition));
        }

        return this;
    }

    LinkedHashMap<ServiceName, BatchEntry> getBatchEntries() {
        return batchEntries;
    }

    /**
     * This class represents an entry in a ServiceBatch.  Basically a wrapper around a ServiceDefinition that also
     * maintain some state information for resolution.
     */
    public class BatchEntry {
        final ServiceDefinition serviceDefinition;
        boolean processed;
        boolean visited;
        BatchEntry prev;
        ServiceBuilder<Service> builder;
        int i;

        public BatchEntry(ServiceDefinition serviceDefinition) {
            this.serviceDefinition = serviceDefinition;
        }
    }
}
