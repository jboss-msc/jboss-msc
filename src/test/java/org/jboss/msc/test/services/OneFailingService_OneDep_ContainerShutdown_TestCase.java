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

import static org.jboss.msc.service.ServiceMode.ACTIVE;
import static org.jboss.msc.service.ServiceMode.LAZY;
import static org.jboss.msc.service.ServiceMode.NEVER;
import static org.jboss.msc.service.ServiceMode.ON_DEMAND;
import static org.jboss.msc.service.ServiceMode.PASSIVE;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.jboss.msc.service.ServiceName;
import org.jboss.msc.test.utils.AbstractServiceTest;
import org.jboss.msc.test.utils.TestService;
import org.junit.Test;

/**
 * @author <a href="mailto:frainone@redhat.com">Flavia Rainone</a>
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
public class OneFailingService_OneDep_ContainerShutdown_TestCase extends AbstractServiceTest {

    private static final ServiceName firstSN = ServiceName.of("first");
    private static final ServiceName secondSN = ServiceName.of("second");

    /**
     * Usecase:
     * <UL>
     *   <LI><B>first failing service</B> (NEVER mode), no dependencies</LI>
     *   <LI><B>second service</B> (NEVER mode), depends on <B>first service</B></LI>
     *   <LI>container shutdown</LI>
     * </UL>
     */
    @Test
    public void usecase1() throws Exception {
        final TestService firstService = addService(firstSN, true, NEVER);
        assertFalse(firstService.isUp());
        assertFalse(firstService.isFailed());
        final TestService secondService = addService(secondSN, NEVER, firstSN);
        assertFalse(firstService.isUp());
        assertFalse(firstService.isFailed());
        assertFalse(secondService.isUp());
        assertFalse(secondService.isFailed());
        shutdownContainer();
        assertFalse(firstService.isUp());
        assertFalse(firstService.isFailed());
        assertFalse(secondService.isUp());
        assertFalse(secondService.isFailed());
    }

    /**
     * Usecase:
     * <UL>
     *   <LI><B>first failing service</B> (ON_DEMAND mode), no dependencies</LI>
     *   <LI><B>second service</B> (NEVER mode), depends on <B>first service</B></LI>
     *   <LI>container shutdown</LI>
     * </UL>
     */
    @Test
    public void usecase2() throws Exception {
        final TestService firstService = addService(firstSN, true, ON_DEMAND);
        assertFalse(firstService.isUp());
        assertFalse(firstService.isFailed());
        final TestService secondService = addService(secondSN, NEVER, firstSN);
        assertFalse(firstService.isUp());
        assertFalse(firstService.isFailed());
        assertFalse(secondService.isUp());
        assertFalse(secondService.isFailed());
        shutdownContainer();
        assertFalse(firstService.isUp());
        assertFalse(firstService.isFailed());
        assertFalse(secondService.isUp());
        assertFalse(secondService.isFailed());
    }

    /**
     * Usecase:
     * <UL>
     *   <LI><B>first failing service</B> (LAZY mode), no dependencies</LI>
     *   <LI><B>second service</B> (NEVER mode), depends on <B>first service</B></LI>
     *   <LI>container shutdown</LI>
     * </UL>
     */
    @Test
    public void usecase3() throws Exception {
        final TestService firstService = addService(firstSN, true, LAZY);
        assertFalse(firstService.isUp());
        assertFalse(firstService.isFailed());
        final TestService secondService = addService(secondSN, NEVER, firstSN);
        assertFalse(firstService.isUp());
        assertFalse(firstService.isFailed());
        assertFalse(secondService.isUp());
        assertFalse(secondService.isFailed());
        shutdownContainer();
        assertFalse(firstService.isUp());
        assertFalse(firstService.isFailed());
        assertFalse(secondService.isUp());
        assertFalse(secondService.isFailed());
    }

    /**
     * Usecase:
     * <UL>
     *   <LI><B>first failing service</B> (PASSIVE mode), no dependencies</LI>
     *   <LI><B>second service</B> (NEVER mode), depends on <B>first service</B></LI>
     *   <LI>container shutdown</LI>
     * </UL>
     */
    @Test
    public void usecase4() throws Exception {
        final TestService firstService = addService(firstSN, true, PASSIVE);
        assertFalse(firstService.isUp());
        assertTrue(firstService.isFailed());
        final TestService secondService = addService(secondSN, NEVER, firstSN);
        assertFalse(firstService.isUp());
        assertTrue(firstService.isFailed());
        assertFalse(secondService.isUp());
        assertFalse(secondService.isFailed());
        shutdownContainer();
        assertFalse(firstService.isUp());
        assertTrue(firstService.isFailed());
        assertFalse(secondService.isUp());
        assertFalse(secondService.isFailed());
    }

    /**
     * Usecase:
     * <UL>
     *   <LI><B>first failing service</B> (ACTIVE mode), no dependencies</LI>
     *   <LI><B>second service</B> (NEVER mode), depends on <B>first service</B></LI>
     *   <LI>container shutdown</LI>
     * </UL>
     */
    @Test
    public void usecase5() throws Exception {
        final TestService firstService = addService(firstSN, true, ACTIVE);
        assertFalse(firstService.isUp());
        assertTrue(firstService.isFailed());
        final TestService secondService = addService(secondSN, NEVER, firstSN);
        assertFalse(firstService.isUp());
        assertTrue(firstService.isFailed());
        assertFalse(secondService.isUp());
        assertFalse(secondService.isFailed());
        shutdownContainer();
        assertFalse(firstService.isUp());
        assertTrue(firstService.isFailed());
        assertFalse(secondService.isUp());
        assertFalse(secondService.isFailed());
    }

    /**
     * Usecase:
     * <UL>
     *   <LI><B>first failing service</B> (NEVER mode), no dependencies</LI>
     *   <LI><B>second service</B> (ON_DEMAND mode), depends on <B>first service</B></LI>
     *   <LI>container shutdown</LI>
     * </UL>
     */
    @Test
    public void usecase6() throws Exception {
        final TestService firstService = addService(firstSN, true, NEVER);
        assertFalse(firstService.isUp());
        assertFalse(firstService.isFailed());
        final TestService secondService = addService(secondSN, ON_DEMAND, firstSN);
        assertFalse(firstService.isUp());
        assertFalse(firstService.isFailed());
        assertFalse(secondService.isUp());
        assertFalse(secondService.isFailed());
        shutdownContainer();
        assertFalse(firstService.isUp());
        assertFalse(firstService.isFailed());
        assertFalse(secondService.isUp());
        assertFalse(secondService.isFailed());
    }

    /**
     * Usecase:
     * <UL>
     *   <LI><B>first failing service</B> (ON_DEMAND mode), no dependencies</LI>
     *   <LI><B>second service</B> (ON_DEMAND mode), depends on <B>first service</B></LI>
     *   <LI>container shutdown</LI>
     * </UL>
     */
    @Test
    public void usecase7() throws Exception {
        final TestService firstService = addService(firstSN, true, ON_DEMAND);
        assertFalse(firstService.isUp());
        assertFalse(firstService.isFailed());
        final TestService secondService = addService(secondSN, ON_DEMAND, firstSN);
        assertFalse(firstService.isUp());
        assertFalse(firstService.isFailed());
        assertFalse(secondService.isUp());
        assertFalse(secondService.isFailed());
        shutdownContainer();
        assertFalse(firstService.isUp());
        assertFalse(firstService.isFailed());
        assertFalse(secondService.isUp());
        assertFalse(secondService.isFailed());
    }

    /**
     * Usecase:
     * <UL>
     *   <LI><B>first failing service</B> (LAZY mode), no dependencies</LI>
     *   <LI><B>second service</B> (ON_DEMAND mode), depends on <B>first service</B></LI>
     *   <LI>container shutdown</LI>
     * </UL>
     */
    @Test
    public void usecase8() throws Exception {
        final TestService firstService = addService(firstSN, true, LAZY);
        assertFalse(firstService.isUp());
        assertFalse(firstService.isFailed());
        final TestService secondService = addService(secondSN, ON_DEMAND, firstSN);
        assertFalse(firstService.isUp());
        assertFalse(firstService.isFailed());
        assertFalse(secondService.isUp());
        assertFalse(secondService.isFailed());
        shutdownContainer();
        assertFalse(firstService.isUp());
        assertFalse(firstService.isFailed());
        assertFalse(secondService.isUp());
        assertFalse(secondService.isFailed());
    }

    /**
     * Usecase:
     * <UL>
     *   <LI><B>first failing service</B> (PASSIVE mode), no dependencies</LI>
     *   <LI><B>second service</B> (ON_DEMAND mode), depends on <B>first service</B></LI>
     *   <LI>container shutdown</LI>
     * </UL>
     */
    @Test
    public void usecase9() throws Exception {
        final TestService firstService = addService(firstSN, true, PASSIVE);
        assertFalse(firstService.isUp());
        assertTrue(firstService.isFailed());
        final TestService secondService = addService(secondSN, ON_DEMAND, firstSN);
        assertFalse(firstService.isUp());
        assertTrue(firstService.isFailed());
        assertFalse(secondService.isUp());
        assertFalse(secondService.isFailed());
        shutdownContainer();
        assertFalse(firstService.isUp());
        assertTrue(firstService.isFailed());
        assertFalse(secondService.isUp());
        assertFalse(secondService.isFailed());
    }

    /**
     * Usecase:
     * <UL>
     *   <LI><B>first failing service</B> (ACTIVE mode), no dependencies</LI>
     *   <LI><B>second service</B> (ON_DEMAND mode), depends on <B>first service</B></LI>
     *   <LI>container shutdown</LI>
     * </UL>
     */
    @Test
    public void usecase10() throws Exception {
        final TestService firstService = addService(firstSN, true, ACTIVE);
        assertFalse(firstService.isUp());
        assertTrue(firstService.isFailed());
        final TestService secondService = addService(secondSN, ON_DEMAND, firstSN);
        assertFalse(firstService.isUp());
        assertTrue(firstService.isFailed());
        assertFalse(secondService.isUp());
        assertFalse(secondService.isFailed());
        shutdownContainer();
        assertFalse(firstService.isUp());
        assertTrue(firstService.isFailed());
        assertFalse(secondService.isUp());
        assertFalse(secondService.isFailed());
    }

    /**
     * Usecase:
     * <UL>
     *   <LI><B>first failing service</B> (NEVER mode), no dependencies</LI>
     *   <LI><B>second service</B> (LAZY mode), depends on <B>first service</B></LI>
     *   <LI>container shutdown</LI>
     * </UL>
     */
    @Test
    public void usecase11() throws Exception {
        final TestService firstService = addService(firstSN, true, NEVER);
        assertFalse(firstService.isUp());
        assertFalse(firstService.isFailed());
        final TestService secondService = addService(secondSN, LAZY, firstSN);
        assertFalse(firstService.isUp());
        assertFalse(firstService.isFailed());
        assertFalse(secondService.isUp());
        assertFalse(secondService.isFailed());
        shutdownContainer();
        assertFalse(firstService.isUp());
        assertFalse(firstService.isFailed());
        assertFalse(secondService.isUp());
        assertFalse(secondService.isFailed());
    }

    /**
     * Usecase:
     * <UL>
     *   <LI><B>first failing service</B> (ON_DEMAND mode), no dependencies</LI>
     *   <LI><B>second service</B> (LAZY mode), depends on <B>first service</B></LI>
     *   <LI>container shutdown</LI>
     * </UL>
     */
    @Test
    public void usecase12() throws Exception {
        final TestService firstService = addService(firstSN, true, ON_DEMAND);
        assertFalse(firstService.isUp());
        assertFalse(firstService.isFailed());
        final TestService secondService = addService(secondSN, LAZY, firstSN);
        assertFalse(firstService.isUp());
        assertFalse(firstService.isFailed());
        assertFalse(secondService.isUp());
        assertFalse(secondService.isFailed());
        shutdownContainer();
        assertFalse(firstService.isUp());
        assertFalse(firstService.isFailed());
        assertFalse(secondService.isUp());
        assertFalse(secondService.isFailed());
    }

    /**
     * Usecase:
     * <UL>
     *   <LI><B>first failing service</B> (LAZY mode), no dependencies</LI>
     *   <LI><B>second service</B> (LAZY mode), depends on <B>first service</B></LI>
     *   <LI>container shutdown</LI>
     * </UL>
     */
    @Test
    public void usecase13() throws Exception {
        final TestService firstService = addService(firstSN, true, LAZY);
        assertFalse(firstService.isUp());
        assertFalse(firstService.isFailed());
        final TestService secondService = addService(secondSN, LAZY, firstSN);
        assertFalse(firstService.isUp());
        assertFalse(firstService.isFailed());
        assertFalse(secondService.isUp());
        assertFalse(secondService.isFailed());
        shutdownContainer();
        assertFalse(firstService.isUp());
        assertFalse(firstService.isFailed());
        assertFalse(secondService.isUp());
        assertFalse(secondService.isFailed());
    }

    /**
     * Usecase:
     * <UL>
     *   <LI><B>first failing service</B> (PASSIVE mode), no dependencies</LI>
     *   <LI><B>second service</B> (LAZY mode), depends on <B>first service</B></LI>
     *   <LI>container shutdown</LI>
     * </UL>
     */
    @Test
    public void usecase14() throws Exception {
        final TestService firstService = addService(firstSN, true, PASSIVE);
        assertFalse(firstService.isUp());
        assertTrue(firstService.isFailed());
        final TestService secondService = addService(secondSN, LAZY, firstSN);
        assertFalse(firstService.isUp());
        assertTrue(firstService.isFailed());
        assertFalse(secondService.isUp());
        assertFalse(secondService.isFailed());
        shutdownContainer();
        assertFalse(firstService.isUp());
        assertTrue(firstService.isFailed());
        assertFalse(secondService.isUp());
        assertFalse(secondService.isFailed());
    }

    /**
     * Usecase:
     * <UL>
     *   <LI><B>first failing service</B> (ACTIVE mode), no dependencies</LI>
     *   <LI><B>second service</B> (LAZY mode), depends on <B>first service</B></LI>
     *   <LI>container shutdown</LI>
     * </UL>
     */
    @Test
    public void usecase15() throws Exception {
        final TestService firstService = addService(firstSN, true, ACTIVE);
        assertFalse(firstService.isUp());
        assertTrue(firstService.isFailed());
        final TestService secondService = addService(secondSN, LAZY, firstSN);
        assertFalse(firstService.isUp());
        assertTrue(firstService.isFailed());
        assertFalse(secondService.isUp());
        assertFalse(secondService.isFailed());
        shutdownContainer();
        assertFalse(firstService.isUp());
        assertTrue(firstService.isFailed());
        assertFalse(secondService.isUp());
        assertFalse(secondService.isFailed());
    }

    /**
     * Usecase:
     * <UL>
     *   <LI><B>first failing service</B> (NEVER mode), no dependencies</LI>
     *   <LI><B>second service</B> (PASSIVE mode), depends on <B>first service</B></LI>
     *   <LI>container shutdown</LI>
     * </UL>
     */
    @Test
    public void usecase16() throws Exception {
        final TestService firstService = addService(firstSN, true, NEVER);
        assertFalse(firstService.isUp());
        assertFalse(firstService.isFailed());
        final TestService secondService = addService(secondSN, PASSIVE, firstSN);
        assertFalse(firstService.isUp());
        assertFalse(firstService.isFailed());
        assertFalse(secondService.isUp());
        assertFalse(secondService.isFailed());
        shutdownContainer();
        assertFalse(firstService.isUp());
        assertFalse(firstService.isFailed());
        assertFalse(secondService.isUp());
        assertFalse(secondService.isFailed());
    }

    /**
     * Usecase:
     * <UL>
     *   <LI><B>first failing service</B> (ON_DEMAND mode), no dependencies</LI>
     *   <LI><B>second service</B> (PASSIVE mode), depends on <B>first service</B></LI>
     *   <LI>container shutdown</LI>
     * </UL>
     */
    @Test
    public void usecase17() throws Exception {
        final TestService firstService = addService(firstSN, true, ON_DEMAND);
        assertFalse(firstService.isUp());
        assertFalse(firstService.isFailed());
        final TestService secondService = addService(secondSN, PASSIVE, firstSN);
        assertFalse(firstService.isUp());
        assertFalse(firstService.isFailed());
        assertFalse(secondService.isUp());
        assertFalse(secondService.isFailed());
        shutdownContainer();
        assertFalse(firstService.isUp());
        assertFalse(firstService.isFailed());
        assertFalse(secondService.isUp());
        assertFalse(secondService.isFailed());
    }

    /**
     * Usecase:
     * <UL>
     *   <LI><B>first failing service</B> (LAZY mode), no dependencies</LI>
     *   <LI><B>second service</B> (PASSIVE mode), depends on <B>first service</B></LI>
     *   <LI>container shutdown</LI>
     * </UL>
     */
    @Test
    public void usecase18() throws Exception {
        final TestService firstService = addService(firstSN, true, LAZY);
        assertFalse(firstService.isUp());
        assertFalse(firstService.isFailed());
        final TestService secondService = addService(secondSN, PASSIVE, firstSN);
        assertFalse(firstService.isUp());
        assertFalse(firstService.isFailed());
        assertFalse(secondService.isUp());
        assertFalse(secondService.isFailed());
        shutdownContainer();
        assertFalse(firstService.isUp());
        assertFalse(firstService.isFailed());
        assertFalse(secondService.isUp());
        assertFalse(secondService.isFailed());
    }

    /**
     * Usecase:
     * <UL>
     *   <LI><B>first failing service</B> (PASSIVE mode), no dependencies</LI>
     *   <LI><B>second service</B> (PASSIVE mode), depends on <B>first service</B></LI>
     *   <LI>container shutdown</LI>
     * </UL>
     */
    @Test
    public void usecase19() throws Exception {
        final TestService firstService = addService(firstSN, true, PASSIVE);
        assertFalse(firstService.isUp());
        assertTrue(firstService.isFailed());
        final TestService secondService = addService(secondSN, PASSIVE, firstSN);
        assertFalse(firstService.isUp());
        assertTrue(firstService.isFailed());
        assertFalse(secondService.isUp());
        assertFalse(secondService.isFailed());
        shutdownContainer();
        assertFalse(firstService.isUp());
        assertTrue(firstService.isFailed());
        assertFalse(secondService.isUp());
        assertFalse(secondService.isFailed());
    }

    /**
     * Usecase:
     * <UL>
     *   <LI><B>first failing service</B> (ACTIVE mode), no dependencies</LI>
     *   <LI><B>second service</B> (PASSIVE mode), depends on <B>first service</B></LI>
     *   <LI>container shutdown</LI>
     * </UL>
     */
    @Test
    public void usecase20() throws Exception {
        final TestService firstService = addService(firstSN, true, ACTIVE);
        assertFalse(firstService.isUp());
        assertTrue(firstService.isFailed());
        final TestService secondService = addService(secondSN, PASSIVE, firstSN);
        assertFalse(firstService.isUp());
        assertTrue(firstService.isFailed());
        assertFalse(secondService.isUp());
        assertFalse(secondService.isFailed());
        shutdownContainer();
        assertFalse(firstService.isUp());
        assertTrue(firstService.isFailed());
        assertFalse(secondService.isUp());
        assertFalse(secondService.isFailed());
    }

    /**
     * Usecase:
     * <UL>
     *   <LI><B>first failing service</B> (NEVER mode), no dependencies</LI>
     *   <LI><B>second service</B> (ACTIVE mode), depends on <B>first service</B></LI>
     *   <LI>container shutdown</LI>
     * </UL>
     */
    @Test
    public void usecase21() throws Exception {
        final TestService firstService = addService(firstSN, true, NEVER);
        assertFalse(firstService.isUp());
        assertFalse(firstService.isFailed());
        final TestService secondService = addService(secondSN, ACTIVE, firstSN);
        assertFalse(firstService.isUp());
        assertFalse(firstService.isFailed());
        assertFalse(secondService.isUp());
        assertFalse(secondService.isFailed());
        shutdownContainer();
        assertFalse(firstService.isUp());
        assertFalse(firstService.isFailed());
        assertFalse(secondService.isUp());
        assertFalse(secondService.isFailed());
    }

    /**
     * Usecase:
     * <UL>
     *   <LI><B>first failing service</B> (ON_DEMAND mode), no dependencies</LI>
     *   <LI><B>second service</B> (ACTIVE mode), depends on <B>first service</B></LI>
     *   <LI>container shutdown</LI>
     * </UL>
     */
    @Test
    public void usecase22() throws Exception {
        final TestService firstService = addService(firstSN, true, ON_DEMAND);
        assertFalse(firstService.isUp());
        assertFalse(firstService.isFailed());
        final TestService secondService = addService(secondSN, ACTIVE, firstSN);
        assertFalse(firstService.isUp());
        assertTrue(firstService.isFailed());
        assertFalse(secondService.isUp());
        assertFalse(secondService.isFailed());
        shutdownContainer();
        assertFalse(firstService.isUp());
        assertTrue(firstService.isFailed());
        assertFalse(secondService.isUp());
        assertFalse(secondService.isFailed());
    }

    /**
     * Usecase:
     * <UL>
     *   <LI><B>first failing service</B> (LAZY mode), no dependencies</LI>
     *   <LI><B>second service</B> (ACTIVE mode), depends on <B>first service</B></LI>
     *   <LI>container shutdown</LI>
     * </UL>
     */
    @Test
    public void usecase23() throws Exception {
        final TestService firstService = addService(firstSN, true, LAZY);
        assertFalse(firstService.isUp());
        assertFalse(firstService.isFailed());
        final TestService secondService = addService(secondSN, ACTIVE, firstSN);
        assertFalse(firstService.isUp());
        assertTrue(firstService.isFailed());
        assertFalse(secondService.isUp());
        assertFalse(secondService.isFailed());
        shutdownContainer();
        assertFalse(firstService.isUp());
        assertTrue(firstService.isFailed());
        assertFalse(secondService.isUp());
        assertFalse(secondService.isFailed());
    }

    /**
     * Usecase:
     * <UL>
     *   <LI><B>first failing service</B> (PASSIVE mode), no dependencies</LI>
     *   <LI><B>second service</B> (ACTIVE mode), depends on <B>first service</B></LI>
     *   <LI>container shutdown</LI>
     * </UL>
     */
    @Test
    public void usecase24() throws Exception {
        final TestService firstService = addService(firstSN, true, PASSIVE);
        assertFalse(firstService.isUp());
        assertTrue(firstService.isFailed());
        final TestService secondService = addService(secondSN, ACTIVE, firstSN);
        assertFalse(firstService.isUp());
        assertTrue(firstService.isFailed());
        assertFalse(secondService.isUp());
        assertFalse(secondService.isFailed());
        shutdownContainer();
        assertFalse(firstService.isUp());
        assertTrue(firstService.isFailed());
        assertFalse(secondService.isUp());
        assertFalse(secondService.isFailed());
    }

    /**
     * Usecase:
     * <UL>
     *   <LI><B>first failing service</B> (ACTIVE mode), no dependencies</LI>
     *   <LI><B>second service</B> (ACTIVE mode), depends on <B>first service</B></LI>
     *   <LI>container shutdown</LI>
     * </UL>
     */
    @Test
    public void usecase25() throws Exception {
        final TestService firstService = addService(firstSN, true, ACTIVE);
        assertFalse(firstService.isUp());
        assertTrue(firstService.isFailed());
        final TestService secondService = addService(secondSN, ACTIVE, firstSN);
        assertFalse(firstService.isUp());
        assertTrue(firstService.isFailed());
        assertFalse(secondService.isUp());
        assertFalse(secondService.isFailed());
        shutdownContainer();
        assertFalse(firstService.isUp());
        assertTrue(firstService.isFailed());
        assertFalse(secondService.isUp());
        assertFalse(secondService.isFailed());
    }
}