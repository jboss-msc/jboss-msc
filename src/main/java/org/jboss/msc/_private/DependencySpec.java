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

import java.util.ArrayList;
import java.util.List;

import org.jboss.msc.service.DependencyFlag;
import org.jboss.msc.service.Injector;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.txn.ServiceContext;
import org.jboss.msc.txn.Transaction;

/**
 * Spec for creating a dependency during service installation.
 * 
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 * @author <a href="mailto:frainone@redhat.com">Flavia Rainone</a>
 */
final class DependencySpec<T> {
    private final ServiceRegistryImpl registry;
    private final ServiceName name;
    private final DependencyFlag[] flags;
    private final List<Injector<? super T>> injections = new ArrayList<Injector<? super T>>();

    DependencySpec(final ServiceRegistryImpl registry, final ServiceName name, final DependencyFlag[] flags) {
        this.registry = registry;
        this.name = name;
        this.flags = flags;
    }

    public ServiceName getName() {
        return name;
    }

    public void addInjection(final Injector<? super T> injector) {
        injections.add(injector);
    }

    /**
     * Creates a dependency based on this spec. Invoked during service installation.
     * 
     * @param serviceBuilder the builder of the service
     * @param transaction    the active transaction
     * @param context        the service context
     * @return the created dependency
     */
    public Dependency<T> createDependency(ServiceBuilderImpl<?> serviceBuilder, Transaction transaction, ServiceContext context) {
        @SuppressWarnings("unchecked")
        final Injector<? super T>[] injectionArray = (Injector<? super T>[]) injections.toArray(new Injector<?>[injections.size()]);
        final Registration dependencyRegistration = registry.getOrCreateRegistration(context, transaction, name);
        final boolean dependencyUp = isDependencyUp();
        final DependencyFlag demandFlag = getDemandFlag();
        final boolean required = isRequired();
        final Dependency<T> dependency = new SimpleDependency<T>(injectionArray, dependencyRegistration, dependencyUp, required, demandFlag, transaction, context);
        return decorate(dependency, serviceBuilder);
    }

    private boolean isDependencyUp() {
        for (DependencyFlag flag: flags) {
            if (flag == DependencyFlag.ANTI) {
                return false;
            }
        }
        // notice that, when there are no flags, the service is required to be UP
        return true;
    }

    private DependencyFlag getDemandFlag() {
        for (DependencyFlag flag: flags) {
            if (flag == DependencyFlag.DEMANDED || flag == DependencyFlag.UNDEMANDED) {
                return flag;
            }
        }
        return null; 
    }

    private boolean isRequired() {
        for (DependencyFlag flag: flags) {
            if (flag == DependencyFlag.UNREQUIRED) {
                return false;
            }
        }
        // notice that, when there are no flags, the dependency is required by default
        return true;
    }

    private final Dependency<T> decorate(Dependency<T> dependency, ServiceBuilderImpl<?> serviceBuilder) {
        for (DependencyFlag flag: flags) {
            switch(flag) {
                case OPTIONAL:
                    dependency = new OptionalDependency<T>(dependency);
                case PARENT:
                    dependency = new ParentDependency<T>(dependency, serviceBuilder);
                case REPLACEABLE:
                    dependency = new ReplaceableDependency<T>(dependency);
                default:
                    // do nothing
            }
        }
        return dependency;
    }
}
