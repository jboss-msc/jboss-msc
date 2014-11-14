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
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.concurrent.TimeUnit;

/**
 * A service container which manages a set of running services.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public interface ServiceContainer extends ServiceTarget, ServiceRegistry {

    /**
     * Stop all services within this container.
     */
    void shutdown();

    /**
     * Determine whether the container is completely shut down.
     *
     * @return {@code true} if shutdown is complete
     */
    boolean isShutdownComplete();

    /**
     * Add a terminate listener to this container.
     * The added {@code listener} will be invoked when this container shutdown process is complete.
     *  
     * @param listener the listener
     */
    void addTerminateListener(TerminateListener listener);

    /**
     * Causes the current thread to wait until the container is shutdown.
     * 
     * @throws InterruptedException if the current thread is interrupted
     *         while waiting
     */
    void awaitTermination() throws InterruptedException;

    /**
     * Causes the current thread to wait until the container is shutdown.
     *
     * @param timeout the maximum time to wait
     * @param unit the time unit of the {@code timeout} argument
     * @throws InterruptedException if the current thread is interrupted
     *         while waiting
     */
    void awaitTermination(long timeout, TimeUnit unit) throws InterruptedException;

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
     * Get the name of this service container.
     *
     * @return the container name
     */
    String getName();

    /**
     * The factory class for service containers.
     */
    class Factory {

        private static final int MAX_THREADS_COUNT;

        private Factory() {
        }

        static {
            MAX_THREADS_COUNT = AccessController.doPrivileged(new PrivilegedAction<Integer>() {
                public Integer run() {
                    return Integer.getInteger("jboss.msc.max.container.threads", 8);
                }
            });
        }

        /**
         * Create a new instance with a generated name and default thread pool.
         *
         * @return a new service container instance
         */
        public static ServiceContainer create() {
            return new ServiceContainerImpl(null, calculateCoreSize(), 30L, TimeUnit.SECONDS);
        }

        /**
         * Create a new instance with a given name and default thread pool.
         *
         * @param name the name of the new container
         * @return a new service container instance
         */
        public static ServiceContainer create(String name) {
            return new ServiceContainerImpl(name, calculateCoreSize(), 30L, TimeUnit.SECONDS);
        }

        /**
         * Create a new instance with a generated name and specified initial thread pool settings.
         *
         *
         * @param coreSize the core pool size (must be greater than zero)
         * @param keepAliveTime the amount of time that non-core threads should linger without tasks
         * @param keepAliveTimeUnit the time unit for {@code keepAliveTime}
         * @return a new service container instance
         */
        public static ServiceContainer create(int coreSize, long keepAliveTime, TimeUnit keepAliveTimeUnit) {
            return new ServiceContainerImpl(null, calculateCoreSize(coreSize), keepAliveTime, keepAliveTimeUnit);
        }

        /**
         * Create a new instance with a given name and specified initial thread pool settings.
         *
         *
         * @param name the name of the new container
         * @param coreSize the core pool size (must be greater than zero)
         * @param keepAliveTime the amount of time that non-core threads should linger without tasks
         * @param keepAliveTimeUnit the time unit for {@code keepAliveTime}
         * @return a new service container instance
         */
        public static ServiceContainer create(String name, int coreSize, long keepAliveTime, TimeUnit keepAliveTimeUnit) {
            return new ServiceContainerImpl(name, calculateCoreSize(coreSize), keepAliveTime, keepAliveTimeUnit);
        }

        private static int calculateCoreSize() {
            int cpuCount = Runtime.getRuntime().availableProcessors();
            return calculateCoreSize(Math.max(cpuCount << 1, 2));
        }

        private static int calculateCoreSize(int coreSize) {
            return Math.min(coreSize, MAX_THREADS_COUNT);
        }
    }

    /**
     * A listener for notification of container shutdown.
     * 
     * @see ServiceContainer#addTerminateListener(TerminateListener)
     * @see ServiceContainer#shutdown()
     */
    interface TerminateListener {
        /**
         * Notifies this listener that the container is shutdown.<br> At the moment this listener is
         * requested to handle termination, all services in the container are stopped and removed.
         * 
         * @param info information regarding the container shutdown process
         */
        void handleTermination(Info info);

        /**
         * Container shutdown information.
         */
        final class Info {
            private final long shutdownInitiated;
            private final long shutdownCompleted;

            Info(final long shutdownInitiated, final long shutdownCompleted) {
                this.shutdownInitiated = shutdownInitiated;
                this.shutdownCompleted = shutdownCompleted;
            }

            /**
             * Returns the time the shutdown process was initiated, in nanoseconds.
             * 
             * @return the shutdown initiated time
             */
            public long getShutdownInitiated() {
                return shutdownInitiated;
            }

            /**
             * Returns the time the shutdown process was completed, in nanoseconds.
             * 
             * @return the shutdown completed time
             */
            public long getShutdownCompleted() {
                return shutdownCompleted;
            }
        }
    }
}
