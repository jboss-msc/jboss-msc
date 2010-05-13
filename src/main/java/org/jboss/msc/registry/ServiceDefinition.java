package org.jboss.msc.registry;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.Location;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ValueInjection;
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
    private final String[] dependencies;
    private final ServiceController.Mode initialMode;
    private final Location location;
    private final Value<? extends Service<T>> service;
    private final ValueInjectionBuilder[] injectionBuilders;

    private static final String[] NO_DEPS = new String[0];
    private static final ValueInjectionBuilder[] NO_INJECTIONS = new ValueInjectionBuilder[0];

    private ServiceDefinition(ServiceName name, ServiceController.Mode initialMode, Location location, Value<? extends Service<T>> service, String[] dependencies, ValueInjectionBuilder[] injectionBuilders) {
        if(name == null) {
            throw new IllegalArgumentException("Name can not be null");
        }
        this.name = name;
        this.dependencies = dependencies;
        this.initialMode = initialMode;
        this.location = location;
        this.service = service;
        this.injectionBuilders = injectionBuilders;
    }

    public static <T> Builder<T> build(final ServiceName name, final Value<? extends Service<T>> service) {
        return new Builder<T>(name, service);
    }

    public static final class Builder<T> {
        private final ServiceName name;
        private final Value<? extends Service<T>> service;
        private List<String> dependencies = new ArrayList<String>(0);
        private List<ValueInjectionBuilder> injections = new ArrayList<ValueInjectionBuilder>(0);
        private ServiceController.Mode initialMode = ServiceController.Mode.AUTOMATIC;
        private Location location;


        private Builder(final ServiceName name, final Value<? extends Service<T>> service) {
            if(name == null) throw new IllegalArgumentException("Name is required");
            if(service == null) throw new IllegalArgumentException("Service is required");
            this.name = name;
            this.service = service;

        }

        public Builder<T> addDependency(String dependency) {
            if(dependency == null)
                throw new IllegalArgumentException("Dependency can not be null");

            dependencies.add(dependency);

            return this;
        }

        public Builder<T> addDependencies(Collection<String> dependencies) {
            if(dependencies == null)
                throw new IllegalArgumentException("Dependencies can not be null");

            this.dependencies.addAll(dependencies);

            return this;
        }

        public Builder<T> addDependencies(String... dependencies) {
            for(String d : dependencies)
                this.dependencies.add(d);

            return this;
        }

        public <I> Builder<T> addInjection(final Value<I> value, final Injector<I> injector) {
            injections.add(new ValueInjectionBuilder<I>(value, injector));

            return this;
        }

        public <I> Builder<T> addInjection(final Value<I> value, final Injector<I> injector, final String dependency) {
            if(!dependencies.contains(dependency))
                dependencies.add(dependency); // HMMM, what if the deps are added after the injections

            return addInjection(value, injector);
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
            final String[] dependencies;
            if(dependenciesSize == 0) {
                dependencies = NO_DEPS;
            } else {
                dependencies = this.dependencies.toArray(new String[dependenciesSize]);
            }
            final int injectionsSize = injections.size();
            final ValueInjectionBuilder[] injections;
            if(injectionsSize == 0) {
                injections = NO_INJECTIONS;
            } else {
                injections = this.injections.toArray(new ValueInjectionBuilder[injectionsSize]);
            }
            return new ServiceDefinition<T>(name, initialMode, location, service, dependencies, injections);
        }
    }

    public ServiceName getName() {
        return name;
    }

    public String[] getDependencies() {
        return dependencies.clone();
    }

    String[] getDependenciesDirect() {
        return dependencies;
    }

    public ValueInjectionBuilder[] getInjections() {
        return injectionBuilders;
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

    public static class ValueInjectionBuilder<T> {
        private Injector<T> injector;
        private Value<T> value;

        ValueInjectionBuilder(Value<T> value, Injector<T> injector) {
            this.injector = injector;
            this.value = value;
        }

        public ValueInjection<T> create() {
            return new ValueInjection<T>(value, injector);
        }
    }
}
