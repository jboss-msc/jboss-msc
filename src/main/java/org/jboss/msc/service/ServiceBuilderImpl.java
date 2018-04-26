/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2018, Red Hat, Inc., and individual contributors
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

import static java.lang.Thread.currentThread;
import static java.util.Collections.emptyList;

import org.jboss.msc.Service;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.value.Value;

import java.util.Collection;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Multi-value services {@link ServiceBuilder} implementation.
 *
 * @param <T> the type of service being built
 *
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
final class ServiceBuilderImpl<T> extends AbstractServiceBuilder<T> {

    private final Thread thread = currentThread();
    private final Map<ServiceName, WritableValueImpl> provides = new HashMap<>();
    private Service service;
    private Set<ServiceName> aliases;
    private ServiceController.Mode initialMode;
    private Map<ServiceName, ReadableValueImpl> requires;
    private Set<StabilityMonitor> monitors;
    private Set<ServiceListener<? super T>> serviceListeners;
    private Set<LifecycleListener> lifecycleListeners;
    private boolean installed;

    ServiceBuilderImpl(final ServiceName serviceId, final ServiceTargetImpl serviceTarget, final ServiceControllerImpl<?> parent) {
        super(serviceId, serviceTarget, parent);
        addProvidesInternal(serviceId, null);
    }

