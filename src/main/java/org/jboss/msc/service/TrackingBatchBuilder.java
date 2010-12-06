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

package org.jboss.msc.service;

import java.util.Set;

/**
 * A batch builder which tracks what services are added to it.  Normally acquired via {@link TrackingServiceTarget#batchBuilder()}.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public class TrackingBatchBuilder extends TrackingServiceTarget implements BatchBuilder {

    private final BatchBuilder delegateBuilder;
    private final Set<ServiceName> targetSet;

    /**
     * Construct a new instance.
     *
     * @param delegateBuilder the batch builder to delegate to
     * @param targetSet the list to append installed services to
     */
    public TrackingBatchBuilder(final BatchBuilder delegateBuilder, final Set<ServiceName> targetSet) {
        super(delegateBuilder);
        this.delegateBuilder = delegateBuilder;
        this.targetSet = targetSet;
    }

    /** {@inheritDoc} */
    public void install() throws ServiceRegistryException {
        delegateBuilder.install();
        targetSet.addAll(getSet());
    }
}
