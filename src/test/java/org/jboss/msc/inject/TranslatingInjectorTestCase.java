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

package org.jboss.msc.inject;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.lang.reflect.Field;

import org.jboss.msc.translate.FieldTraversingTranslator;
import org.jboss.msc.translate.Translator;
import org.jboss.msc.value.Values;
import org.jboss.msc.value.util.AnotherService;
import org.jboss.msc.value.util.AnyService;
import org.junit.Test;

/**
 * Test for {@TranslatingInjector}.
 * 
 * @author <a href="mailto:flavia.rainone@jboss.com">Flavia Rainone</a>
 */
public class TranslatingInjectorTestCase {

    @Test
    public void translateAndInject() throws Exception {
        final Field field = AnyService.class.getField("count");
        final Translator<AnyService, Integer> translator = new FieldTraversingTranslator<AnyService, Integer>(Values.immediateValue(field));
        final AnotherService service = new AnotherService(5, true, "R & D");
        final Injector<Integer> methodInjector = new SetMethodInjector<Integer>(service, AnotherService.class, "setRetry", int.class);
        final Injector<AnyService> injector = new TranslatingInjector<AnyService, Integer>(translator, methodInjector);
        final AnyService anyService = new AnyService();
        anyService.count = 254001;
        injector.inject(anyService);
        assertEquals(254001, service.getRetry());
        injector.uninject() ;
        assertEquals(254001, service.getRetry());
    }

    @Test
    public void nullTranslateAndInject() throws Exception {
        final Field field = AnyService.class.getField("count");
        final Translator<AnyService, Integer> translator = new FieldTraversingTranslator<AnyService, Integer>(Values.immediateValue(field));
        final AnotherService service = new AnotherService(5, true, "R & D");
        final Injector<Integer> methodInjector = new SetMethodInjector<Integer>(service, AnotherService.class, "setRetry", int.class);

        Injector<AnyService> injector = new TranslatingInjector<AnyService, Integer>(null, methodInjector);
        try {
            injector.inject(new AnyService());
            fail("NullPointerException e");
        } catch (NullPointerException e) {}

        injector = new TranslatingInjector<AnyService, Integer>(translator, null);
        try {
            injector.inject(new AnyService());
            fail("NullPointerException e");
        } catch (NullPointerException e) {}

        injector = new TranslatingInjector<AnyService, Integer>(null, null);
        try {
            injector.inject(new AnyService());
            fail("NullPointerException e");
        } catch (NullPointerException e) {}
    }
}
