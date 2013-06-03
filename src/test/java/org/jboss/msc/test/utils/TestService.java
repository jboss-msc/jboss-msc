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
package org.jboss.msc.test.utils;

import java.util.concurrent.atomic.AtomicBoolean;

import org.jboss.msc.service.Service;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StopContext;

/**
 * Basic service for tests.
 * 
 * @author <a href="mailto:frainone@redhat.com">Flavia Rainone</a>
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
public final class TestService implements Service<Void> {

    private final boolean failToStart; 
    private AtomicBoolean up = new AtomicBoolean();
    private AtomicBoolean failed = new AtomicBoolean();

    public TestService(final boolean failToStart) {
        this.failToStart = failToStart;
    }

    @Override
    public void start(final StartContext<Void> context) {
        if (failToStart) {
            failed.set(true);
            context.addProblem(new UnsupportedOperationException());
            context.fail();
        } else {
            up.set(true);
            context.complete();
        }
    }

    @Override
    public void stop(final StopContext context) {
        up.set(false);
        context.complete();
    }

    public boolean isFailed() {
        return failed.get();
    }

    public boolean isUp() {
        return up.get();
    }
}
