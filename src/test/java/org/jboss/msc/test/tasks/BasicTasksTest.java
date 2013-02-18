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

import org.jboss.msc.test.utils.TrackingTask;
import org.jboss.msc.txn.ExecuteContext;
import org.jboss.msc.txn.InvalidTransactionStateException;
import org.jboss.msc.txn.TaskBuilder;
import org.jboss.msc.txn.TaskController;
import org.jboss.msc.txn.Transaction;
import org.junit.Test;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
public class BasicTasksTest extends AbstractTransactionTest {

    @Test
    public void testCommitFromListener() throws InterruptedException {
        final ThreadPoolExecutor executor = new ThreadPoolExecutor(8, 8, 0L, TimeUnit.DAYS, new LinkedBlockingQueue<Runnable>());
        final Transaction transaction = Transaction.create(executor);
        // install task
        final TrackingTask task = new TrackingTask();
        final TaskBuilder<Object> taskBuilder = transaction.newTask(task);
        final TaskController<Object> controller = taskBuilder.release();
        // prepare and commit transaction from listener
        prepareAndCommitFromListener(transaction);
        // asserts
        assertTrue(task.isCommitted());
        assertTrue(task.isExecuted());
        assertFalse(task.isReverted());
        assertTrue(task.isValidated());
        controller.getResult();
        assertTrue(executor.shutdownNow().isEmpty());
    }

    @Test
    public void testSimpleCommit() throws InterruptedException {
        final ThreadPoolExecutor executor = new ThreadPoolExecutor(8, 8, 0L, TimeUnit.DAYS, new LinkedBlockingQueue<Runnable>());
        final Transaction transaction = Transaction.create(executor);
        // install task
        final TrackingTask task = new TrackingTask();
        final TaskBuilder<Object> taskBuilder = transaction.newTask(task);
        final TaskController<Object> controller = taskBuilder.release();
        // prepare transaction
        prepare(transaction);
        // commit transaction
        commit(transaction);
        // asserts
        assertTrue(task.isCommitted());
        assertTrue(task.isExecuted());
        assertFalse(task.isReverted());
        assertTrue(task.isValidated());
        controller.getResult();
        assertTrue(executor.shutdownNow().isEmpty());
    }

