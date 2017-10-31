/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017, Red Hat, Inc., and individual contributors
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
 * An optional dependency. Creates bridge between dependent and dependency.
 *
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
final class OptionalDependencyImpl implements Dependency, Dependent {

    private final Dependency dependency;
    private volatile Dependent dependent;
    private volatile boolean available = true;

    OptionalDependencyImpl(final Dependency dependency) {
        this.dependency = dependency;
    }

    @Override
    public void addDependent(final Dependent dependent) {
        assert this.dependent == null;
        this.dependent = dependent;
        dependency.addDependent(this);
    }

    @Override
    public void removeDependent(final Dependent dependent) {
        assert this.dependent == dependent;
        dependency.removeDependent(this);
        this.dependent = null;
    }

    @Override
    public void addDemand() {
        dependency.addDemand();
    }

    @Override
    public void removeDemand() {
        dependency.removeDemand();
    }

    @Override
    public void dependentStarted() {
        dependency.dependentStarted();
    }

    @Override
    public void dependentStopped() {
        dependency.dependentStopped();
    }

    @Override
    public Object getValue() {
        try {
            return available ? dependency.getValue() : null;
        } catch (final IllegalStateException ignored) {
            return null;
        }
    }

    @Override
    public ServiceName getName() {
        return dependency.getName();
    }

    public ServiceControllerImpl<?> getDependencyController() {
        return dependency.getDependencyController();
    }

    @Override
    public void immediateDependencyAvailable(final ServiceName dependencyName) {
        available = true;
        final Dependent dependent = this.dependent;
        if (dependent != null) dependent.immediateDependencyDown();
    }

    @Override
    public void immediateDependencyUnavailable(final ServiceName dependencyName) {
        available = false;
        final Dependent dependent = this.dependent;
        if (dependent != null) dependent.immediateDependencyUp();
    }

    @Override
    public void immediateDependencyUp() {
        final Dependent dependent = this.dependent;
        if (dependent != null) dependent.immediateDependencyUp();
    }

    @Override
    public void immediateDependencyDown() {
        final Dependent dependent = this.dependent;
        if (dependent != null) dependent.immediateDependencyDown();
    }

    @Override
    public void dependencyFailed() {
        final Dependent dependent = this.dependent;
        if (dependent != null) dependent.dependencyFailed();
    }

    @Override
    public void dependencyFailureCleared() {
        final Dependent dependent = this.dependent;
        if (dependent != null) dependent.dependencyFailureCleared();
    }

    @Override
    public ServiceControllerImpl<?> getController() {
        final Dependent dependent = this.dependent;
        return dependent != null ? dependent.getController() : null;
    }

    @Override
    public Lockable getLock() {
        return dependency.getLock();
    }
}
