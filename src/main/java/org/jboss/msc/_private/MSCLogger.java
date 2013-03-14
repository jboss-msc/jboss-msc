/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jboss.msc._private;

import org.jboss.logging.Logger;
import org.jboss.logging.annotations.Cause;
import org.jboss.logging.annotations.LogMessage;
import org.jboss.logging.annotations.Message;
import org.jboss.logging.annotations.MessageLogger;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartException;
import org.jboss.msc.txn.Committable;
import org.jboss.msc.txn.Executable;
import org.jboss.msc.txn.Revertible;
import org.jboss.msc.txn.Validatable;
import org.jboss.msc.value.Listener;
import org.jboss.msc.value.WritableValue;

import static org.jboss.logging.Logger.Level.ERROR;
import static org.jboss.logging.Logger.Level.FATAL;
import static org.jboss.logging.Logger.Level.INFO;
import static org.jboss.logging.Logger.Level.WARN;

/**
 * MSC2 logging utilities.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
@MessageLogger(projectCode = "MSC")
public interface MSCLogger {

    // **********************************************************
    // **********************************************************
    // **                                                      **
    // ** IMPORTANT - Be sure to check against the 1.x         **
    // **     codebase before assigning additional IDs         **
    // **     in this file!                                    **
    // **                                                      **
    // **********************************************************
    // **********************************************************

    MSCLogger ROOT = Logger.getMessageLogger(MSCLogger.class, "org.jboss.msc");
    MSCLogger SERVICE = Logger.getMessageLogger(MSCLogger.class, "org.jboss.msc.service");
    MSCLogger FAIL = Logger.getMessageLogger(MSCLogger.class, "org.jboss.msc.service.fail");
    MSCLogger TASK = Logger.getMessageLogger(MSCLogger.class, "org.jboss.msc.task");
    MSCLogger TXN = Logger.getMessageLogger(MSCLogger.class, "org.jboss.msc.txn");
    MSCLogger INJECT = Logger.getMessageLogger(MSCLogger.class, "org.jboss.msc.inject");

    @LogMessage(level = INFO)
    @Message(value = "JBoss MSC version %s")
    void greeting(String version);

    @LogMessage(level = ERROR)
    @Message(id = 1, value = "Failed to start %s")
    void startFailed(@Cause StartException cause, ServiceName serviceName);

    @LogMessage(level = ERROR)
    @Message(id = 2, value = "Invocation of listener \"%s\" failed")
    void listenerFailed(@Cause Throwable cause, Listener<?> listener);

    @LogMessage(level = WARN)
    @Message(id = 3, value = "Exception thrown after start was already completed in %s")
    void exceptionAfterComplete(@Cause Throwable cause, ServiceName serviceName);

    @LogMessage(level = WARN)
    @Message(id = 4, value = "Failure during stop of %s")
    void stopFailed(@Cause Throwable cause, ServiceName serviceName);

    @LogMessage(level = WARN)
    @Message(id = 5, value = "Unexpected disappearance of %s during stop")
    void stopServiceMissing(ServiceName serviceName);

    /*
     * This method is for uninjection failures on behalf of a service.  See also id = 100
     */
    @LogMessage(level = WARN)
    @Message(id = 6, value = "Uninjection \"%2$s\" of %1$s failed unexpectedly")
    void uninjectFailed(@Cause Throwable cause, ServiceName serviceName, WritableValue<?> valueInjection);

    @LogMessage(level = WARN)
    @Message(id = 7, value = "An internal service error has occurred while processing an operation on %s")
    void internalServiceError(@Cause Throwable cause, ServiceName serviceName);

    // id = 8: unexpected worker thread exception (N/A)

    // id = 9: profile output file close error (N/A)

    // id = 10: failed mbean registration (N/A)

    @Message(id = 11, value = "Service not started")
    IllegalStateException serviceNotStarted();

    @LogMessage(level = ERROR)
    @Message(id = 12, value = "Execution of task \"%s\" caused an exception")
    void taskExecutionFailed(@Cause Throwable cause, Executable<?> task);

    @LogMessage(level = ERROR)
    @Message(id = 13, value = "Validation of task \"%s\" caused an exception")
    void taskValidationFailed(@Cause Throwable cause, Validatable task);

    @LogMessage(level = ERROR)
    @Message(id = 14, value = "Commit of task \"%s\" caused an exception")
    void taskCommitFailed(@Cause Throwable cause, Committable task);

    @LogMessage(level = ERROR)
    @Message(id = 15, value = "Rollback of task \"%s\" caused an exception")
    void taskRollbackFailed(@Cause Throwable cause, Revertible task);

    @LogMessage(level = FATAL)
    @Message(id = 16, value = "Internal task \"%s\" execution failed (transaction is likely permanently jammed)")
    void runnableExecuteFailed(@Cause Throwable cause, Runnable command);

    // jump to 100...

    /*
     * This method is for uninjection failures which are not service-related.  See also id = 6
     */
    @LogMessage(level = Logger.Level.WARN)
    @Message(id = 100, value = "Unexpected failure to uninject %s")
    void uninjectFailed(@Cause Throwable cause, Object target);

    @Message(id = 101, value = "Parameter %s is null")
    IllegalArgumentException methodParameterIsNull(final String parameterName);

    @Message(id = 102, value = "%s must be at most ERROR")
    IllegalArgumentException illegalSeverity(final String parameterName);

    @Message(id = 103, value = "Too many active transactions")
    IllegalStateException tooManyActiveTransactions();
}
