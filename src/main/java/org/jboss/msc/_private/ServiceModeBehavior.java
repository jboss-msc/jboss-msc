/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jboss.msc._private;

import org.jboss.msc.service.ServiceMode;

/**
 * Behaviors of service modes.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 * @author <a href="mailto:frainone@redhat.com">Flavia Rainone</a>
 */
abstract class ServiceModeBehavior {

    /**
     * Behavior for {@link ServiceMode#ACTIVE}.
     */
    private static final ServiceModeBehavior ACTIVE = new ServiceModeBehavior() {
        boolean shouldStart(ServiceController<?> service) {
            return true;
        }

        boolean shouldStop(ServiceController<?> service) {
            return false;
        }

        Demand shouldDemandDependencies() {
            return Demand.ALWAYS;
        }
    };

    /**
     * Behavior for {@link ServiceMode#LAZY}.
     */
    private static final ServiceModeBehavior LAZY = new ServiceModeBehavior() {
        boolean shouldStart(ServiceController<?> service) {
            return service.isUpDemanded();
        }

        boolean shouldStop(ServiceController<?> service) {
            return false;
        }

        Demand shouldDemandDependencies() {
            return Demand.PROPAGATE;
        }
    };

    /**
     * Behavior for {@link ServiceMode#ON_DEMAND}.
     */
    private static final ServiceModeBehavior ON_DEMAND = new ServiceModeBehavior() {
        boolean shouldStart(ServiceController<?> service) {
            return service.isUpDemanded();
        }

        boolean shouldStop(ServiceController<?> service) {
            return !service.isUpDemanded();
        }

        Demand shouldDemandDependencies() {
            return Demand.PROPAGATE;
        }
    };

    /**
     * Indicates if this mode requires
     * {@code service} to start.
     *  
     * @param service a service at the {@link ServiceController.State$DOWN} state, with no unsatisfied or failed
     *                dependencies
     * @return {@code true} if {@code service} should start
     */
    abstract boolean shouldStart(ServiceController<?> service);

    /**
     * Given {@code service} has no unsatisfied dependencies, this method indicates if {@code service} should stop.
     *  
     * @param service a service at the {@link ServiceController.State$UP} state, with no unsatisfied or failed
     *                dependencies
     * @return {@code true} if {@code service} should stop
     */
    abstract boolean shouldStop(ServiceController<?> service);

    /**
     * Indicates if this mode requires the service to demand its dependencies.
     * 
     * @return {@code true} if this mode requires the service to demand dependencies
     */
    abstract Demand shouldDemandDependencies();

    enum Demand {
        // always demand dependencies
        ALWAYS,
        // demand dependencies only when demanded
        PROPAGATE,
    };

    static ServiceModeBehavior getInstance(ServiceMode serviceMode) {
        switch (serviceMode) {
            case ACTIVE:
                return ACTIVE;
            case LAZY:
                return LAZY;
            case ON_DEMAND:
                return ON_DEMAND;
            default:
                throw new IllegalArgumentException("Unexpected service mode");
        }
    }
}
