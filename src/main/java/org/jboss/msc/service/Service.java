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

import org.jboss.msc.value.ImmediateValue;
import org.jboss.msc.value.Value;

/**
 * A service is a thing which can be started and stopped.  A service may be started or stopped from any thread.  In
 * general, injections will always happen from the same thread that will call {@code start()}, and uninjections will
 * always happen from the same thread that had called {@code stop()}.  However no other guarantees are made with respect
 * to locking or thread safety; a robust service implementation should always take care to protect any mutable state
 * appropriately.
 * <p>
 * The value type specified by this service is used by default by consumers of this service, and should represent the
 * public interface of this service, which may or may not be the same as the implementing type of this service.
 *
 * @param <T> the type of value that this service provides; may be {@link Void}
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public interface Service<T> extends Value<T> {

    /**
     * Start the service.  Do not return until the service has been fully started, unless an asynchronous service
     * start is performed.  All injections will be complete before this method is called.
     *
     * @param context the context which can be used to trigger an asynchronous service start
     * @throws StartException if the service could not be started for some reason
     */
    void start(StartContext context) throws StartException;

    /**
     * Stop the service.  Do not return until the service has been fully stopped, unless an asynchronous service
     * stop is performed.  All injections will remain intact until the service is fully stopped.  This method should
     * not throw an exception.
     *
     * @param context the context which can be used to trigger an asynchronous service stop
     */
    void stop(StopContext context);

    /**
     * A simple null service which performs no start or stop action.
     */
    Service<Void> NULL = NullService.INSTANCE;

    /**
     * A value which resolves to the {@link #NULL null service}.
     */
    Value<Service<Void>> NULL_VALUE = new ImmediateValue<Service<Void>>(NULL);
}
