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

import static java.lang.Thread.holdsLock;
import static org.jboss.msc.txn.Bits.*;

/**
 * A controller for an installed subtask.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
final class TaskControllerImpl<T> extends TaskController<T> implements TaskParent, TaskChild {

    private static final Object NO_RESULT = new Object();

    private final TaskParent parent;
    private final TaskControllerImpl<?>[] dependencies;
    private final Executable<T> executable;
    private final Revertible revertible;
    private final Validatable validatable;
    private final Committable committable;
    private final LittleIdentitySet<Thread> threads = new LittleIdentitySet<Thread>();
    private final ArrayList<TaskControllerImpl<?>> dependents = new ArrayList<TaskControllerImpl<?>>();
    private final ArrayList<TaskChild> children = new ArrayList<TaskChild>();

    private int state;
    private int unfinishedDependencies;
    private int unfinishedChildren;
    private int unvalidatedChildren;
    private int uncommittedDependencies;
    private int uncommittedChildren;
    private int unrevertedDependents;

    @SuppressWarnings("unchecked")
    private volatile T result = (T) NO_RESULT;

    private static final int STATE_MASK        = 0xF;

    private static final int STATE_NEW              = 0;
    private static final int STATE_EXECUTE_WAIT     = 1;
    private static final int STATE_EXECUTE          = 2;
    private static final int STATE_EXECUTE_DONE     = 3;
    private static final int STATE_VALIDATE         = 4;
    private static final int STATE_VALIDATE_DONE    = 5;
    private static final int STATE_COMMIT_WAIT      = 6;
    private static final int STATE_COMMIT           = 7;
    private static final int STATE_COMMIT_DONE      = 8;
    private static final int STATE_ROLLBACK_WAIT    = 9;
    private static final int STATE_ROLLBACK         = 10;
    private static final int STATE_TERMINATED       = 11;
    private static final int STATE_LAST = STATE_TERMINATED;

    private static final int T_NONE = 0;

    private static final int T_NEW_to_EXECUTE_WAIT = 1;
    private static final int T_NEW_to_CANCELLED = 2;

    private static final int T_EXECUTE_WAIT_to_TERMINATED = 3;
    private static final int T_EXECUTE_WAIT_to_EXECUTE = 4;

    private static final int T_EXECUTE_to_ROLLBACK = 5;
    private static final int T_EXECUTE_to_TERMINATED = 6;
    private static final int T_EXECUTE_to_EXECUTE_DONE = 7;

    private static final int T_EXECUTE_DONE_to_ROLLBACK_WAIT = 8;
    private static final int T_EXECUTE_DONE_to_VALIDATE = 9;

    private static final int T_VALIDATE_to_ROLLBACK_WAIT = 10;
    private static final int T_VALIDATE_to_VALIDATE_DONE = 11;

    private static final int T_VALIDATE_DONE_to_ROLLBACK_WAIT = 12;
    private static final int T_VALIDATE_DONE_to_COMMIT_WAIT = 13;

    private static final int T_COMMIT_WAIT_to_COMMIT = 14;

    private static final int T_COMMIT_to_COMMIT_DONE = 15;

    private static final int T_COMMIT_DONE_to_TERMINATED = 16;

    private static final int T_ROLLBACK_WAIT_to_ROLLBACK = 17;

    private static final int T_ROLLBACK_to_TERMINATED = 18;

    /**
     * A cancel request, due to rollback or dependency failure.
     */
    private static final int FLAG_CANCEL_REQ    = 1 << 4;
    private static final int FLAG_ROLLBACK_REQ  = 1 << 5;
    private static final int FLAG_VALIDATE_REQ  = 1 << 6;
    private static final int FLAG_COMMIT_REQ    = 1 << 7;

    private static final int PERSISTENT_STATE = STATE_MASK | FLAG_CANCEL_REQ | FLAG_ROLLBACK_REQ | FLAG_VALIDATE_REQ | FLAG_COMMIT_REQ;

    // non-persistent status flags
    private static final int FLAG_EXECUTE_DONE          = 1 << 8;
    private static final int FLAG_EXECUTE_FAILED        = 1 << 9;
    private static final int FLAG_EXECUTE_CANCELLED     = 1 << 10;
    private static final int FLAG_VALIDATE_DONE         = 1 << 11;
    private static final int FLAG_VALIDATE_CANCELLED    = 1 << 12;
    private static final int FLAG_COMMIT_DONE           = 1 << 13;
    private static final int FLAG_ROLLBACK_DONE         = 1 << 14;
    private static final int FLAG_INSTALL_FAILED        = 1 << 15;

    // non-persistent job flags
    private static final int FLAG_SEND_CHILD_DONE           = 1 << 16; // to parents
    private static final int FLAG_SEND_DEPENDENCY_DONE      = 1 << 17; // to dependents
    private static final int FLAG_SEND_VALIDATE_REQ         = 1 << 18; // to children
    private static final int FLAG_SEND_CHILD_VALIDATE_DONE  = 1 << 19; // to parents
    private static final int FLAG_SEND_COMMIT_DONE          = 1 << 20; // to dependents
    private static final int FLAG_SEND_CHILD_COMMIT_DONE    = 1 << 21; // to parents
    private static final int FLAG_SEND_ROLLBACK_DONE        = 1 << 22; // to dependencies
    private static final int FLAG_SEND_CHILD_ROLLBACK_DONE  = 1 << 23; // to parents
    private static final int FLAG_SEND_CANCEL_DEPENDENTS    = 1 << 24; // to dependents

    private static final int SEND_FLAGS = 0x1FF << 16;

    private static final int FLAG_DO_EXECUTE        = 1 << 25;
    private static final int FLAG_DO_VALIDATE       = 1 << 26;
    private static final int FLAG_DO_COMMIT         = 1 << 27;
    private static final int FLAG_DO_ROLLBACK       = 1 << 28;

    private static final int DO_FLAGS = 0xF << 25;

    @SuppressWarnings("unused")
    private static final int TASK_FLAGS = DO_FLAGS | SEND_FLAGS;

    private static final int FLAG_USER_THREAD       = 1 << 31; // called from user thread; do not block

    TaskControllerImpl(final TaskParent parent, final TaskControllerImpl<?>[] dependencies, final Executable<T> executable, final Revertible revertible, final Validatable validatable, final Committable committable) {
        this.parent = parent;
        this.dependencies = dependencies;
        this.executable = executable;
        this.revertible = revertible;
        this.validatable = validatable;
        this.committable = committable;
        state = STATE_NEW;
    }

    public <T> TaskBuilder<T> newTask(final Executable<T> task) throws IllegalStateException {
        return new TaskBuilder<T>(getTransaction(), this, task);
    }

    public TaskBuilder<Void> newTask() throws IllegalStateException {
        return new TaskBuilder<Void>(getTransaction(), this);
    }

    public TransactionImpl getTransaction() {
        return parent.getTransaction();
    }

    public T getResult() throws IllegalStateException {
        final T result = this.result;
        if (result == NO_RESULT) {
            throw new IllegalStateException("No result is available");
        }
        return result;
    }

    // ===================================================
    //   Private impl
    // ===================================================

    /**
     * Annotate an unlikely condition.
     *
     * @param cond the condition
     * @return the condition
     */
    private static boolean unlikely(boolean cond) { return cond; }

    /**
     * Calculate the transition to take from the current state.
     *
     * @param state the current state
     * @return the transition to take
     */
    private int getTransition(int state) {
        assert holdsLock(this);
        int sid = stateOf(state);
        switch (sid) {
            case STATE_NEW: {
                if (allAreSet(state, FLAG_INSTALL_FAILED)) {
                    return T_NEW_to_CANCELLED;
                } else {
                    return T_NEW_to_EXECUTE_WAIT;
                }
            }
            case STATE_EXECUTE_WAIT: {
                if (allAreSet(state, FLAG_CANCEL_REQ)) {
                    // disregard counts
                    return T_EXECUTE_WAIT_to_TERMINATED;
                } else if (unfinishedDependencies == 0) {
                    return T_EXECUTE_WAIT_to_EXECUTE;
                } else {
                    return T_NONE;
                }
            }
            case STATE_EXECUTE: {
                if (allAreSet(state, FLAG_EXECUTE_DONE)) {
                    return T_EXECUTE_to_EXECUTE_DONE;
                } else if (allAreSet(state, FLAG_EXECUTE_FAILED)) {
                    return T_EXECUTE_to_ROLLBACK;
                } else if (allAreSet(state, FLAG_EXECUTE_CANCELLED)) {
                    return T_EXECUTE_to_TERMINATED;
                } else {
                    return T_NONE;
                }
            }
            case STATE_EXECUTE_DONE: {
                if (allAreSet(state, FLAG_ROLLBACK_REQ)) {
                    return T_EXECUTE_DONE_to_ROLLBACK_WAIT;
                } else if (allAreSet(state, FLAG_VALIDATE_REQ) && unfinishedChildren == 0) {
                    return T_EXECUTE_DONE_to_VALIDATE;
                } else {
                    return T_NONE;
                }
            }
            case STATE_VALIDATE: {
                if (allAreSet(state, FLAG_VALIDATE_CANCELLED)) {
                    return T_VALIDATE_to_ROLLBACK_WAIT;
                } else if (allAreSet(state, FLAG_VALIDATE_DONE)) {
                    return T_VALIDATE_to_VALIDATE_DONE;
                } else {
                    return T_NONE;
                }
            }
            case STATE_VALIDATE_DONE: {
                if (allAreSet(state, FLAG_COMMIT_REQ) && unvalidatedChildren == 0) {
                    return T_VALIDATE_DONE_to_COMMIT_WAIT;
                } else if (allAreSet(state, FLAG_ROLLBACK_REQ)) {
                    return T_VALIDATE_DONE_to_ROLLBACK_WAIT;
                } else {
                    return T_NONE;
                }
            }
            case STATE_COMMIT_WAIT: {
                if (uncommittedDependencies == 0) {
                    return T_COMMIT_WAIT_to_COMMIT;
                } else {
                    return T_NONE;
                }
            }
            case STATE_COMMIT: {
                if (allAreSet(state, FLAG_COMMIT_DONE)) {
                    return T_COMMIT_to_COMMIT_DONE;
                } else {
                    return T_NONE;
                }
            }
            case STATE_COMMIT_DONE: {
                if (uncommittedChildren == 0) {
                    return T_COMMIT_DONE_to_TERMINATED;
                } else {
                    return T_NONE;
                }
            }
            case STATE_ROLLBACK_WAIT: {
                if (unrevertedDependents == 0) {
                    return T_ROLLBACK_WAIT_to_ROLLBACK;
                } else {
                    return T_NONE;
                }
            }
            case STATE_ROLLBACK: {
                if (allAreSet(state, FLAG_ROLLBACK_DONE)) {
                    return T_ROLLBACK_to_TERMINATED;
                } else {
                    return T_NONE;
                }
            }
            case STATE_TERMINATED: {
                return T_NONE;
            }
            default: throw new IllegalStateException();
        }
    }

    /**
     * Perform any necessary/possible transition.
     *
     * @param state the current state
     * @return the new state
     */
    private int transition(int state) {
        assert holdsLock(this);
        for (;;) {
            int t = getTransition(state);
            switch (t) {
                case T_NONE: return state;
                case T_NEW_to_EXECUTE_WAIT: {
                    state = newState(STATE_EXECUTE_WAIT, state);
                    continue;
                }
                case T_EXECUTE_WAIT_to_EXECUTE: {
                    if (executable == null) {
                        state = newState(STATE_EXECUTE, state | FLAG_EXECUTE_DONE);
                        continue;
                    }
                    // not possible to go any farther
                    return newState(STATE_EXECUTE, state | FLAG_DO_EXECUTE);
                }
                case T_EXECUTE_to_EXECUTE_DONE: {
                    state = newState(STATE_EXECUTE_DONE, state | FLAG_SEND_CHILD_DONE | FLAG_SEND_DEPENDENCY_DONE);
                    continue;
                }
                case T_EXECUTE_DONE_to_VALIDATE: {
                    if (validatable == null) {
                        state = newState(STATE_VALIDATE, state | FLAG_SEND_VALIDATE_REQ | FLAG_VALIDATE_DONE);
                        continue;
                    }
                    // not possible to go any farther
                    return newState(STATE_VALIDATE, state | FLAG_SEND_VALIDATE_REQ | FLAG_DO_VALIDATE);
                }
                case T_VALIDATE_to_VALIDATE_DONE: {
                    state = newState(STATE_VALIDATE_DONE, state | FLAG_SEND_CHILD_VALIDATE_DONE);
                    continue;
                }
                case T_VALIDATE_DONE_to_COMMIT_WAIT: {
                    state = newState(STATE_COMMIT_WAIT, state);
                    continue;
                }
                case T_COMMIT_WAIT_to_COMMIT: {
                    if (committable == null) {
                        state = newState(STATE_COMMIT, state | FLAG_COMMIT_DONE);
                        continue;
                    }
                    // not possible to go any farther
                    return newState(STATE_COMMIT, state | FLAG_DO_COMMIT);
                }
                case T_COMMIT_to_COMMIT_DONE: {
                    state = newState(STATE_COMMIT_DONE, state | FLAG_SEND_COMMIT_DONE | FLAG_SEND_CHILD_COMMIT_DONE);
                    continue;
                }
                case T_COMMIT_DONE_to_TERMINATED: {
                    state = newState(STATE_TERMINATED, state);
                    continue;
                }

                // exceptional cases

                case T_NEW_to_CANCELLED: {
                    // not possible to go any farther
                    return newState(STATE_TERMINATED, state);
                }
                case T_EXECUTE_WAIT_to_TERMINATED: {
                    // not possible to go any farther
                    return newState(STATE_TERMINATED, state | FLAG_SEND_CANCEL_DEPENDENTS);
                }
                case T_EXECUTE_to_TERMINATED: {
                    // not possible to go any farther
                    return newState(STATE_TERMINATED, state | FLAG_SEND_CANCEL_DEPENDENTS);
                }
                case T_EXECUTE_to_ROLLBACK: {
                    if (revertible == null) {
                        state = newState(STATE_ROLLBACK, state | FLAG_ROLLBACK_DONE | FLAG_SEND_CANCEL_DEPENDENTS);
                        continue;
                    }
                    // not possible to go any farther
                    return newState(STATE_ROLLBACK, state | FLAG_DO_ROLLBACK | FLAG_SEND_CANCEL_DEPENDENTS);
                }
                case T_EXECUTE_DONE_to_ROLLBACK_WAIT: {
                    state = newState(STATE_ROLLBACK_WAIT, state);
                    continue;
                }
                case T_VALIDATE_to_ROLLBACK_WAIT: {
                    state = newState(STATE_ROLLBACK_WAIT, state);
                    continue;
                }
                case T_VALIDATE_DONE_to_ROLLBACK_WAIT: {
                    state = newState(STATE_ROLLBACK_WAIT, state);
                    continue;
                }
                case T_ROLLBACK_WAIT_to_ROLLBACK: {
                    if (revertible == null) {
                        state = newState(STATE_ROLLBACK, state | FLAG_ROLLBACK_DONE);
                        continue;
                    }
                    // not possible to go any farther
                    return newState(STATE_ROLLBACK, state | FLAG_DO_ROLLBACK | FLAG_SEND_CANCEL_DEPENDENTS);
                }
                case T_ROLLBACK_to_TERMINATED: {
                    // not possible to go any farther
                    return newState(STATE_TERMINATED, state | FLAG_SEND_CHILD_ROLLBACK_DONE | FLAG_SEND_ROLLBACK_DONE);
                }
                default: throw new IllegalStateException();
            }
        }
    }

    private void executeTasks(int state) {
        if (allAreSet(state, FLAG_SEND_DEPENDENCY_DONE)) {
            for (TaskControllerImpl<?> dependent : dependents) {
                dependent.dependencyExecutionComplete(this, allAreSet(state, FLAG_USER_THREAD));
            }
        }
        if (allAreSet(state, FLAG_SEND_CHILD_DONE)) {
            parent.childExecutionFinished(this, allAreSet(state, FLAG_USER_THREAD));
        }
        if (allAreSet(state, FLAG_SEND_VALIDATE_REQ)) {
            for (TaskChild child : children) {
                child.childInitiateValidate(allAreSet(state, FLAG_USER_THREAD));
            }
        }
        if (allAreSet(state, FLAG_SEND_CHILD_VALIDATE_DONE)) {
            parent.childValidationFinished(this, allAreSet(state, FLAG_USER_THREAD));
        }
        if (allAreSet(state, FLAG_SEND_COMMIT_DONE)) {
            for (TaskControllerImpl<?> dependent : dependents) {
                dependent.dependencyCommitComplete(this, allAreSet(state, FLAG_USER_THREAD));
            }
        }
        if (allAreSet(state, FLAG_SEND_CHILD_COMMIT_DONE)) {
            parent.childCommitFinished(this, allAreSet(state, FLAG_USER_THREAD));
        }
        if (allAreSet(state, FLAG_SEND_ROLLBACK_DONE)) {
            for (TaskControllerImpl<?> dependency : dependencies) {
                dependency.dependentRollbackDone(allAreSet(state, FLAG_USER_THREAD));
            }
        }
        if (allAreSet(state, FLAG_SEND_CHILD_ROLLBACK_DONE)) {
            parent.childRollbackFinished(this, allAreSet(state, FLAG_USER_THREAD));
        }
        if (allAreSet(state, FLAG_SEND_CANCEL_DEPENDENTS)) {
            for (TaskControllerImpl<?> dependent : dependents) {
                dependent.forceCancel(allAreSet(state, FLAG_USER_THREAD));
            }
        }

        assert allAreClear(state, DO_FLAGS) || oneIsSet(state, DO_FLAGS);

        if (allAreSet(state, FLAG_USER_THREAD)) {
            if (allAreSet(state, FLAG_DO_EXECUTE)) {
                getTransaction().getExecutor().execute(new AsyncTask(FLAG_DO_EXECUTE));
            }
            if (allAreSet(state, FLAG_DO_VALIDATE)) {
                getTransaction().getExecutor().execute(new AsyncTask(FLAG_DO_VALIDATE));
            }
            if (allAreSet(state, FLAG_DO_ROLLBACK)) {
                getTransaction().getExecutor().execute(new AsyncTask(FLAG_DO_ROLLBACK));
            }
            if (allAreSet(state, FLAG_DO_COMMIT)) {
                getTransaction().getExecutor().execute(new AsyncTask(FLAG_DO_COMMIT));
            }
        } else {
            if (allAreSet(state, FLAG_DO_EXECUTE)) {
                execute();
            }
            if (allAreSet(state, FLAG_DO_VALIDATE)) {
                validate();
            }
            if (allAreSet(state, FLAG_DO_ROLLBACK)) {
                rollback();
            }
            if (allAreSet(state, FLAG_DO_COMMIT)) {
                commit();
            }
        }
    }

    private void forceCancel(final boolean userThread) {
        assert ! holdsLock(this);
        int state;
        synchronized (this) {
            state = this.state;
            if (userThread) state |= FLAG_USER_THREAD;
            state = transition(state | FLAG_CANCEL_REQ);
            this.state = state & PERSISTENT_STATE;
        }
        executeTasks(state);
    }

    private void dependentRollbackDone(final boolean userThread) {
        assert ! holdsLock(this);
        int state;
        synchronized (this) {
            unrevertedDependents--;
            state = this.state;
            if (userThread) state |= FLAG_USER_THREAD;
            this.state = state = transition(state);
        }
        executeTasks(state);
    }

    private static int newState(int sid, int state) {
        assert sid >= 0 && sid <= STATE_LAST;
        return sid & STATE_MASK | state & ~STATE_MASK;
    }

    private boolean checkCancel() {
        return allAreSet(state, FLAG_CANCEL_REQ);
    }

    private static int stateOf(int oldVal) {
        return oldVal & 0xf;
    }

    private static boolean stateIsIn(int state, int sid1, int sid2) {
        final int sid = stateOf(state);
        return sid == sid1 || sid == sid2;
    }

    private static boolean stateIsIn(int state, int sid1, int sid2, int sid3) {
        final int sid = stateOf(state);
        return sid == sid1 || sid == sid2 || sid == sid3;
    }

    private void execComplete(final T result) {
        assert ! holdsLock(this);
        int state;
        synchronized (this) {
            state = this.state | FLAG_USER_THREAD | FLAG_EXECUTE_DONE;
            if (unlikely(stateOf(state) != STATE_EXECUTE)) {
                throw new IllegalStateException("Task may not be completed now");
            }
            this.result = result;
            state = transition(state);
            this.state = state & PERSISTENT_STATE;
        }
        executeTasks(state);
    }

    private void execCancelled() {
        assert ! holdsLock(this);
        int state;
        synchronized (this) {
            state = this.state | FLAG_USER_THREAD | FLAG_EXECUTE_CANCELLED;
            if (unlikely(stateOf(state) != STATE_EXECUTE || allAreClear(state, FLAG_CANCEL_REQ))) {
                throw new IllegalStateException("Task may not be cancelled now");
            }
            state = transition(state);
            this.state = state & PERSISTENT_STATE;
        }
        executeTasks(state);
    }

    private void execFailed() {
        assert ! holdsLock(this);
        int state;
        synchronized (this) {
            state = this.state | FLAG_USER_THREAD | FLAG_EXECUTE_FAILED;
            if (unlikely(stateOf(state) != STATE_EXECUTE)) {
                throw new IllegalStateException("Task may not be completed now");
            }
            state = transition(state);
            this.state = state & PERSISTENT_STATE;
        }
        executeTasks(state);
    }

    private void rollbackComplete() {
        assert ! holdsLock(this);
        int state;
        synchronized (this) {
            state = this.state | FLAG_USER_THREAD | FLAG_ROLLBACK_DONE;
            if (unlikely(stateOf(state) != STATE_ROLLBACK)) {
                throw new IllegalStateException("Task may not be completed now");
            }
            state = transition(state);
            this.state = state & PERSISTENT_STATE;
        }
        executeTasks(state);
    }

    void validate() {
        final ProblemReport problemReport = getTransaction().getProblemReport();
        final Validatable validatable = this.validatable;
        if (validatable != null) try {
            doBegin();
            validatable.validate(new ValidateContext() {
                public boolean isCancelRequested() {
                    return checkCancel();
                }

                public void cancelled() {
                    validateCancelled();
                }

                public void addProblem(final Problem reason) {
                    problemReport.addProblem(reason);
                }

                public void addProblem(final Problem.Severity severity, final String message) {
                    addProblem(new Problem(TaskControllerImpl.this, message, severity));
                }

                public void addProblem(final Problem.Severity severity, final String message, final Throwable cause) {
                    addProblem(new Problem(TaskControllerImpl.this, message, cause, severity));
                }

                public void addProblem(final String message, final Throwable cause) {
                    addProblem(new Problem(TaskControllerImpl.this, message, cause));
                }

                public void addProblem(final String message) {
                    addProblem(new Problem(TaskControllerImpl.this, message));
                }

                public void addProblem(final Throwable cause) {
                    addProblem(new Problem(TaskControllerImpl.this, cause));
                }

                public void complete() {
                    validateComplete();
                }

                public void begin() {
                    doBegin();
                }

                public void end() {
                    doEnd();
                }
            });
        } catch (Throwable t) {

        } finally {
            doEnd();
        }
    }

    void validateComplete() {
        assert ! holdsLock(this);
        int state;
        synchronized (this) {
            state = this.state | FLAG_USER_THREAD | FLAG_VALIDATE_DONE;
            if (unlikely(stateOf(state) != STATE_VALIDATE)) {
                throw new IllegalStateException("Task may not be completed now");
            }
            state = transition(state);
            this.state = state & PERSISTENT_STATE;
        }
        executeTasks(state);
    }

    void validateCancelled() {
        assert ! holdsLock(this);
        int state;
        synchronized (this) {
            state = this.state | FLAG_USER_THREAD | FLAG_VALIDATE_CANCELLED;
            if (unlikely(stateOf(state) != STATE_VALIDATE)) {
                throw new IllegalStateException("Task may not be completed now");
            }
            state = transition(state);
            this.state = state & PERSISTENT_STATE;
        }
        executeTasks(state);
    }

    void doBegin() {
        synchronized (this) {
            if (! threads.add(Thread.currentThread())) {
                throw new IllegalStateException("Thread is already doing work on behalf of this task");
            }
        }
    }

    void doEnd() {
        synchronized (this) {
            threads.remove(Thread.currentThread());
        }
    }

    void rollback() {
        final Revertible rev = revertible;
        if (rev != null) try {
            doBegin();
            rev.rollback(new RollbackContext() {
                public void complete() {
                    rollbackComplete();
                }

                public void begin() {
                    doBegin();
                }

                public void end() {
                    doEnd();
                }
            });
        } catch (Throwable t) {
            // log
        } finally {
            doEnd();
        }
    }

    void execute() {
        final ProblemReport problemReport = getTransaction().getProblemReport();
        final Executable<T> exec = executable;
        if (exec != null) try {
            doBegin();
            exec.execute(new ExecuteContext<T>() {
                public void complete(final T result) {
                    execComplete(result);
                }

                public void complete() {
                    complete(null);
                }

                public void failed() {
                    execFailed();
                }

                public boolean isCancelRequested() {
                    return checkCancel();
                }

                public void cancelled() {
                    execCancelled();
                }

                public void addProblem(final Problem reason) {
                    problemReport.addProblem(reason);
                }

                public void addProblem(final Problem.Severity severity, final String message) {
                    addProblem(new Problem(TaskControllerImpl.this, message, severity));
                }

                public void addProblem(final Problem.Severity severity, final String message, final Throwable cause) {
                    addProblem(new Problem(TaskControllerImpl.this, message, cause, severity));
                }

                public void addProblem(final String message, final Throwable cause) {
                    addProblem(new Problem(TaskControllerImpl.this, message, cause));
                }

                public void addProblem(final String message) {
                    addProblem(new Problem(TaskControllerImpl.this, message));
                }

                public void addProblem(final Throwable cause) {
                    addProblem(new Problem(TaskControllerImpl.this, cause));
                }

                public <T> TaskBuilder<T> newTask(final Executable<T> task) throws IllegalStateException {
                    return new TaskBuilder<T>(getTransaction(), TaskControllerImpl.this, task);
                }

                public TaskBuilder<Void> newTask() throws IllegalStateException {
                    return new TaskBuilder<Void>(getTransaction(), TaskControllerImpl.this);
                }

                public void begin() {
                    doBegin();
                }

                public void end() {
                    doEnd();
                }
            });
        } catch (Throwable t) {
        } finally {
            doEnd();
        }
    }

    void commit() {
        final Committable committable = this.committable;
        if (committable != null) try {
            doBegin();
            committable.commit(new CommitContext() {
                public void complete() {
                    commitComplete();
                }

                public void begin() {
                    doBegin();
                }

                public void end() {
                    doEnd();
                }
            });
        } catch (Throwable t) {
        } finally {
            doEnd();
        }
    }

    void commitComplete() {
        assert ! holdsLock(this);
        int state;
        synchronized (this) {
            state = this.state | FLAG_USER_THREAD | FLAG_COMMIT_DONE;
            if (unlikely(stateOf(state) != STATE_COMMIT)) {
                throw new IllegalStateException("Task may not be completed now");
            }
            state = transition(state);
            this.state = state & PERSISTENT_STATE;
        }
        executeTasks(state);
    }

    public void childExecutionFinished(final TaskChild child, final boolean userThread) {
        assert ! holdsLock(this);
        int state;
        synchronized (this) {
            state = this.state;
            if (userThread) state |= FLAG_USER_THREAD;
            unfinishedChildren--;
            state = transition(state);
            this.state = state & PERSISTENT_STATE;
        }
        executeTasks(state);
    }

    public void childValidationFinished(final TaskChild child, final boolean userThread) {
        assert ! holdsLock(this);
        int state;
        synchronized (this) {
            state = this.state;
            if (userThread) state |= FLAG_USER_THREAD;
            unvalidatedChildren--;
            state = transition(state);
            this.state = state & PERSISTENT_STATE;
        }
        executeTasks(state);
    }

    public void childRollbackFinished(final TaskChild child, final boolean userThread) {
        assert ! holdsLock(this);
        int state;
        synchronized (this) {
            state = this.state;
            if (userThread) state |= FLAG_USER_THREAD;
            state = transition(state);
            this.state = state & PERSISTENT_STATE;
        }
        executeTasks(state);
    }

    public void childCommitFinished(final TaskChild child, final boolean userThread) {
        // a dependent has finished its commit operation.
        assert ! holdsLock(this);
        int state;
        synchronized (this) {
            state = this.state;
            if (userThread) state |= FLAG_USER_THREAD;
            uncommittedChildren--;
            state = transition(state);
            this.state = state & PERSISTENT_STATE;
        }
        executeTasks(state);
    }

    public void childAdded(final TaskChild child, final boolean userThread) throws InvalidTransactionStateException {
        assert ! holdsLock(this);
        int state;
        synchronized (this) {
            state = this.state;
            if (stateIsIn(state, STATE_EXECUTE_WAIT, STATE_EXECUTE)) {
                unfinishedChildren++;
                uncommittedChildren++;
                unvalidatedChildren++;
                if (userThread) state |= FLAG_USER_THREAD;
                state = transition(state);
                this.state = state & PERSISTENT_STATE;
            } else {
                if (userThread) {
                    throw new IllegalStateException("Dependent may not be added at this point");
                } else {
                    // todo log and ignore...
                    return;
                }
            }
        }
        executeTasks(state);
    }

    public void dependencyExecutionComplete(final TaskParent dependency, final boolean userThread) {
        // another dependency has completed execution, thus getting us one step closer to having permission to execute.
        assert ! holdsLock(this);
        int state;
        synchronized (this) {
            state = this.state;
            if (userThread) state |= FLAG_USER_THREAD;
            unfinishedDependencies--;
            state = transition(state);
            this.state = state & PERSISTENT_STATE;
        }
        executeTasks(state);
    }

    public void dependencyValidateComplete(final TaskParent dependency, final boolean userThread) {
        assert ! holdsLock(this);
        int state;
        synchronized (this) {
            state = this.state;
            if (userThread) state |= FLAG_USER_THREAD;
            state = transition(state);
            this.state = state & PERSISTENT_STATE;
        }
        executeTasks(state);
    }

    public void dependencyCommitComplete(final TaskParent dependency, final boolean userThread) {
        assert ! holdsLock(this);
        int state;
        synchronized (this) {
            state = this.state;
            if (userThread) state |= FLAG_USER_THREAD;
            uncommittedDependencies--;
            state = transition(state);
            this.state = state & PERSISTENT_STATE;
        }
        executeTasks(state);
    }

    public void childInitiateRollback(final boolean userThread) {
        assert ! holdsLock(this);
        int state;
        synchronized (this) {
            state = this.state | FLAG_ROLLBACK_REQ;
            if (userThread) state |= FLAG_USER_THREAD;
            if (stateOf(state) == STATE_EXECUTE && allAreClear(state, FLAG_CANCEL_REQ)) {
                state |= FLAG_CANCEL_REQ;
                for (Thread thread : threads) {
                    thread.interrupt();
                }
            }
            state = transition(state);
            this.state = state & PERSISTENT_STATE;
        }
        executeTasks(state);
    }

    public void childInitiateValidate(final boolean userThread) {
        assert ! holdsLock(this);
        int state;
        synchronized (this) {
            state = this.state | FLAG_VALIDATE_REQ;
            if (userThread) state |= FLAG_USER_THREAD;
            state = transition(state);
            this.state = state & PERSISTENT_STATE;
        }
        executeTasks(state);
    }

    public void childInitiateCommit(final boolean userThread) {
        assert ! holdsLock(this);
        int state;
        synchronized (this) {
            state = this.state | FLAG_COMMIT_REQ;
            if (userThread) state |= FLAG_USER_THREAD;
            state = transition(state);
            this.state = state & PERSISTENT_STATE;
        }
        executeTasks(state);
    }

    void dependentAdded(final TaskControllerImpl<?> dependent, final boolean userThread) {
        assert ! holdsLock(this);
        int state;
        synchronized (this) {
            state = this.state;
            if (userThread) state |= FLAG_USER_THREAD;
            if (stateIsIn(state, STATE_EXECUTE_WAIT, STATE_EXECUTE, STATE_EXECUTE_DONE)) {
                dependents.add(dependent);
                unrevertedDependents++;
                state = transition(state);
                this.state = state & PERSISTENT_STATE;
            } else {
                if (userThread) {
                    throw new IllegalStateException("Dependent may not be added at this point");
                } else {
                    // todo log and ignore...
                    return;
                }
            }
        }
        executeTasks(state);
    }

    void install() {
        assert ! holdsLock(this);
        int state;
        synchronized (this) {
            uncommittedDependencies = unfinishedDependencies = dependencies.length;
        }
        try {
            parent.childAdded(this, true);
        } catch (IllegalStateException e) {
            synchronized (this) {
                state = this.state | FLAG_USER_THREAD | FLAG_INSTALL_FAILED;
                state = transition(state);
                this.state = state & PERSISTENT_STATE;
            }
            executeTasks(state);
            throw e;
        }
        TaskControllerImpl<?> dependency;
        for (int i = 0; i < dependencies.length; i++) {
            dependency = dependencies[i];
            try {
                dependency.dependentAdded(this, true);
            } catch (IllegalStateException e) {
                for (; i >= 0; i --) {
                    dependency = dependencies[i];
                    dependency.dependentRollbackDone(true);
                }
                parent.childRollbackFinished(this, true);
                synchronized (this) {
                    state = this.state | FLAG_USER_THREAD | FLAG_INSTALL_FAILED;
                    state = transition(state);
                    this.state = state & PERSISTENT_STATE;
                }
                executeTasks(state);
                throw e;
            }
        }
        synchronized (this) {
            state = this.state | FLAG_USER_THREAD;
            state = transition(state);
            this.state = state & PERSISTENT_STATE;
        }
        executeTasks(state);
    }

    class AsyncTask implements Runnable {
        private final int state;

        AsyncTask(final int state) {
            this.state = state;
        }

        public void run() {
            executeTasks(state);
        }
    }
}
