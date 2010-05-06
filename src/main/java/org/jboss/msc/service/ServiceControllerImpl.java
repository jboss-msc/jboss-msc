/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2009, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.msc.service;

import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;
import org.jboss.logging.Logger;
import org.jboss.msc.inject.Injector;
import org.jboss.msc.value.Value;

final class ServiceControllerImpl<S> implements ServiceController<S> {
    private static final Logger log = Logger.getI18nLogger("org.jboss.msc.controller", null, "MSC");

    private static final String ILLEGAL_CONTROLLER_STATE = "Illegal controller state";
    private static final String ILLEGAL_LOCK_STATE = "Illegal lock state";
    private static final String START_FAIL_EXCEPTION = "Start failed due to exception";
    private static final String SERVICE_REMOVED = "Service has been removed";
    private static final String SERVICE_IS_NOT_UP = "Service is not up";

    /**
     * The service container which contains this instance.
     */
    private final ServiceContainerImpl container;
    /**
     * The service itself.
     */
    private final Value<Service> service;
    /**
     * The value which is passed to listeners.
     */
    private final Value<S> value;
    /**
     * The source location in which this service was defined.
     */
    private final Location location;
    /**
     * The dependencies of this service.
     */
    private final ServiceControllerImpl<?>[] dependencies;
    /**
     * The set of registered service listeners.
     */
    private final Set<ServiceListener> listeners = new HashSet<ServiceListener>();

    private StartException startException;

    private Substate state = Substate.DOWN;

    private Mode mode = Mode.NEVER;

    private int stateLockCount;

    /**
     * The number of dependents which place a demand-to-start on this dependency.  If this value is >0, propagate a demand
     * up to all parent dependents.  If this value is >0 and mode is ON_DEMAND, put a load of +1 on {@code upperCount}.
     */
    private int demandedByCount;
    /**
     * Semaphore count for bringing this dep up.  If the value is <= 0, the service is not started.  Each unstarted
     * dependency will put a load of -1 on this value.  A mode of AUTOMATIC or IMMEDIATE will put a load of +1 on this
     * value.  A mode of NEVER will put a load of -(Integer.MIN_VALUE/2) on this value.
     */
    private int upperCount;

    private int nonUpDependencies;
    private int stoppingOrDownDependencies;

    private int demandingDependents;
    private int runningDependents;
    private Thread thread;

    enum Substate {

        /**
         * Service is down.
         */
        DOWN(State.DOWN),
        /**
         * Notifying dependencies of the intent to start.
         */
        START_DEPENDENCIES(State.STARTING),
        /**
         * Waiting for dependencies to start.
         */
        START_WAIT(State.STARTING),
        /**
         * Dependencies are started, waiting for executor to schedule start task.
         */
        START_PENDING(State.STARTING),
        /**
         * Now starting.  Start task thread will be available in task thread.
         */
        STARTING(State.STARTING),
        /**
         * Service is started, notifying dependents.
         */
        START_FINISHING(State.UP),
        /**
         * Start failed; notifying waiting dependents of the failure.
         */
        START_FAILED_FINISHING(State.START_FAILED),
        /**
         * Start failed.
         */
        START_FAILED(State.START_FAILED),
        /**
         * Up.
         */
        UP(State.UP),
        /**
         * Up, notifying dependents of the intent to stop.
         */
        STOP_DEPENDENTS(State.STOPPING),
        /**
         * Up (stop requested, waiting for dependents to stop).
         */
        STOP_WAIT(State.STOPPING),
        /**
         * Dependents are stopped, waiting for executor to schedule stop task.
         */
        STOP_PENDING(State.STOPPING),
        /**
         * Now stopping.
         */
        STOPPING(State.STOPPING),
        /**
         * Service is stopped, notifying dependencies.
         */
        STOP_FINISHING(State.DOWN),
        /**
         * Removed from the container.
         */
        REMOVED(State.REMOVED),
        ;
        private final State state;

        Substate(final State state) {
            this.state = state;
        }

        public State getState() {
            return state;
        }
    }

    ServiceControllerImpl(final ServiceControllerImpl<?>[] dependencies, final Value<Service> service, final Executor executor, final Location location, final Value<S> value, final ServiceContainerImpl container) {
        this.container = container;
        assert dependencies != null;
        assert service != null;
        assert executor != null;
        assert value != null;
        this.dependencies = dependencies;
        nonUpDependencies = dependencies.length;
        this.service = service;
        this.executor = executor;
        this.location = location;
        this.value = value;
    }

    public Mode getMode() {
        synchronized (lock) {
            return mode;
        }
    }

    public void setMode(final Mode mode) {
        processStateAsync(Command.CMD_CHANGE_MODE, mode);
    }

    public boolean isDemanded() {
        synchronized (lock) {
            return demandingDependents > 0;
        }
    }

