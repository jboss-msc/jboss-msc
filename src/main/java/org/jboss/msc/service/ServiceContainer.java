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

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * A service container. This class is thread safe.
 *
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
public interface ServiceContainer {

    /**
     * Adds service to the container.
     *
     * @param serviceName service name
     * @param service service instance or {@code null}
     * @param <T> service type
     * @return service builder to configure service dependencies, service mode and other stuff.
     */
    <T extends Service> ServiceBuilder<T> addService(String serviceName, T service);

    /**
     * Gets service from the container. This method can be called even
     * {@code #addService(String,T)} have not been yet called on the container.
     *
     * @param serviceName service name
     * @param <T> expected service type
     * @return service future.
     */
    <T extends Service> Future<T> getService(String serviceName);

    /**
     * Removes service from the container.
     *
     * @param serviceName service name
     * @param <T> service type
     * @return removed service future. The future will return instance that have been associated with
     * {@code serviceName} or {@code null} if service was registered but was associated with {@code null} literal.
     * @throws ServiceNotFoundException if service was not available
     */
    <T extends Service> Future<T> removeService(String serviceName) throws ServiceNotFoundException;

    /**
     * Shuts down this container.
     */
    void shutdown();

    /**
     * Returns {@code true} if this container has been shut down.
     *
     * @return {@code true} if {@link #shutdown()} have been called, {@code false} otherwise
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
