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

package org.jboss.msc.value;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.fail;

import org.junit.Test;

/**
 * Test for {@link InjectedValue}.
 * 
 * @author <a href="mailto:flavia.rainone@jboss.com">Flavia Rainone</a>
 */
public class InjectedValueTestCase {

    @Test
    public void nonInjectedValue() {
        assertNoInjectedValue(new InjectedValue<String>());
    }

    private void assertNoInjectedValue(InjectedValue<?> injectedValue) {
        try {
            injectedValue.getValue();
            fail ("IllegalStateException should have been thrown");
        } catch (IllegalStateException e) {}
        assertNull(injectedValue.getOptionalValue());
    }

    @Test
    public void injectedIntValue() {
        final InjectedValue<Integer> injectedValue = new InjectedValue<Integer>();
        injectedValue.inject(15);
        assertEquals(15, (int) injectedValue.getValue());
        assertEquals(15, (int) injectedValue.getOptionalValue());
    }

    @Test
    public void injectedObjectValue() {
        final Object anyObject = new Object();
        final InjectedValue<Object> injectedValue = new InjectedValue<Object>();
        injectedValue.inject(anyObject);
        assertEquals(anyObject, injectedValue.getOptionalValue());
        assertEquals(anyObject, injectedValue.getValue());
    }

    @Test
    public void injectedNullValue() {
        final InjectedValue<Object> injectedValue = new InjectedValue<Object>();
        injectedValue.inject(null);
        assertNull(injectedValue.getValue());
        assertNull(injectedValue.getOptionalValue());
    }

    @Test
    public void uninjectedValue() {
        final InjectedValue<Boolean> injectedValue = new InjectedValue<Boolean>();
        injectedValue.inject(true);
        assertEquals(true, (boolean) injectedValue.getValue());
        assertEquals(true, (boolean) injectedValue.getOptionalValue());

        injectedValue.uninject();
        assertNoInjectedValue(injectedValue);
    }
}