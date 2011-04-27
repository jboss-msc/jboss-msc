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

import static junit.framework.Assert.fail;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jboss.msc.value.util.AbstractService;
import org.jboss.msc.value.util.ConstructedService;
import org.jboss.msc.value.util.ConstructedService.Signature;
import org.junit.Test;

/**
 * Test for {@link ConstructedValue}.
 * 
 * @author <a href="mailto:flavia.rainone@jboss.com">Flavia Rainone</a>
 */
public class ConstructedValueTestCase {

    @Test
    public void defaultConstructor() throws Exception {
        final Constructor<ConstructedService> constructor = getConstructor(ConstructedService.class);
        final Value<ConstructedService> value = new ConstructedValue<ConstructedService>(constructor, Collections.<Value<?>>emptyList());
        assertServiceValuesCreated(value, Signature.DEFAULT);
    }

    @Test
    public void defaultConstructorWrongSignature() throws Exception {
        final Constructor<ConstructedService> constructor = getConstructor(ConstructedService.class);
        final List<Value<Integer>> params= new ArrayList<Value<Integer>>();
        params.add(new ImmediateValue<Integer>(5));
        final Value<ConstructedService> value = new ConstructedValue<ConstructedService>(constructor, params);
        try {
            value.getValue();
            fail("constructor should have thrown illegal argument exception");
        } catch(IllegalArgumentException e) {}
    }

    @Test
    public void constructorWithIntParam() throws Exception {
        final Constructor<ConstructedService> constructor = getConstructor(ConstructedService.class, int.class);
        final List<Value<Integer>> params= new ArrayList<Value<Integer>>();
        params.add(new ImmediateValue<Integer>(11));
        assertServiceValuesCreated(new ConstructedValue<ConstructedService>(constructor, params), Signature.INTEGER);
    }

    @Test
    public void constructorWithIntParamThrowsException() throws Exception {
        final Constructor<ConstructedService> constructor = getConstructor(ConstructedService.class, int.class);
        final List<Value<Integer>> params= new ArrayList<Value<Integer>>();
        params.add(new ImmediateValue<Integer>(0));
        final Value<ConstructedService> value = new ConstructedValue<ConstructedService>(constructor, params);
        try {
            value.getValue();
            fail("constructor should have thrown invocation target exception");
        } catch (IllegalStateException e) {}
    }

    @Test
    public void constructorWithShortParam() throws Exception {
        final Constructor<ConstructedService> constructor = getConstructor(ConstructedService.class, short.class);
        final List<Value<Short>> params= new ArrayList<Value<Short>>();
        params.add(new ImmediateValue<Short>((short) 11));
        assertServiceValuesCreated(new ConstructedValue<ConstructedService>(constructor, params), Signature.SHORT);
    }

    @Test
    public void cachedConstructorWithShortParam() throws Exception {
        final Constructor<ConstructedService> constructor = getConstructor(ConstructedService.class, short.class);
        final List<Value<Short>> params= new ArrayList<Value<Short>>();
        params.add(new ImmediateValue<Short>((short) 11));
        assertCachedServiceValueCreated(new CachedValue<ConstructedService>(
                new ConstructedValue<ConstructedService>(constructor, params)), Signature.SHORT);
    }

    @Test
    public void constructorWithStringParam() throws Exception {
        final Constructor<ConstructedService> constructor = getConstructor(ConstructedService.class, String.class);
        final List<Value<String>> params= new ArrayList<Value<String>>();
        params.add(new ImmediateValue<String>("param"));
        assertServiceValuesCreated(new ConstructedValue<ConstructedService>(constructor, params), Signature.STRING);
    }

    @Test
    public void cachedConstructorWithStringParam() throws Exception {
        final Constructor<ConstructedService> constructor = getConstructor(ConstructedService.class, String.class);
        final List<Value<String>> params= new ArrayList<Value<String>>();
        params.add(new ImmediateValue<String>("param"));
        assertCachedServiceValueCreated(new CachedValue<ConstructedService>(
                new ConstructedValue<ConstructedService>(constructor, params)), Signature.STRING);
    }

    @Test
    public void constructorWithStringStringParams() throws Exception {
        final Constructor<ConstructedService> constructor = getConstructor(ConstructedService.class, String.class, String.class);
        final List<Value<String>> params= new ArrayList<Value<String>>();
        params.add(new ImmediateValue<String>("par"));
        params.add(new ImmediateValue<String>("am"));
        assertServiceValuesCreated(new ConstructedValue<ConstructedService>(constructor, params), Signature.STRING_STRING);
    }

    @Test
    public void constructorWithBooleanIntegerParams() throws Exception {
        final Constructor<ConstructedService> constructor = getConstructor(ConstructedService.class, boolean.class, int.class);
        final List<Value<? extends Object>> params= new ArrayList<Value<? extends Object>>();
        params.add(new ImmediateValue<Boolean>(false));
        params.add(new ImmediateValue<Integer>(10));
        assertServiceValuesCreated(new ConstructedValue<ConstructedService>(constructor, params), Signature.BOOLEAN_INTEGER);
    }

    @Test
    public void invalidConstructor() throws Exception {
        final Constructor<AbstractService> constructor = getConstructor(AbstractService.class);
        final Value<AbstractService> value = new ConstructedValue<AbstractService> (constructor, Collections.<Value<AbstractService>>emptyList());
        try {
            value.getValue();
            fail("Expected to have thrown an IllegalStateException");
        } catch (IllegalStateException e) {}
    }

    @Test
    public void nullConstructor() throws Exception {
        final Constructor<ConstructedService> constructor = getConstructor(ConstructedService.class);

        try {
            new ConstructedValue<ConstructedService>(constructor, null);
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException e) {}
    }

    @Test
    public void inaccessibleConstructor() throws Exception {
        final Constructor<ConstructedService> constructor = getConstructor(ConstructedService.class, boolean.class);
        final List<Value<Boolean>> params = new ArrayList<Value<Boolean>>();
        params.add(new ImmediateValue<Boolean>(false));
        final Value<ConstructedService> value = new ConstructedValue<ConstructedService> (constructor, params);
        try {
            value.getValue();
            fail("Expected to have thrown an IllegalStateException");
        } catch (IllegalStateException e) {}
    }

    private final void assertServiceValuesCreated(Value<ConstructedService> value, Signature constructorSignature) {
        final ConstructedService service1 = getValue(value, constructorSignature);
        final ConstructedService service2 = getValue(value, constructorSignature);
        final ConstructedService service3 = getValue(value, constructorSignature);
        assertNotSame(service1, service2);
        assertNotSame(service1, service3);
        assertNotSame(service2, service3);
    }

    private final void assertCachedServiceValueCreated(Value<ConstructedService> value, Signature constructorSignature) {
        final ConstructedService service1 = getValue(value, constructorSignature);
        final ConstructedService service2 = getValue(value, constructorSignature);
        final ConstructedService service3 = getValue(value, constructorSignature);
        assertSame(service1, service2);
        assertSame(service1, service3);
    }

    private final ConstructedService getValue(Value<ConstructedService> value, Signature expectedSignature) {
        final ConstructedService service = value.getValue();
        assertNotNull(service);
        // make sure that the expected constructor was called, by checking the signature of the invoked constructor
        assertSame(expectedSignature, service.getSignature());
        return service;
    }

    private final <T> Constructor<T> getConstructor(Class<T> targetClass, Class<?> ...params) throws Exception {
        return targetClass.getDeclaredConstructor(params);
    }
}