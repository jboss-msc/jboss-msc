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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

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
public final class OneParentTask_NoDeps_TwoChildTasks_NoDeps_TxnReverted_TestCase extends AbstractTransactionTest {

    /**
     * Scenario:
     * <UL>
     * <LI>parent task completes at EXECUTE
     * <LI>child0 completes at EXECUTE</LI>
     * <LI>child1 completes at EXECUTE</LI>
     * <LI>no dependencies</LI>
     * <LI>transaction reverted</LI>
     * </UL>
     */
    @Test
    public void usecase1() throws Exception {
        final Transaction transaction = newTransaction();
        final CountDownLatch childValidateSignal = new CountDownLatch(1);
        final CountDownLatch childRollbackSignal = new CountDownLatch(1);
        // preparing child0 task
        final TestExecutable<Void> child0e = new TestExecutable<Void>();
        final TestValidatable child0v = new TestValidatable(childValidateSignal);
        final TestRevertible child0r = new TestRevertible(childRollbackSignal);
        final TestCommittable child0c = new TestCommittable();
        // preparing child1 task
        final TestExecutable<Void> child1e = new TestExecutable<Void>();
        final TestValidatable child1v = new TestValidatable(childValidateSignal);
        final TestRevertible child1r = new TestRevertible(childRollbackSignal);
        final TestCommittable child1c = new TestCommittable();
        // installing parent task
        final TestExecutable<Void> parent0e = new TestExecutable<Void>() {
            @Override
            public void executeInternal(final ExecuteContext<Void> ctx) {
                // installing child0 task
                final TaskController<Void> child0Controller = newTask(ctx, child0e, child0v, child0r, child0c);
                assertNotNull(child0Controller);
                // installing child1 task
                final TaskController<Void> child1Controller = newTask(ctx, child1e, child1v, child1r, child1c);
                assertNotNull(child1Controller);
            }
        };
        final TestValidatable parent0v = new TestValidatable();
        final TestRevertible parent0r = new TestRevertible();
        final TestCommittable parent0c = new TestCommittable();
        final TaskController<Void> parentController = newTask(transaction, parent0e, parent0v, parent0r, parent0c);
        assertNotNull(parentController);
        // preparing transaction - children validation is blocked
        final CompletionListener prepareListener = new CompletionListener();
        prepare(transaction, prepareListener);
        assertFalse(prepareListener.awaitCompletion(100, TimeUnit.MILLISECONDS));
        // let children to finish validate
        childValidateSignal.countDown();
        prepareListener.awaitCompletion();
        assertPrepared(transaction);
        // transaction prepared
        assertCalled(parent0e);
        assertCalled(child0e);
        assertCalled(child1e);
        assertCalled(parent0v);
        assertCalled(child0v);
        assertCalled(child1v);
        assertNotCalled(parent0r);
        assertNotCalled(child0r);
        assertNotCalled(child1r);
        assertNotCalled(parent0c);
        assertNotCalled(child0c);
        assertNotCalled(child1c);
        assertCallOrder(parent0e, child0e, parent0v, child0v);
        assertCallOrder(parent0e, child1e, parent0v, child1v);
        // reverting transaction - children rollback is blocked
        assertTrue(canCommit(transaction));
        final CompletionListener rollbackListener = new CompletionListener();
        rollback(transaction, rollbackListener);
        assertFalse(rollbackListener.awaitCompletion(100, TimeUnit.MILLISECONDS));
        // let children to finish commit
        childRollbackSignal.countDown();
        rollbackListener.awaitCompletion();
        assertReverted(transaction);
        // transaction committed
        assertCalled(parent0e);
        assertCalled(child0e);
        assertCalled(child1e);
        assertCalled(parent0v);
        assertCalled(child0v);
        assertCalled(child1v);
        assertCalled(parent0r);
        assertCalled(child0r);
        assertCalled(child1r);
        assertNotCalled(parent0c);
        assertNotCalled(child0c);
        assertNotCalled(child1c);
        assertCallOrder(parent0e, child0e, parent0v, child0v, child0r, parent0r);
        assertCallOrder(parent0e, child1e, parent0v, child1v, child1r, parent0r);
    }

