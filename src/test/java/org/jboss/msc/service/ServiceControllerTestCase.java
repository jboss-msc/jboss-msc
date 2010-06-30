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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

/**
 * Test to verify ServiceController behavior.
 *
 * @author John E. Bailey
 */
public class ServiceControllerTestCase extends AbstractServiceTest {

    @Test
    public void testStartModes() throws Exception {
        perfromTest(new ServiceTestInstance() {
            @Override
            public List<BatchBuilder> initializeBatches(ServiceContainer serviceContainer, LatchedFinishListener finishListener) throws Exception {
                final BatchBuilder batch = serviceContainer.batchBuilder();
                batch.addService(ServiceName.of("automatic"), Service.NULL).setInitialMode(ServiceController.Mode.AUTOMATIC).addListener(finishListener);
                batch.addService(ServiceName.of("never"), Service.NULL).setInitialMode(ServiceController.Mode.NEVER);
                batch.addService(ServiceName.of("immediate"), Service.NULL).setInitialMode(ServiceController.Mode.IMMEDIATE).addListener(finishListener);
                batch.addService(ServiceName.of("on_demand"), Service.NULL).setInitialMode(ServiceController.Mode.ON_DEMAND);
                return Collections.singletonList(batch);
            }

            @Override
            public void performAssertions(ServiceContainer serviceContainer) throws Exception {
                assertState(serviceContainer, ServiceName.of("automatic"), ServiceController.State.UP);
                assertState(serviceContainer, ServiceName.of("never"), ServiceController.State.DOWN);
                assertState(serviceContainer, ServiceName.of("immediate"), ServiceController.State.UP);
                assertState(serviceContainer, ServiceName.of("on_demand"), ServiceController.State.DOWN);
            }
        });
    }

    @Test
    public void testAutomatic() throws Exception {
        perfromTest(new ServiceTestInstance() {
            @Override
            public List<BatchBuilder> initializeBatches(ServiceContainer serviceContainer, LatchedFinishListener finishListener) throws Exception {
                final BatchBuilder batch = serviceContainer.batchBuilder();
                batch.addService(ServiceName.of("automatic"), Service.NULL)
                    .setInitialMode(ServiceController.Mode.AUTOMATIC)
                    .addDependencies(ServiceName.of("never"));
                batch.addService(ServiceName.of("never"), Service.NULL).setInitialMode(ServiceController.Mode.NEVER);
                return Collections.singletonList(batch);
            }

            @Override
            public void performAssertions(ServiceContainer serviceContainer) throws Exception {
                Thread.sleep(50);
                assertState(serviceContainer, ServiceName.of("automatic"), ServiceController.State.DOWN);
                assertState(serviceContainer, ServiceName.of("never"), ServiceController.State.DOWN);

                serviceContainer.getService(ServiceName.of("never")).setMode(ServiceController.Mode.IMMEDIATE);

                Thread.sleep(50);

                assertState(serviceContainer, ServiceName.of("automatic"), ServiceController.State.UP);
                assertState(serviceContainer, ServiceName.of("never"), ServiceController.State.UP);
            }
        });
    }

    @Test
    public void testOnDemand() throws Exception {
        final ServiceContainer serviceContainer = ServiceContainer.Factory.create();

        final BatchBuilder batch = serviceContainer.batchBuilder();

        batch.addService(ServiceName.of("on_demand"), Service.NULL).setInitialMode(ServiceController.Mode.ON_DEMAND);

        batch.install();
        Thread.sleep(50);

        assertState(serviceContainer, ServiceName.of("on_demand"), ServiceController.State.DOWN);

        final BatchBuilder anotherBatch = serviceContainer.batchBuilder();

        anotherBatch.addService(ServiceName.of("automatic"), Service.NULL)
            .setInitialMode(ServiceController.Mode.AUTOMATIC)
            .addDependencies(ServiceName.of("on_demand"));

        anotherBatch.install();
        Thread.sleep(50);

        assertState(serviceContainer, ServiceName.of("on_demand"), ServiceController.State.DOWN);

        final BatchBuilder yetAnotherBatch = serviceContainer.batchBuilder();

        yetAnotherBatch.addService(ServiceName.of("immediate"), Service.NULL)
            .setInitialMode(ServiceController.Mode.IMMEDIATE)
            .addDependencies(ServiceName.of("on_demand"));

        yetAnotherBatch.install();
        Thread.sleep(50);

        assertState(serviceContainer, ServiceName.of("on_demand"), ServiceController.State.UP);
        assertState(serviceContainer, ServiceName.of("automatic"), ServiceController.State.UP);
        serviceContainer.shutdown();
    }

