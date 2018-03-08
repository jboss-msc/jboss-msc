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
 * Depends on one or more dependencies.
 *
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 * @author <a href="mailto:flavia.rainone@jboss.com">Flavia Rainone</a>
 * @see Dependency
 */
interface Dependent {

    /**
     * Notify this dependent that one of its dependencies is available.
     */
    void dependencyAvailable();

    /**
     * Notify this dependent that one of its dependencies is unavailable.
     */
    void dependencyUnavailable();

    /**
     * Notify this dependent that one of its dependencies is up.
     */
    void dependencyUp();

    /**
     * Notify this dependent that one of its dependencies is down.
     */
    void dependencyDown();

    /**
     * Notify this dependent that one of its dependencies failed.
     */
    void dependencyFailed();

    /**
     * Notify this dependent that one of its depenencies was corrected.
     */
    void dependencySucceeded();

    /**
     * Get the controller of this dependent.
     *
     * @return the controller
     */
    ServiceControllerImpl<?> getDependentController();

}