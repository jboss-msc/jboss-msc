/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2012 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
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

package org.jboss.msc.service;

/**
 * Service modes.
 *
 * @author <a href="mailto:david.lloyd@redhat.com">David M. Lloyd</a>
 * @author <a href="mailto:frainone@redhat.com">Flavia Rainone</a>
 * @author <a href="mailto:ropalka@redhat.com">Richard Opalka</a>
 */
public enum ServiceMode {
    /**
     * Starts as soon as all required dependencies are met.
     * Will start even if there are no demandants
     * and will stay running if all demandants are gone. 
     * Actively demands dependencies at install time.
     */
    ACTIVE,
    /**
     * Starts as soon as all required dependencies are met and demanded to start.
     * Will stay running if all demandants are gone.
     * Does not demand dependencies at install time.
     */
    LAZY,
    /**
     * Starts as soon as all required dependencies are met and demanded to start.
     * Will stop if all demandants are gone.
     * Does not demand dependencies at install time.
     */
    ON_DEMAND,
    ;
}
