/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;

/**
 * A service listener which calls a callback once all of the services it was attached to
 * have been removed.
 *
 * @param <T> the callback parameter type
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class MultipleRemoveListener<T> extends AbstractServiceListener<Object> {
    @SuppressWarnings({ "UnusedDeclaration" })
    private volatile int count = 1;
    @SuppressWarnings({ "UnusedDeclaration" })
    private volatile int done;

    private final Callback<T> callback;
    private final T attachment;

    private MultipleRemoveListener(final Callback<T> callback, final T attachment) {
        this.callback = callback;
        this.attachment = attachment;
    }

    /**
     * Construct a new instance.
     *
     * @param callback the callback to invoke
     * @param attachment the attachment to pass to the callback
     * @param <T> the type of the attachment
     * @return the remove listener
     */
    public static <T> MultipleRemoveListener<T> create(final Callback<T> callback, final T attachment) {
        return new MultipleRemoveListener<T>(callback, attachment);
    }

    /**
     * Construct a new instance.
     *
     * @param task the task to call upon completion
     * @return the remove listener
     */
    public static MultipleRemoveListener<Runnable> create(final Runnable task) {
        return new MultipleRemoveListener<Runnable>(new Callback<Runnable>() {
            public void handleDone(final Runnable parameter) {
                if (parameter != null) parameter.run();
            }
        }, task);
    }

    /**
     * Construct a new instance which calls the lifecycle {@code complete()} method when done.
     *
     * @param lifecycleContext the context to notify
     * @return the remove listener
     */
    public static MultipleRemoveListener<LifecycleContext> create(final LifecycleContext lifecycleContext) {
        return new MultipleRemoveListener<LifecycleContext>(LIFECYCLE_CONTEXT_CALLBACK, lifecycleContext);
    }

    @SuppressWarnings({ "RawUseOfParameterizedType" })
    private static final AtomicIntegerFieldUpdater<MultipleRemoveListener> countUpdater = AtomicIntegerFieldUpdater.newUpdater(MultipleRemoveListener.class, "count");
    @SuppressWarnings({ "RawUseOfParameterizedType" })
    private static final AtomicIntegerFieldUpdater<MultipleRemoveListener> doneUpdater = AtomicIntegerFieldUpdater.newUpdater(MultipleRemoveListener.class, "done");

    /** {@inheritDoc} */
    public void listenerAdded(final ServiceController<?> controller) {
        countUpdater.getAndIncrement(this);
    }

    public void transition(final ServiceController<? extends Object> controller, final ServiceController.Transition transition) {
        if (transition.enters(ServiceController.State.REMOVED)) {
            tick();
        }
    }

    /**
     * Called when this listener has been added to all relevant services.
     */
    public void done() {
        if (doneUpdater.getAndSet(this, 1) == 0) {
            tick();
        }
    }

    private void tick() {
        if (countUpdater.decrementAndGet(this) == 0) {
            callback.handleDone(attachment);
        }
    }

    private static final Callback<LifecycleContext> LIFECYCLE_CONTEXT_CALLBACK = new Callback<LifecycleContext>() {
        public void handleDone(final LifecycleContext parameter) {
            parameter.complete();
        }
    };

    /**
     * A generalized callback for when all services are removed.
     */
    public interface Callback<T> {

        /**
         * Handle the completion of all removals.
         *
         * @param parameter the parameter
         */
        void handleDone(T parameter);
    }
}