    public State getState() {
        synchronized (lock) {
            return state.getState();
        }
    }

    public S getValue() throws IllegalStateException {
        synchronized (lock) {
            if (state.getState() != State.UP) {
                throw new IllegalStateException(SERVICE_IS_NOT_UP);
            }
            return value.getValue();
        }
    }

    public boolean isStartable() {
        synchronized (lock) {
            return mode != Mode.NEVER && nonUpDependencies == 0 && (mode != Mode.ON_DEMAND || demandingDependents > 0);
        }
    }

    private enum Command {
        CMD_CHANGE_MODE(true),

        CMD_ADD_DEPENDENT(true),
        CMD_REMOVE_DEPENDENT(true),

        CMD_DEPENDENCY_STARTING(true),
        CMD_DEPENDENCY_STOPPING_PREPARE(true),
        CMD_DEPENDENCY_STOPPING_COMMIT(true),
        CMD_DEPENDENCY_STOPPING_ABORT(true),
        CMD_DEPENDENT_STARTING(true),
        CMD_DEPENDENT_STOPPED(true),
        CMD_DEPENDENT_DEMANDING(true),
        CMD_DEPENDENT_UNDEMANDING(true),

        CMD_NOTIFY_START_DEPENDENTS(false),
        CMD_NOTIFY_START_DEPENDENCIES(false),

        CMD_SUBMIT_START(true),
        CMD_START(true),
        CMD_START_NOTIFIED_DEPENDENCIES(true),
        CMD_RUN_START(false),
        CMD_FINISH_START(true),

        CMD_RUN_STOP(false),

        CMD_SUBMIT_STOP(true),


        CMD_UPWARDS(true),
        CMD_DOWNWARDS(true),

        CMD_FAIL_STATE(true),
        CMD_FAIL_STATE_RETHROW(true),
        ;
        private final boolean locked;

        Command(final boolean locked) {
            this.locked = locked;
        }

        boolean locked() {
            return locked;
        }
    }

