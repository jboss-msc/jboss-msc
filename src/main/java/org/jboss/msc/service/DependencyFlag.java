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
 * Dependency flags.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 * @author <a href="mailto:frainone@redhat.com">Flavia Rainone</a>
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
public enum DependencyFlag {
    /**
     * A missing dependency will cause a transaction error.
     */
    REQUIRED,
    /**
     * A missing dependency will not cause a transaction error. 
     */
    OPTIONAL,
    /**
     * Do not place a demand on this dependency. Overrides default mode behavior.
     */
    UNDEMANDED,
    /**
     * Always place a demand on this dependency. Overrides default mode behavior.
     */
    DEMANDED,
    /**
     * Treat the dependency as a parent; that is, when the dependency is stopped, this service should be removed.  Be
     * sure to consider what happens if the parent re-starts however - this flag should only be used when the parent
     * adds the service as part of its start process.  Implies {@link #REQUIRED}.
     */
    PARENT,
    ;
}