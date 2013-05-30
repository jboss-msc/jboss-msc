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

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

import org.jboss.msc.service.ServiceContainer;
import org.jboss.msc.service.ServiceRegistry;
import org.jboss.msc.txn.CommitContext;
import org.jboss.msc.txn.Committable;
import org.jboss.msc.txn.Revertible;
import org.jboss.msc.txn.RollbackContext;

/**
 * A transactional service container.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 * @author <a href="mailto:frainone@redhat.com">Flavia Rainone</a>
 */
public final class ServiceContainerImpl implements ServiceContainer {
    private final Set<ServiceRegistry> registries = new HashSet<ServiceRegistry>();
    private final Set<CountDownLatch> shutDownLatches = new HashSet<CountDownLatch>();
    private boolean shutdownCompleted;
    private boolean shutdownInitiated = false;

    public ServiceRegistry newRegistry() {
        return new ServiceRegistryImpl();
    }

    private class ShutdownTask implements Committable, Revertible {

        public void commit(CommitContext context) {
            try {
                shutdownCompleted = true;
                synchronized (shutDownLatches) {
                    for (CountDownLatch shutdownLatch: shutDownLatches) {
                        shutdownLatch.countDown();
                    }
                }
            } finally {
                context.complete();
            }
        }

        public void rollback(RollbackContext context) {
            synchronized (ServiceContainerImpl.this) {
                shutdownInitiated = false;
            }
            context.complete();
        }
    }
}
