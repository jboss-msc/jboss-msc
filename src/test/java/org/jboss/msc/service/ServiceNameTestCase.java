/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat Middleware LLC, and individual contributors
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

package org.jboss.msc.service;

import org.junit.Test;

import static java.lang.Integer.signum;
import static junit.framework.Assert.assertEquals;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class ServiceNameTestCase {
    @Test
    public void testIdentityCollation() {
        final ServiceName name1 = ServiceName.of("one", "two", "three");
        assertEquals("compareTo identity equality", 0, name1.compareTo(name1));
        assertEquals("compareTo identity equality plus non-identity component", 0, name1.append("xxx").compareTo(name1.append("xxx")));
    }

    @Test
    public void testEvenLengthCollation() {
        assertEquals("compareTo 1-level equality", 0, signum(ServiceName.of("aaaa").compareTo(ServiceName.of("aaaa"))));
        assertEquals("compareTo 2-level equality", 0, signum(ServiceName.of("aaaa", "bbbb").compareTo(ServiceName.of("aaaa", "bbbb"))));
        assertEquals("compareTo 1-level less-than", -1, signum(ServiceName.of("@@@@").compareTo(ServiceName.of("aaaa"))));
        assertEquals("compareTo 1-level greater-than", 1, signum(ServiceName.of("aaaa").compareTo(ServiceName.of("@@@@"))));
        assertEquals("compareTo 2-level less-than (1)", -1, signum(ServiceName.of("xxxx", "@@@@").compareTo(ServiceName.of("xxxx", "aaaa"))));
        assertEquals("compareTo 2-level less-than (2)", -1, signum(ServiceName.of("aaaa", "zzzz").compareTo(ServiceName.of("xxxx", "aaaa"))));
        assertEquals("compareTo 2-level greater-than (1)", 1, signum(ServiceName.of("xxxx", "aaaa").compareTo(ServiceName.of("xxxx", "@@@@"))));
        assertEquals("compareTo 2-level greater-than (2)", 1, signum(ServiceName.of("xxxx", "aaaa").compareTo(ServiceName.of("aaaa", "zzzz"))));
    }

    @Test
    public void testUnevenLengthCollation() {
        assertEquals("compareTo uneven less-than (1)", -1, signum(ServiceName.of("aaaa").compareTo(ServiceName.of("aaaa", "bbbb"))));
        assertEquals("compareTo uneven less-than (2)", -1, signum(ServiceName.of("aaaa").compareTo(ServiceName.of("aaab", "bbbb"))));
        assertEquals("compareTo uneven less-than (3)", -1, signum(ServiceName.of("aaaa", "cccc", "xxxx").compareTo(ServiceName.of("aaab", "bbbb"))));
        assertEquals("compareTo uneven greater-than (1)", 1, signum(ServiceName.of("aaaa", "bbbb").compareTo(ServiceName.of("aaaa"))));
        assertEquals("compareTo uneven greater-than (2)", 1, signum(ServiceName.of("aaab", "bbbb").compareTo(ServiceName.of("aaaa"))));
        assertEquals("compareTo uneven greater-than (3)", 1, signum(ServiceName.of("aaab", "bbbb").compareTo(ServiceName.of("aaaa", "cccc", "xxxx"))));
    }
}
