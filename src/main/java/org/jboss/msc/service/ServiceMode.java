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

package org.jboss.msc.service;

/**
 * A service mode.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 * @author <a href="mailto:frainone@redhat.com">Flavia Rainone</a>
 */
public enum ServiceMode {
    /**
     * Start as soon as all dependencies are met, unless this service is demanded to start by an ANTI dependency. 
     * Demands dependencies.
     */
    ACTIVE {
        boolean shouldStart(ServiceController<?> service) {
            return !service.isDownDemanded();
        }

        boolean shouldStop(ServiceController<?> service) {
            return service.isDownDemanded();
        }

        Demand shouldDemandDependencies() {
            return Demand.ALWAYS;
        }
    },
    /**
     * Start as soon as all dependencies are met.  Does not demand dependencies.
     */
    PASSIVE {
        boolean shouldStart(ServiceController<?> service) {
            return true;
        }

        boolean shouldStop(ServiceController<?> service) {
            return false;
        }

        Demand shouldDemandDependencies() {
            return Demand.PROPAGATE;
        }
    },
    /**
     * Start only when demanded to, but stay running until required or demanded to stop.
     */
    LAZY {
        boolean shouldStart(ServiceController<?> service) {
            return service.isUpDemanded();
        }

        boolean shouldStop(ServiceController<?> service) {
            return service.isDownDemanded();
        }

        Demand shouldDemandDependencies() {
            return Demand.SERVICE_UP;
        }
    },
    /**
     * Start only when demanded to, and stop when demands disappear, or when demanded to stop.
     */
    ON_DEMAND {
        boolean shouldStart(ServiceController<?> service) {
            return service.isUpDemanded();
        }

        boolean shouldStop(ServiceController<?> service) {
            return !service.isUpDemanded();
        }

        Demand shouldDemandDependencies() {
            return Demand.PROPAGATE;
        }
    },
    /**
     * Do not start; stay in a stopped state.
     */
    NEVER {
        boolean shouldStart(ServiceController<?> service) {
            return false;
        }

        boolean shouldStop(ServiceController<?> service) {
            return true;
        }

        Demand shouldDemandDependencies() {
            return Demand.NEVER;
        }
    },
    ;

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
        // never demand dependencies
        NEVER,
        // demand dependencies only when demanded
        PROPAGATE,
        // demand dependencies only when service enters up state
        SERVICE_UP};
}
