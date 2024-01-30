/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2021, Red Hat, Inc., and individual contributors
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

package org.jboss.msc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.jboss.msc.service.LifecycleListener;
import org.jboss.msc.service.ServiceBuilder;
import org.jboss.msc.service.ServiceController;
import org.jboss.msc.service.ServiceController.Mode;
import org.jboss.msc.service.ServiceController.State;
import org.jboss.msc.service.ServiceName;
import org.jboss.msc.service.ServiceTarget;
import org.jboss.msc.service.StartContext;
import org.jboss.msc.service.StartException;
import org.jboss.msc.service.StopContext;
import org.junit.Before;
import org.junit.Test;

/**
 * Test for {@link StartContext#getChildTarget() ChildTarget}.
 * 
 * @author <a href="mailto:flavia.rainone@jboss.com">Flavia Rainone</a>
 */
public class ChildServiceTargetTestCase extends AbstractServiceTargetTest {

    private static final ServiceName parentServiceName = ServiceName.of("parent");
    private static final ServiceName firstServiceName = ServiceName.of("first", "service");
    private static final ServiceName secondServiceName = ServiceName.of("second", "service");
    private static final ServiceName thirdServiceName = ServiceName.of("third", "service");
    private static final ServiceName fourthServiceName = ServiceName.of("fourth", "service");
    private static final ServiceName fifthServiceName = ServiceName.of("fifth", "service");

    private ParentService parentService;
    private TestLifecycleListener parentListener;

    @Before
    @Override
    public void initializeServiceTarget() throws Exception {
        ServiceBuilder<?> sb = serviceContainer.addService();
        Consumer<String> providedValue = sb.provides(parentServiceName);
        sb.setInstance(parentService = new ParentService(providedValue));
        sb.addListener(parentListener = new TestLifecycleListener());
        ServiceController<?> parentController = sb.install();
        serviceContainer.awaitStability();
        assertSame(serviceContainer.getRequiredService(parentServiceName), parentController);
        serviceTarget = getServiceTarget(serviceContainer);
    }

    @Override
    protected ServiceTarget getServiceTarget(ServiceTarget serviceTarget) {
        return parentService.getChildTarget();
    }

    @Test
    public void invalidChildTarget() throws Exception {
        // retrieve parent service's child target
        final ServiceTarget serviceTarget = parentService.getChildTarget();

        // move parent to never mode, causing it to stop
        final ServiceController<?> parentController = serviceContainer.getService(parentServiceName);
        parentController.setMode(Mode.NEVER);
        serviceContainer.awaitStability();
        assertTrue(parentListener.downValues().contains(parentServiceName));

        // edit child target at free will, nothing happens...
        serviceTarget.addDependency(secondServiceName);
        serviceTarget.addListener(new TestLifecycleListener());

        ServiceBuilder<?> sb = serviceTarget.addService();
        Consumer<String> providedValue = sb.provides(fourthServiceName);
        sb.setInstance(Service.newInstance(providedValue, fourthServiceName.toString()));

        // until we try to install service into it, then it fails wit an IllegalStateException
        try {
            sb.install();
            fail("IllegalStateException expected");
        } catch (IllegalStateException e) {}
    }

    @Test
    public void changeListenersInChildTarget() throws Exception {
        final TestLifecycleListener listener1 = new TestLifecycleListener();
        final TestLifecycleListener listener2 = new TestLifecycleListener();
        final TestLifecycleListener listener3 = new TestLifecycleListener();
        
        // install parent as first service, with listener1...

        ServiceBuilder<?> sb = serviceTarget.addService();
        Consumer<String> providedValue = sb.provides(firstServiceName);
        // create parent service with second service as child, and add listener2 to its child target
        final ParentService parent = new ParentService(providedValue);
        parent.addChild(secondServiceName).addListener(listener2);
        sb.setInstance(parent);
        sb.addListener(listener1);
        ServiceController<?> parentController = sb.install();

        serviceContainer.awaitStability();

        assertSame(serviceContainer.getRequiredService(firstServiceName), parentController);
        final ServiceController<?> secondController = serviceContainer.getRequiredService(secondServiceName);
        assertNotNull(secondController);
        // parent start notification expected from listener1; child start notification expected from listener2
        assertEquals(listener1.upValues().size(), 1);
        assertTrue(listener1.upValues().contains(firstServiceName));
        assertEquals(listener2.upValues().size(), 1);
        assertTrue(listener2.upValues().contains(secondServiceName));

        // edit parent's child target, removing listener2 from it, and adding listener3
        final ServiceTarget childTarget = parent.getChildTarget();
        childTarget.removeListener(listener2);
        childTarget.addListener(listener3);

        // add third service as a child... service start notification expected from listener3 this time
        sb = childTarget.addService();
        providedValue = sb.provides(thirdServiceName);
        sb.setInstance(Service.newInstance(providedValue, thirdServiceName.toString()));
        ServiceController<?> thirdServiceController = sb.install();

        serviceContainer.awaitStability();

        assertSame(serviceContainer.getRequiredService(thirdServiceName), thirdServiceController);

        // stop second service... service stop notification expected from listener 2
        secondController.setMode(Mode.NEVER);

        serviceContainer.awaitStability();

        assertTrue(listener2.downValues().contains(secondServiceName));
    }

