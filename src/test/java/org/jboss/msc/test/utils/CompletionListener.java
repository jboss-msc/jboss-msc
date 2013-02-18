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

package org.jboss.msc.test.utils;

import java.util.concurrent.CountDownLatch;

import org.jboss.msc.txn.Transaction;
import org.jboss.msc.value.Listener;

/**
 * Transaction event listener. It provides utility method {@link #awaitCompletion()} to wait for transaction phase to be
 * completed.
 * 
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
public final class CompletionListener implements Listener<Transaction> {

    private final CountDownLatch latch = new CountDownLatch(1);

    @Override
    public void handleEvent(final Transaction subject) {
        latch.countDown();
    }

    public void awaitCompletion() throws InterruptedException {
        latch.await();
    }

}
