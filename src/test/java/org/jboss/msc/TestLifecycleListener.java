/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2023, Red Hat, Inc., and individual contributors
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

package org.jboss.msc;

import org.jboss.msc.service.LifecycleEvent;
import org.jboss.msc.service.LifecycleListener;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;

import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;


final class TestLifecycleListener implements LifecycleListener {

    private static final Object lock = new Object();
    private final Set<ServiceName> downValues = new ConcurrentSkipListSet<>();
    private final Set<ServiceName> upValues = new ConcurrentSkipListSet<>();
    private final Set<ServiceName> failedValues = new ConcurrentSkipListSet<>();
    private final Set<ServiceName> removedValues = new ConcurrentSkipListSet<>();

    @Override
    public void handleEvent(ServiceController<?> controller, LifecycleEvent event) {
        synchronized (lock) {
            Set<ServiceName> providedValues = controller.provides();
            if (event == LifecycleEvent.DOWN) {
                downValues.addAll(providedValues);
                upValues.removeAll(providedValues);
                failedValues.removeAll(providedValues);
            } else if (event == LifecycleEvent.UP) {
                upValues.addAll(providedValues);
                downValues.removeAll(providedValues);
                failedValues.removeAll(providedValues);
            } else if (event == LifecycleEvent.FAILED) {
                failedValues.addAll(providedValues);
                downValues.removeAll(providedValues);
            } else if (event == LifecycleEvent.REMOVED) {
                removedValues.addAll(providedValues);
                downValues.removeAll(providedValues);
            }
        }
    }

    Set<ServiceName> downValues() {
        synchronized (lock) {
            return Set.copyOf(downValues);
        }
    }

    Set<ServiceName> upValues() {
        synchronized (lock) {
            return Set.copyOf(upValues);
        }
    }

    Set<ServiceName> failedValues() {
        synchronized (lock) {
            return Set.copyOf(failedValues);
        }
    }

    Set<ServiceName> removedValues() {
        synchronized (lock) {
            return Set.copyOf(removedValues);
        }
    }

    void clear() {
        synchronized (lock) {
            downValues.clear();
            upValues.clear();
            failedValues.clear();
            removedValues.clear();
        }
    }

}
