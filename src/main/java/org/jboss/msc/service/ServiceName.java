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
package org.jboss.msc.service;

import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Service name class.
 *
 * @author John Bailey
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class ServiceName implements Comparable<ServiceName>, Serializable {

    private static final long serialVersionUID = 2336190201880964151L;
    private static final Pattern validNameSegmentPattern = Pattern.compile("[\\u0000-\\u10FFFF&&[^\\u0000-\\u001F]&&[^\\u007F-\\u009F]&&[^ ]]+");

    private final String name;
    private final ServiceName parent;
    private final transient int hashCode;

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
        if (parts == null || parts.length < 1)
            throw new IllegalArgumentException("Must provide at least one name segment");
        
        ServiceName current = parent;
        for (String part : parts) {
            if (part == null) {
                throw new IllegalArgumentException("Name segment is null for " + current.getSimpleName());
            }
            if (part.isEmpty()) {
                throw new IllegalArgumentException("Empty name segment is not allowed for " + current.getSimpleName());
            }
            current = new ServiceName(current, part);
        }
        return current;
    }

    private ServiceName(final ServiceName parent, final String name) {
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
     * Determine whether this service name is the same as, or a parent of, the given service name.
     *
     * @param other the other name
     * @return {@code true} if this service name is a parent
     */
    public boolean isParentOf(ServiceName other) {
        return other != null && (equals(other) || isParentOf(other.parent));
    }

    /**
     * Return the service name that is the nearest common ancestor of the this name and the given one.
     *
     * @param other the other name
     * @return the nearest common ancestor, or {@code null} if they are unrelated
     */
    public ServiceName commonAncestorOf(ServiceName other) {
        if (other == null) return null;
        final Deque<ServiceName> myAncestry = new ArrayDeque<ServiceName>();
        final Deque<ServiceName> otherAncestry = new ArrayDeque<ServiceName>();
        ServiceName i = this;
        do {
            myAncestry.addFirst(i);
            i = i.parent;
        } while (i != null);
        i = other;
        do {
            otherAncestry.addFirst(i);
            i = i.parent;
        } while (i != null);
        final Iterator<ServiceName> mi = myAncestry.iterator();
        final Iterator<ServiceName> oi = otherAncestry.iterator();
        // i = null;
        while (mi.hasNext() && oi.hasNext()) {
            final ServiceName mn = mi.next();
            final ServiceName on = oi.next();
            if (! mn.equals(on)) {
                break;
            }
            i = mn;
        }
        return i;
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
        return "service " + getCanonicalName();
    }

    /**
     * Get the canonical name of this service name.
     *
     * @return the canonical name
     */
    public String getCanonicalName() {
        return getCanonicalName(new StringBuilder()).toString();
    }

    /**
     * Indicates if the name segment is valid.
     * 
     * @param  part a name segment
     * @return {@code true} if {@code part} is valid
     */
    public static boolean isValidNameSegment(String part) {
        return part != null && validNameSegmentPattern.matcher(part).matches();
    }

    /**
     * Parse a string-form service name.  If the given name contains quoted sections (surrounded by '{@code "}' characters), then
     * the section will be parsed as a quoted string with optional escaped characters.  The set of valid escapes is
     * similar to what is supported by the JLS (§3.3 and §3.10.6), with one exception: the string {@code \0} is always parsed as a NUL character
     * (0) and not as an octal escape sequence.  Control characters are not allowed in any part of a name
     * and must be escaped in a quoted section if they are present in the service name string.  Whitespace characters
     * are allowed only in a quoted section.
     *
     * @param original the string form of a service name
     * @return a {@code ServiceName} instance
     * @throws IllegalArgumentException if the original is not valid
     */
    public static ServiceName parse(String original) throws IllegalArgumentException {
        final int originalLength = original.length();
        final List<String> segments = new ArrayList<String>();
        final StringBuilder builder = new StringBuilder();
        int state = 0;
        char escapedChar = '\0';
        int charSize;
        for (int i = 0; i < originalLength; i += charSize) {
            int nextOffset = original.offsetByCodePoints(i, 1);
            charSize = nextOffset - i;
            final int c = original.codePointAt(i);
            if (! Character.isValidCodePoint(c)) {
                throw invalidCodePoint(i);
            }
            if (Character.isISOControl(c)) {
                throw invalidNameCharacter(i);
            }
            switch (state) {
                case 0: {
                    // First character in a section.
                    builder.setLength(0);
                    if (c == '"') {
                        // Quoted section.
                        state = 2;
                        continue;
                    } else {
                        // Unquoted section.  Make sure c is valid.
                        if (c == '.' || c == '\\' || Character.isWhitespace(c)) {
                            throw invalidNameCharacter(i);
                        }
                        builder.append(original.substring(i, nextOffset));
                        state = 1;
                        continue;
                    }
                    // not reached
                }
                case 1: {
                    // Subsequent character in an unquoted section.
                    if (c == '\\' || c == '"' || Character.isWhitespace(c)) {
                        throw invalidNameCharacter(i);
                    } else if (c == '.') {
                        // Section finished.
                        segments.add(builder.toString());
                        state = 0;
                        continue;
                    } else {
                        builder.append(original.substring(i, nextOffset));
                        continue;
                    }
                    // not reached
                }
                case 2: {
                    // First character in a quoted section.
                    if (c == '"') {
                        throw invalidNameCharacter(i);
                    } else if (c == '\\') {
                        // First character is escaped.
                        state = 3;
                        continue;
                    } else {
                        builder.append(original.substring(i, nextOffset));
                        state = 4;
                        continue;
                    }
                    // not reached
                }
                case 3: {
                    // First character in a quoted section, after a \ character.
                    // All valid escapes:
                    switch (c) {
                        case '"': builder.append('"'); state = 4; continue;
                        case '\'': builder.append('\''); state = 4; continue;
                        case '\\': builder.append('\\'); state = 4; continue;
                        case 'u': state = 5; continue;
                        case 'b': builder.append('\b'); state = 4; continue;
                        case 't': builder.append('\t'); state = 4; continue;
                        case 'n': builder.append('\n'); state = 4; continue;
                        case 'f': builder.append('\f'); state = 4; continue;
                        case 'r': builder.append('\r'); state = 4; continue;
                        case '0': builder.append('\0'); state = 4; continue;
                        default: throw invalidNameCharacter(i);
                    }
                    // not reached
                }
                case 4: {
                    // Subsequent character in a quoted section
                    if (c == '"') {
                        // End of section; expect only a . next.
                        segments.add(builder.toString());
                        state = 9;
                        continue;
                    } else if (c == '\\') {
                        // Character is escaped.
                        state = 3;
                        continue;
                    } else {
                        builder.append(original.substring(i, nextOffset));
                        continue;
                    }
                    // not reached
                }
                case 5: {
                    // Unicode escape, first char.
                    int v;
                    try {
                        v = Integer.parseInt(original.substring(i, nextOffset), 16);
                    } catch (NumberFormatException e) {
                        throw invalidNameCharacter(i);
                    }
                    escapedChar = (char) (v << 12);
                    state = 6;
                    continue;
                }
                case 6: {
                    // Unicode escape, second char.
                    int v;
                    try {
                        v = Integer.parseInt(original.substring(i, nextOffset), 16);
                    } catch (NumberFormatException e) {
                        throw invalidNameCharacter(i);
                    }
                    escapedChar |= (char) (v << 8);
                    state = 7;
                    continue;
                }
                case 7: {
                    // Unicode escape, third char.
                    int v;
                    try {
                        v = Integer.parseInt(original.substring(i, nextOffset), 16);
                    } catch (NumberFormatException e) {
                        throw invalidNameCharacter(i);
                    }
                    escapedChar |= (char) (v << 4);
                    state = 8;
                    continue;
                }
                case 8: {
                    // Unicode escape, last char.
                    int v;
                    try {
                        v = Integer.parseInt(original.substring(i, nextOffset), 16);
                    } catch (NumberFormatException e) {
                        throw invalidNameCharacter(i);
                    }
                    escapedChar |= (char) v;
                    builder.append(escapedChar);
                    state = 4;
                    continue;
                }
                case 9: {
                    if (c == '.') {
                        state = 0;
                        continue;
                    }
                    throw invalidNameCharacter(i);
                }
                default: {
                    throw new IllegalStateException();
                }
            }
            // not reached
        }
        switch (state) {
            case 0:
            case 2:
            case 3:
            case 4:
            case 5:
            case 6:
            case 7:
            case 8: {
                // End of string unexpected.
                throw unexpectedEnd();
            }
            case 1: {
                segments.add(builder.toString());
                // fall thru
            }
            case 9: {
                break;
            }
            default: throw new IllegalStateException();
        }
        return ServiceName.of(segments.toArray(new String[segments.size()]));
    }

    private static IllegalArgumentException unexpectedEnd() {
        return new IllegalArgumentException("Unexpected end of name");
    }

    private static IllegalArgumentException invalidCodePoint(final int i) {
        return new IllegalArgumentException("Invalid code point at offset " + i);
    }

    private static IllegalArgumentException invalidNameCharacter(final int i) {
        return new IllegalArgumentException("Invalid name character at offset " + i);
    }

    private StringBuilder getCanonicalName(StringBuilder target) {
        final ServiceName parent = this.parent;
        if (parent != null) {
            parent.getCanonicalName(target);
            target.append('.');
        }
        final String name = this.name;
        final int nameLength = name.length();
        boolean simple = true;
        for (int i = 0; i < nameLength; i = name.offsetByCodePoints(i, 1)) {
            final int c = name.codePointAt(i);
            if (Character.isISOControl(c) || Character.isWhitespace(c) || c == '.' || c == '"') {
                simple = false;
                break;
            }
        }
        if (simple) {
            target.append(name);
        } else {
            target.append('"');
            int charSize;
            for (int i = 0; i < nameLength; i += charSize) {
                int nextOffset = name.offsetByCodePoints(i, 1);
                charSize = nextOffset - i;
                final int c = name.codePointAt(i);
                switch (c) {
                    case '\b': target.append('\\').append('b'); break;
                    case '\t': target.append('\\').append('t'); break;
                    case '\n': target.append('\\').append('n'); break;
                    case '\f': target.append('\\').append('f'); break;
                    case '\r': target.append('\\').append('r'); break;
                    case '\0': target.append('\\').append('0'); break;
                    case '\"': target.append('\\').append('"'); break;
                    case '\\': target.append('\\').append('\\'); break;
                    default: {
                        if (Character.isISOControl(c)) {
                            final String hs = Integer.toHexString(c);
                            target.append("\\u");
                            for (int j = hs.length(); j < 4; j ++) {
                                target.append('0');
                            }
                            target.append(hs);
                        } else {
                            target.append(name.substring(i, nextOffset));
                        }
                        break;
                    }
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

    /**
     * Convert this service name into an array of strings containing the segments of the name.  If this array
     * is passed into {@link #of(String...)} it will yield a {@code ServiceName} which is equal to this one.
     *
     * @return the string array
     */
    public String[] toArray() {
        return toArray(0);
    }

    private String[] toArray(final int idx) {
        if (parent == null) {
            return new String[idx];
        } else {
            String[] result = parent.toArray(idx + 1);
            result[result.length - idx - 1] = name;
            return result;
        }
    }
}
