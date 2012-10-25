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
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

import static java.util.concurrent.locks.LockSupport.park;
import static org.jboss.msc.txn.Bits.*;

/**
 * A controller for an installed subtask.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
final class TaskControllerImpl<T> extends TaskController<T> implements TaskDependency, TaskDependent {

    private final TaskDependency[] dependencies;
    private final Executable<T> executable;
    private final Revertible revertible;
    private final Validatable validatable;
    private final Committable committable;
    private final LittleIdentitySet<TaskControllerImpl<?>> dependents = new LittleIdentitySet<TaskControllerImpl<?>>();

    private volatile int state;
    private volatile Thread waiter;
    private T result;

    private static final AtomicIntegerFieldUpdater<TaskControllerImpl> stateUpdater = AtomicIntegerFieldUpdater.newUpdater(TaskControllerImpl.class, "state");
    private static final AtomicReferenceFieldUpdater<TaskControllerImpl, Thread> waiterUpdater = AtomicReferenceFieldUpdater.newUpdater(TaskControllerImpl.class, Thread.class, "waiter");

    private static final int SPINS;

    static {
        if (Runtime.getRuntime().availableProcessors() < 2) {
            SPINS = 0;
        } else {
            SPINS = 200;
        }
    }

    private static final int STATE_VALIDATE_FAILED = -5;
    private static final int STATE_FAILED = -3;
    private static final int STATE_ROLLBACK_WAIT = -2;
    private static final int STATE_ROLLBACK = -1;
    private static final int STATE_DEAD = -4;
    private static final int STATE_EXECUTE_WAIT = 0;
    private static final int STATE_EXECUTE = 1;
    private static final int STATE_VALIDATE_WAIT = 2;
    private static final int STATE_VALIDATE = 3;
    private static final int STATE_COMMIT_WAIT = 4;
    private static final int STATE_COMMIT = 5;
    private static final int STATE_COMMITTED = 6;

    private static final int FLAG_CANCEL_REQ = 1 << 4;
    private static final int FLAG_ROLLBACK_REQ = 1 << 5;
    private static final int FLAG_VALIDATE_REQ = 1 << 6;
    private static final int FLAG_COMMIT_REQ = 1 << 7;
    private static final int FLAG_LOCKED = 1 << 31;

    TaskControllerImpl(final Transaction transaction, final TaskDependency[] dependencies, final Executable<T> executable, final Revertible revertible, final Validatable validatable, final Committable committable) {
        super(transaction);
        this.dependencies = dependencies;
        this.executable = executable;
        this.revertible = revertible;
        this.validatable = validatable;
        this.committable = committable;
    }

    public <T> TaskBuilder<T> newTask(final Executable<T> task) throws IllegalStateException {
        return null;
    }

    public TaskBuilder<Void> newTask() throws IllegalStateException {
        return null;
    }

    public T getResult() throws IllegalStateException {
        return null;
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

    }

    private void execCancelled() {

    }

    private void execFailed(final Problem reason) {

    }

    void execute() {
        final Executable<T> exec = executable;
        if (exec != null) {
            exec.execute(new ExecuteContext<T>() {
                public void complete(final T result) {
                    execComplete(result);
                }

                public void complete() {
                    complete(null);
                }

                public void failed() {
                }

                public boolean isCancelRequested() {
                    return checkCancel();
                }

                public void cancelled() {
                    execCancelled();
                }

                public void addProblem(final Problem reason) {
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

    void addDependent(TaskControllerImpl<?> controller) {
        synchronized (dependents) {
            if (dependents.add(controller)) {
                // if state is exec_wait o
            }
        }
    }

    public void dependentExecutionFinished() {
    }

    public void dependentValidationFinished() {
    }

    public void dependentRollbackFinished() {
    }

    public void dependentCommitFinished() {
        // a dependent has finished its commit operation.
    }

    public void dependencyExecutionComplete() {
        // another dependency has completed execution, thus getting us one step closer to having permission to execute.

    }

    public void dependencyValidateComplete() {

    }

    public void dependencyCommitComplete() {
    }

    public void dependentInitiateRollback() {
        // we're requested to roll back by the transaction.
        // TODO: get lock..
        int state = stateOf(this.state);
        if (state == STATE_ROLLBACK || state == STATE_ROLLBACK_WAIT || state == STATE_DEAD) {
            // nada
            return;
        }
        assert state >= STATE_EXECUTE_WAIT && state <= STATE_COMMIT_WAIT || state == STATE_VALIDATE_FAILED;
        // TODO: set state = STATE_ROLLBACK_WAIT
        for (TaskControllerImpl<?> dependent : dependents) {
            dependent.dependentInitiateRollback();
        }
        // TODO: set state = STATE_ROLLBACK

        // TODO: release lock..
    }

    public void dependentInitiateValidate() {
        int state = stateOf(this.state);
        assert state == STATE_VALIDATE_WAIT;

    }

    public void dependentInitiateCommit() {

    }
}
