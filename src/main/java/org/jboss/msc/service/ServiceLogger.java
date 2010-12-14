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

import java.io.IOException;
import org.jboss.logging.Cause;
import org.jboss.logging.LogMessage;
import org.jboss.logging.Logger;
import org.jboss.logging.Message;
import org.jboss.logging.MessageLogger;

import static org.jboss.logging.Logger.Level.*;

/**
 * The logger interface for the service controller.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
@MessageLogger(projectCode = "MSC")
interface ServiceLogger {

    ServiceLogger INSTANCE = Logger.getMessageLogger(ServiceLogger.class, "org.jboss.msc");

    @LogMessage(level = INFO)
    @Message(id = 1, value = "JBoss MSC version %s")
    void greeting(String version);

    @LogMessage(level = ERROR)
    @Message(id = 2, value = "Service \"%s\" failed to start")
    void startFailed(@Cause StartException cause, ServiceName serviceName);

    @LogMessage(level = ERROR)
    @Message(id = 3, value = "Invocation of listener '%s' failed")
    void listenerFailed(@Cause Throwable cause, ServiceListener<?> listener);

    @LogMessage(level = WARN)
    @Message(id = 4, value = "Exception thrown after start was already completed for service \"%s\"")
    void exceptionAfterComplete(@Cause Throwable cause, ServiceName serviceName);

    @LogMessage(level = WARN)
    @Message(id = 5, value = "Stop of service \"%s\" failed")
    void stopFailed(@Cause Throwable cause, ServiceName serviceName);

    @LogMessage(level = WARN)
    @Message(id = 6, value = "Service \"%s\" disappeared before stop")
    void stopServiceMissing(ServiceName serviceName);

    @LogMessage(level = WARN)
    @Message(id = 7, value = "Uninjection \"%2$s\" of service \"%1$s\" failed unexpectedly")
    void uninjectFailed(@Cause Throwable cause, ServiceName serviceName, ValueInjection<?> valueInjection);

    @LogMessage(level = WARN)
    @Message(id = 8, value = "An internal service error has occurred while processing an operation on service \"%s\"")
    void internalServiceError(@Cause Throwable cause, ServiceName serviceName);

    @LogMessage(level = ERROR)
    @Message(id = 9, value = "A worker thread threw an uncaught exception")
    void uncaughtException(@Cause Throwable cause);

    @LogMessage(level = WARN)
    @Message(id = 10, value = "An error occurred while trying to close the profile output file: %s")
    void profileOutputCloseFailed(/* ! @Cause */ IOException cause);

    @LogMessage(level = ERROR)
    @Message(id = 12, value = "Failed to register MBean with MBeanServer")
    void mbeanFailed(@Cause Exception e);
}
