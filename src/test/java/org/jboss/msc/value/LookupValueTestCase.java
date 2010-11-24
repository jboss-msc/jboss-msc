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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.security.AccessControlContext;
import java.security.AccessController;
import java.util.ArrayList;
import java.util.List;

import org.jboss.msc.value.util.AnotherService;
import org.jboss.msc.value.util.AnyService;
import org.jboss.msc.value.util.AnyService.TaskType;
import org.jboss.msc.value.util.ConstructedService;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Test for {@link LookupClassValue}, {@link LookupConstructorValue}, {@link LookupDeclaredConstructorValue}, 
 * {@link LookupMethodValue}, {@link LookupDeclaredMethodValue}, {@link LookupFieldValue}, and
 * {@link LookupDeclaredFieldValue}.
 * 
 * @author <a href="mailto:flavia.rainone@jboss.com">Flavia Rainone</a>
 */
public class LookupValueTestCase {

    private static Value<Class<?>> constructedServiceClass;
    private static Value<Class<?>> anyServiceClass;
    private static Value<Class<?>> anotherServiceClass;
    private static AccessControlContext context;
    private static Value<ClassLoader> classLoader;

    @BeforeClass
    public static void initializeClassValues() {
        constructedServiceClass = new ImmediateValue<Class<?>>(ConstructedService.class);
        anyServiceClass = new ImmediateValue<Class<?>>(AnyService.class);
        anotherServiceClass = new ImmediateValue<Class<?>>(AnotherService.class);
        context = AccessController.getContext();
        classLoader = Values.immediateValue(Thread.currentThread().getContextClassLoader());
    }

    private List<Value<Class<?>>> paramTypes;

    @Before
    public void createParamTypes() {
        paramTypes = new ArrayList<Value<Class<?>>>();
    }

    @Test
    public void classValue() {
        final Value<Class<?>> value = new LookupClassValue("java.lang.String", classLoader);
        assertSame(String.class, value.getValue());
        // access cached class value now
        assertSame(String.class, value.getValue());
    }

    @Test
    public void nullClassLoaderClassValue() {
        final Value<Class<?>> value = new LookupClassValue("java.lang.String", Values.<ClassLoader>nullValue());
        assertSame(String.class, value.getValue());
    }

    @Test
    public void notFoundClassValue() {
        final Value<Class<?>> value = new LookupClassValue("NonExistentClass", classLoader);
        try {
            value.getValue();
            fail("IllegalStateException expected");
        } catch (IllegalStateException e) {};
    }

    @Test
    public void illegalClassValue() {
        try {
            new LookupClassValue(null, classLoader);
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException e) {};

        try {
            new LookupClassValue("java.lang.String", null);
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException e) {};

        try {
            new LookupClassValue(null, null);
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException e) {};
    }

    @Test
    public void defaultConstructorValue() throws Exception {
        final Value<Constructor<?>> value = new LookupConstructorValue(constructedServiceClass, paramTypes);
        final Constructor<?> constructor = value.getValue();
        assertNotNull(constructor);
        assertTrue(constructor.newInstance() instanceof ConstructedService);
    }

    @Test
    public void constructorValue() throws Exception {
        final Value<Class<?>> target = new ImmediateValue<Class<?>>(ConstructedService.class);
        paramTypes.add(new ImmediateValue<Class<?>>(boolean.class));
        paramTypes.add(new ImmediateValue<Class<?>>(int.class));
        final Value<Constructor<?>> value = new LookupConstructorValue(target, paramTypes);
        final Constructor<?> constructor = value.getValue();
        assertNotNull(constructor);
        assertTrue(constructor.newInstance(true, 19) instanceof ConstructedService);
    }

    @Test
    public void inexistentConstructorValue() throws Exception {
        paramTypes.add(new ImmediateValue<Class<?>>(boolean.class));
        paramTypes.add(new ImmediateValue<Class<?>>(boolean.class));
        final Value<Constructor<?>> value = new LookupConstructorValue(constructedServiceClass, paramTypes);
        try {
            value.getValue();
            fail("IllegalStateException expected");
        } catch (IllegalStateException e) {}
    }

