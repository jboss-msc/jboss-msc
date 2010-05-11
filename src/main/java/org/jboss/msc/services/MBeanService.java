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

import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.Value;

import javax.management.JMException;
import javax.management.MBeanServer;
import javax.management.ObjectName;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class MBeanService implements Service {
    private final Value<? extends MBeanServer> mbeanServer;
    private final Value<?> value;
    private final ObjectName objectName;

    public MBeanService(final Value<? extends MBeanServer> mbeanServer, final Value<?> value, final ObjectName objectName) {
        this.mbeanServer = mbeanServer;
        this.value = value;
        this.objectName = objectName;
    }

    public void start(final StartContext context) throws StartException {
        try {
            mbeanServer.getValue().registerMBean(value.getValue(), objectName);
        } catch (Exception e) {
            throw new StartException(e);
        }
    }

    public void stop(final StopContext context) {
        try {
            mbeanServer.getValue().unregisterMBean(objectName);
        } catch (JMException e) {
            // todo log
        }
    }
}
