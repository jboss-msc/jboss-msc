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

import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;

import static org.jboss.msc.txn.Bits.*;

/**
 * A controller for an installed subtask.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class TaskController<T> {

    private final Transaction transaction;
    private final TaskController<?>[] dependencies;
    private final Executable<T> executable;
    private final Revertible revertible;
    private final Validatable validatable;
    private final Committable committable;
    private final LittleIdentitySet<TaskController<?>> dependents = new LittleIdentitySet<TaskController<?>>();

    private volatile int state;
    private T result;

    private static final AtomicIntegerFieldUpdater<TaskController> stateUpdater = AtomicIntegerFieldUpdater.newUpdater(TaskController.class, "state");

    private static final int STATE_DEAD = 0;
    private static final int STATE_WAIT_DEPS = 1;
    private static final int STATE_INJECTING = 2;
    private static final int STATE_EXECUTING = 3;
    private static final int STATE_FAILED = 4;
    private static final int STATE_COMPLETE = 5; // aka wait-validate...
    private static final int STATE_WAIT_ROLLBACK = 6;
    private static final int STATE_ROLLBACK = 7;
    private static final int STATE_VALIDATING = 8;
    private static final int STATE_WAIT_COMMIT = 9;
    private static final int STATE_COMMITTING = 10;
    private static final int STATE_DONE = 11;

    private static final int FLAG_CANCEL_REQ = 1 << 4;
    private static final int FLAG_ROLLBACK_REQ = 1 << 5;
    private static final int FLAG_VALIDATE_REQ = 1 << 6;
    private static final int FLAG_COMMIT_REQ = 1 << 7;

    /**
     * Get the transaction associated with this controller.
     *
     * @return the transaction associated with this controller
     */
    public Transaction getTransaction() {
        return transaction;
    }

    private boolean checkCancel() {
        return allAreSet(state, FLAG_CANCEL_REQ);
    }

    private static int stateOf(int oldVal) {
        return oldVal & 0xf;
    }

    private static int withState(int oldVal, int state) {
        assert (state & 0xf) == state;
        return oldVal & ~0xf | state;
    }

    private boolean compareAndSetState(int expect, int update) {
        return stateUpdater.compareAndSet(this, expect, update);
    }

    private void execComplete(final T result) {
        int oldVal, newVal;
        do {
            oldVal = state;
            if (stateOf(oldVal) != STATE_EXECUTING) {
                throw new IllegalStateException("Task is not currently executing");
            }
            if (allAreSet(oldVal, FLAG_ROLLBACK_REQ)) {
                // -> rollback_wait
            } else {

            }
        } while (! compareAndSetState(oldVal, newVal));
    }

    private void execCancelled() {

    }

    private void execFailed(final Failure reason) {

    }

    void execute() {
        final Executable<T> exec = executable;
        if (exec != null) {
            exec.execute(new ExecuteContext<T>() {
                public void complete(final T result) {
                    execComplete(result);
                }

                public boolean isCancelRequested() {
                    return checkCancel();
                }

                public void cancelled() {
                    execCancelled();
                }

                public void failed(final Failure reason) {
                    execFailed(reason);
                }

                public void begin() {

                }

                public void end() {
                }

                public Transaction getTransaction() {
                    return transaction;
                }
            });
        }
    }

    void addDependent(TaskController<?> controller) {

    }
}