    @Test
    public void inaccessibleConstructorValue() throws Exception {
        paramTypes.add(new ImmediateValue<Class<?>>(boolean.class));
        final Value<Constructor<?>> value = new LookupConstructorValue(constructedServiceClass, paramTypes);
        try {
            value.getValue();
            fail("IllegalStateException expected");
        } catch (IllegalStateException e) {}
    }

    @Test
    public void nullConstructorValue() throws Exception {
        Value<Constructor<?>> value = new LookupConstructorValue(Values.<Class<?>>nullValue(), paramTypes);
        try {
            value.getValue();
            fail("NullPointerException expected");
        } catch (NullPointerException e) {};

        paramTypes.add(null);
        value = new LookupConstructorValue(constructedServiceClass, paramTypes);
        try {
            value.getValue();
            fail("NullPointerException expected");
        } catch (NullPointerException e) {};
    }

    @Test
    public void illegalConstructorValue() throws Exception {
        try {
            new LookupConstructorValue(null, paramTypes);
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException e) {};

        try {
            new LookupConstructorValue(constructedServiceClass, null);
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException e) {};

        try {
            new LookupConstructorValue(null, null);
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException e) {};
    }

    @Test
    public void declaredConstructorValue() throws Exception {
        paramTypes.add(new ImmediateValue<Class<?>>(boolean.class));
        paramTypes.add(new ImmediateValue<Class<?>>(int.class));
        final Value<Constructor<?>> value = new LookupDeclaredConstructorValue(constructedServiceClass, paramTypes, context, false);
        final Constructor<?> constructor = value.getValue();
        assertNotNull(constructor);
        assertTrue(constructor.newInstance(true, 19) instanceof ConstructedService);
    }

    @Test
    public void inexistentDeclaredConstructorValue() throws Exception {
        paramTypes.add(new ImmediateValue<Class<?>>(boolean.class));
        paramTypes.add(new ImmediateValue<Class<?>>(boolean.class));
        final Value<Constructor<?>> value = new LookupDeclaredConstructorValue(constructedServiceClass, paramTypes, context, true);
        try {
            value.getValue();
            fail("IllegalStateException expected");
        } catch (IllegalStateException e) {}
    }

    @Test
    public void inaccessibleDeclaredConstructorValue() throws Exception {
        paramTypes.add(new ImmediateValue<Class<?>>(boolean.class));
        final Value<Constructor<?>> value = new LookupDeclaredConstructorValue(constructedServiceClass, paramTypes, context, false);
        final Constructor<?> constructor = value.getValue();
        try {
            constructor.newInstance(false);
            fail("IllegalAccessException expected");
        } catch (IllegalAccessException e) {}
    }

    @Test
    public void inaccessibleDeclaredConstructorValueMadeAccessible() throws Exception {
        paramTypes.add(new ImmediateValue<Class<?>>(boolean.class));
        final Value<Constructor<?>> value = new LookupDeclaredConstructorValue(constructedServiceClass, paramTypes, context, true);
        final Constructor<?> constructor = value.getValue();
        assertTrue(constructor.newInstance(true) instanceof ConstructedService);
    }

    @Test
    public void nullDeclaredConstructorValue() throws Exception {
        Value<Constructor<?>> value = new LookupDeclaredConstructorValue(Values.<Class<?>>nullValue(), paramTypes,
                context, false);
        try {
            value.getValue();
            fail("NullPointerException expected");
        } catch (NullPointerException e) {};

        paramTypes.add(null);
        value = new LookupDeclaredConstructorValue(constructedServiceClass, paramTypes, context, true);
        try {
            value.getValue();
            fail("NullPointerException expected");
        } catch (NullPointerException e) {};
    }

