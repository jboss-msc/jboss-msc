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

import org.jboss.msc.service.ServiceController.State;

/**
 * A ServiceProperty represents a characteristic or criteria that can be met by a service. 
 * 
 * @author <a href="mailto:flavia.rainone@jboss.com">Flavia Rainone</a>
 */
enum ServiceProperty {
    /**
     * The service failed to start property.
     */
    FAILED_TO_START() {
        boolean holdsFor(ServiceRegistrationImpl service) {
            ServiceInstanceImpl<?> instance = service.getInstance();
            return instance != null && instance.getState() == State.START_FAILED;
        }

        void notifyAffectedDependents(Iterable<ServiceInstanceImpl<?>> dependents) {
            for (ServiceInstanceImpl<?> dependent: dependents) {
                dependent.dependencyFailed();
            }
        }

        void notifyClearedDependents(Iterable<ServiceInstanceImpl<?>> dependents) {
            for (ServiceInstanceImpl<?> dependent: dependents) {
                dependent.dependencyFailureCleared();
            }
        }
    },
    /**
     * The uninstalled service property.
     */
    UNINSTALLED () {
        boolean holdsFor(ServiceRegistrationImpl service) {
            return service.getInstance() == null;
        }

        void notifyAffectedDependents(Iterable<ServiceInstanceImpl<?>> dependents) {
            for (ServiceInstanceImpl<?> dependent: dependents) {
                dependent.dependencyUninstalled();
            }
        }

        void notifyClearedDependents(Iterable<ServiceInstanceImpl<?>> dependents) {
            for (ServiceInstanceImpl<?> dependent: dependents) {
                dependent.dependencyInstalled();
            }
        }
    };

    /**
     * Returns {@code true} if this property holds for {@code service}.
     * @param service the service whose internal state will be analyzed in order to determine if it
     *                meets the specified criteria
     *
     * @return {@code true} if {@code service} has the characteristic determined by this property
     */
    abstract boolean holdsFor(ServiceRegistrationImpl service);

    /**
     * Notify dependents that one or more of its dependencies has this property.
     *  
     * @param dependents the dependents to be notified
     */
    abstract void notifyAffectedDependents(Iterable<ServiceInstanceImpl<?>> dependents);

    /**
     * Notify dependents that none of its dependencies has this property.
     * 
     * @param dependents the dependents to be notified.
     */
    abstract void notifyClearedDependents(Iterable<ServiceInstanceImpl<?>> dependents);
}