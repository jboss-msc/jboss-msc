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

package org.jboss.msc.translate;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import org.jboss.msc.translate.util.TargetWrapper;
import org.jboss.msc.value.ImmediateValue;
import org.jboss.msc.value.Value;
import org.jboss.msc.value.Values;
import org.junit.Before;
import org.junit.Test;

/**
 * Test for {@link MethodTraversingTranslator}.
 * 
 * @author <a href="mailto:flavia.rainone@jboss.com">Flavia Rainone</a>
 */
public class MethodTraversingTranslatorTestCase {

    private List<Value<?>> params = new ArrayList<Value<?>>();

    @Before
    public void initialize() {
        params = new ArrayList<Value<?>>();
    }

    @Test
    @SuppressWarnings({ "rawtypes"})
    public void methodWithCollectionInput() throws Exception {
        // create target wrapper
        final TargetWrapper target = new TargetWrapper(new StringBuffer());
        // method traversing translator that invokes readTarget with empty parameters on target wrapper
        final Value<TargetWrapper> targetValue = Values.immediateValue(target);
        final Value<Method> method = Values.immediateValue (TargetWrapper.class.getMethod("readTarget"));
        final Translator<Collection<?>, Void> translator =
                new MethodTraversingTranslator<Collection<?>, Void>(method, targetValue, params);

        // the input that will be made available during method traversal
        Collection<?> input = new ArrayList<String>();
        // translate
        translator.translate(input);
        // the target input should have been accessed by target wrapper without problems
        assertSame(input, target.getTarget());

        // new input
        input = new HashSet<Integer>();
        translator.translate(input);
        // again, target read the input correctly
        assertSame(input, target.getTarget());

        try {
            // a null input results in a RuntimeException thrown by targetWrapper, that is wrapped in
            // translation exception
            translator.translate(null);
            fail("TranslationException expected");
        } catch (TranslationException e) {}
        assertNull(target.getTarget());
    }

    @Test
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void methodTraversingWithObjectInput() throws Exception {
        // create target wrapper
        final TargetWrapper<Object> target = new TargetWrapper(new StringBuffer());
        // create method traversing translator that invokes readTarget(boolean) on target wrapper
        final Value<TargetWrapper> targetValue = Values.<TargetWrapper>immediateValue(target);
        final Value<Method> method = Values.immediateValue (TargetWrapper.class.getMethod("readTarget", boolean.class));
        params.add(new ImmediateValue<Boolean>(false));
        final Translator<Object, Void> translator =
                new MethodTraversingTranslator<Object, Void>(method, targetValue, params);

        // translate with input
        Object input = new Object();
        translator.translate(input);
        // target should have read the input without problems
        assertSame(input, target.getTarget());
        
        // try with a different input
        input = new StringBuffer();
        translator.translate(input);
        assertSame(input, target.getTarget());

        try {
            // a null input results in a RuntimeException thrown by targetWrapper, that is wrapped in
            // translation exception
            translator.translate(null);
            fail("TranslationException expected");
        } catch (TranslationException e) {}
        assertNull(target.getTarget());
    }

    @Test
    @SuppressWarnings({ "rawtypes"})
    public void methodTraversingFails() throws Exception {
        // create target wrapper
        final TargetWrapper target = new TargetWrapper(new StringBuffer());
        // create a translator that invokes readTarget(true) on targetWrapper
        final Value<TargetWrapper> targetValue = new ImmediateValue<TargetWrapper>(target);
        final Value<Method> method = Values.immediateValue(TargetWrapper.class.getMethod("readTarget", boolean.class));
        params.add(new ImmediateValue<Boolean>(true));
        final Translator<Short, Void> translator =
                new MethodTraversingTranslator<Short, Void>(method, targetValue, params);
        try {
            // the RuntimeException thrown by readTarget is wrapped in a TranslationException
            translator.translate(Short.valueOf((short) 78));
            fail("TranslationException expected");
        } catch (TranslationException e) {}
    }

    @Test
    @SuppressWarnings({ "rawtypes"})
    public void nonexistentMethod() throws Exception {
        // create a translator that invokes the nonexistent method readTarget on a StringBuffer
        final Value<Method> method = Values.immediateValue (TargetWrapper.class.getMethod("readTarget"));
        params.add(new ImmediateValue<String>("arg"));
        final Translator<Object, Void> translator = new MethodTraversingTranslator<Object, Void>(
                method, Values.immediateValue(new TargetWrapper(new StringBuffer())), params);
        try {
            translator.translate("target");
            fail("TranslationException expected");
        } catch (TranslationException e) {}
    }

