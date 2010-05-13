package org.jboss.msc.registry;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.jboss.msc.service.Location;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
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

    private static final String[] NO_DEPS = new String[0];

    private ServiceDefinition(ServiceName name, ServiceController.Mode initialMode, Location location, Value<? extends Service<T>> service, String[] dependencies) {
        if (name == null) {
            throw new IllegalArgumentException("Name can not be null");
        }
        this.name = name;
        this.dependencies = dependencies;
        this.initialMode = initialMode;
        this.location = location;
        this.service = service;
    }
    
    public static <T> Builder<T> build(final ServiceName name, final Value<? extends Service<T>> service) {
        return new Builder<T>(name, service);
    }
    
    public static final class Builder<T> {
        private final ServiceName name;
        private final Value<? extends Service<T>> service;
        private List<String> dependencies = new ArrayList<String>(0);
        private ServiceController.Mode initialMode = ServiceController.Mode.AUTOMATIC;
        private Location location;


        private Builder(final ServiceName name, final Value<? extends Service<T>> service) {
            if(name == null) throw new IllegalArgumentException("Name is required");
            if(service == null) throw new IllegalArgumentException("Service is required");
            this.name = name;
            this.service = service;

        }
        
        public Builder<T> addDependency(String dependency) {
            if (dependency == null)
                throw new IllegalArgumentException("Dependency can not be null");
            
            dependencies.add(dependency);
            
            return this;
        }
        
        public Builder<T> addDependencies(Collection<String> dependencies)
        {
            if (dependencies == null)
                throw new IllegalArgumentException("Dependencies can not be null");
            
            this.dependencies.addAll(dependencies);
            
            return this;
        }
        
        public Builder<T> addDependencies(String... dependencies)
        {
            for (String d : dependencies)
                this.dependencies.add(d);
            
            return this;
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
            final int size = dependencies.size();
            if (size == 0) {
                return new ServiceDefinition<T>(name, initialMode, location, service, NO_DEPS);
            } else {
                return new ServiceDefinition<T>(name, initialMode, location, service, dependencies.toArray(new String[size]));
            }
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
