package org.jboss.msc.registry;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.Location;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceListener;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ValueInjection;
import org.jboss.msc.value.ImmediateValue;
import org.jboss.msc.value.Value;

import java.util.Collection;

/**
 * Class representing the definition of a services prior to the service being installed.
 *
 * @author John Bailey
 * @author Jason T. Greene
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class ServiceDefinition<T> {
    private final ServiceName name;
    private final ServiceName[] dependencies;
    private final ServiceController.Mode initialMode;
    private final Location location;
    private final Value<? extends Service<T>> service;
    private final ValueInjection<?>[] injections;
    private final NamedServiceInjection<?>[] namedInjections;
    private final ServiceListener<?>[] listeners;

    private static final ServiceName[] NO_DEPS = new ServiceName[0];
    private static final ValueInjection[] NO_INJECTIONS = new ValueInjection[0];
    private static final NamedServiceInjection<?>[] NO_NAMED_INJECTIONS = new NamedServiceInjection<?>[0];
    private static final ServiceListener<?>[] NO_LISTENERS = new ServiceListener<?>[0];

    private ServiceDefinition(ServiceName name, ServiceController.Mode initialMode, Location location, Value<? extends Service<T>> service, ServiceName[] dependencies, ValueInjection<?>[] injections, final NamedServiceInjection<?>[] namedInjections, ServiceListener<?>[] listeners) {
        this.namedInjections = namedInjections;
        if(name == null) {
            throw new IllegalArgumentException("Name can not be null");
        }
        this.name = name;
        this.dependencies = dependencies;
        this.initialMode = initialMode;
        this.location = location;
        this.service = service;
        this.injections = injections;
        this.listeners = listeners;
    }

    /**
     * Build a new service definition via a builder object.
     *
     * @param name the service name
     * @param service a value which resolves to the service object
     * @param <T> the service value type
     * @return the builder
     */
    public static <T> Builder<T> build(final ServiceName name, final Value<? extends Service<T>> service) {
        return new Builder<T>(name, service);
    }

    /**
     * Build a new service definition via a builder object.
     *
     * @param name the service name
     * @param service the service object
     * @param <T> the service value type
     * @return the builder
     */
    public static <T> Builder<T> build(final ServiceName name, final Service<T> service) {
        return new Builder<T>(name, new ImmediateValue<Service<T>>(service));
    }

    /**
     * A service definition builder.
     *
     * @param <T> the service value type
     */
    public static final class Builder<T> {
        private final ServiceName name;
        private final Value<? extends Service<T>> service;
        private Collection<ServiceName> dependencies = new HashSet<ServiceName>(0);
        private List<ValueInjection<?>> injections = new ArrayList<ValueInjection<?>>(0);
        private List<NamedServiceInjection<?>> namedServiceInjections = new ArrayList<NamedServiceInjection<?>>(0);
        private List<ServiceListener<? super T>> listeners = new ArrayList<ServiceListener<? super T>>(0);
        private ServiceController.Mode initialMode = ServiceController.Mode.AUTOMATIC;
        private Location location;

        private Builder(final ServiceName name, final Value<? extends Service<T>> service) {
            if(name == null) throw new IllegalArgumentException("Name is required");
            if(service == null) throw new IllegalArgumentException("Service is required");
            this.name = name;
            this.service = service;
        }

        /**
         *
         * @param dependency
         * @return
         */
        public Builder<T> addDependency(ServiceName dependency) {
            if(dependency == null)
                throw new IllegalArgumentException("Dependency can not be null");

            dependencies.add(dependency);

            return this;
        }
        public Builder<T> addDependencies(Collection<ServiceName> dependencies) {
            if(dependencies == null)
                throw new IllegalArgumentException("Dependencies can not be null");

            this.dependencies.addAll(dependencies);

            return this;
        }

        public Builder<T> addDependencies(ServiceName... dependencies) {
            for(ServiceName d : dependencies)
                this.dependencies.add(d);

            return this;
        }

        public <I> Builder<T> addInjection(final Value<I> value, final Injector<I> injector) {
            injections.add(new ValueInjection<I>(value, injector));

            return this;
        }

        public <I> Builder<T> addInjection(final ServiceName dependency, final Injector<I> injector) {
            dependencies.add(dependency);
            namedServiceInjections.add(new NamedServiceInjection<I>(dependency, injector));

            return this;
        }

        public Builder<T> addListener(final ServiceListener<? super T> listener) {
            listeners.add(listener);
            return this;
        }

        public List<ServiceListener<? super T>> getListeners() {
            return listeners;
        }

        public Builder<T> setLocation(Location location) {
            this.location = location;

            return this;
        }

        public Builder<T> setInitialMode(ServiceController.Mode initialMode) {
            this.initialMode = initialMode;
            return this;
        }

        public ServiceDefinition<T> create() {
            final int dependenciesSize = dependencies.size();
            final ServiceName[] dependencies;
            if(dependenciesSize == 0) {
                dependencies = NO_DEPS;
            } else {
                dependencies = this.dependencies.toArray(new ServiceName[dependenciesSize]);
            }
            final int injectionsSize = injections.size();
            final ValueInjection<?>[] injections;
            if(injectionsSize == 0) {
                injections = NO_INJECTIONS;
            } else {
                injections = this.injections.toArray(new ValueInjection<?>[injectionsSize]);
            }
            final int namedServiceInjectionsSize = namedServiceInjections.size();
            final NamedServiceInjection<?>[] namedInjections;
            if (namedServiceInjectionsSize == 0) {
                namedInjections = NO_NAMED_INJECTIONS;
            } else {
                namedInjections = namedServiceInjections.toArray(new NamedServiceInjection<?>[namedServiceInjectionsSize]);
            }
            int listenersSize = listeners.size();
            final ServiceListener<?>[] listeners;
            if(listenersSize == 0) {
                listeners = NO_LISTENERS;
            } else {
                listeners = this.listeners.toArray(new ServiceListener<?>[listenersSize]);
            }
            return new ServiceDefinition<T>(name, initialMode, location, service, dependencies, injections, namedInjections, listeners);
        }
    }

    public ServiceName getName() {
        return name;
    }

    public ServiceName[] getDependencies() {
        return dependencies.clone();
    }

    ServiceName[] getDependenciesDirect() {
        return dependencies;
    }

    public ValueInjection<?>[] getInjections() {
        return injections.clone();
    }

    ValueInjection<?>[] getInjectionsDirect() {
        return injections;
    }

    public NamedServiceInjection<?>[] getNamedInjections() {
        return namedInjections.clone();
    }

    NamedServiceInjection<?>[] getNamedInjectionsDirect() {
        return namedInjections;
    }

    public ServiceListener<?>[] getListeners() {
        return listeners.clone();
    }

    public ServiceListener<?>[] getListenersDirect() {
        return listeners;
    }

    public ServiceController.Mode getInitialMode() {
        return initialMode;
    }

    public Location getLocation() {
        return location;
    }

    public Value<? extends Service<T>> getService() {
        return service;
    }

    @Override
    public String toString() {
        return String.format("ServiceDefinition{dependencies=%s, name='%s', initialMode=%s, location=%s}",
                Arrays.toString(dependencies),
                name,
                initialMode,
                location);
    }
}
