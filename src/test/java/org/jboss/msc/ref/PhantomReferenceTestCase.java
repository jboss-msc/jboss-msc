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

package org.jboss.msc.ref;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import java.lang.ref.ReferenceQueue;
import java.util.ArrayList;
import java.util.Collection;

import org.jboss.msc.ref.Reference.Type;
import org.jboss.msc.ref.util.TestReaper;
import org.jboss.msc.value.util.AnyService;
import org.junit.Test;

/**
 * Test for PhantomReference.
 * 
 * @author <a href="mailto:flavia.rainone@jboss.com">Flavia Rainone</a>
 * @see PhantomReference
 */
public class PhantomReferenceTestCase extends AbstractReapableReferenceTest {

    @Test
    public void plainPhantomReference() {
        final Reference<String, String> reference = new PhantomReference<String, String>("referent", "attachment",
                (ReferenceQueue<String>) null);
        assertReference(reference, null, "attachment", null);
    }

    @Test
    public void nullPhantomReference() throws Exception {
        final Reference<AnyService, Integer> reference = new PhantomReference<AnyService, Integer>(null, 0, new ReferenceQueue<AnyService>());
        assertReference(reference, null, 0, null);
    }

    @Test
    public void phantomReferenceWithReferenceQueue() throws Exception {
        final ReferenceQueue<Collection<Object>> referenceQueue = new ReferenceQueue<Collection<Object>>();
        Collection<Object> collection = new ArrayList<Object>();
        final Reference<Collection<Object>, String> reference = new PhantomReference<Collection<Object>, String>(collection, "collection", referenceQueue);
        assertReference(reference, null, "collection", null);
        collection = null;
        System.gc();
        assertSame(reference, referenceQueue.remove(300));
    }

    @Test
    public void phantomReferenceWithReaper() throws Exception {
        AnyService service = new AnyService();
        final TestReaper<AnyService, Void> reaper = new TestReaper<AnyService, Void>();
        final Reference<AnyService, Void> reference = new PhantomReference<AnyService, Void>(service, null, reaper);
        assertReference(reference, null, null, reaper);
        service = null;
        System.gc();
        assertSame(reference, reaper.getReapedReference());
    }

    @Override @Test
    public void clearReference() {
        final Reference<Boolean, String> reference = createReference(true, "attachment for true");
        assertNull(reference.get());
        assertEquals("attachment for true", reference.getAttachment());

        reference.clear();
        assertNull(reference.get());
        assertEquals("attachment for true", reference.getAttachment());
    }

    @Override
    <T, A> Reference<T, A> createReference(T value, A attachment) {
        return new PhantomReference<T, A>(value, attachment, new TestReaper<T, A>());
    }

    @Override
    Type getTestedReferenceType() {
        return Type.PHANTOM;
    }
}