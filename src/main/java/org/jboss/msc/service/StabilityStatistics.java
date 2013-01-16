/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013, Red Hat, Inc., and individual contributors
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
 * A stability monitor statistics.
 *
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
public final class StabilityStatistics {
    
    private int active;
    private int failed;
    private int lazy;
    private int never;
    private int onDemand;
    private int passive;
    private int problems;
    private int started;
    private int removed;
    
    public int getActiveCount() {
        return active;
    }
    
    public int getFailedCount() {
        return failed;
    }
    
    public int getLazyCount() {
        return lazy;
    }
    
    public int getNeverCount() {
        return never;
    }
    
    public int getOnDemandCount() {
        return onDemand;
    }
    
    public int getPassiveCount() {
        return passive;
    }
    
    public int getProblemsCount() {
        return problems;
    }

    public int getStartedCount() {
        return started;
    }
    
    public int getRemovedCount() {
        return removed;
    }
    
    void setActiveCount(final int count) {
        active = count;
    }
    
    void setFailedCount(final int count) {
        failed = count;
    }
    
    void setLazyCount(final int count) {
        lazy = count;
    }
    
    void setNeverCount(final int count) {
        never = count;
    }

    void setOnDemandCount(final int count) {
        onDemand = count;
    }

    void setPassiveCount(final int count) {
        passive = count;
    }
    
    void setProblemsCount(final int count) {
        problems = count;
    }

    void setStartedCount(final int count) {
        started = count;
    }

    void setRemovedCount(final int count) {
        removed = count;
    }
}
