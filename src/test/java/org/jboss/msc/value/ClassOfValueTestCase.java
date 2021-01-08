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

import org.junit.Test;

/**
 * Test for {@link ClassOfValue}.
 * 
 * @author <a href="mailto:flavia.rainone@jboss.com">Flavia Rainone</a>
 */
public class ClassOfValueTestCase {

    @Test
    public void intValue() {
        final Value<Class<? extends Integer>> value = new ClassOfValue<Integer>(new ImmediateValue<Integer>(0));
        assertSame(Integer.class, value.getValue());
    }

    @Test
    public void longValue() {
        final Value<Class<? extends Long>> value = new ClassOfValue<Long>(new ImmediateValue<Long>(11111111111111l));
        assertSame(Long.class, value.getValue());
    }

    @Test
    public void shortValue() {
        final Value<Class<? extends Short>> value = new ClassOfValue<Short>(new ImmediateValue<Short>((short) 1));
        assertSame(Short.class, value.getValue());
    }

    @Test
    public void byteValue() {
        final Value<Class<? extends Byte>> value = new ClassOfValue<Byte>(new ImmediateValue<Byte>((byte) 4));
        assertSame(Byte.class, value.getValue());
    }
    
    @Test
    public void doubleValue() {
        final Value<Class<? extends Double>> value = new ClassOfValue<Double>(new ImmediateValue<Double>(0.4));
        assertSame(Double.class, value.getValue());
    }

    @Test
    public void floatValue() {
        final Value<Class<? extends Float>> value = new ClassOfValue<Float>(new ImmediateValue<Float>(0.4f));
        assertSame(Float.class, value.getValue());
    }

    @Test
    public void booleanValue() {
        final Value<Class<? extends Boolean>> value = new ClassOfValue<Boolean>(new ImmediateValue<Boolean>(true));
        assertSame(Boolean.class, value.getValue());
    }

    @Test
    public void objectValue() {
        final Value<Class<? extends Object>> value = new ClassOfValue<Object>(new ImmediateValue<Object>(new Object()));
        assertSame(Object.class, value.getValue());
    }

    @Test
    public void stringBufferValue() {
        final Value<Class<? extends StringBuffer>> value = new ClassOfValue<StringBuffer>(new ImmediateValue<StringBuffer>(new StringBuffer()));
        assertSame(StringBuffer.class, value.getValue());
    }

    @Test
    public void nullStringBufferValue() {
        final Value<Class<? extends StringBuffer>> value = new ClassOfValue<StringBuffer>(new ImmediateValue<StringBuffer>(null));
        assertNull(value.getValue());
    }

    @Test
    public void nullValue() {
        final Value<?> value = new ClassOfValue<Object>(null);
        try {
            value.getValue();
            fail("NullPointerException expected");
        } catch (NullPointerException e) {}
    }
}
