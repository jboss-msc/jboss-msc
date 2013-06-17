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

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.jboss.msc.service.ServiceContainer;
import org.jboss.msc.service.ServiceRegistry;
import org.jboss.msc.txn.Transaction;

/**
 * A transactional service container.
 * 
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 * @author <a href="mailto:frainone@redhat.com">Flavia Rainone</a>
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
public final class ServiceContainerImpl implements ServiceContainer {

    private final Set<ServiceRegistryImpl> registries = Collections.synchronizedSet(new HashSet<ServiceRegistryImpl>());

    public ServiceRegistry newRegistry() {
        final ServiceRegistryImpl returnValue = new ServiceRegistryImpl();
        registries.add(returnValue);
        return returnValue;
    }

    void shutdown(final Transaction txn) {
        synchronized(registries) {
            for (final ServiceRegistryImpl registry : registries) {
                registry.remove(txn);
            }
        }
    }
}
