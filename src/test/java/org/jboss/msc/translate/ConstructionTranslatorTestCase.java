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

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;

import org.jboss.msc.translate.util.TargetWrapper;
import org.jboss.msc.value.ImmediateValue;
import org.jboss.msc.value.Value;
import org.jboss.msc.value.Values;
import org.junit.Before;
import org.junit.Test;

/**
 * Test for {@link ConstructionTranslator}.
 * 
 * @author <a href="mailto:flavia.rainone@jboss.com">Flavia Rainone</a>
 */
public class ConstructionTranslatorTestCase {

    private List<Value<?>> params = new ArrayList<Value<?>>();

    @Before
    public void initialize() {
        params = new ArrayList<Value<?>>();
    }

    @Test
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void constructionWithStringInput() throws Exception { 
        final Value<Constructor<? extends TargetWrapper>> constructor =
            Values.<Constructor<? extends TargetWrapper>>immediateValue (TargetWrapper.class.getConstructor());
        final Translator<String, TargetWrapper> translator =
                new ConstructionTranslator<String, TargetWrapper>(constructor, params);
        
        // translate; i.e., call traget wrapper constructor
        TargetWrapper<String> translated = (TargetWrapper<String>) translator.translate("input");
        assertEquals("input", translated.getTarget());
        assertNull(translated.getArg());

        // call again
        translated = (TargetWrapper<String>) translator.translate("different input");
        assertEquals("different input", translated.getTarget());
        assertNull(translated.getArg());

        // and again
        translated = (TargetWrapper<String>) translator.translate(null);
        assertNull(translated.getTarget());
        assertNull(translated.getArg());
    }

    @Test
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public void constructionWithObjectInput() throws Exception { 
        final Value<Constructor<? extends TargetWrapper>> constructor =
            Values.<Constructor<? extends TargetWrapper>>immediateValue (TargetWrapper.class.getConstructor(int.class));
        params.add(Values.immediateValue(5));
        Translator<Object, TargetWrapper> translator =
                new ConstructionTranslator<Object, TargetWrapper>(constructor, params);
        // call constructor
        final Object input1 = new Object();
        TargetWrapper<Object> translated = translator.translate(input1);
        assertSame(input1, translated.getTarget());
        assertEquals((Integer) 5, translated.getArg());

        // construct TargetWrapper again
        final StringBuffer input2 = new StringBuffer();
        translated = translator.translate(input2);
        assertSame(input2, translated.getTarget());
        assertEquals((Integer) 5, translated.getArg());

        // one more time
        translated = translator.translate(null);
        assertNull(translated.getTarget());
        assertEquals((Integer) 5, translated.getArg());
    }

    @Test
    @SuppressWarnings({ "rawtypes"})
    public void constructionFails() throws Exception { 
        final Value<Constructor<? extends TargetWrapper>> constructor =
            Values.<Constructor<? extends TargetWrapper>>immediateValue (TargetWrapper.class.getConstructor(boolean.class));
        params.add(new ImmediateValue<Boolean>(true));
        final Translator<Object, TargetWrapper> translator =
                new ConstructionTranslator<Object, TargetWrapper>(constructor, params);
        try {
            // constructor throws an exception
            translator.translate(new Object());
            // that is translated in the form of Translation exception
            fail("TranslationException expected");
        } catch (TranslationException e) {}
    }

    @Test
    @SuppressWarnings({ "rawtypes"})
    public void inexistentConstructionWithObjectInput() throws Exception { 
        final Value<Constructor<? extends TargetWrapper>> constructor = Values.<Constructor<? extends TargetWrapper>>
            immediateValue (TargetWrapper.class.getConstructor(int.class));
        params.add(new ImmediateValue<String>("arg"));
        final Translator<Object, TargetWrapper> translator =
                new ConstructionTranslator<Object, TargetWrapper>(constructor, params);
        try {
            // wrong parameter type
            translator.translate("target");
            fail("TranslationException expected");
        } catch (TranslationException e) {}
    }

    @Test
    @SuppressWarnings({ "rawtypes"})
    public void unaccessibleConstructionWithObjectInput() throws Exception { 
        final Value<Constructor<? extends TargetWrapper>> constructor = Values.<Constructor<? extends TargetWrapper>>
            immediateValue (TargetWrapper.class.getDeclaredConstructor(Integer.class));
        params.add(new ImmediateValue<Integer>(11));
        final Translator<Object, TargetWrapper> translator =
                new ConstructionTranslator<Object, TargetWrapper>(constructor, params);
        try {
            // unaccessible constructor
            translator.translate("target");
            fail("TranslationException expected");
        } catch (TranslationException e) {}
    }

    @Test
    @SuppressWarnings({ "rawtypes"})
    public void nullConstruction() throws Exception {
        // null constructor value
        params.add(new ImmediateValue<Integer>(5));
        Translator<Object, TargetWrapper> translator =
                new ConstructionTranslator<Object, TargetWrapper>(
                        Values.<Constructor<? extends TargetWrapper>>nullValue(), params);
        try {
            translator.translate(new Object());
            fail("TranslationException expected");
        } catch (TranslationException e) {}

        // null parameter
        params.clear();
        params.add(null);
        final Value<Constructor<? extends TargetWrapper>> constructor = new ImmediateValue<Constructor<? extends TargetWrapper>> (TargetWrapper.class.getConstructor(int.class));
        translator = new ConstructionTranslator<Object, TargetWrapper>(constructor, params);
        try {
            translator.translate(new Object());
            fail("TranslationException expected");
        } catch (TranslationException e) {}

        translator = new ConstructionTranslator<Object, TargetWrapper>(
                Values.<Constructor<? extends TargetWrapper>>nullValue(), params);
        try {
            translator.translate(new Object());
            fail("TranslationException expected");
        } catch (TranslationException e) {}

        translator = new ConstructionTranslator<Object, TargetWrapper>(null, params);
        try {
            translator.translate(new Object());
            fail("TranslationException expected");
        } catch (TranslationException e) {}

        translator = new ConstructionTranslator<Object, TargetWrapper>(constructor, null);
        try {
            translator.translate(new Object());
            fail("TranslationException expected");
        } catch (TranslationException e) {}

        translator = new ConstructionTranslator<Object, TargetWrapper>(null, null);
        try {
            translator.translate(new Object());
            fail("TranslationException expected");
        } catch (TranslationException e) {}
    }
}
