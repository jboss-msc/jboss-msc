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

import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;

import org.jboss.modules.Module;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.msc.value.util.AnotherService;
import org.jboss.msc.value.util.AnyService;
import org.junit.Test;

/**
 * Test for {@link LookupModuleClassValue}.
 * 
 * @author <a href="mailto:flavia.rainone@jboss.com">Flavia Rainone</a>
 */
public class LookupModuleValueTestCase {

    @Test
    public void lookupAnotherServiceClassValue() {
        final Value<Class<?>> value = new LookupModuleClassValue(AnotherService.class.getName(), ModuleIdentifier.SYSTEM, Module.getSystemModuleLoader());
        assertSame(AnotherService.class, value.getValue());
        assertSame(AnotherService.class, value.getValue());
        assertSame(AnotherService.class, value.getValue());
    }

    @Test
    public void lookupAnyServiceClassValue() {
        final Value<Class<?>> value = new LookupModuleClassValue(AnyService.class.getName(), ModuleIdentifier.SYSTEM, Module.getSystemModuleLoader());
        assertSame(AnyService.class, value.getValue());
        assertSame(AnyService.class, value.getValue());
        assertSame(AnyService.class, value.getValue());
    }

    @Test
    public void lookupNonExistentModuleClassValue() {
        final Value<Class<?>> value = new LookupModuleClassValue(AnotherService.class.getName(), ModuleIdentifier.fromString("non:existent"), Module.getSystemModuleLoader());
        try {
            value.getValue();
            fail("IllegalStateException expected");
        } catch (IllegalStateException e) {}
    }

    @Test
    public void lookupModuleNonExistentClassValue() {
        Value<Class<?>> value = new LookupModuleClassValue("NonExistentClass", ModuleIdentifier.SYSTEM, Module.getSystemModuleLoader());
        try {
            value.getValue();
            fail("IllegalStateException expected");
        } catch (IllegalStateException e) {}
    }

    @Test
    public void illegalLookupModuleClassValue() {
        try {
            new LookupModuleClassValue(null, ModuleIdentifier.SYSTEM, Module.getSystemModuleLoader());
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException e) {}

        try {
            new LookupModuleClassValue(AnyService.class.getName(), null, Module.getSystemModuleLoader());
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException e) {}

        try {
            new LookupModuleClassValue(null, null, Module.getSystemModuleLoader());
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException e) {}
    }
}