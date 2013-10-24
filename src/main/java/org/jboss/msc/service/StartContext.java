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

/**
 * The start lifecycle context.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
public interface StartContext extends LifecycleContext {

    /**
     * Call within the service lifecycle method to trigger an <em>asynchronous</em> lifecycle action.
     * This action will not be considered complete until indicated so by calling 
     * either {@link #complete()} or {@link #failed(StartException)} method on this interface.
     */
    void asynchronous();

    /**
     * Call when start lifecycle action has failed for some reason.
     *
     * @param reason the reason for the failure
     * @throws IllegalStateException if called after {@link #complete()} was called
     */
    void failed(StartException reason) throws IllegalStateException;

    /**
     * Call when either <em>synchronous</em> or <em>asynchronous</em> lifecycle action is complete.
     *
     * @throws IllegalStateException if called after {@link #failed(StartException)} was called or if called twice in a row
     */
    void complete() throws IllegalStateException;

    /**
     * Get a service target which may be used to add child services.  Child services have an implicit dependency on
     * their parent, and are automatically removed when the parent service stops (or if the parent service fails
     * during startup).
     * <p>
     * Any listeners added directly to this service target will be added to child services when they are installed.
     * Listeners added directly to this service target will be applied recursively to descendants as well, as long as
     * the listener exists on this service target.  To avoid this recursive behavior, apply listeners to any sub-target
     * of this this target (see {@link ServiceTarget#subTarget()} for more information about sub-targets).
     * <p>
     * Note that any listeners existing on the starting service are <b>not</b> applied to child services automatically;
     * they must be explicitly added to the child service target.
     *
     * @return the child target
     */
    ServiceTarget getChildTarget();
}
