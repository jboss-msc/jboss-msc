/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jboss.msc.service;

import org.jboss.msc.value.WritableValue;

final class Dependency {
    private final Registration dependencyRegistration;
    private final DependencyFlag[] flags;
    private final WritableValue<?>[] injections;

    @SuppressWarnings("unchecked")
    static final WritableValue<?>[] NO_INJECTIONS = new WritableValue[0];

    Dependency(final WritableValue<?>[] injections, final DependencyFlag[] flags, final Registration dependencyRegistration) {
        this.injections = injections;
        this.flags = flags;
        this.dependencyRegistration = dependencyRegistration;
    }

    public Registration getDependencyRegistration() {
        return dependencyRegistration;
    }

    public DependencyFlag[] getFlags() {
        return flags;
    }

    public WritableValue<?>[] getInjections() {
        return injections;
    }
}
