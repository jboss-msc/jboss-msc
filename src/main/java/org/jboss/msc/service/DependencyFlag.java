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
 * The possible values for a dependency flag.  The special value {@link #NONE} is provided
 * for varargs use when no flags are desired.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 * @author <a href="mailto:frainone@redhat.com">Flavia Rainone</a>
 */
public enum DependencyFlag {

    /**
     * The dependency is met when the target is down, rather than up.  Implies {@link #UNREQUIRED}.  Injection is
     * not allowed for {@code ANTI} dependencies.
     */
    ANTI,
    /**
     * A missing dependency will cause a transaction error.  This is implied by default.
     */
    REQUIRED,
    /**
     * A missing dependency will not cause an error, though unmet dependencies will still prevent start.
     */
    UNREQUIRED,
    /**
     * Start without the dependency when the dependency is not met at the time the service is otherwise ready.
     * Cannot coexist with {@link #ANTI}.
     */
    OPTIONAL,
    /**
     * Do not place a demand on this dependency even if the mode otherwise would.
     */
    UNDEMANDED,
    /**
     * Always place a demand on this dependency even if the mode otherwise wouldn't.
     */
    DEMANDED,
    /**
     * Treat the dependency as a parent; that is, when the dependency is stopped, this service should be removed.  Be
     * sure to consider what happens if the parent re-starts however - this flag should only be used when the parent
     * adds the service as part of its start process.  Implies {@link #REQUIRED}.
     */
    PARENT,
    /**
     * Indicate that the dependency can be replaced without stopping this service.
     */
    REPLACEABLE,
    ;
}