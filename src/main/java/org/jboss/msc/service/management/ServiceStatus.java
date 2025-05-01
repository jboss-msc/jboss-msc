/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2022, Red Hat, Inc., and individual contributors
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
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
public final class ServiceStatus implements Serializable, Comparable<ServiceStatus> {

    private static final long serialVersionUID = 2L;
    private static final String[] EMPTY_STRING_ARRAY = new String[0];

    private final String id;
    private final String parentId;
    private final String[] childrenIds;
    private final String[] requiredValues;
    private final String[] providedValues;
    private final String[] missingValues;
    private final String mode;
    private final String state;
    private final String exception;

    /**
     * Constructs a new instance.
     *
     * @param id runtime identification of service class
     * @param parentId runtime identification of parent service class
     * @param childrenIds runtime identification of children services classes
     * @param requiredValues required values by this service
     * @param providedValues provided values by this service
     * @param missingValues missing values of this service
     * @param mode the service mode
     * @param state the service state
     * @param exception the start failure reason
     */
    @ConstructorProperties({"id", "parentId", "childrenIds", "requiredValues", "providedValues", "missingValues", "serviceMode", "serviceState", "startException"})
    public ServiceStatus(final String id, final String parentId, final String[] childrenIds, final String[] requiredValues, final String[] providedValues, final String[] missingValues, final String mode, final String state, final String exception) {
        this.id = id;
        this.parentId = parentId;
        this.childrenIds = childrenIds != null ? childrenIds : EMPTY_STRING_ARRAY;
        this.requiredValues = requiredValues != null ? requiredValues : EMPTY_STRING_ARRAY;
        this.providedValues = providedValues != null ? providedValues : EMPTY_STRING_ARRAY;
        this.missingValues = missingValues != null ? missingValues : EMPTY_STRING_ARRAY;
        this.mode = mode;
        this.state = state;
        this.exception = exception;
    }

    /**
     * Get runtime identification of service class
     *
     * @return the id
     */
    public String getId() {
        return id;
    }

    /**
     * Get runtime identification of parent service class
     *
     * @return the parent id
     */
    public String getParentId() {
        return parentId;
    }

    /**
     * Get runtime identification of child service classes
     *
     * @return the children ids
     */
    public String[] getChildrenIds() {
        return childrenIds;
    }

    /**
     * The list of required values by this service.
     *
     * @return the required value names
     */
    public String[] getRequiredValues() {
        return requiredValues;
    }

    /**
     * The list of provided values by this service.
     *
     * @return the provided value names
     */
    public String[] getProvidedValues() {
        return providedValues;
    }

    /**
     * The list of missing values of this service.
     *
     * @return the missing value names
     */
    public String[] getMissingValues() {
        return missingValues;
    }

    /**
     * Get the service mode, as a string.
     *
     * @return the service mode
     */
    public String getMode() {
        return mode;
    }

    /**
     * Get the service state, as a string.
     *
     * @return the service state
     */
    public String getState() {
        return state;
    }

    /**
     * Get the service start exception.
     *
     * @return the service start exception
     */
    public String getException() {
        return exception;
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public boolean equals(final Object o) {
        if (!(o instanceof ServiceStatus)) return false;
        final ServiceStatus other = (ServiceStatus) o;
        return id.equals(other.id);
    }

    @Override
    public int compareTo(final ServiceStatus s) {
        return id.compareTo(s.id);
    }

    @Override
    public String toString() {
        final String ls = System.lineSeparator();
        final String indentation = " ".repeat(4);
        final StringBuilder sb = new StringBuilder();
        sb.append("Service").append(ls);
        sb.append(indentation).append("id: ").append(id).append(ls);
        if (parentId != null) {
            sb.append(indentation).append("parent id: ").append(parentId).append(ls);
        }
        if (childrenIds.length > 0) {
            sb.append(indentation).append("children ids: ").append(ls);
            for (String childrenId : childrenIds) {
                sb.append(indentation).append(indentation).append(childrenId).append(ls);
            }
        }
        if (requiredValues.length > 0) {
            sb.append(indentation).append("requires: ").append(ls);
            for (String requiredValue : requiredValues) {
                sb.append(indentation).append(indentation).append(requiredValue).append(ls);
            }
        }
        if (providedValues.length > 0) {
            sb.append(indentation).append("provides: ").append(ls);
            for (String providedValue : providedValues) {
                sb.append(indentation).append(indentation).append(providedValue).append(ls);
            }
        }
        if (missingValues.length > 0) {
            sb.append(indentation).append("missing: ").append(ls);
            for (String missingValue : missingValues) {
                sb.append(indentation).append(indentation).append(missingValue).append(ls);
            }
        }
        sb.append(indentation).append("mode: ").append(mode).append(ls);
        sb.append(indentation).append("state: ").append(state).append(ls);
        if (exception != null && !exception.isEmpty()) {
            sb.append(indentation).append("exception: ").append(exception).append(ls);
        }
        return sb.toString();
    }
}
