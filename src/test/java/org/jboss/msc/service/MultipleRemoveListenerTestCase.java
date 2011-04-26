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

package org.jboss.msc.service;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNull;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.jboss.msc.service.MultipleRemoveListener.Callback;
import org.jboss.msc.service.ServiceController.Mode;
import org.jboss.msc.util.TestServiceListener;
import org.jboss.msc.value.Value;
import org.junit.Test;

/**
 * Test for {@link MultipleRemoveListener}.
 * 
 * @author <a href="mailto:flavia.rainone@jboss.com">Flavia Rainone</a>
 */
public class MultipleRemoveListenerTestCase extends AbstractServiceTest {

    private static final ServiceName firstServiceName = ServiceName.of("1");
    private static final ServiceName secondServiceName = ServiceName.of("2");
    private static final ServiceName thirdServiceName = ServiceName.of("3");

    @SuppressWarnings("unchecked")
    @Test
    public void testDoneAfterRemoval() throws Exception {
        final IntegerValue integerValue = new IntegerValue(); 
        final SetValueCallback callback = new SetValueCallback(integerValue);
        final MultipleRemoveListener<Integer> removeListener = MultipleRemoveListener.create(callback, Integer.valueOf(1050));
        final TestServiceListener testListener = new TestServiceListener();

        final Future<ServiceController<?>> firstServiceStart = testListener.expectServiceStart(firstServiceName);
        serviceContainer.addService(firstServiceName, Service.NULL).addListener(removeListener, testListener).install();
        assertController(firstServiceName, firstServiceStart);

        final Future<ServiceController<?>> secondServiceStart = testListener.expectServiceStart(secondServiceName);
        serviceContainer.addService(secondServiceName, Service.NULL).addListener(removeListener, testListener).install();
        assertController(secondServiceName, secondServiceStart);

        final Future<ServiceController<?>> thirdServiceStart = testListener.expectServiceStart(thirdServiceName);
        serviceContainer.addService(thirdServiceName, Service.NULL).addListener(removeListener, testListener).install();
        assertController(thirdServiceName, thirdServiceStart);

        assertNull(integerValue.getValue());
        shutdownContainer();
        assertNull(integerValue.getValue());
        // call done after removal
        removeListener.done();
        assertEquals(Integer.valueOf(1050), callback.get());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testDoneBeforeRemoval() throws Exception {
        final IntegerValue integerValue = new IntegerValue(); 
        final SetValueCallback callback = new SetValueCallback(integerValue);
        final MultipleRemoveListener<Integer> removeListener = MultipleRemoveListener.create(callback, Integer.valueOf(2457));
        final TestServiceListener testListener = new TestServiceListener();

        final Future<ServiceController<?>> firstServiceStart = testListener.expectServiceStart(firstServiceName);
        serviceContainer.addService(firstServiceName, Service.NULL).addListener(removeListener, testListener).install();
        final ServiceController<?> firstController = assertController(firstServiceName, firstServiceStart);

        final Future<ServiceController<?>> secondServiceStart = testListener.expectServiceStart(secondServiceName);
        serviceContainer.addService(secondServiceName, Service.NULL).addListener(removeListener, testListener).install();
        final ServiceController<?> secondController = assertController(secondServiceName, secondServiceStart);

        final Future<ServiceController<?>> thirdServiceStart = testListener.expectServiceStart(thirdServiceName);
        serviceContainer.addService(thirdServiceName, Service.NULL).addListener(removeListener, testListener).install();
        final ServiceController<?> thirdController = assertController(thirdServiceName, thirdServiceStart);

        // call done before removal
        removeListener.done();
        assertNull(integerValue.getValue());

        final Future<ServiceController<?>> firstServiceRemoval = testListener.expectServiceRemoval(firstServiceName);
        firstController.setMode(Mode.REMOVE);
        assertController(firstController, firstServiceRemoval);
        assertNull(integerValue.getValue());

        final Future<ServiceController<?>> secondServiceRemoval = testListener.expectServiceRemoval(secondServiceName);
        secondController.setMode(Mode.REMOVE);
        assertController(secondController, secondServiceRemoval);
        assertNull(integerValue.getValue());

        final Future<ServiceController<?>> thirdServiceRemoval = testListener.expectServiceRemoval(thirdServiceName);
        thirdController.setMode(Mode.REMOVE);
        assertController(thirdController, thirdServiceRemoval);
        assertEquals(Integer.valueOf(2457), callback.get());
    }

    private class IntegerValue implements Value<Integer> {
        private Integer value;

        public void setValue(Integer value) {
            this.value = value;
        }

        @Override
        public Integer getValue() throws IllegalStateException, IllegalArgumentException {
            return value;
        }
    }

    private class SetValueCallback implements Callback<Integer>, Future<Integer> {

        private final IntegerValue value;
        private final CountDownLatch countDownLatch;

        public SetValueCallback(IntegerValue value) {
            this.value = value;
            countDownLatch = new CountDownLatch(1);
        }

        @Override
        public void handleDone(Integer parameter) {
            synchronized (this) {
                value.setValue(parameter);
            }
            countDownLatch.countDown();
        }

        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            return false;
        }

        @Override
        public boolean isCancelled() {
            return false;
        }

        @Override
        public synchronized boolean isDone() {
            return value.getValue() != null;
        }

        @Override
        public Integer get() throws InterruptedException, ExecutionException {
            try {
                return get(50, TimeUnit.SECONDS);
            } catch (TimeoutException e) {
                throw new RuntimeException("Could not get value in 500 miliseconds timeout");
            }
        }

        @Override
        public Integer get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
            countDownLatch.await(timeout, unit);
            synchronized( this) {
                return value.getValue();
            }
        }
    }
}
