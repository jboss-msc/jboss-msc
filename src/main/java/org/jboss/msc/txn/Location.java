/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013 Red Hat, Inc., and individual contributors
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

package org.jboss.msc.txn;

import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import org.jboss.msc._private.MSCLogger;

/**
 * A location description for a {@link Problem}.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class Location implements Serializable {

    private static final long serialVersionUID = 1717735949029632210L;

    private static final Nesting[] nestingValues = Nesting.values();

    private final String locationName;
    private final int lineNumber;
    private final int columnNumber;
    private final Location parent;
    private final int nestingType;

    private transient int hashCode;

    /**
     * Construct a new instance which has a parent.
     *
     * @param locationName the location name (must not be {@code null})
     * @param lineNumber the line number, or 0 if none
     * @param columnNumber the column number, or 0 if none
     * @param parent the parent location (must not be {@code null})
     * @param nestingType the nesting type (must not be {@code null})
     */
    public Location(final String locationName, final int lineNumber, final int columnNumber, final Location parent, final Nesting nestingType) {
        if (locationName == null) {
            throw new IllegalArgumentException("locationName is null");
        }
        if (parent == null) {
            throw new IllegalArgumentException("parent is null");
        }
        if (nestingType == null) {
            throw new IllegalArgumentException("nestingType is null");
        }
        if (lineNumber < 0) {
            throw new IllegalArgumentException("line number is less than 0");
        }
        if (columnNumber < 0) {
            throw new IllegalArgumentException("column number is less than 0");
        }
        if (nestingType == Nesting.UNKNOWN) {
            throw new IllegalArgumentException("nestingType cannot be UNKNOWN");
        }
        this.locationName = locationName;
        this.lineNumber = lineNumber;
        this.columnNumber = columnNumber;
        this.parent = parent;
        this.nestingType = nestingType.ordinal();
    }

    /**
     * Construct a new instance with no parent.
     *
     * @param locationName the location name (must not be {@code null})
     * @param lineNumber the line number, or 0 if none
     * @param columnNumber the column number, or 0 if none
     */
    public Location(final String locationName, final int lineNumber, final int columnNumber) {
        if (locationName == null) {
            throw new IllegalArgumentException("locationName is null");
        }
        if (lineNumber < 0) {
            throw new IllegalArgumentException("line number is less than 0");
        }
        if (columnNumber < 0) {
            throw new IllegalArgumentException("column number is less than 0");
        }
        this.locationName = locationName;
        this.lineNumber = lineNumber;
        this.columnNumber = columnNumber;
        parent = null;
        nestingType = 0;
    }

    /**
     * Get the location name.  This can be a file name, XML element name, etc.
     *
     * @return the location name
     */
    public String getLocationName() {
        return locationName;
    }

    /**
     * Get the source line number starting from 1.  If no line is associated, 0 is returned.
     *
     * @return the source line number
     */
    public int getLineNumber() {
        return lineNumber;
    }

    /**
     * Get the source column number starting from 1.  If no column is associated, 0 is returned.
     *
     * @return the source column number
     */
    public int getColumnNumber() {
        return columnNumber;
    }

    /**
     * Get the parent location, or {@code null} if none.
     *
     * @return the parent location
     */
    public Location getParent() {
        return parent;
    }

    /**
     * Get the nesting type, or {@code null} if this location does not have a parent.
     *
     * @return the nesting type
     */
    public Nesting getNestingType() {
        if (nestingType == 0) return null;
        try {
            return nestingValues[nestingType];
        } catch (ArrayIndexOutOfBoundsException e) {
            return Nesting.UNKNOWN;
        }
    }

    /**
     * Determine if this location is equal to another.
     *
     * @param obj the other location
     * @return {@code true} if equal, {@code false} otherwise
     */
    public boolean equals(final Object obj) {
        return obj == this || obj instanceof Location && equals((Location) obj);
    }

    /**
     * Determine if this location is equal to another.
     *
     * @param obj the other location
     * @return {@code true} if equal, {@code false} otherwise
     */
    public boolean equals(final Location obj) {
        return obj == this || obj != null && locationName.equals(obj.locationName) && lineNumber == obj.lineNumber && columnNumber == obj.columnNumber && nestingType == obj.nestingType && (parent == null || parent.equals(obj.parent));
    }

    /**
     * Get the hash code.
     *
     * @return the hash code
     */
    public int hashCode() {
        if (hashCode == 0) {
            int hc = locationName.hashCode();
            hc = hc * 17 + lineNumber;
            hc = hc * 17 + columnNumber;
            hc = hc * 17 + parent.hashCode();
            hc = hc * 17 + nestingType;
            if (hc == 0) {
                hc = 1 << 31; // basically the same in many circumstances but not 0 to prevent recalculation
            }
            return hashCode = hc;
        }
        return hashCode;
    }

    private StringBuilder toString(StringBuilder builder) {
        builder.append(locationName);
        if (lineNumber > 0) {
            builder.append(':').append(lineNumber);
            if (columnNumber > 0) {
                builder.append(':').append(columnNumber);
            }
        }
        if (parent != null) {
            builder.append(',').append(' ').append(getNestingType().getDescription());
            builder.append(':').append('\n').append('\t');
        }
        return builder;
    }

    /**
     * Get the string representation of this location.
     *
     * @return the string representation of this location
     */
    public String toString() {
        return toString(new StringBuilder()).toString();
    }

    private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
        ois.defaultReadObject();
        final int nestingType = ois.readUnsignedByte();
        if (locationName == null) {
            throw new InvalidObjectException("locationName is null");
        }
        if ((parent == null) != (nestingType == -1)) {
            throw new InvalidObjectException("either both nesting and parent must be null, or neither must be null");
        }
        if (lineNumber < 0) {
            throw new InvalidObjectException("line number is less than 0");
        }
        if (columnNumber < 0) {
            throw new InvalidObjectException("column number is less than 0");
        }
    }

    /**
     * The nesting type for a location within a location.
     */
    // RETAIN THIS ORDER
    public enum Nesting {
        /**
         * The nesting relationship is not known.
         */
        UNKNOWN,
        /**
         * This location is physically contained within a parent location. For example, an XML element within a file, or a
         * file within a JAR, or a management attribute within a management resource.
         */
        CONTAINED,
        /**
         * This location was included within a parent location. For example, an XML file included from another XML file.
         */
        INCLUDED,
        ;

        /**
         * Get the description of this nesting type.
         *
         * @return the description of this nesting type
         */
        public String getDescription() {
            switch (ordinal()) {
                case 0: return MSCLogger.ROOT.nestingUnknown();
                case 1: return MSCLogger.ROOT.nestingContained();
                case 2: return MSCLogger.ROOT.nestingIncluded();
                default: throw new IllegalStateException();
            }
        }
    }
}
