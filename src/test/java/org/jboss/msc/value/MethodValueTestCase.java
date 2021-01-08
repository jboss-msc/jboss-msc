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

package org.jboss.msc.value;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.jboss.msc.value.util.AnotherService;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Test for {@link MethodValue}.
 * 
 * @author <a href="mailto:flavia.rainone@jboss.com">Flavia Rainone</a>
 */
public class MethodValueTestCase {

    private static Value<AnotherService> target;
    private static Value<Method> method;
    private List<Value<Boolean>> arguments;

    @BeforeClass
    public static void staticSetUp() throws Exception {
        target = new ImmediateValue<AnotherService>(new AnotherService(1, true, "JBoss"));
        method = new ImmediateValue<Method>(AnotherService.class.getMethod("discoverDefinedBy", boolean.class));
    }

    @Before
    public void setUp() {
        arguments = new ArrayList<Value<Boolean>>();
    }

    @Test
    public void publicMethodValue() throws Exception {
        arguments.add(new ImmediateValue<Boolean>(false));
        final MethodValue<String> value = new MethodValue<String>(method, target, arguments);
        Values.thisValue().setValue(Values.immediateValue(this));
        assertEquals("JBoss", value.getValue());
        assertSame(this, Values.thisValue().getValue());
    }

    @Test
    public void unaccessibleMethodValue() throws Exception {
        final Value<Method> unaccessibleMethod = new ImmediateValue<Method>(AnotherService.class.getDeclaredMethod("getDefinedBy"));
        final MethodValue<String> value = new MethodValue<String>(unaccessibleMethod, target, arguments);
        try {
            value.getValue();
            fail("IllegalStateExcpetion expected");
        } catch (IllegalStateException e) {}
    }

    @Test
    public void methodValueWithException() throws Exception {
        arguments.add(new ImmediateValue<Boolean>(true));
        final MethodValue<String> value = new MethodValue<String>(method, target, arguments);
        try {
            value.getValue();
            fail("IllegalStateException expected");
        } catch (IllegalStateException e) {}
    }

    @Test
    public void nullMethodValue() throws Exception {
        arguments.add(new ImmediateValue<Boolean>(false));

        MethodValue<String> value = new MethodValue<String>(null, target, arguments);
        try {
            value.getValue();
            fail("NullPointerException expected");
        } catch (NullPointerException e) {}

        value = new MethodValue<String>(method, null, arguments);
        try {
            value.getValue();
            fail("NullPointerException expected");
        } catch (NullPointerException e) {}

        value = new MethodValue<String>(method, target, null);
        try {
            value.getValue();
            fail("NullPointerException expected");
        } catch (NullPointerException e) {}

        value = new MethodValue<String>(null, null, arguments);
        try {
            value.getValue();
            fail("NullPointerException expected");
        } catch (NullPointerException e) {}

        value = new MethodValue<String>(null, target, null);
        try {
            value.getValue();
            fail("NullPointerException expected");
        } catch (NullPointerException e) {}

        value = new MethodValue<String>(method, null, null);
        try {
            value.getValue();
            fail("NullPointerException expected");
        } catch (NullPointerException e) {}

        value = new MethodValue<String>(null, null, null);
        try {
            value.getValue();
            fail("NullPointerException expected");
        } catch (NullPointerException e) {}

        value = new MethodValue<String>(Values.<Method>nullValue(), target, arguments);
        try {
            value.getValue();
            fail("NullPointerException expected");
        } catch (NullPointerException e) {}
        
        value = new MethodValue<String>(method, Values.<AnotherService>nullValue(), arguments);
        try {
            value.getValue();
            fail("NullPointerException expected");
        } catch (NullPointerException e) {}

        value = new MethodValue<String>(Values.<Method>nullValue(), Values.<AnotherService>nullValue(), arguments);
        try {
            value.getValue();
            fail("NullPointerException expected");
        } catch (NullPointerException e) {}

        arguments.clear();
        arguments.add(Values.<Boolean>nullValue());
        value = new MethodValue<String>(method, target, arguments);
        try {
            value.getValue();
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException e) {}

        value = new MethodValue<String>(Values.<Method>nullValue(), target, arguments);
        try {
            value.getValue();
            fail("NullPointerException expected");
        } catch (NullPointerException e) {}
        
        value = new MethodValue<String>(method, Values.<AnotherService>nullValue(), arguments);
        try {
            value.getValue();
            fail("NullPointerException expected");
        } catch (NullPointerException e) {}

        arguments.add(new ImmediateValue<Boolean>(false));
        value = new MethodValue<String>(Values.<Method>nullValue(), Values.<AnotherService>nullValue(), arguments);
        try {
            value.getValue();
            fail("NullPointerException expected");
        } catch (NullPointerException e) {}
    }
}
