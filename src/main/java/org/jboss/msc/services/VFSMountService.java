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

package org.jboss.msc.services;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.vfs.TempFileProvider;
import org.jboss.vfs.VFS;
import org.jboss.vfs.VFSUtils;
import org.jboss.vfs.VirtualFile;

public final class VFSMountService implements Service {

    // Configuration properties
    private String path;
    private TempFileProvider tempFileProvider;
    private boolean exploded;
    // Service state
    private Closeable handle;

    public String getPath() {
        return path;
    }

    public void setPath(final String path) {
        this.path = path;
    }

    public TempFileProvider getTempFileProvider() {
        return tempFileProvider;
    }

    public void setTempFileProvider(final TempFileProvider tempFileProvider) {
        this.tempFileProvider = tempFileProvider;
    }

    public boolean isExploded() {
        return exploded;
    }

    public void setExploded(final boolean exploded) {
        this.exploded = exploded;
    }

    public void start(final StartContext context) throws StartException {
        try {
            final VirtualFile virtualFile = VFS.getChild(path);
            final File file = virtualFile.getPhysicalFile();
            if (file.isDirectory()) {
                handle = null;
                return;
            }
            if (exploded) {
                handle = VFS.mountZipExpanded(virtualFile, virtualFile, tempFileProvider);
            } else {
                handle = VFS.mountZip(virtualFile, virtualFile, tempFileProvider);
            }
        } catch (IOException e) {
            throw new StartException(e);
        }
    }

    public void stop(final StopContext context) {
        VFSUtils.safeClose(handle);
        handle = null;
    }
}
