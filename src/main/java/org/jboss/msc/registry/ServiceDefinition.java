/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
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
    
    public static ServiceDefinition create(String name, ServiceController.Mode initialMode, Location location, Set<String> dependencies) {
    	return new ServiceDefinition(ServiceName.create(name), initialMode, location, dependencies);
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
