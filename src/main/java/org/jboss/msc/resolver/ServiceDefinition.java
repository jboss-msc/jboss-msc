package org.jboss.msc.resolver;

import org.jboss.msc.service.Location;
import org.jboss.msc.service.ServiceController;

import java.util.*;

/**
 * Class representing the definition of a services prior to the service being installed.
 *
 * @author John Bailey
 */
public class ServiceDefinition {
    private final String name;
    private Set<String> dependencies;
    private ServiceController.Mode initialMode;
    private Location location;
    private boolean resolved;
    private boolean processed;

    public ServiceDefinition(final String name, String... dependencies) {
        this(name, Arrays.asList(dependencies));            
    }

    public ServiceDefinition(final String name, Collection<String> dependencies) {
        this.name = name;
        this.dependencies = new HashSet<String>(dependencies);
    }

    public String getName() {
        return name;
    }

    public Set<String> getDependencies() {
        return dependencies;
    }

    public void setDependencies(Set<String> dependencies) {
        this.dependencies = dependencies;
    }

    public ServiceController.Mode getInitialMode() {
        return initialMode;
    }

    public void setInitialMode(ServiceController.Mode initialMode) {
        this.initialMode = initialMode;
    }

    public Location getLocation() {
        return location;
    }

    public void setLocation(Location location) {
        this.location = location;
    }

    public boolean isProcessed() {
        return processed;
    }

    public void setProcessed(boolean processed) {
        this.processed = processed;
    }

    public boolean isResolved() {
        return resolved;
    }

    public void setResolved(boolean resolved) {
        this.resolved = resolved;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ServiceDefinition that = (ServiceDefinition) o;

        if (name != null ? !name.equals(that.name) : that.name != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return name != null ? name.hashCode() : 0;
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
