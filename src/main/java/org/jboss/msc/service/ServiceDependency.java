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

import java.util.ArrayList;
import java.util.List;

/**
 * A service dependency used when describing service controller configuration.
 * 
 * @author John E. Bailey
 */
final class ServiceDependency {
    private final ServiceName serviceName;
    private boolean optional;
    private final List<NamedInjection> namedInjections = new ArrayList<NamedInjection>();

    ServiceDependency(ServiceName serviceName, boolean optional) {
        this(serviceName, optional, null);
    }

    ServiceDependency(ServiceName serviceName, boolean optional, NamedInjection namedInjection) {
        this.serviceName = serviceName;
        this.optional = optional;
        if(namedInjection != null)
            namedInjections.add(namedInjection);
    }

    ServiceName getServiceName() {
        return serviceName;
    }

    void setOptional(boolean optional) {
        this.optional = this.optional && optional;
    }

    boolean isOptional() {
        return optional;
    }

    void addNamedInjection(final NamedInjection namedInjection) {
        namedInjections.add(namedInjection);
    }

    List<NamedInjection> getNamedInjections() {
        return namedInjections;
    }
}
