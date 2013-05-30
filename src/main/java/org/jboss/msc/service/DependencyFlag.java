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
 * @author <a href="mailto:frainone@redhat.com">Flavia Rainone</a>
 */
public enum DependencyFlag {

    /**
     * The dependency is met when the target is down, rather than up.  Implies {@link #UNREQUIRED}.  Injection is
     * not allowed for {@code ANTI} dependencies.
     */
    ANTI(false, false, false),
    /**
     * A missing dependency will cause a transaction error.  This is implied by default.
     */
    REQUIRED (true, true, true),
    /**
     * A missing dependency will not cause an error, though unmet dependencies will still prevent start.
     */
    UNREQUIRED (true, true, false),
    /**
     * Start without the dependency when the dependency is not met at the time the service is otherwise ready.
     * Cannot coexist with {@link #ANTI}.
     */
    OPTIONAL (true, false, false) {
        <T> Dependency<T> decorate(Dependency<T> dependency, ServiceBuilderImpl<?> serviceBuilder) {
            return new OptionalDependency<T>(dependency);
        }
    },
    /**
     * Do not place a demand on this dependency even if the mode otherwise would.
     */
    UNDEMANDED (true, false, false),
    /**
     * Always place a demand on this dependency even if the mode otherwise wouldn't.
     */
    DEMANDED (true, true, false),
    /**
     * Treat the dependency as a parent; that is, when the dependency is stopped, this service should be removed.  Be
     * sure to consider what happens if the parent re-starts however - this flag should only be used when the parent
     * adds the service as part of its start process.  Implies {@link #REQUIRED}.
     */
    PARENT (true, true, false) {
        @Override
        <T> Dependency<T> decorate(Dependency<T> dependency, ServiceBuilderImpl<?> serviceBuilder) {
            return new ParentDependency<T>(dependency, serviceBuilder);
        }
    },
    /**
     * Indicate that the dependency can be replaced without stopping this service.
     */
    REPLACEABLE (true, false, false) {
        @Override
        <T> Dependency<T> decorate(Dependency<T> dependency, ServiceBuilderImpl<?> serviceBuilder) {
            return new ReplaceableDependency<T>(dependency);
        }
    },
    ;

    /**
     * Indicates if this flag requires the dependency to be up. If false, indicates that this dependency requires it be
     * down in order for the dependency to be satisfied.
     */
    private final boolean dependencyUp;
    /**
     * Indicates if the dependency should be demanded to be satisfied when service is attempting to start.
     */
    private final boolean demand;
    /**
     * Indicates if the dependency should be satisfied by the end of the transaction, under the penalty of invalidating
     * the transaction if not satisfied.
     */
    private final boolean enforce;

    private DependencyFlag(boolean dependencyUp, boolean demand, boolean enforce) {
        this.dependencyUp = dependencyUp;
        this.demand = demand;
        this.enforce = enforce;
    }

    <T> Dependency<T> decorate(Dependency<T> dependency, ServiceBuilderImpl<?> serviceBuilder) {
        return dependency;
    }

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
        boolean anti = false, required = false, unrequired = false, optional = false, undemanded = false, demanded = false, parent = false, replaceable = false;
        for (DependencyFlag flag : flags) {
            if (optional && flag.in(REQUIRED, PARENT, UNREQUIRED, ANTI) || (parent || required) && flag.in(OPTIONAL, UNREQUIRED, ANTI) || (anti || unrequired) && flag.in(OPTIONAL, REQUIRED, PARENT)) {
                throw new IllegalArgumentException("Only one of REQUIRED, UNREQUIRED, or OPTIONAL may be given (ANTI implies UNREQUIRED, PARENT implies REQUIRED)");
            }
            if (flag == DEMANDED && undemanded || flag == UNDEMANDED && demanded) {
                throw new IllegalArgumentException("DEMANDED cannot coexist with UNDEMANDED");
            }
            if (flag == ANTI) anti = true;
            if (flag == REQUIRED) required = true;
            if (flag == UNREQUIRED) unrequired = true;
            if (flag == OPTIONAL) optional = true;
            if (flag == UNDEMANDED) undemanded = true;
            if (flag == DEMANDED) demanded = true;
            if (flag == PARENT) parent = true;
            if (flag == REPLACEABLE) replaceable = true;
        }
        if (optional && ! replaceable) {
            throw new IllegalArgumentException("OPTIONAL requires REPLACEABLE");
        }
    }

    /**
     * Indicates if the dependency is required to be {@code UP} or {@code DOWN}.
     * 
     * @param flags flags that specify the dependency
     * @return {@code true} if the dependency is expected to be {@code UP}; {@code false} if the dependency is expected
     *         to be {@code DOWN}
     */
    static boolean isDependencyUpExpected(DependencyFlag... flags) {
        for (DependencyFlag flag: flags) {
            if (!flag.dependencyUp) {
                return false;
            }
        }
        // notice that, when there are no flags, the service is required to be UP
        return true;
    }

    /**
     * Indicates if demand should be propagated to dependencies.
     * 
     * @param flags that specify the dependency
     * @return  {@code} if dependency should be demanded
     */
    static boolean propagateDemand(DependencyFlag... flags) {
        for (DependencyFlag flag: flags) {
            if (!flag.demand) {
                return false;
            }
        }
        // notice that, when there are no flags, the service is required to propagate demand requests
        return true;
    }

    /**
     * Indicates if dependency must be enforced to be satisfied by the end of the transaction. Required dependencies
     * that are not satisfied by the end of the transaction will invalidate it.
     * 
     * @param flags flags that specify the dependency
     * @return {@code true} if dependency is required to be satisfied
     */
    static boolean isDependencyRequired(DependencyFlag... flags) {
        for (DependencyFlag flag: flags) {
            if (flag.enforce) {
                return true;
            }
        }
        // notice that, when there are no flags, the dependency is required by default
        return flags.length == 0? true: false;
    }

    /**
     * Decorates {@code dependency} with the special requirements posed by {@code flags}.
     * 
     * @param dependency     the dependency
     * @param serviceBuilder dependency builder
     * @param flags          the flags that could contain special requirements
     * @return the decorated dependency
     */
    static <T> Dependency<T> decorate(Dependency<T> dependency, ServiceBuilderImpl<?> serviceBuilder, DependencyFlag... flags) {
        for (DependencyFlag flag: flags) {
            dependency = flag.decorate(dependency, serviceBuilder);
        }
        return dependency;
    }
}