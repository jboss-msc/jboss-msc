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

import org.jboss.msc.service.ServiceContainer;
import java.util.logging.Logger;

import org.junit.After;
import org.junit.Before;

abstract class AbstractServiceTest {

    protected volatile ServiceContainer serviceContainer;
    private volatile boolean shutdownOnTearDown;

    @Before
    public void setUp() {
        Logger.getLogger("").fine("Setting up test " + getClass());
        serviceContainer = ServiceContainer.Factory.create();
        shutdownOnTearDown = true;
    }

    @After
    public void tearDown() {
        Logger.getLogger("").fine("Tearing down test " + getClass());
        if (shutdownOnTearDown) {
            shutdownContainer();
        }
        serviceContainer = null;
    }

    /**
     * Shutdowns the container.
     */
    public void shutdownContainer() {
        serviceContainer.shutdown();
        try {
            serviceContainer.awaitTermination();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        shutdownOnTearDown = false;
    }

}