    @Test
    public void illegalDeclaredConstructorValue() throws Exception {
        try {
            new LookupDeclaredConstructorValue(null, paramTypes, context, false);
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException e) {};

        try {
            new LookupDeclaredConstructorValue(constructedServiceClass, null, context, true);
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException e) {};

        try {
            new LookupDeclaredConstructorValue(constructedServiceClass, paramTypes, null, true);
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException e) {};

        try {
            new LookupDeclaredConstructorValue(null, null, context, false);
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException e) {};

        try {
            new LookupDeclaredConstructorValue(null, paramTypes, null, false);
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException e) {};

        try {
            new LookupDeclaredConstructorValue(constructedServiceClass, null, null, true);
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException e) {};

        try {
            new LookupDeclaredConstructorValue(null, null, null, false);
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException e) {};
    }

    @Test
    public void methodValue() throws Exception {
        Value<Method> value = new LookupMethodValue(anyServiceClass, "executeMainTask", paramTypes);
        final Method resultingMethod = value.getValue();
        assertNotNull(resultingMethod);
        final AnyService anyService = new AnyService();
        resultingMethod.invoke(anyService);
        assertSame(TaskType.MAIN, anyService.getExecutedTask());

        value = new LookupMethodValue(anyServiceClass, "executeMainTask", 0);
        assertEquals(resultingMethod, value.getValue());
    }

    @Test
    public void methodWithParamsValue() throws Exception {
        paramTypes.add(Values.<Class<?>>immediateValue(String.class));
        Value<Method> value = new LookupMethodValue(anyServiceClass, "executeNonImportantTask", paramTypes);
        final Method resultingMethod = value.getValue();
        assertNotNull(resultingMethod);
        final AnyService anyService = new AnyService();
        resultingMethod.invoke(anyService, "NonImportantTask");
        assertSame(TaskType.NON_IMPORTANT, anyService.getExecutedTask());

        value = new LookupMethodValue(anyServiceClass, "executeNonImportantTask", 1);
        assertEquals(resultingMethod, value.getValue());
    }

    @Test
    public void overloadedMethodValue1() throws Exception {
        paramTypes.add(Values.<Class<?>>immediateValue(int.class));
        paramTypes.add(Values.<Class<?>>immediateValue(boolean.class));
        Value<Method> value = new LookupMethodValue(anyServiceClass, "executeSecondaryTask", paramTypes);
        final Method resultingMethod = value.getValue();
        assertNotNull(resultingMethod);
        final AnyService anyService = new AnyService();
        resultingMethod.invoke(anyService, 4, false);
        assertSame(TaskType.SECONDARY, anyService.getExecutedTask());

        value = new LookupMethodValue(anyServiceClass, "executeSecondaryTask", 2);
        assertEquals(resultingMethod, value.getValue());
    }

    @Test
    public void overloadedMethodValue2() throws Exception {
        paramTypes.add(Values.<Class<?>>immediateValue(boolean.class));
        Value<Method> value = new LookupMethodValue(anyServiceClass, "executeSecondaryTask", paramTypes);
        final Method booleanParamMethod = value.getValue();
        assertNotNull(booleanParamMethod);
        final AnyService anyService = new AnyService();
        booleanParamMethod.invoke(anyService, true);
        assertSame(TaskType.SECONDARY, anyService.getExecutedTask());

        paramTypes.clear();
        paramTypes.add(Values.<Class<?>>immediateValue(String.class));
        value = new LookupMethodValue(anyServiceClass, "executeSecondaryTask", paramTypes);
        final Method stringParamMethod = value.getValue();
        assertNotNull(stringParamMethod);
        stringParamMethod.invoke(anyService, "SecondaryTask");
        assertSame(TaskType.SECONDARY, anyService.getExecutedTask());

        value = new LookupMethodValue(anyServiceClass, "executeSecondaryTask", 1);
        assertTrue(value.getValue().equals(booleanParamMethod) || value.getValue().equals(stringParamMethod));
    }

