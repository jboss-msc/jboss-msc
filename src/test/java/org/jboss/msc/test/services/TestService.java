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
package org.jboss.msc.test.services;

import org.jboss.msc.service.Service;
import org.jboss.msc.txn.ExecuteContext;

/**
 * Basic service for tests.
 * 
 * @author <a href="mailto:frainone@redhat.com">Flavia Rainone</a>
 *
 */
public class TestService implements Service<Void> {

    private boolean failToStart; 
    private boolean up = false;

    public TestService(boolean failToStart) {
        this.failToStart = failToStart;
    }

    @Override
    public synchronized void start(ExecuteContext<Void> context) {
        up = true;
        context.complete();
    }

    @Override
    public void stop(ExecuteContext<Void> context) {
        up = false;
        context.complete();
    }

    public synchronized boolean isFailed() {
        return false; // TODO
    }

    public synchronized boolean isUp() {
        return up;
    }

}
