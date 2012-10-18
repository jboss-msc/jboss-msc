/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012 Red Hat, Inc., and individual contributors
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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.transaction.RollbackException;
import javax.transaction.SystemException;
import javax.transaction.TransactionManager;
import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;

/**
 * An XAResource which corresponds to the transactional task container.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class TransactionXAResource implements XAResource, Serializable {
    private static final long serialVersionUID = -5916852431271162444L;

    private static final ConcurrentMap<XidKey, Transaction> incompleteTransactions = new ConcurrentHashMap<XidKey, Transaction>();
    private static final ThreadLocal<Transaction> currentTransaction = new ThreadLocal<Transaction>();
    private static final TransactionXAResource INSTANCE = new TransactionXAResource();

    private static final AttachmentKey<XidKey> XID_KEY = AttachmentKey.create();
    private static final Xid[] NO_XIDS = new Xid[0];
    private static final AttachmentKey<TransactionManager> TM_KEY = AttachmentKey.create();

    public static TransactionXAResource getInstance() {
        return INSTANCE;
    }

    /**
     * Get the current managed transaction.  May be {@code null} if no transaction manager has enlisted the resource.
     *
     * @return the current transaction
     */
    public static Transaction getCurrentTransaction() {
        return currentTransaction.get();
    }

    public static TransactionManager getTransactionManagerFor(Transaction transaction) {
        return transaction.getAttachmentIfPresent(TM_KEY);
    }

    public void enlist(Transaction transaction, TransactionManager transactionManager) throws SystemException, RollbackException {
        if (! transaction.ensureAttachmentValue(TM_KEY, transactionManager)) {
            throw new IllegalStateException("Transaction is already associated with another transaction manager");
        }
        transactionManager.getTransaction().enlistResource(this);
    }

    private static XidKey getKeyFor(Transaction transaction) {
        return transaction.getAttachment(XID_KEY);
    }

    public boolean isSameRM(final XAResource resource) throws XAException {
        return resource == this;
    }

    public Xid[] recover(final int flags) throws XAException {
        switch (flags) {
            case TMNOFLAGS:
            case TMENDRSCAN: return NO_XIDS;
            case TMSTARTRSCAN: break;
            default: throw new XAException(XAException.XAER_INVAL);
        }
        List<Xid> list = new ArrayList<Xid>(incompleteTransactions.size());
        for (Map.Entry<XidKey, Transaction> entry : incompleteTransactions.entrySet()) {
            list.add(entry.getKey().getXid());
        }
        return list.toArray(new Xid[list.size()]);
    }

    public void start(final Xid xid, final int flags) throws XAException {
        if (xid == null || currentTransaction.get() != null) {
            throw new XAException(XAException.XAER_INVAL);
        }
        XidKey key = new XidKey(xid);
        Transaction transaction = incompleteTransactions.get(key);
        if (transaction != null && flags != TMJOIN && flags != TMRESUME) {
            throw new XAException(XAException.XAER_DUPID);
        }
        XidKey appearing;
        if ((appearing = transaction.putAttachmentIfAbsent(XID_KEY, key)) != null) {
            if (! appearing.equals(key)) {
                // transaction is already associated with a different Xid...
                throw new XAException(XAException.XAER_RMERR);
            }
        }
        currentTransaction.set(transaction);
    }

    public void end(final Xid xid, final int flags) throws XAException {

    }

    public void forget(final Xid xid) throws XAException {
    }

    public int getTransactionTimeout() throws XAException {
        return Integer.MAX_VALUE;
    }

    public int prepare(final Xid xid) throws XAException {
        return 0;
    }

    public void commit(final Xid xid, final boolean onePhase) throws XAException {

    }

    public void rollback(final Xid xid) throws XAException {
    }

    public boolean setTransactionTimeout(final int timeout) throws XAException {
        return false;
    }

    protected Object readResolve() {
        return INSTANCE;
    }

    private static final class XidKey {
        private final Xid xid;
        private final int hashCode;

        private XidKey(final Xid xid) {
            hashCode = (Arrays.hashCode(xid.getGlobalTransactionId()) * 17 + Arrays.hashCode(xid.getBranchQualifier())) * 17 + xid.getFormatId();
            this.xid = xid;
        }

        public Xid getXid() {
            return xid;
        }

        public boolean equals(Object other) {
            return other instanceof XidKey && equals((XidKey) other);
        }

        public boolean equals(XidKey other) {
            return other == this || other != null
                    && hashCode == other.hashCode
                    && xid == other.xid
                    || (xid.getFormatId() == other.xid.getFormatId()
                        && Arrays.equals(xid.getGlobalTransactionId(), other.xid.getGlobalTransactionId())
                        && Arrays.equals(xid.getBranchQualifier(), other.xid.getBranchQualifier()));
        }

        public int hashCode() {
            return hashCode;
        }
    }
}