    @Test
    public void childFails() throws Exception {
        // child of parent will fail to start
        final TestLifecycleListener listener1 = new TestLifecycleListener();
        ServiceBuilder<?> sb = parentService.getChildTarget().addService();
        Consumer<String> providedValue = sb.provides(firstServiceName);
        sb.setInstance(new FailToStartService(providedValue, true));
        sb.addListener(listener1);
        ServiceController<?> childController = sb.install();

        serviceContainer.awaitStability();

        assertSame(serviceContainer.getRequiredService(firstServiceName), childController);
        assertTrue(listener1.failedValues().contains(firstServiceName));
        // this won't affect parent
        assertSame(State.UP, serviceContainer.getService(parentServiceName).getState());
    }

    @Test
    public void parentDependsOnChild() throws Exception {
        TestLifecycleListener testListener = new TestLifecycleListener();
        ServiceBuilder<?> sb = serviceContainer.addService();
        Consumer<String> providedValue = sb.provides(firstServiceName);

        // create parent service with second service as a child; also add testListener to its childTarget
        final ParentService parentService = new ParentService(providedValue);
        parentService.addListener(testListener);
        parentService.addChild(secondServiceName);

        // install parent as first service, with a dependency on its child secondService
        sb.setInstance(parentService);
        sb.requires(secondServiceName);
        sb.addListener(testListener);
        sb.install();

        // install secondService as a normal, non-child, service...
        // this will cause firstService to attempt to start, but it will fail, as it will try to
        // add the already installed second service as a child on its start method
        sb = serviceContainer.addService();
        providedValue = sb.provides(secondServiceName);
        sb.setInstance(Service.newInstance(providedValue, secondServiceName.toString()));
        sb.addListener(testListener);
        sb.install();

        serviceContainer.awaitStability();

        assertTrue(testListener.upValues().contains(secondServiceName));
        assertTrue(testListener.failedValues().contains(firstServiceName));
    }

    @Test
    public void parentDependsOnChildViaNickName() throws Exception {
        TestLifecycleListener testListener = new TestLifecycleListener();
        ServiceBuilder<?> sb = serviceContainer.addService();
        Consumer<String> providedValue = sb.provides(firstServiceName);

        // create parent service with third service as a child; also add testListener to its childTarget
        final ParentService parentService = new ParentService(providedValue);
        parentService.addListener(testListener);
        parentService.addChild(thirdServiceName);

        // install parent as first service, with a dependency on secondService
        sb.setInstance(parentService);
        sb.requires(secondServiceName);
        sb.addListener(testListener);
        sb.install();

        // install thirdService as a normal, non-child, service, with secondService alias
        // this will cause firstService to attempt to start, but it will fail, as it will try to
        // add the already installed third service as a child on its start method
        sb = serviceContainer.addService();
        providedValue = sb.provides(secondServiceName, thirdServiceName);
        sb.setInstance(Service.newInstance(providedValue, secondServiceName.toString()));
        sb.addListener(testListener);
        sb.install();

        serviceContainer.awaitStability();

        assertTrue(testListener.upValues().contains(thirdServiceName));
        assertTrue(testListener.failedValues().contains(firstServiceName));
    }

