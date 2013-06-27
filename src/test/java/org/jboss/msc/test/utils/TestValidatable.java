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

import org.jboss.msc.txn.Problem;
import org.jboss.msc.txn.Validatable;
import org.jboss.msc.txn.ValidateContext;

/**
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 * @author <a href="mailto:frainone@redhat.com">Flavia Rainone</a>
 */
public final class TestValidatable extends TestTask implements Validatable {

    private final Problem.Severity[] validationProblems;

    public TestValidatable(Problem.Severity... validationProblems) {
        super(null);
        this.validationProblems = validationProblems;
    }

    public TestValidatable(String name, Problem.Severity... validationProblems) {
        super(name);
        this.validationProblems = validationProblems;
    }

    public TestValidatable(final CountDownLatch signal, Problem.Severity... validationProblems) {
        super(null, signal);
        this.validationProblems = validationProblems;
    }

    @Override
    public void validate(final ValidateContext ctx) {
        super.call();
        for (Problem.Severity severity: validationProblems) {
            ctx.addProblem(severity, "test validation problem");
        }
        ctx.complete();
    }

}

