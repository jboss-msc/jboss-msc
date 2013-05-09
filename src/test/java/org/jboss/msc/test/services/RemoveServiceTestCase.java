/*
 * JBoss, Home of Professional Open Source
 *
 * Copyright 2013 Red Hat, Inc. and/or its affiliates.
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
package org.jboss.msc.test.services;

import static org.junit.Assert.assertFalse;

import org.junit.Ignore;
import org.junit.Test;

/**
 * Test service removal.
 * 
 * @author <a href="mailto:frainone@redhat.com">Flavia Rainone</a>
 *
 */
public class RemoveServiceTestCase extends ServiceModeTestCase {

    @Override @Test
    public void neverModeService() throws Exception {
        super.neverModeService();
        remove(firstServiceName);
    }

    @Override @Test
    public void demandedNeverService() throws Exception {
        super.demandedNeverService();
        remove(firstServiceName);
        assertFalse(((TestService) serviceRegistry.getRequiredService(secondServiceName)).isUp());
    }

    @Override @Test
    public void upOnDemandSecondService() throws Exception {
        super.upOnDemandSecondService();
        remove(secondServiceName);
        assertFalse(((TestService) serviceRegistry.getRequiredService(firstServiceName)).isUp());
    }

    @Override @Test
    public void downOnDemandFirstService() throws Exception {
        super.downOnDemandFirstService();
        remove(firstServiceName);
    }

    @Override @Test @Ignore
    public void failedToStartOnDemandSecondService() throws Exception {
        super.failedToStartOnDemandSecondService();
        remove(secondServiceName);
        assertFalse(((TestService) serviceRegistry.getRequiredService(firstServiceName)).isUp());
    }

    @Override  @Test
    public void upLazySecondService() throws Exception {
        super.upLazySecondService();
        remove(secondServiceName);
        assertFalse(((TestService) serviceRegistry.getRequiredService(firstServiceName)).isUp());;
    }

    @Override @Ignore @Test
    public void upLazySecondServiceWithNoActiveDependents() throws Exception {
        super.upLazySecondServiceWithNoActiveDependents();
        remove(secondServiceName);
        assertFalse(((TestService) serviceRegistry.getRequiredService(firstServiceName)).isUp());;
    }

    @Override  @Test
    public void downLazyFirstService() throws Exception {
        super.downLazyFirstService();
        remove(firstServiceName);
    }

    @Ignore @Override @Test
    public void failedToStartLazySecondService() throws Exception {
        super.failedToStartLazySecondService();
        remove(secondServiceName);
        assertFalse(((TestService) serviceRegistry.getRequiredService(firstServiceName)).isUp());;
    }

    @Override @Test
    public void upPassiveFirstService() throws Exception{
        super.upPassiveFirstService();
        remove(firstServiceName);
    }

    @Override @Test
    public void downPassiveFirstService() throws Exception {
        super.downPassiveFirstService();
        remove(firstServiceName);
    }

    @Override @Ignore @Test
    public void failedToStartPassiveSecondService() throws Exception {
        super.failedToStartPassiveSecondService();
        remove(secondServiceName);
        assertFalse(((TestService) serviceRegistry.getRequiredService(firstServiceName)).isUp());;
    }
    
    @Override @Test
    public void upActiveFirstService() throws Exception{
        super.upActiveFirstService();
        remove(firstServiceName);
    }

    @Ignore @Override  @Test
    public void downActiveFirstService() throws Exception {
        super.downActiveFirstService();
        remove(firstServiceName);
    }

    @Ignore @Override @Test
    public void failedToStartActiveSecondService() throws Exception {
        super.failedToStartActiveSecondService();
        remove(secondServiceName);
        assertFalse(((TestService) serviceRegistry.getRequiredService(firstServiceName)).isUp());;
    }}