    @Test
    public void inaccessibleMethodValue() throws Exception {
        paramTypes.add(Values.<Class<?>>immediateValue(boolean.class));
        Value<Method> value = new LookupMethodValue(anyServiceClass, "executeTask", paramTypes);
        try {
            value.getValue();
            fail("IllegalStateException expected");
        } catch (IllegalStateException e) {}

        value = new LookupMethodValue(anyServiceClass, "executeTask", 1);
        try {
            value.getValue();
            fail("IllegalStateException expected");
        } catch (IllegalStateException e) {}
    }

    @Test
    public void nonexistentMethodValue() throws Exception {
        Value<Method> value = new LookupMethodValue(anyServiceClass, "executeNothing", paramTypes);
        try {
            value.getValue();
            fail("IllegalStateException expected");
        } catch (IllegalStateException e) {}

        value = new LookupMethodValue(anyServiceClass, "executeNothing", 9);
        try {
            value.getValue();
            fail("IllegalStateException expected");
        } catch (IllegalStateException e) {}
    }

    @Test
    public void invalidMethodValue() throws Exception {
        paramTypes.add(null);
        paramTypes.add(null);
        final Value<Method> value = new LookupMethodValue(anyServiceClass, "executeMainTask", paramTypes);
        try {
            value.getValue();
            fail("NullPointerException expected");
        } catch (NullPointerException e) {}
    }

    @Test
    public void nullMethodValue() throws Exception {
        Value<Method> value = new LookupMethodValue(Values.<Class<?>>nullValue(), "executeSecondaryTask", paramTypes);
        try {
            value.getValue();
            fail("NullPointerException expected");
        } catch (NullPointerException e) {}

        paramTypes.add(Values.<Class<?>>immediateValue(int.class));
        paramTypes.add(Values.<Class<?>>nullValue());
        value = new LookupMethodValue(anyServiceClass, "executeSecondaryTask", paramTypes);
        try {
            value.getValue();
            fail("IllegalStateException expected");
        } catch (IllegalStateException e) {}

        value = new LookupMethodValue(Values.<Class<?>>nullValue(), "executeSecondaryTask", paramTypes);
        try {
            value.getValue();
            fail("NullPointerException expected");
        } catch (NullPointerException e) {}
    }

    @Test
    public void illegalMethodValue() throws Exception {
        try {
            new LookupMethodValue(null, "executeSecondaryTask", paramTypes);
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException e) {}

        try {
            new LookupMethodValue(anyServiceClass, null, paramTypes);
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException e) {}

        try {
            new LookupMethodValue(anyServiceClass, "executeSecondaryTask", null);
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException e) {}

        try {
            new LookupMethodValue(null, null, paramTypes);
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException e) {}

        try {
            new LookupMethodValue(null, "executeSecondaryTask", null);
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException e) {}

        try {
            new LookupMethodValue(anyServiceClass, null, null);
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException e) {}

        try {
            new LookupMethodValue(null, null, null);
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException e) {}

        try {
            new LookupMethodValue(null, "executeSecondaryTask", 0);
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException e) {}

        try {
            new LookupMethodValue(anyServiceClass, null, 0);
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException e) {}

        try {
            new LookupMethodValue(null, null, 0);
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException e) {}
    }

    @Test
    public void declaredMethodValue() throws Exception {
        Value<Method> value = new LookupDeclaredMethodValue(anyServiceClass, "executeMainTask", paramTypes, context, false);
        final Method resultingMethod = value.getValue();
        assertNotNull(resultingMethod);
        final AnyService anyService = new AnyService();
        resultingMethod.invoke(anyService);
        assertSame(TaskType.MAIN, anyService.getExecutedTask());

        value = new LookupDeclaredMethodValue(anyServiceClass, "executeMainTask", 0, context, false);
        assertEquals(resultingMethod, value.getValue());
    }

