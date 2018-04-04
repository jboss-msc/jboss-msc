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

import org.jboss.msc.inject.Injector;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Service builder abstraction.
 *
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
abstract class AbstractServiceBuilder<T> implements ServiceBuilder<T> {

    private final ServiceName serviceId;
    private final ServiceTargetImpl serviceTarget;
    private final ServiceControllerImpl<?> parent;

    AbstractServiceBuilder(final ServiceName serviceId, final ServiceTargetImpl serviceTarget, final ServiceControllerImpl<?> parent) {
        this.serviceId = serviceId;
        this.serviceTarget = serviceTarget;
        this.parent = parent;
    }

    final ServiceTargetImpl getServiceTarget() {
        return serviceTarget;
    }

    final ServiceName getServiceId() {
        return serviceId;
    }

    final ServiceControllerImpl<?> getParent() {
        return parent;
    }

    abstract org.jboss.msc.Service getService();
    abstract Collection<ServiceName> getServiceAliases();
    abstract Map<ServiceName, WritableValueImpl> getProvides();
    abstract Map<ServiceName, Dependency> getDependencies();
    abstract Set<StabilityMonitor> getMonitors();
    abstract Set<ServiceListener<? super T>> getServiceListeners();
    abstract Set<LifecycleListener> getLifecycleListeners();
    abstract List<ValueInjection<?>> getValueInjections();
    abstract ServiceController.Mode getInitialMode();
    abstract List<Injector<? super T>> getOutInjections();

    abstract void addServiceListenersNoCheck(final Set<? extends ServiceListener<? super T>> serviceListeners);
    abstract void addLifecycleListenersNoCheck(final Set<LifecycleListener> lifecycleListeners);
    abstract void addMonitorsNoCheck(final Collection<? extends StabilityMonitor> monitors);
    abstract void addDependenciesNoCheck(final Iterable<ServiceName> newDependencies);

    static final class Dependency {
        private final ServiceName name;
        private DependencyType dependencyType;
        private List<Injector<Object>> injectorList = new ArrayList<Injector<Object>>(0);

        Dependency(final ServiceName name, final DependencyType dependencyType) {
            this.name = name;
            this.dependencyType = dependencyType;
        }

        ServiceName getName() {
            return name;
        }

        List<Injector<Object>> getInjectorList() {
            return injectorList;
        }

        DependencyType getDependencyType() {
            return dependencyType;
        }

        void setDependencyType(final DependencyType dependencyType) {
            this.dependencyType = dependencyType;
        }
    }

}
