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

import org.jboss.msc.txn.CommitContext;
import org.jboss.msc.txn.Committable;
import org.jboss.msc.txn.Executable;
import org.jboss.msc.txn.ExecuteContext;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
final class ServiceRemoveFromContainerTask implements Committable, Executable<Void> {

    ServiceRemoveFromContainerTask(final ServiceName name) {
    }

    public void commit(final CommitContext context) {
        // remove service registration in container
    }

    public void execute(final ExecuteContext<Void> context) {
        // remove service registration in transaction view
    }
}
