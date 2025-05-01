/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2025, Red Hat, Inc., and individual contributors
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

package org.jboss.msc.service;

import org.jboss.msc.service.management.ServiceContainerMXBean;
import org.jboss.msc.service.management.ServiceStatus;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Collection;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Function;

/**
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
final class ServiceContainerMXBeanImpl implements ServiceContainerMXBean {

    private static final String LS = System.lineSeparator();
    private static final String DOUBLE_LS = LS.repeat(2);

    private final ConcurrentMap<ServiceName, ServiceRegistrationImpl> registry;
    private final String containerName;

    ServiceContainerMXBeanImpl(final String containerName, final ConcurrentMap<ServiceName, ServiceRegistrationImpl> registry) {
        this.containerName = containerName;
        this.registry = registry;
    }

    @Override
    public Set<String> queryValues() {
        final Set<ServiceName> values = registry.keySet();
        final Set<String> retVal = new TreeSet<>();
        for (ServiceName value : values) {
            retVal.add(value.getCanonicalName());
        }
        return retVal;
    }

    @Override
    public void dumpValues() {
        dumpValues(System.out);
    }

    @Override
    public String dumpValuesToString() {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        final PrintStream out = new PrintStream(baos, false, UTF_8);
        dumpValues(out);
        return baos.toString(UTF_8);
    }

    @Override
    public Set<String> queryServiceIds() {
        return queryServiceIds(Functions.ServiceIdIdentityFunction.INSTANCE);
    }

    @Override
    public void dumpServiceIds() {
        dumpServiceIds(null, Functions.ServiceIdIdentityFunction.INSTANCE, null, System.out);
    }

    @Override
    public String dumpServiceIdsToString() {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        final PrintStream out = new PrintStream(baos, false, UTF_8);
        dumpServiceIds(null, Functions.ServiceIdIdentityFunction.INSTANCE, null, out);
        return baos.toString(UTF_8);
    }

    @Override
    public Set<ServiceStatus> queryServices() {
        return queryServices(Functions.ServiceIdentityFunction.INSTANCE);
    }

    @Override
    public void dumpServices() {
        dumpServices(null, Functions.ServiceIdentityFunction.INSTANCE, null, System.out);
    }

    @Override
    public String dumpServicesToString() {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        final PrintStream out = new PrintStream(baos, false, UTF_8);
        dumpServices(null, Functions.ServiceIdentityFunction.INSTANCE, null, out);
        return baos.toString(UTF_8);
    }

    @Override
    public Set<ServiceStatus> queryServicesRequiringValue(final String value) {
        return queryServices(new Functions.ServiceRequiringValueFunction(value));
    }

    @Override
    public void dumpServicesRequiringValue(final String value) {
        dumpServices("requiring value", new Functions.ServiceRequiringValueFunction(value), value, System.out);
    }

    @Override
    public String dumpServicesRequiringValueToString(final String value) {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        final PrintStream out = new PrintStream(baos, false, UTF_8);
        dumpServices("requiring value", new Functions.ServiceRequiringValueFunction(value), value, out);
        return baos.toString(UTF_8);
    }

    @Override
    public Set<String> queryServiceIdsRequiringValue(final String value) {
        return queryServiceIds(new Functions.ServiceIdRequiringValueFunction(value));
    }

    @Override
    public void dumpServiceIdsRequiringValue(final String value) {
        dumpServiceIds("requiring value", new Functions.ServiceIdRequiringValueFunction(value), value, System.out);
    }

    @Override
    public String dumpServiceIdsRequiringValueToString(final String value) {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        final PrintStream out = new PrintStream(baos, false, UTF_8);
        dumpServiceIds("requiring value", new Functions.ServiceIdRequiringValueFunction(value), value, out);
        return baos.toString(UTF_8);
    }

    @Override
    public ServiceStatus queryServiceProvidingValue(final String value) {
        final Iterator<ServiceStatus> i = queryServices(new Functions.ServiceProvidingValueFunction(value)).iterator();
        return i.hasNext() ? i.next() : null;
    }

    @Override
    public void dumpServiceProvidingValue(final String value) {
        dumpServices("providing value", new Functions.ServiceProvidingValueFunction(value), value, System.out);
    }

    @Override
    public String dumpServiceProvidingValueToString(final String value) {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        final PrintStream out = new PrintStream(baos, false, UTF_8);
        dumpServices("providing value", new Functions.ServiceProvidingValueFunction(value), value, out);
        return baos.toString(UTF_8);
    }

    @Override
    public String queryServiceIdProvidingValue(final String value) {
        final Iterator<String> i = queryServiceIds(new Functions.ServiceIdProvidingValueFunction(value)).iterator();
        return i.hasNext() ? i.next() : null;
    }

    @Override
    public void dumpServiceIdProvidingValue(final String value) {
        dumpServiceIds("providing value", new Functions.ServiceIdProvidingValueFunction(value), value, System.out);
    }

    @Override
    public String dumpServiceIdProvidingValueToString(final String value) {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        final PrintStream out = new PrintStream(baos, false, UTF_8);
        dumpServiceIds("providing value", new Functions.ServiceIdProvidingValueFunction(value), value, out);
        return baos.toString(UTF_8);
    }

    @Override
    public Set<ServiceStatus> queryServicesMissingValue(final String value) {
        return queryServices(new Functions.ServiceMissingValueFunction(value));
    }

    @Override
    public void dumpServicesMissingValue(final String value) {
        dumpServices("missing value", new Functions.ServiceMissingValueFunction(value), value, System.out);
    }

    @Override
    public String dumpServicesMissingValueToString(final String value) {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        final PrintStream out = new PrintStream(baos, false, UTF_8);
        dumpServices("missing value", new Functions.ServiceMissingValueFunction(value), value, out);
        return baos.toString(UTF_8);
    }

    @Override
    public Set<String> queryServiceIdsMissingValue(final String value) {
        return queryServiceIds(new Functions.ServiceIdMissingValueFunction(value));
    }

    @Override
    public void dumpServiceIdsMissingValue(final String value) {
        dumpServiceIds("missing value", new Functions.ServiceIdMissingValueFunction(value), value, System.out);
    }

    @Override
    public String dumpServiceIdsMissingValueToString(final String value) {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        final PrintStream out = new PrintStream(baos, false, UTF_8);
        dumpServiceIds("missing value", new Functions.ServiceIdMissingValueFunction(value), value, out);
        return baos.toString(UTF_8);
    }

    @Override
    public ServiceStatus queryServiceById(final String id) {
        final Iterator<ServiceStatus> i = queryServices(new Functions.ServiceIdFunction(id)).iterator();
        return i.hasNext() ? i.next() : null;
    }

    @Override
    public void dumpServiceById(final String id) {
        dumpServices("of id", new Functions.ServiceIdFunction(id), id, System.out);
    }

    @Override
    public String dumpServiceByIdToString(final String id) {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        final PrintStream out = new PrintStream(baos, false, UTF_8);
        dumpServices("of id", new Functions.ServiceIdFunction(id), id, out);
        return baos.toString(UTF_8);
    }

    @Override
    public Set<ServiceStatus> queryServicesByState(final String state) {
        return queryServices(new Functions.ServiceStateFunction(state));
    }

    @Override
    public void dumpServicesByState(final String state) {
        dumpServices("in state", new Functions.ServiceStateFunction(state), state, System.out);
    }

    @Override
    public String dumpServicesByStateToString(final String state) {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        final PrintStream out = new PrintStream(baos, false, UTF_8);
        dumpServices("in state", new Functions.ServiceStateFunction(state), state, out);
        return baos.toString(UTF_8);
    }

    @Override
    public Set<String> queryServiceIdsByState(final String state) {
        return queryServiceIds(new Functions.ServiceIdStateFunction(state));
    }

    @Override
    public void dumpServiceIdsByState(final String state) {
        dumpServiceIds("in state", new Functions.ServiceIdStateFunction(state), state, System.out);
    }

    @Override
    public String dumpServiceIdsByStateToString(final String state) {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        final PrintStream out = new PrintStream(baos, false, UTF_8);
        dumpServiceIds("in state", new Functions.ServiceIdStateFunction(state), state, out);
        return baos.toString(UTF_8);
    }

    @Override
    public Set<ServiceStatus> queryServicesByMode(final String mode) {
        return queryServices(new Functions.ServiceModeFunction(mode));
    }

    @Override
    public void dumpServicesByMode(final String mode) {
        dumpServices("in mode", new Functions.ServiceModeFunction(mode), mode, System.out);
    }

    @Override
    public String dumpServicesByModeToString(final String mode) {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        final PrintStream out = new PrintStream(baos, false, UTF_8);
        dumpServices("in mode", new Functions.ServiceModeFunction(mode), mode, out);
        return baos.toString(UTF_8);
    }

    @Override
    public Set<String> queryServiceIdsByMode(final String mode) {
        return queryServiceIds(new Functions.ServiceIdModeFunction(mode));
    }

    @Override
    public void dumpServiceIdsByMode(final String mode) {
        dumpServiceIds("in mode", new Functions.ServiceIdModeFunction(mode), mode, System.out);
    }

    @Override
    public String dumpServiceIdsByModeToString(final String mode) {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        final PrintStream out = new PrintStream(baos, false, UTF_8);
        dumpServiceIds("in mode", new Functions.ServiceIdModeFunction(mode), mode, out);
        return baos.toString(UTF_8);
    }

    void dumpServices(final String conditionDescription, final Function<ServiceStatus, ServiceStatus> function, final String value, final PrintStream out) {
        final Collection<ServiceStatus> services = queryServices(function);
        synchronized (out) {
            if (conditionDescription != null) {
                out.printf("Number of services in container \"%s\" " + conditionDescription + " \"%s\" is: %d", containerName, value, services.size());
                out.print(DOUBLE_LS);
            } else {
                out.printf("Number of services in container \"%s\" is: %d", containerName, services.size());
                out.print(DOUBLE_LS);
            }
            for (ServiceStatus service : services) {
                out.print(service);
                out.print(LS);
            }
            out.print(LS);
            out.flush();
        }
    }

    private void dumpValues(final PrintStream out) {
        final Collection<String> values = queryValues();
        synchronized (out) {
            out.printf("Number of values in container \"%s\" is: %d", containerName, values.size());
            out.print(DOUBLE_LS);
            for (String value : values) {
                out.print(value);
                out.print(LS);
            }
            out.print(LS);
            out.flush();
        }
    }

    private void dumpServiceIds(final String conditionDescription, final Function<ServiceStatus, String> function, final String value, final PrintStream out) {
        final Collection<String> serviceIds = queryServiceIds(function);
        synchronized (out) {
            if (conditionDescription != null) {
                out.printf("Number of service identifiers in container \"%s\" " + conditionDescription + " \"%s\" is: %d", containerName, value, serviceIds.size());
                out.print(DOUBLE_LS);
            } else {
                out.printf("Number of service identifiers in container \"%s\" is: %d", containerName, serviceIds.size());
                out.print(DOUBLE_LS);
            }
            for (String serviceId : serviceIds) {
                out.print(serviceId);
                out.print(LS);
            }
            out.print(LS);
            out.flush();
        }
    }

    private Set<ServiceStatus> queryServices(final Function<ServiceStatus, ServiceStatus> function) {
        final Collection<ServiceRegistrationImpl> values = registry.values();
        final Set<ServiceStatus> retVal = new TreeSet<>();
        ServiceStatus service;
        ServiceControllerImpl<?> controller;
        for (ServiceRegistrationImpl value : values) {
            controller = value.getDependencyController();
            if (controller == null) continue;
            service = function.apply(controller.getStatus());
            if (service != null) retVal.add(service);
        }
        return retVal;
    }

    private Set<String> queryServiceIds(final Function<ServiceStatus, String> function) {
        final Collection<ServiceRegistrationImpl> values = registry.values();
        final Set<String> retVal = new TreeSet<>();
        String serviceId;
        ServiceControllerImpl<?> controller;
        for (ServiceRegistrationImpl value : values) {
            controller = value.getDependencyController();
            if (controller == null) continue;
            serviceId = function.apply(controller.getStatus());
            if (serviceId != null) retVal.add(serviceId);
        }
        return retVal;
    }
}
