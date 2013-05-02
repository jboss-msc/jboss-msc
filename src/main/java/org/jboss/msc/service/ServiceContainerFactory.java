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

/**
 * A {@link ServiceContainerImpl} factory. This singleton is thread safe.
 *
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
public final class ServiceContainerFactory {

    private static final ServiceContainerFactory instance = new ServiceContainerFactory();

    /**
     * Gets container factory instance.
     *
     * @return container factory instance
     */
    public static ServiceContainerFactory getInstance() {
        return instance;
    }

    private ServiceContainerFactory() {}

    /**
     * Creates new service container.
     *
     * @return a reference to this object
     */
    public ServiceContainer newServiceContainer() {
        return new ServiceContainerImpl();
    }

}
