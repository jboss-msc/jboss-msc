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

package org.jboss.msc.value.util;

/**
 * Class for test purposes.
 * 
 * @author <a href="mailto:flavia.rainone@jboss.com">Flavia Rainone</a>
 */
public final class ConstructedService extends AbstractService {

    public enum Signature {DEFAULT, INTEGER, SHORT, STRING_STRING, STRING, BOOLEAN_INTEGER, BOOLEAN};

    private final Signature signature;

    public ConstructedService() {
        signature = Signature.DEFAULT;
    }

    public ConstructedService(int param) throws Exception {
        if (param == 0) {
            throw new Exception("Param is 0");
        }
        signature = Signature.INTEGER;
    }

    public ConstructedService(short param) {
        signature = Signature.SHORT;
    }

    public ConstructedService(String param1, String param2) {
        signature = Signature.STRING_STRING;
    }

    public ConstructedService(String param) {
        signature = Signature.STRING;
    }

    public ConstructedService(boolean param1, int param2) {
        signature = Signature.BOOLEAN_INTEGER;
    }

    @SuppressWarnings("unused")
    private ConstructedService(boolean param) {
        signature = Signature.BOOLEAN;
    }

    public Signature getSignature() {
        return signature;
    }
}
