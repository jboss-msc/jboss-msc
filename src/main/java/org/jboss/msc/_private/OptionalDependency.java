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
package org.jboss.msc._private;

/**
 * OptionalDependencyDecorator.<br>This class establishes a transitive dependency relationship between the
 * dependent and the real dependency. The intermediation performed by this class adds the required optional
 * behavior to the dependency relation, by:
 * <ul>
 * <li> notifying the dependent that it is satisfied if dependency is not installed</li>
 * <li> once the real dependency is installed, if there is a demand previously added by the dependent, this dependency
 *      does not start forwarding the notifications to the dependent, meaning that the dependent won't even be aware
 *      that the dependency is down</li>
 * <li> waits for the dependency to be satisfied and the dependent to be down, so it can finally start forwarding
 *      notifications in both directions (from dependency to dependent and vice-versa)</li>
 * </ul>
 * 
 * @author <a href="mailto:frainone@redhat.com">Flavia Rainone</a>
 */ 
final class OptionalDependency<T> extends DependencyDecorator<T> {

    public OptionalDependency(Dependency<T> dependency) {
        super(dependency);
    }

    // TODO toggle connected on and off according to notifications
}
