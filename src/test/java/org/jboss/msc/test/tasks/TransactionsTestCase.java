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

import org.jboss.msc.test.utils.AbstractTransactionTest;
import org.jboss.msc.txn.Transaction;
import org.junit.Test;

import static org.junit.Assert.fail;

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
}
