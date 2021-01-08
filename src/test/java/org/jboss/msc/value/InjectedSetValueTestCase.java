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

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import junit.framework.Assert;

import org.jboss.msc.inject.Injector;
import org.junit.Test;

/**
 * Test for {@link InjectedSetValue}.
 * 
 * @author Stuart Douglas
 */
public class InjectedSetValueTestCase {

    @Test
    public void nonInjectedValue() {
        Assert.assertEquals(Collections.emptySet(), new InjectedSetValue<Object>().getValue());
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
        final InjectedSetValue<Integer> injectedSetValue = new InjectedSetValue<Integer>();
        injectedSetValue.injector().inject(15);
        injectedSetValue.injector().inject(10);
        Set<Integer> expected = new LinkedHashSet<Integer>();
        expected.add(15);
        expected.add(10);
        assertEquals(expected, injectedSetValue.getValue());
        assertEquals(expected, injectedSetValue.getValue());
    }

    @Test
    public void injectedObjectValue() {
        final Object anyObject = new Object();
        final Object anyObject2 = new Object();
        final InjectedSetValue<Object> injectedSetValue = new InjectedSetValue<Object>();
        injectedSetValue.injector().inject(anyObject);
        injectedSetValue.injector().inject(anyObject2);
        Set<Object> expected = new LinkedHashSet<Object>();
        expected.add(anyObject);
        expected.add(anyObject2);
        assertEquals(expected, injectedSetValue.getValue());
        assertEquals(expected, injectedSetValue.getValue());
    }

    @Test
    public void injectedNullValue() {
        final InjectedSetValue<Object> injectedSetValue = new InjectedSetValue<Object>();
        injectedSetValue.injector().inject(null);
        assertEquals(Collections.singleton(null), injectedSetValue.getValue());
        assertEquals(Collections.singleton(null), injectedSetValue.getValue());
    }

    @Test
    public void uninjectedValue() {
        final InjectedSetValue<Integer> injectedSetValue = new InjectedSetValue<Integer>();
        Injector<Integer> firstIInjector = injectedSetValue.injector();
        firstIInjector.inject(15);
        injectedSetValue.injector().inject(10);
        Set<Integer> expected = new LinkedHashSet<Integer>();
        expected.add(15);
        expected.add(10);
        assertEquals(expected, injectedSetValue.getValue());
        assertEquals(expected, injectedSetValue.getValue());
        firstIInjector.uninject();
        expected = new LinkedHashSet<Integer>();
        expected.add(10);
        assertEquals(expected, injectedSetValue.getValue());
        assertEquals(expected, injectedSetValue.getValue());
    }
}
