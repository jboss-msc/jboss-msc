package org.jboss.msc.registry;

import java.util.Collection;
import java.util.LinkedHashMap;

/**
 * An ordered set of service definitions that should be processed as one.
 * 
 * @author Jason T. Greene
 */
public final class ServiceBatch {

    private LinkedHashMap<ServiceName, ServiceDefinition> definitions = new LinkedHashMap<ServiceName, ServiceDefinition>();
    private ServiceRegistry serviceRegistry;

    ServiceBatch(final ServiceRegistry registry) {
        this.serviceRegistry = registry;
    }

    public void install() throws ServiceRegistryException {
        serviceRegistry.install(definitions);
    }

    /**
     * Add a service definition to the batch.
     * 
     * @param definition
     * @return this batch
     */
    public ServiceBatch add(ServiceDefinition definition) {
        definitions.put(definition.getName(), definition);

        return this;
    }

    /**
     * Add a list of service definitions to the batch, in the order of the list.
     * 
     * @param definitions add a list of service definitions to the batch, in the
     *        order of the list
     * @return this batch
     */
    public ServiceBatch add(ServiceDefinition... definitions) {
        for (ServiceDefinition d : definitions)
            this.definitions.put(d.getName(), d);

        return this;
    }

    /**
     * Add a collection of service definitions to the batch, in the order of the
     * collection (if ordered).
     * 
     * @param definitions add a list of service definitions to the batch, in the
     *        order of the list
     * @return this batch
     */
    public ServiceBatch add(Collection<ServiceDefinition> definitions) {
        if (definitions == null)
            throw new IllegalArgumentException("Definitions can not be null");
        
        for (ServiceDefinition d : definitions)
            this.definitions.put(d.getName(), d);

        return this;
    }

    LinkedHashMap<ServiceName, ServiceDefinition> definitionMap() {
        return definitions;
    }
}
