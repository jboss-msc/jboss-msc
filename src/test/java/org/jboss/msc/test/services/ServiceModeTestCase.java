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

package org.jboss.msc.test.services;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.jboss.msc.service.ServiceMode;
import org.jboss.msc.service.ServiceName;
import org.junit.Ignore;
import org.junit.Test;

/**
 * Test behavior of service modes.
 * 
 * @author <a href="mailto:flavia.rainone@jboss.com">Flavia Rainone</a>
 */
//@Ignore
public class ServiceModeTestCase extends AbstractServiceTest {

    protected static final ServiceName firstServiceName = ServiceName.of("firstService");
    protected static final ServiceName secondServiceName = ServiceName.of("secondService");

    /**
     * Installs a service named {@code firstServiceName}, with service mode {@link ServiceMode#NEVER NEVER}
     */
    @Test
    public void neverModeService() throws Exception {
        final TestService service = installService(firstServiceName, ServiceMode.NEVER);
        assertFalse(service.isUp());
    }

    /**
     * Installs a service named {@code firstServiceName}, with service mode {@link ServiceMode#NEVER NEVER}, demanded
     * by second service.
     */
    @Test
    public void demandedNeverService() throws Exception {
        final TestService firstService =  installService(firstServiceName, ServiceMode.NEVER);
        final TestService secondService = installService(secondServiceName, firstServiceName);
        assertFalse(firstService.isUp());
        assertFalse(secondService.isUp());
    }

    /**
     * Installs a service named {@code secondServiceName} with service mode {@link ServiceMode#ON_DEMAND ON_DEMAND}.
     * The service is in the {@code UP} state, as there is an active service named {@code firstServiceName} with a
     * dependency on second service.
     */
    @Test
    public void upOnDemandSecondService() throws Exception{
        final TestService secondService = installService(secondServiceName, ServiceMode.ON_DEMAND);
        final TestService firstService = installService(firstServiceName, secondServiceName);
        assertTrue(firstService.isUp());
        assertTrue(secondService.isUp());
    }

    /**
     * Installs a service named {@code firstServiceName} with service mode  {@link ServiceMode#ON_DEMAND ON_DEMAND}.
     * The service is in the {@code DOWN} state, as there are no dependent services.
     */
    @Test
    public void downOnDemandFirstService() throws Exception {
        final TestService firstService = installService(firstServiceName, ServiceMode.ON_DEMAND);
        assertFalse(firstService.isUp());
    }

    /**
     * Installs a service named {@code secondServiceName} with service mode {@link ServiceMode#ON_DEMAND ON_DEMAND}.
     * The controller is in the {@code START_FAILED} state, as the start attempt, triggered by a dependent named
     * {@code firstServiceName}, failed to occur.
     * 
     */
    @Ignore @Test
    public void failedToStartOnDemandSecondService() throws Exception {
        final TestService secondService = installService(secondServiceName, true, ServiceMode.ON_DEMAND);
        final TestService firstService = installService(firstServiceName,true, secondServiceName);
        assertTrue(secondService.isFailed());
        assertFalse(firstService.isUp());
    }

    /**
     * Installs a service named {@code secondServiceName} with service mode {@link ServiceMode#LAZY LAZY}. The
     * service is in the {@code UP} state, as there is an active service named {@code firstServiceName} with a
     * dependency on second service.
     */
    @Test
    public void upLazySecondService() throws Exception {
        final TestService secondService = installService(secondServiceName, ServiceMode.LAZY);
        assertFalse(secondService.isUp());
        final TestService firstService = installService(firstServiceName, secondServiceName);
        assertTrue(firstService.isUp());
        assertTrue(secondService.isUp());
    }

    /**
     * Installs a service named {@code secondServiceName} with service mode {@link ServiceMode#LAZY LAZY}. The service
     * is in the {@code UP} state, as there has been an active service named {@code firstServiceName} with a dependency
     * on second service . First service is currently down in {@link ServiceMode#NEVER NEVER} mode.
     */
    @Test @Ignore
    public void upLazySecondServiceWithNoActiveDependents() throws Exception {
        upLazySecondService();
        remove(firstServiceName);
        final TestService secondService = (TestService) serviceRegistry.getRequiredService(secondServiceName);
        assertTrue(secondService.isUp());
        final TestService firstService = installService(firstServiceName, ServiceMode.NEVER, secondServiceName);
        assertFalse(firstService.isUp());
        assertTrue(secondService.isUp());
    }

