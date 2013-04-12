/*
 * JBoss, Home of Professional Open Source
 *
 * Copyright 2013 Red Hat, Inc. and/or its affiliates.
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
package org.jboss.msc.service;

import org.jboss.msc.txn.CommitContext;
import org.jboss.msc.txn.Committable;
import org.jboss.msc.txn.DeadlockException;
import org.jboss.msc.txn.Problem;
import org.jboss.msc.txn.Revertible;
import org.jboss.msc.txn.RollbackContext;
import org.jboss.msc.txn.Transaction;

/**
 * Object with write lock support per transaction.
 * <p>
 * With {@link #lockWrite}, the object is locked under a transaction that is attempting to change the object's state.
 * Once locked, no other transaction can edit the object's state. When the transaction completes, the object is
 * automatically unlocked. If the transaction holding the lock is rolled back, {@link #revert(Object)} is invoked, and
 * the object is reverted to its original state before locked.
 * 
 * @author <a href="mailto:frainone@redhat.com">Flavia Rainone</a>
 *
 */
abstract class TransactionalObject {

    // inner lock
    private Transaction lock;

    /**
     * Write locks this object under {@code transaction}. If another transaction holds the lock, this method will block
     * until the object is unlocked.
     * 
     * <p> This operation is idempotent. Unlocking occurs automatically when the transaction is finished.
     *  
     * @param transaction the transaction that is attempting to modify current's object state
     */
    final void lockWrite(Transaction transaction) {
        assert !Thread.holdsLock(this);
        while (true) {
            Transaction currentLock;
            synchronized (this) {
                currentLock = lock;
            }
            if (currentLock != null) {
                try {
                    transaction.waitFor(lock);
                } catch (DeadlockException e) {
                    // TODO do I need a task controller? what is the correct approach?
                    final Problem problem = new Problem(null, e);
                    transaction.getProblemReport().addProblem(problem);
                } catch (InterruptedException e) {
                    // TODO just log in this case?
                }
            } else {
                synchronized (this) {
                    if (lock == null) {
                        lock = transaction;
                        break;
                    }
                }
            }
        } 
        final Object snapshot = takeSnapshot();
        transaction.newTask().setTraits(new UnlockWriteTask(snapshot)).release();
        writeLocked(transaction);
    }

    /**
     * Returns the transaction that holds the lock.
     * 
     * @return the transaction that holds the lock, or {@code null} if this object is {@link #isWriteLocked() unlocked}.
     */
    synchronized final Transaction getCurrentTransaction() {
        return lock;
    }

    /**
     * Indicates if this object is locked.
     * 
     * @return {@code true} only if this object is locked under an active transaction
     */
    synchronized final boolean isWriteLocked() {
        return lock != null;
    }

    /**
     * Indicates if this object is locked by {@code transaction}.
     * 
     * @param transaction an active transaction
     * @return {@code true} only if this object is locked by {@code transaction}.
     */
    synchronized final boolean isWriteLocked(Transaction transaction) {
        return lock == transaction;
    }

    private final void unlockWrite() {
        assert Thread.holdsLock(this);
        lock = null;
        writeUnlocked();
    }

    /**
     * Take a snapshot of this transactional object's inner state. Invoked when this object is write locked.
     * 
     * @return the snapshot
     */
    abstract Object takeSnapshot();

    /**
     * Revert this object's inner state to what its original state when it was locked. Invoked during transaction
     * rollback.
     * 
     * @param snapshot the snapshot
     */
    abstract void revert(Object snapshot);

    /**
     * Notifies that this object is now write locked. Invoked only once per transaction lock.
     * 
     * @param transaction the transaction under which this object is locked
     */
    void writeLocked(Transaction transaction) {}

    /**
     * Notifies that this object is now write unlocked.
     */
    void writeUnlocked() {}

    class UnlockWriteTask implements Committable, Revertible {

        private Object snapshot;

        public UnlockWriteTask(Object snapshot) {
            this.snapshot = snapshot;
        }

        @Override
        public void rollback(RollbackContext context) {
            synchronized (TransactionalObject.this) {
                unlockWrite();
                revert(snapshot);
            }
            context.complete();
        }

        @Override
        public void commit(CommitContext context) {
            synchronized (TransactionalObject.this) {
                unlockWrite();
            }
            context.complete();
        }
    }

}
