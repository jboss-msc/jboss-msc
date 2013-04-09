/*
 * JBoss, Home of Professional Open Source
 *
 * Copyright 2013 Red Hat, Inc. and/or its affiliates.
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
 * Parent dependency decorator. The dependent is created whenever dependency is satisfied, and is removed whenever
 * dependency is no longer satisfied.
 * 
 * @author <a href="mailto:frainone@redhat.com">Flavia Rainone</a>
 *
 */
final class ParentDependency<T> extends DependencyDecorator<T> {

    @SuppressWarnings("unused")
    private final ServiceBuilderImpl<?> childServiceBuilder;

    public ParentDependency(Dependency<T> dependency, ServiceBuilderImpl<?> childServiceBuilder) {
        super(dependency);
        this.childServiceBuilder = childServiceBuilder;
    }

    // TODO install child when dependency is satisfied, and remove child when dependency is unsatisfied
}
