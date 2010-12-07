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
import java.util.concurrent.TimeUnit;

import org.jboss.msc.inject.InjectionException;
import org.jboss.msc.inject.Injector;

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

    /**
     * A convenience injector for the container executor.  This class makes it easier to implement
     * a service which configures a thread pool on a container.
     */
    class ExecutorInjector implements Injector<Executor> {

        private final ServiceContainer container;

        private ExecutorInjector(final ServiceContainer container) {
            this.container = container;
        }

        public static ExecutorInjector create(final ServiceContainer container) {
            return new ExecutorInjector(container);
        }

        public void inject(final Executor value) throws InjectionException {
            container.setExecutor(value);
        }

        public void uninject() {
            container.setExecutor(null);
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
