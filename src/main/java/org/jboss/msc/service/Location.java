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

/**
 * A location at which a service was defined.
 */
public final class Location {
    private final String fileName;
    private final int lineNumber;
    private final int columnNumber;
    private final Location parentLocation;

    /**
     * Create a new instance.
     *
     * @param fileName the file name, or {@code null} if unknown
     * @param lineNumber the line number, or {@code 0} if unknown
     * @param columnNumber the column number, or {@code 0} if unknown
     * @param parentLocation the parent location, or {@code null} for none
     */
    public Location(final String fileName, final int lineNumber, final int columnNumber, final Location parentLocation) {
        this.fileName = fileName;
        this.lineNumber = lineNumber;
        this.columnNumber = columnNumber;
        this.parentLocation = parentLocation;
    }

    /**
     * Get the file name.
     *
     * @return the file name, or {@code null} if unknown
     */
    public String getFileName() {
        return fileName;
    }

    /**
     * Get the line number.
     *
     * @return the line number, or {@code 0} if unknown
     */
    public int getLineNumber() {
        return lineNumber;
    }

    /**
     * Get the column number.
     *
     * @return the column number, or {@code 0} if unknown
     */
    public int getColumnNumber() {
        return columnNumber;
    }

    private void toString(StringBuilder b) {
        if (fileName == null) {
            b.append("<Unknown File>");
        } else {
            b.append(fileName);
        }
        if (lineNumber > 0) {
            b.append(" line ");
            b.append(lineNumber);
            if (columnNumber > 0) {
                b.append(", column ");
                b.append(columnNumber);
            }
        }
        if (parentLocation != null) {
            b.append("\n\tincluded from ");
            parentLocation.toString(b);
        }
    }

    /**
     * Get the human-readable string representation of this location.
     *
     * @return the string
     */
    public String toString() {
        StringBuilder b = new StringBuilder();
        toString(b);
        return b.toString();
    }
}
