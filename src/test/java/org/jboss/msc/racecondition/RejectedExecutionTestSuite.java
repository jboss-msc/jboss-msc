/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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

package org.jboss.msc.racecondition;

import static org.junit.Assert.assertTrue;

import org.jboss.msc.service.OptionalDependencyListenersTestCase;
import org.jboss.msc.service.ServiceBuilderTestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

/**
 * Runs the tests under a rejected execution scenario, meaning the executions triggered by {@link ServiceControllerImpl}
 * will have its executions constantly denied.
 * 
 * @author <a href="mailto:flavia.rainone@jboss.com">Flavia Rainone</a>
 */
@RunWith(Suite.class)
@SuiteClasses({RejectedExecutionTestSuite.OptionalDependencyListenersTest.class, 
    RejectedExecutionTestSuite.ServiceBuilderTest.class})
public class RejectedExecutionTestSuite {

    public static class ServiceBuilderTest extends ServiceBuilderTestCase {
        // this is a safeguard field, that allows to check if the execution was rejected or not
        private boolean executionRejected;

        @Before
        public void resetExecutionRejected() {
            // before each test, executionRejected must be reset
            executionRejected = false;
        }

        @After
        public void checkExecutionRejected() {
            // after every test, texecutionRejected should have changed to true,
            // indicating that the Byteman rule was enabled during test execution
            assertTrue("Execution was not rejected. Check if the Byteman rules are being applied.", executionRejected);
        }
    }

    public static class OptionalDependencyListenersTest extends OptionalDependencyListenersTestCase {
        // this is a safeguard field, that allows to check if the execution was rejected or not
        private boolean executionRejected;

        @Before
        public void resetExecutionRejected() {
         // before each test, executionRejected must be reset
            executionRejected = false;
        }

        @After
        public void checkExecutionRejected() {
            // after every test, executionRejected should have changed to true,
            // indicating that the Byteman rule was enabled during test execution
            assertTrue("Execution was not rejected. Check if the Byteman rules are being applied.", executionRejected);
        }
    }
}
