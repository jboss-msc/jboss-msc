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

import org.jboss.msc.test.utils.CommittingListener;
import org.jboss.msc.test.utils.CompletionListener;
import org.jboss.msc.test.utils.RevertingListener;
import org.jboss.msc.txn.InvalidTransactionStateException;
import org.jboss.msc.txn.Transaction;
import org.jboss.msc.txn.TransactionRolledBackException;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Test base providing some utility methods.
 * 
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
public abstract class AbstractTransactionTest {

    protected static final void assertPrepared(final Transaction transaction) {
        assertNotNull(transaction);
        assertTrue(transaction.canCommit());
        try {
            transaction.prepare(null);
            fail("Cannot call prepare() more than once on transaction object");
        } catch (final InvalidTransactionStateException expected) {
        }
    }

    protected static final void assertReverted(final Transaction transaction) {
        assertNotNull(transaction);
        // assertFalse(transaction.canCommit()); // TODO: can commit() when transaction have been rolled back?
        try {
            transaction.commit(null); // TODO: shouldn't we also provide parameterless version of commit(), prepare() and rollback()?
            fail("Cannot call commit() on rolled back transaction object");
        } catch (final TransactionRolledBackException expected) {
        }
    }

    protected static final void assertCommitted(final Transaction transaction) {
        assertNotNull(transaction);
        // assertFalse(transaction.canCommit()); // TODO: can commit() when transaction have been committed?
        try {
            transaction.commit(null);
            fail("Cannot call commit() more than once on transaction object");
        } catch (final InvalidTransactionStateException expected) {
        }
    }

    protected static final void prepare(final Transaction transaction) throws InterruptedException {
        assertNotNull(transaction);
        final CompletionListener prepareListener = new CompletionListener();
        transaction.prepare(prepareListener);
        prepareListener.awaitCompletion();
        assertPrepared(transaction);
    }

    protected static final void commit(final Transaction transaction) throws InterruptedException {
        assertNotNull(transaction);
        final CompletionListener commitListener = new CompletionListener();
        transaction.commit(commitListener);
        commitListener.awaitCompletion();
        assertCommitted(transaction);
    }

    protected static final void rollback(final Transaction transaction) throws InterruptedException {
        assertNotNull(transaction);
        final CompletionListener rollbackListener = new CompletionListener();
        transaction.rollback(rollbackListener);
        rollbackListener.awaitCompletion();
        assertReverted(transaction);
    }

    protected static final void prepareAndRollbackFromListener(final Transaction transaction) throws InterruptedException {
        assertNotNull(transaction);
        final RevertingListener transactionListener = new RevertingListener();
        transaction.prepare(transactionListener);
        transactionListener.awaitRollback();
        assertReverted(transaction);
    }

    protected static final void prepareAndCommitFromListener(final Transaction transaction) throws InterruptedException {
        assertNotNull(transaction);
        final CommittingListener transactionListener = new CommittingListener();
        transaction.prepare(transactionListener);
        transactionListener.awaitCommit();
        assertCommitted(transaction);
    }

}