    @Test
    public void testStop() throws Exception {
        perfromTest(new ServiceTestInstance() {
            @Override
            public List<BatchBuilder> initializeBatches(ServiceContainer serviceContainer, LatchedFinishListener finishListener) throws Exception {
                final BatchBuilder batch = serviceContainer.batchBuilder();
                batch.addService(ServiceName.of("serviceOne"), Service.NULL)
                    .addDependencies(ServiceName.of("serviceTwo"));
                batch.addService(ServiceName.of("serviceTwo"), Service.NULL);
                batch.addListener(finishListener);
                return Collections.singletonList(batch);
            }

            @Override
            public void performAssertions(ServiceContainer serviceContainer) throws Exception {
                assertState(serviceContainer, ServiceName.of("serviceOne"), ServiceController.State.UP);
                assertState(serviceContainer, ServiceName.of("serviceTwo"), ServiceController.State.UP);

                serviceContainer.getService(ServiceName.of("serviceTwo")).setMode(ServiceController.Mode.NEVER);

                Thread.sleep(50);

                assertState(serviceContainer, ServiceName.of("serviceOne"), ServiceController.State.DOWN);
                assertState(serviceContainer, ServiceName.of("serviceTwo"), ServiceController.State.DOWN);
            }
        });
    }

    @Test
    public void testRemove() throws Exception {
        perfromTest(new ServiceTestInstance() {
            @Override
            public List<BatchBuilder> initializeBatches(ServiceContainer serviceContainer, LatchedFinishListener finishListener) throws Exception {
                final BatchBuilder batch = serviceContainer.batchBuilder();
                batch.addService(ServiceName.of("serviceOne"), Service.NULL)
                    .addDependencies(ServiceName.of("serviceTwo"));
                batch.addService(ServiceName.of("serviceTwo"), Service.NULL);
                batch.addListener(finishListener);
                return Collections.singletonList(batch);
            }

            @Override
            public void performAssertions(ServiceContainer serviceContainer) throws Exception {
                assertState(serviceContainer, ServiceName.of("serviceOne"), ServiceController.State.UP);
                assertState(serviceContainer, ServiceName.of("serviceTwo"), ServiceController.State.UP);

                try {
                    serviceContainer.getService(ServiceName.of("serviceTwo")).remove();
                    fail("Should throw an IllegalStateException since the controller is not stopped");
                } catch (IllegalStateException expected) {
                }

                serviceContainer.getService(ServiceName.of("serviceTwo")).setMode(ServiceController.Mode.NEVER);
                Thread.sleep(50);
                serviceContainer.getService(ServiceName.of("serviceTwo")).remove();
                Thread.sleep(50);
                assertNull(serviceContainer.getService(ServiceName.of("serviceTwo")));
                assertState(serviceContainer, ServiceName.of("serviceOne"), ServiceController.State.DOWN);
            }
        });
    }

    @Test
    public void testFailedStart() throws Exception {
        final StartException startException = new StartException("Blahhhh");
        perfromTest(new ServiceTestInstance() {
            @Override
            public List<BatchBuilder> initializeBatches(ServiceContainer serviceContainer, LatchedFinishListener finishListener) throws Exception {
                final BatchBuilder batch = serviceContainer.batchBuilder();
                batch.addService(ServiceName.of("serviceOne"), new Service<Void>() {
                    @Override
                    public void start(StartContext context) throws StartException {
                        throw startException;
                    }

                    @Override
                    public void stop(StopContext context) {
                    }

                    @Override
                    public Void getValue() throws IllegalStateException {
                        return null;
                    }
                });
                return Collections.singletonList(batch);
            }

            @Override
            public void performAssertions(ServiceContainer serviceContainer) throws Exception {
                Thread.sleep(50);

                assertState(serviceContainer, ServiceName.of("serviceOne"), ServiceController.State.START_FAILED);
                assertEquals(startException, serviceContainer.getService(ServiceName.of("serviceOne")).getStartException());
            }
        });
    }

    private void assertState(final ServiceContainer serviceContainer, final ServiceName serviceName, final ServiceController.State state) {
        assertEquals(state, serviceContainer.getService(serviceName).getState());
    }
}