    @Test
    @SuppressWarnings({ "rawtypes"})
    public void unaccessibleMethodWithByteInput() throws Exception {
        // create target wrapper
        final TargetWrapper target = new TargetWrapper(new StringBuffer());
        // and a traversing method translator that invokes private readTarget(901)
        final Value<TargetWrapper> targetValue = Values.immediateValue(target);
        final Method method = TargetWrapper.class.getDeclaredMethod("readTarget", int.class);
        final Value<Method> methodValue = new ImmediateValue<Method> (method);
        params.add(new ImmediateValue<Integer>(901));
        final Translator<Byte, Integer> translator =
                new MethodTraversingTranslator<Byte, Integer>(methodValue, targetValue, params);
        try {
            translator.translate((byte) 4);
            fail("TranslationException expected");
        } catch (TranslationException e) {}

        // set method accessible
        method.setAccessible(true);
        // now it is possible to invoke the method
        // expected return is 901*2
        assertEquals(1802, (int) translator.translate((byte) 3));
        // target wrapper should have been able of reading input without problems
        assertEquals((byte) 3, target.getTarget());
    }

    @Test
    public void nullMethodTraversing() throws Exception {
        // create target wrapper
        final TargetWrapper<Object> target = new TargetWrapper<Object>(new StringBuffer());
        // create method traversing translator that invokes null method
        final Value<TargetWrapper<?>> targetValue = Values.<TargetWrapper<?>>immediateValue(target);
        params.add(Values.immediateValue(false));
        Translator<Object, Void> translator =
                new MethodTraversingTranslator<Object, Void>(Values.<Method>nullValue(), targetValue, params);
        try {
            translator.translate(new Object());
            fail("TranslationException expected");
        } catch (TranslationException e) {}

        // create translator that invokes readTarget(boolean) on a null target 
        Value<Method> method = Values.immediateValue(TargetWrapper.class.getMethod("readTarget", boolean.class));
        translator = new MethodTraversingTranslator<Object, Void>(method, Values.<TargetWrapper<?>>nullValue(), params);
        try {
            translator.translate(new Object());
            fail("TranslationException expected");
        } catch (TranslationException e) {}

        // creat a translator that invokes null method on a null target
        translator = new MethodTraversingTranslator<Object, Void>(Values.<Method>nullValue(),
                Values.<TargetWrapper<?>>nullValue(), params);
        try {
            translator.translate(new Object());
            fail("TranslationException expected");
        } catch (TranslationException e) {}

        // create a translator that invokes readTarget(boolean) on target wrapper, with a null parameter
        params.clear();
        params.add(null);
        translator = new MethodTraversingTranslator<Object, Void>(method, targetValue, params);
        try {
            translator.translate(new Object());
            fail("TranslationException expected");
        } catch (TranslationException e) {}

        // create a translator that invokes null method on target wrapper, with a null parameter
        translator = new MethodTraversingTranslator<Object, Void>(Values.<Method>nullValue(), targetValue, params);
        try {
            translator.translate(new Object());
            fail("TranslationException expected");
        } catch (TranslationException e) {}

        // create a translator that invokes readTarget(boolean) on a null target with null parameters
        translator = new MethodTraversingTranslator<Object, Void>(method, Values.<TargetWrapper<?>>nullValue(), params);
        try {
            translator.translate(new Object());
            fail("TranslationException expected");
        } catch (TranslationException e) {}

        // create a translator that invokes null method on a null target with a null parameter
        translator = new MethodTraversingTranslator<Object, Void>(Values.<Method>nullValue(),
                Values.<TargetWrapper<?>>nullValue(), params);
        try {
            translator.translate(new Object());
            fail("TranslationException expected");
        } catch (TranslationException e) {}

        // create a translator with a null method value
        params.clear();
        params.add(Values.immediateValue(false));
        translator = new MethodTraversingTranslator<Object, Void>(null, targetValue, params);
        try {
            translator.translate(new Object());
            fail("TranslationException expected");
        } catch (TranslationException e) {}

        // create a translator with a null target value
        translator = new MethodTraversingTranslator<Object, Void>(method, null, params);
        try {
            translator.translate(new Object());
            fail("TranslationException expected");
        } catch (TranslationException e) {}

        // create a translator with a null parameter list
        translator = new MethodTraversingTranslator<Object, Void>(method, targetValue, null);
        try {
            translator.translate(new Object());
            fail("TranslationException expected");
        } catch (TranslationException e) {}

        // create a translator with a null method value and a null target value
        translator = new MethodTraversingTranslator<Object, Void>(null, null, params);
        try {
            translator.translate(new Object());
            fail("TranslationException expected");
        } catch (TranslationException e) {}

        // create a translator with a null method value and a null parameter list
        translator = new MethodTraversingTranslator<Object, Void>(null, targetValue, null);
        try {
            translator.translate(new Object());
            fail("TranslationException expected");
        } catch (TranslationException e) {}

        // create a translator with all parameters null
        translator = new MethodTraversingTranslator<Object, Void>(null, null, null);
        try {
            translator.translate(new Object());
            fail("TranslationException expected");
        } catch (TranslationException e) {}
    }
}