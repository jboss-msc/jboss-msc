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
 * Test for {@link EnvironmentPropertyValue}.
 * 
 * @author <a href="mailto:flavia.rainone@jboss.com">Flavia Rainone</a>
 */
public class EnvironmentPropertyValueTestCase {

    @Test
    public void pathEnvironmentProperty() {
        final Value<String> value = new EnvironmentPropertyValue("PATH");
        assertSame(System.getenv("PATH"), value.getValue());
    }

    @Test
    public void pathEnvironmentPropertyWithDefaultValue() {
        final Value<String> value = new EnvironmentPropertyValue("PATH", new ImmediateValue<String>("default"));
        assertSame(System.getenv("PATH"), value.getValue());
    }

    @Test
    public void pathEnvironmentPropertyWithAccessController() {
        final Value<String> value = new EnvironmentPropertyValue("PATH", AccessController.getContext());
        assertSame(System.getenv("PATH"), value.getValue());
    }

    @Test
    public void nonExistentProperty() {
        final Value<String> value = new EnvironmentPropertyValue("any");
        assertNull(value.getValue());
    }

    @Test
    public void nonExistentPropertyWithDefaultValue1() {
        final Value<String> value = new EnvironmentPropertyValue("any_", new ImmediateValue<String>("not found"));
        assertSame("not found", value.getValue());
    }

    @Test
    public void nonExistentPropertyWithDefaultValue2() {
        final Value<String> value = new EnvironmentPropertyValue("another_property", new ClassOfValue<String>(new ImmediateValue<String>("default")));
        assertEquals(String.class.toString(), value.getValue());
    }

    @Test
    public void nonExistentPropertyWithDefaultValueAndAccessController() {
        final Value<String> value = new EnvironmentPropertyValue("inexistent", AccessController.getContext(),
                new ImmediateValue<String>("default for inexistent"));
        assertEquals("default for inexistent", value.getValue());
    }

    @Test
    public void nonExistentPropertyWithNullDefaultValue() {
        final Value<String> value = new EnvironmentPropertyValue("inexistent", Values.nullValue());
        assertNull(value.getValue());
    }

    @Test
    public void nullEnvironmentProperty() {
        try {
            new EnvironmentPropertyValue(null);
            fail("IllegalArgumentException should have been thrown");
        } catch (IllegalArgumentException e) {}
    }

    @Test
    public void nullAccessControlContext() {
        try {
            new EnvironmentPropertyValue("PATH", (AccessControlContext) null);
            fail("IllegalArgumentException should have been thrown");
        } catch (IllegalArgumentException e) {}
    }

    @Test
    public void nullDefaultValue() {
        try {
            new EnvironmentPropertyValue("PATH", (Value<?>) null);
            fail("IllegalArgumentException should have been thrown");
        } catch (IllegalArgumentException e) {}
    }

    @Test
    public void emptyProperty() {
        Value<String> value = new EnvironmentPropertyValue("");
        try {
            value.getValue();
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException e) {}

        value = new EnvironmentPropertyValue("", AccessController.getContext());
        try {
            value.getValue();
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException e) {}

        value = new EnvironmentPropertyValue("", new ImmediateValue<String>("default"));
        try {
            value.getValue();
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException e) {}

        value = new EnvironmentPropertyValue("", AccessController.getContext(), new ImmediateValue<String>("default"));
        try {
            value.getValue();
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException e) {}
    }
}
