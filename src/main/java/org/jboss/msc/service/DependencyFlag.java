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
 * Dependency flags.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 * @author <a href="mailto:frainone@redhat.com">Flavia Rainone</a>
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
public enum DependencyFlag {
    /**
     * A missing dependency will cause a transaction error. This is implied by default.
     */
    REQUIRED,
    /**
     * A missing dependency will not cause a transaction error.
     */
    UNREQUIRED,
    /**
     * Always place a demand on this dependency. Overrides default mode behavior.
     */
    DEMANDED,
    /**
     * Do not place a demand on this dependency. Overrides default mode behavior.
     */
    UNDEMANDED,
    /**
     * Treat the dependency as a parent. That is when the dependency is stopped this service should be removed.
     * Implies {@link #REQUIRED}.
     */
    PARENT,
    ;

    /**
     * Returns true if the specified dependency flag is identical to this enum constant.
     * @param other the dependency flag to be compared for identity with this object
     * @return true if the specified dependency flag is identical to this enum constant
     */
    public final boolean in(final DependencyFlag other) {
        return this == other;
    }

    /**
     * Returns true if this enum constant is identical to any of the specified dependency flags.
     * @param other1 first dependency flag to be compared for identity with this object
     * @param other2 second dependency flag to be compared for identity with this object
     * @return true if this enum constant is identical to any of the specified dependency flags 
     */
    public final boolean in(final DependencyFlag other1, final DependencyFlag other2) {
        return this == other1 || this == other2;
    }

    /**
     * Returns true if this enum constant is identical to any of the specified dependency flags.
     * @param other1 first dependency flag to be compared for identity with this object
     * @param other2 second dependency flag to be compared for identity with this object
     * @param other3 third dependency flag to be compared for identity with this object
     * @return true if this enum constant is identical to any of the specified dependency flags 
     */
    public final boolean in(final DependencyFlag other1, final DependencyFlag other2, final DependencyFlag other3) {
        return this == other1 || this == other2 || this == other3;
    }

    /**
     * Returns true if this enum constant is identical to any of the specified dependency flags.
     * @param others dependency flags to be compared for identity with this object
     * @return true if this enum constant is identical to any of the specified dependency flags 
     */
    public final boolean in(final DependencyFlag... others) {
        if (others != null) {
            for (final DependencyFlag other : others) {
                if (this == other) return true;
            }
        }
        return false;
    }

    /**
     * Returns true if this enum constant is identical to any of the specified dependency flags.
     * @param others dependency flags to be compared for identity with this object
     * @return true if this enum constant is identical to any of the specified dependency flags 
     */
    public final boolean in(final Collection<DependencyFlag> others) {
        return others != null ? others.contains(this) : false;
    }

}

