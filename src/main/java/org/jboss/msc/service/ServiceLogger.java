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

package org.jboss.msc.service;

import static org.jboss.logging.Logger.Level.ERROR;
import static org.jboss.logging.Logger.Level.INFO;
import static org.jboss.logging.Logger.Level.WARN;

import java.io.IOException;

import org.jboss.logging.Logger;
import org.jboss.logging.annotations.Cause;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;

/**
 * The logger interface for the service controller.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
@MessageLogger(projectCode = "MSC")
interface ServiceLogger {

    // **********************************************************
    // **********************************************************
    // **                                                      **
    // ** IMPORTANT - Be sure to check against the 2.x         **
    // **     codebase before assigning additional IDs         **
    // **     in this file!                                    **
    // **                                                      **
    // **********************************************************
    // **********************************************************

    ServiceLogger ROOT = Logger.getMessageLogger(ServiceLogger.class, "org.jboss.msc");
    ServiceLogger SERVICE = Logger.getMessageLogger(ServiceLogger.class, "org.jboss.msc.service");
    ServiceLogger FAIL = Logger.getMessageLogger(ServiceLogger.class, "org.jboss.msc.service.fail");

    @LogMessage(level = INFO)
    @Message(value = "JBoss MSC version %s")
    void greeting(String version);

    @LogMessage(level = ERROR)
    @Message(id = 1, value = "Failed to start %s")
    void startFailed(@Cause StartException cause, ServiceName serviceName);

    @LogMessage(level = ERROR)
    @Message(id = 2, value = "Invocation of listener \"%s\" failed")
    void listenerFailed(@Cause Throwable cause, Object listener);

    @LogMessage(level = WARN)
    @Message(id = 3, value = "Exception thrown after start was already completed in %s")
    void exceptionAfterComplete(@Cause Throwable cause, ServiceName serviceName);

    @LogMessage(level = WARN)
    @Message(id = 4, value = "Failure during stop of %s")
    void stopFailed(@Cause Throwable cause, ServiceName serviceName);

    @LogMessage(level = WARN)
    @Message(id = 5, value = "Unexpected disappearance of %s during stop")
    void stopServiceMissing(ServiceName serviceName);

    @LogMessage(level = WARN)
    @Message(id = 6, value = "Uninjection \"%2$s\" of %1$s failed unexpectedly")
    void uninjectFailed(@Cause Throwable cause, ServiceName serviceName, ValueInjection<?> valueInjection);

    @LogMessage(level = WARN)
    @Message(id = 7, value = "An internal service error has occurred while processing an operation on %s")
    void internalServiceError(@Cause Throwable cause, ServiceName serviceName);

    @LogMessage(level = ERROR)
    @Message(id = 8, value = "Worker thread %s threw an uncaught exception")
    void uncaughtException(@Cause Throwable cause, Thread thread);

    @LogMessage(level = WARN)
    @Message(id = 9, value = "An error occurred while trying to close the profile output file: %s")
    void profileOutputCloseFailed(/* ! @Cause */ IOException cause);

    @LogMessage(level = ERROR)
    @Message(id = 10, value = "Failed to register MBean with MBeanServer")
    void mbeanFailed(@Cause Exception e);

    @Message(id = 11, value = "Service not started")
    IllegalStateException serviceNotStarted();

    @LogMessage(level = ERROR)
    @Message(id = 12, value = "Injection failed for service %s")
    void injectFailed(@Cause Throwable cause, ServiceName serviceName);

    @LogMessage(level = WARN)
    @Message(id = 13, value = "Failed to retrieve platform MBeanServer")
    void mbeanServerNotAvailable(@Cause Exception e);

}
