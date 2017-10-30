/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017, Red Hat, Inc., and individual contributors
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

import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;

/**
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
final class ContainerShutdownListener {
    @SuppressWarnings({ "UnusedDeclaration" })
    private volatile int count = 1;
    @SuppressWarnings({ "UnusedDeclaration" })
    private volatile int done;
    @SuppressWarnings({ "RawUseOfParameterizedType" })
    private static final AtomicIntegerFieldUpdater<ContainerShutdownListener> countUpdater = AtomicIntegerFieldUpdater.newUpdater(ContainerShutdownListener.class, "count");
    @SuppressWarnings({ "RawUseOfParameterizedType" })
    private static final AtomicIntegerFieldUpdater<ContainerShutdownListener> doneUpdater = AtomicIntegerFieldUpdater.newUpdater(ContainerShutdownListener.class, "done");
    private final Runnable callback;

    ContainerShutdownListener(final Runnable callback) {
        this.callback = callback;
    }

    final void controllerAlive() {
        countUpdater.getAndIncrement(this);
    }

    final void controllerDied() {
        tick();
    }

    final void done() {
        if (doneUpdater.getAndSet(this, 1) == 0) {
            tick();
        }
    }

    private void tick() {
        if (countUpdater.decrementAndGet(this) == 0) {
            callback.run();
        }
    }
}