    @Test
    public void testRollbackFromListener() throws InterruptedException {
        final ThreadPoolExecutor executor = new ThreadPoolExecutor(8, 8, 0L, TimeUnit.DAYS, new LinkedBlockingQueue<Runnable>());
        final Transaction transaction = Transaction.create(executor);
        // install task
        final TrackingTask task = new TrackingTask();
        final TaskBuilder<Object> taskBuilder = transaction.newTask(task);
        final TaskController<Object> controller = taskBuilder.release();
        // prepare and roll back transaction from listener
        prepareAndRollbackFromListener(transaction);
        // asserts
        assertFalse(task.isCommitted());
        assertTrue(task.isExecuted());
        assertTrue(task.isReverted());
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
        // prepare transaction
        prepare(transaction);
        // roll back transaction
        rollback(transaction);
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
                if (d > 0)
                    for (int i = 0; i < n; i++) {
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
        // prepare and commit transaction from listener
        prepareAndCommitFromListener(transaction);
        // asserts
        assertTrue(task.isCommitted());
        assertTrue(task.isExecuted());
        assertFalse(task.isReverted());
        assertTrue(task.isValidated());
        controller.getResult();
        executor.shutdown();
        executor.awaitTermination(5L, TimeUnit.SECONDS);
    }

    @Test
    public void testSimpleRollbackWithDependency() throws InterruptedException {
        final ThreadPoolExecutor executor = new ThreadPoolExecutor(8, 8, 0L, TimeUnit.DAYS, new LinkedBlockingQueue<Runnable>());
        final Transaction transaction = Transaction.create(executor);
        // instal first task
        final TrackingTask task1 = new TrackingTask();
        final TaskBuilder<Object> taskBuilder1 = transaction.newTask(task1);
        final TaskController<Object> controller1 = taskBuilder1.release();
        // instal second task depending on first one
        final TrackingTask task2 = new TrackingTask();
        final TaskBuilder<Object> taskBuilder2 = transaction.newTask(task2).addDependency(controller1);
        final TaskController<Object> controller2 = taskBuilder2.release();
        // prepare and roll back transaction from listener
        prepareAndRollbackFromListener(transaction);
        // asserts
        assertFalse(task1.isCommitted());
        assertTrue(task1.isExecuted());
        assertTrue(task1.isReverted());
        assertTrue(task1.isValidated());
        controller1.getResult();
        assertFalse(task2.isCommitted());
        assertTrue(task2.isExecuted());
        assertTrue(task2.isReverted());
        assertTrue(task2.isValidated());
        controller2.getResult();
        executor.shutdown();
        executor.awaitTermination(5L, TimeUnit.SECONDS);
    }

    @Test
    public void installNewTaskToPreparedTransaction() throws InterruptedException {
        final ThreadPoolExecutor executor = new ThreadPoolExecutor(8, 8, 0L, TimeUnit.DAYS, new LinkedBlockingQueue<Runnable>());
        final Transaction transaction = Transaction.create(executor);
        // install task 1
        final TrackingTask task1 = new TrackingTask();
        final TaskBuilder<Object> taskBuilder = transaction.newTask(task1);
        final TaskController<Object> controller = taskBuilder.release();
        // prepare transaction
        prepare(transaction);
        // install task 2 - should fail because transaction have been prepared
        final TrackingTask task2 = new TrackingTask();
        try {
            transaction.newTask(task2).release();
            fail("cannot add new tasks to prepared transaction");
        } catch (InvalidTransactionStateException expected) {
        }
        // rollback transaction
        rollback(transaction);
        // asserts
        assertFalse(task1.isCommitted());
        assertTrue(task1.isExecuted());
        assertTrue(task1.isReverted());
        assertTrue(task1.isValidated());
        assertFalse(task2.isCommitted());
        assertFalse(task2.isExecuted());
        assertFalse(task2.isReverted());
        assertFalse(task2.isValidated());
        controller.getResult();
        executor.shutdown();
        executor.awaitTermination(5L, TimeUnit.SECONDS);
    }

    @Test
    public void installNewTaskToCommitedTransaction() throws InterruptedException {
        final ThreadPoolExecutor executor = new ThreadPoolExecutor(8, 8, 0L, TimeUnit.DAYS, new LinkedBlockingQueue<Runnable>());
        final Transaction transaction = Transaction.create(executor);
        // install task 1
        final TrackingTask task1 = new TrackingTask();
        final TaskBuilder<Object> taskBuilder = transaction.newTask(task1);
        final TaskController<Object> controller = taskBuilder.release();
        // prepare and commit transaction from listener
        prepareAndCommitFromListener(transaction);
        // install task 2 - should fail because transaction have been commited
        final TrackingTask task2 = new TrackingTask();
        try {
            transaction.newTask(task2).release();
            fail("cannot add new tasks to committed transaction");
        } catch (InvalidTransactionStateException expected) {
        }
        // asserts
        assertTrue(task1.isCommitted());
        assertTrue(task1.isExecuted());
        assertFalse(task1.isReverted());
        assertTrue(task1.isValidated());
        assertFalse(task2.isCommitted());
        assertFalse(task2.isExecuted());
        assertFalse(task2.isReverted());
        assertFalse(task2.isValidated());
        controller.getResult();
        executor.shutdown();
        executor.awaitTermination(5L, TimeUnit.SECONDS);
    }

    @Test
    public void installNewTaskToRevertedTransaction() throws InterruptedException {
        final ThreadPoolExecutor executor = new ThreadPoolExecutor(8, 8, 0L, TimeUnit.DAYS, new LinkedBlockingQueue<Runnable>());
        final Transaction transaction = Transaction.create(executor);
        // install task 1
        final TrackingTask task1 = new TrackingTask();
        final TaskBuilder<Object> taskBuilder = transaction.newTask(task1);
        final TaskController<Object> controller = taskBuilder.release();
        // prepare and roll back transaction from listener
        prepareAndRollbackFromListener(transaction);
        // install task 2 - should fail because transaction have been rolled back
        final TrackingTask task2 = new TrackingTask();
        try {
            transaction.newTask(task2).release();
            fail("cannot add new tasks to committed transaction");
        } catch (InvalidTransactionStateException expected) {
        }
        // asserts
        assertFalse(task1.isCommitted());
        assertTrue(task1.isExecuted());
        assertTrue(task1.isReverted());
        assertTrue(task1.isValidated());
        assertFalse(task2.isCommitted());
        assertFalse(task2.isExecuted());
        assertFalse(task2.isReverted());
        assertFalse(task2.isValidated());
        controller.getResult();
        executor.shutdown();
        executor.awaitTermination(5L, TimeUnit.SECONDS);
    }
}
