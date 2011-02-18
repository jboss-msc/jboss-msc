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

package org.jboss.msc.inject;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import org.jboss.msc.util.TargetWrapper;
import org.jboss.msc.value.ImmediateValue;
import org.jboss.msc.value.Value;
import org.jboss.msc.value.Values;
import org.jboss.msc.value.util.AnotherService;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Test for {@link SetMethodInjector}.
 * 
 * @author <a href="mailto:flavia.rainone@jboss.com">Flavia Rainone</a>
 */
public class SetMethodInjectorTestCase {
    private static AnotherService service;
    private static Value<AnotherService> target;
    private static Method method;
    private static Value<Method> methodValue;

    @BeforeClass
    public static void setUpStatic() throws Exception {
        service = new AnotherService(5, true, "JBoss");
        target = new ImmediateValue<AnotherService>(service);
        method = AnotherService.class.getMethod("setRetry", int.class);
        methodValue = new ImmediateValue<Method>(method);
    }

    @Test
    public void publicMethod1() throws Exception {
        final Injector<Integer> injector = SetMethodInjector.<Integer>create(target, methodValue);
        assertSetRetryInjector(injector);
    }

    @Test
    public void publicMethod2() throws Exception {
        final Injector<Integer> injector = SetMethodInjector.<Integer>create(target, method);
        assertSetRetryInjector(injector);
    }

    @Test
    public void lookupAndInject1() throws Exception {
        final Injector<Integer> injector = new SetMethodInjector<Integer>(service, AnotherService.class, "setRetry", int.class);
        assertSetRetryInjector(injector);
    }

    @Test
    public void lookupAndInject2() throws Exception {
        final Injector<Integer> injector = new SetMethodInjector<Integer>(target, AnotherService.class, "setRetry", int.class);
        assertSetRetryInjector(injector);
    }

    @Test
    public void lookupUnnaccessibleSetMethod() throws Exception {
        try {
            new SetMethodInjector<String>(target, AnotherService.class, "setDefinedBy", String.class);
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException e) {}
    }

    @Test
    public void lookupInexistentSetMethod() throws Exception {
        try {
            new SetMethodInjector<String>(target, AnotherService.class, "setDefinedFor", String.class);
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException e) {}
    }

    private void assertSetRetryInjector(Injector<Integer> injector) {
        injector.inject(149);
        assertEquals(149, service.getRetry());
        injector.uninject();
        assertEquals(149, service.getRetry());
        injector.inject(154);
        assertEquals(154, service.getRetry());
    }


    @Test
    public void publicMethodWithExceptionOnUninjection() throws Exception {
        final TargetWrapper<?> targetWrapper = new TargetWrapper<Object>(new StringBuffer());
        Injector<Boolean> injector = new SetMethodInjector<Boolean>(targetWrapper, TargetWrapper.class, "readTarget", boolean.class);
        injector.uninject();
    }

    @Test
    public void unaccessibleMethod() throws Exception {
        final Method method = AnotherService.class.getDeclaredMethod("setDefinedBy", String.class);
        final Injector<String> injector = new SetMethodInjector<String>(target, Values.immediateValue(method));
        try {
            injector.inject("unnaccessibleMethod");
            fail("IllegalExcpetion expected");
        } catch (InjectionException e) {}
        method.setAccessible(true);
        injector.inject("unnaccessibleMethod - attempt2");
        final Field definedBy = AnotherService.class.getDeclaredField("definedBy");
        definedBy.setAccessible(true);
        assertEquals("unnaccessibleMethod - attempt2", definedBy.get(service));
        injector.uninject();
        assertNull(definedBy.get(service));
    }

    @Test
    public void methodWithException() throws Exception {
        final Injector<Boolean> injector = new SetMethodInjector<Boolean>(service, AnotherService.class, "discoverDefinedBy", boolean.class);
        try {
            injector.inject(true);
            fail("InjectionException expected");
        } catch (InjectionException e) {}
        injector.uninject(); // no error expected for uninjection
    }

    @Test
    public void nullSetMethod() throws Exception {
        Injector<Integer> injector;

        injector = new SetMethodInjector<Integer>(null, method);
        try {
            injector.inject(0);
            fail("InjectionException expected");
        } catch (InjectionException e) {}

        injector = new SetMethodInjector<Integer>(target, (Method) null);
        try {
            injector.inject(0);
            fail("InjectionException expected");
        } catch (InjectionException e) {}

        injector = new SetMethodInjector<Integer>(null, (Method) null);
        try {
            injector.inject(0);
            fail("InjectionException expected");
        } catch (InjectionException e) {}
    }
}
