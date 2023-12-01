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

import org.junit.Test;

import java.util.concurrent.TimeUnit;

public final class ServiceContainerTestCase extends ServiceContainerTestBase {

    @Test
    public void test1() throws Exception {
        ServiceContainer container = ServiceContainer.Factory.create();
        assertStartedContainerInvariants(container);
        container.shutdown();
        container.awaitTermination();
        assertStoppedContainerInvariants(container);
    }

    @Test
    public void test2() throws Exception {
        ServiceContainer container = ServiceContainer.Factory.create("Foo");
        assertStartedContainerInvariants(container);
        container.shutdown();
        container.awaitTermination();
        assertStoppedContainerInvariants(container);
    }

    @Test
    public void test3() throws Exception {
        ServiceContainer container = ServiceContainer.Factory.create(false);
        assertStartedContainerInvariants(container);
        container.shutdown();
        container.awaitTermination();
        assertStoppedContainerInvariants(container);
    }

    @Test
    public void test4() throws Exception {
        ServiceContainer container = ServiceContainer.Factory.create("Foo", false);
        assertStartedContainerInvariants(container);
        container.shutdown();
        container.awaitTermination();
        assertStoppedContainerInvariants(container);
    }

    @Test
    public void test5() throws Exception {
        ServiceContainer container = ServiceContainer.Factory.create(1, 60L, TimeUnit.MILLISECONDS);
        assertStartedContainerInvariants(container);
        container.shutdown();
        container.awaitTermination();
        assertStoppedContainerInvariants(container);
    }

    @Test
    public void test6() throws Exception {
        ServiceContainer container = ServiceContainer.Factory.create(1, 60L, TimeUnit.MILLISECONDS, true);
        assertStartedContainerInvariants(container);
        container.shutdown();
        container.awaitTermination();
        assertStoppedContainerInvariants(container);
    }

    @Test
    public void test7() throws Exception {
        ServiceContainer container = ServiceContainer.Factory.create("Foo", 1, 60L, TimeUnit.MILLISECONDS);
        assertStartedContainerInvariants(container);
        container.shutdown();
        container.awaitTermination();
        assertStoppedContainerInvariants(container);
    }

    @Test
    public void test8() throws Exception {
        ServiceContainer container = ServiceContainer.Factory.create("Foo", 1, 60L, TimeUnit.MILLISECONDS, true);
        assertStartedContainerInvariants(container);
        container.shutdown();
        container.awaitTermination();
        assertStoppedContainerInvariants(container);
    }

}