    /**
     * Scenario:
     * <UL>
     * <LI>parent task completes at EXECUTE
     * <LI>child0 cancels at EXECUTE</LI>
     * <LI>child1 cancels at EXECUTE</LI>
     * <LI>no dependencies</LI>
     * <LI>transaction reverted</LI>
     * </UL>
     */
    @Test
    public void usecase2() throws Exception {
        final Transaction transaction = newTransaction();
        final CountDownLatch signal = new CountDownLatch(1);
        // preparing child0 task
        final TestExecutable<Void> child0e = new TestExecutable<Void>(true, signal);
        final TestValidatable child0v = new TestValidatable();
        final TestRevertible child0r = new TestRevertible();
        final TestCommittable child0c = new TestCommittable();
        // preparing child1 task
        final TestExecutable<Void> child1e = new TestExecutable<Void>(true, signal);
        final TestValidatable child1v = new TestValidatable();
        final TestRevertible child1r = new TestRevertible();
        final TestCommittable child1c = new TestCommittable();
        // installing parent task
        final TestExecutable<Void> parent0e = new TestExecutable<Void>(signal) {
            @Override
            public void executeInternal(final ExecuteContext<Void> ctx) {
                // installing child0 task
                final TaskController<Void> child0Controller = newTask(ctx, child0e, child0v, child0r, child0c);
                assertNotNull(child0Controller);
                // installing child1 task
                final TaskController<Void> child1Controller = newTask(ctx, child1e, child1v, child1r, child1c);
                assertNotNull(child1Controller);
            }
        };
        final TestValidatable parent0v = new TestValidatable();
        final TestRevertible parent0r = new TestRevertible();
        final TestCommittable parent0c = new TestCommittable();
        final TaskController<Void> parentController = newTask(transaction, parent0e, parent0v, parent0r, parent0c);
        assertNotNull(parentController);
        // reverting transaction
        final CompletionListener rollbackListener = new CompletionListener();
        rollback(transaction, rollbackListener);
        signal.countDown();
        rollbackListener.awaitCompletion();
        // assert parent0 calls
        assertCalled(parent0e);
        assertNotCalled(parent0v);
        assertCalled(parent0r);
        assertNotCalled(parent0c);
        // assert child0 calls
        assertCalled(child0e);
        assertNotCalled(child0v);
        assertNotCalled(child0r);
        assertNotCalled(child0c);
        // assert child1 calls
        assertCalled(child1e);
        assertNotCalled(child1v);
        assertNotCalled(child1r);
        assertNotCalled(child1c);
        assertCallOrder(parent0e, child0e, parent0r);
        assertCallOrder(parent0e, child1e, parent0r);
    }

    /**
     * Scenario:
     * <UL>
     * <LI>parent task completes at EXECUTE
     * <LI>child0 completes at EXECUTE</LI>
     * <LI>child1 cancels at EXECUTE</LI>
     * <LI>no dependencies</LI>
     * <LI>transaction reverted</LI>
     * </UL>
     */
    @Test
    public void usecase3() throws Exception {
        final Transaction transaction = newTransaction();
        final CountDownLatch signal = new CountDownLatch(1);
        // preparing child0 task
        final TestExecutable<Void> child0e = new TestExecutable<Void>(signal);
        final TestValidatable child0v = new TestValidatable();
        final TestRevertible child0r = new TestRevertible();
        final TestCommittable child0c = new TestCommittable();
        // preparing child1 task
        final TestExecutable<Void> child1e = new TestExecutable<Void>(true, signal);
        final TestValidatable child1v = new TestValidatable();
        final TestRevertible child1r = new TestRevertible();
        final TestCommittable child1c = new TestCommittable();
        // installing parent task
        final TestExecutable<Void> parent0e = new TestExecutable<Void>(signal) {
            @Override
            public void executeInternal(final ExecuteContext<Void> ctx) {
                // installing child0 task
                final TaskController<Void> child0Controller = newTask(ctx, child0e, child0v, child0r, child0c);
                assertNotNull(child0Controller);
                // installing child1 task
                final TaskController<Void> child1Controller = newTask(ctx, child1e, child1v, child1r, child1c);
                assertNotNull(child1Controller);
            }
        };
        final TestValidatable parent0v = new TestValidatable();
        final TestRevertible parent0r = new TestRevertible();
        final TestCommittable parent0c = new TestCommittable();
        final TaskController<Void> parentController = newTask(transaction, parent0e, parent0v, parent0r, parent0c);
        assertNotNull(parentController);
        // reverting transaction
        final CompletionListener rollbackListener = new CompletionListener();
        rollback(transaction, rollbackListener);
        signal.countDown();
        rollbackListener.awaitCompletion();
        // assert parent0 calls
        assertCalled(parent0e);
        assertNotCalled(parent0v);
        assertCalled(parent0r);
        assertNotCalled(parent0c);
        // assert child0 calls
        assertCalled(child0e);
        assertNotCalled(child0v);
        assertCalled(child0r);
        assertNotCalled(child0c);
        // assert child1 calls
        assertCalled(child1e);
        assertNotCalled(child1v);
        assertNotCalled(child1r);
        assertNotCalled(child1c);
        assertCallOrder(parent0e, child0e, child0r, parent0r);
        assertCallOrder(parent0e, child1e, parent0r);
    }

