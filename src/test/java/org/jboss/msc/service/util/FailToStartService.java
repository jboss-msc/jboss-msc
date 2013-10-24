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

package org.jboss.msc.service.util;

import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A Service that can be set to fail to start on any start attempt.
 * 
 * @author <a href="mailto:flavia.rainone@jboss.com">Flavia Rainone</a>
 *
 */
public class FailToStartService implements Service<Void> {
    boolean fail = false;

    /**
     * Constructor.
     * 
     * @param fail sets whether the service should fail to start the first start attempt.
     */
    public FailToStartService(boolean fail) {
        this.fail = fail;
    }

    @Override
    public void start(StartContext context) throws StartException {
        context.asynchronous();
        if (fail) {
            fail = false;
            Logger logger = Logger.getLogger("org.jboss.msc.service.fail");
            Level level = logger.getLevel();
            try {
                logger.setLevel(Level.OFF);
                context.failed(new StartException("Second service failed"));
            } finally {
                logger.setLevel(level);
            }
            // a second attempt to mark a context as asynchronous should be noop
            context.asynchronous();
        }
        else {
            context.complete();
            try {
                context.complete();
                Logger.getLogger("org.jboss.msc.service.fail").info("IllegalStateException expected");
            } catch (IllegalStateException e) {}
        }
    }

    @Override
    public void stop(StopContext context) {
        context.asynchronous();
        try {
            context.asynchronous();
        } catch (IllegalStateException e) {}
        context.complete();
    }

    @Override
    public Void getValue() throws IllegalStateException {
        return null;
    }

    /**
     * Set this service to fail the next time it tries to start.
     */
    public void failNextTime() {
        fail = true;
    }
}