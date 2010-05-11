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

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;
import org.jboss.logging.Logger;
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
    private final Value<? extends Service> serviceValue;
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
    private final Set<ServiceListener<? super S>> listeners = new HashSet<ServiceListener<? super S>>();
    /**
     * The start exception.
     */
    private StartException startException;
    /**
     * The controller mode.
     */
    private Mode mode = Mode.NEVER;
    /**
     * The controller state.
     */
    private State state = State.DOWN;

    /**
     * The number of dependents which place a demand-to-start on this dependency.  If this value is >0, propagate a demand
     * up to all parent dependents.  If this value is >0 and mode is ON_DEMAND, put a load of +1 on {@code upperCount}.
     */
    private int demandedByCount;
    /**
     * Semaphore count for bringing this dep up.  If the value is <= 0, the service is stopped.  Each unstarted
     * dependency will put a load of -1 on this value.  A mode of AUTOMATIC or IMMEDIATE will put a load of +1 on this
     * value.  A mode of NEVER will cause this value to be ignored.
     */
    private int upperCount;
    /**
     * The number of dependents that are currently running.  The deployment will not leave the {@link State#STOPPING} state
     * until all running dependents are stopped.
     */
    private int runningDependents;
    /**
     * The number of listeners that are currently running.
     */
    private int runningListeners;

    /**
     * Listener which is added to dependencies of this service.
     */
    private final ServiceListener<Object> dependencyListener = new ServiceListener<Object>() {
        public void serviceStarting(final ServiceController<? extends Object> serviceController) {
        }

        public void serviceStarted(final ServiceController<? extends Object> serviceController) {
            synchronized (ServiceControllerImpl.this) {
                if (++upperCount == 1) {
                    if (runningListeners == 0 && state == State.DOWN) {
                        doStart();
                    }
                }
            }
        }

        public void serviceFailed(final ServiceController<? extends Object> serviceController, final StartException reason) {
        }

        public void serviceStopping(final ServiceController<? extends Object> serviceController) {
            synchronized (ServiceControllerImpl.this) {
                if (--upperCount == 0) {
                    if (runningListeners == 0 && state == State.UP) {
                        doStop();
                    }
                }
            }
        }

        public void serviceStopped(final ServiceController<? extends Object> serviceController) {
        }

        public void serviceRemoved(final ServiceController<? extends Object> serviceController) {
        }
    };

    ServiceControllerImpl(final ServiceContainerImpl container, final Value<? extends Service> serviceValue, final Value<S> value, final Location location, final ServiceControllerImpl<?>[] dependencies) {
        this.container = container;
        this.serviceValue = serviceValue;
        this.value = value;
        this.location = location;
        this.dependencies = dependencies;
        for (ServiceControllerImpl<?> controller : dependencies) {
            controller.addListener(dependencyListener);
        }
    }

    public Handle<S> demand() {
        addDemand();
        return new HandleImpl<S>(this);
    }

    public ServiceContainer getServiceContainer() {
        return container;
    }

    public State getState() {
        return state;
    }

    public S getValue() throws IllegalStateException {
        return value.getValue();
    }

    public void addListener(final ServiceListener<? super S> listener) {
        final State state;
        synchronized (this) {
            runningListeners ++;
            state = this.state;
        }
        invokeListener(listener, state);
        synchronized (this) {
            listeners.add(listener);
            if (--runningListeners == 0) {
                doFinishListener();
            }
        }
    }

    private void invokeListener(final ServiceListener<? super S> listener, final State state) {
        switch (state) {
            case DOWN: {
                listener.serviceStopped(this);
                break;
            }
            case STARTING: {
                listener.serviceStarting(this);
                break;
            }
            case START_FAILED: {
                listener.serviceFailed(this, startException);
                break;
            }
            case UP: {
                listener.serviceStarted(this);
                break;
            }
            case STOPPING: {
                listener.serviceStopping(this);
                break;
            }
            case REMOVED: {
                listener.serviceRemoved(this);
                break;
            }
        }
    }

    public void removeListener(final ServiceListener<? super S> listener) {
        synchronized (this) {
            listeners.remove(listener);
        }
    }

    public void remove() throws IllegalStateException {
        synchronized (this) {
            if (state == State.DOWN) {
                state = State.REMOVED;
                for (ServiceControllerImpl<?> controller : dependencies) {
                    controller.removeListener(dependencyListener);
                }
                Arrays.fill(dependencies, null);
            }
        }
    }

    public StartException getStartException() {
        return startException;
    }

    public void retry() {
        synchronized (this) {
            if (state == State.START_FAILED) {
                doStart();
            }
        }
    }

    public Mode getMode() {
        synchronized (this) {
            return mode;
        }
    }

    public void setMode(final Mode newMode) {
        synchronized (this) {
            final Mode oldMode = mode;
            mode = newMode;
            switch (oldMode) {
                case NEVER: {
                    switch (newMode) {
                        case NEVER: {
                            return;
                        }
                        case ON_DEMAND: {
                            if (demandedByCount > 0) {
                                upperCount++;
                            }
                            if (state == State.DOWN && upperCount > 0) {
                                doStart();
                            }
                            return;
                        }
                        case AUTOMATIC: {
                            if (upperCount++ == 0 && state == State.DOWN) {
                                doStart();
                            }
                            return;
                        }
                        case IMMEDIATE: {
                            doDemandParents();
                            if (upperCount++ == 0 && state == State.DOWN) {
                                doStart();
                            }
                            return;
                        }
                    }
                }
                case ON_DEMAND: {
                    switch (newMode) {
                        case NEVER: {
                            if (demandedByCount > 0) {
                                upperCount--;
                            }
                            if (state == State.UP) {
                                doStop();
                            }
                            return;
                        }
                        case ON_DEMAND: {
                            return;
                        }
                        case AUTOMATIC: {
                            if (demandedByCount == 0) {
                                upperCount++;
                            }
                            if (upperCount > 0 && state == State.DOWN) {
                                doStart();
                            }
                            return;
                        }
                        case IMMEDIATE: {
                            doDemandParents();
                            if (demandedByCount == 0) {
                                upperCount++;
                            }
                            if (upperCount > 0 && state == State.DOWN) {
                                doStart();
                            }
                            return;
                        }
                    }
                }
                case AUTOMATIC: {
                    switch (newMode) {
                        case NEVER: {
                            upperCount--;
                            if (state == State.UP) {
                                doStop();
                            }
                            return;
                        }
                        case ON_DEMAND: {
                            if (demandedByCount == 0) {
                                upperCount--;
                            }
                            if (state == State.UP && upperCount == 0) {
                                doStop();
                            }
                            return;
                        }
                        case AUTOMATIC: {
                            return;
                        }
                        case IMMEDIATE: {
                            doDemandParents();
                            return;
                        }
                    }
                }
                case IMMEDIATE: {
                    switch (newMode) {
                        case NEVER: {
                            upperCount--;
                            if (state == State.UP) {
                                doStop();
                            }
                            return;
                        }
                        case ON_DEMAND: {
                            if (demandedByCount == 0) {
                                upperCount--;
                            }
                            if (state == State.UP && upperCount == 0) {
                                doStop();
                            }
                            return;
                        }
                        case AUTOMATIC: {
                            doUndemandParents();
                            return;
                        }
                        case IMMEDIATE: {
                            return;
                        }
                    }
                }
            }
        }
    }

    enum StartContextState {
        SYNC,
        ASYNC,
        COMPLETE,
        FAILED,
    }

    private void doStart() {
        assert Thread.holdsLock(this);
        assert state == State.DOWN;
        state = State.STARTING;
        runningListeners = 1;
        doRunListeners();
        try {
            final Service service = serviceValue.getValue();
            container.getExecutor().execute(new Runnable() {
                public void run() {
                    final StartContextImpl context = new StartContextImpl();
                    try {
                        service.start(context);
                        synchronized (ServiceControllerImpl.this) {
                            if (context.state == StartContextState.SYNC) {
                                context.state = StartContextState.COMPLETE;
                                doFinishListener();
                            }
                        }
                    } catch (StartException e) {
                        synchronized (ServiceControllerImpl.this) {
                            final StartContextState oldState = context.state;
                            if (oldState == StartContextState.SYNC || oldState == StartContextState.ASYNC) {
                                context.state = StartContextState.FAILED;
                                startException = e;
                                doFinishListener();
                            } else {
                                // todo log warning
                            }
                        }
                    } catch (Throwable t) {
                        synchronized (ServiceControllerImpl.this) {
                            final StartContextState oldState = context.state;
                            if (oldState == StartContextState.SYNC || oldState == StartContextState.ASYNC) {
                                context.state = StartContextState.FAILED;
                                startException = new StartException("Failed to start service", t, location);
                                doFinishListener();
                            } else {
                                // todo log warning
                            }
                        }
                    }
                }
            });
        } catch (RuntimeException e) {
            doFail(new StartException("Failed to start service", e));
        }
    }

    private void doRunListeners() {
        assert ! Thread.holdsLock(this);
        final ServiceListener[] toRun;
        final int cnt;
        synchronized (this) {
            final Set<ServiceListener<? super S>> listeners = this.listeners;
            toRun = listeners.toArray(new ServiceListener[this.listeners.size()]);
            cnt = toRun.length;
            runningListeners += cnt;
        }
        final Executor executor = container.getExecutor();
        for (final ServiceListener listener : toRun) {
            executor.execute(new Runnable() {
                public void run() {
                    invokeListener(listener, state);
                }
            });
        }
    }

    private void doStartComplete() {
        assert Thread.holdsLock(this);
        assert state == State.STARTING;
        state = State.UP;
        final Executor executor = container.getExecutor();
        final Set<ServiceListener<? super S>> listeners = this.listeners;
        final int cnt = listeners.size();
        runningListeners = cnt;
        for (final ServiceListener<? super S> listener : listeners) {
            executor.execute(new Runnable() {
                public void run() {
                    try {
                        listener.serviceStarted(ServiceControllerImpl.this);
                    } finally {
                        doFinishListener();
                    }
                }
            });
        }
    }

    private void doFinishListener() {
        assert Thread.holdsLock(this);
        if (--runningListeners == 0) {
            
        }
    }

    private void doFail(final StartException e) {
        assert Thread.holdsLock(this);
        assert state == State.STARTING;
        state = State.START_FAILED;
        startException = e;
        // todo invoke listeners
    }

    private void doStop() {
        assert Thread.holdsLock(this);
        assert state == State.UP;
    }

    private void doStopComplete() {
        assert Thread.holdsLock(this);
        assert state == State.STOPPING;
    }

    private void doDemandParents() {
        assert Thread.holdsLock(this);
        for (ServiceControllerImpl<?> dependency : dependencies) {
            dependency.addDemand();
        }
    }

    private void doUndemandParents() {
        assert Thread.holdsLock(this);
        for (ServiceControllerImpl<?> dependency : dependencies) {
            dependency.removeDemand();
        }
    }

    void addDemand() {
        synchronized (this) {
            final int cnt = demandedByCount++;
            if (cnt == 0 && mode == Mode.ON_DEMAND) {
                if (upperCount++ == 0 && state == State.DOWN) {
                    doStart();
                }
            }
        }
    }

    void removeDemand() {
        synchronized (this) {
            final int cnt = --demandedByCount;
            if (cnt == 0 && mode == Mode.ON_DEMAND) {
                if (--upperCount == 0) {
                    if (state == State.UP) {
                        doStop();
                    } else if (state == State.START_FAILED) {
                        doStopComplete();
                    }
                }
            }
        }
    }

    void dependentStarted() {
        synchronized (this) {
            runningDependents++;
        }
    }

    void dependentStopped() {
        synchronized (this) {
            if (--runningDependents == 0) {
                if (state == State.STOPPING && runningListeners == 0) {
                    doStopComplete();
                }
            }
        }
    }

    private static class HandleImpl<S> implements Handle<S> {

        private volatile ServiceControllerImpl<S> serviceController;

        private static final AtomicReferenceFieldUpdater<HandleImpl, ServiceControllerImpl> serviceControllerUpdater = AtomicReferenceFieldUpdater.newUpdater(HandleImpl.class, ServiceControllerImpl.class, "serviceController");

        public HandleImpl(final ServiceControllerImpl<S> serviceController) {
            this.serviceController = serviceController;
        }

        @SuppressWarnings({ "unchecked" })
        public void close() {
            final ServiceControllerImpl<S> controller = serviceControllerUpdater.getAndSet(this, null);
            if (controller != null) {
                controller.removeDemand();
            }
        }

        protected void finalize() {
            close();
        }

        public ServiceController<S> getValue() throws IllegalStateException {
            final ServiceControllerImpl<S> controller = serviceController;
            if (controller == null) {
                throw new IllegalStateException("Handle was already closed");
            }
            return controller;
        }
    }

    private class StartContextImpl implements StartContext {

        private StartContextState state;

        public void failed(final StartException reason) throws IllegalStateException {
            synchronized (ServiceControllerImpl.this) {
                if (state == StartContextState.ASYNC) {
                    state = StartContextState.FAILED;
                    doFail(reason);
                } else {
                    throw new IllegalStateException(ILLEGAL_CONTROLLER_STATE);
                }
            }
        }

        public void asynchronous() throws IllegalStateException {
            synchronized (ServiceControllerImpl.this) {
                if (state == StartContextState.SYNC) {
                    state = StartContextState.ASYNC;
                } else {
                    throw new IllegalStateException(ILLEGAL_CONTROLLER_STATE);
                }
            }
        }

        public void complete() throws IllegalStateException {
            synchronized (ServiceControllerImpl.this) {
                if (state == StartContextState.ASYNC) {
                    state = StartContextState.COMPLETE;
                    doStartComplete();
                } else {
                    throw new IllegalStateException(ILLEGAL_CONTROLLER_STATE);
                }
            }
        }

        protected void finalize() throws Throwable {
            synchronized (ServiceControllerImpl.this) {
                if (state == StartContextState.ASYNC) {
                    state = StartContextState.FAILED;
                    doFail(new StartException("Asynchronous service never started"));
                }
            }
        }
    }
}
