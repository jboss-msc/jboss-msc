/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

package org.jboss.msc.service.management;

import java.beans.ConstructorProperties;
import java.io.Serializable;

/**
 * A representation of the current status of some service.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public class ServiceStatus implements Serializable {

    private static final long serialVersionUID = 6538576441150451665L;

    private final String serviceName;
    private final String[] aliases;
    private final String serviceClassName;
    private final String modeName;
    private final String stateName;
    private final String substateName;
    private final String[] dependencies;
    private final boolean dependencyFailed;
    private final boolean dependencyMissing;
    private final String parentName;

    /**
     * Construct a new instance.
     *
     * @param parentName the name of the parent
     * @param serviceName the service name
     * @param aliases the aliases of this service
     * @param serviceClassName the name of the service class
     * @param modeName the service mode name
     * @param stateName the service state name
     * @param substateName the internal service substate name
     * @param dependencies the list of dependencies for this service
     * @param dependencyFailed {@code true} if some dependency is failed
     * @param dependencyMissing {@code true} if some dependency is missing
     */
    @ConstructorProperties({"parentName", "serviceName", "serviceClassName", "modeName", "stateName", "substateName", "dependencies", "dependencyFailed", "dependencyMissing"})
    public ServiceStatus(final String parentName, final String serviceName, final String[] aliases, final String serviceClassName, final String modeName, final String stateName, final String substateName, final String[] dependencies, final boolean dependencyFailed, final boolean dependencyMissing) {
        if (serviceName == null) {
            throw new IllegalArgumentException("serviceName is null");
        }
        if (aliases == null) {
            throw new IllegalArgumentException("aliases is null");
        }
        if (serviceClassName == null) {
            throw new IllegalArgumentException("serviceClassName is null");
        }
        if (modeName == null) {
            throw new IllegalArgumentException("modeName is null");
        }
        if (stateName == null) {
            throw new IllegalArgumentException("stateName is null");
        }
        if (substateName == null) {
            throw new IllegalArgumentException("substateName is null");
        }
        if (dependencies == null) {
            throw new IllegalArgumentException("dependencies is null");
        }
        this.serviceName = serviceName;
        this.aliases = aliases;
        this.serviceClassName = serviceClassName;
        this.modeName = modeName;
        this.stateName = stateName;
        this.substateName = substateName;
        this.dependencies = dependencies;
        this.dependencyFailed = dependencyFailed;
        this.dependencyMissing = dependencyMissing;
        this.parentName = parentName;
    }

    /**
     * Get the service name, as a string.
     *
     * @return the service name
     */
    public String getServiceName() {
        return serviceName;
    }

    /**
     * Get the service aliases, if any, as strings.  If there are no aliases, an empty array is returned.
     *
     * @return the service aliases
     */
    public String[] getAliases() {
        return aliases;
    }

    /**
     * Get the service class name, or {@code "&lt;unknown&gt;"} if not known.
     *
     * @return the service class name
     */
    public String getServiceClassName() {
        return serviceClassName;
    }

    /**
     * Get the service mode, as a string.
     *
     * @return the service mode
     */
    public String getModeName() {
        return modeName;
    }

    /**
     * Get the service state, as a string.
     *
     * @return the service state
     */
    public String getStateName() {
        return stateName;
    }

    /**
     * Get the service internal substate, as a string.
     *
     * @return the service substate
     */
    public String getSubstateName() {
        return substateName;
    }

    /**
     * The list of dependency names for this service.
     *
     * @return the dependency names
     */
    public String[] getDependencies() {
        return dependencies;
    }

    /**
     * Determine if some dependency was failed at the time of the query.
     *
     * @return {@code true} if some dependency was failed
     */
    public boolean isDependencyFailed() {
        return dependencyFailed;
    }

    /**
     * Determine if some dependency was missing at the time of the query.
     *
     * @return {@code true} if some dependency was missing
     */
    public boolean isDependencyMissing() {
        return dependencyMissing;
    }

    /**
     * Get a string representation of the current status.
     *
     * @return a string representation
     */
    public String toString() {
        final StringBuilder builder = new StringBuilder();
        builder.append("Service \"").append(serviceName).append("\" ");
        final String[] aliases = this.aliases;
        if (aliases.length > 0) {
            builder.append("(aliases: ");
            for (int i = 0; i < aliases.length; i++) {
                builder.append(aliases[i]);
                if (i < aliases.length - 1) {
                    builder.append(", ");
                }
            }
            builder.append(") ");
        }
        builder.append("(class ").append(serviceClassName).append(')');
        builder.append(" mode ").append(modeName);
        builder.append(" state ").append(stateName);
        if (! stateName.equals(substateName)) {
            builder.append(" (").append(substateName).append(')');
        }
        final String[] dependencies = this.dependencies;
        final int dependenciesLength = dependencies.length;
        if (dependenciesLength > 0) {
            builder.append(" (dependencies: ");
            for (int i = 0; i < dependenciesLength; i++) {
                builder.append(dependencies[i]);
                if (i < dependenciesLength - 1) {
                    builder.append(", ");
                }
            }
            builder.append(")");
        }
        if (dependencyFailed) {
            builder.append(" (has failed dependency)");
        }
        if (dependencyMissing) {
            builder.append(" (has missing dependency)");
        }
        return builder.toString();
    }

    /**
     * Get the name of the parent service, if any.
     *
     * @return the parent name
     */
    public String getParentName() {
        return parentName;
    }
}
