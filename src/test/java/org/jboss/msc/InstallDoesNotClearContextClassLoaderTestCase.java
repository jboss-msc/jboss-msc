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

package org.jboss.msc;

import org.jboss.msc.service.*;

import org.junit.Assert;
import org.junit.Test;

import java.util.function.Consumer;

/**
 * @author Stuart Douglas
 */
public class InstallDoesNotClearContextClassLoaderTestCase extends AbstractServiceTest {

    @Test
    public void testServiceInstallationDoesNotClearTCCL() {
        serviceContainer.addListener(new LifecycleListener() {
            @Override
            public void handleEvent(ServiceController<?> controller, LifecycleEvent event) {
            }
        });
        final ClassLoader classLoader = new ClassLoader() {

        };
        Thread.currentThread().setContextClassLoader(classLoader);
        try {
            ServiceBuilder<?> sb = serviceContainer.addService();
            ServiceName sn = ServiceName.of("nothingService");
            Consumer<String> providedValue = sb.provides(sn);
            sb.setInstance(Service.newInstance(providedValue, sn.toString()));
            sb.install();

            Assert.assertEquals(classLoader, Thread.currentThread().getContextClassLoader());
        } finally {
            Thread.currentThread().setContextClassLoader(null);
        }
    }

    @Test
    public void setAddListenerDoesNotClearTCCL() {
        serviceContainer.addListener(new LifecycleListener() {
            @Override
            public void handleEvent(ServiceController<?> controller, LifecycleEvent event) {
            }
        });
        final ClassLoader classLoader = new ClassLoader() {

        };
        Thread.currentThread().setContextClassLoader(classLoader);
        try {
            ServiceBuilder<?> sb = serviceContainer.addService();
            ServiceName sn = ServiceName.of("listenerService");
            Consumer<String> providedValue = sb.provides(sn);
            sb.setInstance(Service.newInstance(providedValue, sn.toString()));
            ServiceController<?> controller = sb.install();

            controller.addListener(new LifecycleListener() {
                @Override
                public void handleEvent(ServiceController<?> controller, LifecycleEvent event) {
                }
            });
            Assert.assertEquals(classLoader, Thread.currentThread().getContextClassLoader());
        } finally {
            Thread.currentThread().setContextClassLoader(null);
        }
    }
}
