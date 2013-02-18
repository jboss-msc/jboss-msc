/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2013 Red Hat, Inc., and individual contributors
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

package org.jboss.msc.test.tasks;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.jboss.msc.test.utils.CommittingListener;
import org.jboss.msc.test.utils.RevertingListener;
import org.jboss.msc.test.utils.TrackingTask;
import org.jboss.msc.txn.ExecuteContext;
import org.jboss.msc.txn.TaskBuilder;
import org.jboss.msc.txn.TaskController;
import org.jboss.msc.txn.Transaction;
import org.junit.Test;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
public class BasicTasksTest {

    @Test
    public void testSimpleExecute() throws InterruptedException {
        final ThreadPoolExecutor executor = new ThreadPoolExecutor(8, 8, 0L, TimeUnit.DAYS, new LinkedBlockingQueue<Runnable>());
        final Transaction transaction = Transaction.create(executor);
        // install task
        final TrackingTask task = new TrackingTask();
        final TaskBuilder<Object> taskBuilder = transaction.newTask(task);
        final TaskController<Object> controller = taskBuilder.release();
        // commit transaction from listener
        final CommittingListener transactionListener = new CommittingListener();
        transaction.prepare(transactionListener);
        transactionListener.awaitCommit();
        // asserts
        assertTrue(task.isCommitted());
        assertTrue(task.isExecuted());
        assertFalse(task.isReverted());
        assertTrue(task.isValidated());
        controller.getResult();
        assertTrue(executor.shutdownNow().isEmpty());
    }

    @Test
    public void testSimpleRollback() throws InterruptedException {
        final ThreadPoolExecutor executor = new ThreadPoolExecutor(8, 8, 0L, TimeUnit.DAYS, new LinkedBlockingQueue<Runnable>());
        final Transaction transaction = Transaction.create(executor);
        // install task
        final TrackingTask task = new TrackingTask();
        final TaskBuilder<Object> taskBuilder = transaction.newTask(task);
        final TaskController<Object> controller = taskBuilder.release();
        // roll back transaction from listener
        final RevertingListener transactionListener = new RevertingListener();
        transaction.prepare(transactionListener);
        transactionListener.awaitRollback();
        // asserts
        assertFalse(task.isCommitted());
        assertTrue(task.isExecuted());
        assertTrue(task.isReverted());
        assertTrue(task.isValidated());
        controller.getResult();
        assertTrue(executor.shutdownNow().isEmpty());
    }

    @Test
    public void testSimpleChildren() throws InterruptedException {
        final ThreadPoolExecutor executor = new ThreadPoolExecutor(8, 8, 0L, TimeUnit.DAYS, new LinkedBlockingQueue<Runnable>());
        final Transaction transaction = Transaction.create(executor);
        class Task extends TrackingTask {
            private final int n;
            private final int d;

            Task(final int n, final int d) {
                this.n = n;
                this.d = d;
            }

            public void execute(final ExecuteContext<Object> context) {
                if (d > 0) for (int i = 0; i < n; i ++) {
                    Task task = new Task(n, d - 1);
                    context.newTask(task).release();
                }
                super.execute(context);
            }
        }
        // install task
        Task task = new Task(3, 4);
        final TaskBuilder<Object> taskBuilder = transaction.newTask(task);
        final TaskController<Object> controller = taskBuilder.release();
        // commit transaction from listener
        final CommittingListener transactionListener = new CommittingListener();
        transaction.prepare(transactionListener);
        transactionListener.awaitCommit();
        // asserts
        assertTrue(task.isCommitted());
        assertTrue(task.isExecuted());
        assertFalse(task.isReverted());
        assertTrue(task.isValidated());
        controller.getResult();
        assertTrue(executor.shutdownNow().isEmpty());
    }
}
