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

import java.util.concurrent.Executor;

/**
 * A context object for lifecycle events.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public interface LifecycleContext extends Executor {

    /**
     * Call within the service lifecycle method to trigger an <em>asynchronous</em> lifecycle action.  This action
     * will not be considered complete until indicated so by calling a method on this interface.
     *
     * @throws IllegalStateException if called outside of the main service lifecycle method
     */
    void asynchronous() throws IllegalStateException;

    /**
     * Call when an <em>asynchronous</em> lifecycle action is complete.
     *
     * @throws IllegalStateException if called before {@link #asynchronous()} is called, or if the action was already
     * completed
     */
    void complete() throws IllegalStateException;

    /**
     * Get the amount of time elapsed since the start or stop was initiated, in nanoseconds.
     *
     * @return the elapsed time
     */
    long getElapsedTime();

    /**
     * Get the associated service controller.
     *
     * @return the service controller
     */
    ServiceController<?> getController();

    /**
     * Execute a task asynchronously using the MSC task executor.
     * <p>
     * <strong>Note:</strong> This method should not be used for executing tasks that may block,
     * particularly from within a service's {@link Service#start(StartContext)} or {@link Service#stop(StopContext)}
     * methods. See {@link Service the Service class javadoc} for further details.
     *
     * @param command the command to execute
     */
    @Override
    void execute(Runnable command);
}
