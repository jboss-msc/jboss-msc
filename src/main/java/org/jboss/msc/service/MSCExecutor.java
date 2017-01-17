package org.jboss.msc.service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * An executor that introduces the concept of forward and backwards tasks, where forward tasks are tasks that are working
 * towards service start, and backwards tasks are working towards service stop/removal.
 *
 * This executor will only run forward or backward tasks concurrently. If a forward task is running when a
 * backwards task is submitted the task will be queued until all forward tasks are completed, and visa versa.
 *
 * This is a workaround for various race conditions that can occur if services are being removed at the same time as
 * others are starting.
 *
 * @author Stuart Douglas
 */
class MSCExecutor {

    static final String ENABLED_PROP = "org.jboss.msc.directionalExecutor";

    private final Executor delegate;

    /**
     * The number of outstanding tasks, this number is positive for forward tasks, and negative for backwards ones.
     *
     * A transition back to zero must be done under lock, other transitions can be done via a CAS
     *
     */
    private final AtomicInteger outstandingCount = new AtomicInteger();
    private final List<MSCRunnable> queue = new ArrayList<MSCRunnable>();

    private static final boolean enabled;

    static {
        enabled = Boolean.getBoolean(ENABLED_PROP);
    }

    MSCExecutor(Executor delegate) {
        this.delegate = delegate;
    }

    void execute(final MSCRunnable runnable) {
        if(!enabled || runnable.isBiDirectional()) {
            delegate.execute(runnable);
            return;
        }
        final boolean forward = runnable.isForwardTask();
        final int direction = forward ? 1 : -1;
        int count;
        for (; ; ) {
            count = outstandingCount.get();
            if (count * direction < 0) { //make sure the executor is running the right way
                synchronized (this) {
                    if (outstandingCount.get() * direction < 0) { //check again under lock
                        queue.add(runnable);                      //enqueue the task
                        return;
                    }
                }
            } else {
                int newVal = count + direction;
                if (outstandingCount.compareAndSet(count, newVal)) {
                    //CAS succeeded, we can execute the task
                    break;
                }
            }
        }
        final int reverse = direction * -1;
        try {
            //run the task
            delegate.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        runnable.run();
                    } finally {
                        taskComplete(reverse);
                    }
                }
            });
        } catch (RuntimeException e) {
            taskComplete(reverse);
            throw e;
        } catch (Error e) {
            taskComplete(reverse);
            throw e;
        }
    }

    /**
     * Method that is executed when a task is done
     * @param reverse The opposite direction to the current task
     */
    private void taskComplete(int reverse) {
        boolean ok;
        do {
            int count = outstandingCount.get();
            int newVal = count + reverse;
            if (newVal == 0) {
                //if we are going back to zero we need to do it under lock, so we can run any queued tasks
                synchronized (MSCExecutor.this) {
                    ok = outstandingCount.compareAndSet(count, newVal);
                    if (ok) {
                        runQueue(); //try and execute and queued tasks
                    }
                }
            } else {
                ok = outstandingCount.compareAndSet(count, newVal);
            }
        } while (!ok);
    }

    /**
     * run's queued tasks, must be executed under lock
     */
    private void runQueue() {
        assert Thread.holdsLock(this);
        //we just make a copy of the list, and then run them in a separate thread (So they are not submitted under the lock)
        //I am not 100% sure that this is necessary, however it makes the implementation much simpler and the locking semantics
        //more consistent, and given that queued tasks will be fairly rare for most use cases the extra thread pool
        //dispatch is worth the simpler implementation
        //note that there is a possible race, if another task of the wrong direction is submitted in the meantime then
        //the tasks will just get queued again, however in practice this should not happen, and even if it does it will
        //be handled correctly
        final List<MSCRunnable> runnables = new ArrayList<MSCRunnable>(queue);
        queue.clear();
        delegate.execute(new Runnable() {
            @Override
            public void run() {
                for (MSCRunnable r : runnables) {
                    execute(r);
                }

            }
        });
    }

}
