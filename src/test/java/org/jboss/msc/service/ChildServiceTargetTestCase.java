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

package org.jboss.msc.service;

import static org.junit.Assert.assertSame;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;

import org.jboss.msc.service.ServiceController.Mode;
import org.jboss.msc.service.ServiceController.State;
import org.jboss.msc.service.util.FailToStartService;
import org.jboss.msc.util.TestServiceListener;
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
    private TestServiceListener parentListener;

    @Before
    @Override
    public void initializeServiceTarget() throws Exception {
        parentService = new ParentService();
        parentListener = new TestServiceListener();
        Future<ServiceController<?>> parentStart = parentListener.expectServiceStart(ServiceName.of("parent"));
        serviceContainer.addService(ServiceName.of("parent"), parentService).addListener(parentListener).install();
        assertController(ServiceName.of("parent"), parentStart);
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
        final Future<ServiceController<?>> parentStopped = parentListener.expectServiceStop(parentServiceName);
        parentController.setMode(Mode.NEVER);
        assertController(parentServiceName, parentStopped);

        // edit child target at free will, nothing happens...
        serviceTarget.addDependency(secondServiceName);
        serviceTarget.addListener(testListener);
        ServiceBuilder<?> serviceBuilder = serviceTarget.addService(fourthServiceName, Service.NULL);
        // until we try to install service into it, then it fails wit an InvalidStateException
        try {
            serviceBuilder.install();
        } catch (IllegalStateException e) {}
    }

    @Test
    public void changeListenersInChildTarget() throws Exception {
        final TestServiceListener listener1 = new TestServiceListener();
        final TestServiceListener listener2 = new TestServiceListener();
        final TestServiceListener listener3 = new TestServiceListener();
        
        // create parent service with second service as child, and add listener2 to its child target
        final ParentService parent = new ParentService();
        parent.addChild(secondServiceName).addListener(listener2);

        // install parent as first service, with listener1...
        // parent start notification expected from listener1; child start notification expected from listener2
        final Future<ServiceController<?>> firstServiceStart = listener1.expectServiceStart(firstServiceName);
        final Future<ServiceController<?>> secondServiceStart = listener2.expectServiceStart(secondServiceName);
        serviceContainer.addService(firstServiceName, parent).addListener(listener1).install();
        assertController(firstServiceName, firstServiceStart);
        final ServiceController<?> secondController = assertController(secondServiceName, secondServiceStart);

        // edit parent's child target, removing listener2 from it, and adding listener3
        final ServiceTarget childTarget = parent.getChildTarget();
        childTarget.removeListener(listener2);
        childTarget.addListener(listener3);

        // add third service as a child... service start notification expected from listener3 this time
        final Future<ServiceController<?>> thirdServiceStart = listener3.expectServiceStart(thirdServiceName);
        childTarget.addService(thirdServiceName, Service.NULL).install();
        assertController(thirdServiceName, thirdServiceStart);

        // stop second service... service stop notification expected from listener 2
        final Future<ServiceController<?>> secondServiceStop = listener2.expectServiceStop(secondServiceName);
        secondController.setMode(Mode.NEVER);
        assertController(secondController, secondServiceStop);
    }

    @Test
    public void childFails() throws Exception {
        // child of parent will fail to start
        final Future<StartException> childFailure = testListener.expectServiceFailure(firstServiceName); 
        parentService.getChildTarget().addService(firstServiceName, new FailToStartService(true)).addListener(testListener).install();
        assertFailure(firstServiceName, childFailure);
        // this won't affect parent
        assertSame(State.UP, serviceContainer.getService(parentServiceName).getState());
    }

    @Test
    public void parentDependsOnChild() throws Exception {
        // create parent service with second service as a child; also add testListener to its childTarget
        final ParentService parentService = new ParentService();
        parentService.addListener(testListener);
        parentService.addChild(secondServiceName);

        // install parent as first service, with a dependency on its child secondService
        final Future<ServiceController<?>> firstServiceDepMissing = testListener.expectDependencyUninstall(firstServiceName);
        serviceContainer.addService(firstServiceName, parentService).addDependency(secondServiceName)
            .addListener(testListener).install();
        assertController(firstServiceName, firstServiceDepMissing);

        // install secondService as a normal, non-child, service...
        // this will cause firstService to attempt to start, but it will fail, as it will try to
        // add the already installed second service as a child on its start method
        final Future<ServiceController<?>> secondServiceStart = testListener.expectServiceStart(secondServiceName);
        final Future<StartException> firstServiceStartFailure = testListener.expectServiceFailure(firstServiceName);
        serviceContainer.addService(secondServiceName, Service.NULL).addListener(testListener).install();
        assertController(secondServiceName, secondServiceStart);
        assertFailure(firstServiceName, firstServiceStartFailure);
    }

    @Test
    public void parentDependsOnChildViaNickName() throws Exception {
        // create parent service with third service as a child; also add testListener to its childTarget
        final ParentService parentService = new ParentService();
        parentService.addListener(testListener);
        parentService.addChild(thirdServiceName);

        // install parent as first service, with a dependency on secondService
        final Future<ServiceController<?>> firstServiceDepMissing = testListener.expectDependencyUninstall(firstServiceName);
        serviceContainer.addService(firstServiceName, parentService).addDependency(secondServiceName)
            .addListener(testListener).install();
        assertController(firstServiceName, firstServiceDepMissing);

        // install thirdService as a normal, non-child, service, with secondService alias
        // this will cause firstService to attempt to start, but it will fail, as it will try to
        // add the already installed third service as a child on its start method
        final Future<ServiceController<?>> thirdServiceStart = testListener.expectServiceStart(thirdServiceName);
        final Future<StartException> firstServiceStartFailure = testListener.expectServiceFailure(firstServiceName);
        serviceContainer.addService(thirdServiceName, Service.NULL).addAliases(secondServiceName).addListener(testListener).install();
        assertController(thirdServiceName, thirdServiceStart);
        assertFailure(firstServiceName, firstServiceStartFailure);
    }

    @Test
    public void parentStartStop() throws Exception {
        // create parent with second and third services as children, add listener to childTarget
        final ParentService parent = new ParentService();
        parent.addChild(secondServiceName).addChild(thirdServiceName);
        parent.addListener(testListener);

        // install parent as first service; second and third services are expected to state as well
        Future<ServiceController<?>> firstServiceStart = testListener.expectServiceStart(firstServiceName);
        Future<ServiceController<?>> secondServiceStart = testListener.expectServiceStart(secondServiceName);
        Future<ServiceController<?>> thirdServiceStart = testListener.expectServiceStart(thirdServiceName);
        serviceContainer.addService(firstServiceName, parent).addListener(testListener).install();
        final ServiceController<?> firstController = assertController(firstServiceName, firstServiceStart);
        ServiceController<?> secondController = assertController(secondServiceName, secondServiceStart);
        ServiceController<?> thirdController = assertController(thirdServiceName, thirdServiceStart);

        // stop first services; second and third services are expected to be removed
        Future<ServiceController<?>> firstServiceStop = testListener.expectServiceStop(firstServiceName);
        Future<ServiceController<?>> secondServiceRemoval = testListener.expectServiceRemoval(secondServiceName);
        Future<ServiceController<?>> thirdServiceRemoval = testListener.expectServiceRemoval(thirdServiceName);
        firstController.setMode(Mode.NEVER);
        assertController(firstController, firstServiceStop);
        assertController(secondController, secondServiceRemoval);
        assertController(thirdController, thirdServiceRemoval);

        // reactivate first service; second and third services should be installed again
        firstServiceStart = testListener.expectServiceStart(firstServiceName);
        secondServiceStart = testListener.expectServiceStart(secondServiceName);
        thirdServiceStart = testListener.expectServiceStart(thirdServiceName);
        firstController.setMode(Mode.ACTIVE);
        assertController(firstServiceName, firstServiceStart);
        secondController = assertController(secondServiceName, secondServiceStart);
        thirdController = assertController(thirdServiceName, thirdServiceStart);

        // move parent to ON_DEMAND mode; the dependency of its children on the parent are expected to
        // keep it alive
        firstController.setMode(Mode.ON_DEMAND);

        // move second service to NEVER mode, causing it to stop
        final Future<ServiceController<?>> secondServiceStop = testListener.expectServiceStop(secondServiceName);
        secondController.setMode(Mode.NEVER);
        assertController(secondController, secondServiceStop);

        // remove third service... this will trigger first service stop, which will cause second service removal
        thirdServiceRemoval = testListener.expectServiceRemoval(thirdServiceName);
        secondServiceRemoval = testListener.expectServiceRemoval(secondServiceName);
        firstServiceStop = testListener.expectServiceStop(firstServiceName);
        thirdController.setMode(Mode.REMOVE);
        assertController(thirdController, thirdServiceRemoval);
        assertController(firstController, firstServiceStop);
    }

    @Test
    public void parentStartFailed() throws Exception {
        // install first service, that depends on missing second service
        final ServiceController<?> firstController = serviceContainer.addService(firstServiceName, Service.NULL)
            .addListener(testListener).addDependency(secondServiceName).install();
        assertController(firstServiceName, firstController);

        // create parent, mark to fail on start
        final ParentService parent = new ParentService(true);
        // add third, fourth, and fifth services as children, and add testListener to childTarget
        parent.addChild(thirdServiceName).addChild(fourthServiceName).addChild(fifthServiceName);
        parent.addListener(testListener);

        // install parent as second service, firstService is supposed to send a dep failed notification
        final Future<ServiceController<?>> firstServiceDependencyFailed = testListener.expectDependencyFailure(firstServiceName);
        final Future<StartException> secondServiceFailure = testListener.expectServiceFailure(secondServiceName);
        serviceContainer.addService(secondServiceName, parent).addListener(testListener).install();
        final ServiceController<?> secondController = assertFailure(secondServiceName, secondServiceFailure);
        assertController(firstController, firstServiceDependencyFailed);

        // move parent to NEVER mode, and expect the process to finish by waiting for firstServiceDepFailureCleared notification
        final Future<ServiceController<?>> firstServiceDepFailureCleared = testListener.expectDependencyFailureCleared(firstServiceName);
        secondController.setMode(Mode.NEVER);
        assertController(firstController, firstServiceDepFailureCleared);

        // reactivate parent, this time it won't fail to start
        // this will trigger the start of children third, fourth, and fifth services
        final Future<ServiceController<?>> secondServiceStart = testListener.expectServiceStart(secondServiceName);
        final Future<ServiceController<?>> thirdServiceStart = testListener.expectServiceStart(thirdServiceName);
        final Future<ServiceController<?>> fourthServiceStart = testListener.expectServiceStart(fourthServiceName);
        final Future<ServiceController<?>> fifthServiceStart = testListener.expectServiceStart(fifthServiceName);
        secondController.setMode(Mode.ACTIVE);
        assertController(secondController, secondServiceStart);
        final ServiceController<?> thirdController = assertController(thirdServiceName, thirdServiceStart);
        final ServiceController<?> fourthController = assertController(fourthServiceName, fourthServiceStart);
        final ServiceController<?> fifthController = assertController(fifthServiceName, fifthServiceStart);

        // move third service to NEVER mode, thus causing it to stop
        final Future<ServiceController<?>> thirdServiceStop = testListener.expectServiceStop(thirdServiceName);
        thirdController.setMode(Mode.NEVER);
        assertController(thirdController, thirdServiceStop);

        // remove fourth service
        final Future<ServiceController<?>> fourthServiceRemoval = testListener.expectServiceRemoval(fourthServiceName);
        fourthController.setMode(Mode.REMOVE);
        assertController(fourthController, fourthServiceRemoval);

        // move parent to ON_DEMAND mode
        secondController.setMode(Mode.ON_DEMAND);
        // it won't stop because the only dependent, its child fifth service, is UP
        assertSame(State.UP, secondController.getState());
        assertSame(State.UP, fifthController.getState());

        // remove parent, causing children third and fifth to be removed as well
        final Future<ServiceController<?>> secondServiceRemoval = testListener.expectServiceRemoval(secondServiceName);
        final Future<ServiceController<?>> thirdServiceRemoval = testListener.expectServiceRemoval(thirdServiceName);
        final Future<ServiceController<?>> fifthServiceRemoval = testListener.expectServiceRemoval(fifthServiceName);
        secondController.setMode(Mode.REMOVE);
        assertController(secondController, secondServiceRemoval);
        assertController(thirdController, thirdServiceRemoval);
        assertController(fifthController, fifthServiceRemoval);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void twoLevelChildrenServices() throws Exception {
        // create grandParent and parent services
        final ServiceName grandParentName = ServiceName.of("grand", "parent", "service");
        final ServiceName parentName = ServiceName.of("parent", "service");
        final TestServiceListener testListener1_1 = new TestServiceListener();
        final TestServiceListener testListener1_2 = new TestServiceListener();
        final TestServiceListener testListener1_3 = new TestServiceListener();
        final TestServiceListener testListener2_1 = new TestServiceListener();
        final TestServiceListener testListener2_2 = new TestServiceListener();

        // grand parent has first service as child, and listener is added to its childTarget
        final ParentService grandParentService = new ParentService();
        grandParentService.addChild(firstServiceName).addListener(testListener);
        // parent has fourth and fifth service as children
        final ParentService parentService = new ParentService();
        parentService.addChild(fourthServiceName).addChild(fifthServiceName);
        // parent's child target has testListener2_1 and testListener2_2
        parentService.addListener(testListener2_1).addListener(testListener2_2);

        // install grandParent; its only child first service should start as well
        final Future<ServiceController<?>> grandParentStart = testListener.expectServiceStart(grandParentName);
        final Future<ServiceController<?>> firstServiceStart = testListener.expectServiceStart(firstServiceName);
        final ServiceController<?> grandParentController = serviceContainer.addService(grandParentName, grandParentService)
            .addListener(testListener).install();
        assertController(grandParentName, grandParentController);
        assertController(grandParentController, grandParentStart);
        final ServiceController<?> firstController = assertController(firstServiceName, firstServiceStart);
        // add testListener1_2 to first child... as we will see, this does not affect any other service in this test
        firstController.addListener(testListener1_2);

        // add testListener1_1 to grand parent's child target; this will affect all grand parent's children created
        // from now on, but won't affect first service
        grandParentService.getChildTarget().addListener(testListener, testListener1_1);

        // install second service as child of grand parent; it won't start because it has a dependency on
        // missing third service
        final Future<ServiceController<?>> secondServiceDepMissing = testListener.expectDependencyUninstall(secondServiceName);
        final Future<ServiceController<?>> secondServiceDepMissing1_1 = testListener1_1.expectDependencyUninstall(secondServiceName);
        final Future<ServiceController<?>> secondServiceDepMissing1_3 = testListener1_3.expectDependencyUninstall(secondServiceName);
        final ServiceController<?> secondController = grandParentService.getChildTarget()
            .addService(secondServiceName, Service.NULL).addDependency(thirdServiceName)
            .addListener(testListener1_3).install();
        // notifications expected from testListener, testListener1_1 (both from parent's child target), and testListener1_3
        assertController(secondServiceName, secondController);
        assertController(secondController, secondServiceDepMissing);
        assertController(secondController, secondServiceDepMissing1_1);
        assertController(secondController, secondServiceDepMissing1_3);

        // install third service; dependent second service is supposed to start 
        final Future<ServiceController<?>> thirdServiceStart = testListener.expectServiceStart(thirdServiceName);
//        Future<ServiceController<?>> secondServiceDepInstall = testListener.expectDependencyInstall(secondServiceName);
//        Future<ServiceController<?>> secondServiceDepInstall1_1 = testListener1_1.expectDependencyInstall(secondServiceName);
//        Future<ServiceController<?>> secondServiceDepInstall1_3 = testListener1_3.expectDependencyInstall(secondServiceName);
        final Future<ServiceController<?>> secondServiceStart = testListener.expectDependencyInstall(secondServiceName);
        final Future<ServiceController<?>> secondServiceStart1_1 = testListener1_1.expectDependencyInstall(secondServiceName);
        final Future<ServiceController<?>> secondServiceStart1_3 = testListener1_3.expectDependencyInstall(secondServiceName);
        serviceContainer.addService(thirdServiceName, Service.NULL).addListener(testListener).install();
        final ServiceController<?> thirdController = assertController(thirdServiceName, thirdServiceStart);
        // FIXME
//        assertController(secondController, secondServiceDepInstall);
//        assertController(secondController, secondServiceDepInstall1_1);
//        assertController(secondController, secondServiceDepInstall1_3);
        assertController(secondController, secondServiceStart);
        assertController(secondController, secondServiceStart1_1);
        assertController(secondController, secondServiceStart1_3);

        // install parent with listeners 2_1 and listener2_2
        final Future<ServiceController<?>> parentServiceStart = testListener.expectServiceStart(parentName);
        final Future<ServiceController<?>> parentServiceStart1_1 = testListener1_1.expectServiceStart(parentName);
        final Future<ServiceController<?>> parentServiceStart2_1 = testListener2_1.expectServiceStart(parentName);
        final Future<ServiceController<?>> parentServiceStart2_2 = testListener2_2.expectServiceStart(parentName);
        final Future<ServiceController<?>> fourthServiceStart = testListener.expectServiceStart(fourthServiceName);
        final Future<ServiceController<?>> fourthServiceStart1_1 = testListener1_1.expectServiceStart(fourthServiceName);
        final Future<ServiceController<?>> fourthServiceStart2_1 = testListener2_1.expectServiceStart(fourthServiceName);
        final Future<ServiceController<?>> fourthServiceStart2_2 = testListener2_2.expectServiceStart(fourthServiceName);
        final Future<ServiceController<?>> fifthServiceStart = testListener.expectServiceStart(fifthServiceName);
        final Future<ServiceController<?>> fifthServiceStart1_1 = testListener1_1.expectServiceStart(fifthServiceName);
        final Future<ServiceController<?>> fifthServiceStart2_1 = testListener2_1.expectServiceStart(fifthServiceName);
        final Future<ServiceController<?>> fifthServiceStart2_2 = testListener2_2.expectServiceStart(fifthServiceName);
        final ServiceController<?> parentController = grandParentService.getChildTarget()
            .addService(parentName, parentService).addListener(testListener2_1, testListener2_2).addDependency(thirdServiceName).install();
        assertController(parentName, parentController);
        // notifications of parent, fourth, and fifth service started are expected from testListener,
        // testListener1_1 (both from grand'parents child target), and testListener2_1, testListener2_2
        // (both from parent's child target)
        assertController(parentController, parentServiceStart);
        assertController(parentController, parentServiceStart1_1);
        assertController(parentController, parentServiceStart2_1);
        assertController(parentController, parentServiceStart2_2);
        final ServiceController<?> fourthController = assertController(fourthServiceName, fourthServiceStart);
        assertController(fourthController, fourthServiceStart1_1);
        assertController(fourthController, fourthServiceStart2_1);
        assertController(fourthController, fourthServiceStart2_2);
        final ServiceController<?> fifthController = assertController(fifthServiceName, fifthServiceStart);
        assertController(fifthController, fifthServiceStart1_1);
        assertController(fifthController, fifthServiceStart2_1);
        assertController(fifthController, fifthServiceStart2_2);

        // remove third service, this will cause parent service to stop, and fourth and fifth services to be removed
        final Future<ServiceController<?>> thirdServiceRemoval = testListener.expectServiceRemoval(thirdServiceName);
        final Future<ServiceController<?>> parentServiceDepMissing = testListener.expectDependencyUninstall(parentName);
        final Future<ServiceController<?>> parentServiceDepMissing1_1 = testListener1_1.expectDependencyUninstall(parentName);
        final Future<ServiceController<?>> parentServiceDepMissing2_1 = testListener2_1.expectDependencyUninstall(parentName);
        final Future<ServiceController<?>> parentServiceDepMissing2_2 = testListener2_2.expectDependencyUninstall(parentName);
        final Future<ServiceController<?>> parentServiceStop = testListener.expectServiceStop(parentName);
        final Future<ServiceController<?>> parentServiceStop1_1 = testListener1_1.expectServiceStop(parentName);
        final Future<ServiceController<?>> parentServiceStop2_1 = testListener2_1.expectServiceStop(parentName);
        final Future<ServiceController<?>> parentServiceStop2_2 = testListener2_2.expectServiceStop(parentName);
        final Future<ServiceController<?>> fourthServiceRemoval = testListener.expectServiceRemoval(fourthServiceName);
        final Future<ServiceController<?>> fourthServiceRemoval1_1 = testListener1_1.expectServiceRemoval(fourthServiceName);
        final Future<ServiceController<?>> fourthServiceRemoval2_1 = testListener2_1.expectServiceRemoval(fourthServiceName);
        final Future<ServiceController<?>> fourthServiceRemoval2_2 = testListener2_2.expectServiceRemoval(fourthServiceName);
        final Future<ServiceController<?>> fifthServiceRemoval = testListener.expectServiceRemoval(fifthServiceName);
        final Future<ServiceController<?>> fifthServiceRemoval1_1 = testListener1_1.expectServiceRemoval(fifthServiceName);
        final Future<ServiceController<?>> fifthServiceRemoval2_1 = testListener2_1.expectServiceRemoval(fifthServiceName);
        final Future<ServiceController<?>> fifthServiceRemoval2_2 = testListener2_2.expectServiceRemoval(fifthServiceName);
        thirdController.setMode(Mode.REMOVE);
        assertController(thirdController, thirdServiceRemoval);
        assertController(parentController, parentServiceDepMissing);
        assertController(parentController, parentServiceDepMissing1_1);
        assertController(parentController, parentServiceDepMissing2_1);
        assertController(parentController, parentServiceDepMissing2_2);
        assertController(parentController, parentServiceStop);
        assertController(parentController, parentServiceStop1_1);
        assertController(parentController, parentServiceStop2_1);
        assertController(parentController, parentServiceStop2_2);
        assertController(fourthController, fourthServiceRemoval);
        assertController(fourthController, fourthServiceRemoval1_1);
        assertController(fourthController, fourthServiceRemoval2_1);
        assertController(fourthController, fourthServiceRemoval2_2);
        assertController(fifthController, fifthServiceRemoval);
        assertController(fifthController, fifthServiceRemoval1_1);
        assertController(fifthController, fifthServiceRemoval2_1);
        assertController(fifthController, fifthServiceRemoval2_2);
    }

    public static class ParentService extends FailToStartService {

        private ServiceTarget childTarget;
        private List<ServiceName> children = new ArrayList<ServiceName>();
        private List<ServiceListener<Object>> listeners = new ArrayList<ServiceListener<Object>>();

        public ParentService() {
            super(false);
        }

        public ParentService(boolean failToStart) {
            super(failToStart);
        }

        @Override
        public Void getValue() throws IllegalStateException, IllegalArgumentException {
            return null;
        }

        @Override
        public void start(StartContext context) throws StartException {
            childTarget = context.getChildTarget();
            childTarget.addListener(listeners);
            for (ServiceName childName: children) {
                childTarget.addService(childName, Service.NULL).install();
            }
            super.start(context);
        }

        @Override
        public void stop(StopContext context) {}
        
        public ServiceTarget getChildTarget() {
            return this.childTarget;
        }

        public ParentService addChild(ServiceName childName) {
            children.add(childName);
            return this;
        }

        @SuppressWarnings({ "rawtypes", "unchecked" })
        public ParentService addListener(ServiceListener listener) {
            listeners.add(listener);
            return this;
        }
    }
}
