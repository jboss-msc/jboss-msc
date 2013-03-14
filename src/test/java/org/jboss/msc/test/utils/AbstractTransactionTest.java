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

package org.jboss.msc.test.utils;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.jboss.msc.txn.Committable;
import org.jboss.msc.txn.Executable;
import org.jboss.msc.txn.InvalidTransactionStateException;
import org.jboss.msc.txn.Revertible;
import org.jboss.msc.txn.TaskController;
import org.jboss.msc.txn.Transaction;
import org.jboss.msc.txn.TransactionRolledBackException;
import org.jboss.msc.txn.Validatable;
import org.junit.After;
import org.junit.Before;

/**
 * Test base providing some utility methods.
 * 
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
public abstract class AbstractTransactionTest {

    protected ThreadPoolExecutor defaultExecutor;

    @Before
    public void setUp() {
        defaultExecutor = newExecutor(8, true);
    }

    @After
    public void tearDown() {
        defaultExecutor.shutdown();
        try {
            defaultExecutor.awaitTermination(60, TimeUnit.SECONDS);
        } catch (final InterruptedException ignored) {}
        assertTrue(defaultExecutor.getQueue().size() == 0);
    }
    
    protected static ThreadPoolExecutor newExecutor(final int maximumPoolSize) {
        return newExecutor(maximumPoolSize, false);
    }

    protected static ThreadPoolExecutor newExecutor(final int maximumPoolSize, final boolean prestartCoreThreads) {
        final ThreadPoolExecutor executor = new ThreadPoolExecutor(maximumPoolSize, maximumPoolSize, 0L, TimeUnit.DAYS, new LinkedBlockingQueue<Runnable>());
        if (prestartCoreThreads) {
            executor.prestartAllCoreThreads();
        }
        return executor;
    }

    protected static <T> TaskController<T> newTask(final Transaction transaction, final Executable<T> e, final Validatable v, final Revertible r, final Committable c, final TaskController<?>... dependencies) {
        return transaction.newTask(e).addDependencies(dependencies).setValidatable(v).setRevertible(r).setCommittable(c).release();
    }

    protected Transaction newTransaction() {
        assertNotNull(defaultExecutor);
        return Transaction.create(defaultExecutor);
    }

    protected Transaction newTransaction(final Executor executor) {
        assertNotNull(executor);
        return Transaction.create(executor);
    }

    protected static void assertCalled(final TestTask task) {
        assertNotNull(task);
        assertTrue("Task " + task + " was not called", task.wasCalled());
    }

    protected static void assertNotCalled(final TestTask task) {
        assertNotNull(task);
        assertFalse("Task " + task + " was called", task.wasCalled());
    }

    protected static void assertCallOrder(final TestTask firstTask, final TestTask secondTask) {
        assertCalled(firstTask);
        assertCalled(secondTask);
        assertTrue("Task " + firstTask + " have been called after " + secondTask, firstTask.getCallTime() <= secondTask.getCallTime());
    }

    protected static void assertCallOrder(final TestTask firstTask, final TestTask secondTask, final TestTask... otherTasks) {
        assertCallOrder(firstTask, secondTask);
        if (otherTasks != null && otherTasks.length > 0) {
            TestTask previousTask = secondTask;
            for (final TestTask currentTask : otherTasks) {
                assertCallOrder(previousTask, currentTask);
                previousTask = currentTask;
            }
        }
    }

    private static void assertPrepared(final Transaction transaction) {
        assertNotNull(transaction);
        assertTrue(transaction.canCommit());
        try {
            transaction.prepare(null);
            fail("Cannot call prepare() more than once on transaction");
        } catch (final InvalidTransactionStateException expected) {
        }
    }

    private static void assertReverted(final Transaction transaction) {
        assertNotNull(transaction);
        assertFalse(transaction.canCommit());
        // ensure it's not possible to call prepare on rolled back transaction
        try {
            transaction.prepare(null);
            fail("Cannot call prepare() on rolled back transaction");
        } catch (final TransactionRolledBackException expected) {
        }
        try {
            transaction.commit(null);
            fail("Cannot call commit() on rolled back transaction");
        } catch (final TransactionRolledBackException expected) {
        }
        // ensure it's not possible to call rollback on rolled back transaction more than once
        try {
            transaction.rollback(null);
            fail("Cannot call rollback() on rolled back transaction");
        } catch (final TransactionRolledBackException expected) {
        }
        assertTrue(transaction.isTerminated());
    }

    private static void assertCommitted(final Transaction transaction) {
        assertNotNull(transaction);
        assertFalse(transaction.canCommit());
        // ensure it's not possible to call prepare() on committed transaction
        try {
            transaction.prepare(null);
            fail("Cannot call prepare() on committed transaction");
        } catch (final InvalidTransactionStateException expected) {
        }
        // ensure it's not possible to call commit on committed transaction more than once
        try {
            transaction.commit(null);
            fail("Cannot call commit() on committed transaction");
        } catch (final InvalidTransactionStateException expected) {
        }
        // ensure it's not possible to call rollback() on committed transaction
        try {
            transaction.rollback(null);
            fail("Cannot call rollback() on committed transaction");
        } catch (final InvalidTransactionStateException expected) {
        }
        assertTrue(transaction.isTerminated());
    }

    protected static void prepare(final Transaction transaction) throws InterruptedException {
        assertNotNull(transaction);
        final CompletionListener prepareListener = new CompletionListener();
        transaction.prepare(prepareListener);
        prepareListener.awaitCompletion();
        assertPrepared(transaction);
    }

    protected static void commit(final Transaction transaction) throws InterruptedException {
        assertNotNull(transaction);
        final CompletionListener commitListener = new CompletionListener();
        transaction.commit(commitListener);
        commitListener.awaitCompletion();
        assertCommitted(transaction);
    }

    protected static void rollback(final Transaction transaction) throws InterruptedException {
        assertNotNull(transaction);
        final CompletionListener rollbackListener = new CompletionListener();
        transaction.rollback(rollbackListener);
        rollbackListener.awaitCompletion();
        assertReverted(transaction);
    }

    protected static void prepareAndRollbackFromListener(final Transaction transaction) throws InterruptedException {
        assertNotNull(transaction);
        final RevertingListener transactionListener = new RevertingListener();
        transaction.prepare(transactionListener);
        transactionListener.awaitRollback();
        assertReverted(transaction);
    }

    protected static void prepareAndCommitFromListener(final Transaction transaction) throws InterruptedException {
        assertNotNull(transaction);
        final CommittingListener transactionListener = new CommittingListener();
        transaction.prepare(transactionListener);
        transactionListener.awaitCommit();
        assertCommitted(transaction);
    }

}
