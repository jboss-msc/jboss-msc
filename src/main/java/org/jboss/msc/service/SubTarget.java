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


/**
 * A sub-target represents a set of ServiceBuilders that will be installed in the parent target.
 * This class can be used to apply listeners/dependencies to all the ServiceBuilders of the represented set.
 *
 * @author John Bailey
 * @author <a href="mailto:flavia.rainone@jboss.com">Flavia Rainone</a>
 */
final class SubTarget extends AbstractServiceTarget {

    // the parent of this subTarget
    private final AbstractServiceTarget parent;

    SubTarget(final AbstractServiceTarget parent) {
        this.parent = parent;
    }

    /**
     * Applies the listeners and dependencies to the set of ServiceBuilders represented by this subTarget, before
     * proceeding with installation of {@code serviceBuilder} into the parent target.
     */
    @Override
    void install(ServiceBuilderImpl<?> serviceBuilder) throws ServiceRegistryException {
        apply(serviceBuilder);
        parent.install(serviceBuilder);
    }

    @Override
    void validateTargetState() {
        parent.validateTargetState();
    }

    @Override
    boolean hasService(ServiceName name) {
        return parent.hasService(name);
    }
}