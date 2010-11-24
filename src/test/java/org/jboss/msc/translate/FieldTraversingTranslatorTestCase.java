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
import static org.junit.Assert.fail;

import java.lang.reflect.Field;

import org.jboss.msc.value.Values;
import org.jboss.msc.value.util.AnyService;
import org.junit.Test;

/**
 * Test for {@link FieldTraversingTranslator}.
 * 
 * @author <a href="mailto:flavia.rainone@jboss.com">Flavia Rainone</a>
 */
public class FieldTraversingTranslatorTestCase {

    @Test
    public void fieldTraversing() throws Exception {
        // create translator that gets the value of count field
        final Field field = AnyService.class.getField("count");
        final Translator<AnyService, Integer> translator = new FieldTraversingTranslator<AnyService, Integer>(Values.immediateValue(field));
        final AnyService anyService = new AnyService();

        // traverse to count field after construction 
        assertEquals(0, (int) translator.translate(anyService));
        anyService.count = 10;
        // traver to count field after it is set to 10
        assertEquals(10, (int) translator.translate(anyService));
    }

    @Test
    public void unaccessibleFieldTraversing() throws Exception {
        // translator that reads the value of sum field
        final Field field = AnyService.class.getDeclaredField("sum");
        final Translator<AnyService, Integer> translator = new FieldTraversingTranslator<AnyService, Integer>(Values.immediateValue(field));
        final AnyService anyService = new AnyService();
        try {
            // field is unaccessible
            translator.translate(anyService);
            fail("TranslationException expected");
        } catch (TranslationException e) {}
    }

    @Test
    public void nullFieldTraversing() throws Exception {
        // traverse to null field
        Translator<AnyService, Integer> translator = new FieldTraversingTranslator<AnyService, Integer>(Values.<Field>nullValue());
        final AnyService anyService = new AnyService();
        try {
            translator.translate(anyService);
            fail("TranslationException expected");
        } catch (TranslationException e) {}

        // traverse to null field value
        translator = new FieldTraversingTranslator<AnyService, Integer>(null);
        try {
            translator.translate(anyService);
            fail("TranslationException expected");
        } catch (TranslationException e) {}

        // traverse to count field 
        translator = new FieldTraversingTranslator<AnyService, Integer>(Values.immediateValue(AnyService.class.getDeclaredField("count")));
        try {
            // with a null target
            translator.translate(null);
            fail("TranslationException expected");
        } catch (TranslationException e) {}
    }
}
