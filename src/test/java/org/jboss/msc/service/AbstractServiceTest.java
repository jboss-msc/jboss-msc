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

import java.util.List;

/**
 * Test base used for service test cases.
 *
 * @author John E. Bailey
 */
public class AbstractServiceTest {

    public static abstract class ServiceTestInstance {
        public abstract List<BatchBuilder> initializeBatches(ServiceContainer serviceContainer, LatchedFinishListener finishListener) throws Exception;

        public void performAssertions(ServiceContainer serviceContainer) throws Exception {
        }

        public void handle(BatchBuilder batch, Throwable t) {
        }
    }

    protected void perfromTest(ServiceTestInstance instance) throws Exception {
        final ServiceContainer serviceContainer = ServiceContainer.Factory.create();
        final LatchedFinishListener finishListener = new LatchedFinishListener();

        final List<BatchBuilder> batches = instance.initializeBatches(serviceContainer, finishListener);
        for (BatchBuilder batch : batches) {
            try {
                batch.install();
            } catch (Throwable t) {
                instance.handle(batch, t);
            }
        }
        finishListener.await();
        instance.performAssertions(serviceContainer);
        serviceContainer.shutdown();
    }
}