    /**
     * Installs a service named {@code firstServiceName} with service mode {@link ServiceMode#LAZY LAZY}. The service
     * is in the {@code DOWN} state, as there is no dependent services.
     * 
     */
    @Test
    public void downLazyFirstService() throws Exception {
        final TestService firstService = installService(firstServiceName, ServiceMode.LAZY);
        assertFalse(firstService.isUp());
    }

    /**
     * Installs a service named {@code secondServiceName} with initial mode {@link ServiceMode#LAZY LAZY}. The service
     * is in the {@code START_FAILED} state, and it has a dependent named {@code firstServiceName}, waiting for the
     * failure to be cleared so it can start.
     */
    @Ignore @Test
    public void failedToStartLazySecondService() throws Exception {
        final TestService secondService = installService(secondServiceName, true, ServiceMode.LAZY);
        final TestService firstService = installService(firstServiceName, true, secondServiceName);
        assertTrue(secondService.isFailed());
        assertFalse(firstService.isUp());
    }

    /**
     * Installs a service named {@code firstServiceName} with service mode {@link ServiceMode#PASSIVE PASSIVE},
     * in the {@code UP} state.
     */
    @Test
    public void upPassiveFirstService() throws Exception{
        final TestService firstService = installService(firstServiceName, ServiceMode.PASSIVE);
        assertTrue(firstService.isUp());
    }

    /**
     * Installs a service named {@code firstServiceName} with service mode {@link ServiceMode#PASSIVE PASSIVE}. The
     * service is in the {@code DOWN} state, as it has a missing dependency on a service named {@code secondServiceName}.
     */
    @Test
    public void downPassiveFirstService() throws Exception {
        final TestService firstService = installService(firstServiceName, ServiceMode.PASSIVE, secondServiceName);
        assertFalse(firstService.isUp());
    }

    /**
     * Installs a service named {@code secondServiceName} with initial mode {@link ServiceMode#PASSIVE PASSIVE}. The
     * service is in the {@code START_FAILED} state, and it has a dependent named {@code firstServiceName}, waiting for
     * the failure to be cleared so it can start.
     */
    @Ignore @Test
    public void failedToStartPassiveSecondService() throws Exception {
        final TestService secondService = installService(secondServiceName, true, ServiceMode.PASSIVE);
        final TestService firstService = installService(firstServiceName, true, secondServiceName);
        assertTrue(secondService.isFailed());
        assertFalse(firstService.isUp());
    }

    /**
     * Installs a service named {@code firstServiceName} with initial mode {@link ServiceMode#ACTIVE ACTIVE}, in the
     * {@code UP} state.
     */
    @Test
    public void upActiveFirstService() throws Exception{
        final TestService firstService = installService(firstServiceName, ServiceMode.ACTIVE);
        assertTrue(firstService.isUp());
    }

    /**
     * Installs a service named {@code firstServiceName} with initial mode {@link ServiceMode#ACTIVE ACTIVE}. The
     * service is in the {@code DOWN} state, as it has a dependency on a service named {@code secondServiceName} in the
     * {@code START_FAILED} state.
     */
    @Ignore @Test
    public void downActiveFirstService() throws Exception {
        final TestService secondService = installService(secondServiceName, true);
        assertTrue(secondService.isFailed());
        final TestService firstService = installService(firstServiceName, ServiceMode.ACTIVE, secondServiceName);
        assertFalse(firstService.isUp());
    }

    /**
     * Installs a service named {@code secondServiceName} with initial mode {@link ServiceMode#ACTIVE ACTIVE}. The
     * service is in the {@code START_FAILED} state, and has a dependent named {@code firstServiceName} waiting for the
     * failure to be cleared so it can start.
     */
    @Ignore @Test
    public void failedToStartActiveSecondService() throws Exception {
        final TestService secondService = installService(secondServiceName, true, ServiceMode.ACTIVE);
        final TestService firstService = installService(firstServiceName, true, secondServiceName);
        assertTrue(secondService.isFailed());
        assertFalse(firstService.isUp());
    }
}