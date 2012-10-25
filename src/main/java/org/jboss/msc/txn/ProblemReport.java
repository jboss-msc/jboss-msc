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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * A problem report.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 */
public final class ProblemReport implements Iterable<Problem> {
    private Problem.Severity maxSeverity = null;
    private final List<Problem> problems = new ArrayList<Problem>();

    public ProblemReport() {
    }

    public void addProblem(Problem problem) {
        synchronized (problems) {
            final Problem.Severity severity = problem.getSeverity();
            if (maxSeverity == null || severity.compareTo(maxSeverity) < 0) {
                maxSeverity = severity;
            }
            problems.add(problem);
        }
    }

    public List<Problem> getProblems() {
        synchronized (problems) {
            return Collections.<Problem>unmodifiableList(Arrays.asList(problems.toArray(new Problem[problems.size()])));
        }
    }

    public Problem.Severity getMaxSeverity() {
        synchronized (problems) {
            return maxSeverity;
        }
    }

    public Iterator<Problem> iterator() {
        return getProblems().iterator();
    }
}
