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

import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.regex.Pattern;

/**
 * Service name class.
 *
 * @author John Bailey
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class ServiceName implements Comparable<ServiceName>, Serializable {

    private static final long serialVersionUID = 2336190201880964151L;

    private final String name;
    private final ServiceName parent;
    private transient final int hashCode;

    /**
     * The root name "jboss".
     */
    public static final ServiceName JBOSS = new ServiceName(null, "jboss");

    /**
     * Create a ServiceName from a series of String parts.
     *
     * @param parts The string representations of the service name segments
     * @return A ServiceName instance 
     */
    public static ServiceName of(final String... parts) {
        return of(null, parts);
    }

    /**
     * Create a ServiceName from a series of String parts and a parent service name.
     *
     * @param parent The parent ServiceName for this name
     * @param parts The string representations of the service name segments
     * @return A ServiceName instance
     */
    public static ServiceName of(final ServiceName parent, String... parts) {
        if(parts.length < 1)
            throw new IllegalArgumentException("Must provide at least one name segment");
        
        ServiceName current = parent;
        for(String part : parts)
            current = new ServiceName(current, part);
        return current;
    }

    private ServiceName(final ServiceName parent, final String name) {
        if (name == null) {
            throw new IllegalArgumentException("name is null");
        }
        this.name = name;
        this.parent = parent;

        hashCode = calculateHashCode(parent, name);
    }

    private static int calculateHashCode(final ServiceName parent, final String name) {
        int result = parent == null ? 1 : parent.hashCode();
        result = 31 * result + name.hashCode();
        return result;
    }

    /**
     * Create a service name by appending name parts using this as a parent to the new ServiceName.
     *
     * @param parts The parts to append
     * @return A new ServiceName
     */
    public ServiceName append(final String... parts) {
        return of(this, parts);
    }

    /**
     * Create a service name by appending name parts of the provided ServiceName using this as a parent.
     *
     * @param serviceName The service name to use as the parts to append
     * @return A new ServiceName
     */
    public ServiceName append(final ServiceName serviceName) {
        if (serviceName.parent == null) {
            return append(serviceName.name);
        } else {
            return append(serviceName.parent).append(serviceName.name);
        }
    }

    /**
     * Get the length (in segments) of this service name.
     *
     * @return the length
     */
    public int length() {
        final ServiceName parent = this.parent;
        return parent == null ? 1 : 1 + parent.length();
    }

    /**
     * Get the parent (enclosing) service name.
     *
     * @return the parent name
     */
    public ServiceName getParent() {
        return parent;
    }

    /**
     * Get the simple (unqualified) name of this service.
     *
     * @return the simple name
     */
    public String getSimpleName() {
        return name;
    }

    /**
     * Compare this service name to another service name.  This is done by comparing the parents and leaf name of
     * each service name.
     *
     * @param o the other service name
     * @return {@code true} if they are equal, {@code false} if they are not equal or the argument is not a service name or is {@code null}
     */
    @Override
    public boolean equals(Object o) {
        return this == o || o instanceof ServiceName && equals((ServiceName)o);
    }

    /**
     * Compare this service name to another service name.  This is done by comparing the parents and leaf name of
     * each service name.
     *
     * @param o the other service name
     * @return {@code true} if they are equal, {@code false} if they are not equal or the argument is {@code null}
     */
    public boolean equals(ServiceName o) {
        if (o == this) {
            return true;
        }
        if (o == null || hashCode != o.hashCode || ! name.equals(o.name)) {
            return false;
        }

        final ServiceName parent = this.parent;
        final ServiceName oparent = o.parent;
        return parent != null && parent.equals(oparent) || oparent == null;
    }

    /**
     * Return the hash code of this service name.
     *
     * @return the hash code
     */
    @Override
    public int hashCode() {
        return hashCode;
    }

    /**
     * Get a string representation of this service name.
     *
     * @return the string representation
     */
    @Override
    public String toString() {
        return toString(new StringBuilder()).toString();
    }

    private static final Pattern SIMPLE_NAME = Pattern.compile("^[-_a-z@-Z0-9!#$%^&*()+=\\[\\]{}|/?<>,;:~]+$");

    private StringBuilder toString(StringBuilder target) {
        final ServiceName parent = this.parent;
        if (parent != null) {
            parent.toString(target);
            target.append('.');
        }
        final String name = this.name;
        if (SIMPLE_NAME.matcher(name).matches()) {
            target.append(name);
        } else {
            target.append('"');
            final int len = name.length();
            for (int i = 0; i < len; i++) {
                final char c = name.charAt(i);
                if (Character.isISOControl(c)) {
                    final String hs = Integer.toHexString(c);
                    target.append("\\u");
                    for (int j = hs.length(); j < 4; j ++) {
                        target.append('0');
                    }
                    target.append(hs);
                } else if (c == '\\' || c == '"') {
                    target.append('\\');
                    target.append(c);
                } else {
                    target.append(c);
                }
            }
            target.append('"');
        }
        return target;
    }

    /**
     * Compare two service names lexicographically.
     *
     * @param o the other name
     * @return -1 if this name collates before the argument, 1 if it collates after, or 0 if they are equal
     */
    public int compareTo(final ServiceName o) {
        if (o == null) {
            throw new IllegalArgumentException("o is null");
        }
        if (this == o) return 0;
        final int length1 = length();
        final int length2 = o.length();
        int res;
        if (length1 == length2) {
            return compareTo(o, length1 - 1);
        }
        int diff = length1 - length2;
        if (diff > 0) {
            ServiceName x;
            for (x = this; diff > 0; diff--) {
                x = x.parent;
            }
            res = x.compareTo(o, length2 - 1);
            return res == 0 ? 1 : res;
        } else {
            return - o.compareTo(this);
        }
    }

    private int compareTo(final ServiceName o, final int remainingLength) {
        if (this == o) {
            return 0;
        } else if (remainingLength == 0) {
            return name.compareTo(o.name);
        } else {
            int res = parent.compareTo(o.parent, remainingLength - 1);
            return res == 0 ? name.compareTo(o.name) : res;
        }
    }

    // Serialization stuff

    private static final Field hashCodeField;

    static {
        hashCodeField = AccessController.doPrivileged(new PrivilegedAction<Field>() {
            public Field run() {
                final Field field;
                try {
                    field = ServiceName.class.getDeclaredField("hashCode");
                } catch (Exception e) {
                    throw new IllegalStateException(e);
                }
                field.setAccessible(true);
                return field;
            }
        });
    }

    private void readObject(ObjectInputStream ois) throws ClassNotFoundException, IOException {
        ois.defaultReadObject();
        try {
            hashCodeField.setInt(this, calculateHashCode(parent, name));
        } catch (IllegalAccessException e) {
            final InvalidObjectException e2 = new InvalidObjectException("Cannot set hash code field");
            e2.initCause(e);
            throw e2;
        }
    }
}
