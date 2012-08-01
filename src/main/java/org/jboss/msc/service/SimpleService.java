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
 * A simple service which starts and stops and has a void value (i.e. is not injectable).  Services may be
 * stopped and started multiple times.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public interface SimpleService {

    /**
     * Start the service.  If this method throws an exception, it will be recorded as a failure to start.
     *
     * @param transaction the transaction
     * @param startContext the start context
     */
    void start(Transaction transaction, StartContext startContext);

    /**
     * Stop the service.
     *
     * @param transaction the transaction
     */
    void stop(Transaction transaction);
}