    @Test
    public void declaredMethodWithParamsValue() throws Exception {
        paramTypes.add(Values.<Class<?>>immediateValue(String.class));
        Value<Method> value = new LookupDeclaredMethodValue(anyServiceClass, "executeNonImportantTask", paramTypes, context, true);
        final Method resultingMethod = value.getValue();
        assertNotNull(resultingMethod);
        final AnyService anyService = new AnyService();
        resultingMethod.invoke(anyService, "NonImportantTask");
        assertSame(TaskType.NON_IMPORTANT, anyService.getExecutedTask());

        value = new LookupDeclaredMethodValue(anyServiceClass, "executeNonImportantTask", 1, context, true);
    }

    @Test
    public void overloadedDeclaredMethodValue1() throws Exception {
        paramTypes.add(Values.<Class<?>>immediateValue(int.class));
        paramTypes.add(Values.<Class<?>>immediateValue(boolean.class));
        Value<Method> value = new LookupDeclaredMethodValue(anyServiceClass, "executeSecondaryTask", paramTypes, context, false);
        final Method resultingMethod = value.getValue();
        assertNotNull(resultingMethod);
        final AnyService anyService = new AnyService();
        resultingMethod.invoke(anyService, 4, false);
        assertSame(TaskType.SECONDARY, anyService.getExecutedTask());

        value = new LookupDeclaredMethodValue(anyServiceClass, "executeSecondaryTask", 2, context, false);
        assertEquals(resultingMethod, value.getValue());
    }

    @Test
    public void overloadedDeclaredMethodValue2() throws Exception {
        paramTypes.add(Values.<Class<?>>immediateValue(boolean.class));
        Value<Method> value = new LookupDeclaredMethodValue(anyServiceClass, "executeSecondaryTask", paramTypes, context, true);
        final Method booleanParamMethod = value.getValue();
        assertNotNull(booleanParamMethod);
        final AnyService anyService = new AnyService();
        booleanParamMethod.invoke(anyService, true);
        assertSame(TaskType.SECONDARY, anyService.getExecutedTask());

        paramTypes.clear();
        paramTypes.add(Values.<Class<?>>immediateValue(String.class));
        value = new LookupDeclaredMethodValue(anyServiceClass, "executeSecondaryTask", paramTypes, context, false);
        final Method stringParamMethod = value.getValue();
        assertNotNull(stringParamMethod);
        stringParamMethod.invoke(anyService, "SecondaryTask");
        assertSame(TaskType.SECONDARY, anyService.getExecutedTask());

        assertFalse(booleanParamMethod.equals(stringParamMethod));

        value = new LookupDeclaredMethodValue(anyServiceClass, "executeSecondaryTask", 1, context, true);
        assertTrue(value.getValue().equals(booleanParamMethod) || value.getValue().equals(stringParamMethod));
    }

    @Test
    public void inaccessibleDeclaredMethodValue() throws Exception {
        paramTypes.add(Values.<Class<?>>immediateValue(boolean.class));
        Value<Method> value = new LookupDeclaredMethodValue(anyServiceClass, "executeTask", paramTypes, context, false);
        Method resultingMethod = value.getValue();
        final AnyService anyService = new AnyService();
        try {
            resultingMethod.invoke(anyService, true);
            fail("IllegalAccessException expected");
        } catch (IllegalAccessException e) {}

        value = new LookupDeclaredMethodValue(anyServiceClass, "executeTask", paramTypes, context, true);
        assertEquals(resultingMethod, value.getValue());
        resultingMethod = value.getValue();
        resultingMethod.invoke(anyService, true);
        assertSame(TaskType.PRIVATE_DAEMON, anyService.getExecutedTask());
        resultingMethod.invoke(anyService, false);
        assertSame(TaskType.PRIVATE, anyService.getExecutedTask());
    }

    @Test
    public void nonexistentDeclaredMethodValue() throws Exception {
        Value<Method> value = new LookupDeclaredMethodValue(anyServiceClass, "executeNothing", paramTypes, context, true);
        try {
            value.getValue();
            fail("IllegalStateException expected");
        } catch (IllegalStateException e) {}

        value = new LookupDeclaredMethodValue(anyServiceClass, "executeNothing", 0, context, true);
        try {
            value.getValue();
            fail("IllegalStateException expected");
        } catch (IllegalStateException e) {}
    }

