package org.jboss.msc.service;

/**
 * A runnable that contains the notion of forwards and backwards tasks. Only tasks of the same type can run concurrently.
 *
 * @author Stuart Douglas
 */
interface MSCRunnable extends Runnable {

    boolean isForwardTask();

    /**
     *
     * @return <code>true</code> if it does not matter which direction the executor is in for this task to run
     */
    boolean isBiDirectional();
}
