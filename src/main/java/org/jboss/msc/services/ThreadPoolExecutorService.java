/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat Middleware LLC, and individual contributors
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

package org.jboss.msc.services;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.jboss.msc.value.Value;
import org.jboss.threads.JBossExecutors;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class ThreadPoolExecutorService implements Service {
    private final Value<ExecutorService> executorServiceValue = new Value<ExecutorService>() {
        public ExecutorService getValue() throws IllegalStateException {
            return getExecutorService();
        }
    };

    private boolean allowCoreTimeout = false;
    private int corePoolSize = 10;
    private int maximumPoolSize = 40;
    private long keepAliveTime = 30L;
    private TimeUnit unit = TimeUnit.SECONDS;
    private BlockingQueue<Runnable> workQueue = new ArrayBlockingQueue<Runnable>(100);
    private ThreadFactory threadFactory = Executors.defaultThreadFactory();
    private RejectedExecutionHandler handler = new ThreadPoolExecutor.AbortPolicy();

    public ThreadPoolExecutorService() {
    }

    public synchronized boolean isAllowCoreTimeout() {
        return allowCoreTimeout;
    }

    public synchronized void setAllowCoreTimeout(final boolean allowCoreTimeout) {
        this.allowCoreTimeout = allowCoreTimeout;
        final ThreadPoolExecutor realExecutor = this.realExecutor;
        if (realExecutor != null) {
            realExecutor.allowCoreThreadTimeOut(allowCoreTimeout);
        }
    }

    public synchronized int getCorePoolSize() {
        return corePoolSize;
    }

    public synchronized void setCorePoolSize(final int corePoolSize) {
        this.corePoolSize = corePoolSize;
        final ThreadPoolExecutor realExecutor = this.realExecutor;
        if (realExecutor != null) {
            realExecutor.setCorePoolSize(corePoolSize);
        }
    }

    public synchronized int getMaximumPoolSize() {
        return maximumPoolSize;
    }

    public synchronized void setMaximumPoolSize(final int maximumPoolSize) {
        this.maximumPoolSize = maximumPoolSize;
        final ThreadPoolExecutor realExecutor = this.realExecutor;
        if (realExecutor != null) {
            realExecutor.setMaximumPoolSize(maximumPoolSize);
        }
    }

    public synchronized long getKeepAliveTime(final TimeUnit unit) {
        return unit.convert(keepAliveTime, this.unit);
    }

    public synchronized void setKeepAliveTime(final long keepAliveTime, final TimeUnit unit) {
        if (unit == null) {
            throw new IllegalArgumentException("unit is null");
        }
        this.keepAliveTime = keepAliveTime;
        this.unit = unit;
        final ThreadPoolExecutor realExecutor = this.realExecutor;
        if (realExecutor != null) {
            realExecutor.setKeepAliveTime(keepAliveTime, unit);
        }
    }

    public synchronized BlockingQueue<Runnable> getWorkQueue() {
        return workQueue;
    }

    public synchronized void setWorkQueue(final BlockingQueue<Runnable> workQueue) {
        if (workQueue == null) {
            throw new IllegalArgumentException("workQueue is null");
        }
        this.workQueue = workQueue;
    }

    public synchronized ThreadFactory getThreadFactory() {
        return threadFactory;
    }

    public synchronized void setThreadFactory(final ThreadFactory threadFactory) {
        if (threadFactory == null) {
            throw new IllegalArgumentException("threadFactory is null");
        }
        this.threadFactory = threadFactory;
        final ThreadPoolExecutor realExecutor = this.realExecutor;
        if (realExecutor != null) {
            realExecutor.setThreadFactory(threadFactory);
        }
    }

    public synchronized RejectedExecutionHandler getHandler() {
        return handler;
    }

    public synchronized void setHandler(final RejectedExecutionHandler handler) {
        if (handler == null) {
            throw new IllegalArgumentException("handler is null");
        }
        this.handler = handler;
        final ThreadPoolExecutor realExecutor = this.realExecutor;
        if (realExecutor != null) {
            realExecutor.setRejectedExecutionHandler(handler);
        }
    }

    private ExecutorService publicExecutor;
    private ThreadPoolExecutor realExecutor;
    private StopContext stopContext;

    public Value<ExecutorService> getExecutorValue() {
        return executorServiceValue;
    }

    public synchronized ExecutorService getExecutorService() {
        final ExecutorService publicExecutor = this.publicExecutor;
        if (publicExecutor == null) {
            throw new IllegalStateException();
        }
        return publicExecutor;
    }

    public synchronized void start(final StartContext context) throws StartException {
        realExecutor = new OurExecutor(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory, handler);
        realExecutor.allowCoreThreadTimeOut(allowCoreTimeout);
        publicExecutor = JBossExecutors.protectedExecutorService(realExecutor);
    }

    public synchronized void stop(final StopContext context) {
        stopContext = context;
        context.asynchronous();
        realExecutor.shutdown();
    }

    private final class OurExecutor extends ThreadPoolExecutor {

        OurExecutor(final int corePoolSize, final int maximumPoolSize, final long keepAliveTime, final TimeUnit unit, final BlockingQueue<Runnable> workQueue, final ThreadFactory threadFactory, final RejectedExecutionHandler handler) {
            super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory, handler);
        }

        protected void terminated() {
            synchronized (ThreadPoolExecutorService.this) {
                final StopContext context = stopContext;
                if (context != null) {
                    context.complete();
                    stopContext = null;
                    publicExecutor = null;
                    realExecutor = null;
                }
            }
        }
    }
}