    @Test
    public void parentStartStop() throws Exception {
        TestLifecycleListener testListener = new TestLifecycleListener();
        ServiceBuilder<?> sb = serviceContainer.addService();
        Consumer<String> providedValue = sb.provides(firstServiceName);

        // create parent with second and third services as children, add listener to childTarget
        final ParentService parentService = new ParentService(providedValue);
        parentService.addChild(secondServiceName).addChild(thirdServiceName);
        parentService.addListener(testListener);

        // install parent as first service; second and third services are expected to state as well
        sb.setInstance(parentService);
        sb.addListener(testListener);
        ServiceController<?> firstController = sb.install();

        serviceContainer.awaitStability();

        assertSame(3, testListener.upValues().size());
        assertTrue(testListener.upValues().contains(firstServiceName));
        assertTrue(testListener.upValues().contains(secondServiceName));
        assertTrue(testListener.upValues().contains(thirdServiceName));

        // stop first services; second and third services are expected to be removed
        firstController.setMode(Mode.NEVER);
        serviceContainer.awaitStability();

        assertSame(1, testListener.downValues().size());
        assertTrue(testListener.downValues().contains(firstServiceName));
        assertSame(2, testListener.removedValues().size());
        assertTrue(testListener.removedValues().contains(secondServiceName));
        assertTrue(testListener.removedValues().contains(thirdServiceName));

        // reactivate first service; second and third services should be installed again
        firstController.setMode(Mode.ACTIVE);
        serviceContainer.awaitStability();

        assertSame(3, testListener.upValues().size());
        assertTrue(testListener.upValues().contains(firstServiceName));
        assertTrue(testListener.upValues().contains(secondServiceName));
        assertTrue(testListener.upValues().contains(thirdServiceName));

        // move parent to ON_DEMAND mode; the dependency of its children on the parent are expected to
        // keep it alive
        firstController.setMode(Mode.ON_DEMAND);
        serviceContainer.awaitStability();

        // move second service to NEVER mode, causing it to stop
        serviceContainer.getRequiredService(secondServiceName).setMode(Mode.NEVER);
        serviceContainer.awaitStability();

        assertSame(1, testListener.downValues().size());
        assertTrue(testListener.downValues().contains(secondServiceName));

        // remove third service... this will trigger first service stop, which will cause second service removal
        serviceContainer.getRequiredService(thirdServiceName).setMode(Mode.REMOVE);
        serviceContainer.awaitStability();

        assertSame(1, testListener.downValues().size());
        assertTrue(testListener.downValues().contains(firstServiceName));
        assertSame(2, testListener.removedValues().size());
        assertTrue(testListener.removedValues().contains(secondServiceName));
        assertTrue(testListener.removedValues().contains(thirdServiceName));
    }

    @Test
    public void stopGrandParent() throws Exception {
        // create grandParent and parent services
        final ServiceName grandParentName = ServiceName.of("grand", "parent", "service");
        final ServiceName parentName = ServiceName.of("parent", "service");

        TestLifecycleListener testListener = new TestLifecycleListener();
        ServiceBuilder<?> sb = serviceContainer.addService();
        Consumer<String> providedValue = sb.provides(grandParentName);

        // create parent with second and third services as children, add listener to childTarget
        final ParentService grandParentService = new ParentService(providedValue);
        // grand parent has first service as child, and listener is added to its childTarget
        grandParentService.addChild(firstServiceName).addChild(secondServiceName).addListener(testListener);

        // install grandParent; first and second service should start as well
        sb.setInstance(grandParentService);
        sb.addListener(testListener);
        ServiceController<?> grandParentController = sb.install();

        serviceContainer.awaitStability();

        assertSame(serviceContainer.getRequiredService(grandParentName), grandParentController);
        assertSame(3, testListener.upValues().size());
        assertTrue(testListener.upValues().contains(grandParentName));
        assertTrue(testListener.upValues().contains(firstServiceName));
        assertTrue(testListener.upValues().contains(secondServiceName));

        // parent has third, fourth, and fifth service as children
        sb = grandParentService.getChildTarget().addService();
        providedValue = sb.provides(parentName);
        final ParentService parentService = new ParentService(providedValue);
        parentService.addChild(thirdServiceName).addChild(fourthServiceName).addChild(fifthServiceName).addListener(testListener);
        sb.setInstance(parentService);

        // install parent
        sb.install();

        serviceContainer.awaitStability();

        assertSame(serviceContainer.getRequiredService(grandParentName), grandParentController);
        // notifications of parent, third, fourth, and fifth service started are expected from testListener
        // (listener inherited from parent's child target)
        assertSame(7, testListener.upValues().size());
        assertTrue(testListener.upValues().contains(parentName));
        assertTrue(testListener.upValues().contains(thirdServiceName));
        assertTrue(testListener.upValues().contains(fourthServiceName));
        assertTrue(testListener.upValues().contains(fifthServiceName));

        // remove grand parent, this will cause children, parent, and grandchildren services to stop
        grandParentController.setMode(Mode.REMOVE);

        serviceContainer.awaitStability();

        assertSame(0, testListener.downValues().size());
        assertSame(0, testListener.upValues().size());
        assertSame(0, testListener.failedValues().size());
        assertSame(7, testListener.removedValues().size());
        assertTrue(testListener.removedValues().contains(grandParentName));
        assertTrue(testListener.removedValues().contains(parentName));
        assertTrue(testListener.removedValues().contains(firstServiceName));
        assertTrue(testListener.removedValues().contains(secondServiceName));
        assertTrue(testListener.removedValues().contains(thirdServiceName));
        assertTrue(testListener.removedValues().contains(fourthServiceName));
        assertTrue(testListener.removedValues().contains(fifthServiceName));
    }

