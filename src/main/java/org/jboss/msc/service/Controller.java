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

import org.jboss.msc.value.AtomicValue;
import org.jboss.msc.value.ReadableValue;
import org.jboss.msc.value.WritableValue;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
final class Controller<T> {
    private final Registration primaryRegistration;
    private final Registration[] aliasRegistrations;
    private final Dependency[] dependencies;
    private final AtomicValue<T> value = new AtomicValue<T>();
    private volatile ServiceMode mode = ServiceMode.NEVER;
    private volatile State state = State.NEW;

    Controller(final Dependency[] dependencies, final Registration[] aliasRegistrations, final Registration primaryRegistration) {
        this.dependencies = dependencies;
        this.aliasRegistrations = aliasRegistrations;
        this.primaryRegistration = primaryRegistration;
    }

    public Registration getPrimaryRegistration() {
        return primaryRegistration;
    }

    public Registration[] getAliasRegistrations() {
        return aliasRegistrations;
    }

    public Dependency[] getDependencies() {
        return dependencies;
    }

    public ReadableValue<T> getValue() {
        return value;
    }

    WritableValue<T> getWriteValue() {
        return value;
    }

    enum State {
        DOWN,
        STARTING,
        UP,
        STOPPING,
        NEW,
        REMOVING,
        REMOVED,
        ;
    }
}
