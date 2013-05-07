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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.CountDownLatch;

import org.jboss.msc.test.utils.AbstractTransactionTest;
import org.jboss.msc.test.utils.CompletionListener;
import org.jboss.msc.test.utils.TestCommittable;
import org.jboss.msc.test.utils.TestExecutable;
import org.jboss.msc.test.utils.TestRevertible;
import org.jboss.msc.test.utils.TestValidatable;
import org.jboss.msc.txn.ExecuteContext;
import org.jboss.msc.txn.TaskController;
import org.jboss.msc.txn.Transaction;
import org.junit.Test;

/**
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
public final class OneParentTask_NoDeps_OneChildTask_NoDeps_TxnReverted_TestCase extends AbstractTransactionTest {

    /**
     * Scenario:
     * <UL>
     * <LI>parent task completes at EXECUTE</LI>
     * <LI>child task completes at EXECUTE</LI>
     * <LI>transaction rolled back</LI>
     * </UL>
     */
    @Test
    public void usecase1() throws Exception {
        final Transaction transaction = newTransaction();
        // preparing child task
        final TestExecutable<Void> e1 = new TestExecutable<Void>();
        final TestValidatable v1 = new TestValidatable();
        final TestRevertible r1 = new TestRevertible();
        final TestCommittable c1 = new TestCommittable();
        // installing parent task
        final TestExecutable<Void> e0 = new TestExecutable<Void>() {
            @Override
            public void executeInternal(final ExecuteContext<Void> ctx) {
                // installing child task
                final TaskController<Void> childController = newTask(ctx, e1, v1, r1, c1);
                assertNotNull(childController);
            }
        };
        final TestValidatable v0 = new TestValidatable();
        final TestRevertible r0 = new TestRevertible();
        final TestCommittable c0 = new TestCommittable();
        final TaskController<Void> parentController = newTask(transaction, e0, v0, r0, c0);
        assertNotNull(parentController);
        // preparing transaction
        prepare(transaction);
        assertCalled(e0);
        assertCalled(v0);
        assertNotCalled(r0);
        assertNotCalled(c0);
        assertCalled(e1);
        assertCalled(v1);
        assertNotCalled(r1);
        assertNotCalled(c1);
        assertCallOrder(e0, e1, v0, v1);
        // reverting transaction
        assertTrue(transaction.canCommit());
        rollback(transaction);
        assertCalled(e0);
        assertCalled(v0);
        assertCalled(r0);
        assertNotCalled(c0);
        assertCalled(e1);
        assertCalled(v1);
        assertCalled(r1);
        assertNotCalled(c1);
        assertCallOrder(e0, v0, r0);
        assertCallOrder(e1, v1, r1);
        assertCallOrder(e0, e1, v0, v1, r1, r0);
    }

    /**
     * Scenario:
     * <UL>
     * <LI>parent task completes at EXECUTE</LI>
     * <LI>child task cancels at EXECUTE</LI>
     * <LI>transaction rolled back</LI>
     * </UL>
     */
    @Test
    public void usecase2() throws Exception {
        final Transaction transaction = newTransaction();
        final CountDownLatch signal = new CountDownLatch(1);
        // preparing child task
        final TestExecutable<Void> e1 = new TestExecutable<Void>(true, signal);
        final TestValidatable v1 = new TestValidatable();
        final TestRevertible r1 = new TestRevertible();
        final TestCommittable c1 = new TestCommittable();
        // installing parent task
        final TestExecutable<Void> e0 = new TestExecutable<Void>(signal) {
            @Override
            public void executeInternal(final ExecuteContext<Void> ctx) {
                // installing child task
                final TaskController<Void> childController = newTask(ctx, e1, v1, r1, c1);
                assertNotNull(childController);
            }
        };
        final TestValidatable v0 = new TestValidatable();
        final TestRevertible r0 = new TestRevertible();
        final TestCommittable c0 = new TestCommittable();
        final TaskController<Void> parentController = newTask(transaction, e0, v0, r0, c0);
        assertNotNull(parentController);
        // reverting transaction
        final CompletionListener rollbackListener = new CompletionListener();
        transaction.rollback(rollbackListener);
        signal.countDown();
        rollbackListener.awaitCompletion();
        assertCalled(e0);
        assertNotCalled(v0);
        assertCalled(r0);
        assertNotCalled(c0);
        assertCalled(e1);
        assertNotCalled(v1);
        assertNotCalled(r1);
        assertNotCalled(c1);
        assertCallOrder(e0, e1, r0);
    }

    /**
     * Scenario:
     * <UL>
     * <LI>parent task cancels at EXECUTE</LI>
     * <LI>child task cancels at EXECUTE</LI>
     * <LI>transaction rolled back</LI>
     * </UL>
     */
    @Test
    public void usecase3() throws Exception {
        final Transaction transaction = newTransaction();
        final CountDownLatch signal = new CountDownLatch(1);
        // preparing child task
        final TestExecutable<Void> e1 = new TestExecutable<Void>(true, signal);
        final TestValidatable v1 = new TestValidatable();
        final TestRevertible r1 = new TestRevertible();
        final TestCommittable c1 = new TestCommittable();
        // installing parent task
        final TestExecutable<Void> e0 = new TestExecutable<Void>(true, signal) {
            @Override
            public void executeInternal(final ExecuteContext<Void> ctx) {
                // installing child task
                final TaskController<Void> childController = newTask(ctx, e1, v1, r1, c1);
                assertNotNull(childController);
            }
        };
        final TestValidatable v0 = new TestValidatable();
        final TestRevertible r0 = new TestRevertible();
        final TestCommittable c0 = new TestCommittable();
        final TaskController<Void> parentController = newTask(transaction, e0, v0, r0, c0);
        assertNotNull(parentController);
        // reverting transaction
        final CompletionListener rollbackListener = new CompletionListener();
        transaction.rollback(rollbackListener);
        signal.countDown();
        rollbackListener.awaitCompletion();
        assertCalled(e0);
        assertNotCalled(v0);
        assertNotCalled(r0);
        assertNotCalled(c0);
        // e1.wasCalled() can return either true or false, depends on threads scheduling
        assertNotCalled(v1);
        assertNotCalled(r1);
        assertNotCalled(c1);
        if (e1.wasCalled()) {
            assertCallOrder(e0, e1);
        }
    }

    /**
     * Scenario:
     * <UL>
     * <LI>parent task cancels at EXECUTE</LI>
     * <LI>child task completes at EXECUTE</LI>
     * <LI>transaction rolled back</LI>
     * </UL>
     */
    @Test
    public void usecase4() throws Exception {
        final Transaction transaction = newTransaction();
        final CountDownLatch signal = new CountDownLatch(1);
        // preparing child task
        final TestExecutable<Void> e1 = new TestExecutable<Void>(signal);
        final TestValidatable v1 = new TestValidatable();
        final TestRevertible r1 = new TestRevertible();
        final TestCommittable c1 = new TestCommittable();
        // installing parent task
        final TestExecutable<Void> e0 = new TestExecutable<Void>(true, signal) {
            @Override
            public void executeInternal(final ExecuteContext<Void> ctx) {
                // installing child task
                final TaskController<Void> childController = newTask(ctx, e1, v1, r1, c1);
                assertNotNull(childController);
            }
        };
        final TestValidatable v0 = new TestValidatable();
        final TestRevertible r0 = new TestRevertible();
        final TestCommittable c0 = new TestCommittable();
        final TaskController<Void> parentController = newTask(transaction, e0, v0, r0, c0);
        assertNotNull(parentController);
        // reverting transaction
        final CompletionListener rollbackListener = new CompletionListener();
        transaction.rollback(rollbackListener);
        signal.countDown();
        rollbackListener.awaitCompletion();
        assertCalled(e0);
        assertNotCalled(v0);
        assertNotCalled(r0);
        assertNotCalled(c0);
        // e1.wasCalled() can return either true or false, depends on threads scheduling
        assertNotCalled(v1);
        // r1.wasCalled() can return either true or false, depends on threads scheduling
        assertNotCalled(c1);
        if (e1.wasCalled()) {
            assertCallOrder(e0, e1, r1);
        }
    }
}
