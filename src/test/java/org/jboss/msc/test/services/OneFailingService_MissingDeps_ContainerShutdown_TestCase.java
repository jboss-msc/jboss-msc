/*
 * JBoss, Home of Professional Open Source
 *
 * Copyright 2013 Red Hat, Inc. and/or its affiliates.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.msc.test.services;

import static org.jboss.msc.service.ServiceMode.ACTIVE;
import static org.jboss.msc.service.ServiceMode.LAZY;
import static org.jboss.msc.service.ServiceMode.ON_DEMAND;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;

import org.jboss.msc.service.ServiceName;
import org.jboss.msc.test.utils.AbstractServiceTest;
import org.jboss.msc.test.utils.TestService;
import org.junit.Test;

/**
 * @author <a href="mailto:frainone@redhat.com">Flavia Rainone</a>
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
public class OneFailingService_MissingDeps_ContainerShutdown_TestCase extends AbstractServiceTest {

    private static final ServiceName firstSN = ServiceName.of("first");
    private static final ServiceName secondSN = ServiceName.of("second");

    /**
     * Usecase:
     * <UL>
     *   <LI>first failing service (ON_DEMAND mode), missing required dependency</LI>
     * </UL>
     */
    @Test
    public void usecase1() throws Exception {
        assertNull(addService(firstSN, true, ON_DEMAND, secondSN));
    }

    /**
     * Usecase:
     * <UL>
     *   <LI>first failing service (LAZY mode), missing required dependency</LI>
     * </UL>
     */
    @Test
    public void usecase2() throws Exception {
        assertNull(addService(firstSN, true, LAZY, requiredFlag, secondSN));
    }

    /**
     * Usecase:
     * <UL>
     *   <LI>first failing service (ACTIVE mode), missing required dependency</LI>
     * </UL>
     */
    @Test
    public void usecase3() throws Exception {
        assertNull(addService(firstSN, true, ACTIVE, secondSN));
    }

    /**
     * Usecase:
     * <UL>
     *   <LI>first failing service (ON_DEMAND mode), missing unrequired dependency</LI>
     *   <LI>container shutdown</LI>
     * </UL>
     */
    @Test
    public void usecase4() throws Exception {
        final TestService firstService = addService(firstSN, true, ON_DEMAND, unrequiredFlag, secondSN);
        assertFalse(firstService.isUp());
        shutdownContainer();
        assertFalse(firstService.isUp());
    }

    /**
     * Usecase:
     * <UL>
     *   <LI>first failing service (LAZY mode), missing unrequired dependency</LI>
     *   <LI>container shutdown</LI>
     * </UL>
     */
    @Test
    public void usecase5() throws Exception {
        final TestService firstService = addService(firstSN, true, LAZY, unrequiredFlag, secondSN);
        assertFalse(firstService.isUp());
        shutdownContainer();
        assertFalse(firstService.isUp());
    }

    /**
     * Usecase:
     * <UL>
     *   <LI>first failing service (ACTIVE mode), missing unrequired dependency</LI>
     *   <LI>container shutdown</LI>
     * </UL>
     */
    @Test
    public void usecase6() throws Exception {
        final TestService firstService = addService(firstSN, true, ACTIVE, unrequiredFlag, secondSN);
        assertFalse(firstService.isUp());
        shutdownContainer();
        assertFalse(firstService.isUp());
    }

}
