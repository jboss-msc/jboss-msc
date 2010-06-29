/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2010, Red Hat Middleware LLC, and individual contributors
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

package org.jboss.msc.service;

import org.jboss.msc.service.util.LatchedFinishListener;
import org.junit.Test;

import java.util.Collections;
import java.util.List;

import static junit.framework.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * Test case used to verify service alias functionality.
 *
 * @author John Bailey
 */
public class ServiceAliasTestCase extends AbstractServiceTest {

    @Test
    public void testAliases() throws Exception {
        perfromTest(new ServiceTestInstance() {
            @Override
            public List<BatchBuilder> initializeBatches(ServiceContainer serviceContainer, LatchedFinishListener finishListener) throws Exception {
                final BatchBuilder builder = serviceContainer.batchBuilder();
                builder.addService(ServiceName.of("service1"), Service.NULL)
                    .addAliases(ServiceName.of("alias1"))
                    .addAliases(ServiceName.of("alias2"));
                return Collections.singletonList(builder);
            }

            @Override
            public void performAssertions(ServiceContainer serviceContainer) throws Exception {
                assertEquals(serviceContainer.getService(ServiceName.of("service1")), serviceContainer.getService(ServiceName.of("alias1")));
                assertEquals(serviceContainer.getService(ServiceName.of("service1")), serviceContainer.getService(ServiceName.of("alias2")));

                ServiceController controller = serviceContainer.getService(ServiceName.of("service1"));
                controller.setMode(ServiceController.Mode.NEVER);
                Thread.sleep(100);
                controller.remove();
                Thread.sleep(100);
                assertNull(serviceContainer.getService(ServiceName.of("service1")));
                assertNull(serviceContainer.getService(ServiceName.of("alias1")));
                assertNull(serviceContainer.getService(ServiceName.of("alias2")));
            }
        });
    }

}
