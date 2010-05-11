package org.jboss.msc.registry;

import org.jboss.msc.service.Location;
import org.jboss.msc.service.ServiceController;

import java.util.*;

/**
 * Class representing the definition of a services prior to the service being installed.
 *
 * @author John Bailey
 * @author Jason T. Greene
 */
public final class ServiceDefinition {
    private final ServiceName name;
    private final Set<String> dependencies;
    private final ServiceController.Mode initialMode;
    private final Location location;
    
    private ServiceDefinition(ServiceName name, ServiceController.Mode initialMode, Location location, Set<String> dependencies) {
    	if (name == null)
    		throw new IllegalArgumentException("Name can not be null");
    	
        this.name = name;
        this.dependencies = new HashSet<String>(dependencies);
        this.initialMode = ServiceController.Mode.AUTOMATIC;
        this.location = location;
    }
    
    public static Builder build() {
        return new Builder();
    }
    
    public static final class Builder {
        private String name;
        private Set<String> dependencies = new HashSet<String>();
        private ServiceController.Mode initialMode = ServiceController.Mode.AUTOMATIC;
        private Location location;
        
        public Builder setName(String name) {
            this.name = name;
            
            return this;
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
        
        public ServiceDefinition create() {
            return new ServiceDefinition(ServiceName.create(name), initialMode, location, dependencies);
        }
    }

    public ServiceName getName() {
        return name;
    }

    public Set<String> getDependencies() {
        return dependencies;
    }

    public ServiceController.Mode getInitialMode() {
        return initialMode;
    }

    public Location getLocation() {
        return location;
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
