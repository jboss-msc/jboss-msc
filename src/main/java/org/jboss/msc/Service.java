/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2021, Red Hat, Inc., and individual contributors
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

import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;

import java.util.function.Consumer;

/**
 * A service is a thing which can be started and stopped.
 * A service may be started or stopped from any thread.
 * Service implementation should always take care to protect any mutable state appropriately.
 * Service may provide multiple values to its consumers.
 * <p>
 * When writing MSC service implementations, your {@link #start(StartContext)} and {@link #stop(StopContext)}
 * methods must never block. This means these methods must not:
 * <ul>
 * <li>Use network connections</li>
 * <li>Wait for network connections</li>
 * <li>Sleep</li>
 * <li>Wait on a condition</li>
 * <li>Wait on a count down latch</li>
 * <li>Call any method which may do any of the above</li>
 * <li>Wait for termination of a thread pool or other service</li>
 * <li>Wait for another service to change state</li>
 * </ul>
 *
 * If your service start/stop does any of these things, you must use the asynchronous start/stop mechanism
 * ({@link org.jboss.msc.service.LifecycleContext#asynchronous()}) and do one of the following:
 *
 * <ul>
 * <li>Initiate your task in start()/stop(), and utilize a callback (NIO, ThreadPoolExecutor.terminated(), etc.) to call
 * {@link org.jboss.msc.service.LifecycleContext#complete()} when your start/stop completes instead of blocking</li>
 * <li>Delegate your blocking task to a thread pool ({@code Executor}) which calls
 * {@link org.jboss.msc.service.LifecycleContext#complete()} when done</li>
 * <li>Use proper dependencies instead of explicitly waiting for services in your start/stop</li>
 * </ul>
 * <p>
 * Note that using {@link org.jboss.msc.service.LifecycleContext#execute(Runnable)} to execute the blocking task is also not permissible.
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
