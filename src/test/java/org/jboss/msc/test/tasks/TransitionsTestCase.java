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

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.jboss.msc.test.utils.AbstractTransactionTest;
import org.jboss.msc.test.utils.CancelingExecutable;
import org.jboss.msc.test.utils.CancelingValidatable;
import org.jboss.msc.test.utils.CompletingExecutable;
import org.jboss.msc.test.utils.CompletionListener;
import org.jboss.msc.txn.Executable;
import org.jboss.msc.txn.TaskController;
import org.jboss.msc.txn.Transaction;
import org.jboss.msc.txn.Validatable;
import org.junit.Test;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

/**
 * TODO: javadoc
 *
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
public final class TransitionsTestCase extends AbstractTransactionTest {

    @Test
    public void allTasksCancelled_transactionCommitted() throws InterruptedException {
        final Transaction transaction = newTransaction();
        // installing canceling e1 task
        final Executable<Void> e1 = new CancelingExecutable<Void>();
        final TaskController<Void> e1Controller = transaction.newTask(e1).release();
        assertNotNull(e1Controller);
        // installing canceling e2 task depending on e1 task
        final Executable<Void> e2 = new CancelingExecutable<Void>();
        final TaskController<Void> e2Controller = transaction.newTask(e2).addDependency(e1Controller).release();
        assertNotNull(e2Controller);
        // preparing transaction
        prepare(transaction);
        assertTrue(transaction.canCommit());
        commit(transaction);
    }

    @Test
    public void someTasksExecutedSomeTasksCancelled_transactionCommitted() throws InterruptedException {
        final Transaction transaction = newTransaction();
        // installing completing e0 task
        final Executable<Void> e0 = new CompletingExecutable<Void>();
        final TaskController<Void> e0Controller = transaction.newTask(e0).release();
        assertNotNull(e0Controller);
        // installing canceling e1 task
        final Executable<Void> e1 = new CancelingExecutable<Void>();
        final TaskController<Void> e1Controller = transaction.newTask(e1).release();
        assertNotNull(e1Controller);
        // installing canceling e2 task depending on e1 task
        final Executable<Void> e2 = new CancelingExecutable<Void>();
        final TaskController<Void> e2Controller = transaction.newTask(e2).addDependency(e1Controller).release();
        assertNotNull(e2Controller);
        // preparing transaction
        prepare(transaction);
        assertTrue(transaction.canCommit());
        commit(transaction);
    }

    @Test
    public void allTasksCancelled_transactionRolledBack() throws InterruptedException {
        final Transaction transaction = newTransaction();
        // installing canceling e1 task
        final Executable<Void> e1 = new CancelingExecutable<Void>();
        final TaskController<Void> e1Controller = transaction.newTask(e1).release();
        assertNotNull(e1Controller);
        // installing canceling e2 task depending on e1 task
        final Executable<Void> e2 = new CancelingExecutable<Void>();
        final TaskController<Void> e2Controller = transaction.newTask(e2).addDependency(e1Controller).release();
        assertNotNull(e2Controller);
        // preparing transaction
        prepare(transaction);
        assertTrue(transaction.canCommit());
        commit(transaction);
    }

    @Test
    public void someTasksExecutedSomeTasksCancelled_transactionRolledBack() throws InterruptedException {
        final Transaction transaction = newTransaction();
        // installing completing e0 task
        final Executable<Void> e0 = new CompletingExecutable<Void>();
        final TaskController<Void> e0Controller = transaction.newTask(e0).release();
        assertNotNull(e0Controller);
        // installing canceling e1 task
        final Executable<Void> e1 = new CancelingExecutable<Void>();
        final TaskController<Void> e1Controller = transaction.newTask(e1).release();
        assertNotNull(e1Controller);
        // installing canceling e2 task depending on e1 task
        final Executable<Void> e2 = new CancelingExecutable<Void>();
        final TaskController<Void> e2Controller = transaction.newTask(e2).addDependency(e1Controller).release();
        assertNotNull(e2Controller);
        // preparing transaction
        prepare(transaction);
        assertTrue(transaction.canCommit());
        commit(transaction);
    }
    
    @Test
    public void asdf() throws InterruptedException { // TODO: rename method
        final Transaction transaction = newTransaction();
        // installing task 1
        final CountDownLatch task1Signal = new CountDownLatch(1);
        final Executable<Void> task1Executable = new CompletingExecutable<Void>();
        final Validatable task1Validatable = new CancelingValidatable(task1Signal);
        final TaskController<Void> task1Controller = transaction.newTask(task1Executable).setValidatable(task1Validatable).release();
        assertNotNull(task1Controller);
        // installing task 2, depending on task 1
        final CountDownLatch task2Signal = new CountDownLatch(1);
        final Executable<Void> e2 = new CancelingExecutable<Void>(task2Signal);
        final TaskController<Void> e2Controller = transaction.newTask(e2).addDependency(task1Controller).release();
        assertNotNull(e2Controller);
        final CompletionListener txnListener = new CompletionListener();
        transaction.prepare(txnListener);
        task1Signal.countDown();
        assertFalse(txnListener.awaitCompletion(5, TimeUnit.SECONDS));
        task2Signal.countDown();
        txnListener.awaitCompletion();
    }
}

