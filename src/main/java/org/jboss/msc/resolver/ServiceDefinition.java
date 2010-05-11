package org.jboss.msc.resolver;

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
    private final String name;
    private final Set<String> dependencies;
    private final ServiceController.Mode initialMode;
    private final Location location;
    private boolean resolved;
    private boolean processed;

    private ServiceDefinition(String name, ServiceController.Mode initialMode, Location location, Set<String> dependencies) {
    	if (name == null)
    		throw new IllegalArgumentException("Name can not be null");
    	
        this.name = name;
        this.dependencies = new HashSet<String>(dependencies);
        this.initialMode = ServiceController.Mode.AUTOMATIC;
        this.location = location;
    }
    
    public static ServiceDefinition create(String name, ServiceController.Mode initialMode, Location location, Set<String> dependencies) {
    	return new ServiceDefinition(name, initialMode, location, dependencies);
    }
    
    public static ServiceDefinition create(String name, ServiceController.Mode initialMode, Location location, String... dependencies) {
    	return create(name, initialMode, location, new HashSet<String>(Arrays.asList(dependencies)));
    }
    
    public static ServiceDefinition create(String name, Location location, String... dependencies) {
    	return create(name, ServiceController.Mode.AUTOMATIC, location, new HashSet<String>(Arrays.asList(dependencies)));
    }
    
    public static ServiceDefinition create(String name, String... dependencies) {
    	return create(name, ServiceController.Mode.AUTOMATIC, null, new HashSet<String>(Arrays.asList(dependencies)));
    }

    public String getName() {
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

    public boolean isProcessed() {
        return processed;
    }

    void setProcessed(boolean processed) {
        this.processed = processed;
    }

    public boolean isResolved() {
        return resolved;
    }

    void setResolved(boolean resolved) {
        this.resolved = resolved;
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
