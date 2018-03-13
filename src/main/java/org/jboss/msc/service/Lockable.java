/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2017, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.jboss.msc.service;

import static java.lang.Thread.holdsLock;

/**
 * Utility locking class requiring its intrinsic lock being held while calling its methods.
 * This implementation heavily favors reader threads against writer threads.
 *
 * <p></p>
 * Example of read lock usage:
 * <pre>
 *     Lockable lock = ...
 *     synchronized (lock) {
 *         lock.acquireRead();
 *     }
 *     try {
 *         // ... do read-locked work here
 *     } finally {
 *         synchronized (lock) {
 *             lock.releaseRead();
 *         }
 *     }
 * </pre>
 * <p></p>
 * Example of write lock usage:
 * <pre>
 *     Lockable lock = ...
 *     synchronized (lock) {
 *         lock.acquireWrite();
 *         try {
 *             // ... do write-locked work here
 *         } finally {
 *             lock.releaseWrite();
 *         }
 *     }
 * </pre>
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
class Lockable {

    /**
     * Number of read locks being held.
     */
    private int readLocksCount;

    /**
     * Acquires read lock. Must be called under the intrinsic lock.
     * The read lock may be held simultaneously by multiple reader threads.
     */
    final void acquireRead() {
        assert holdsLock(this);
        readLocksCount++;
    }

    /**
     * Releases read lock. Must be called under the intrinsic lock.
     * The write lock is exclusive.
     */
    final void releaseRead() {
        assert holdsLock(this);
        if (--readLocksCount == 0) notify();
    }

    /**
     * Acquires write lock. Must be called under the intrinsic lock.
     * Write lock is available if and only if all read locks have been released.
     */
    final void acquireWrite() {
        assert holdsLock(this);
        if (readLocksCount > 0) {
            boolean intr = Thread.interrupted();
            try {
                do try {
                    wait();
                } catch (InterruptedException ignored) {
                    intr = true;
                } while (readLocksCount > 0);
            } finally {
                if (intr) Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * Releases write lock. Must be called under the intrinsic lock.
     */
    final void releaseWrite() {
        assert holdsLock(this);
        notify();
    }

    /**
     * Returns <code>true</code> iff write lock is effective <code>false</code> otherwise
     * @return <code>true</code> if write locked <code>false</code> otherwise
     */
    final boolean isWriteLocked() {
        return holdsLock(this) && readLocksCount == 0;
    }

}