    @Test
    public void nullDeclaredMethodValue() throws Exception {
        Value<Method> value = new LookupDeclaredMethodValue(Values.<Class<?>>nullValue(), "executeSecondaryTask", paramTypes, context, false);
        try {
            value.getValue();
            fail("NullPointerException expected");
        } catch (NullPointerException e) {}

        value = new LookupDeclaredMethodValue(Values.<Class<?>>nullValue(), "executeSecondaryTask", 0, context, false);
        try {
            value.getValue();
            fail("NullPointerException expected");
        } catch (NullPointerException e) {}

        paramTypes.add(Values.<Class<?>>immediateValue(int.class));
        paramTypes.add(Values.<Class<?>>nullValue());
        value = new LookupDeclaredMethodValue(anyServiceClass, "executeSecondaryTask", paramTypes, context, true);
        try {
            value.getValue();
            fail("IllegalStateException expected");
        } catch (IllegalStateException e) {}

        value = new LookupDeclaredMethodValue(Values.<Class<?>>nullValue(), "executeSecondaryTask", paramTypes, context, true);
        try {
            value.getValue();
            fail("NullPointerException expected");
        } catch (NullPointerException e) {}
    }

    @Test
    public void illegalDeclaredMethodValue() throws Exception {
        try {
            new LookupDeclaredMethodValue(anyServiceClass, "executeSecondaryTask", paramTypes, null, false);
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException e) {}

        try {
            new LookupDeclaredMethodValue(null, "executeSecondaryTask", paramTypes, context, false);
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException e) {}

        try {
            new LookupDeclaredMethodValue(null, "executeSecondaryTask", paramTypes, null, false);
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException e) {}

        try {
            new LookupDeclaredMethodValue(anyServiceClass, null, paramTypes, context, false);
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException e) {}

        try {
            new LookupDeclaredMethodValue(anyServiceClass, null, paramTypes, null, false);
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException e) {}

        try {
            new LookupDeclaredMethodValue(anyServiceClass, "executeSecondaryTask", null, context, false);
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException e) {}

        try {
            new LookupDeclaredMethodValue(anyServiceClass, "executeSecondaryTask", null, null, false);
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException e) {}

        try {
            new LookupDeclaredMethodValue(null, null, paramTypes, context, false);
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException e) {}

        try {
            new LookupDeclaredMethodValue(null, null, paramTypes, null, false);
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException e) {}

        try {
            new LookupDeclaredMethodValue(null, null, paramTypes, context, false);
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException e) {}

        try {
            new LookupDeclaredMethodValue(null, null, paramTypes, null, false);
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException e) {}

        try {
            new LookupDeclaredMethodValue(anyServiceClass, null, null, context, false);
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException e) {}

        try {
            new LookupDeclaredMethodValue(anyServiceClass, null, null, null, false);
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException e) {}

        try {
            new LookupDeclaredMethodValue(null, null, null, context, false);
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException e) {}

        try {
            new LookupDeclaredMethodValue(null, null, null, null, false);
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException e) {}

        try {
            new LookupDeclaredMethodValue(anyServiceClass, "executeSecondaryTask", 0, null, false);
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException e) {}

        try {
            new LookupDeclaredMethodValue(null, "executeSecondaryTask", 0, context, false);
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException e) {}

        try {
            new LookupDeclaredMethodValue(null, "executeSecondaryTask", 0, null, false);
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException e) {}

        try {
            new LookupDeclaredMethodValue(anyServiceClass, null, 0, context, false);
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException e) {}

        try {
            new LookupDeclaredMethodValue(anyServiceClass, null, 0, null, false);
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException e) {}

        try {
            new LookupDeclaredMethodValue(null, null, 0, context, false);
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException e) {}

        try {
            new LookupDeclaredMethodValue(null, null, 0, null, false);
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException e) {}

        try {
            new LookupDeclaredMethodValue(null, null, 0, context, false);
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException e) {}

        try {
            new LookupDeclaredMethodValue(null, null, 0, null, false);
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException e) {}
    }

