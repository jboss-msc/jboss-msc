/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2022, Red Hat, Inc., and individual contributors
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
import java.net.URL;
import java.util.Enumeration;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

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
        final Enumeration<URL> resources;
        String jarName = "(unknown)";
        String versionString = "(unknown)";
        try {
            final ClassLoader classLoader = Version.class.getClassLoader();
            resources = classLoader == null ? ClassLoader.getSystemResources("META-INF/MANIFEST.MF") : classLoader.getResources("META-INF/MANIFEST.MF");
            while (resources.hasMoreElements()) {
                final URL url = resources.nextElement();
                final InputStream stream = url.openStream();
                if (stream != null) try {
                    final Manifest manifest = new Manifest(stream);
                    final Attributes mainAttributes = manifest.getMainAttributes();
                    if (mainAttributes != null && "JBoss Modular Service Container".equals(mainAttributes.getValue("Specification-Title"))) {
                        jarName = mainAttributes.getValue("Jar-Name");
                        versionString = mainAttributes.getValue("Jar-Version");
                    }
                } finally {
                    try { stream.close(); } catch (Throwable ignored) {}
                }
            }
        } catch (IOException ignored) {
        }
        JAR_NAME = jarName;
        VERSION_STRING = versionString;
    }

    /**
     * Get the name of the JBoss Modules JAR.
     *
     * @return the name
     */
    public static String getJarName() {
        return JAR_NAME;
    }

    /**
     * Get the version string of JBoss Modules.
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
