package org.jboss.msc.registry;

import java.util.ArrayList;
import java.util.List;
import org.jboss.msc.service.Location;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.value.Value;

import java.util.Collection;

/**
 * Class representing the definition of a services prior to the service being installed.
 *
 * @author John Bailey
 * @author Jason T. Greene
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class ServiceDefinition {
    private final ServiceName name;
    private final List<String> dependencies;
    private final ServiceController.Mode initialMode;
    private final Location location;
    private final Value<Service> service;

    private ServiceDefinition(ServiceName name, ServiceController.Mode initialMode, Location location, Value<Service> service, List<String> dependencies) {
        if (name == null) {
            throw new IllegalArgumentException("Name can not be null");
        }
        this.name = name;
        this.dependencies = dependencies;
        this.initialMode = initialMode;
        this.location = location;
        this.service = service;
    }
    
    public static Builder build(final String name, final Value<Service> service) {
        return new Builder(name, service);
    }
    
    public static final class Builder {
        private final String name;
        private final Value<Service> service;
        private List<String> dependencies = new ArrayList<String>(0);
        private ServiceController.Mode initialMode = ServiceController.Mode.AUTOMATIC;
        private Location location;


        private Builder(final String name, final Value<Service> service) {
            if(name == null) throw new IllegalArgumentException("Name is required");
            if(service == null) throw new IllegalArgumentException("Service is required");
            this.name = name;
            this.service = service;

        }
        
        public Builder addDependency(String dependency) {
            if (dependency == null)
                throw new IllegalArgumentException("Dependency can not be null");
            
            dependencies.add(dependency);
            
            return this;
        }
        
        public Builder addDependencies(Collection<String> dependencies)
        {
            if (dependencies == null)
                throw new IllegalArgumentException("Dependencies can not be null");
            
            this.dependencies.addAll(dependencies);
            
            return this;
        }
        
        public Builder addDependencies(String... dependencies)
        {
            for (String d : dependencies)
                this.dependencies.add(d);
            
            return this;
        }
        
        public Builder setLocation(Location location) {
            this.location = location;
            
            return this;
        }

        public Builder setInitialMode(ServiceController.Mode initialMode) {
            this.initialMode = initialMode;
            return this;
        }

        public ServiceDefinition create() {
            return new ServiceDefinition(ServiceName.create(name), initialMode, location, service, dependencies);
        }
    }

    public ServiceName getName() {
        return name;
    }

    public List<String> getDependencies() {
        return dependencies;
    }

    public ServiceController.Mode getInitialMode() {
        return initialMode;
    }

    public Location getLocation() {
        return location;
    }

    public Value<Service> getService() {
        return service;
    }

    @Override
    public String toString() {
        return "ServiceDefinition{" +
                "dependencies=" + dependencies +
                ", name='" + name + '\'' +
                ", initialMode=" + initialMode +
                ", location=" + location +
                '}';
    }
}
