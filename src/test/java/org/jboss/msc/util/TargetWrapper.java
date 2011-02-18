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

package org.jboss.msc.util;

import org.jboss.msc.value.Values;

/**
 * Class for test purposes.
 * 
 * @author <a href="mailto:flavia.rainone@jboss.com">Flavia Rainone</a>
 */
@SuppressWarnings("unchecked")
public class TargetWrapper<I> {

    private I target;
    private final Integer arg;

    public TargetWrapper() {
        this((Integer) null);
    }

    public TargetWrapper(int arg) {
        this(Integer.valueOf(arg));
    }

    public TargetWrapper(boolean throwException) {
        this();
        if (throwException) {
            throw new RuntimeException();
        }
    }

    public TargetWrapper(StringBuffer arg) {
        this.arg = null;
    }

    private TargetWrapper(Integer arg) {
        target = (I) Values.injectedValue().getValue();
        this.arg = arg;
    }

    public I getTarget() {
        return target;
    }

    public Integer getArg() {
        return arg;
    }

    public void readTarget() {
        target = (I) Values.injectedValue().getValue();
        if (target == null) {
            throw new RuntimeException();
        }
    }

    @SuppressWarnings("unused")
    private int readTarget(int maxAttempts) {
        target = (I) Values.injectedValue().getValue();
        return maxAttempts * 2;
    }

    public void readTarget(boolean throwException) {
        if (throwException) {
            throw new RuntimeException();
        }
        target = (I) Values.injectedValue().getValue();
        if (target == null) {
            throw new RuntimeException();
        }
    }
}
