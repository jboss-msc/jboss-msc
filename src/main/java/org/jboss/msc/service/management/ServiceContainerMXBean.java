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

import java.util.Set;

/**
 * The service container management bean interface.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
public interface ServiceContainerMXBean {

    /**
     * Gets all registered values.
     *
     * @return all registered values
     */
    Set<String> queryValues();

    /**
     * Dumps all registered values to system console.
     * The output has no particular standard format and may change over time.
     */
    void dumpValues();

    /**
     * Dumps all registered values to string.
     * The output has no particular standard format and may change over time.
     *
     * @return all registered values as string
     */
    String dumpValuesToString();

    /**
     * Gets all registered service ids.
     *
     * @return all registered service ids
     */
    Set<String> queryServiceIds();

    /**
     * Dumps all registered service ids to system console.
     * The output has no particular standard format and may change over time.
     */
    void dumpServiceIds();

    /**
     * Dumps all registered service ids to string.
     * The output has no particular standard format and may change over time.
     *
     * @return all registered service ids as string
     */
    String dumpServiceIdsToString();

    /**
     * Gets the statuses of all registered services.
     *
     * @return the statuses of all registered services
     */
    Set<ServiceStatus> queryServices();

    /**
     * Dumps the statuses of all registered services to system console.
     * The output has no particular standard format and may change over time.
     */
    void dumpServices();

    /**
     * Dumps the statuses of all registered services to string.
     * The output has no particular standard format and may change over time.
     *
     * @return the statuses of all registered services as string
     */
    String dumpServicesToString();

    /**
     * Gets the services that require the given value.
     *
     * @param value the name of the value
     * @return the services that require the given value
     */
    Set<ServiceStatus> queryServicesRequiringValue(String value);

    /**
     * Dumps the services that require the given value to system console.
     * The output has no particular standard format and may change over time.
     *
     * @param value the name of the value
     */
    void dumpServicesRequiringValue(String value);

    /**
     * Dumps the services that require the given value to string.
     * The output has no particular standard format and may change over time.
     *
     * @param value the name of the value
     * @return the services that require the given value as string
     */
    String dumpServicesRequiringValueToString(String value);

    /**
     * Gets the service ids that require the given value.
     *
     * @param value the name of the value
     * @return the service ids that require the given value
     */
    Set<String> queryServiceIdsRequiringValue(String value);

    /**
     * Dumps the service ids that require the given value to system console.
     * The output has no particular standard format and may change over time.
     *
     * @param value the name of the value
     */
    void dumpServiceIdsRequiringValue(String value);

    /**
     * Dumps the service ids that require the given value to string.
     * The output has no particular standard format and may change over time.
     *
     * @param value the name of the value
     * @return the service ids that require the given value as string
     */
    String dumpServiceIdsRequiringValueToString(String value);

    /**
     * Gets the service that provides the given value.
     *
     * @param value the name of the value
     * @return the service that provides the given value
     */
    ServiceStatus queryServiceProvidingValue(String value);

    /**
     * Dumps the service that provides the given value to system console.
     * The output has no particular standard format and may change over time.
     *
     * @param value the name of the value
     */
    void dumpServiceProvidingValue(String value);

    /**
     * Dumps the service that provides the given value to string.
     * The output has no particular standard format and may change over time.
     *
     * @param value the name of the value
     * @return the service that provides the given value as string
     */
    String dumpServiceProvidingValueToString(String value);

    /**
     * Gets the service id that provides the given value.
     *
     * @param value the name of the value
     * @return the service id that provides the given value
     */
    String queryServiceIdProvidingValue(String value);

    /**
     * Dumps the service id that provides the given value to system console.
     * The output has no particular standard format and may change over time.
     *
     * @param value the name of the value
     */
    void dumpServiceIdProvidingValue(String value);

    /**
     * Dumps the service id that provides the given value to string.
     * The output has no particular standard format and may change over time.
     *
     * @param value the name of the value
     * @return the service id that provides the given value as string
     */
    String dumpServiceIdProvidingValueToString(String value);

    /**
     * Gets the services missing the given value.
     *
     * @param value the name of the value
     * @return the services missing the given value
     */
    Set<ServiceStatus> queryServicesMissingValue(String value);

    /**
     * Dumps the services missing the given value to system console.
     * The output has no particular standard format and may change over time.
     *
     * @param value the name of the value
     */
    void dumpServicesMissingValue(String value);

    /**
     * Dumps the services missing the given value to string.
     * The output has no particular standard format and may change over time.
     *
     * @param value the name of the value
     * @return the services missing the given value as string
     */
    String dumpServicesMissingValueToString(String value);

    /**
     * Gets the service ids missing the given value.
     *
     * @param value the name of the value
     * @return the service ids missing the given value
     */
    Set<String> queryServiceIdsMissingValue(String value);

    /**
     * Dumps the service ids missing the given value to system console.
     * The output has no particular standard format and may change over time.
     *
     * @param value the name of the value
     */
    void dumpServiceIdsMissingValue(String value);

    /**
     * Dumps the service ids missing the given value to string.
     * The output has no particular standard format and may change over time.
     *
     * @param value the name of the value
     * @return the service ids missing the given value as string
     */
    String dumpServiceIdsMissingValueToString(String value);

    /**
     * Gets the service with given id.
     *
     * @param id the service runtime identification
     * @return the service with given id
     */
    ServiceStatus queryServiceById(String id);

    /**
     * Dumps the service with given id to system console.
     * The output has no particular standard format and may change over time.
     *
     * @param id the service runtime identification
     */
    void dumpServiceById(String id);

    /**
     * Dumps the service with given id to string.
     * The output has no particular standard format and may change over time.
     *
     * @param id the service runtime identification
     * @return the service with given id as string
     */
    String dumpServiceByIdToString(String id);

    /**
     * Gets the services in given state.
     *
     * @param state the name of the state
     * @return the services in given state
     */
    Set<ServiceStatus> queryServicesByState(String state);

    /**
     * Dumps the services in given state to system console.
     * The output has no particular standard format and may change over time.
     *
     * @param state the name of the state
     */
    void dumpServicesByState(String state);

    /**
     * Dumps the services in given state to string.
     * The output has no particular standard format and may change over time.
     *
     * @param state the name of the state
     * @return the services in given state as string
     */
    String dumpServicesByStateToString(String state);

    /**
     * Gets the service ids in given state.
     *
     * @param state the name of the state
     * @return the service ids in given state
     */
    Set<String> queryServiceIdsByState(String state);

    /**
     * Dumps the service ids in given state to system console.
     * The output has no particular standard format and may change over time.
     *
     * @param state the name of the state
     */
    void dumpServiceIdsByState(String state);

    /**
     * Dumps the service ids in given state to string.
     * The output has no particular standard format and may change over time.
     *
     * @param state the name of the state
     * @return the service ids in given state as string
     */
    String dumpServiceIdsByStateToString(String state);

    /**
     * Gets the services in given mode.
     *
     * @param mode the name of the mode
     * @return the services in given mode
     */
    Set<ServiceStatus> queryServicesByMode(String mode);

    /**
     * Dumps the services in given mode to system console.
     * The output has no particular standard format and may change over time.
     *
     * @param mode the name of the mode
     */
    void dumpServicesByMode(String mode);

    /**
     * Dumps the services in given mode to string.
     * The output has no particular standard format and may change over time.
     *
     * @param mode the name of the mode
     * @return the services in given mode as string
     */
    String dumpServicesByModeToString(String mode);

    /**
     * Gets the service ids in given mode.
     *
     * @param mode the name of the mode
     * @return the service ids in given mode
     */
    Set<String> queryServiceIdsByMode(String mode);

    /**
     * Dumps the service ids in given mode to system console.
     * The output has no particular standard format and may change over time.
     *
     * @param mode the name of the mode
     */
    void dumpServiceIdsByMode(String mode);

    /**
     * Dumps the service ids in given mode to string.
     * The output has no particular standard format and may change over time.
     *
     * @param mode the name of the mode
     * @return the service ids in given mode as string
     */
    String dumpServiceIdsByModeToString(String mode);

}
