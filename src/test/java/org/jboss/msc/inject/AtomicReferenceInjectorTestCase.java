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

package org.jboss.msc.inject;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.util.concurrent.atomic.AtomicReference;

import org.junit.Test;

/**
 * Test for {@link AtomicReferenceInjector}.
 * 
 * @author <a href="mailto:flavia.rainone@jboss.com">Flavia Rainone</a>
 */
public class AtomicReferenceInjectorTestCase {

    @Test
    public void atomicReferenceInjection() throws Exception {
        final AtomicReference<String> reference = new AtomicReference<String>("");
        final Injector<String> injector = new AtomicReferenceInjector<String>(reference);
        injector.inject("new value");
        assertEquals("new value", reference.get());
        injector.uninject();
        assertNull(reference.get());

        injector.inject("another value");
        assertEquals("another value", reference.get());

        injector.inject(null);
        assertNull(reference.get());
    }

    @Test
    public void nullAtomicReferenceInjection() throws Exception {
        final Injector<String> injector = new AtomicReferenceInjector<String>(null);
        try {
            injector.inject("new value");
            fail("NullPointerException expected");
        } catch (NullPointerException e) {}
    }
}