    @Test
    public void stopGrandParentWithSubTarget() throws Exception {
        // create grandParent and parent services
        final ServiceName grandParentName = ServiceName.of("grand", "parent", "service");
        final ServiceName parentName = ServiceName.of("parent", "service");

        TestLifecycleListener testListener = new TestLifecycleListener();
        ServiceBuilder<?> sb = serviceContainer.addService();
        Consumer<String> providedValue = sb.provides(grandParentName);

        // grand parent has first & second service as child, and listener is added to its childTarget
        final ParentService grandParentService = new ParentService(providedValue);
        grandParentService.addListener(testListener);

        // install grandParent; first and second service should start as well
        sb.setInstance(grandParentService);
        sb.addListener(testListener);
        ServiceController<?> grandParentController = sb.install();

        serviceContainer.awaitStability();

        assertSame(serviceContainer.getRequiredService(grandParentName), grandParentController);
        assertSame(1, testListener.upValues().size());
        assertTrue(testListener.upValues().contains(grandParentName));

        // add first and second services as children of grand parent, using a subTarget of the childTarget
        ServiceTarget childSubTarget = grandParentService.getChildTarget().subTarget();
        sb = childSubTarget.addService();
        providedValue = sb.provides(firstServiceName);
        sb.setInstance(Service.newInstance(providedValue, firstServiceName.toString()));
        sb.install();

        sb = childSubTarget.addService();
        providedValue = sb.provides(secondServiceName);
        sb.setInstance(Service.newInstance(providedValue, secondServiceName.toString()));
        sb.install();

        serviceContainer.awaitStability();

        assertSame(3, testListener.upValues().size());
        assertTrue(testListener.upValues().contains(grandParentName));
        assertTrue(testListener.upValues().contains(firstServiceName));
        assertTrue(testListener.upValues().contains(secondServiceName));

        // parent has third, fourth, and fifth service as children
        sb = childSubTarget.addService();
        providedValue = sb.provides(parentName);
        final ParentService parentService = new ParentService(providedValue);
        parentService.addListener(testListener);
        sb.setInstance(parentService);

        // install parent
        ServiceController<?> parentController = sb.install();

        serviceContainer.awaitStability();

        assertSame(serviceContainer.getRequiredService(parentName), parentController);
        assertSame(4, testListener.upValues().size());
        assertTrue(testListener.upValues().contains(grandParentName));
        assertTrue(testListener.upValues().contains(parentName));
        assertTrue(testListener.upValues().contains(firstServiceName));
        assertTrue(testListener.upValues().contains(secondServiceName));

        // TODO
        // add third, fourth, and fifth services as children of parent, using a subTarget of the childTarget
        childSubTarget = parentService.getChildTarget().subTarget();

        sb = childSubTarget.addService();
        providedValue = sb.provides(thirdServiceName);
        sb.setInstance(Service.newInstance(providedValue, thirdServiceName.toString()));
        sb.install();

        sb = childSubTarget.addService();
        providedValue = sb.provides(fourthServiceName);
        sb.setInstance(Service.newInstance(providedValue, fourthServiceName.toString()));
        sb.install();

        sb = childSubTarget.addService();
        providedValue = sb.provides(fifthServiceName);
        sb.setInstance(Service.newInstance(providedValue, fifthServiceName.toString()));
        sb.install();

        serviceContainer.awaitStability();

        assertSame(7, testListener.upValues().size());
        assertTrue(testListener.upValues().contains(grandParentName));
        assertTrue(testListener.upValues().contains(parentName));
        assertTrue(testListener.upValues().contains(firstServiceName));
        assertTrue(testListener.upValues().contains(secondServiceName));
        assertTrue(testListener.upValues().contains(thirdServiceName));
        assertTrue(testListener.upValues().contains(fourthServiceName));
        assertTrue(testListener.upValues().contains(fifthServiceName));

        // remove grand parent, this will cause children, parent, and grandchildren services to stop
        grandParentController.setMode(Mode.REMOVE);

        serviceContainer.awaitStability();

        assertSame(0, testListener.downValues().size());
        assertSame(0, testListener.upValues().size());
        assertSame(0, testListener.failedValues().size());
        assertSame(7, testListener.removedValues().size());
        assertTrue(testListener.removedValues().contains(grandParentName));
        assertTrue(testListener.removedValues().contains(parentName));
        assertTrue(testListener.removedValues().contains(firstServiceName));
        assertTrue(testListener.removedValues().contains(secondServiceName));
        assertTrue(testListener.removedValues().contains(thirdServiceName));
        assertTrue(testListener.removedValues().contains(fourthServiceName));
        assertTrue(testListener.removedValues().contains(fifthServiceName));
    }

