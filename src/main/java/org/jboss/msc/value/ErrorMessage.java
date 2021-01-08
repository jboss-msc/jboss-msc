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

import java.util.Iterator;
import java.util.List;

/**
 * Utility class that generates a verbose message for error description.
 * 
 * @author <a href="mailto:flavia.rainone@jboss.com">Flavia Rainone</a>
 */
@Deprecated
class ErrorMessage {

    /**
     * Returns a message describing a {@code NoSuchMethodException} that occurred on a method lookup.
     * @param targetClass     the target class of the not found method
     * @param methodName      the name of the not found method
     * @param parameterTypes  parameter list of the not found method
     * @return the error message
     */
    public static String noSuchMethod(Class<?> targetClass, String methodName, final List<? extends Value<Class<?>>> parameterTypes) {
        StringBuffer message = new StringBuffer();
        message.append("No such method '");
        appendParameterList(message, parameterTypes);
        message.append("' found on ").append(targetClass);
        return message.toString();
    }

    /**
     * Returns a message describing a {@code NoSuchMethodException} that occurred on a constructor lookup.
     * 
     * @param targetClass     the target class of the not found constructor
     * @param parameterTypes  parameter list of the not found constructor
     * @return the error message
     */
    public static String noSuchConstructor(Class<?> targetClass, final List<? extends Value<Class<?>>> parameterTypes) {
        StringBuffer message = new StringBuffer();
        message.append("No such constructor found '");
        message.append(targetClass);
        appendParameterList(message, parameterTypes);
        message.append('\'');
        return message.toString();
    }

    /**
     * Appends a list of parameter types to {@code stringBuffer}.
     */
    private static void appendParameterList(StringBuffer stringBuffer, final List<? extends Value<Class<?>>> parameterTypes) {
        stringBuffer.append('(');
        if (parameterTypes != null && !parameterTypes.isEmpty()) {
            Iterator<? extends Value<Class<?>>> iterator = parameterTypes.iterator();
            Value<Class<?>> param = iterator.next();
            Class<?> paramClass = param == null? null: param.getValue();
            stringBuffer.append(paramClass);
            while(iterator.hasNext()) {
                param = iterator.next();
                paramClass = param == null? null: param.getValue();
                stringBuffer.append(", ").append(paramClass);
            }
        }
        stringBuffer.append(')');
    }
}