    /**
     * The primary state machine.
     *
     * @param cmd the state machine command
     * @param arg the command argument
     */
    void processStateAsync(Command cmd, Object arg) {
        final Object lock = this.lock;
        final int dependencyCount = dependencies.length;
        assert ! Thread.holdsLock(lock) : ILLEGAL_LOCK_STATE;
        // The purpose of these nested for/for/switches are to allow multiple sequential commands to be executed
        // with or without the lock held without acquiring or releasing the lock unnecessarily.
        //
        // To run a subsequent command with the SAME lock state as the current lock state, use 'continue'.
        // To run a subsequent command with the OPPOSITE lock state as the current lock state, use 'break'.
        // To finish running commands, use 'return'.
        //
        // If a command is not matched, the command will be retried with the opposite lock state.
        for (;;) {
            // unlocked commands
            for (;;) {
                switch (cmd) {
                    /**
                     * Run the service start method.  If a failure occurs, set the fail state; propagate any VM errors.
                     */
                    case CMD_RUN_START: {
                        try {
                            service.getValue().start(null);
                            cmd = Command.CMD_FINISH_START;
                        } catch (StartException e) {
                            cmd = Command.CMD_FAIL_STATE;
                            arg = e;
                        } catch (VirtualMachineError e) {
                            cmd = Command.CMD_FAIL_STATE_RETHROW;
                            arg = new StartException(START_FAIL_EXCEPTION, e, location);
                        } catch (Throwable t) {
                            cmd = Command.CMD_FAIL_STATE;
                            arg = new StartException(START_FAIL_EXCEPTION, t, location);
                        }
                        // Next command uses lock
                        break;
                    }
                    case CMD_NOTIFY_START_DEPENDENCIES: {
                        for (ServiceControllerImpl<?> dependency : dependencies) {
                            dependency.processStateAsync(Command.CMD_DEPENDENT_STARTING, null);
                        }
                        cmd = Command.CMD_START_NOTIFIED_DEPENDENCIES;
                    }
                }
                break;
            }
            // break infinite loops
            assert cmd.locked() : ILLEGAL_LOCK_STATE;
            // unsynchronized commands may not directly trigger upwards/downwards movement
            assert cmd != Command.CMD_UPWARDS : ILLEGAL_CONTROLLER_STATE;
            assert cmd != Command.CMD_DOWNWARDS : ILLEGAL_CONTROLLER_STATE;
            // locked commands
            synchronized (lock) {
                TO_UNLOCKED: for (;;) {
                    switch (cmd) {
                        case CMD_SUBMIT_START: {
                            assert state == Substate.START_PENDING;
                            try {
                                executor.execute(new Runnable() {
                                    public void run() {
                                        processStateAsync(Command.CMD_START, null);
                                    }
                                });
                                return;
                            } catch (VirtualMachineError e) {
                                cmd = Command.CMD_FAIL_STATE_RETHROW;
                                arg = new StartException(START_FAIL_EXCEPTION, e, location);
                            } catch (Throwable t) {
                                cmd = Command.CMD_FAIL_STATE;
                                arg = new StartException(START_FAIL_EXCEPTION, t, location);
                            }
                            // Next command uses lock
                            continue;
                        }
                        case CMD_START: {
                            assert state == Substate.START_PENDING;
                            cmd = Command.CMD_RUN_START;
                            state = Substate.STARTING;
                            lock.notifyAll();
                            thread = Thread.currentThread();
                            break TO_UNLOCKED;
                        }
                        case CMD_FINISH_START: {
                            assert state == Substate.STARTING;
                            thread = null;
                            state = Substate.START_FINISHING;
                            cmd = Command.CMD_NOTIFY_START_DEPENDENTS;
                            lock.notifyAll();
                            break TO_UNLOCKED;
                        }
                        case CMD_FAIL_STATE:
                        case CMD_FAIL_STATE_RETHROW: {
                            assert state == Substate.STARTING;
                            thread = null;
                            startException = (StartException) arg;
                            state = Substate.START_FAILED;
                            lock.notifyAll();
                            Throwable cause = ((StartException) arg).getCause();
                            if (cause instanceof Error && cmd == Command.CMD_FAIL_STATE_RETHROW) {
                                throw (Error) cause;
                            } else {
                                return;
                            }
                        }
                        case CMD_SUBMIT_STOP: {
                            try {
                                executor.execute(new Runnable() {
                                    public void run() {
                                        processStateAsync(Command.CMD_RUN_STOP, null);
                                    }
                                });
                                state = Substate.STOP_PENDING;
                                return;
                            } catch (Throwable t) {
                                log.warnf(t, "Failed to submit stop task for '%s'", service.getValue());
                            }
                        }
                        case CMD_CHANGE_MODE: {
                            assert arg != null;
                            if (state == Substate.REMOVED) {
                                throw new IllegalStateException(SERVICE_REMOVED);
                            }
                            if (mode == arg) {
                                // no change
                                return;
                            }
                            Mode newMode = (mode = (Mode) arg);
                            if (newMode == Mode.IMMEDIATE ||
                                    nonUpDependencies == 0 && (newMode == Mode.AUTOMATIC || newMode == Mode.ON_DEMAND && demandingDependents > 0)) {
                                cmd = Command.CMD_UPWARDS;
                            } else if (runningDependents == 0 && (newMode == Mode.NEVER ||
                                    nonUpDependencies > 0 && (newMode == Mode.AUTOMATIC || newMode == Mode.ON_DEMAND && demandingDependents == 0))) {
                                cmd = Command.CMD_DOWNWARDS;
                            } else {
                                // nothing to do!
                                return;
                            }
                            arg = null;
                            continue;
                        }
                        case CMD_START_NOTIFIED_DEPENDENCIES: {
                            assert state == Substate.START_DEPENDENCIES;
                            if (nonUpDependencies > 0) {
                                state = Substate.START_WAIT;
                                lock.notifyAll();
                                return;
                            }
                            cmd = Command.CMD_SUBMIT_START;
                            continue;
                        }
                        case CMD_UPWARDS: {
                            switch (state) {
                                case DOWN: {
                                    if (dependencyCount == 0) {
                                        state = Substate.START_PENDING;
                                        lock.notifyAll();
                                        cmd = Command.CMD_SUBMIT_START;
                                        continue;
                                    } else {
                                        state = Substate.START_DEPENDENCIES;
                                        lock.notifyAll();
                                        cmd = Command.CMD_NOTIFY_START_DEPENDENCIES;
                                        break TO_UNLOCKED;
                                    }
                                }
                                case START_DEPENDENCIES:
                                    break;
                                case START_WAIT:
                                    break;
                                case START_PENDING:
                                    break;
                                case STARTING:
                                    break;
                                case START_FINISHING:
                                    break;
                                case START_FAILED_FINISHING:
                                    break;
                                case START_FAILED:
                                    break;
                                case UP:
                                    break;
                                case STOP_DEPENDENTS:
                                    break;
                                case STOP_WAIT:
                                    break;
                                case STOP_PENDING:
                                    break;
                                case STOPPING:
                                    break;
                                case STOP_FINISHING:
                                    break;
                                case REMOVED: {
                                    throw new IllegalStateException(SERVICE_REMOVED);
                                }
                                default: {
                                    throw new IllegalStateException(ILLEGAL_CONTROLLER_STATE);
                                }
                            }
                        }
                    }
                    break;
                }
            }
            // break infinite loops
            assert ! cmd.locked() : ILLEGAL_LOCK_STATE;
        }
    }

    public StartException getStartException() {
        synchronized (lock) {
            return startException;
        }
    }
}
