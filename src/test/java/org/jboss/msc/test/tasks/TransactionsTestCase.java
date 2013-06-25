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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.jboss.msc.test.utils.AbstractTransactionTest;
import org.jboss.msc.txn.DeadlockException;
import org.jboss.msc.txn.Listener;
import org.jboss.msc.txn.Transaction;
import org.junit.Test;

/**
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
public final class TransactionsTestCase extends AbstractTransactionTest {

    private static final int MAX_ACTIVE_TRANSACTIONS = 64;

    @Test
    public void testMaximumActiveTransactions() throws Exception {
        // create transactions to fill in the transaction id limit
        final Transaction[] transactions = new Transaction[MAX_ACTIVE_TRANSACTIONS];
        for (int i = 0; i < MAX_ACTIVE_TRANSACTIONS; i++) {
            transactions[i] = newTransaction();
        }
        // stress test maximum transaction limit
        for (int i = 0; i < 10; i++) {
            // stress test using commit approach
            try {
                newTransaction();
                fail("expected active transactions limit exceeded");
            } catch (final IllegalStateException expected) {
            }
            commit(transactions[0]);
            transactions[0] = newTransaction();
            // stress test using rollback approach
            try {
                newTransaction();
                fail("expected active transactions limit exceeded");
            } catch (final IllegalStateException expected) {
            }
            rollback(transactions[0]);
            transactions[0] = newTransaction();
            // stress test using prepare / commit approach
            try {
                newTransaction();
                fail("expected active transactions limit exceeded");
            } catch (final IllegalStateException expected) {
            }
            prepare(transactions[0]);
            commit(transactions[0]);
            transactions[0] = newTransaction();
            // stress test using prepare / rollback approach
            try {
                newTransaction();
                fail("expected active transactions limit exceeded");
            } catch (final IllegalStateException expected) {
            }
            prepare(transactions[0]);
            rollback(transactions[0]);
            transactions[0] = newTransaction();
        }
        // release all transactions to free all transaction id resources
        for (int i = 0; i < MAX_ACTIVE_TRANSACTIONS; i++) {
            rollback(transactions[i]);
        }
    }

    @Test
    public void testDeadlockRollbackVersion() throws Exception {
        testDeadlock(false, true);
    }

    @Test
    public void testDeadlockCommitVersion() throws Exception {
        testDeadlock(false, false);
    }

    @Test
    public void testDeadlockPrepareRollbackVersion() throws Exception {
        testDeadlock(true, true);
    }

    @Test
    public void testDeadlockPrepareCommitVersion() throws Exception {
        testDeadlock(true, false);
    }

    @Test
    public void testWaitForRollbackVersion() throws Exception {
        testWaitForQueueSplittedFromSeparateThread(false, true);
    }

    @Test
    public void testWaitForCommitVersion() throws Exception {
        testWaitForQueueSplittedFromSeparateThread(false, false);
    }

    @Test
    public void testWaitForPrepareRollbackVersion() throws Exception {
        testWaitForQueueSplittedFromSeparateThread(true, true);
    }

    @Test
    public void testWaitForPrepareCommitVersion() throws Exception {
        testWaitForQueueSplittedFromSeparateThread(true, false);
    }

    @Test
    public void testTransactionSelfWaits() throws Exception {
        final Transaction transaction = newTransaction();
        transaction.waitFor(transaction);
        rollback(transaction);
    }

    private void testDeadlock(final boolean callPrepare, final boolean callRollback) throws Exception {
        final int deadlockSize = 5;
        final ThreadPoolExecutor executor = newExecutor(deadlockSize);
        // prepare tasks
        final TransactionTask[] tasks = new TransactionTask[deadlockSize];
        final StringBuffer out = new StringBuffer();
        for (int i = 0; i < deadlockSize; i++) {
            tasks[i] = new TransactionTask(out, callPrepare, callRollback, i);
            tasks[i].setDependent(newTransaction());
            if (i > 0) {
                tasks[i].setDependency(tasks[i - 1].getDependent());
            }
        }
        // creating deadlock
        tasks[0].setDependency(tasks[deadlockSize - 1].getDependent());
        // submit tasks in reverse order - let's the fun begin
        for (int i = deadlockSize - 1; i >= 0; i--) {
            executor.execute(tasks[i]);
        }
        // ensure deadlock
        TransactionTask deadlockingTask = null;
        boolean deadlockDetected = false;
        do {
            for (int i = 0; i < deadlockSize; i++) {
                if (tasks[i].isTerminated()) {
                    assertTrue(tasks[i].getThrowable() instanceof DeadlockException);
                    deadlockingTask = tasks[i];
                    deadlockDetected = true;
                    break;
                }
            }
        } while (!deadlockDetected);
        assertEquals("", out.toString());
        // resolving deadlock
        deadlockingTask.setDependency(null);
        // submit fixed task
        executor.execute(deadlockingTask);
        // ensure all tasks will be finished
        boolean allTasksFinished = false;
        do {
            allTasksFinished = true;
            for (int i = 0; i < deadlockSize; i++) {
                if (!tasks[i].isTerminated()) {
                    allTasksFinished = false;
                    break;
                }
            }
        } while (!allTasksFinished);
        executor.shutdown();
        executor.awaitTermination(60, TimeUnit.SECONDS);
        // assert test output
        final String testOutput = out.toString();
        final String[] expectedStartedTxnOrder = new String[deadlockSize];
        // When deadlock is resolved the order in which transactions are executed is well ordered
        // The order really matter in this case (doesn't depend on threads scheduling)
        int txnOrder = deadlockingTask.getId();
        for (int i = 0; i < deadlockSize; i++) {
            expectedStartedTxnOrder[i] = "started" + txnOrder;
            txnOrder = (txnOrder + 1) % deadlockSize;
        }
        for (int i = 0; i < expectedStartedTxnOrder.length - 1; i++) {
            int txn1StartedIdx = testOutput.indexOf(expectedStartedTxnOrder[i]);
            int txn2StartedIdx = testOutput.indexOf(expectedStartedTxnOrder[i + 1]);
            assertTrue(txn1StartedIdx < txn2StartedIdx);
        }
        if (callPrepare) {
            // Ensure prepare() was called on all transactions.
            // The order doesn't matter (depends on threads scheduling).
            for (int i = 0; i < deadlockSize; i++) {
                assertTrue(testOutput.indexOf("prepared" + i) != -1);
            }
        }
        if (callRollback) {
            // Ensure rollback() was called on all transactions.
            // The order doesn't matter (depends on threads scheduling).
            for (int i = 0; i < deadlockSize; i++) {
                assertTrue(testOutput.indexOf("reverted" + i) != -1);
            }
        } else {
            // Ensure rollback() was called on all transactions.
            // The order doesn't matter (depends on threads scheduling).
            for (int i = 0; i < deadlockSize; i++) {
                assertTrue(testOutput.indexOf("committed" + i) != -1);
            }
        }
    }

    private void testWaitForQueueSplittedFromSeparateThread(final boolean callPrepare, final boolean callRollback)
            throws Exception {
        final ThreadPoolExecutor executor = newExecutor(4);
        // prepare tasks
        final Transaction txn0 = newTransaction();
        final Transaction txn1 = newTransaction();
        final Transaction txn2 = newTransaction();
        final StringBuffer out = new StringBuffer();
        final TransactionTask[] tasks = new TransactionTask[4];
        // create wait queue
        final TransactionTask task0 = new TransactionTask(out, callPrepare, callRollback, 0);
        final CountDownLatch task0Start = new CountDownLatch(1);
        task0.setDependent(txn0);
        task0.setStartSignal(task0Start);
        tasks[0] = task0;
        final TransactionTask task1_0 = new TransactionTask(out, callPrepare, callRollback, 1);
        task1_0.setDependent(txn1);
        task1_0.setDependency(txn0);
        tasks[1] = task1_0;
        final TransactionTask task2_1 = new TransactionTask(out, callPrepare, callRollback, 2);
        task2_1.setDependent(txn2);
        task2_1.setDependency(txn1);
        tasks[2] = task2_1;
        // creating separate thread that will execute second transaction from the beginning of the queue
        final TransactionTask specialTask = new TransactionTask(out, callPrepare, callRollback, 3);
        specialTask.setDependent(txn1);
        tasks[3] = specialTask;
        // submit tasks, task queue first, special task last
        for (int i = 0; i < tasks.length; i++) {
            executor.execute(tasks[i]);
        }
        // ensure tasks ordering
        boolean task2_1Terminated = false;
        do {
            if (task2_1.isTerminated()) {
                assertFalse(task0.isTerminated());
                task0Start.countDown();
                task2_1Terminated = true;
            }
        } while (!task2_1Terminated);
        // ensure all tasks are finished
        boolean allTasksFinished = false;
        do {
            allTasksFinished = true;
            for (int i = 0; i < tasks.length; i++) {
                if (!tasks[i].isTerminated()) {
                    allTasksFinished = false;
                    break;
                }
            }
        } while (!allTasksFinished);
        executor.shutdown();
        executor.awaitTermination(60, TimeUnit.SECONDS);
        // assert test output
        final String testOutput = out.toString();
        // The order in which transactions are executed is well defined.
        // The order really matter in this case (doesn't depend on threads scheduling)
        int specialTaskIndex = testOutput.indexOf("started3");
        int task2_1Index = testOutput.indexOf("started2");
        int task1_0Index = testOutput.indexOf("terminated1");
        int task0Index = testOutput.indexOf("started0");
        assertTrue(specialTaskIndex < task2_1Index);
        assertTrue(specialTaskIndex < task1_0Index);
        assertTrue(task2_1Index < task0Index);
        if (callPrepare) {
            // Ensure prepare() was called on all transactions except middle one.
            // The order doesn't matter (depends on threads scheduling).
            for (int i = 0; i < tasks.length; i++) {
                if (i != 1) {
                    assertTrue(testOutput.contains("prepared" + i));
                } else {
                    assertFalse(testOutput.contains("prepared" + i));
                }
            }
        }
        if (callRollback) {
            // Ensure rollback() was called on all transactions except middle one.
            // The order doesn't matter (depends on threads scheduling).
            for (int i = 0; i < tasks.length; i++) {
                if (i != 1) {
                    assertTrue(testOutput.contains("reverted" + i));
                } else {
                    assertFalse(testOutput.contains("reverted" + i));
                }
            }
        } else {
            // Ensure rollback() was called on all transactions except middle one.
            // The order doesn't matter (depends on threads scheduling).
            for (int i = 0; i < tasks.length; i++) {
                if (i != 1) {
                    assertTrue(testOutput.contains("committed" + i));
                } else {
                    assertFalse(testOutput.contains("committed" + i));
                }
            }
        }
    }

    private static final class TransactionTask implements Runnable {
        private final StringBuffer out;
        private final boolean prepare;
        private final boolean rollback;
        private final int id;
        private CountDownLatch startSignal;
        private CountDownLatch endSignal;
        private Transaction dependency;
        private Transaction dependent;
        private Throwable t;

        private TransactionTask(final StringBuffer out, final boolean prepare, final boolean rollback, final int id) {
            this.out = out;
            this.prepare = prepare;
            this.rollback = rollback;
            this.id = id;
        }

        @Override
        public void run() {
            synchronized (this) {
                endSignal = new CountDownLatch(prepare ? 2 : 1);
            }
            try {
                if (dependency != null) {
                    dependent.waitFor(dependency);
                }
                CountDownLatch startSignal = null;
                synchronized (this) {
                    startSignal = this.startSignal;
                }
                if (startSignal != null) {
                    startSignal.await();
                }
                if (dependent.isTerminated()) {
                    synchronized (out) {
                        out.append(" terminated").append(id);
                    }
                    endSignal.countDown();
                    if (prepare) {
                        endSignal.countDown();
                    }
                    return;
                }
                synchronized (out) {
                    out.append(" started").append(id);
                }
                if (prepare) {
                    dependent.prepare(new Listener<Transaction>() {
                        @Override
                        public void handleEvent(final Transaction subject) {
                            synchronized (out) {
                                out.append(" prepared").append(id);
                            }
                            synchronized (TransactionTask.this) {
                                endSignal.countDown();
                            }
                        }
                    });
                }
                if (!rollback) {
                    dependent.commit(new Listener<Transaction>() {
                        @Override
                        public void handleEvent(final Transaction subject) {
                            synchronized (out) {
                                out.append(" committed").append(id);
                            }
                            synchronized (TransactionTask.this) {
                                endSignal.countDown();
                            }
                        }
                    });
                } else {
                    dependent.rollback(new Listener<Transaction>() {
                        @Override
                        public void handleEvent(final Transaction subject) {
                            synchronized (out) {
                                out.append(" reverted").append(id);
                            }
                            synchronized (TransactionTask.this) {
                                endSignal.countDown();
                            }
                        }
                    });
                }
            } catch (final Throwable t) {
                synchronized (this) {
                    this.t = t;
                    endSignal.countDown();
                    if (prepare) {
                        endSignal.countDown();
                    }
                }
            }
        }

        private void setDependency(final Transaction dependency) {
            this.dependency = dependency;
        }

        @SuppressWarnings("unused")
        private Transaction getDependency() {
            return this.dependency;
        }

        private void setDependent(final Transaction dependent) {
            this.dependent = dependent;
        }

        private Transaction getDependent() {
            return this.dependent;
        }

        private synchronized void setStartSignal(final CountDownLatch startSignal) {
            this.startSignal = startSignal;
        }

        private synchronized Throwable getThrowable() {
            return t;
        }

        private int getId() {
            return id;
        }

        private synchronized boolean isTerminated() throws InterruptedException {
            return endSignal != null ? endSignal.await(50, TimeUnit.NANOSECONDS) : false;
        }
    }
}
