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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;

import org.junit.Test;

import java.util.function.Consumer;

/**
 * Test to verify the behavior of scenarios involving multiple listeners.
 *
 * @author John Bailey
 */
public class MultipleListenersTestCase extends AbstractServiceTest {

    private static final ServiceName firstServiceName = ServiceName.of("firstService");
    private static final ServiceName secondServiceName = ServiceName.of("secondService");

    @Test
    public void test1() throws Exception {
        final TestLifecycleListener listenerOne = new TestLifecycleListener();
        final TestLifecycleListener listenerTwo = new TestLifecycleListener();
        serviceContainer.addListener(listenerOne);
        serviceContainer.addListener(listenerTwo);

        ServiceBuilder<?> sb = serviceContainer.addService();
        Consumer<String> providedValue = sb.provides(firstServiceName);
        sb.setInstance(Service.newInstance(providedValue, firstServiceName.toString()));
        sb.install();

        sb = serviceContainer.addService();
        providedValue = sb.provides(secondServiceName);
        sb.setInstance(Service.newInstance(providedValue, secondServiceName.toString()));
        sb.install();

        serviceContainer.awaitStability();

        assertEquals(2, listenerOne.upValues().size());
        assertTrue(listenerOne.upValues().contains(firstServiceName));
        assertTrue(listenerOne.upValues().contains(secondServiceName));

        assertEquals(2, listenerTwo.upValues().size());
        assertTrue(listenerTwo.upValues().contains(firstServiceName));
        assertTrue(listenerTwo.upValues().contains(secondServiceName));
    }

    @Test
    public void test2() throws Exception {
        final TestLifecycleListener listenerOne = new TestLifecycleListener();
        final TestLifecycleListener listenerTwo = new TestLifecycleListener();
        serviceContainer.addListener(listenerOne);
        serviceContainer.addListener(listenerTwo);

        ServiceBuilder<?> sb = serviceContainer.addService();
        Consumer<String> providedValue = sb.provides(firstServiceName);
        sb.setInstance(Service.newInstance(providedValue, firstServiceName.toString()));
        sb.install();

        final ServiceTarget subTarget = serviceContainer.subTarget();
        subTarget.addListener(listenerTwo);

        sb = subTarget.addService();
        providedValue = sb.provides(secondServiceName);
        sb.setInstance(Service.newInstance(providedValue, secondServiceName.toString()));
        sb.install();

        serviceContainer.awaitStability();

        assertEquals(2, listenerOne.upValues().size());
        assertTrue(listenerOne.upValues().contains(firstServiceName));
        assertTrue(listenerOne.upValues().contains(secondServiceName));

        assertEquals(2, listenerTwo.upValues().size());
        assertTrue(listenerTwo.upValues().contains(firstServiceName));
        assertTrue(listenerTwo.upValues().contains(secondServiceName));
    }

}
