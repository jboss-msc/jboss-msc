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

package org.jboss.msc.services;

import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.Value;

import javax.management.JMException;
import javax.management.MBeanServer;
import javax.management.ObjectName;

/**
 * A service which registers the target object as an MBean.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class MBeanService implements Service<Void> {
    private final Value<? extends MBeanServer> mbeanServer;
    private final Value<?> value;
    private final ObjectName objectName;

    /**
     * The service name under which JMX-related entities are registered.
     */
    public static ServiceName JBOSS_JMX = ServiceName.JBOSS.append("jmx");
    /**
     * The service name under which mbeans for services are registered.  The service name that was registered as an
     * mbean will follow this part.
     */
    public static ServiceName JBOSS_JMX_MBEAN = JBOSS_JMX.append("mbean");
    /**
     * The service name under which mbean servers are registered.
     */
    public static ServiceName JBOSS_JMX_MBEANSERVER = JBOSS_JMX.append("mbeanServer");

    /**
     * Construct a new instance.
     *
     * @param mbeanServer the mbean server
     * @param value the value to install
     * @param objectName the object name to use
     */
    public MBeanService(final Value<? extends MBeanServer> mbeanServer, final Value<?> value, final ObjectName objectName) {
        this.mbeanServer = mbeanServer;
        this.value = value;
        this.objectName = objectName;
    }

    /** {@inheritDoc} */
    public void start(final StartContext context) throws StartException {
        try {
            mbeanServer.getValue().registerMBean(value.getValue(), objectName);
        } catch (Exception e) {
            throw new StartException(e);
        }
    }

    /** {@inheritDoc} */
    public void stop(final StopContext context) {
        try {
            mbeanServer.getValue().unregisterMBean(objectName);
        } catch (JMException e) {
            // todo log
        }
    }

    /** {@inheritDoc} */
    public Void getValue() throws IllegalStateException {
        return null;
    }
}