    @Test
    public void illegalChildTarget() throws Throwable {
        final ServiceController<?> parentController = serviceContainer.getService(parentServiceName);
        assertSame(State.UP, parentController.getState());

        try {
            parentService.getStartContext().getChildTarget();
            fail("IllegalStateException expected");
        } catch (IllegalStateException e) {}

        parentController.setMode(Mode.NEVER);
        serviceContainer.awaitStability();
        assertFalse(parentListener.upValues().contains(parentServiceName));
        assertTrue(parentListener.downValues().contains(parentServiceName));

        parentService.failNextTime();
        parentController.setMode(Mode.ACTIVE);
        serviceContainer.awaitStability();
        assertFalse(parentListener.downValues().contains(parentServiceName));
        assertTrue(parentListener.failedValues().contains(parentServiceName));

        try {
            parentService.getStartContext().getChildTarget();
            fail("IllegalStateException expected");
        } catch (IllegalStateException e) {}
    }

    public static class ParentService extends FailToStartService {

        private ServiceTarget childTarget;
        private StartContext startContext;
        private List<ServiceName> children = new ArrayList<ServiceName>();
        private List<LifecycleListener> listeners = new ArrayList<>();

        public ParentService(Consumer<String> providedValue) {
            super(providedValue, false);
        }

        public ParentService(Consumer<String> providedValue, boolean failToStart) {
            super(providedValue, failToStart);
        }

        @Override
        public void start(StartContext context) throws StartException {
            startContext = context;
            childTarget = context.getChildTarget();
            assertSame(childTarget, context.getChildTarget());
            for (LifecycleListener listener : listeners) {
                childTarget.addListener(listener);
            }
            for (ServiceName childName: children) {
                ServiceBuilder<?> sb = childTarget.addService();
                Consumer<String> providedValue = sb.provides(childName);
                sb.setInstance(Service.newInstance(providedValue, childName.toString()));
                sb.install();
            }
            super.start(context);
        }

        @Override
        public void stop(StopContext context) {}

        public ServiceTarget getChildTarget() {
            return this.childTarget;
        }

        public StartContext getStartContext() {
            return this.startContext;
        }

        public ParentService addChild(ServiceName childName) {
            children.add(childName);
            return this;
        }

        @SuppressWarnings({ "rawtypes", "unchecked" })
        public ParentService addListener(LifecycleListener listener) {
            listeners.add(listener);
            return this;
        }
    }
}
