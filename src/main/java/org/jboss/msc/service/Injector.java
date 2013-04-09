/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013 Red Hat, Inc., and individual contributors
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

import java.util.concurrent.atomic.AtomicReference;

/**
 * An injector. Every service that wants to inject some dependencies have to use this class.
 * This class is thread safe.
 *
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
public final class Injector<T> {

    private final AtomicReference<T> value;

    /**
     * Constructs new injector with {@code null} default value.
     */
    public Injector() {
        this(null);
    }

    /**
     * Constructs new injector with specified initial value.
     *
     * @param initialValue initial value
     */
    public Injector(final T initialValue) {
        value = new AtomicReference<T>(initialValue);
    }

    /**
     * Sets new value. This method will be invoked by the container to inject the satisfied dependencies.
     * This method will be also called on service removal during the uninject phase with {@code null} parameter.
     *
     * @param newValue new value
     */
    public void set(final T newValue) {
        value.set(newValue);
    }

    /**
     * Gets the current value. This method guarantees to return injected value if service have been properly
     * installed to the container.
     *
     * @return the value associated with this object
     */
    public T get() {
        return value.get();
    }

}
