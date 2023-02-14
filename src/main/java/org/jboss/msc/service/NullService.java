/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2022, Red Hat, Inc., and individual contributors
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

import java.io.Serializable;

/**
* @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
*/
class NullService implements Service<Void>, Serializable {

    private static final long serialVersionUID = 2463357698038752840L;

    static final NullService INSTANCE = new NullService();

    private NullService() {
    }

    public void start(final StartContext context) {
    }

    public void stop(final StopContext context) {
    }

    public Void getValue() {
        return null;
    }

    public String toString() {
        return "Null service";
    }

    public int hashCode() {
        return 97;
    }

    public boolean equals(final Object obj) {
        return this == obj;
    }

    protected Object readResolve() {
        return INSTANCE;
    }
}
