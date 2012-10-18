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

import java.util.Collection;

/**
 * The possible values for a dependency flag.  The special value {@link #NONE} is provided
 * for varargs use when no flags are desired.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
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

    ;

    public final boolean in(DependencyFlag flag) {
        return this == flag;
    }

    public final boolean in(DependencyFlag flag1, DependencyFlag flag2) {
        return this == flag1 || this == flag2;
    }

    public final boolean in(DependencyFlag flag1, DependencyFlag flag2, DependencyFlag flag3) {
        return this == flag1 || this == flag2 || this == flag3;
    }

    public final boolean in(DependencyFlag flag1, DependencyFlag flag2, DependencyFlag flag3, DependencyFlag flag4) {
        return this == flag1 || this == flag2 || this == flag3 || this == flag4;
    }

    public final boolean in(DependencyFlag... flags) {
        for (DependencyFlag flag : flags) {
            if (this == flag) return true;
        }
        return false;
    }

    public final boolean in(Collection<? super DependencyFlag> flags) {
        return flags.contains(this);
    }

    /**
     * An array containing no flags.
     */
    public static final DependencyFlag[] NONE = new DependencyFlag[0];

    /**
     * Determine whether a flag combination is valid.
     *
     * @param flags the flags to check
     * @throws IllegalArgumentException if the combination is invalid
     */
    public static void validate(DependencyFlag... flags) throws IllegalArgumentException {
        boolean anti = false, required = false, unrequired = false, optional = false, undemanded = false, demanded = false;
        for (DependencyFlag flag : flags) {
            if (flag == OPTIONAL && anti || flag == ANTI && optional) {
                throw new IllegalArgumentException("ANTI cannot coexist with OPTIONAL");
            }
            if (flag == REQUIRED && unrequired || flag == UNREQUIRED && required) {
                throw new IllegalArgumentException("REQUIRED cannot coexist with UNREQUIRED");
            }
            if (optional && flag.in(REQUIRED, UNREQUIRED, ANTI) || required && flag.in(OPTIONAL, UNREQUIRED, ANTI) || (anti || unrequired) && flag.in(OPTIONAL, REQUIRED)) {
                throw new IllegalArgumentException("Only one of REQUIRED, UNREQUIRED, or OPTIONAL may be given (ANTI implies UNREQUIRED)");
            }
            if (flag == DEMANDED && undemanded || flag == UNDEMANDED && demanded) {
                throw new IllegalArgumentException("DEMANDED cannot coexist with UNDEMANDED");
            }
        }
    }
}
