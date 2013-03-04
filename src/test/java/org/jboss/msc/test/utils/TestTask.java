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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Utility class that:
 * <UL>
 *   <LI>allows to associate latch with a task to await before continuing in processing</LI>
 *   <LI>allows to detect whether this task have been called</LI>
 *   <LI>provides time info when this task have been called</LI>
 * </UL>
 *
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
public abstract class TestTask {

    private final AtomicBoolean wasCalled = new AtomicBoolean();
    private final AtomicLong callTime = new AtomicLong();
    private final CountDownLatch signal;

    TestTask() {
        this(null);
    }

    TestTask(final CountDownLatch signal) {
        this.signal = signal;
    }

    void call() {
        callTime.set(System.currentTimeMillis());
        wasCalled.set(true);
        if (signal != null) {
            try {
                signal.await();
            } catch (final InterruptedException ignored) {
            }
        }
    }

    public boolean wasCalled() {
        return wasCalled.get();
    }

    public long getCallTime() {
        return callTime.get();
    }

}
