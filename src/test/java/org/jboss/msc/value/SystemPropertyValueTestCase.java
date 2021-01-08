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

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.fail;
import static org.junit.Assert.assertSame;

import java.security.AccessControlContext;
import java.security.AccessController;

import org.junit.Test;

/**
 * Test for {@link SystemPropertyValue}.
 * 
 * @author <a href="mailto:flavia.rainone@jboss.com">Flavia Rainone</a>
 */
public class SystemPropertyValueTestCase {

    @Test
    public void javaVersionProperty() {
        final Value<String> value = new SystemPropertyValue("java.version");
        assertSame(System.getProperty("java.version"), value.getValue());
    }

    @Test
    public void javaHomePropertyWithDefaultValue() {
        final Value<String> value = new SystemPropertyValue("java.home", new ImmediateValue<String>("default"));
        assertSame(System.getProperty("java.home"), value.getValue());
    }

    @Test
    public void userNamePropertyWithAccessController() {
        final Value<String> value = new SystemPropertyValue("user.name", AccessController.getContext());
        assertSame(System.getProperty("user.name"), value.getValue());
    }

    @Test
    public void nonExistentProperty() {
        final Value<String> value = new SystemPropertyValue("non.existent");
        assertNull(value.getValue());
    }

    @Test
    public void nonExistentPropertyWithDefaultValue1() {
        final Value<String> value = new SystemPropertyValue("non.existent2", new ImmediateValue<String>("not found"));
        assertSame("not found", value.getValue());
    }

    @Test
    public void nonExistentPropertyWithDefaultValue2() {
        final Value<String> value = new SystemPropertyValue("non.existent3", new ClassOfValue<String>(new ImmediateValue<String>("default")));
        assertEquals(String.class.toString(), value.getValue());
    }

    @Test
    public void nonExistentPropertyWithDefaultValueAndAccessController() {
        final Value<String> value = new SystemPropertyValue("inexistent", AccessController.getContext(),
                new ImmediateValue<String>("default for inexistent"));
        assertEquals("default for inexistent", value.getValue());
    }

    @Test
    public void nonExistentPropertyWithNullDefaultValue() {
        final Value<String> value = new SystemPropertyValue("non.existent4", Values.nullValue());
        assertNull(value.getValue());
    }

    @Test
    public void nullProperty() {
        try {
            new SystemPropertyValue(null);
            fail("IllegalArgumentException should have been thrown");
        } catch (IllegalArgumentException e) {}
    }

    @Test
    public void nullAccessControlContext() {
        try {
            new SystemPropertyValue("java.vendor", (AccessControlContext) null);
            fail("IllegalArgumentException should have been thrown");
        } catch (IllegalArgumentException e) {}
    }

    @Test
    public void nullDefaultValue() {
        try {
            new SystemPropertyValue("os.name", (Value<?>) null);
            fail("IllegalArgumentException should have been thrown");
        } catch (IllegalArgumentException e) {}
    }

    @Test
    public void emptyProperty() {
        Value<String> value = new SystemPropertyValue("");
        try {
            value.getValue();
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException e) {}

        value = new SystemPropertyValue("", AccessController.getContext());
        try {
            value.getValue();
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException e) {}

        value = new SystemPropertyValue("", new ImmediateValue<String>("default"));
        try {
            value.getValue();
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException e) {}

        value = new SystemPropertyValue("", AccessController.getContext(), new ImmediateValue<String>("default"));
        try {
            value.getValue();
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException e) {}
    }
}
