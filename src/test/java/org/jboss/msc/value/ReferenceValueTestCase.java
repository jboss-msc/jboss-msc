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

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;

import java.lang.ref.ReferenceQueue;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jboss.modules.ref.Reference;
import org.jboss.modules.ref.Reference.Type;
import org.jboss.modules.ref.References;
import org.junit.Test;

/**
 * Test for {@link ReferenceValue}.
 * 
 * @author <a href="mailto:flavia.rainone@jboss.com">Flavia Rainone</a>
 */
public class ReferenceValueTestCase {

    @Test
    public void strongReferenceValue() {
        final Object referent = new Object();
        final Reference<Object, Void> reference = References.create(Type.STRONG, referent, null);
        final Value<Object> value = new ReferenceValue<Object>(reference);
        assertSame(referent, value.getValue());
    }

    @Test
    public void softReferenceValue() throws Exception {
        final List<String> referent = new ArrayList<String>();
        final ReferenceQueue<List<String>> queue = new ReferenceQueue<List<String>>();
        final Reference<List<String>, String> reference = References.create(Type.SOFT, referent, "attachment", queue);
        final Value<List<String>> value = new ReferenceValue<List<String>>(reference);
        assertSame(referent, value.getValue());
    }

    @Test
    public void weakReferenceValue() throws Exception {
        Object referent = new Object();
        final ReferenceQueue<Object> queue = new ReferenceQueue<Object>();
        final Reference<Object, String> reference = References.create(Type.WEAK, referent, "attachment", queue);
        final Value<Object> value = new ReferenceValue<Object>(reference);
        assertSame(referent, value.getValue());
        referent = null;
        System.gc();
        assertSame(reference, queue.remove());
        assertNull(value.getValue());
    }

    @Test
    public void phantomReferenceValue() throws Exception {
        Map<String, String> referent = new HashMap<String, String>();
        referent.put("key", "value");
        final ReferenceQueue<Object> queue = new ReferenceQueue<Object>();
        final Reference<Map<String, String>, Void> reference = References.create(Type.PHANTOM, referent, null, queue);
        final Value<Map<String, String>> value = new ReferenceValue<Map<String, String>>(reference);
        assertNull(value.getValue());
        referent = null;
        System.gc();
        assertSame(reference, queue.remove());
        assertNull(value.getValue());
    }

    @Test
    public void nullReferenceValue() {
        final Value<?> value = new ReferenceValue<Object>(null);
        try {
            value.getValue();
            fail ("NullPointerException expected");
        } catch (NullPointerException e) {}
    }
}