    @Override
    public ServiceBuilder<T> addAliases(final ServiceName... aliases) {
        // preconditions
        assertNotInstalled();
        assertNotNull(aliases);
        assertThreadSafety();
        assertServiceNotConfigured();
        for (ServiceName alias : aliases) {
            assertNotNull(alias);
            assertNotRequired(alias, false);
        }
        // implementation
        for (ServiceName alias : aliases) {
            if (!alias.equals(getServiceId()) && addAliasInternal(alias)) {
                addProvidesInternal(alias, null);
            }
        }
        return this;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <V> Supplier<V> requires(final ServiceName dependency) {
        // preconditions
        assertNotInstalled();
        assertNotNull(dependency);
        assertThreadSafety();
        assertServiceNotConfigured();
        assertNotInstanceId(dependency);
        assertNotRequired(dependency, true);
        assertNotProvided(dependency, true);
        // implementation
        final ReadableValueImpl retVal = getServiceTarget().getOrCreateRegistration(dependency).getReadableValue();
        addRequiresInternal(dependency, retVal);
        return (Supplier<V>)retVal;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <V> Consumer<V> provides(final ServiceName... dependencies) {
        // preconditions
        assertNotInstalled();
        assertNotNull(dependencies);
        assertThreadSafety();
        assertServiceNotConfigured();
        for (ServiceName dependency : dependencies) {
            assertNotNull(dependency);
            assertNotRequired(dependency, false);
            assertNotProvided(dependency, false);
        }
        // implementation
        final WritableValueImpl retVal = new WritableValueImpl();
        for (ServiceName dependency : dependencies) {
            addProvidesInternal(dependency, retVal);
        }
        return (Consumer<V>)retVal;
    }

    @Override
    @SuppressWarnings("unchecked")
    public ServiceBuilder<T> setInstance(final Service service) {
        // preconditions
        assertNotInstalled();
        assertThreadSafety();
        assertServiceNotConfigured();
        // implementation
        this.service = service != null ? service : Service.NULL;
        return this;
    }

    @Override
    public ServiceBuilder<T> setInitialMode(final ServiceController.Mode mode) {
        // preconditions
        assertNotInstalled();
        assertNotNull(mode);
        assertNotRemove(mode);
        assertModeNotConfigured();
        assertThreadSafety();
        // implementation
        this.initialMode = mode;
        return this;
    }

    @Override
    public ServiceBuilder<T> addMonitor(final StabilityMonitor monitor) {
        // preconditions
        assertNotInstalled();
        assertNotNull(monitor);
        assertThreadSafety();
        // implementation
        addMonitorInternal(monitor);
        return this;
    }

    @Override
    public ServiceBuilder<T> addListener(final LifecycleListener listener) {
        // preconditions
        assertNotInstalled();
        assertNotNull(listener);
        assertThreadSafety();
        // implementation
        addListenerInternal(listener);
        return this;
    }

    @Override
    public ServiceController<T> install() throws ServiceRegistryException {
        // preconditions
        assertNotInstalled();
        assertThreadSafety();
        // implementation
        installed = true;
        if (service == null) service = Service.NULL;
        if (initialMode == null) initialMode = ServiceController.Mode.ACTIVE;
        return getServiceTarget().install(this);
    }

    // implementation internals

    void addServiceListenersNoCheck(final Set<? extends ServiceListener<? super T>> listeners) {
        // For backward compatibility reasons when
        // ServiceListeners are defined via ServiceTarget
        if (listeners == null || listeners.isEmpty()) return;
        if (serviceListeners == null) serviceListeners = new IdentityHashSet<>();
        serviceListeners.addAll(listeners);
    }

    void addLifecycleListenersNoCheck(final Set<LifecycleListener> listeners) {
        if (listeners == null || listeners.isEmpty()) return;
        for (LifecycleListener listener : listeners) {
            if (listener != null) addListenerInternal(listener);
        }
    }

    void addMonitorsNoCheck(final Collection<? extends StabilityMonitor> monitors) {
        for (StabilityMonitor monitor : monitors) {
            if (monitor != null) addMonitorInternal(monitor);
        }
    }

    void addDependenciesNoCheck(final Iterable<ServiceName> dependencies) {
        // For backward compatibility reasons when
        // service dependencies are defined via ServiceTarget
        for (ServiceName dependency : dependencies) {
            if (dependency == null) continue;
            if (requires != null && requires.containsKey(dependency)) continue; // dependency already required
            if (provides != null && provides.containsKey(dependency)) continue; // cannot depend on ourselves
            addRequiresInternal(dependency, null);
        }
    }

    Service getService() {
        return service;
    }

    void addRequiresInternal(final ServiceName name, final ReadableValueImpl dependency) {
        if (requires == null) requires = new HashMap<>();
        if (requires.size() == ServiceControllerImpl.MAX_DEPENDENCIES) {
            throw new IllegalArgumentException("Too many dependencies specified (max is " + ServiceControllerImpl.MAX_DEPENDENCIES + ")");
        }
        requires.put(name, dependency);
    }

    boolean addAliasInternal(final ServiceName alias) {
        if (aliases == null) aliases = new HashSet<>();
        if (!aliases.contains(alias)) {
            aliases.add(alias);
            return true;
        }
        return false;
    }

    void addProvidesInternal(final ServiceName name, final WritableValueImpl dependency) {
        if (dependency != null) {
            provides.put(name, dependency);
        } else if (!provides.containsKey(name)) {
            provides.put(name, null);
        }
    }

    void addMonitorInternal(final StabilityMonitor monitor) {
        if (monitors == null) monitors = new IdentityHashSet<>();
        monitors.add(monitor);
    }

    void addListenerInternal(final LifecycleListener listener) {
        if (lifecycleListeners == null) lifecycleListeners = new IdentityHashSet<>();
        lifecycleListeners.add(listener);
    }

    Collection<ServiceName> getServiceAliases() {
        return aliases == null ? Collections.emptySet() : aliases;
    }

    Map<ServiceName, WritableValueImpl> getProvides() {
        return provides;
    }

    Map<ServiceName, Dependency> getDependencies() {
        if (requires == null) return Collections.emptyMap();
        final Map<ServiceName, Dependency> retVal = new HashMap<>(requires.size());
        for (Entry<ServiceName, ReadableValueImpl> entry : requires.entrySet()) {
            retVal.put(entry.getKey(), new Dependency(entry.getKey(), DependencyType.REQUIRED));
        }
        return retVal;
    }

    Set<StabilityMonitor> getMonitors() {
        ServiceControllerImpl parent = getParent();
        while (parent != null) {
            synchronized (parent) {
                addMonitorsNoCheck(parent.getMonitors());
                parent = parent.getParent();
            }
        }
        return monitors == null ? Collections.emptySet() : monitors;
    }

    Set<ServiceListener<? super T>> getServiceListeners() {
        return serviceListeners == null ? Collections.emptySet() : serviceListeners;
    }

    Set<LifecycleListener> getLifecycleListeners() {
        return lifecycleListeners == null ? Collections.emptySet() : lifecycleListeners;
    }

    ServiceController.Mode getInitialMode() {
        return initialMode;
    }

    List<Injector<? super T>> getOutInjections() {
        return emptyList();
    }

    List<ValueInjection<?>> getValueInjections() {
        return emptyList();
    }

    // implementation assertions

    private void assertNotInstalled() {
        if (installed) {
            throw new IllegalStateException("ServiceBuilder already installed");
        }
    }

    private void assertProvidesCalled() {
        if (provides == null || provides.isEmpty()) {
            throw new IllegalStateException("ServiceBuilder.provides() must be called first");
        }
    }

    private void assertThreadSafety() {
        if (thread != currentThread()) {
            throw new ConcurrentModificationException("ServiceBuilder used by multiple threads");
        }
    }

    private void assertNotInstanceId(final ServiceName dependency) {
        if (getServiceId().equals(dependency)) {
            throw new IllegalArgumentException("Cannot both require and provide same dependency:" + dependency);
        }
    }

    private void assertNotRequired(final ServiceName dependency, final boolean processingRequires) {
        if (requires != null && requires.keySet().contains(dependency)) {
            if (processingRequires) {
                throw new IllegalArgumentException("Cannot require dependency more than once:" + dependency);
            } else {
                throw new IllegalArgumentException("Cannot both require and provide same dependency:" + dependency);
            }
        }
    }

    private void assertNotProvided(final ServiceName dependency, final boolean processingRequires) {
        if (provides == null) return;
        if (processingRequires) {
            if (provides.containsKey(dependency)) {
                throw new IllegalArgumentException("Cannot both require and provide same dependency:" + dependency);
            }
        } else {
            if (provides.get(dependency) != null) {
                throw new IllegalArgumentException("Cannot provide dependency more than once: " + dependency);
            }
        }
    }

    private void assertServiceNotConfigured() {
        if (service != null) {
            throw new IllegalStateException("Detected addAliases(), requires(), provides() or setService() call after setService() method call");
        }
    }

    private void assertModeNotConfigured() {
        if (initialMode != null) {
            throw new IllegalStateException("setInitialMode() method called twice");
        }
    }

    private static void assertNotNull(final Object parameter) {
        if (parameter == null) {
            throw new NullPointerException("Method parameter cannot be null");
        }
    }

    private static void assertNotRemove(final ServiceController.Mode mode) {
        if (mode == ServiceController.Mode.REMOVE) {
            throw new IllegalArgumentException("Initial service mode cannot be REMOVE");
        }
    }

    // Forbidden method calls

    @Override
    public ServiceBuilder<T> addMonitors(final StabilityMonitor... monitors) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ServiceBuilder<T> addDependencies(final ServiceName... newDependencies) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ServiceBuilder<T> addDependencies(final DependencyType dependencyType, final ServiceName... newDependencies) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ServiceBuilder<T> addDependencies(final Iterable<ServiceName> newDependencies) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ServiceBuilder<T> addDependencies(final DependencyType dependencyType, final Iterable<ServiceName> newDependencies) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ServiceBuilder<T> addDependency(final ServiceName dependency) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ServiceBuilder<T> addDependency(final DependencyType dependencyType, final ServiceName dependency) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ServiceBuilder<T> addDependency(final ServiceName dependency, final Injector<Object> target) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ServiceBuilder<T> addDependency(final DependencyType dependencyType, final ServiceName dependency, final Injector<Object> target) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <I> ServiceBuilder<T> addDependency(final ServiceName dependency, final Class<I> type, final Injector<I> target) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <I> ServiceBuilder<T> addDependency(final DependencyType dependencyType, final ServiceName dependency, final Class<I> type, final Injector<I> target) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <I> ServiceBuilder<T> addInjection(final Injector<? super I> target, final I value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public <I> ServiceBuilder<T> addInjectionValue(final Injector<? super I> target, final Value<I> value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ServiceBuilder<T> addInjection(final Injector<? super T> target) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ServiceBuilder<T> addListener(final ServiceListener<? super T> listener) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ServiceBuilder<T> addListener(final ServiceListener<? super T>... listeners) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ServiceBuilder<T> addListener(final Collection<? extends ServiceListener<? super T>> listeners) {
        throw new UnsupportedOperationException();
    }

}