    @Test
    public void getMethodValue() throws Exception {
        final Value<Method> value = new LookupGetMethodValue(anotherServiceClass, "retry");
        final Method resultingMethod = value.getValue();
        final AnotherService service = new AnotherService(3, false, "Red Hat");
        assertEquals(3, resultingMethod.invoke(service));
    }

    @Test
    public void isMethodValue() throws Exception {
        final Value<Method> value = new LookupGetMethodValue(anotherServiceClass, "enabled");
        final Method resultingMethod = value.getValue();
        final AnotherService service = new AnotherService(0, true, "Red Hat");
        assertTrue((Boolean) resultingMethod.invoke(service));
    }

    @Test
    public void privateGetMethodValue() throws Exception {
        final Value<Method> value = new LookupGetMethodValue(anotherServiceClass, "definedBy");
        try {
            value.getValue();
            fail("IllegalStateException expected");
        } catch (IllegalStateException e) {}
    }

    @Test
    public void nonExistentGetMethodValue() throws Exception {
        final Value<Method> value = new LookupGetMethodValue(anotherServiceClass, "nonexistent");
        try {
            value.getValue();
            fail("IllegalStateException expected");
        } catch (IllegalStateException e) {}
    }

    @Test
    public void nullClassGetMethodValue() throws Exception {
        final Value<Method> value = new LookupGetMethodValue(Values.<Class<?>>nullValue(), "nonexistent");
        try {
            value.getValue();
            fail("NullPointerException expected");
        } catch (NullPointerException e) {}
    }

    @Test
    public void illegalGetMethodValue() throws Exception {
        try {
            new LookupGetMethodValue(anotherServiceClass, null);
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException e) {}

        try {
            new LookupGetMethodValue(null, "retry");
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException e) {}

        try {
            new LookupGetMethodValue(null, null);
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException e) {}
    }

    @Test
    public void setMethodValue1() throws Exception {
        final Value<Method> value = new LookupSetMethodValue(anotherServiceClass, "retry");
        final Method resultingMethod = value.getValue();
        final AnotherService service = new AnotherService(1, true, "JBoss");
        resultingMethod.invoke(service, 2);
        assertEquals(2, service.getRetry());
        resultingMethod.invoke(service, 0);
        assertEquals(0, service.getRetry());
    }

    @Test
    public void setMethodValue2() throws Exception {
        final Value<Method> value = new LookupSetMethodValue(anotherServiceClass, "enabled");
        final Method resultingMethod = value.getValue();
        final AnotherService service = new AnotherService(0, true, "Red Hat");
        resultingMethod.invoke(service, false);
        assertFalse(service.isEnabled());
        resultingMethod.invoke(service, true);
        assertTrue(service.isEnabled());
    }

    @Test
    public void protectedSetMethodValue() throws Exception {
        final Value<Method> value = new LookupSetMethodValue(anotherServiceClass, "definedBy");
        try {
            value.getValue();
            fail("IllegalStateException expected");
        } catch (IllegalStateException e) {}
    }

    @Test
    public void nonExistentSetMethodValue() throws Exception {
        final Value<Method> value = new LookupSetMethodValue(anotherServiceClass, "nonexistent");
        try {
            value.getValue();
            fail("IllegalStateException expected");
        } catch (IllegalStateException e) {}
    }

    @Test
    public void nullClassSetMethodValue() throws Exception {
        final Value<Method> value = new LookupSetMethodValue(Values.<Class<?>>nullValue(), "nonexistent");
        try {
            value.getValue();
            fail("NullPointerException expected");
        } catch (NullPointerException e) {}
    }

    @Test
    public void illegalSetMethodValue() throws Exception {
        try {
            new LookupSetMethodValue(anotherServiceClass, null);
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException e) {}

        try {
            new LookupSetMethodValue(null, "retry");
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException e) {}

        try {
            new LookupSetMethodValue(null, null);
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException e) {}
    }

