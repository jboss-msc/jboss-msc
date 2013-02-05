/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat, Inc., and individual contributors
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

import static org.jboss.msc.service.ServiceBuilder.DependencyType.OPTIONAL;
import static org.jboss.msc.service.ServiceBuilder.DependencyType.REQUIRED;
import static org.jboss.msc.service.ServiceController.Mode.ACTIVE;
import static org.jboss.msc.service.ServiceController.Mode.ON_DEMAND;
import static org.jboss.msc.service.ServiceController.Mode.REMOVE;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertEquals;

import org.jboss.byteman.contrib.bmunit.BMScript;
import org.jboss.byteman.contrib.bmunit.BMUnitRunner;
import org.jboss.msc.service.AbstractServiceTest;
import org.jboss.msc.service.Service;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * [MSC-126] It was possible for services with optional dependencies to throw NPE on MSC shutdown
 * and causing MSC to freeze (because service that threw NPE was never removed).
 * 
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
@RunWith(BMUnitRunner.class)
@BMScript(dir="src/test/resources")
public class ChildServicesWithOptionalDependenciesTestCase extends AbstractServiceTest {

    private static final ServiceName child1SN = ServiceName.of("child1");
    private static final ServiceName child2SN = ServiceName.of("child2");
    private static final ServiceName child3SN = ServiceName.of("child3");
    private static final ServiceName child4SN = ServiceName.of("child4");
    private static final ServiceName parentSN = ServiceName.of("parent");

    @Test
    public void serviceWithOptionalDependencyOnMscShutdown() throws Exception {
        final ServiceController<?> parentController = serviceContainer.addService(parentSN, new ParentService()).install();
        Thread.sleep(2000);
        final ServiceController<?> child1Controller = serviceContainer.getRequiredService(child1SN); 
        final ServiceController<?> child2Controller = serviceContainer.getRequiredService(child2SN);
        final ServiceController<?> child3Controller = serviceContainer.getRequiredService(child3SN);
        final ServiceController<?> child4Controller = serviceContainer.getRequiredService(child4SN);
        assertNotNull(parentController);
        assertNotNull(child1Controller);
        assertNotNull(child2Controller);
        assertNotNull(child3Controller);
        assertNotNull(child4Controller);
        assertEquals(ServiceController.State.UP,  parentController.getState());
        assertEquals(ServiceController.State.UP,  child1Controller.getState());
        assertEquals(ServiceController.State.UP,  child2Controller.getState());
        assertEquals(ServiceController.State.UP,  child3Controller.getState());
        assertEquals(ServiceController.State.UP,  child4Controller.getState());
        child3Controller.setMode(REMOVE);
        child4Controller.setMode(REMOVE);
        Thread.sleep(2000);
    }

    private final class ParentService implements Service<Void> {
        @Override
        public void start(final StartContext context) throws StartException {
            final ServiceTarget childTarget = context.getChildTarget();
            // installing child4
            childTarget.addService(child4SN, NULL).install();
            // installing child3
            childTarget.addService(child3SN, NULL).install();
            // installing child2 with optional dependency on child3
            childTarget.addService(child2SN, NULL).setInitialMode(ON_DEMAND).addDependency(OPTIONAL, child3SN).addDependency(REQUIRED, child4SN).install();
            // installing child1 with mandatory dependency on child2
            childTarget.addService(child1SN, NULL).setInitialMode(ACTIVE).addDependency(REQUIRED, child2SN).install();
        }

        @Override
        public void stop(final StopContext context) {
            // does nothing
        }

        @Override
        public Void getValue()  {
            return null;
        }
    }
}
