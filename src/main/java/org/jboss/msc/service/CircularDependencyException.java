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

/**
 * Exception used to indicate there was a circular dependency discovered during resolution.
 * 
 * @author John Bailey
 */
public class CircularDependencyException extends ServiceRegistryException {

    private static final long serialVersionUID = -4826336558749601678L;

    private ServiceName[] cycle;

    /**
     * Constructs a {@code CircularDependencyException} with the specified detail message. The cause is not initialized, and
     * may subsequently be initialized by a call to {@link #initCause(Throwable) initCause}.
     *
     * @param msg the detail message
     */
    public CircularDependencyException(final String msg, ServiceName[] cycle) {
        super(msg);
        this.cycle = cycle;
    }

    /**
     * Returns a cycle found during service installation.
     * 
     * @return an array formed by the service names involved in the cycle, in dependency order. Last name in the array
     *         has a dependency on the name in the first position, thus completing the cycle.
     */
    public ServiceName[] getCycle() {
        return cycle;
    }
}
