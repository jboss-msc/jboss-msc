package org.jboss.msc.service;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.jboss.msc.value.ImmediateValue;
import org.jboss.msc.value.Value;

/**
 * An ordered set of service batchEntries that should be processed as one.
 * 
 * @author Jason T. Greene
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
final class BatchBuilderImpl implements BatchBuilder {

    private final Map<ServiceName, BatchServiceBuilderImpl<?>> batchServices = new HashMap<ServiceName, BatchServiceBuilderImpl<?>>();
    private final ServiceContainerImpl container;
    private final Set<ServiceListener<Object>> listeners = new HashSet<ServiceListener<Object>>();
    private boolean done;

    BatchBuilderImpl(final ServiceContainerImpl container) {
        this.container = container;
    }

    public void install() throws ServiceRegistryException {
        if (done) {
            throw alreadyInstalled();
        }
        done = true;
        container.install(this);
    }

    static IllegalStateException alreadyInstalled() {
        return new IllegalStateException("Batch already installed");
    }

    public <T> BatchServiceBuilderImpl<T> addServiceValue(final ServiceName name, final Value<? extends Service<T>> value) throws DuplicateServiceException {
        return createServiceBuilder(name, value, false);
    }

    private <T> BatchServiceBuilderImpl<T> createServiceBuilder(final ServiceName name, final Value<? extends Service<T>> value, final boolean ifNotExist) throws DuplicateServiceException {
        if (done) {
            throw alreadyInstalled();
        }
        final Map<ServiceName, BatchServiceBuilderImpl<?>> batchServices = this.batchServices;
        final BatchServiceBuilderImpl<?> old = batchServices.get(name);
        if (old != null) {
            throw new DuplicateServiceException("Service named " + name + " is already defined in this batch");
        }
        final BatchServiceBuilderImpl<T> builder = new BatchServiceBuilderImpl<T>(this, value, name, ifNotExist);
        batchServices.put(name, builder);
        return builder;
    }

    public <T> BatchServiceBuilder<T> addServiceValueIfNotExist(final ServiceName name, final Value<? extends Service<T>> value) throws DuplicateServiceException {
        return createServiceBuilder(name, value, true);
    }

    public <T> BatchServiceBuilderImpl<T> addService(final ServiceName name, final Service<T> service) throws DuplicateServiceException {
        return createServiceBuilder(name, new ImmediateValue<Service<T>>(service), false);
    }

    @Override
    public BatchBuilderImpl addListener(ServiceListener<Object> listener) {
        if (done) {
            throw alreadyInstalled();
        }
        listeners.add(listener);
        return this;
    }

    @Override
    public BatchBuilderImpl addListener(ServiceListener<Object>... listeners) {
        if (done) {
            throw alreadyInstalled();
        }
        final Set<ServiceListener<Object>> batchListeners = this.listeners;

        for(ServiceListener<Object> listener : listeners) {
            batchListeners.add(listener);
        }

        return this;
    }

    @Override
    public BatchBuilderImpl addListener(Collection<ServiceListener<Object>> listeners) {
        if (done) {
            throw alreadyInstalled();
        }
        if(listeners == null)
            throw new IllegalArgumentException("Listeners can not be null");

        final Set<ServiceListener<Object>> batchListeners = this.listeners;

        for(ServiceListener<Object> listener : listeners) {
            batchListeners.add(listener);
        }
        return this;
    }

    Set<ServiceListener<Object>> getListeners() {
        return listeners;
    }

    Map<ServiceName, BatchServiceBuilderImpl<?>> getBatchServices() {
        return batchServices;
    }

    boolean isDone() {
        return done;
    }
}
