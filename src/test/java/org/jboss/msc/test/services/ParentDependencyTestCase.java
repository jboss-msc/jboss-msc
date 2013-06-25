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
import static org.jboss.msc.service.ServiceMode.ON_DEMAND;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.jboss.msc.service.DependencyFlag;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceMode;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.test.utils.AbstractServiceTest;
import org.jboss.msc.test.utils.TestService;
import org.jboss.msc.txn.Transaction;
import org.junit.Test;

/**
 * Parent dependencies test case
 * 
 * @author <a href="mailto:frainone@redhat.com">Flavia Rainone</a>
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
public class ParentDependencyTestCase extends AbstractServiceTest {

    private static final ServiceName firstSN = ServiceName.of("first");
    private static final ServiceName secondSN = ServiceName.of("second");

    protected final TestService addService(final ServiceName serviceName, final ServiceMode serviceMode, final ServiceName parentDependency) throws InterruptedException {
        final Transaction txn = newTransaction();
        final TestService service = new TestService(false);
        final ServiceBuilder<Void> serviceBuilder = txn.addService(serviceRegistry, serviceName);
        if (serviceMode != null) serviceBuilder.setMode(serviceMode);
        serviceBuilder.setService(service);
        serviceBuilder.addDependency(parentDependency, DependencyFlag.PARENT);
        serviceBuilder.install();
        commit(txn);
        final TestService parentService = (TestService) serviceRegistry.getService(parentDependency);
        if (service.isUp() || (parentService != null && parentService.isUp())) {
            assertSame(service, serviceRegistry.getRequiredService(serviceName));
        } else {
            assertNull(serviceRegistry.getService(serviceName));
        }
        return service;
    }

    /**
     * Usecase:
     * <UL>
     *   <LI><B>first service</B> (ON_DEMAND mode), no dependencies</LI>
     *   <LI><B>second service</B> (ON_DEMAND mode), depends on <B>first service</B></LI>
     *   <LI>dependency removed before container is shut down</LI>
     * </UL>
     */
    @Test
    public void usecase1() throws Exception {
        final TestService firstService = addService(firstSN, ON_DEMAND);
        assertFalse(firstService.isUp());
        final TestService secondService = addService(secondSN, ON_DEMAND, firstSN);
        assertFalse(firstService.isUp());
        assertFalse(secondService.isUp());
        removeService(firstSN);
        assertFalse(firstService.isUp());
    }

    /**
     * Usecase:
     * <UL>
     *   <LI><B>first service</B> (LAZY mode), no dependencies</LI>
     *   <LI><B>second service</B> (ON_DEMAND mode), depends on <B>first service</B></LI>
     *   <LI>dependency removed before container is shut down</LI>
     * </UL>
     */
    @Test
    public void usecase2() throws Exception {
        final TestService firstService = addService(firstSN, LAZY);
        assertFalse(firstService.isUp());
        final TestService secondService = addService(secondSN, ON_DEMAND, firstSN);
        assertFalse(firstService.isUp());
        assertFalse(secondService.isUp());
        removeService(firstSN);
        assertFalse(firstService.isUp());
        assertFalse(secondService.isUp());
    }

    /**
     * Usecase:
     * <UL>
     *   <LI><B>first service</B> (ACTIVE mode), no dependencies</LI>
     *   <LI><B>second service</B> (ON_DEMAND mode), depends on <B>first service</B></LI>
     *   <LI>dependency removed before container is shut down</LI>
     * </UL>
     */
    @Test
    public void usecase3() throws Exception {
        final TestService firstService = addService(firstSN, ACTIVE);
        assertTrue(firstService.isUp());
        final TestService secondService = addService(secondSN, ON_DEMAND, firstSN);
        assertTrue(firstService.isUp());
        assertFalse(secondService.isUp());
        removeService(firstSN);
        assertFalse(firstService.isUp());
        assertFalse(secondService.isUp());
    }

    /**
     * Usecase:
     * <UL>
     *   <LI><B>first service</B> (ON_DEMAND mode), no dependencies</LI>
     *   <LI><B>second service</B> (LAZY mode), depends on <B>first service</B></LI>
     *   <LI>dependency removed before container is shut down</LI>
     * </UL>
     */
    @Test
    public void usecase4() throws Exception {
        final TestService firstService = addService(firstSN, ON_DEMAND);
        assertFalse(firstService.isUp());
        final TestService secondService = addService(secondSN, LAZY, firstSN);
        assertFalse(firstService.isUp());
        assertFalse(secondService.isUp());
        removeService(firstSN);
        assertFalse(firstService.isUp());
        assertFalse(secondService.isUp());
    }

    /**
     * Usecase:
     * <UL>
     *   <LI><B>first service</B> (LAZY mode), no dependencies</LI>
     *   <LI><B>second service</B> (LAZY mode), depends on <B>first service</B></LI>
     *   <LI>dependency removed before container is shut down</LI>
     * </UL>
     */
    @Test
    public void usecase5() throws Exception {
        final TestService firstService = addService(firstSN, LAZY);
        assertFalse(firstService.isUp());
        final TestService secondService = addService(secondSN, LAZY, firstSN);
        assertFalse(firstService.isUp());
        assertFalse(secondService.isUp());
        removeService(firstSN);
        assertFalse(firstService.isUp());
        assertFalse(secondService.isUp());
    }

    /**
     * Usecase:
     * <UL>
     *   <LI><B>first service</B> (ACTIVE mode), no dependencies</LI>
     *   <LI><B>second service</B> (LAZY mode), depends on <B>first service</B></LI>
     *   <LI>dependency removed before container is shut down</LI>
     * </UL>
     */
    @Test
    public void usecase6() throws Exception {
        final TestService firstService = addService(firstSN, ACTIVE);
        assertTrue(firstService.isUp());
        final TestService secondService = addService(secondSN, LAZY, firstSN);
        assertTrue(firstService.isUp());
        assertFalse(secondService.isUp());
        removeService(firstSN);
        assertFalse(firstService.isUp());
        assertFalse(secondService.isUp());
    }

    /**
     * Usecase:
     * <UL>
     *   <LI><B>first service</B> (ON_DEMAND mode), no dependencies</LI>
     *   <LI><B>second service</B> (ACTIVE mode), depends on <B>first service</B></LI>
     *   <LI>dependency removed before container is shut down</LI>
     * </UL>
     */
    @Test
    public void usecase7() throws Exception {
        final TestService firstService = addService(firstSN, ON_DEMAND);
        final TestService secondService = addService(secondSN, ACTIVE, firstSN);
        assertFalse(firstService.isUp());
        assertFalse(secondService.isUp());
        removeService(firstSN);
        assertFalse(firstService.isUp());
        assertFalse(secondService.isUp());
    }

    /**
     * Usecase:
     * <UL>
     *   <LI><B>first service</B> (LAZY mode), no dependencies</LI>
     *   <LI><B>second service</B> (ACTIVE mode), depends on <B>first service</B></LI>
     *   <LI>dependency removed before container is shut down</LI>
     * </UL>
     */
    @Test
    public void usecase8() throws Exception {
        final TestService firstService = addService(firstSN, LAZY);
        assertFalse(firstService.isUp());
        final TestService secondService = addService(secondSN, ACTIVE, firstSN);
        assertFalse(firstService.isUp());
        assertFalse(secondService.isUp());
        removeService(firstSN);
        assertFalse(firstService.isUp());
        assertFalse(secondService.isUp());
    }

    /**
     * Usecase:
     * <UL>
     *   <LI><B>first service</B> (ACTIVE mode), no dependencies</LI>
     *   <LI><B>second service</B> (ACTIVE mode), depends on <B>first service</B></LI>
     *   <LI>dependency removed before container is shut down</LI>
     * </UL>
     */
    @Test
    public void usecase9() throws Exception {
        final TestService firstService = addService(firstSN, ACTIVE);
        assertTrue(firstService.isUp());
        final TestService secondService = addService(secondSN, ACTIVE, firstSN);
        assertTrue(firstService.isUp());
        assertTrue(secondService.isUp());
        removeService(firstSN);
        assertFalse(firstService.isUp());
        assertFalse(secondService.isUp());
    }
}