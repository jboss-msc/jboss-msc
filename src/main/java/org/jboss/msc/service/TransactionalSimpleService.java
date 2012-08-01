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

import org.jboss.msc.txn.Transaction;

/**
 * A simple service which starts transactionally.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public interface TransactionalSimpleService extends SimpleService {

    void start(Transaction transaction, StartContext startContext);

    /**
     * Perform any prepare-state operations for this service start.  If prepare cannot proceed, the given start context
     * should be used to report the failure cause.
     *
     * @param transaction the owing transaction
     * @param startContext the start context
     */
    void prepareStart(Transaction transaction, StartContext startContext);

    /**
     * Commit the service start.  Expected to succeed; any exceptions thrown will be ignored.
     */
    void commitStart();

    /**
     * Inform the service of intention to stop.
     *
     * @param transaction the owning transaction
     */
    void intendStop(Transaction transaction);

    void stop(Transaction transaction);

    /**
     * Commit the service stop.  Expected to succeed; any exceptions thrown will be ignored.
     */
    void commitStop();

}
