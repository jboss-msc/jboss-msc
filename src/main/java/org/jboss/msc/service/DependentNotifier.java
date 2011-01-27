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

package org.jboss.msc.service;

import java.lang.reflect.Array;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * After service installation is performed, this visitor can be used to traverse the dependency graph
 * selecting dependents of services that have a specific {@link ServiceProperty property} and notifying
 * them of their current status.<p>
 * Notice the selection of dependents is performed transitively, i.e., a dependent is selected as affected
 * by a property if and only if one of its dependencies is either affected by it or {@link
 * ServiceProperty#holdsFor(ServiceRegistrationImpl) has} the defined property itself. On {@link #finish()},
 * all selected dependents are notified they have one or more dependencies with the specified property.
 * Rejected dependents are also notified their dependencies are cleared in relation to the property.
 * <p>Multiple property notification is supported and should be the approach of choice when more than one
 * property needs to be verified. This will result in more than one property  being verified in a single traversal,
 * thus saving the cost of performing multiple traversals through the dependency graph.
 * <p>Cycles detected during traversal can be retrieved by calling {@link #getDetectedCycles()}.
 * 
 * @author <a href="mailto:flavia.rainone@jboss.com">Flavia Rainone</a>
 */
final class DependentNotifier implements Visitor<boolean[]> {
    // services selected as dependents (direct or not) of services that have a specific property
    private final HashSet<ServiceControllerImpl<?>>[] selectedServices;
    // cycles detected during visit
    private final Collection<List<ServiceName>> cycles;
    // all dependents of services having these properties will be selected (each one in the corresponding
    // element of selectedServices array)
    private final ServiceProperty[] properties;
    // visit path of traversal
    private final Deque<ServiceVisitInfo> visitPath;
    // services already visited
    private final Set<ServiceControllerImpl<?>> visited;
    // services being currently visited
    private final Set<ServiceControllerImpl<?>> beingVisited;
    // shortcut for rejecting a service for all properties
    private final boolean[] falseForAllProps;

    @SuppressWarnings("unchecked")
    DependentNotifier(ServiceProperty... properties) {
        this.properties = properties;
        selectedServices = (HashSet<ServiceControllerImpl<?>>[]) Array.newInstance(HashSet.class, properties.length);
        for (int i = 0; i < properties.length; i++) {
            selectedServices[i] = new HashSet<ServiceControllerImpl<?>>();
        }
        cycles = new ArrayList<List<ServiceName>>();
        visitPath = new ArrayDeque<ServiceVisitInfo>();
        visited = new HashSet<ServiceControllerImpl<?>>();
        beingVisited = new HashSet<ServiceControllerImpl<?>>();
        falseForAllProps = new boolean[properties.length];
    }

    /**
     * Returns a collection of all cycles detected during dependencies visit.<p>
     * Every cycle is represented by a list of the service names involved in the cycle. The names
     * are contained in dependency order, whereas the first and last element are always the same, to
     * indicate a dependency link that closes the cycle.<p>
     * An example of result: {{A, B, C, A}, {A, B, A}}
     * 
     * @return a collection containing the cycles detected during the dependency graph traversal.
     */
    Collection<List<ServiceName>> getDetectedCycles() {
        return cycles;
    }

    @Override
    public void finish() {
        if (properties.length > 1) {
            for (int i = 1; i < properties.length; i++) {
                properties[i].notifyAffectedDependents(selectedServices[i]);
                HashSet<ServiceControllerImpl<?>> rejectedServices = new HashSet<ServiceControllerImpl<?>>();
                rejectedServices.addAll(visited);
                rejectedServices.removeAll(selectedServices[i]);
                properties[i].notifyClearedDependents(rejectedServices);
            }
        }
        properties[0].notifyAffectedDependents(selectedServices[0]);
        visited.removeAll(selectedServices[0]);
        properties[0].notifyClearedDependents(visited);
    }

    @Override
    public boolean[] visit(ServiceRegistrationImpl service) {
        if (service == null) {
            return falseForAllProps;
        }
        final ServiceControllerImpl<?> instance;
        final boolean[] selectDependents = new boolean[properties.length];
        synchronized(service) {
            instance = service.getInstance();
            for (int i = 0; i < selectDependents.length; i++) {
                selectDependents[i] = properties[i].holdsFor(service);
            }
        }
        // visit instance if it is installed
        if (instance != null) {
            boolean[] newSelect = visit(instance);
            for (int i = 0; i < selectDependents.length; i++) {
                selectDependents[i] = newSelect[i] || selectDependents[i];
            }
        }
        return selectDependents;
    }

    private boolean[] visit(ServiceControllerImpl<?> service) {
        // check if instance has already been visited
        if (visited.contains(service)) {
            boolean select[] = new boolean[properties.length];
            for (int i = 0; i < select.length; i++) {
                select[i] = selectedServices[i].contains(service);
            }
            // select the dependents only if this service is in the selected set
            return select;
        }
        // if this instance is currently being visited we have a cycle
        if (!beingVisited.add(service)) {
            recordCycle(service);
            return falseForAllProps;
        }
        // proceed with service instance visit
        final ServiceVisitInfo serviceVisitInfo = new ServiceVisitInfo(service);
        // add the instance to visit path for future reference
        visitPath.addLast(serviceVisitInfo);
        // recursively visit dependencies
        final boolean[] selectService = new boolean[properties.length];
        for (Dependency dependency: service.getDependencies()) {
            boolean[] newSelect = dependency.accept(this);
            for (int i = 0; i < selectService.length; i++) {
                selectService[i] = newSelect[i] || selectService[i];
            }
        }
        // select this service if needed, for each property being analysed
        for (int i = 0; i < selectService.length; i++) {
            if (selectService[i]) {
                serviceVisitInfo.selectService(selectedServices[i]);
            }
        }
        // move service from visitPath/beingVisited to visited collection
        visitPath.removeLast();
        beingVisited.remove(service);
        visited.add(service);
        // let dependents know if they should be transitively selected
        return selectService;
    }

    private void recordCycle(ServiceControllerImpl<?> service) {
        final List<ServiceName> cycleServiceNames = new ArrayList<ServiceName>();
        // iterate through cycle recursively, recording the service names and updating visitPath information on the way
        recordCycle(service, cycleServiceNames, visitPath.descendingIterator());
        // add the service name to end of cycle, so we can have something like A, B, C, A instead of A, B, C only
        cycleServiceNames.add(service.getPrimaryRegistration().getName());
        cycles.add(cycleServiceNames);
    }

    private CycleInfo recordCycle(ServiceControllerImpl<?> currentService, List<ServiceName> cycleServiceNames, Iterator<ServiceVisitInfo> pathIterator) {
        assert pathIterator.hasNext();
        final ServiceVisitInfo visitInfo = pathIterator.next();
        final ServiceControllerImpl<?> serviceInstance = visitInfo.getService();
        final CycleInfo cycleInfo; 
        // found the cycle head
        if (serviceInstance == currentService) {
            cycleInfo = new CycleInfo(visitInfo);
        } else {
            // recursively iterate until we find the cycle head
            cycleInfo = recordCycle(currentService, cycleServiceNames, pathIterator);
        }
        // record service name
        cycleServiceNames.add(serviceInstance.getName());
        // update cycle info for visitInfo
        return visitInfo.joinCycle(cycleInfo);
    }

    private class ServiceVisitInfo {
        private CycleInfo cycle;
        private ServiceControllerImpl<?> service;

        public ServiceVisitInfo(ServiceControllerImpl<?> service) {
            this.service = service;
        }

        public ServiceControllerImpl<?> getService() {
            return service;
        }

        public void selectService(HashSet<ServiceControllerImpl<?>> selectedServices) {
            if (cycle != null && cycle.getCycleHead() == this) {
                selectedServices.addAll(cycle.getCycleNodes());
            }
            else {
                selectedServices.add(service);
            }
        }

        CycleInfo joinCycle(CycleInfo newCycle) {
            if (cycle == null) {
                cycle = newCycle;
                cycle.addCycleNode(this);
            } else {
                cycle = cycle.join(newCycle);
            }
            return cycle;
        }
    }

    private static class CycleInfo {
        private final ServiceVisitInfo cycleHead;
        private final Set<ServiceControllerImpl<?>> cycleNodes;

        CycleInfo(ServiceVisitInfo cycleHead) {
            this.cycleHead = cycleHead;
            cycleNodes = new HashSet<ServiceControllerImpl<?>>();
        }

        ServiceVisitInfo getCycleHead() {
            return cycleHead;
        }

        void addCycleNode(ServiceVisitInfo serviceVisitInfo) {
            cycleNodes.add(serviceVisitInfo.getService());
        }

        Set<ServiceControllerImpl<?>> getCycleNodes() {
            return cycleNodes;
        }

        CycleInfo join(CycleInfo cycle) {
            if (cycleNodes.contains(cycle.cycleHead.getService())) {
                cycleNodes.addAll(cycle.cycleNodes);
                return this;
            }
            cycle.cycleNodes.addAll(cycleNodes);
            return cycle;
        }
    }
}