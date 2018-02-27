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

package org.jboss.msc.inject;

/**
 * Utility class to construct injector types.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 * @deprecated use {@link org.jboss.msc.service.ServiceBuilder#provides(org.jboss.msc.service.ServiceName...)}
 * method instead. This class will be removed in future releases.
 */
@Deprecated
public final class Injectors {

    private Injectors() {
    }

    /**
     * Construct a casting injector.
     *
     * @param injector an injector of any type
     * @param type a class of the injected type
     * @param <T> the type of the injected value
     * @return an Injector which implements {@code Injector<Object>}
     */
    public static <T> Injector<Object> cast(Injector<T> injector, Class<T> type) {
        return new CastingInjector<T>(injector, type);
    }
}
