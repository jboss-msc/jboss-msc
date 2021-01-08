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

package org.jboss.msc.inject;

/**
 * Reference to a writable dependency.
 * <p>
 * Implementations of this interface are thread safe.
 *
 * @param <T> referenced writable dependency type
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 * @deprecated Use {@link org.jboss.msc.service.ServiceBuilder#provides(org.jboss.msc.service.ServiceName...)}
 * method instead. This class will be removed in a future release.
 */
@Deprecated
public interface Injector<T> {

    /**
     * Inject the given value.
     *
     * @param value the value
     * @throws InjectionException if the injection failed
     */
    void inject(T value) throws InjectionException;

    /**
     * Uninject the given value (in other words, cancel or undo a previous injection).  Only called after {@code inject()}
     * has been called.
     */
    void uninject();

}
