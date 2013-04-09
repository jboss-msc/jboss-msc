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

/**
 * A  {@link ServiceContainer} builder.
 * The implementations of this interface are not thread safe.
 *
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
public interface ServiceContainerBuilder {

    /**
     * Sets whether target container will be automatically shut down at VM exit.
     *
     * @param autoShutdown {@code true} to automatically shut down the container
     *        at VM exit, {@code false} otherwise
     * @return a reference to this object
     * @throws IllegalStateException if {@link #build()} have been called.
     */
    ServiceContainerBuilder setAutoShutdown(boolean autoShutdown) throws IllegalStateException;

    /**
     * Sets executor parameters. If not called, default will be generated.
     *
     * @param corePoolSize the number of threads to keep in the pool, even
     *        if they are idle, unless {@code allowCoreThreadTimeOut} is set
     * @param maximumPoolSize the maximum number of threads to allow in the
     *        pool
     * @param keepAliveTime when the number of threads is greater than
     *        the core, this is the maximum time that excess idle threads
     *        will wait for new tasks before terminating.
     * @param unit the time unit for the {@code keepAliveTime} argument
     * @return a reference to this object
     * @throws IllegalStateException if {@link #build()} have been called.
     */
    ServiceContainerBuilder setExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit) throws IllegalStateException;

    /**
     * Creates a new container.
     *
     * @return new container instance.
     */
    ServiceContainerImpl build();

}
