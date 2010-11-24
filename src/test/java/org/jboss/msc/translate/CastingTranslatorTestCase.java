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

import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import org.jboss.msc.value.util.AbstractService;
import org.jboss.msc.value.util.ConstructedService;
import org.junit.Test;

/**
 * Test for {@link CastingTranslator}.
 * 
 * @author <a href="mailto:flavia.rainone@jboss.com">Flavia Rainone</a>
 */
public class CastingTranslatorTestCase {
    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    public void cast1() {
        // cast collection to list
        final CastingTranslator<Collection, List> translator = new CastingTranslator<Collection, List>(List.class);

        Collection<String> collection = new ArrayList<String>();
        List<String> list = translator.translate(collection);
        assertSame(collection, list); // make sure that the value is unchanged

        collection = new LinkedList<String>();
        list = translator.translate(collection);
        assertSame(collection, list); // make sure that the value is unchanged
    }

    @Test
    public void cast2() {
        // cast abstract service to constructed service
        final CastingTranslator<AbstractService, ConstructedService> translator =
                new CastingTranslator<AbstractService, ConstructedService>(ConstructedService.class);
        final AbstractService service = new ConstructedService();
        final ConstructedService result = translator.translate(service);
        assertSame(service, result); // make sure that the value is unchanged
    }

    @Test
    public void illegalCast() {
        // cast object to string
        final CastingTranslator<Object, String> translator = new CastingTranslator<Object, String>(String.class);
        try {
            // illegal cat... ArrayList is not String
            translator.translate(new ArrayList<String>());
            fail("TranslationException expected");
        } catch (TranslationException e) {}
    }

    @Test
    public void nullCast() {
        // cast Object to null!
        final CastingTranslator<Object, String> translator = new CastingTranslator<Object, String>(null);
        try {
            // NPE expected
            translator.translate(new ArrayList<String>());
            fail("NullPointerException expected");
        } catch (NullPointerException e) {}
    }
}
