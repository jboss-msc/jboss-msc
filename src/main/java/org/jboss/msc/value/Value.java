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

package org.jboss.msc.value;

/**
 * Reference to a readonly dependency value.
 * User code should never store referenced value in global fields
 * and should always use {@link #getValue()} method instead.
 * <p>
 * Implementations of this interface are thread safe.
 *
 * @param <T> referenced dependency value type
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 * @deprecated Use {@link org.jboss.msc.service.ServiceBuilder#requires(org.jboss.msc.service.ServiceName)}
 * method instead. This class will be removed in a future release.
 */
@Deprecated
public interface Value<T> {

    /**
     * Get the actual dependency value.
     *
     * @return the actual dependency value
     * @throws IllegalStateException if the value is time-sensitive and the current state does not allow retrieval.
     * @throws IllegalArgumentException when the value cannot be read due to misconfiguration 
     */
    T getValue() throws IllegalStateException, IllegalArgumentException;

}
