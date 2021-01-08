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

package org.jboss.msc.service;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.junit.Before;
import org.junit.Test;

/**
 * Test for {@link NullService}.
 * 
 * @author <a href="mailto:flavia.rainone@jboss.com">Flavia Rainone</a>
 */
public class NullServiceTestCase {

    private Service<Void> service;

    @Before
    public void initialize() {
        service = NullService.INSTANCE;
    }

    @Test
    public void testNullService() {
        assertNull(service.getValue());
        assertNotNull(service.toString());
    }

    @Test
    public void hashCodeAndEquals() {
        assertSame(service.hashCode(), service.hashCode());
        assertFalse(service.equals(new Service<Void>(){

            @Override
            public Void getValue() throws IllegalStateException {
                return null;
            }

            @Override
            public void start(StartContext context) throws StartException {}

            @Override
            public void stop(StopContext context) {}
        }));
        assertTrue(service.equals(service));
        assertFalse(service.equals(true));
    }

    @Test
    public void readWrite() throws Exception {
        final ByteArrayOutputStream byteArrayStream = new ByteArrayOutputStream();
        final ObjectOutputStream output = new ObjectOutputStream(byteArrayStream);
        output.writeObject(service);
        final ObjectInputStream input = new ObjectInputStream(new ByteArrayInputStream(byteArrayStream.toByteArray()));
        @SuppressWarnings("unchecked")
        final Service<Void> readService = (Service<Void>) input.readObject();
        assertNotNull(readService);
        assertSame(service, readService);
    }
}
