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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * A utility class for service actions.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 * @deprecated This class will be removed in a future release.
 */
@Deprecated
public final class ServiceUtils {

    private ServiceUtils() {
    }

    /**
     * Undeploy all the controllers and call the given runnable task when complete.
     *
     * @param completeTask the complete task
     * @param controllers the controllers to undeploy
     */
    public static void undeployAll(Runnable completeTask, ServiceController<?>... controllers) {
        undeployAll(completeTask, controllers == null ? Collections.<ServiceController<?>>emptyList(): Arrays.asList(controllers));
    }

    /**
     * Undeploy all the controllers and call the given runnable task when complete.  The given controllers list
     * should not be modified while this method runs, or the results will be undefined.
     *
     * @param completeTask the complete task
     * @param controllers the controllers to undeploy
     */
    public static void undeployAll(final Runnable completeTask, final List<ServiceController<?>> controllers) {
        final MultipleRemoveListener<Runnable> listener = MultipleRemoveListener.create(completeTask);
        for (ServiceController<?> controller : controllers == null ? Collections.<ServiceController<?>>emptyList() : controllers) {
            if (controller != null) {
                controller.setMode(ServiceController.Mode.REMOVE);
                controller.addListener(listener);
            }
        }
        listener.done();
    }

    /**
     * Determine whether the given thread is a service thread.
     *
     * @param thread the thread to test
     * @return {@code true} if it is a service thread, {@code false} otherwise
     */
    public static boolean isServiceThread(Thread thread) {
        return thread instanceof ServiceContainerImpl.ServiceThread;
    }

    /**
     * Determine whether the given thread is a service thread which is associated with the given container.
     *
     * @param thread the thread to test
     * @param container the container to compare to
     * @return {@code true} if it is a service thread, {@code false} otherwise
     */
    public static boolean isServiceThread(Thread thread, ServiceContainer container) {
        return thread instanceof ServiceContainerImpl.ServiceThread && ((ServiceContainerImpl.ServiceThread) thread).getContainer() == container;
    }
}
