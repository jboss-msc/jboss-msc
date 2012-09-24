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

package org.jboss.msc.txn;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class ChildTransaction extends Transaction {
    private final Transaction parent;
    private final List<TaskController<?>> rootControllers = new ArrayList<TaskController<?>>(); // controllers with no dependencies in this txn

    ChildTransaction(final Transaction parent) {
        this.parent = parent;
    }

    /**
     * Get the parent transaction.
     *
     * @return the parent transaction
     */
    public Transaction getParent() {
        return parent;
    }

    <T> TaskBuilder<T> newSubtask(final Executable<T> executable, final Object subtask, final Transaction owner) throws IllegalStateException {
        return getParent().newSubtask(executable, subtask, owner);
    }

    public Executor getExecutor() {
        return getParent().getExecutor();
    }
}
