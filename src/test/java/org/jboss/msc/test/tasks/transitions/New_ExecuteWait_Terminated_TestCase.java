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

package org.jboss.msc.test.tasks.transitions;

import org.jboss.msc.test.utils.AbstractTransactionTest;
import org.jboss.msc.test.utils.CancelingExecutable;
import org.jboss.msc.txn.Executable;
import org.jboss.msc.txn.TaskController;
import org.jboss.msc.txn.Transaction;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * TODO: javadoc
 *
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
public final class New_ExecuteWait_Terminated_TestCase extends AbstractTransactionTest {

    @Test
    public void testCancellingExecutablesWithDependency() throws InterruptedException {
        final Transaction transaction = newTransaction();
        // installing canceling e1 task
        final Executable<Void> e1 = new CancelingExecutable<Void>();
        final TaskController<Void> e1Controller = transaction.newTask(e1).release();
        assertEquals(e1Controller.getTransaction(), transaction);
        // installing canceling e2 task
        final Executable<Void> e2 = new CancelingExecutable<Void>();
        final TaskController<Void> e2Controller = transaction.newTask(e2).addDependency(e1Controller).release();
        assertEquals(e2Controller.getTransaction(), transaction);
        // preparing transaction
        // prepare(transaction); // TODO: uncomment to reproduce failure
    }

}