    @Test
    public void fieldValue() throws Exception {
        final Value<Field> value = new LookupFieldValue(anyServiceClass, "count");
        final Field resultingField = value.getValue();
        final AnyService anyService = new AnyService();
        anyService.count++;
        assertEquals(1, resultingField.get(anyService));
        assertEquals(++anyService.count, resultingField.get(anyService));
    }

    @Test
    public void privateFieldValue() throws Exception {
        final Value<Field> value = new LookupFieldValue(anyServiceClass, "sum");
        try {
            value.getValue();
            fail("IllegalStateException expected");
        } catch (IllegalStateException e) {}
    }

    @Test
    public void nonexistentFieldValue() throws Exception {
        final Value<Field> value = new LookupFieldValue(anyServiceClass, "id");
        try {
            value.getValue();
            fail("IllegalStateException expected");
        } catch (IllegalStateException e) {}
    }

    @Test
    public void nullClassFieldValue() throws Exception {
        final Value<Field> value = new LookupFieldValue(Values.<Class<?>>nullValue(), "id");
        try {
            value.getValue();
            fail("NullPointerException expected");
        } catch (NullPointerException e) {}
    }

    @Test
    public void illegalFieldValue() throws Exception {
        try {
            new LookupFieldValue(anyServiceClass, null);
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException e) {}

        try {
            new LookupFieldValue(null, "sum");
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException e) {}
        
        try {
            new LookupFieldValue(null, null);
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException e) {}
    }

    @Test
    public void declaredFieldValue() throws Exception {
        final Value<Field> value = new LookupDeclaredFieldValue(anyServiceClass, "count", context, false);
        final Field resultingField = value.getValue();
        final AnyService anyService = new AnyService();
        anyService.count++;
        assertEquals(1, resultingField.get(anyService));
        assertEquals(++anyService.count, resultingField.get(anyService));
    }

    @Test
    public void privateDeclaredFieldValue() throws Exception {
        Value<Field> value = new LookupDeclaredFieldValue(anyServiceClass, "sum", context, false);
        final Field inaccessibleField = value.getValue();
        final AnyService anyService = new AnyService();
        try {
            inaccessibleField.get(anyService);
            fail("IllegalAccessException expected");
        } catch (IllegalAccessException e) {}

        value = new LookupDeclaredFieldValue(anyServiceClass, "sum", context, true);
        Field accessibleField = value.getValue();
        assertEquals(inaccessibleField, accessibleField);
        assertEquals(0, accessibleField.get(anyService));
    }

    @Test
    public void nonexistentDeclaredFieldValue() throws Exception {
        final Value<Field> value = new LookupDeclaredFieldValue(anyServiceClass, "id", context, true);
        try {
            value.getValue();
            fail("IllegalStateException expected");
        } catch (IllegalStateException e) {}
    }

    @Test
    public void nullClassDeclaredFieldValue() throws Exception {
        final Value<Field> value = new LookupDeclaredFieldValue(Values.<Class<?>>nullValue(), "id", context, false);
        try {
            value.getValue();
            fail("NullPointerException expected");
        } catch (NullPointerException e) {}
    }

    @Test
    public void illegalDeclaredFieldValue() throws Exception {
        try {
            new LookupDeclaredFieldValue(anyServiceClass, null, context, false);
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException e) {}

        try {
            new LookupDeclaredFieldValue(null, "sum", context, true);
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException e) {}

        try {
            new LookupDeclaredFieldValue(anyServiceClass, "sum", null, true);
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException e) {}

        try {
            new LookupDeclaredFieldValue(null, null, context, true);
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException e) {}

        try {
            new LookupDeclaredFieldValue(anyServiceClass, null, null, false);
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException e) {}

        try {
            new LookupDeclaredFieldValue(null, "sum", null, true);
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException e) {}

        try {
            new LookupDeclaredFieldValue(null, null, null, true);
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException e) {}

    }
}