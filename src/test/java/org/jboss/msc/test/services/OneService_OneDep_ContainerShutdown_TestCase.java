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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.jboss.msc.service.ServiceName;
import org.jboss.msc.test.utils.AbstractServiceTest;
import org.jboss.msc.test.utils.TestService;
import org.junit.Test;

/**
 * @author <a href="mailto:frainone@redhat.com">Flavia Rainone</a>
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
public class OneService_OneDep_ContainerShutdown_TestCase extends AbstractServiceTest {

    private static final ServiceName firstSN = ServiceName.of("first");
    private static final ServiceName secondSN = ServiceName.of("second");

    /**
     * Usecase:
     * <UL>
     *   <LI><B>first service</B> (ON_DEMAND mode), no dependencies</LI>
     *   <LI><B>second service</B> (ON_DEMAND mode), depends on <B>first service</B></LI>
     *   <LI>container shutdown</LI>
     * </UL>
     */
    @Test
    public void usecase1() throws Exception {
        final TestService firstService = addService(firstSN, ON_DEMAND);
        assertFalse(firstService.isUp());
        final TestService secondService = addService(secondSN, ON_DEMAND, firstSN);
        assertFalse(firstService.isUp());
        assertFalse(secondService.isUp());
        shutdownContainer();
        assertFalse(firstService.isUp());
        assertFalse(secondService.isUp());
    }

    /**
     * Usecase:
     * <UL>
     *   <LI><B>first service</B> (LAZY mode), no dependencies</LI>
     *   <LI><B>second service</B> (ON_DEMAND mode), depends on <B>first service</B></LI>
     *   <LI>container shutdown</LI>
     * </UL>
     */
    @Test
    public void usecase2() throws Exception {
        final TestService firstService = addService(firstSN, LAZY);
        assertFalse(firstService.isUp());
        final TestService secondService = addService(secondSN, ON_DEMAND, firstSN);
        assertFalse(firstService.isUp());
        assertFalse(secondService.isUp());
        shutdownContainer();
        assertFalse(firstService.isUp());
        assertFalse(secondService.isUp());
    }

    /**
     * Usecase:
     * <UL>
     *   <LI><B>first service</B> (ACTIVE mode), no dependencies</LI>
     *   <LI><B>second service</B> (ON_DEMAND mode), depends on <B>first service</B></LI>
     *   <LI>container shutdown</LI>
     * </UL>
     */
    @Test
    public void usecase3() throws Exception {
        final TestService firstService = addService(firstSN, ACTIVE);
        assertTrue(firstService.isUp());
        final TestService secondService = addService(secondSN, ON_DEMAND, firstSN);
        assertTrue(firstService.isUp());
        assertFalse(secondService.isUp());
        shutdownContainer();
        assertFalse(firstService.isUp());
        assertFalse(secondService.isUp());
    }

    /**
     * Usecase:
     * <UL>
     *   <LI><B>first service</B> (ON_DEMAND mode), no dependencies</LI>
     *   <LI><B>second service</B> (LAZY mode), depends on <B>first service</B></LI>
     *   <LI>container shutdown</LI>
     * </UL>
     */
    @Test
    public void usecase4() throws Exception {
        final TestService firstService = addService(firstSN, ON_DEMAND);
        assertFalse(firstService.isUp());
        final TestService secondService = addService(secondSN, LAZY, firstSN);
        assertFalse(firstService.isUp());
        assertFalse(secondService.isUp());
        shutdownContainer();
        assertFalse(firstService.isUp());
        assertFalse(secondService.isUp());
    }

    /**
     * Usecase:
     * <UL>
     *   <LI><B>first service</B> (LAZY mode), no dependencies</LI>
     *   <LI><B>second service</B> (LAZY mode), depends on <B>first service</B></LI>
     *   <LI>container shutdown</LI>
     * </UL>
     */
    @Test
    public void usecase5() throws Exception {
        final TestService firstService = addService(firstSN, LAZY);
        assertFalse(firstService.isUp());
        final TestService secondService = addService(secondSN, LAZY, firstSN);
        assertFalse(firstService.isUp());
        assertFalse(secondService.isUp());
        shutdownContainer();
        assertFalse(firstService.isUp());
        assertFalse(secondService.isUp());
    }

    /**
     * Usecase:
     * <UL>
     *   <LI><B>first service</B> (ACTIVE mode), no dependencies</LI>
     *   <LI><B>second service</B> (LAZY mode), depends on <B>first service</B></LI>
     *   <LI>container shutdown</LI>
     * </UL>
     */
    @Test
    public void usecase6() throws Exception {
        final TestService firstService = addService(firstSN, ACTIVE);
        assertTrue(firstService.isUp());
        final TestService secondService = addService(secondSN, LAZY, firstSN);
        assertTrue(firstService.isUp());
        assertFalse(secondService.isUp());
        shutdownContainer();
        assertFalse(firstService.isUp());
        assertFalse(secondService.isUp());
    }

    /**
     * Usecase:
     * <UL>
     *   <LI><B>first service</B> (ON_DEMAND mode), no dependencies</LI>
     *   <LI><B>second service</B> (ACTIVE mode), depends on <B>first service</B></LI>
     *   <LI>container shutdown</LI>
     * </UL>
     */
    @Test
    public void usecase7() throws Exception {
        final TestService firstService = addService(firstSN, ON_DEMAND);
        final TestService secondService = addService(secondSN, ACTIVE, firstSN);
        assertTrue(firstService.isUp());
        assertTrue(secondService.isUp());
        shutdownContainer();
        assertFalse(firstService.isUp());
        assertFalse(secondService.isUp());
    }

    /**
     * Usecase:
     * <UL>
     *   <LI><B>first service</B> (LAZY mode), no dependencies</LI>
     *   <LI><B>second service</B> (ACTIVE mode), depends on <B>first service</B></LI>
     *   <LI>container shutdown</LI>
     * </UL>
     */
    @Test
    public void usecase8() throws Exception {
        final TestService firstService = addService(firstSN, LAZY);
        assertFalse(firstService.isUp());
        final TestService secondService = addService(secondSN, ACTIVE, firstSN);
        assertTrue(firstService.isUp());
        assertTrue(secondService.isUp());
        shutdownContainer();
        assertFalse(firstService.isUp());
        assertFalse(secondService.isUp());
    }

    /**
     * Usecase:
     * <UL>
     *   <LI><B>first service</B> (ACTIVE mode), no dependencies</LI>
     *   <LI><B>second service</B> (ACTIVE mode), depends on <B>first service</B></LI>
     *   <LI>container shutdown</LI>
     * </UL>
     */
    @Test
    public void usecase9() throws Exception {
        final TestService firstService = addService(firstSN, ACTIVE);
        assertTrue(firstService.isUp());
        final TestService secondService = addService(secondSN, ACTIVE, firstSN);
        assertTrue(firstService.isUp());
        assertNotNull(secondService);
        assertTrue(secondService.isUp());
        shutdownContainer();
        assertFalse(firstService.isUp());
        assertFalse(secondService.isUp());
    }
}