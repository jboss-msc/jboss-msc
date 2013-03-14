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

package org.jboss.msc.txn;

import static org.jboss.msc.txn.Bits.allAreSet;
import static org.jboss.msc._private.MSCLogger.TXN;

import java.util.IdentityHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Shared thread-safe utility class that keeps track of active transactions and their dependencies.
 * 
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
final class Transactions {

    private static final Lock lock = new ReentrantLock();
    private static final int maxTxns = 64;
    private static final Transaction[] activeTxns = new Transaction[maxTxns];
    private static final Map<Transaction, List<Condition>> txnConds = new IdentityHashMap<Transaction, List<Condition>>(maxTxns);
    private static final long[] txnDeps = new long[maxTxns];

    private Transactions() {
        // forbidden inheritance
    }

    /**
     * Register transaction.
     * 
     * @param txn new active transaction
     * @throws IllegalStateException if there are too many active transactions
     */
    static void register(final Transaction txn) throws IllegalStateException {
        lock.lock();
        try {
            for (int i = 0; i < maxTxns; i++) {
                if (activeTxns[i] == null) {
                    activeTxns[i] = txn;
                    txnConds.put(txn, new LinkedList<Condition>());
                    return;
                }
            }
            throw TXN.tooManyActiveTransactions();
        } finally {
            lock.unlock();
        }
    }

    /**
     * Unregister transaction.
     * 
     * @param txn old terminated transaction
     */
    static void unregister(final Transaction txn) {
        lock.lock();
        try {
            List<Condition> conds = null;
            int txnIndex = -1;
            // unregister transaction and clean up its dependencies list
            for (int i = 0; i < maxTxns; i++) {
                if (activeTxns[i] == txn) {
                    activeTxns[i] = null;
                    conds = txnConds.remove(txn);
                    txnDeps[i] = 0L;
                    txnIndex = i;
                    break;
                }
            }
            // clean up transaction dependency for every dependent
            long bit = 1L << txnIndex;
            for (int i = 0; i < maxTxns; i++) {
                txnDeps[i] &= ~bit;
            }
            // wake up associated waiters
            for (final Condition cond : conds) {
                cond.signal();
            }
            conds.clear();
        } finally {
            lock.unlock();
        }
    }

    /**
     * Causes <code>dependent</code> transaction to wait for <code>dependency</code> transaction.
     * If some of the participating transactions have been terminated in the meantime wait will not happen.
     * 
     * @param dependent the dependent
     * @param dependency the dependency
     * @throws DeadlockException if transactions dependency deadlock was detected
     * @throws InterruptedException if the current thread was interrupted while waiting
     */
    static void waitFor(final Transaction dependent, final Transaction dependency) throws DeadlockException, InterruptedException {
        // detect self waits
        if (dependent == dependency) {
            return;
        }
        // check interrupt status
        if (Thread.interrupted()) {
            throw new InterruptedException();
        }
        lock.lock();
        try {
            // lookup transaction indices from active transactions
            int dependentIndex = -1, dependencyIndex = -1;
            for (int i = 0; i < maxTxns; i++) {
                if (activeTxns[i] == dependent) {
                    dependentIndex = i;
                } else if (activeTxns[i] == dependency) {
                    dependencyIndex = i;
                }
                if (dependentIndex >= 0 && dependencyIndex >= 0) {
                    break; // we have both indices
                }
            }
            // ensure indices are still valid
            if (dependentIndex == -1 || dependencyIndex == -1) {
                // Stale data - some of participating transactions have been terminated in the meantime
                return;
            }
            // register transactions dependency and detect deadlock
            try {
                addDependency(dependentIndex, dependencyIndex);
                checkDeadlock(dependentIndex, 0L);
            } catch (final DeadlockException e) {
                removeDependency(dependentIndex, dependencyIndex);
                throw e;
            }
            // transactions dependency have been registered and no deadlock was detected, let's wait
            final Condition cond = lock.newCondition();
            txnConds.get(dependent).add(cond);
            txnConds.get(dependency).add(cond);
            cond.await();
        } finally {
            lock.unlock();
        }
    }

    private static void addDependency(final int dependentIndex, final int dependencyIndex) {
        long bit = 1L << dependencyIndex;
        txnDeps[dependentIndex] |= bit;
    }

    private static void removeDependency(final int dependentIndex, final int dependencyIndex) {
        long bit = 1L << dependencyIndex;
        txnDeps[dependentIndex] &= ~bit;
    }

    private static void checkDeadlock(final int txnIndex, long visited) throws DeadlockException {
        // check deadlock
        final long bit = 1L << txnIndex;
        if (allAreSet(visited, bit)) {
            throw new DeadlockException();
        }
        visited |= bit;
        // process transaction dependencies
        long dependencies = txnDeps[txnIndex];
        long dependencyBit;
        int dependencyIndex;
        while (dependencies != 0L) {
            dependencyBit = Long.lowestOneBit(dependencies);
            dependencyIndex = Long.numberOfTrailingZeros(dependencyBit);
            checkDeadlock(dependencyIndex, visited);
            dependencies &= ~dependencyBit;
        }
    }
}
