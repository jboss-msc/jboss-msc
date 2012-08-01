/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jboss.msc.value;

import java.util.Iterator;
import java.util.List;

/**
 * Utility class that generates a verbose message for error description.
 * 
 * @author <a href="mailto:flavia.rainone@jboss.com">Flavia Rainone</a>
 */
class ErrorMessage {

    private ErrorMessage() {
    }

    /**
     * Returns a message describing a {@code NoSuchMethodException} that occurred on a method lookup.
     *
     * @param targetClass     the target class of the not found method
     * @param parameterTypes  parameter list of the not found method
     * @return the error message
     */
    public static String noSuchMethod(Class<?> targetClass, final List<? extends ReadableValue<Class<?>>> parameterTypes) {
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
    public static String noSuchConstructor(Class<?> targetClass, final List<? extends ReadableValue<Class<?>>> parameterTypes) {
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
    private static void appendParameterList(StringBuffer stringBuffer, final List<? extends ReadableValue<Class<?>>> parameterTypes) {
        stringBuffer.append('(');
        if (parameterTypes != null && !parameterTypes.isEmpty()) {
            Iterator<? extends ReadableValue<Class<?>>> iterator = parameterTypes.iterator();
            ReadableValue<Class<?>> param = iterator.next();
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
