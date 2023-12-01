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

import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.junit.Before;
import org.junit.Test;

import java.util.function.Consumer;

/**
 * Test for scenarios where a service has a duplicate dependency on another service (by depending on
 * aliases).
 * 
 * @author <a href="mailto:flavia.rainone@jboss.com">Flavia Rainone</a>
 */
public class DuplicateDependencyTestCase extends AbstractServiceTest {

    private static final ServiceName firstServiceName = ServiceName.of("first", "service");
    private static final ServiceName secondServiceName = ServiceName.of("second", "service");
    private static final ServiceName thirdServiceName = ServiceName.of("third", "service");
    private static final ServiceName secondServiceAlias1 = ServiceName.of("second", "alias", "1");
    private static final ServiceName secondServiceAlias2 = ServiceName.of("second", "alias", "2");
    private static final ServiceName secondServiceAlias3 = ServiceName.of("second", "alias", "3");
    private static final ServiceName secondServiceAlias4 = ServiceName.of("second", "alias", "4");
    private static final ServiceName secondServiceAlias5 = ServiceName.of("second", "alias", "5");
    private static final ServiceName firstServiceAlias = ServiceName.of("first", "alias");
    private TestLifecycleListener testListener;

    @Before
    public void setTestListener() {
        testListener = new TestLifecycleListener();
    }

    @Test
    public void duplicateDependency() throws Exception {
        ServiceBuilder<?> sb = serviceContainer.addService();
        Consumer<String> providedValue = sb.provides(secondServiceName, secondServiceAlias1, secondServiceAlias2);
        sb.setInstance(Service.newInstance(providedValue, secondServiceName.toString()));
        sb.addListener(testListener);
        final ServiceController<?> secondController = sb.install();

        sb = serviceContainer.addService();
        providedValue = sb.provides(firstServiceName);
        sb.setInstance(Service.newInstance(providedValue, firstServiceName.toString()));
        sb.requires(secondServiceName);
        sb.requires(secondServiceAlias2);
        sb.addListener(testListener);
        final ServiceController<?> firstController = sb.install();

        serviceContainer.awaitStability();

        assertSame(firstController, serviceContainer.getRequiredService(firstServiceName));
        assertSame(secondController, serviceContainer.getRequiredService(secondServiceName));

        assertTrue(testListener.upValues().contains(firstServiceName));
        assertTrue(testListener.upValues().contains(secondServiceName));
        assertTrue(testListener.upValues().contains(secondServiceAlias1));
        assertTrue(testListener.upValues().contains(secondServiceAlias2));
    }

    @Test
    public void duplicateDependencyWithMissingDependency() throws Exception {
        ServiceBuilder<?> sb = serviceContainer.addService();
        Consumer<String> providedValue = sb.provides(secondServiceName, secondServiceAlias1, secondServiceAlias2);
        sb.setInstance(Service.newInstance(providedValue, secondServiceName.toString()));
        sb.addListener(testListener);
        final ServiceController<?> secondController = sb.install();

        sb = serviceContainer.addService();
        providedValue = sb.provides(firstServiceName, firstServiceAlias);
        sb.setInstance(Service.newInstance(providedValue, firstServiceName.toString()));
        sb.requires(secondServiceName);
        sb.requires(secondServiceAlias2);
        sb.requires(thirdServiceName);
        sb.addListener(testListener);
        final ServiceController<?> firstController = sb.install();

        serviceContainer.awaitStability();

        assertSame(firstController, serviceContainer.getRequiredService(firstServiceName));
        assertSame(secondController, serviceContainer.getRequiredService(secondServiceName));
        assertTrue(testListener.upValues().contains(secondServiceName));
        assertTrue(testListener.upValues().contains(secondServiceAlias1));
        assertTrue(testListener.upValues().contains(secondServiceAlias2));
        assertTrue(testListener.downValues().contains(firstServiceName));
        assertTrue(testListener.downValues().contains(firstServiceAlias));
    }

}
