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

import org.jboss.msc.service.management.ServiceStatus;

import java.util.function.Function;

/**
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
final class Functions {

    private Functions() {
        // forbidden instantiation
    }

    static final class ServiceIdentityFunction implements Function<ServiceStatus, ServiceStatus> {
        static final Function<ServiceStatus, ServiceStatus> INSTANCE = new ServiceIdentityFunction();

        @Override
        public ServiceStatus apply(final ServiceStatus serviceStatus) {
            return serviceStatus;
        }
    }

    static final class ServiceIdIdentityFunction implements Function<ServiceStatus, String> {
        static final Function<ServiceStatus, String> INSTANCE = new ServiceIdIdentityFunction();

        @Override
        public String apply(final ServiceStatus serviceStatus) {
            return serviceStatus.getId();
        }
    }

    static final class ServiceRequiringValueFunction implements Function<ServiceStatus, ServiceStatus> {
        private final String expectedValue;

        ServiceRequiringValueFunction(final String expectedValue) {
            this.expectedValue = expectedValue;
        }

        @Override
        public ServiceStatus apply(final ServiceStatus serviceStatus) {
            for (String requiredValue : serviceStatus.getRequiredValues()) {
                if (requiredValue.equals(expectedValue)) return serviceStatus;
            }
            return null;
        }
    }

    static final class ServiceIdRequiringValueFunction implements Function<ServiceStatus, String> {
        private final String expectedValue;

        ServiceIdRequiringValueFunction(final String expectedValue) {
            this.expectedValue = expectedValue;
        }

        @Override
        public String apply(final ServiceStatus serviceStatus) {
            for (String requiredValue : serviceStatus.getRequiredValues()) {
                if (requiredValue.equals(expectedValue)) return serviceStatus.getId();
            }
            return null;
        }
    }

    static final class ServiceProvidingValueFunction implements Function<ServiceStatus, ServiceStatus> {
        private final String expectedValue;

        ServiceProvidingValueFunction(final String expectedValue) {
            this.expectedValue = expectedValue;
        }

        @Override
        public ServiceStatus apply(final ServiceStatus serviceStatus) {
            for (String providedValue : serviceStatus.getProvidedValues()) {
                if (providedValue.equals(expectedValue)) return serviceStatus;
            }
            return null;
        }
    }

    static final class ServiceIdProvidingValueFunction implements Function<ServiceStatus, String> {
        private final String expectedValue;

        ServiceIdProvidingValueFunction(final String expectedValue) {
            this.expectedValue = expectedValue;
        }

        @Override
        public String apply(final ServiceStatus serviceStatus) {
            for (String providedValue : serviceStatus.getProvidedValues()) {
                if (providedValue.equals(expectedValue)) return serviceStatus.getId();
            }
            return null;
        }
    }

    static final class ServiceMissingValueFunction implements Function<ServiceStatus, ServiceStatus> {
        private final String expectedValue;

        ServiceMissingValueFunction(final String expectedValue) {
            this.expectedValue = expectedValue;
        }

        @Override
        public ServiceStatus apply(final ServiceStatus serviceStatus) {
            for (String missingValue : serviceStatus.getMissingValues()) {
                if (missingValue.equals(expectedValue)) return serviceStatus;
            }
            return null;
        }
    }

    static final class ServiceIdMissingValueFunction implements Function<ServiceStatus, String> {
        private final String expectedValue;

        ServiceIdMissingValueFunction(final String expectedValue) {
            this.expectedValue = expectedValue;
        }

        @Override
        public String apply(final ServiceStatus serviceStatus) {
            for (String missingValue : serviceStatus.getMissingValues()) {
                if (missingValue.equals(expectedValue)) return serviceStatus.getId();
            }
            return null;
        }
    }

    static final class ServiceIdFunction implements Function<ServiceStatus, ServiceStatus> {
        private final String expectedValue;

        ServiceIdFunction(final String expectedValue) {
            this.expectedValue = expectedValue;
        }

        @Override
        public ServiceStatus apply(final ServiceStatus serviceStatus) {
            return serviceStatus.getId().equals(expectedValue) ? serviceStatus : null;
        }
    }

    static final class ServiceStateFunction implements Function<ServiceStatus, ServiceStatus> {
        private final String expectedValue;

        ServiceStateFunction(final String expectedValue) {
            this.expectedValue = expectedValue;
        }

        @Override
        public ServiceStatus apply(final ServiceStatus serviceStatus) {
            return serviceStatus.getState().equals(expectedValue) ? serviceStatus : null;
        }
    }

    static final class ServiceIdStateFunction implements Function<ServiceStatus, String> {
        private final String expectedValue;

        ServiceIdStateFunction(final String expectedValue) {
            this.expectedValue = expectedValue;
        }

        @Override
        public String apply(final ServiceStatus serviceStatus) {
            return serviceStatus.getState().equals(expectedValue) ? serviceStatus.getId() : null;
        }
    }

    static final class ServiceModeFunction implements Function<ServiceStatus, ServiceStatus> {
        private final String expectedValue;

        ServiceModeFunction(final String expectedValue) {
            this.expectedValue = expectedValue;
        }

        @Override
        public ServiceStatus apply(final ServiceStatus serviceStatus) {
            return serviceStatus.getMode().equals(expectedValue) ? serviceStatus : null;
        }
    }

    static final class ServiceIdModeFunction implements Function<ServiceStatus, String> {
        private final String expectedValue;

        ServiceIdModeFunction(final String expectedValue) {
            this.expectedValue = expectedValue;
        }

        @Override
        public String apply(final ServiceStatus serviceStatus) {
            return serviceStatus.getMode().equals(expectedValue) ? serviceStatus.getId() : null;
        }
    }

}
