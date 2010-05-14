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

package org.jboss.msc.bench;

import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;

/**
 * A test service which performs a long-ish calculation.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class ReallyBusyService implements Service<ReallyBusyService> {
    private static final int N = 500000000;
    double sum = 0;

    public void start(final StartContext context) throws StartException {
        // calculate pi, slowly (and inaccurately).  Takes around 8800 ms on my 1.86GHz system.
        double sum = 0.0;
        double term;
        double sign = 1.0;
        for (int k = 0; k < N; k++) {
           term = 1.0/(2.0*k + 1.0);
           sum = sum + sign*term;
           sign = -sign;
        }
        this.sum = sum;
    }

    public void stop(final StopContext context) {
    }

    public ReallyBusyService getValue() throws IllegalStateException {
        return this;
    }

    public static void main(String[] args) throws StartException {
        long start = System.currentTimeMillis();
        new ReallyBusyService().start(null);
        long t = System.currentTimeMillis() - start;
        System.out.println("Time = " + t);
    }
}