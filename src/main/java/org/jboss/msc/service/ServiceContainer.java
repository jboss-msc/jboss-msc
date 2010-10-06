/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.msc.service;

import java.io.PrintStream;
import java.util.concurrent.Executor;

/**
 * A service container which manages a set of running services.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public interface ServiceContainer extends ServiceTarget, ServiceRegistry {

    /**
     * Set the container executor.  If {@code null} is specified, a default single-thread executor is used.
     * <p>
     * You <b>must</b> adhere to the following rules when setting an executor:
     * <ul>
     * <li>The executor must always accept tasks (throwing {@link java.util.concurrent.RejectedExecutionException RejectedExecutionException}
     * can cause significant problems)</li>
     * <li>The executor must be removed (by setting this value to {@code null} or another executor) before it is shut down.</li>
     * </ul>
     *
     * @param executor the executor to use
     */
    void setExecutor(Executor executor);

    /**
     * Stop all services within this container.
     */
    void shutdown();

    /**
     * Dump a complete list of services to {@code System.out}.
     */
    void dumpServices();

    /**
     * Dump a complete list of services to the given stream.
     *
     * @param stream the stream to which the service list should be written
     */
    void dumpServices(PrintStream stream);

    /**
     * The factory class for service containers.
     */
    class Factory {

        private Factory() {
        }

        /**
         * Create a new instance.
         *
         * @return a new service container instance
         */
        public static ServiceContainer create() {
            final ServiceContainerImpl container = new ServiceContainerImpl();
            return container;
        }
    }
}
