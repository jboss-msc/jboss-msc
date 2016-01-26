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

package org.jboss.msc;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Properties;

/**
 * The current version of this module.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class Version {

    private Version() {}

    private static final String JAR_NAME;
    private static final String VERSION_STRING;

    static {
        String version = "(Unknown)";
        String jarName = "(Unknown)";
        try {
            final InputStream stream = Version.class.getResourceAsStream("Version.properties");
            try {
                final InputStreamReader reader = new InputStreamReader(stream, "UTF-8");
                try {
                    Properties versionProps = new Properties();
                    versionProps.load(reader);
                    jarName = versionProps.getProperty("jarName", jarName);
                    version = versionProps.getProperty("version", version);
                } finally {
                    try {
                        reader.close();
                    } catch (Throwable ignored) {
                    }
                }
            } finally {
                try {
                    stream.close();
                } catch (Throwable ignored) {
                }
            }
        } catch (IOException ignored) {
        }
        VERSION_STRING = version;
        JAR_NAME = jarName;
    }

    /**
     * Get the name of the Jboss MSC JAR.
     *
     * @return the name
     */
    public static String getJarName() {
        return JAR_NAME;
    }


    /**
     * Get the version string of MSC.
     *
     * @return the version string
     */
    public static String getVersionString() {
        return VERSION_STRING;
    }

    /**
     * Print out the current version on {@code System.out}.
     *
     * @param args ignored
     */
    public static void main(String[] args) {
        System.out.printf("JBoss Modular Service Container version %s\n", VERSION_STRING);
    }
}
