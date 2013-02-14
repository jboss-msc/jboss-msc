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
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.jboss.msc.txn.Executable;
import org.jboss.msc.txn.ExecuteContext;
import org.jboss.msc.txn.Revertible;
import org.jboss.msc.txn.RollbackContext;
import org.jboss.msc.txn.TaskBuilder;
import org.jboss.msc.txn.TaskController;
import org.jboss.msc.txn.Transaction;
import org.jboss.msc.value.Listener;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

/**
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public class BasicTasksTest {

    @Test
    public void testSimpleExecute() throws InterruptedException {
        final ThreadPoolExecutor executor = new ThreadPoolExecutor(8, 8, 0L, TimeUnit.DAYS, new LinkedBlockingQueue<Runnable>());
        final Transaction transaction = Transaction.create(executor);
        final AtomicBoolean ran = new AtomicBoolean();
        final TaskBuilder<Object> taskBuilder = transaction.newTask(new Executable<Object>() {
            public void execute(final ExecuteContext<Object> context) {
                ran.set(true);
                context.complete();
            }
        });
        final TaskController<Object> controller = taskBuilder.release();
        final CountDownLatch latch = new CountDownLatch(1);
        transaction.prepare(new Listener<Transaction>() {
            public void handleEvent(final Transaction subject) {
                subject.commit(new Listener<Transaction>() {
                    public void handleEvent(final Transaction subject) {
                        latch.countDown();
                    }
                });
            }
        });
        latch.await();
        assertTrue(ran.get());
        controller.getResult();
        assertTrue(executor.shutdownNow().isEmpty());
    }

    @Test
    public void testSimpleRollback() throws InterruptedException {
        final ThreadPoolExecutor executor = new ThreadPoolExecutor(8, 8, 0L, TimeUnit.DAYS, new LinkedBlockingQueue<Runnable>());
        final Transaction transaction = Transaction.create(executor);
        final AtomicBoolean ran = new AtomicBoolean();
        final AtomicBoolean reverted = new AtomicBoolean();
        class Task implements Executable<Object>, Revertible {

            public void execute(final ExecuteContext<Object> context) {
                ran.set(true);
                context.complete();
            }

            public void rollback(final RollbackContext context) {
                reverted.set(true);
                context.complete();
            }
        }
        final TaskBuilder<Object> taskBuilder = transaction.newTask(new Task());
        final TaskController<Object> controller = taskBuilder.release();
        final CountDownLatch latch = new CountDownLatch(1);
        transaction.prepare(new Listener<Transaction>() {
            public void handleEvent(final Transaction subject) {
                subject.rollback(new Listener<Transaction>() {
                    public void handleEvent(final Transaction subject) {
                        latch.countDown();
                    }
                });
            }
        });
        latch.await();
        assertTrue(ran.get());
        assertTrue(reverted.get());
        controller.getResult();
        assertTrue(executor.shutdownNow().isEmpty());
    }
}
