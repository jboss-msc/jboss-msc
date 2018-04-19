/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2018, Red Hat, Inc., and individual contributors
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

package org.jboss.msc;

import org.jboss.msc.service.LifecycleContext;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.threads.AsyncFuture;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * A service is a thing which can be started and stopped.
 * A service may be started or stopped from any thread.
 * <p id="thread-safety">
 * Service implementation should always take care to protect any mutable state appropriately; however, the following
 * thread-safety properties and relationships are always guaranteed:
 * <ul>
 *     <li>Dependency service start operations always <em>happen-before</em> dependent service start operations</li>
 *     <li>Dependent service stop operations always <em>happen-before</em> dependency service stop operations</li>
 *     <li>Memory which is modified within a dependency service start operation will be visible to dependents (even if the writes
 *     are to non-volatile fields or were otherwise unprotected by locks or other memory barriers)</li>
 * </ul>
 * In general, services do not need to concern themselves with thread safety unless they contain state which can be used
 * or manipulated outside of the service dependency graph and lifecycle operations.
 * <p>
 * Service implementations may provide multiple values to their consumers.
 * <p>
 * Lifecycle operations may block as necessary.  Note that if enough concurrent service lifecycles are blocking, it is possible
 * that the thread pool will fill and no further lifecycle operations can complete until some of the existing lifecycle operations
 * complete.
 * <p>
 * It is important that lifecycle operations do not perform tasks that will block infinitely.  One example of such a task
 * would be a stop method which calls {@link System#exit(int)}.  Such a task will never complete, causing the service container
 * to never shut down, which in turn may cause the JVM to never exit, resulting in permanent deadlock.
 * <p>
 * It is also important that services not block on conditions which may only be resolved by other services. Such situations
 * must always be modeled as service dependencies to ensure correct operation and prevent a deadlock.
 * <p>
 * Lifecycle operations which are naturally asynchronous should consider use of the {@link LifecycleContext#asynchronous()} API
 * in order to increase parallelism and reduce resource consumption.  Examples of such operations include:
 * <ul>
 *     <li>Operations which run in the background and signal completion via a callback</li>
 *     <li>Operations which use a callback-driven {@code Future} variant like {@link CompletableFuture} or {@link AsyncFuture}</li>
 * </ul>
 * <p>
 * Lifecycle operations which can benefit from parallel execution of work should consider use of the {@link LifecycleContext#execute(Runnable)}
 * API in combination with the asynchronous API.  This can allow many threads to work in parallel to complete the lifecycle step.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
public interface Service {

    /**
     * A simple null service whose start and stop methods do nothing.
     */
    Service NULL = NullService.INSTANCE;

    /**
     * Start the service.  Do not return until the service has been fully started, unless an asynchronous service
     * start is performed.  All injections will be complete before this method is called.
     * <p>
     * If the service start involves any activities that may block, the asynchronous mechanism
     * provided by the {@code context} should be used. See the {@link Service class javadoc} for details.
     *
     * @param context the context which can be used to trigger an asynchronous service start
     * @throws StartException if the service could not be started for some reason
     */
    void start(StartContext context) throws StartException;

    /**
     * Stop the service.  Do not return until the service has been fully stopped, unless an asynchronous service
     * stop is performed.  All injections will remain intact until the service is fully stopped.  This method should
     * not throw an exception.
     * <p>
     * If the service start involves any activities that may block, the asynchronous mechanism
     * provided by the {@code context} should be used. See the {@link Service class javadoc} for details.
     *
     * @param context the context which can be used to trigger an asynchronous service stop
     */
    void stop(StopContext context);

    /**
     * Factory for services providing single value.
     *
     * @param injector target
     * @param value to assign
     * @param <V> provided value type
     * @return new service instance
     */
    static <V> Service newInstance(final Consumer<V> injector, final V value) {
        return new SimpleService<>(injector, value);
    }

}