    /**
     * Scenario:
     * <UL>
     * <LI>parent task cancels at EXECUTE
     * <LI>child0 completes at EXECUTE</LI>
     * <LI>child1 cancels at EXECUTE</LI>
     * <LI>no dependencies</LI>
     * <LI>transaction reverted</LI>
     * </UL>
     */
    @Test
    public void usecase4() throws Exception {
        final Transaction transaction = newTransaction();
        final CountDownLatch signal = new CountDownLatch(1);
        // preparing child0 task
        final TestExecutable<Void> child0e = new TestExecutable<Void>(signal);
        final TestValidatable child0v = new TestValidatable();
        final TestRevertible child0r = new TestRevertible();
        final TestCommittable child0c = new TestCommittable();
        // preparing child1 task
        final TestExecutable<Void> child1e = new TestExecutable<Void>(true, signal);
        final TestValidatable child1v = new TestValidatable();
        final TestRevertible child1r = new TestRevertible();
        final TestCommittable child1c = new TestCommittable();
        // installing parent task
        final TestExecutable<Void> parent0e = new TestExecutable<Void>(true, signal) {
            @Override
            public void executeInternal(final ExecuteContext<Void> ctx) {
                // installing child0 task
                final TaskController<Void> child0Controller = newTask(ctx, child0e, child0v, child0r, child0c);
                assertNotNull(child0Controller);
                // installing child1 task
                final TaskController<Void> child1Controller = newTask(ctx, child1e, child1v, child1r, child1c);
                assertNotNull(child1Controller);
            }
        };
        final TestValidatable parent0v = new TestValidatable();
        final TestRevertible parent0r = new TestRevertible();
        final TestCommittable parent0c = new TestCommittable();
        final TaskController<Void> parentController = newTask(transaction, parent0e, parent0v, parent0r, parent0c);
        assertNotNull(parentController);
        // reverting transaction
        final CompletionListener rollbackListener = new CompletionListener();
        rollback(transaction, rollbackListener);
        signal.countDown();
        rollbackListener.awaitCompletion();
        // assert parent0 calls
        assertCalled(parent0e);
        assertNotCalled(parent0v);
        assertNotCalled(parent0r);
        assertNotCalled(parent0c);
        // assert child0 calls
        // child0e.wasCalled() can return either true or false, depends on threads scheduling
        assertNotCalled(child0v);
        // child0r.wasCalled() can return either true of false, depends on threads scheduling
        assertNotCalled(child0c);
        // assert child1 calls
        // child1e.wasCalled() can return either true or false, depends on threads scheduling
        assertNotCalled(child1v);
        assertNotCalled(child1r);
        assertNotCalled(child1c);
        if (child0e.wasCalled()) {
            assertCalled(child0r);
            assertCallOrder(parent0e, child0e, child0r);
        }
        if (child1e.wasCalled()) {
            assertCallOrder(parent0e, child1e);
        }
    }
}
