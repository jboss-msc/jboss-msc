package org.jboss.msc.registry;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceListener;
import org.jboss.msc.service.ServiceName;

/**
 * An ordered set of service batchEntries that should be processed as one.
 * 
 * @author Jason T. Greene
 */
final class BatchBuilderImpl implements ServiceRegistrationBatchBuilder {

    private final LinkedHashMap<ServiceName, BatchEntry> batchEntries = new LinkedHashMap<ServiceName, BatchEntry>();
    private final ServiceRegistryImpl serviceRegistry;
    private final Set<ServiceListener<?>> listeners = new HashSet<ServiceListener<?>>();

    BatchBuilderImpl(final ServiceRegistryImpl serviceRegistry) {
        this.serviceRegistry = serviceRegistry;
    }

    public void install() throws ServiceRegistryException {
        serviceRegistry.install(this);
    }

    public BatchBuilderImpl add(ServiceDefinition definition) {
        batchEntries.put(definition.getName(), new BatchEntry(definition));

        return this;
    }

    public BatchBuilderImpl add(ServiceDefinition<?>... definitions) {
        final Map<ServiceName, BatchEntry> batchEntries = this.batchEntries;

        for (ServiceDefinition definition : definitions) {
            batchEntries.put(definition.getName(), new BatchEntry(definition));
        }

        return this;
    }

    public BatchBuilderImpl add(Collection<ServiceDefinition<?>> definitions) {
        if (definitions == null)
            throw new IllegalArgumentException("Definitions can not be null");

        final Map<ServiceName, BatchEntry> batchEntries = this.batchEntries;

        for (ServiceDefinition definition : definitions) {
            batchEntries.put(definition.getName(), new BatchEntry(definition));
        }

        return this;
    }

    @Override
    public ServiceRegistrationBatchBuilder addListener(ServiceListener<?> listener) {
        listeners.add(listener);

        return this; 
    }


    @Override
    public ServiceRegistrationBatchBuilder addListener(ServiceListener<?>... listeners) {
        final Set<ServiceListener<?>> batchListeners = this.listeners;

        for(ServiceListener<?> listener : listeners) {
            batchListeners.add(listener);
        }

        return this;
    }

    @Override
    public ServiceRegistrationBatchBuilder addListener(Collection<ServiceListener<?>> listeners) {
        if(listeners == null)
            throw new IllegalArgumentException("Listeners can not be null");

        final Set<ServiceListener<?>> batchListeners = this.listeners;

        for(ServiceListener<?> listener : listeners) {
            batchListeners.add(listener);
        }
        return this;
    }

    Set<ServiceListener<?>> getListeners() {
        return listeners;
    }

    LinkedHashMap<ServiceName, BatchEntry> getBatchEntries() {
        return batchEntries;
    }

    /**
     * This class represents an entry in a ServiceBatch.  Basically a wrapper around a ServiceDefinition that also
     * maintain some state information for resolution.
     */
    public class BatchEntry {
        final ServiceDefinition<?> serviceDefinition;
        boolean processed;
        boolean visited;
        BatchEntry prev;
        ServiceBuilder<?> builder;
        int i;

        public BatchEntry(ServiceDefinition<?> serviceDefinition) {
            this.serviceDefinition = serviceDefinition;
        }
    }
}
