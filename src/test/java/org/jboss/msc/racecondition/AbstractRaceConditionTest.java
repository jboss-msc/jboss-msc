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

import static junit.framework.Assert.assertTrue;

import org.jboss.byteman.contrib.bmunit.BMRule;
import org.jboss.byteman.contrib.bmunit.BMUnitRunner;
import org.jboss.msc.service.AbstractServiceTest;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Super class of race condition tests, asserts the test is being instrumented by Byteman.
 * 
 * @author <a href="mailto:flavia.rainone@jboss.com">Flavia Rainone</a>
 */
@RunWith(BMUnitRunner.class)
public class AbstractRaceConditionTest extends AbstractServiceTest {

    private static final String RULE = "$0.instrumented = true";
    private boolean instrumented = false;

    @Test
    @BMRule(name="set instrumented to true",
            targetClass= "AbstractRaceConditionTest",
            targetMethod="isInstrumented",
            condition="TRUE",
            action= RULE)
    public void isInstrumented() {
        assertTrue(instrumented);
    }
}
