/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2018, Red Hat, Inc., and individual contributors
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

package org.jboss.msc.multi_value_services;

import static org.junit.Assert.fail;

import org.jboss.msc.inject.InjectionException;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.service.AbstractServiceListener;
import org.jboss.msc.service.AbstractServiceTest;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StabilityMonitor;
import org.jboss.msc.value.InjectedValue;

import java.util.Collections;
import java.util.List;

import org.junit.Test;

/**
 * <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
public class MixingOldAndNewServicesAPITestCase extends AbstractServiceTest {

    private static final ServiceName ID = ServiceName.of("id");
    private static final ServiceName A = ServiceName.of("a");
    private static final ServiceName B = ServiceName.of("b");
    private static final List DEPENDENCIES = Collections.singletonList(B);
    private static final List LISTENERS = Collections.singletonList(NoopServiceListener.INSTANCE);

    /**
     * If ServiceBuilder is created via deprecated addService()
     * methods then it is forbidden to use new "multi value services" API.
     */
    @Test
    public void testOldServiceAPIUsingMultiValueServicesAPI() {
        ServiceBuilder<?> sb = serviceContainer.addService(A, Service.NULL);
        try {
            sb.requires(B);
            fail("UnsupportedOperationException expected");
        } catch (UnsupportedOperationException ignored) {}
        try {
            sb.provides(B);
            fail("UnsupportedOperationException expected");
        } catch (UnsupportedOperationException ignored) {}
        try {
            sb.setInstance(null);
            fail("UnsupportedOperationException expected");
        } catch (UnsupportedOperationException ignored) {}

        sb = serviceContainer.addServiceValue(A, Service.NULL_VALUE);
        try {
            sb.requires(B);
            fail("UnsupportedOperationException expected");
        } catch (UnsupportedOperationException ignored) {}
        try {
            sb.provides(B);
            fail("UnsupportedOperationException expected");
        } catch (UnsupportedOperationException ignored) {}
        try {
            sb.setInstance(null);
            fail("UnsupportedOperationException expected");
        } catch (UnsupportedOperationException ignored) {}
    }

    /**
     * If ServiceBuilder is created via undeprecated addService()
     * method then it is forbidden to use old "single value services" API.
     */
    @Test
    public void testMultiValueServicesAPIUsingOldServiceAPI() {
        ServiceBuilder<?> sb = serviceContainer.addService(ID);
        try {
            sb.addDependencies(B);
            fail("UnsupportedOperationException expected");
        } catch (UnsupportedOperationException ignored) {}
        try {
            sb.addDependencies(DEPENDENCIES);
            fail("UnsupportedOperationException expected");
        } catch (UnsupportedOperationException ignored) {}
        try {
            sb.addDependencies(ServiceBuilder.DependencyType.REQUIRED, B);
            fail("UnsupportedOperationException expected");
        } catch (UnsupportedOperationException ignored) {}
        try {
            sb.addDependencies(ServiceBuilder.DependencyType.REQUIRED, DEPENDENCIES);
            fail("UnsupportedOperationException expected");
        } catch (UnsupportedOperationException ignored) {}
        try {
            sb.addDependency(B);
            fail("UnsupportedOperationException expected");
        } catch (UnsupportedOperationException ignored) {}
        try {
            sb.addDependency(ServiceBuilder.DependencyType.REQUIRED, B);
            fail("UnsupportedOperationException expected");
        } catch (UnsupportedOperationException ignored) {}
        try {
            sb.addDependency(B, NoopInjector.INSTANCE);
            fail("UnsupportedOperationException expected");
        } catch (UnsupportedOperationException ignored) {}
        try {
            sb.addDependency(B, Object.class, NoopInjector.INSTANCE);
            fail("UnsupportedOperationException expected");
        } catch (UnsupportedOperationException ignored) {}
        try {
            sb.addDependency(ServiceBuilder.DependencyType.REQUIRED, B, NoopInjector.INSTANCE);
            fail("UnsupportedOperationException expected");
        } catch (UnsupportedOperationException ignored) {}
        try {
            sb.addDependency(ServiceBuilder.DependencyType.REQUIRED, B, Object.class, NoopInjector.INSTANCE);
            fail("UnsupportedOperationException expected");
        } catch (UnsupportedOperationException ignored) {}
        try {
            sb.addInjection(NoopInjector.INSTANCE);
            fail("UnsupportedOperationException expected");
        } catch (UnsupportedOperationException ignored) {}
        try {
            sb.addInjection(NoopInjector.INSTANCE, new Object());
            fail("UnsupportedOperationException expected");
        } catch (UnsupportedOperationException ignored) {}
        try {
            sb.addInjectionValue(NoopInjector.INSTANCE, new InjectedValue<>());
            fail("UnsupportedOperationException expected");
        } catch (UnsupportedOperationException ignored) {}
        try {
            sb.addMonitors(new StabilityMonitor());
            fail("UnsupportedOperationException expected");
        } catch (UnsupportedOperationException ignored) {}
        try {
            sb.addListener(NoopServiceListener.INSTANCE);
            fail("UnsupportedOperationException expected");
        } catch (UnsupportedOperationException ignored) {}
        try {
            sb.addListener(NoopServiceListener.INSTANCE, NoopServiceListener.INSTANCE);
            fail("UnsupportedOperationException expected");
        } catch (UnsupportedOperationException ignored) {}
        try {
            sb.addListener(LISTENERS);
            fail("UnsupportedOperationException expected");
        } catch (UnsupportedOperationException ignored) {}
    }

    private static final class NoopInjector implements Injector<Object> {
        private static final NoopInjector INSTANCE = new NoopInjector();

        @Override
        public void inject(Object value) throws InjectionException {
            throw new UnsupportedOperationException();
        }

        @Override
        public void uninject() {
            throw new UnsupportedOperationException();
        }
    }

    private static final class NoopServiceListener extends AbstractServiceListener {
        private static final NoopServiceListener INSTANCE = new NoopServiceListener();
    }

}
