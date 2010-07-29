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

import org.jboss.logging.Cause;
import org.jboss.logging.Logger;

/**
 * @deprecated Will be removed when the logger tooling is complete.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
@Deprecated
public final class ServiceLogger_$logger implements ServiceLogger {

    public ServiceLogger_$logger(Logger logger) {
    }

    public void greeting(final String version) {
    }

    public void listenerFailed(@Cause final Throwable cause, final ServiceListener<?> listener) {
    }

    public void exceptionAfterComplete(@Cause final Throwable cause, final ServiceName serviceName) {
    }

    public void stopFailed(@Cause final Throwable cause, final ServiceName serviceName) {
    }

    public void stopServiceMissing(final ServiceName serviceName) {
    }

    public void uninjectFailed(@Cause final Throwable cause, final ServiceName serviceName, final ValueInjection<?> valueInjection) {
    }

    public void internalServiceError(@Cause final Throwable cause, final ServiceName serviceName) {
    }
}
