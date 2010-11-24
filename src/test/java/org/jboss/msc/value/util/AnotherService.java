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

import org.jboss.msc.value.Values;

/**
 * Class for test purposes.
 * 
 * @author <a href="mailto:flavia.rainone@jboss.com">Flavia Rainone</a>
 */
public class AnotherService {
    private int retry;
    private boolean enabled;
    private String definedBy;
    private static String lastDiscovered;

    public AnotherService(int retry, boolean enabled, String definedBy) {
        this.retry = retry;
        this.enabled = enabled;
        this.definedBy = definedBy;
    }

    public int getRetry() {
        return retry;
    }

    public void setRetry(int retry) {
        this.retry = retry;
    }

    public boolean isEnabled() {
        return this.enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    private String getDefinedBy() {
        return definedBy;
    }

    protected void setDefinedBy(String definedBy) {
        this.definedBy = definedBy;
    }
    
    public static String getLastDiscovered() {
        return lastDiscovered;
    }

    public static String discoverDefinedBy() throws Exception {
        lastDiscovered = (new A()).discover();
        return lastDiscovered;
    }
    
    public String discoverDefinedBy(boolean throwException) throws Exception {
        if (throwException) {
            throw new Exception();
        }
        lastDiscovered = (new A()).discover();
        return lastDiscovered;
    }

    private static class A {
        public String discover() {
            return (new B()).discover();
        }
    }

    private static class B {
        public String discover() {
            return C.discover();
        }
    }

    private static class C {
        public static String discover() {
            AnotherService anotherService = (AnotherService) Values.thisValue().getValue();
            return anotherService.getDefinedBy();
        }
    }
}
