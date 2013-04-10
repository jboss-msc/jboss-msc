/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013 Red Hat, Inc., and individual contributors
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

import java.util.concurrent.TimeUnit;

import org.jboss.msc.txn.Transaction;

/**
 * A service container. This class is thread safe.
 *
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 * @author <a href="mailto:frainone@redhat.com">Flavia Rainone</a>
 */
public interface ServiceContainer {

    /**
     * Shuts down this container.
     *
     * @param txn transaction
     */
    void shutdown(Transaction txn);

    /**
     * Returns {@code true} if this container has been shut down.
     *
     * @return {@code true} if {@link #shutdown(Transaction)} have been called, {@code false} otherwise
     */
    boolean isShutdown();

    /**
     * Returns {@code true} if all container tasks have completed following shut down.
     * Note that {@link #isTerminated()} is never true unless shutdown was called first.
     *
     * @return {@code true} if container have been terminated, {@code false} otherwise
     */
    boolean isTerminated();

    /**
     * Blocks until all container tasks have completed execution after a shutdown request,
     * or the current thread is interrupted, whichever happens first.
     *
     * @throws InterruptedException if interrupted while waiting
     */
    void awaitTermination() throws InterruptedException;

    /**
     * Blocks until all container tasks have completed execution after a shutdown request, or the timeout occurs,
     * or the current thread is interrupted, whichever happens first.
     *
     * @param timeout the maximum time to wait
     * @param unit the time unit of the timeout argument
     * return {@code true} if this container terminated and {@code false} if the timeout elapsed before termination
     * @throws InterruptedException if interrupted while waiting
     */
    boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException;

}
