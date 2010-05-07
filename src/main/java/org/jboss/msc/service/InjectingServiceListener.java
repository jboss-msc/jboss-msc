/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat Middleware LLC, and individual contributors
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

import org.jboss.msc.inject.Injector;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class InjectingServiceListener<T> implements ServiceListener<T> {
    private final Injector<T> injector;

    public InjectingServiceListener(final Injector<T> injector) {
        this.injector = injector;
    }

    public void serviceStarting(final ServiceController<? extends T> controller) {
    }

    public void serviceStarted(final ServiceController<? extends T> controller) {
        injector.inject(controller.getValue());
    }

    public void serviceFailed(final ServiceController<? extends T> controller, final StartException reason) {
    }

    public void serviceStopping(final ServiceController<? extends T> controller) {
        injector.uninject();
    }

    public void serviceStopped(final ServiceController<? extends T> controller) {
    }
}
