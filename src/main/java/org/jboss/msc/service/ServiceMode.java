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
 * A service mode.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public enum ServiceMode {
    /**
     * Start as soon as all dependencies are met.  Demands dependencies.
     */
    ACTIVE,
    /**
     * Start as soon as all dependencies are met.  Does not demand dependencies.
     */
    PASSIVE,
    /**
     * Start only when demanded to, but stay running until required to stop.
     */
    LAZY,
    /**
     * Start only when demanded to, and stop when demands disappear.
     */
    ON_DEMAND,
    /**
     * Do not start; stay in a stopped state.
     */
    NEVER,
    ;
}
