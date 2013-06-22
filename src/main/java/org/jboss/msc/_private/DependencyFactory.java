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

import org.jboss.msc.service.DependencyFlag;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.txn.Transaction;

/**
 * Factory for creating a dependency during service installation.
 * 
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 * @author <a href="mailto:frainone@redhat.com">Flavia Rainone</a>
 */
final class DependencyFactory {
    /**
     * Creates a dependency based on this spec. Invoked during service installation.
     * 
     * @param serviceBuilder the builder of the service
     * @param transaction    the active transaction
     * @param context        the service context
     * @return the created dependency
     */
    static <T> AbstractDependency<T> create(final ServiceRegistryImpl registry, final ServiceName name,
            final DependencyFlag[] flags, final ServiceBuilderImpl<?> serviceBuilder, final Transaction transaction) {
        final Registration dependencyRegistration = registry.getOrCreateRegistration(transaction, transaction, name);
        final DependencyFlag demandFlag = getDemandFlag(flags);
        final SimpleDependency<T> dependency = new SimpleDependency<T>(dependencyRegistration, demandFlag, transaction);
        return decorate(dependency, flags, serviceBuilder, transaction);
    }

    private static DependencyFlag getDemandFlag(DependencyFlag[] flags) {
        for (DependencyFlag flag: flags) {
            if (flag == DependencyFlag.DEMANDED || flag == DependencyFlag.UNDEMANDED) {
                return flag;
            }
        }
        return null; 
    }

    private static <T> AbstractDependency<T> decorate(SimpleDependency<T> dependency, DependencyFlag[] flags,
            ServiceBuilderImpl<?> serviceBuilder, Transaction transaction) {
        for (DependencyFlag flag: flags) {
            if (flag == DependencyFlag.PARENT) {
                return new ParentDependency<T>(dependency, serviceBuilder, transaction);
            }
        }
        return dependency;
    }
}
