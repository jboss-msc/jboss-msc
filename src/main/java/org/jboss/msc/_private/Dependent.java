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

import org.jboss.msc.service.ServiceName;
import org.jboss.msc.txn.ServiceContext;
import org.jboss.msc.txn.TaskController;
import org.jboss.msc.txn.Transaction;

/**
 * Dependent service.
 * 
 * @author <a href="mailto:frainone@redhat.com">Flavia Rainone</a>
 * @see DependencyImpl#setDependent(Dependent, Transaction, ServiceContext)
 */
public interface Dependent {

    /**
     * Returns dependent service name.
     */
    public ServiceName getServiceName();

    /**
     * Notifies that a dependency is satisfied (during installation, all dependencies are
     * considered unsatisfied until a dependencySatisfied notification is received).
     * 
     * @param transaction the active transaction
     * @param context     the service context
     * @return the transition task resulting of this notification, if any
     */
    public TaskController<?> dependencySatisfied(Transaction transaction, ServiceContext context);

    /**
     * Notifies that a dependency no longer satisfied.
     * 
     * @param transaction the active transaction
     * @param context     the service context
     * @return the transition task resulting of this notification, if any
     */
    public TaskController<?> dependencyUnsatisfied(Transaction transaction, ServiceContext context);

}