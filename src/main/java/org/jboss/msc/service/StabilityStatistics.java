/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2021, Red Hat, Inc., and individual contributors
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
 * A stability monitor statistics. Allows to collect statistics data
 * about {@link ServiceController}s registered with {@link StabilityMonitor} object.
 * The following data are available:
 * <ul>
 *   <li>count of controllers in <b>ACTIVE</b> mode - see method {@link #getActiveCount()}</li> 
 *   <li>count of controllers that <b>FAILED</b> to start - see method {@link #getFailedCount()}</li> 
 *   <li>count of controllers in <b>LAZY</b> mode - see method {@link #getLazyCount()}</li> 
 *   <li>count of controllers in <b>NEVER</b> mode - see method {@link #getNeverCount()}</li> 
 *   <li>count of controllers in <b>ON_DEMAND</b> mode - see method {@link #getOnDemandCount()}</li> 
 *   <li>count of controllers in <b>PASSIVE</b> mode - see method {@link #getPassiveCount()}</li> 
 *   <li>count of controllers that had <b>PROBLEM</b> to start - see method {@link #getProblemsCount()}</li> 
 *   <li>count of controllers in <b>UP</b> state - see method {@link #getStartedCount()}</li> 
 * </ul>
 * 
 * Sample usage:
 * <pre>
 * StabilityMonitor monitor = ...
 * <b>StabilityStatistics statistics = new StabilityStatistics();</b>
 * monitor.awaitStability(<b>statistics</b>);
 * // do something with <b>statistics</b> object.
 * </pre>
 *
 * @see StabilityMonitor
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 * @deprecated Stability monitors are unreliable - do not use them.
 * This class will be removed in a future release.
 */
@Deprecated
public final class StabilityStatistics {
    
    private int active;
    private int failed;
    private int lazy;
    private int never;
    private int onDemand;
    private int passive;
    private int problems;
    private int started;

    /**
     * Returns count of controllers registered with {@link StabilityMonitor} that are in
     * {@link ServiceController.Mode#ACTIVE} mode.
     * @return count of <b>ACTIVE</b> controllers
     */
    public int getActiveCount() {
        return active;
    }
    
    /**
     * Returns count of controllers registered with {@link StabilityMonitor} that failed to start
     * because of start exception being thrown.
     * @return count of <b>FAILED</b> controllers
     */
    public int getFailedCount() {
        return failed;
    }
    
    /**
     * Returns count of controllers registered with {@link StabilityMonitor} that are in
     * {@link ServiceController.Mode#LAZY} mode.
     * @return count of <b>LAZY</b> controllers
     */
    public int getLazyCount() {
        return lazy;
    }
    
    /**
     * Returns count of controllers registered with {@link StabilityMonitor} that are in
     * {@link ServiceController.Mode#NEVER} mode.
     * @return count of <b>NEVER</b> controllers
     */
    public int getNeverCount() {
        return never;
    }
    
    /**
     * Returns count of controllers registered with {@link StabilityMonitor} that are in
     * {@link ServiceController.Mode#ON_DEMAND} mode.
     * @return count of <b>ON_DEMAND</b> controllers
     */
    public int getOnDemandCount() {
        return onDemand;
    }
    
    /**
     * Returns count of controllers registered with {@link StabilityMonitor} that are in
     * {@link ServiceController.Mode#PASSIVE} mode.
     * @return count of <b>PASSIVE</b> controllers
     */
    public int getPassiveCount() {
        return passive;
    }
    
    /**
     * Returns count of controllers registered with {@link StabilityMonitor} that had problem to start
     * because of missing dependencies.
     * @return count of <b>PROBLEM</b> controllers
     */
    public int getProblemsCount() {
        return problems;
    }

    /**
     * Returns count of controllers registered with {@link StabilityMonitor} that are in
     * {@link ServiceController.State#UP} state.
     * @return count of <b>STARTED</b> controllers
     */
    public int getStartedCount() {
        return started;
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
}
